package com.nightwave.player.v2.ui

import android.content.Context
import com.nightwave.player.v2.model.NightWaveDesign
import com.nightwave.player.v2.model.NightWaveTheme

/** Read-only lookup: never invokes profile creation, consent, migration or any writer. */
object NightWaveNativeMatte {
    private val palettes = NightWaveTheme.entries.associateWith(NightWaveDesign::palette)
    fun selectedTheme(context: Context): NightWaveTheme {
        val preferences = context.getSharedPreferences("nightwave_v2_guest_profile_1", Context.MODE_PRIVATE)
        val raw = runCatching { preferences.getString("theme", null) }.getOrNull()
        return NightWaveTheme.entries.firstOrNull { it.name == raw } ?: NightWaveTheme.MIDNIGHT
    }
    fun palette(context: Context) = palettes.getValue(selectedTheme(context))
    fun applyNativeTheme(context: Context) {
        context.setTheme(if (palette(context).isDark) com.nightwave.player.R.style.Theme_NightWave
            else com.nightwave.player.R.style.Theme_NightWave_Pearl)
    }
}

