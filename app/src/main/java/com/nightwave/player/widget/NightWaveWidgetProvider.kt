package com.nightwave.player.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.nightwave.player.R
import com.nightwave.player.v2.ui.NightWaveStudioActivity

/**
 * Launcher widget uses only Android RemoteViews; no Compose or online artwork.
 * Receivers never parse or accept media URLs or file IDs from external Intent data.
 */
class NightWaveWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (NightWaveWidgetController.accepted(action)) {
            val pending = goAsync()
            NightWaveWidgetController.perform(context, action!!) {
                pending.finish()
            }
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context, manager: AppWidgetManager, appWidgetIds: IntArray
    ) {
        val snapshot = NowPlayingWidgetStore(context).read()
        appWidgetIds.forEach { id -> manager.updateAppWidget(id, layout(context, snapshot)) }
    }

    companion object {
        /**
         * Called on playback-state/track transitions, never once per progress tick.
         * Do not write widget cache if the user has not installed any widgets.
         */
        fun publish(context: Context, state: WidgetPlaybackSnapshot) {
            runCatching {
                val app = context.applicationContext
                val manager = AppWidgetManager.getInstance(app)
                val ids = manager.getAppWidgetIds(
                    ComponentName(app, NightWaveWidgetProvider::class.java))
                if (ids.isEmpty()) return
                val safe = NowPlayingWidgetPolicy.normalized(state)
                NowPlayingWidgetStore(app).write(safe)
                ids.forEach { id -> manager.updateAppWidget(id, layout(app, safe)) }
            }
        }

        private fun actionIntent(context: Context, action: String, requestCode: Int) =
            PendingIntent.getBroadcast(
                context, requestCode,
                Intent(context, NightWaveWidgetProvider::class.java).apply {
                    this.action = action
                    addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        private fun layout(context: Context, snapshot: WidgetPlaybackSnapshot): RemoteViews {
            val safe = NowPlayingWidgetPolicy.normalized(snapshot)
            return RemoteViews(context.packageName, R.layout.nightwave_playback_widget).apply {
                setTextViewText(R.id.widget_track, NowPlayingWidgetPolicy.title(safe))
                setTextViewText(R.id.widget_artist, NowPlayingWidgetPolicy.subtitle(safe))
                setImageViewResource(R.id.widget_play_pause,
                    if (safe.isPlaying) R.drawable.ic_nw_widget_pause
                    else R.drawable.ic_nw_widget_play)
                setContentDescription(R.id.widget_play_pause,
                    NowPlayingWidgetPolicy.playbackDescription(safe))
                setOnClickPendingIntent(R.id.widget_open, PendingIntent.getActivity(
                    context, 0,
                    Intent(context, NightWaveStudioActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                val previous = if (safe.hasTrack)
                    actionIntent(context, NightWaveWidgetController.ACTION_PREVIOUS, 41) else null
                val toggle = if (safe.hasTrack)
                    actionIntent(context, NightWaveWidgetController.ACTION_TOGGLE, 42) else null
                val next = if (safe.hasTrack)
                    actionIntent(context, NightWaveWidgetController.ACTION_NEXT, 43) else null
                setOnClickPendingIntent(R.id.widget_previous, previous)
                setOnClickPendingIntent(R.id.widget_play_pause, toggle)
                setOnClickPendingIntent(R.id.widget_next, next)
            }
        }
    }
}
