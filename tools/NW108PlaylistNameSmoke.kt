import com.nightwave.player.v2.data.PlaylistNameIdentity as P
import java.util.Locale

fun main() {
    var checks = 0
    fun eq(actual: Any?, wanted: Any?, name: String) {
        check(actual == wanted) { "$name: expected $wanted but got $actual" }; checks++
    }
    val former = Locale.getDefault()
    try {
        val verdicts = mutableListOf<List<Any>>()
        for (lang in listOf("en-US", "tr-TR", "az-AZ", "lt-LT", "bn-BD")) {
            Locale.setDefault(Locale.forLanguageTag(lang))
            val outcome = listOf(
                P.key("I"), P.key("i"), P.key("İ"),
                P.same("I", "i"), P.same("Mix", "mix"),
                P.allDistinct(listOf("I", "i")),
                P.allDistinct(listOf("Mix", "Focus")),
                P.allDistinct(listOf("Mix", "mIX")),
                P.allDistinct(listOf("İ", "i")),
                P.allDistinct(listOf("I", "ı")),
                P.same("İ", "i"),
                P.same("I", "ı")
            )
            verdicts.add(outcome)
            eq(outcome, verdicts.first(), "locale parity $lang")
        }
        eq(P.key("I"), "i", "canonical ascii I")
        eq(P.key("İ"), "i\u0307", "canonical dotted uppercase I")
        eq(P.allDistinct(emptyList()), true, "empty safe")
        eq(P.allDistinct((1..50).map { "Playlist $it" }), true, "50 unique")
        eq(P.allDistinct((1..50).map { if (it==50) "pLAYLIST 1" else "Playlist $it" }), false, "50th duplicate")
        eq(P.same("Ambient", "ambient"), true, "plain case")
        eq(P.same("Mix", "Another"), false, "different names")
    } finally { Locale.setDefault(former) }
    println("NW108_PLAYLIST_LOCALE_SMOKE_PASS checks=$checks")
}
