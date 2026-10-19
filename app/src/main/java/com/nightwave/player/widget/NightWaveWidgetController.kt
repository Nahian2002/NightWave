package com.nightwave.player.widget

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.nightwave.player.playback.PlaybackService
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Widget touches an existing Media3 session through its supported controller.
 * No startForegroundService(), no launch from background, no new playback engine.
 * Async receiver is always finished, including disconnect and timeout paths.
 */
object NightWaveWidgetController {
    const val ACTION_TOGGLE = "com.nightwave.player.widget.TOGGLE"
    const val ACTION_NEXT = "com.nightwave.player.widget.NEXT"
    const val ACTION_PREVIOUS = "com.nightwave.player.widget.PREVIOUS"
    const val COMMAND_NEXT = "com.nightwave.player.widget.SESSION_NEXT"
    const val COMMAND_PREVIOUS = "com.nightwave.player.widget.SESSION_PREVIOUS"

    fun accepted(action: String?): Boolean = action == ACTION_TOGGLE ||
        action == ACTION_NEXT || action == ACTION_PREVIOUS

    fun perform(context: Context, action: String, finished: () -> Unit) {
        if (!accepted(action)) {
            finished()
            return
        }
        val app = context.applicationContext
        val main = Handler(Looper.getMainLooper())
        val executor = Executor { runnable -> main.post(runnable) }
        val completed = AtomicBoolean(false)
        var controller: MediaController? = null
        var future: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
        lateinit var timeout: Runnable
        fun finish() {
            if (!completed.compareAndSet(false, true)) return
            main.removeCallbacks(timeout)
            // Also handles cancellation before the async controller connects.
            // Media3 owns the future's lifecycle, including delayed binder setup.
            val pendingFuture = future
            if (pendingFuture != null)
                runCatching { MediaController.releaseFuture(pendingFuture) }
            else runCatching { controller?.release() }
            runCatching { finished() }
        }
        timeout = Runnable {
            runCatching { future?.cancel(true) }
            finish()
        }
        main.postDelayed(timeout, 7_000L)
        try {
            val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
            future = MediaController.Builder(app, token).buildAsync()
            future?.addListener(Runnable {
                if (completed.get()) return@Runnable
                try {
                    val connected = future?.get() ?: return@Runnable finish()
                    controller = connected
                    when (action) {
                        ACTION_TOGGLE -> {
                            // A widget must not silently resurrect a saved queue.
                            // When no media item is active, the launcher opens NightWave.
                            if (connected.currentMediaItem != null) {
                                if (connected.isPlaying) connected.pause()
                                else if (connected.isCommandAvailable(Player.COMMAND_PLAY_PAUSE))
                                    connected.play()
                            }
                            // Let the just-sent transport command reach the session
                            // before disconnecting the short-lived widget controller.
                            main.postDelayed({ finish() }, 180L)
                        }
                        ACTION_NEXT, ACTION_PREVIOUS -> {
                            val command = if (action == ACTION_NEXT)
                                COMMAND_NEXT else COMMAND_PREVIOUS
                            val result = connected.sendCustomCommand(
                                SessionCommand(command, Bundle.EMPTY), Bundle.EMPTY)
                            result.addListener(Runnable { finish() }, executor)
                        }
                    }
                } catch (error: Exception) {
                    Log.w("NightWaveWidget", "Widget control unavailable", error)
                    finish()
                }
            }, executor)
        } catch (error: Exception) {
            Log.w("NightWaveWidget", "Widget session unavailable", error)
            finish()
        }
    }
}
