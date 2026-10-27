package com.nightwave.player.offline

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nightwave.player.v2.ui.NightWaveStudioActivity
import java.util.UUID

object NightWaveOfflineNavigator {
    const val ACTION_MAIN = "com.nightwave.player.OPEN_MAIN_SHELL"
    const val EXTRA_REQUEST = "nw128-main-request"
    const val EXTRA_TAB = "nw128-main-tab"
    const val EXTRA_PLAYER = "nw128-main-player"
    const val EXTRA_RECOVERY = "nw128-native-recovery"

    /** Reuse each private offline screen; never clear a live foreground transfer
     * simply to switch sibling routes. All unrelated/external intents pass through.
     */
    fun reuseOfflineIntent(intent: Intent): Intent {
        val component = intent.component ?: return intent
        if (component.packageName != "com.nightwave.player" ||
            NightWaveOfflineNavigationPolicy.route(component.className) == null) return intent
        return Intent(intent).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }

    fun open(context: Context, route: NightWaveOfflineRoute) {
        context.startActivity(reuseOfflineIntent(Intent().setClassName(context, route.componentName)))
    }

    /** Deliberately leave the offline branch, returning to the existing main
     * shell rather than piling a second main Activity above the branch.
     * This is navigation only: no PLAY action, service start or queue rewrite.
     */
    fun main(activity: Activity, tab: Int, openPlayer: Boolean = false) {
        require(tab in 0..3)
        val base = runCatching {
            (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .appTasks.firstOrNull {
                    val info = it.taskInfo
                    @Suppress("DEPRECATION") // RecentTaskInfo.id is the API 26–28 field.
                    val id = if (Build.VERSION.SDK_INT >= 29) info.taskId else info.id
                    id == activity.taskId
                }?.taskInfo?.baseActivity?.className
        }.getOrNull()
        val flags = if (NightWaveOfflineNavigationPolicy.needsMainRoot(base))
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        else Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        activity.startActivity(Intent(activity, NightWaveStudioActivity::class.java)
            .setAction(ACTION_MAIN)
            .putExtra(EXTRA_REQUEST, UUID.randomUUID().toString())
            .putExtra(EXTRA_TAB, tab).putExtra(EXTRA_PLAYER, openPlayer)
            .addFlags(flags))
        // If launched from a legacy entry with no main shell below, do not
        // leave this screen directly underneath the new main shell.
        activity.finish()
    }

    fun request(intent: Intent?, lastApplied: String?): NightWaveMainNavigationRequest? =
        if (intent?.action != ACTION_MAIN) null else runCatching {
            NightWaveOfflineNavigationPolicy.mainRequest(intent.getStringExtra(EXTRA_REQUEST),
                intent.getIntExtra(EXTRA_TAB, -1), intent.getBooleanExtra(EXTRA_PLAYER, false), lastApplied)
        }.getOrNull()
}
