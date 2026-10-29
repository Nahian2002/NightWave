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
import android.os.StatFs
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkInfo
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import java.io.IOException

/**
 * Durable licensed-audio transfer. WorkManager reschedules across process
 * death/reboot when constraints are met. Interrupted transfers restart cleanly
 * from byte zero (NOT a byte-range resume); partial files are never indexed.
 */
class CatalogDownloadWorker(
    context: Context,
    parameters: WorkerParameters
) : Worker(context, parameters) {
    @Volatile private var currentDownload: PermittedAudioDownloader? = null
    private val notificationId = 4000 + (id.hashCode() and 0x3fff)
    private val channelId = "nightwave_offline_downloads"

    override fun doWork(): Result {
        val candidate = FreeAudioCandidate(
            title = inputData.getString(CatalogDownloadQueue.KEY_TITLE).orEmpty(),
            sourceUrl = inputData.getString(CatalogDownloadQueue.KEY_URL).orEmpty(),
            descriptionUrl = inputData.getString(CatalogDownloadQueue.KEY_DESCRIPTION).orEmpty(),
            licenseName = inputData.getString(CatalogDownloadQueue.KEY_LICENSE).orEmpty(),
            bytes = inputData.getLong(CatalogDownloadQueue.KEY_BYTES, -1L),
            saveName = inputData.getString(CatalogDownloadQueue.KEY_NAME).orEmpty(),
            sha1Base36 = inputData.getString(CatalogDownloadQueue.KEY_SHA1).orEmpty()
        )
        if (!CatalogDownloadQueue.valid(candidate)) return failed("Invalid licensed audio source")
        if (isStopped) return failed("Download cancelled")
        // Transfer staging and private library import briefly hold two copies.
        // Fail before using bandwidth if there is not enough space for both.
        val headroom = candidate.bytes * 2L + (16L * 1024L * 1024L)
        if (runCatching {
                StatFs(applicationContext.filesDir.absolutePath).availableBytes < headroom
            }.getOrDefault(true)) return failed("Not enough private storage for offline audio")

        return try {
            ensureNotificationChannel()
            // A WorkManager retry is a clean start, not a byte-range resume.
            // Never carry a previous attempt's progress into a new transfer.
            setProgressAsync(Data.Builder()
                .putLong(CatalogDownloadQueue.KEY_RECEIVED, 0L)
                .putLong(CatalogDownloadQueue.KEY_TOTAL, -1L)
                .putBoolean(CatalogDownloadQueue.KEY_VERIFYING, false).build())
            setForegroundAsync(foreground(candidate.title, 0, null)).get()
            if (isStopped) return failed("Download cancelled")
            val source = PermittedAudioDownloadPolicy.prepare(
                candidate.sourceUrl, candidate.saveName)
            val transfer = PermittedAudioDownloader(applicationContext, retainOnStop = {
                if (Build.VERSION.SDK_INT >= 31)
                    stopReason != WorkInfo.STOP_REASON_CANCELLED_BY_APP &&
                        stopReason != WorkInfo.STOP_REASON_USER
                else AudioTransferData.legacyMayRetain(applicationContext, id)
            })
            currentDownload = transfer
            var lastUi = 0L
            var lastBytes = -1L
            var latestReceived = 0L
            var latestTotal = -1L
            val saved = transfer.download(
                source, candidate.sha1Base36, candidate.bytes,
                onVerifying = {
                    setProgressAsync(Data.Builder()
                        .putLong(CatalogDownloadQueue.KEY_RECEIVED, latestReceived)
                        .putLong(CatalogDownloadQueue.KEY_TOTAL, latestTotal)
                        .putBoolean(CatalogDownloadQueue.KEY_VERIFYING, true)
                        .putString(AudioTransferData.DETAILS, transfer.transferDetails())
                        .putLong(AudioTransferData.RESUMED, transfer.resumedBytes())
                        .build())
                    setForegroundAsync(foreground(candidate.title,
                        latestReceived, latestTotal.takeIf { it > 0L }, verifying = true))
                }
            ) { received, total ->
                latestReceived = received
                latestTotal = total ?: -1L
                if (isStopped) {
                    transfer.cancel()
                    throw IOException("Download cancelled")
                }
                val now = SystemClock.elapsedRealtime()
                if (now - lastUi >= 700L || received == total || lastBytes < 0 ||
                    received < lastBytes) {
                    lastUi = now
                    lastBytes = received
                    setProgressAsync(Data.Builder()
                        .putLong(CatalogDownloadQueue.KEY_RECEIVED, received)
                        .putLong(CatalogDownloadQueue.KEY_TOTAL, total ?: -1L)
                        .putBoolean(CatalogDownloadQueue.KEY_VERIFYING, false)
                        .putString(AudioTransferData.DETAILS, transfer.transferDetails())
                        .putLong(AudioTransferData.RESUMED, transfer.resumedBytes())
                        .build())
                    setForegroundAsync(foreground(candidate.title, received, total))
                }
            }
            // The staged copy and index commit have completed; re-read the
            // actual private file before WorkManager declares SUCCESS.
            val verified = OfflineAudioLibrary(applicationContext)
                .availableIds(setOf(saved.id))
            if (!OfflineTransferCompletionPolicy.canCommit(isStopped, saved.id, verified)) {
                failed(OfflineTransferCompletionPolicy.NOT_VERIFIED)
            } else Result.success(Data.Builder()
                .putString(CatalogDownloadQueue.KEY_DONE, saved.id)
                    .apply { AudioTransferData.putCompletion(this, transfer.completedTransfer()) }
                    .putString(AudioTransferData.DETAILS, transfer.transferDetails())
                .putString(CatalogDownloadQueue.KEY_SAVED_TITLE, saved.title)
                .build())
        } catch (error: Exception) {
            if (isStopped) failed("Download cancelled")
            else if (error is IOException && runAttemptCount < 2) Result.retry()
            else failed(DownloadRecoveryPolicy.failedMessage(error.message))
        } finally {
            currentDownload = null
        }
    }

    override fun onStopped() {
        currentDownload?.cancel()
        super.onStopped()
    }

    private fun failed(reason: String): Result = Result.failure(
        Data.Builder().putString(CatalogDownloadQueue.KEY_ERROR, reason)
            .putString(AudioTransferData.DETAILS, currentDownload?.transferDetails().orEmpty()).build()
    )

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
            manager.createNotificationChannel(NotificationChannel(
                channelId, "NightWave offline downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Progress for user-selected offline audio downloads"
                setShowBadge(false)
            })
        }
    }

    private fun foreground(title: String, downloaded: Long, total: Long?, verifying: Boolean = false): ForegroundInfo {
        val safeTitle = title.take(65).ifBlank { "Audio file" }
        val manager = WorkManager.getInstance(applicationContext)
        val click = Intent(applicationContext, CatalogDownloadsActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val open = PendingIntent.getActivity(applicationContext,
            notificationId, click, PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE)
        val hasTotal = total != null && total > 0
        val percent = total?.takeIf { it > 0L }?.let {
            ((downloaded * 100L) / it).toInt().coerceIn(0, 100)
        } ?: 0
        val notification: Notification = NotificationCompat.Builder(
            applicationContext, channelId
        )
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("NightWave · Offline download")
            .setContentText(if (verifying) "Checking audio before saving" else safeTitle)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, percent, verifying || !hasTotal)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel,
                "Cancel", manager.createCancelPendingIntent(id))
            .build()
        return if (Build.VERSION.SDK_INT >= 29) {
            ForegroundInfo(notificationId, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else ForegroundInfo(notificationId, notification)
    }
}
