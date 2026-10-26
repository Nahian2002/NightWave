import com.nightwave.player.offline.OfflinePlaybackQueue
import com.nightwave.player.offline.OfflineGaplessPolicy
import com.nightwave.player.playback.PlaybackQueue

fun main() {
    var cases = 0
    fun expect(actual: Any?, expected: Any?, label: String) {
        check(actual == expected) { "$label: expected=$expected actual=$actual" }; cases++
    }
    fun queue() = OfflinePlaybackQueue(pick = { 0 }).apply {
        select(listOf("a", "b", "c", "d", "e"), "a")
        setShuffle(true)
    }
    run {
        val q = queue()
        expect(q.next(), "b", "first shuffled next")
        expect(q.previous(), "a", "previous goes to real played song")
        expect(q.jump(3), "d", "branch to d")
        expect(q.next(), "c", "branch cannot pick already-heard b")
        expect(q.next(), "e", "continue genuinely unheard")
        expect(q.next(), null, "repeat off exhausts without replaying b")
    }
    run {
        val q = queue()
        expect(q.next(), "b", "walk b")
        expect(q.next(), "c", "walk c")
        expect(q.previous(), "b", "back to b")
        expect(q.previous(), "a", "back to a")
        expect(q.next(), "b", "forward b not shuffled")
        expect(q.next(), "c", "forward c not shuffled")
        expect(q.next(), "d", "unplayed d")
        expect(q.next(), "e", "unplayed e")
        expect(q.next(), null, "cycle ends correctly")
    }
    run {
        val q = queue()
        q.next() // b
        q.next() // c
        q.previous() // b
        expect(q.jump(4), "e", "new jump clears redo trail")
        expect(q.next(), "d", "new branch skips already-heard c")
        expect(q.next(), null, "no seen candidates after jump")
    }
    run {
        val q = queue()
        q.next()
        q.previous()
        q.setRepeat(PlaybackQueue.Repeat.ONE)
        expect(q.next(true), "a", "repeat one does not lose forward")
        expect(q.next(false), "b", "manual skip follows forward")
        q.setRepeat(PlaybackQueue.Repeat.ALL)
        q.next() // c
        q.next() // d
        q.next() // e
        expect(q.next(), "a", "repeat all starts new cycle, no immediate repeat")
    }
    run {
        val ids = listOf("a", "b", "c")
        expect(OfflineGaplessPolicy.transitionIndex(ids, -1, "offline:b", false), null,
            "stale explicit seek no selection")
        expect(OfflineGaplessPolicy.transitionIndex(ids, 99, "offline:b", false), null,
            "out of range selection")
        expect(OfflineGaplessPolicy.transitionIndex(ids, 0, "offline:c", true), null,
            "linear auto cannot skip")
        expect(OfflineGaplessPolicy.transitionIndex(ids, 0, "offline:b", true), 1,
            "adjacent autoplay accepted")
        expect(OfflineGaplessPolicy.transitionIndex(ids, 0, "offline:c", true, shuffled = true), 2,
            "shuffled Media3 choice accepted")
        expect(OfflineGaplessPolicy.transitionIndex(ids, 2, "offline:a", true, repeatAll = true), 0,
            "repeat all may wrap")
        expect(OfflineGaplessPolicy.transitionIndex(ids, 2, "offline:a", true), null,
            "repeat off cannot wrap")
        expect(OfflineGaplessPolicy.transitionIndex(ids, 1, "offline:c", false), 2,
            "explicit valid seek")
        expect(OfflineGaplessPolicy.transitionIndex(listOf("a","a","b"), 0,
            "offline:b", false), null, "duplicate queue rejected")
        expect(OfflineGaplessPolicy.transitionIndex(listOf("a","../bad"), 0,
            "offline:../bad", false), null, "unsafe path rejected")
        expect(OfflineGaplessPolicy.transitionIndex(ids, 0, "https://example.com", false), null,
            "non-offline media ignored")
        expect(OfflineGaplessPolicy.transitionIndex(ids, 0, "offline:missing", false), null,
            "unrecognized id ignored")
    }
    println("NW97_OFFLINE_TRANSITION_SMOKE_PASS cases=$cases")
}
