package com.nightwave.player.v2.ui

/** Executable tests of exact pure Kotlin sizing policies, no Android SDK. */
object NW25AdaptiveChromeSmoke {
    @JvmStatic
    fun main(args: Array<String>) {
        val p = NightWaveAdaptiveChromePolicy
        val art = p.artworkSizeDp(320f, 640f, 1f)
        val brokenInputs = listOf(Float.NaN, Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY, -100f, 0f)
        val cases = listOf(
            p.navHeightDp(1f) == 60,
            p.navHeightDp(1.3f) == 72,
            p.navHeightDp(1.6f) == 84,
            p.navHeightDp(Float.NaN) == 60,
            p.homeHeroHeightDp(1f) == 96,
            p.homeHeroHeightDp(1.3f) == 132,
            p.homeHeroHeightDp(1.6f) == 180,
            p.miniPlayerHeightDp(1f) == 52,
            p.miniPlayerHeightDp(1.3f) == 64,
            p.miniPlayerHeightDp(1.6f) == 78,
            p.horizontalPaddingDp(320f) == 14,
            p.horizontalPaddingDp(390f) == 20,
            p.horizontalPaddingDp(900f) == 28,
            art in 1f..320f,
            p.artworkSizeDp(28f, 800f, 1f) == 0f,
            p.artworkSizeDp(360f, 300f, 1f) <= 102f,
            p.artworkSizeDp(600f, 1000f, 2f) <= 260f,
            p.artworkSizeDp(1440f, 1600f, 1f) == 360f,
            brokenInputs.all { x -> p.artworkSizeDp(x, x, 1f).isFinite() },
            brokenInputs.all { x -> p.artworkSizeDp(x, x, 1f) >= 0f },
            p.nativeRowHeightDp(1f) == 72,
            p.nativeRowHeightDp(1.3f) == 82,
            p.nativeRowHeightDp(1.6f) == 96,
            p.motionDurationMs(true, 200) == 0,
            p.motionDurationMs(false, 200) == 200,
            p.motionDurationMs(false, 1000) == 600,
            p.motionDurationMs(false, -1) == 0,
            NightWaveChromePolicy.selectedTab(100) == 3
        )
        cases.forEachIndexed { index, value ->
            check(value) { "NW25 adaptive UI policy assertion " + (index + 1) + " failed" }
        }
        println("PASS: NW25 " + cases.size + " exact-production Kotlin adaptive chrome assertions")
        println("LIMIT: Android Compose, native layout and device screenshots not compiled")
    }
}
