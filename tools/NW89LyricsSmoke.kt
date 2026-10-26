package com.nightwave.player.v2.lyrics

/**
 * Standalone source-only Kotlin/JVM regression smoke.
 *
 * kotlinc app/src/main/java/com/nightwave/player/v2/lyrics/TimedLyrics.kt \
 *   app/src/main/java/com/nightwave/player/v2/lyrics/LyricsSyncPolicy.kt \
 *   app/src/main/java/com/nightwave/player/v2/lyrics/LyricsPresentationPolicy.kt \
 *   app/src/main/java/com/nightwave/player/v2/lyrics/LyricsReadingPolicy.kt \
 *   app/src/main/java/com/nightwave/player/v2/lyrics/LyricsImportReview.kt \
 *   tools/NW89LyricsSmoke.kt -include-runtime -d nw89-lyrics.jar
 * java -cp nw89-lyrics.jar com.nightwave.player.v2.lyrics.NW89LyricsSmoke
 *
 * No Android SDK, network, microphone, platform OCR or remote lyrics provider.
 */
object NW89LyricsSmoke {
    @JvmStatic fun main(args: Array<String>) {
        fun parse(text: String) =
            TimedLyricsCodec.parse(text.toByteArray(Charsets.UTF_8))
        val lyrics = parse("[00:01.00]zebra\n[00:01.00]apple\n[00:05.00]শেষ লাইন")
        check(lyrics.cues.map { it.text } == listOf("zebra", "apple", "শেষ লাইন"))
        check(lyrics.activeIndex(1000L) == 1)
        check(LyricsPresentationPolicy.lineProgress(lyrics, 1, 3_000L, 0) == .5f)
        check(LyricsReadingPolicy.shouldAnimateScroll(5, 12, false))
        check(!LyricsReadingPolicy.shouldAnimateScroll(5, 100, false))
        check(!LyricsReadingPolicy.shouldAnimateScroll(5, 6, true))
        check(LyricsReadingPolicy.cueDescription(
            LyricCue(10_000L, "আলো"), 500, 30_000L)
            .startsWith("Seek to 0 minutes 10 seconds. আলো"))
        check(LyricsImportReviewPolicy.normalizedName("মালা") !=
            LyricsImportReviewPolicy.normalizedName("মেলা"))
        check(LyricsImportReviewPolicy.compare("Moonlighter.lrc", "Moonlight",
            "Moonlighter") == LyricsImportMatch.DIFFERENT)
        val large = buildString {
            for (i in 0 until 1000) append(
                "[%02d:%02d.00]line%d\n".format(java.util.Locale.ROOT,
                    i / 60, i % 60, i))
        }
        val many = parse(large)
        check(many.cues.size == 1000)
        check(LyricsPresentationPolicy.lineProgress(many, 500, 500_500L, 0) == .5f)
        println("NW89_LYRICS_ACCESSIBILITY_PERFORMANCE_SMOKE_PASS")
    }
}
