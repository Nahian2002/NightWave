package com.nightwave.player.net

/**
 * NW81 standalone smoke. VideoPreview below is a compile-only stub for the
 * production YouTubeGateway model; this tools file is not part of app sources.
 *
 * From a local repo checkout:
 * kotlinc app/src/main/java/com/nightwave/player/net/MusicIdentity.kt \
 *   tools/NW81TitleSmoke.kt -include-runtime -d nw81-title.jar
 * java -cp nw81-title.jar com.nightwave.player.net.NW81TitleSmoke
 *
 * This tests only title/creator presentation rules, NOT Android UI or network.
 */
data class VideoPreview(
    val title: String,
    val author: String,
    val url: String,
    val thumbnail: String,
    val durationSeconds: Long
)

object NW81TitleSmoke {
    private fun expect(actual: MusicDisplayIdentity, expected: MusicDisplayIdentity) =
        check(actual == expected) { "Expected $expected but got $actual" }

    @JvmStatic fun main(args: Array<String>) {
        expect(MusicIdentity.display(
            "Ed Sheeran - Shape of You (Official Music Video)", "Ed Sheeran - Topic"),
            MusicDisplayIdentity("Shape of You", "Ed Sheeran"))
        expect(MusicIdentity.display(
            "Song - [Official Video] (Official Lyric Video) [Official Visualizer]",
            "Publisher"),
            MusicDisplayIdentity("Song", "Publisher"))
        expect(MusicIdentity.display(
            "Song (Acoustic) [Official Audio]", "Publisher"),
            MusicDisplayIdentity("Song (Acoustic)", "Publisher"))
        expect(MusicIdentity.display(
            "Artist - Song (Live Remix)", "Unrelated publisher"),
            MusicDisplayIdentity("Artist - Song (Live Remix)", "Unrelated publisher"))
        expect(MusicIdentity.display("মালা - গান (Official Audio)", "মেলা - Topic"),
            MusicDisplayIdentity("মালা - গান", "মেলা - Topic"))
        expect(MusicIdentity.display("মালা - গান (Official Audio)", "মালা - Topic"),
            MusicDisplayIdentity("গান", "মালা"))
        val original = "Ed Sheeran - Shape of You [Official Video]"
        expect(MusicIdentity.videoDisplay(original, "Ed Sheeran"),
            MusicDisplayIdentity(original, "Ed Sheeran"))
        val entry = VideoPreview(original, "Ed Sheeran",
            "https://www.youtube.com/watch?v=12345678901", "", 100L)
        check(MusicIdentity.display(entry.title, entry.author).title == "Shape of You")
        check(entry.title == original)
        check(entry.url.endsWith("12345678901"))
        println("NW81_TITLE_PRESENTATION_SMOKE_PASS")
    }
}
