package com.nightwave.player.v2.ui

/**
 * NW83 exact production pure-Kotlin smoke. No Android runtime.
 *
 * kotlinc app/src/main/java/com/nightwave/player/v2/ui/NightWaveChromePolicy.kt \
 *   app/src/main/java/com/nightwave/player/v2/ui/NightWaveAdaptiveChromePolicy.kt \
 *   app/src/main/java/com/nightwave/player/v2/ui/NightWavePremiumDesignTokens.kt \
 *   app/src/main/java/com/nightwave/player/v2/ui/NightWaveMotionPolicy.kt \
 *   tools/NW83VisualTokensSmoke.kt -include-runtime -d nw83-visual.jar
 * java -cp nw83-visual.jar com.nightwave.player.v2.ui.NW83VisualTokensSmoke
 *
 * Does not certify Compose rendering or an Android/Gradle build.
 */
object NW83VisualTokensSmoke {
    @JvmStatic fun main(args: Array<String>) {
        val d = NightWavePremiumDesignTokens
        check(d.pageGutterDp(320f) == 14)
        check(d.pageGutterDp(390f) == 20)
        check(d.pageGutterDp(900f) == 28)
        check(d.pageGutterDp(Float.NaN) == 14)
        check(d.rowMinHeightDp(1f) == 72)
        check(d.rowMinHeightDp(1.3f) == 88)
        check(d.rowMinHeightDp(1.8f) == 108)
        check(d.titleLines(1f, 320f) == 1)
        check(d.titleLines(1f, 430f) == 2)
        check(d.titleLines(1.4f, 320f) == 2)
        check(d.MIN_TOUCH_TARGET_DP >= 48)
        check(d.controlTouchTargetDp(2f) >= 56)
        check(d.rowBorderAlpha(true) > d.rowBorderAlpha(false))
        check(NightWaveMotionPolicy.duration(true, 280) == 0)
        check(NightWaveMotionPolicy.duration(false, 280) == 280)
        check(NightWaveAdaptiveChromePolicy.navHeightDp(1.3f) == 72)
        println("NW83_PREMIUM_VISUAL_TOKENS_SMOKE_PASS")
    }
}
