package com.nightwave.player.offline

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Applies cutout, system/navigation bar and keyboard safe areas once.
 * Preserves the view's authored padding, avoids cumulative insets during
 * rotation, and respects gesture navigation / Android 15 edge-to-edge.
 */
object NightWaveSafeInsets {
    fun bind(view: View) {
        val left = view.paddingLeft
        val top = view.paddingTop
        val right = view.paddingRight
        val bottom = view.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(view) { current, insets ->
            val edges = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout() or
                    WindowInsetsCompat.Type.ime()
            )
            current.setPadding(
                left + edges.left, top + edges.top,
                right + edges.right, bottom + edges.bottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(view)
    }
}
