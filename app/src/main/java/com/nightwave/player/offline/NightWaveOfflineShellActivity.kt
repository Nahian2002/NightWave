package com.nightwave.player.offline

import android.content.Intent
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.MotionEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.viewinterop.AndroidView
import com.nightwave.player.v2.data.GuestProfileStore
import com.nightwave.player.v2.model.MotionPreference
import com.nightwave.player.v2.model.ProfileSettings
import com.nightwave.player.v2.playback.StudioPlaybackBridge
import com.nightwave.player.v2.ui.NightWaveMaterialTheme
import com.nightwave.player.v2.ui.NightWaveOfflineShell
import com.nightwave.player.R

/** A paged native panel must retain access to its controls in a short shell. */
internal fun createOfflineViewport(context: Context, content: View): ScrollView =
    ScrollView(context).apply {
        isFillViewport = true
        addView(content, ViewGroup.LayoutParams(-1, -2))
    }

/** Incremental View migration: native content/workers survive chrome changes.
 * Recovery is an explicit in-place switch, without recreation or data reset.
 */
abstract class NightWaveOfflineShellActivity : ComponentActivity() {
    protected abstract val offlineRoute: NightWaveOfflineRoute
    private var bridge: StudioPlaybackBridge? = null
    private var nativeRecovery = false
    private var profile by mutableStateOf(ProfileSettings("offline-shell"))
    private var nativeContent: View? = null
    private var legacyHeader: View? = null
    private var viewport: ScrollView? = null
    private var restoredViewport: Int? = null
    private val viewportLayoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> restoreViewport() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nativeRecovery = savedInstanceState?.getBoolean("nw128-recovery")
            ?: intent.getBooleanExtra(NightWaveOfflineNavigator.EXTRA_RECOVERY, false)
        restoredViewport = savedInstanceState?.getInt("nw128-viewport")
            ?.let(NightWaveOfflineNavigationPolicy::scroll)
    }

    protected fun setOfflineContent(view: View, header: View? = null) {
        nativeContent = view
        legacyHeader = header
        installContent()
    }

    private fun installContent() {
        val content = nativeContent ?: return
        viewport?.let { restoredViewport = it.scrollY }
        legacyHeader?.visibility = if (nativeRecovery) View.VISIBLE else View.GONE
        (content.parent as? ViewGroup)?.removeView(content)
        content.removeOnLayoutChangeListener(viewportLayoutListener)
        // Compose owns all insets in the normal shell, exactly once.
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(content, null)
        content.setPadding(0, 0, 0, 0)
        if (nativeRecovery) {
            viewport = null
            bridge?.pauseUiUpdates()
            val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            panel.addView(Button(this).apply {
                text = getString(R.string.nw128_restore_navigation)
                setOnClickListener { nativeRecovery = false; installContent() }
            })
            panel.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
            setContentView(panel)
            NightWaveSafeInsets.bind(panel)
            return
        }
        val player = bridge ?: StudioPlaybackBridge(this).also { bridge = it }
        // The shell takes vertical space. Scroll the entire retained panel,
        // including its controls, rather than starving the weighted song list
        // on short windows. The original paged row bounds remain unchanged.
        val body = createOfflineViewport(this, content)
        viewport = body
        body.addOnLayoutChangeListener(viewportLayoutListener)
        content.addOnLayoutChangeListener(viewportLayoutListener)
        profile = GuestProfileStore(this).load()
        setContent {
            NightWaveMaterialTheme(profile.theme, profile.motion == MotionPreference.REDUCED) {
                NightWaveOfflineShell(offlineRoute, player.state, player,
                    reducedMotion = profile.motion == MotionPreference.REDUCED,
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    onRoute = { NightWaveOfflineNavigator.open(this, it) },
                    onMain = { tab, expanded -> NightWaveOfflineNavigator.main(this, tab, expanded) },
                    onRecovery = { nativeRecovery = true; installContent() }) {
                    AndroidView(factory = { body }, modifier = Modifier.fillMaxSize())
                }
            }
        }
        player.resumeUiUpdates()
    }

    private fun restoreViewport() {
        val target = restoredViewport ?: return
        val scroll = viewport ?: return
        scroll.post {
            if (!isFinishing && !isDestroyed && viewport === scroll && restoredViewport == target) {
                scroll.scrollTo(0, target)
                // A first paint can precede the asynchronous library/jobs.
                // Keep the offset until enough rows exist, or the user scrolls.
                if (scroll.scrollY == target) restoredViewport = null
            }
        }
    }

    protected fun resetOfflineViewport() {
        restoredViewport = null
        viewport?.scrollTo(0, 0)
    }

    override fun startActivity(intent: Intent, options: Bundle?) {
        super.startActivity(NightWaveOfflineNavigator.reuseOfflineIntent(intent), options)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) restoredViewport = null
        return super.dispatchTouchEvent(event)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.hasExtra(NightWaveOfflineNavigator.EXTRA_RECOVERY)) {
            nativeRecovery = intent.getBooleanExtra(NightWaveOfflineNavigator.EXTRA_RECOVERY, false)
            installContent()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!nativeRecovery) {
            profile = GuestProfileStore(this).load()
            bridge?.resumeUiUpdates()
        }
    }

    override fun onStop() { bridge?.pauseUiUpdates(); super.onStop() }
    override fun onDestroy() {
        bridge?.release()
        bridge = null
        nativeContent?.removeOnLayoutChangeListener(viewportLayoutListener)
        viewport?.removeOnLayoutChangeListener(viewportLayoutListener)
        viewport = null
        nativeContent = null
        super.onDestroy()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("nw128-recovery", nativeRecovery)
        outState.putInt("nw128-viewport", viewport?.scrollY ?: restoredViewport ?: 0)
        super.onSaveInstanceState(outState)
    }
}
