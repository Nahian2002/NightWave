package com.nightwave.player.v2.cloud

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.tasks.Tasks
import com.nightwave.player.library.MusicLibrary
import com.nightwave.player.v2.lyrics.PrivateLyricsStore
import com.nightwave.player.v2.data.GuestProfileStore
import com.nightwave.player.v2.data.LibraryArchiveManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** Work can run late; permission prompts never run in background. */
class NightWaveCloudWorker(
    context: Context, params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val store = CloudBackupState(applicationContext)
        val settings = store.read()
        val consent = settings.consentSnapshot()
        if (!settings.enabled || settings.account.isBlank() || settings.lastSuccessMs <= 0L) {
            return@withContext Result.success()
        }
        var accessRevision = settings.accessRevision
        try {
            val authorization = Tasks.await(
                Identity.getAuthorizationClient(applicationContext)
                    .authorize(NightWaveCloudAuthorization.request(settings.account)),
                20, TimeUnit.SECONDS
            )
            if (authorization.hasResolution()) {
                if (store.recordAccessIfCurrent(consent, CloudAccessState.NEEDS_REVIEW,
                        expectedAccessRevision = accessRevision) == null) return@withContext Result.success()
                store.statusIfCurrent(consent,
                    "Google needs permission again. Open NightWave backup settings.")
                return@withContext Result.success()
            }
            val token = NightWaveCloudAuthorization.requireToken(authorization)
            // Drive's about endpoint binds identity to the actual authorized
            // token even when Google's deprecated profile field is omitted.
            val account = NightWaveDriveClient(token).authorizedEmailAddress()
            val legacyAccount = NightWaveCloudAuthorization.authorizedAccount(authorization)
            if (!settings.account.equals(account, ignoreCase = true) ||
                (legacyAccount != null && !account.equals(legacyAccount, ignoreCase = true))) {
                if (store.recordAccessIfCurrent(consent, CloudAccessState.NEEDS_REVIEW,
                        expectedAccessRevision = accessRevision) == null) return@withContext Result.success()
                store.statusIfCurrent(consent,
                    "Google account changed; reconnect backup in NightWave.")
                return@withContext Result.success()
            }
            // Recheck before network upload; disconnect means no new snapshot.
            if (!CloudBackupConsentPolicy.same(consent,
                    store.read().consentSnapshot())) {
                return@withContext Result.success()
            }
            store.recordAccessIfCurrent(consent, CloudAccessState.CHECKED)?.let { accessRevision = it }
            val backup = LibraryArchiveManager(applicationContext).export()
            val history = if (settings.includeHistory)
                MusicLibrary(applicationContext).recent() else emptyList()
            val extraPreferences = if (settings.includePreferences)
                CloudPlaybackPreferences.from(GuestProfileStore(applicationContext).load())
                else null
            val extraLyrics = if (settings.includeLyrics)
                PrivateLyricsStore(applicationContext).exportCloud() else emptyList()
            val payload = CloudBackupCodec.encode(backup, history, settings.includeHistory,
                extraPreferences, extraLyrics, settings.includeLyrics)
            val beforeUpload = store.read()
            if (!CloudBackupConsentPolicy.same(consent,
                    beforeUpload.consentSnapshot())) {
                // Preferences changed while the worker prepared its payload:
                // never upload with a stale history/privacy or network choice.
                return@withContext Result.success()
            }
            val outcome = NightWaveDriveClient(token).backUpIfChangedVerified(payload)
            // Upload cannot be rolled back, but a changed privacy choice/account
            // must NEVER be reported as the current verified backup.
            if (!CloudBackupConsentPolicy.same(consent,
                    store.read().consentSnapshot())) return@withContext Result.success()
            if (store.successIfCurrent(consent, System.currentTimeMillis(), outcome.created))
                Result.success()
            else if (!CloudBackupConsentPolicy.same(consent, store.read().consentSnapshot()))
                Result.success() // A new consent owns the next operation.
            else {
                store.statusIfCurrent(consent, "Drive copy verified; local receipt could not be saved. Open backup settings.")
                Result.failure()
            }
        } catch (cancel: CancellationException) {
            // WorkManager cancellation is not a failed/retryable backup.
            // In particular, disconnecting the Drive account must not
            // enqueue extra work or replace the user's current status.
            throw cancel
        } catch (error: Exception) {
            // A disconnected or reconfigured account must not be overwritten
            // by a late failure from its cancelled/stale previous worker.
            if (!CloudBackupConsentPolicy.same(consent,
                    store.read().consentSnapshot())) return@withContext Result.success()
            val driveCode = CloudFailureCausePolicy.find(error, DriveHttpFailure::class.java)?.statusCode
            val googleCode = CloudFailureCausePolicy.find(error,
                com.google.android.gms.common.api.ApiException::class.java)?.statusCode
            val failureStage = if (driveCode != null) CloudAuthorizationFailurePolicy.Stage.DRIVE_CHECK
                else CloudAuthorizationFailurePolicy.Stage.REQUEST
            if (store.recordAccessIfCurrent(consent, CloudAccessPolicy.afterFailure(
                    settings.accessState, failureStage, driveCode ?: googleCode),
                    expectedAccessRevision = accessRevision) == null) return@withContext Result.success()
            when {
                googleCode != null &&
                    !CloudAuthorizationFailurePolicy.canSuggestRetry(googleCode) -> {
                    // Background work cannot present a consent picker. Config
                    // failures and user cancellations require an explicit
                    // interactive action; retrying forever will not fix them.
                    store.statusIfCurrent(consent,
                        CloudAuthorizationFailurePolicy.shortMessage(
                            googleCode, CloudAuthorizationFailurePolicy.Stage.REQUEST))
                    Result.failure()
                }
                driveCode != null && !DriveHttpFailurePolicy.mayRetry(driveCode) -> {
                    store.statusIfCurrent(consent, DriveHttpFailurePolicy.message(driveCode))
                    Result.failure()
                }
                else -> {
                    store.statusIfCurrent(consent,
                        if (googleCode != null) "Automatic Drive authorization temporarily unavailable; " +
                            "retrying when available"
                        else "Automatic backup could not finish; will retry when available")
                    if (runAttemptCount < 3) Result.retry() else Result.failure()
                }
            }
        }
    }
}

object NightWaveCloudSchedule {
    private const val NAME = "nightwave_google_drive_daily_v1"

    fun update(context: Context) {
        val state = CloudBackupState(context).read()
        if (!state.enabled || state.account.isBlank() || state.lastSuccessMs == 0L) {
            stop(context)
            return
        }
        val type = if (state.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        val request = PeriodicWorkRequestBuilder<NightWaveCloudWorker>(24, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(type).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NAME, ExistingPeriodicWorkPolicy.UPDATE, request
        )
    }

    fun stop(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(NAME)
    }
}
