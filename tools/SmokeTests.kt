import com.nightwave.player.core.TimeLabel
import com.nightwave.player.core.VideoLink
import com.nightwave.player.core.SearchInput
import com.nightwave.player.core.SearchInputResolver

fun main() {
    fun expect(actual: String?, expected: String?) { check(actual == expected) { "Got $actual, expected $expected" } }
    val known = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    val good = listOf(
        known,
        "https://youtube.com/watch?t=11&v=dQw4w9WgXcQ",
        "https://m.youtube.com/watch?v=dQw4w9WgXcQ",
        "https://music.youtube.com/watch?v=dQw4w9WgXcQ",
        "https://youtu.be/dQw4w9WgXcQ?t=2",
        "youtu.be/dQw4w9WgXcQ",
        "https://youtube.com/shorts/dQw4w9WgXcQ",
        "https://youtube.com/live/dQw4w9WgXcQ",
        "https://youtube.com/embed/dQw4w9WgXcQ",
        "A video to listen to: https://youtu.be/dQw4w9WgXcQ thanks"
    )
    good.forEach { expect(VideoLink.normalize(it), known) }
    val bad = listOf("", "music", "https://youtube.com.evil.com/watch?v=dQw4w9WgXcQ",
        "https://notyoutube.com/watch?v=dQw4w9WgXcQ", "https://youtu.be/short", "https://youtu.be/dQw4w9WgXcQmore",
        "file:///etc/passwd", "https://youtube.com/watch", "https://youtube.com/playlist?list=abc")
    bad.forEach { expect(VideoLink.normalize(it), null) }
    listOf(0L to "0:00", 62_000L to "1:02", 3_600_000L to "1:00:00", -8L to "0:00").forEach {
        expect(TimeLabel.format(it.first), it.second)
    }
    val validSearchEntries = listOf(
        "/watch?v=dQw4w9WgXcQ",
        "watch?v=dQw4w9WgXcQ",
        "/shorts/dQw4w9WgXcQ",
        "dQw4w9WgXcQ",
        "https://music.youtube.com/watch?v=dQw4w9WgXcQ"
    )
    validSearchEntries.forEach { expect(VideoLink.normalizeSearchResult(it), known) }
    val invalidSearchEntries = listOf(
        "https://youtube.com.evil.example/watch?v=dQw4w9WgXcQ",
        "../watch?v=dQw4w9WgXcQ",
        "/watch?v=wrong",
        "file:///etc/passwd"
    )
    invalidSearchEntries.forEach { expect(VideoLink.normalizeSearchResult(it), null) }
    println("PASS: search extraction URL normalization (" + validSearchEntries.size +
        " accepted, " + invalidSearchEntries.size + " rejected)")
    val nameQueries = listOf("shape of you", "Adele Hello", "আকাশ", "Punjabi songs 2026", "lofi hip hop")
    nameQueries.forEach { check(SearchInputResolver.resolve(it) == SearchInput.Keywords(it)) }
    check(SearchInputResolver.resolve(" https://youtu.be/dQw4w9WgXcQ ") == SearchInput.VideoUrl(known))
    check(SearchInputResolver.resolve(" ") == SearchInput.Blank)
    check(SearchInputResolver.resolve("https://youtube.com/watch?v=bad") == SearchInput.BadLink)
    println("PASS: ${nameQueries.size} free-form song names route to search; direct link, blank and malformed links classified")
    println("PASS: ${good.size} valid links, ${bad.size} rejected links, 4 duration labels")
}
