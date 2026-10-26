import com.nightwave.player.playback.*
import kotlin.random.Random

/** NW110: real public PlaybackQueue invariants across 10,800 transitions.
 * Assertions are independent of Android/Media3 and do not grant release GO.
 */
fun main() {
    var assertions = 0
    for (seed in 0 until 90) {
        val rng = Random(seed)
        val queue = PlaybackQueue(pick = { bound -> rng.nextInt(bound) })
        fun track(i: Int) = QueueEntry(
            "https://www.youtube.com/watch?v=" + i.toString().padStart(11, '0'),
            "title $i", "artist", "")
        val total = 3 + seed % 11
        queue.replace((1..total).map(::track), seed % total)
        for (step in 0 until 120) {
            when (rng.nextInt(8)) {
                0 -> queue.shuffle = !queue.shuffle
                1 -> queue.repeat = PlaybackQueue.Repeat.entries[rng.nextInt(3)]
                2 -> queue.jump(rng.nextInt(queue.size))
                3 -> queue.previous()
                4 -> if (queue.size < 50) queue.insertNext(track(1000 + seed * 120 + step))
                5 -> if (!queue.shuffle)
                    queue.removeUpcoming(rng.nextInt(queue.size), "incorrect-url")
                6 -> queue.next(automatic = true)
                7 -> queue.restoreNextPriorities(queue.priorityNextUrls())
            }
            val before = queue.snapshot()
            val current = queue.current
            val history = queue.shuffleRecoveryState()
            val priorities = queue.priorityNextUrls()
            val available = queue.hasNext()
            check(queue.hasNext() == available) {
                "Probe returned changing value seed=$seed step=$step"
            }
            check(queue.snapshot() == before && queue.current == current &&
                queue.shuffleRecoveryState() == history &&
                queue.priorityNextUrls() == priorities) {
                "Availability probe modified queue seed=$seed step=$step"
            }
            val next = queue.next(automatic = false)
            check(available == (next != null)) {
                "Next availability disagreed with action seed=$seed step=$step"
            }
            assertions += 3
        }
    }
    println("NW110_RANDOM_QUEUE_INVARIANTS_PASS assertions=$assertions")
}
