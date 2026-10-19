package com.nightwave.player.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Only NightWave-created immutable PendingIntents can reach this private
 * receiver. The exported system AppWidgetProvider never handles transport.
 */
class NightWaveWidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (!NightWaveWidgetController.accepted(action)) return
        val pending = goAsync()
        NightWaveWidgetController.perform(context, action!!) { pending.finish() }
    }
}
