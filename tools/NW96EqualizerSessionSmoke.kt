import com.nightwave.player.playback.EqualizerSelection
import com.nightwave.player.playback.EqualizerSelectionPolicy as P

fun main() {
    var cases = 0
    fun verify(actual: Boolean, wanted: Boolean, detail: String) {
        check(actual == wanted) { detail }; cases++
    }
    val centers = listOf(60, 230, 910, 3600, 14000)
    val gains = listOf(250, 100, 0, -100, 175)
    val curve = EqualizerSelection(21, centers, gains)
    verify(P.valid(EqualizerSelection(0)), true, "off")
    verify(P.valid(EqualizerSelection(7)), true, "device preset")
    verify(P.valid(EqualizerSelection(22)), false, "invalid preset")
    verify(P.valid(curve), true, "valid manual")
    verify(P.matchesSession(curve, centers), true, "same live bands")
    verify(P.matchesSession(curve, listOf(60,230,910,3500,14000)), false,
        "same count different frequencies must be rejected")
    verify(P.matchesSession(curve, centers.reversed()), false, "reordered bands")
    verify(P.valid(curve.copy(levelsMb = gains.dropLast(1))), false, "truncated curve")
    verify(P.valid(curve.copy(levelsMb = listOf(1600, 0, 0, 0, 0))), false,
        "out of safe gain")
    verify(P.valid(curve.copy(centersHz = listOf(100, 100, 300, 400, 500))), false,
        "duplicate centers")
    verify(P.valid(curve.copy(centersHz = listOf(100, -100, 300, 400, 500))), false,
        "unsafe center")
    verify(P.valid(EqualizerSelection(0, centers, gains)), false, "off cannot have stale curve")
    verify(P.valid(EqualizerSelection(5, centers, gains)), false, "preset cannot masquerade as manual")
    verify(P.valid(EqualizerSelection(21, List(33){it+100},List(33){0})), false,
        "overlong curve")
    verify(P.matchesSession(EqualizerSelection(5), emptyList()), true,
        "device preset validates on effect separately")
    println("NW96_EQ_SELECTION_POLICY_PASS cases=$cases")
}
