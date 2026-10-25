package com.nightwave.player.offline

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.nightwave.player.net.YouTubeGateway
import java.io.IOException

/**
 * User-approved single audio-only stream save. This does not bypass DRM,
 * require authentication/cookies, access paid media or store expiring URLs.
 * If extraction fails, the network denies access or Android cannot validate
 * the audio: FAIL without marking the searched track offline-ready.
 */
class SearchTrackSaveWorker(
    context: Context, parameters: WorkerParameters
) : Worker(context, parameters) {
    private val channel = "nightwave_searched_audio"
    private val notificationId = 17000 + (id.hashCode() and 0x0fff)
    @Volatile private var transfer: PermittedAudioDownloader? = null

    override fun doWork(): Result {
        val canonical = inputData.getString(SearchTrackSaveQueue.KEY_URL).orEmpty()
        val title = inputData.getString(SearchTrackSaveQueue.KEY_TITLE).orEmpty()
        if (SearchTrackSavePolicy.canonicalTrack(canonical) != canonical ||
            title.isBlank()) return fail("Invalid saved track information")
        if (isStopped) return fail("Cancelled")
        SearchTrackOfflineIndex.readySong(applicationContext, canonical)?.let {
            return Result.success(Data.Builder()
                .putString(SearchTrackSaveQueue.KEY_ID, it.id).build())
        }
        return try {
            ensureChannel()
            // Resolve and restart cleanly on WorkManager retries.
            setProgressAsync(Data.Builder()
                .putLong(SearchTrackSaveQueue.KEY_RECEIVED, 0L)
                .putLong(SearchTrackSaveQueue.KEY_TOTAL, -1L).build())
            setForegroundAsync(foreground(title, 0L, null)).get()
            if (isStopped) return fail("Cancelled")
            // Resolve at execution time. Signed streaming addresses expire and
            // must not be queued, cached or persisted for a later worker run.
            val stream = YouTubeGateway().resolveAudioOnlyForOffline(canonical)
            if (isStopped) return fail("Cancelled")
            val downloader = PermittedAudioDownloader(applicationContext)
            transfer = downloader
            var lastRefresh = 0L
            var lastReported = -1L
            val saved = downloader.downloadResolvedSearchAudio(
                stream.audioUrl, stream.video.title.ifBlank { title }
            ) { received, total ->
                if (isStopped) {
                    downloader.cancel()
                    throw IOException("Cancelled")
                }
                val now = SystemClock.elapsedRealtime()
                if (lastRefresh == 0L || now - lastRefresh >= 800L ||
                    (total != null && received == total) || received < lastReported) {
                    lastRefresh = now
                    lastReported = received
                    setProgressAsync(Data.Builder()
                        .putLong(SearchTrackSaveQueue.KEY_RECEIVED, received)
                        .putLong(SearchTrackSaveQueue.KEY_TOTAL, total ?: -1L).build())
                    setForegroundAsync(foreground(title, received, total))
                }
            }
            if (isStopped) fail("Cancelled")
            else if (!SearchTrackOfflineIndex.linkVerified(applicationContext, canonical, saved)) {
                fail("Audio was saved in Offline Library, but its search link was not recorded")
            } else {
                val verified = OfflineAudioLibrary(applicationContext)
                    .availableIds(setOf(saved.id))
                if (!OfflineTransferCompletionPolicy.canCommit(isStopped, saved.id, verified))
                    fail(OfflineTransferCompletionPolicy.NOT_VERIFIED)
                else Result.success(Data.Builder()
                    .putString(SearchTrackSaveQueue.KEY_ID, saved.id).build())
            }
        } catch (error: Exception) {
            if (isStopped) fail("Cancelled")
            else if (error is IOException && runAttemptCount < 1) Result.retry()
            else fail(SearchOfflineFailurePolicy.message(
                error.message, error is IOException))
        } finally {
            transfer = null
        }
    }

    override fun onStopped() {
        transfer?.cancel()
        super.onStopped()
    }

    private fun fail(reason: String): Result = Result.failure(Data.Builder()
        .putString(SearchTrackSaveQueue.KEY_ERROR, reason).build())

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
            manager.createNotificationChannel(NotificationChannel(channel,
                "NightWave save offline", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Progress of audio-only offline saving requested by the user"
                setShowBadge(false)
            })
        }
    }

    private fun foreground(title: String, received: Long, total: Long?): ForegroundInfo {
        val open = PendingIntent.getActivity(applicationContext, notificationId,
            Intent(applicationContext, CatalogDownloadsActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val percent = total?.takeIf { it > 0L }
            ?.let { ((received * 100L) / it).toInt().coerceIn(0, 100) } ?: 0
        val notification: Notification = NotificationCompat.Builder(
            applicationContext, channel
        ).setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("NightWave - Saving offline")
            .setContentText(title.take(64))
            .setContentIntent(open)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, percent, total == null || total <= 0)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel",
                WorkManager.getInstance(applicationContext).createCancelPendingIntent(id))
            .build()
        return if (Build.VERSION.SDK_INT >= 29) {
            ForegroundInfo(notificationId, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else ForegroundInfo(notificationId, notification)
    }
}
