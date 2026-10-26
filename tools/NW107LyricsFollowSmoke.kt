import com.nightwave.player.v2.lyrics.LyricsReadingPolicy as P
import com.nightwave.player.v2.lyrics.LyricCue

fun main() {
    var checks = 0
    fun eq(actual: Any?, wanted: Any?, label: String) {
        check(actual == wanted) { "$label expected=$wanted actual=$actual" }
        checks++
    }
    eq(P.followActionLabel(true), "Pause", "following action")
    eq(P.followActionLabel(false), "Follow", "browsing action")
    eq(P.followActionDescription(true), "Pause automatic lyric scrolling", "pause description")
    eq(P.followActionDescription(false), "Resume automatic lyric scrolling", "resume description")
    eq(P.followStateDescription(true), "Automatic lyric follow on", "enabled a11y state")
    eq(P.followStateDescription(false), "Automatic lyric follow off", "disabled a11y state")
    eq(P.toggledFollow(true), false, "pause explicit")
    eq(P.toggledFollow(false), true, "resume explicit")
    eq(P.toggledFollow(P.toggledFollow(true)), true, "round trip following")
    eq(P.toggledFollow(P.toggledFollow(false)), false, "round trip browsing")
    eq(P.followHint(true).startsWith("Following"), true, "following hint")
    eq(P.followHint(false).contains("Select Follow"), true, "browsing hint")
    eq(P.shouldAnimateScroll(0, 12, false), true, "near scroll")
    eq(P.shouldAnimateScroll(0, 13, false), false, "far scroll")
    eq(P.shouldAnimateScroll(0, 2, true), false, "reduced motion")
    eq(P.cueDescription(LyricCue(62_000L, "  আলো   জ্বলে  "), 500, 90_000L),
        "Seek to 1 minute 2 seconds. আলো জ্বলে", "Bangla time description")
    eq(P.cueDescription(LyricCue(61_000L, "  আমার    গান  "), 250, 90_000L),
        "Seek to 1 minute 1 second. আমার গান", "Bangla normalized description")
    println("NW107_LYRICS_FOLLOW_ACCESSIBILITY_SMOKE_PASS checks=$checks")
}
