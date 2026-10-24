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
import java.io.IOException

/**
 * A user-consented direct HTTPS audio download that survives Activity closing,
 * backgrounding, and ordinary process restarts. Interrupted downloads restart
 * from zero and are validated before the offline-library index is committed.
 */
class DirectAudioDownloadWorker(
    context: Context, parameters: WorkerParameters
) : Worker(context, parameters) {
    @Volatile private var transfer: PermittedAudioDownloader? = null
    private val channelId = "nightwave_authorized_audio"
    private val notificationId = 6500 + (id.hashCode() and 0x0fff)

    override fun doWork(): Result {
        val url = inputData.getString(DirectAudioDownloadQueue.KEY_URL).orEmpty()
        val title = inputData.getString(DirectAudioDownloadQueue.KEY_NAME).orEmpty()
        val source = runCatching { PermittedAudioDownloadPolicy.prepare(url, title) }
            .getOrElse { return fail("Unsupported direct audio source") }
        if (isStopped) return fail("Download cancelled")
        return try {
            makeNotificationChannel()
            setForegroundAsync(foreground(source.fileName, 0L, null)).get()
            if (isStopped) return fail("Download cancelled")
            val downloader = PermittedAudioDownloader(applicationContext)
            transfer = downloader
            var lastProgress = -1L
            var lastTime = 0L
            val saved = downloader.download(source) { received, total ->
                if (isStopped) {
                    downloader.cancel()
                    throw IOException("Download cancelled")
                }
                val now = SystemClock.elapsedRealtime()
                if (lastProgress < 0L || now - lastTime > 750L || received == total) {
                    lastProgress = received
                    lastTime = now
                    setProgressAsync(Data.Builder()
                        .putLong(DirectAudioDownloadQueue.KEY_RECEIVED, received)
                        .putLong(DirectAudioDownloadQueue.KEY_TOTAL, total ?: -1L)
                        .build())
                    setForegroundAsync(foreground(source.fileName, received, total))
                }
            }
            if (isStopped) fail("Download cancelled") else
                Result.success(Data.Builder()
                    .putString(DirectAudioDownloadQueue.KEY_DONE, saved.id)
                    .build())
        } catch (error: Exception) {
            if (isStopped) fail("Download cancelled")
            else if (error is IOException && runAttemptCount < 2) Result.retry()
            else fail(
                if (error is IOException) "Network interrupted; requeue the audio link."
                else "Download rejected; check audio type, source or permissions."
            )
        } finally {
            transfer = null
        }
    }

    override fun onStopped() {
        transfer?.cancel()
        super.onStopped()
    }

    private fun fail(reason: String): Result =
        Result.failure(Data.Builder()
            .putString(DirectAudioDownloadQueue.KEY_ERROR, reason).build())

    private fun makeNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
            manager.createNotificationChannel(NotificationChannel(channelId,
                "NightWave audio downloads", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Progress of user-authorized direct audio downloads"
                setShowBadge(false)
            })
        }
    }

    private fun foreground(name: String, received: Long, total: Long?): ForegroundInfo {
        val target = Intent(applicationContext, OfflineLibraryActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val intent = PendingIntent.getActivity(applicationContext, notificationId, target,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val hasTotal = total != null && total > 0
        val percent = total?.takeIf { it > 0 }?.let {
            (received * 100L / it).toInt().coerceIn(0, 100)
        } ?: 0
        val notification: Notification = NotificationCompat.Builder(
            applicationContext, channelId
        ).setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("NightWave - Offline audio")
            .setContentText(name.take(64))
            .setContentIntent(intent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, percent, !hasTotal)
            .build()
        return if (Build.VERSION.SDK_INT >= 29) {
            ForegroundInfo(notificationId, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else ForegroundInfo(notificationId, notification)
    }
}
