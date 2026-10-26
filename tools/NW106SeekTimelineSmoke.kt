import com.nightwave.player.v2.ui.NightWaveSeekGesturePolicy as P

fun main() {
    var cases = 0
    fun requireEqual(value: Float?, wanted: Float?, name: String) {
        check(value == wanted) { "$name: expected=$wanted actual=$value" }; cases++
    }
    fun requireNear(value: Float?, wanted: Float, name: String) {
        check(value != null && kotlin.math.abs(value - wanted) < 0.00002f) {
            "$name: expected=$wanted actual=$value"
        }
        cases++
    }
    requireEqual(P.touchFractionOrNull(4f, 10f, 8f), null, "narrow")
    requireEqual(P.touchFractionOrNull(8f, 16f, 8f), null, "collapsed")
    requireEqual(P.touchFractionOrNull(3f, 0f, 8f), null, "zero width")
    requireEqual(P.touchFractionOrNull(Float.NaN, 108f, 8f), null, "nan X")
    requireEqual(P.touchFractionOrNull(8f, Float.POSITIVE_INFINITY, 8f), null, "infinite width")
    requireEqual(P.touchFractionOrNull(8f, 108f, Float.NaN), null, "nan edge")
    requireEqual(P.touchFractionOrNull(8f, 108f, -1f), null, "negative edge")
    requireEqual(P.accessibilityFractionOrNull(Float.NaN), null, "nan a11y")
    requireEqual(P.accessibilityFractionOrNull(Float.POSITIVE_INFINITY), null, "infinite a11y")
    requireEqual(P.accessibilityFractionOrNull(Float.NEGATIVE_INFINITY), null, "negative infinite a11y")
    requireEqual(P.accessibilityFractionOrNull(-1f), 0f, "a11y clamp low")
    requireEqual(P.accessibilityFractionOrNull(2f), 1f, "a11y clamp high")
    requireEqual(P.accessibilityFractionOrNull(.35f), .35f, "a11y valid")
    for (v in listOf(0f, .05f, .25f, .5f, .75f, .95f, 1f)) {
        requireNear(P.touchFractionOrNull(P.thumbX(v, 320f, 8f), 320f, 8f),
            v, "thumb inverse $v")
    }
    requireEqual(P.touchFractionOrNull(-500f, 108f, 8f), 0f, "clamped start")
    requireEqual(P.touchFractionOrNull(500f, 108f, 8f), 1f, "clamped end")
    requireEqual(P.touchFraction(4f, 10f, 8f), 0f, "legacy fallback")
    requireEqual(P.thumbX(1f, 10f, 8f), 5f, "narrow thumb center")
    println("NW106_SEEK_TIMELINE_POLICY_SMOKE_PASS cases=$cases")
}
