package com.nightwave.player.v2.ui

/** Layout only. Minimum heights never cap intrinsic text or reduce artwork/touch targets. */
internal object NightWaveSecondaryLayoutPolicy {
    const val SETTINGS_MIN_DP = 56
    const val SETTINGS_VERTICAL_DP = 6
    const val GROUP_CAPTION_BOTTOM_DP = 4
    const val MIX_VERTICAL_DP = 8
    const val MIX_CAPTION_DP = 12
    const val QUEUE_GAP_DP = 4
    const val QUEUE_SUMMARY_BOTTOM_DP = 4
    const val SOUND_PADDING_DP = 12
    const val SOUND_GAP_DP = 8
    const val STATUS_GAP_DP = 4

    fun stackSettingAction(availableWidthDp: Float, fontScale: Float): Boolean =
        !availableWidthDp.isFinite() || availableWidthDp < 320f ||
            (fontScale.isFinite() && fontScale >= 1.3f)
}
