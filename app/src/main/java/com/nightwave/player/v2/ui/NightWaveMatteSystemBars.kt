package com.nightwave.player.v2.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.core.view.WindowCompat
import com.nightwave.player.v2.model.Palette

/** Android26-34 still require legacy bar colors; Android35+ draws the window canvas edge to edge. */
internal object NightWaveMatteSystemBars {
    @Suppress("DEPRECATION") // Only the compatibility setters below; no lint finding/baseline is suppressed.
    fun apply(context: Context, palette: Palette) {
        var current = context
        repeat(8) {
            if (current is Activity) {
                val activity = current as Activity
                val window = activity.window
                window.setBackgroundDrawable(ColorDrawable(palette.background.toInt()))
                if (Build.VERSION.SDK_INT < 35) {
                    window.statusBarColor = palette.background.toInt()
                    window.navigationBarColor = palette.background.toInt()
                }
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !palette.isDark
                    isAppearanceLightNavigationBars = !palette.isDark
                }
                return
            }
            current = (current as? ContextWrapper)?.baseContext ?: return
        }
    }
}
