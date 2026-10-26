import com.nightwave.player.playback.ShuffleRecoveryPolicy
import com.nightwave.player.playback.ShuffleRecoverySnapshot

private fun requireValid(condition: Boolean, title: String) { check(condition) { title } }
fun main() {
    val ids = (1..5).map { "https://www.youtube.com/watch?v=" + it.toString().padStart(11, '0') }
    val available = ids.toSet()
    var n = 0
    val current = ids[2]
    val good = ShuffleRecoverySnapshot(listOf(ids[0], ids[1]), listOf(ids[4]), listOf(ids[0],ids[1]))
    requireValid(ShuffleRecoveryPolicy.validated(good, available, current) == good, "valid history")
    n++
    requireValid(ShuffleRecoveryPolicy.validated(good, available, null) == null, "no selected song")
    n++
    requireValid(ShuffleRecoveryPolicy.validated(null, available, current) == null, "missing legacy history")
    n++
    requireValid(ShuffleRecoveryPolicy.validated(good.copy(visited = listOf("https://other.example/signed")), available, current) == null, "foreign stream")
    n++
    requireValid(ShuffleRecoveryPolicy.validated(good.copy(forward = List(101){ids[0]}), available, current) == null, "history oversized")
    n++
    requireValid(ShuffleRecoveryPolicy.validated(good.copy(seen = listOf(ids[0], ids[0])), available, current) == null, "duplicate seen")
    n++
    requireValid(ShuffleRecoveryPolicy.validated(good.copy(visited = listOf(ids[4],ids[4])), available, current) != null, "repeat visits allowed")
    n++
    requireValid(ShuffleRecoveryPolicy.validated(good.copy(forward = listOf(ids[0])), available, current)?.forward == listOf(ids[0]), "forward trail preserved")
    n++
    requireValid(ShuffleRecoveryPolicy.validated(good, available, "unrelated") == null, "wrong song")
    n++
    requireValid(ShuffleRecoveryPolicy.validated(good.copy(seen = ids), available, current)?.seen == ids, "full cycle")
    n++
    println("NW93_SHUFFLE_HISTORY_POLICY_SMOKE_PASS scenarios=$n")
}
