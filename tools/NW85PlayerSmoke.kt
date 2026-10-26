package com.nightwave.player.v2.ui

/**
 * NW85 standalone JVM-only player presentation check.
 *
 * kotlinc app/src/main/java/com/nightwave/player/v2/ui/NightWaveNowPlayingPresentationPolicy.kt \
 *   tools/NW85PlayerSmoke.kt -include-runtime -d nw85-player-smoke.jar
 * java -cp nw85-player-smoke.jar com.nightwave.player.v2.ui.NW85PlayerSmoke
 *
 * No Android SDK, Compose runtime, playback service or network access needed.
 */
object NW85PlayerSmoke {
    @JvmStatic fun main(args: Array<String>) {
        val p = NightWaveNowPlayingPresentationPolicy
        val urls = listOf("a", "b", "c")
        val names = listOf("Alo Alo", "Moonlight", "Night Drive")
        check(p.queuePreview(urls, names, 0, emptyList(), false).subtitle == "Moonlight")
        check(p.queuePreview(urls, names, 0, listOf("c"), false).sourceSlot == 2)
        check(p.queuePreview(urls, names, 0, emptyList(), true).sourceSlot == null)
        check(p.queuePreview(urls, names, 0, emptyList(), true).subtitle ==
            "2 possible next tracks")
        check(p.queuePreview(urls, names, 2, emptyList(), false, repeatAll = true).sourceSlot == 0)
        check(p.queuePreview(urls, names, 0, listOf("c"), false, repeatOne = true).heading ==
            "Repeat one")
        check(p.queuePreview(urls, names, -1, emptyList(), false).sourceSlot == null)
        check(p.offlineActionLabel(true, false) == "Offline options")
        check(p.offlineActionLabel(false, false) == "Choose a song")
        check(p.lyricsSubtitle(true, true) == "Your synchronized lyrics · ready")
        check(p.titleLines(1.3f, 390f) == 3)
        check(p.artistLines(1.6f) == 2)
        println("NW85_PRESENTATION_POLICY_SMOKE_PASS")
    }
}
