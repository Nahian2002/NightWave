package com.nightwave.player.playback

/**
 * Exact-production-policy JVM smoke; no Media3, Android, audio privileges or
 * user files. From the repository root:
 *
 * kotlinc app/src/main/java/com/nightwave/player/playback/EqualizerReadbackPolicy.kt \
 *   app/src/main/java/com/nightwave/player/playback/CrossfadePolicy.kt \
 *   app/src/main/java/com/nightwave/player/playback/OfflineNormalizationPolicy.kt \
 *   app/src/main/java/com/nightwave/player/playback/AudioTransitionUiPolicy.kt \
 *   tools/NW88AudioSmoke.kt -include-runtime -d nw88-audio.jar
 * java -cp nw88-audio.jar com.nightwave.player.playback.NW88AudioSmoke
 */
object NW88AudioSmoke {
    @JvmStatic fun main(args: Array<String>) {
        val eq = EqualizerReadbackPolicy
        check(eq.inspect(listOf(-250, 200), listOf(-250, 200)) ==
            EqualizerReadbackPolicy.Result.VERIFIED)
        check(eq.inspect(listOf(-275, 200), listOf(-250, 200)) ==
            EqualizerReadbackPolicy.Result.DEVICE_QUANTIZED)
        check(eq.inspect(listOf(300), listOf(0)) ==
            EqualizerReadbackPolicy.Result.MISMATCH)
        check(eq.inspect(listOf(0, 0), listOf(0)) ==
            EqualizerReadbackPolicy.Result.INVALID)
        check(CrossfadePolicy.DEFAULT_SECONDS == 0)
        check(CrossfadePolicy.accepts(10) && !CrossfadePolicy.accepts(11))
        val fade = CrossfadePolicy.gains(.5f, .7f)
        check(kotlin.math.abs(fade.first + fade.second - .7f) < 0.0001f)
        check(CrossfadePolicy.canPrepare(5, true, true, true, 0, 2, 20_000, 17_000))
        check(!CrossfadePolicy.canPrepare(5, false, true, true, 0, 2, 20_000, 17_000))
        check(OfflineNormalizationPolicy.attenuation(.5f, .8f)!! <= 1f)
        check(OfflineNormalizationPolicy.combined(.8f, .5f) <= .4f)
        check(AudioTransitionUiPolicy.description(
            false, 5, false, false, true).contains("Streaming"))
        check(AudioTransitionUiPolicy.description(
            true, 5, true, false, true).contains("Shuffle"))
        println("NW88_SOUND_STUDIO_AUDIO_POLICIES_SMOKE_PASS")
    }
}
