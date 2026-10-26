import com.nightwave.player.playback.PublicBookmarkCheckpointPolicy

private fun assertDecision(
    loaded: String?, selected: String?, offline: Boolean,
    expected: Boolean, note: String
) {
    check(PublicBookmarkCheckpointPolicy.shouldPersist(loaded, selected, offline) == expected) { note }
}
fun main() {
    val a = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    val b = "https://www.youtube.com/watch?v=J---aiyznGQ"
    val bad = "https://rr.googlevideo.com/videoplayback?expire=10&sig=private"
    var scenarios = 0
    assertDecision(a, a, false, true, "current public song"); scenarios++
    assertDecision(a, b, false, false, "outgoing Media3 item"); scenarios++
    assertDecision(b, a, false, false, "other selected song"); scenarios++
    assertDecision(a, a, true, false, "offline mode"); scenarios++
    assertDecision(null, a, false, false, "no MediaItem"); scenarios++
    assertDecision(a, null, false, false, "no selection"); scenarios++
    assertDecision(null, null, false, false, "neither"); scenarios++
    assertDecision(bad, bad, false, false, "signed stream"); scenarios++
    assertDecision(a + "&sig=x", a + "&sig=x", false, false, "noncanonical query"); scenarios++
    assertDecision("file:///private/song.mp3", "file:///private/song.mp3", false,
        false, "private path"); scenarios++
    assertDecision(a, a, false, true, "no stale bleed after resumed selection"); scenarios++
    println("NW94_PUBLIC_BOOKMARK_IDENTITY_SMOKE_PASS scenarios=$scenarios")
}
