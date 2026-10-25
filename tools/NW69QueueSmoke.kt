import com.nightwave.player.offline.OfflinePlaybackQueue
import com.nightwave.player.playback.PlaybackQueue
import com.nightwave.player.playback.QueueEntry
import kotlin.random.Random

/**
 * NW69 standalone smoke: compile with the two exact production queue files:
 *
 * kotlinc app/src/main/java/com/nightwave/player/playback/PlaybackQueue.kt
 *   app/src/main/java/com/nightwave/player/offline/OfflinePlaybackQueue.kt
 *   tools/NW69QueueSmoke.kt -include-runtime -d nw69-queues.jar
 * java -jar nw69-queues.jar
 *
 * Keep the above kotlinc arguments on ONE command line.
 * No Android SDK, JUnit, audio playback, network, or secret is needed.
 */
private var assertions = 0
private fun equal(actual: Any?, expected: Any?, message: String) {
    assertions++
    check(actual == expected) { "$message: got [$actual], expected [$expected]" }
}
private fun valid(condition: Boolean, message: String) {
    assertions++
    check(condition) { message }
}
private fun song(i: Int) = QueueEntry(
    "https://www.youtube.com/watch?v=" + i.toString().padStart(11, '0'),
    "Song $i", "Artist", ""
)
private fun publicQueue() = PlaybackQueue(pick = { 0 }).apply {
    replace((1..5).map(::song), 0)
}
private fun offlineQueue() = OfflinePlaybackQueue(pick = { 0 }).apply {
    select(listOf("a", "b", "c", "d"), "a")
    setShuffle(true)
}

fun main() {
    run {
        val q = offlineQueue()
        listOf("b", "c", "d").forEach { equal(q.next(), it, "offline forward") }
        listOf("c", "b", "a").forEach { equal(q.previous(), it, "offline backward") }
        listOf("b", "c", "d").forEach { equal(q.next(), it, "offline redo") }
        equal(q.next(), null, "offline exhausted")
    }
    run {
        val q = offlineQueue()
        q.next(); q.previous(); q.setRepeat(PlaybackQueue.Repeat.ONE)
        repeat(75) { equal(q.next(true), "a", "offline repeat one") }
        equal(q.next(false), "b", "manual redo")
    }
    run {
        val q = offlineQueue()
        q.next(); q.previous(); q.jump(2)
        equal(q.next(), "d", "jump drops stale forward")
        equal(q.previous(), "c", "branch backward")
        equal(q.next(), "d", "branch redo")
    }
    run {
        val q = offlineQueue()
        q.next(); q.previous(); q.jump(0)
        equal(q.next(), "b", "same slot retains forward")
    }
    run {
        val q = publicQueue()
        q.insertNext(song(4)); q.insertNext(song(3))
        equal(q.priorityNextUrls(), listOf(song(3).url, song(4).url), "priority order")
        equal(q.jump(2), song(4), "jump past")
        equal(q.priorityNextUrls(), emptyList<String>(), "old priorities cleared")
        equal(q.next(), song(2), "no backwards forced priority")
    }
    run {
        val q = publicQueue()
        q.insertNext(song(5)); q.jump(0)
        equal(q.priorityNextUrls(), listOf(song(5).url), "future priority survives")
        equal(q.next(), song(5), "future priority wins")
    }
    run {
        val q = publicQueue()
        q.insertNext(song(4))
        val saved = q.snapshot()
        q.jump(2)
        equal(q.priorityNextUrls(), emptyList<String>(), "past priority removed")
        equal(q.snapshot(), saved, "original queue preserved")
    }
    run {
        val q = publicQueue()
        q.shuffle = true
        q.insertNext(song(5)); q.jump(2)
        equal(q.priorityNextUrls(), emptyList<String>(), "shuffle priority removed")
        valid(q.next() != null, "shuffle still has valid candidate")
    }
    run {
        val q = publicQueue()
        q.shuffle = true
        equal(q.next(), song(2), "public shuffle")
        equal(q.previous(), song(1), "public Previous")
        equal(q.next(), song(2), "public forward")
    }
    run {
        val q = publicQueue()
        q.repeat = PlaybackQueue.Repeat.ONE
        repeat(25) { equal(q.next(true), song(1), "public repeat one") }
        equal(q.next(false), song(2), "manual Next leaves repeat one")
    }
    run {
        val q = OfflinePlaybackQueue(pick = { 0 })
        q.select(listOf("single"), "single")
        q.setShuffle(true)
        q.setRepeat(PlaybackQueue.Repeat.ALL)
        repeat(100) { equal(q.next(), "single", "one-song Repeat All") }
    }
    // Seeded stress: enforce bounded queues, valid indices and no file-ID mutation.
    run {
        val q = PlaybackQueue(pick = { it - 1 })
        q.replace((1..100).map(::song), 49)
        q.shuffle = true
        val random = Random(74)
        repeat(5_000) {
            when (random.nextInt(5)) {
                0 -> q.next()
                1 -> q.previous()
                2 -> q.jump(random.nextInt(-1, 102))
                3 -> q.insertNext(song(random.nextInt(1, 103)))
                else -> q.shuffle = !q.shuffle
            }
            valid(q.index in 0 until q.size, "public index remains valid")
            valid(q.priorityNextUrls().all { url ->
                q.snapshot().indexOfFirst { it.url == url } > q.index
            }, "public priorities must stay ahead")
            valid(q.size <= 100, "public queue cap")
        }
    }
    run {
        val q = OfflinePlaybackQueue(pick = { it - 1 })
        q.select((0..399).map { "song-$it" }, "song-0")
        q.setShuffle(true)
        val original = q.snapshot()
        val random = Random(93)
        repeat(5_000) {
            when (random.nextInt(6)) {
                0 -> q.next()
                1 -> q.previous()
                2 -> q.jump(random.nextInt(-1, 402))
                3 -> q.setRepeat(PlaybackQueue.Repeat.ALL)
                4 -> q.setRepeat(PlaybackQueue.Repeat.OFF)
                else -> q.setShuffle(!q.shuffle)
            }
            valid(q.index in 0 until q.size, "offline index remains valid")
            equal(q.snapshot(), original, "offline file IDs are immutable")
        }
    }
    println("NW69 production Kotlin queues smoke PASS $assertions/$assertions")
}
