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
        try {
            val authorization = Tasks.await(
                Identity.getAuthorizationClient(applicationContext)
                    .authorize(NightWaveCloudAuthorization.request(settings.account)),
                20, TimeUnit.SECONDS
            )
            if (authorization.hasResolution()) {
                store.status("Google needs permission again. Open NightWave backup settings.")
                return@withContext Result.success()
            }
            val token = NightWaveCloudAuthorization.requireToken(authorization)
            // Drive's about endpoint binds identity to the actual authorized
            // token even when Google's deprecated profile field is omitted.
            val account = NightWaveDriveClient(token).authorizedEmailAddress()
            val legacyAccount = NightWaveCloudAuthorization.authorizedAccount(authorization)
            if (!settings.account.equals(account, ignoreCase = true) ||
                (legacyAccount != null && !account.equals(legacyAccount, ignoreCase = true))) {
                store.status("Google account changed; reconnect backup in NightWave.")
                return@withContext Result.success()
            }
            // Recheck before network upload; disconnect means no new snapshot.
            if (!CloudBackupConsentPolicy.same(consent,
                    store.read().consentSnapshot())) {
                return@withContext Result.success()
            }
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
            if (store.success(settings.account, System.currentTimeMillis()) && !outcome.created) {
                store.status("Already backed up; library unchanged")
            }
            Result.success()
        } catch (error: Exception) {
            val code = (error as? DriveHttpFailure)?.statusCode
            if (code != null && !DriveHttpFailurePolicy.mayRetry(code)) {
                store.status(DriveHttpFailurePolicy.message(code))
                Result.failure()
            } else {
                store.status("Automatic backup could not finish; will retry when available")
                if (runAttemptCount < 3) Result.retry() else Result.failure()
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
