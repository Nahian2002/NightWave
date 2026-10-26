import com.nightwave.player.offline.OfflineGaplessPolicy
import com.nightwave.player.offline.OfflinePlaybackQueue
import com.nightwave.player.playback.PlaybackPreviousPolicy
import com.nightwave.player.playback.PlaybackQueue
import com.nightwave.player.playback.QueueEntry
import com.nightwave.player.v2.playback.PlaybackSeekIdentityPolicy

/**
 * JVM-only exact-source policy smoke, no Android runtime or media files.
 *
 * kotlinc app/src/main/java/com/nightwave/player/playback/PlaybackQueue.kt \
 *   app/src/main/java/com/nightwave/player/playback/PlaybackPreviousPolicy.kt \
 *   app/src/main/java/com/nightwave/player/offline/OfflinePlaybackQueue.kt \
 *   app/src/main/java/com/nightwave/player/offline/OfflineGaplessPolicy.kt \
 *   app/src/main/java/com/nightwave/player/v2/playback/PlaybackSeekIdentityPolicy.kt \
 *   tools/NW80PlaybackSmoke.kt -include-runtime -d nw80-playback.jar
 * java -jar nw80-playback.jar
 *
 * Does not certify an Android build, headset controls or device playback.
 */
object NW80PlaybackSmoke {
    private fun item(n: Int) = QueueEntry(
        "https://www.youtube.com/watch?v=" + n.toString().padStart(11, '0'),
        "Song $n", "Artist $n", ""
    )

    @JvmStatic fun main(args: Array<String>) {
        val q = PlaybackQueue(pick = { 0 })
        q.replace((1..4).map(::item), 0)
        q.shuffle = true
        check(q.jump(2) == item(3))
        check(q.insertNext(item(1)))
        check(q.current == item(3))
        check(q.next() == item(1))
        check(q.previous() == item(3))
        check(q.previous() == item(1))
        check(q.next() == item(3))
        check(q.next() == item(1))
        check(q.snapshot().size == 4)

        val id = "offline:track"
        check(PlaybackSeekIdentityPolicy.target(id, id, id, 120_000L, 65_000L) == 65_000L)
        check(PlaybackSeekIdentityPolicy.target(id, id, "offline:new", 120_000L, 65_000L) == null)
        check(PlaybackSeekIdentityPolicy.target(id, id, id, 0L, 65_000L) == null)
        check(PlaybackPreviousPolicy.canRestartLoaded(item(1).url,
            item(1).url, item(1).url, false))
        check(!PlaybackPreviousPolicy.canRestartLoaded(item(1).url,
            item(2).url, item(1).url, false))

        val ids = listOf("local-a", "local-b", "local-c")
        check(OfflineGaplessPolicy.transitionIndex(
            ids, -1, "offline:local-a", automatic = true) == null)
        check(OfflineGaplessPolicy.transitionIndex(
            ids, 0, "offline:local-b", automatic = true) == 1)
        check(OfflineGaplessPolicy.transitionIndex(
            ids, 2, "offline:local-a", automatic = true, repeatAll = true) == 0)
        val off = OfflinePlaybackQueue(pick = { 0 })
        check(off.select(ids, "local-a") == "local-a")
        off.setShuffle(true)
        check(off.next(automatic = false) == "local-b")
        check(off.previous() == "local-a")
        check(off.next(automatic = false) == "local-b")
        println("NW80_PLAYBACK_POLICY_SMOKE_PASS")
    }
}
