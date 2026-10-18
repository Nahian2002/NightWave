package com.nightwave.player.v2.ui

/**
 * Pure design/interaction policy for the NightWave 6V7 premium shell.
 * Do not move Media3 or persistence decisions into visual layout policies.
 */
object NightWaveChromePolicy {
    const val NAV_ITEM_COUNT = 4
    const val NAV_HEIGHT_DP = 66
    const val NAV_MIN_TOUCH_DP = 56
    const val MINI_NEXT_MIN_WIDTH_DP = 365f

    fun greeting(hour: Int): String {
        require(hour in 0..23) { "Local hour must be 0–23" }
        return when (hour) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    fun displayFirstName(displayName: String): String {
        val clean = displayName.trim().replace(Regex("""\s+"""), " ")
        return clean.substringBefore(' ').take(22)
            .ifBlank { "your space" }
    }

    fun showMiniNext(queueCount: Int, availableWidthDp: Float): Boolean =
        queueCount > 1 && availableWidthDp.isFinite() &&
            availableWidthDp >= MINI_NEXT_MIN_WIDTH_DP

    fun selectedTab(index: Int): Int = index.coerceIn(0, NAV_ITEM_COUNT - 1)
}
