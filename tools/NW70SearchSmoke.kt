import com.nightwave.player.net.SearchScriptNormalization
import kotlin.random.Random

/**
 * NW70 Android-free smoke:
 * kotlinc app/src/main/java/com/nightwave/player/net/SearchScriptNormalization.kt tools/NW70SearchSmoke.kt -include-runtime -d nw70-search.jar
 * java -jar nw70-search.jar
 */
private var checks = 0
private fun eq(actual: Any?, expected: Any?, why: String) {
    checks++
    check(actual == expected) { "$why: expected [$expected], got [$actual]" }
}
private fun yes(condition: Boolean, why: String) {
    checks++
    check(condition) { why }
}

fun main() {
    val n = SearchScriptNormalization::normalize
    eq(n("Beyoncé - Halo"), "beyonce halo", "fold Latin accent")
    eq(n("CAFÉ"), "cafe", "fold upper Latin accent")
    eq(n("élève"), "eleve", "fold multiple accents")
    eq(n("Late-Night / Drive"), "late night drive", "separate punctuation")
    eq(n("ভালোবাসা"), "ভালোবাসা", "preserve Bangla vowel marks")
    eq(n("ফিরে দেখা"), "ফিরে দেখা", "preserve Bangla phrasing")
    eq(n("মালা"), "মালা", "retain aa")
    eq(n("মেলা"), "মেলা", "retain e")
    yes(n("মালা") != n("মেলা"), "distinct Bangla titles stay distinct")
    yes(n("ভালোবাসা") != n("ভলবস"), "vowel-less text cannot match")
    eq(n("    রাত   -    গান "), "রাত গান", "normalize Bangla spaces")
    eq(n("?!  -"), "", "punctuation-only query empty")
    eq(n(""), "", "empty input")
    eq(n("বাংলা Beyoncé"), "বাংলা beyonce", "mixed Latin/Bangla")
    eq(n("  café  café "), "cafe cafe", "stable repeated tokens")
    val cases = listOf("মালা", "মেলা", "ভালোবাসা", "ফিরে দেখা",
        "Beyoncé", "CAFÉ", "Night Drive", "রাত ১২", "  Bangla - Song! ")
    cases.forEach { text ->
        val once = n(text)
        eq(n(once), once, "idempotent normalization")
    }
    val random = Random(70)
    val alphabet = listOf("মা", "মে", "ভা", "বি", "é", "à", "A", "b", " ", "-", "_", ".", "রাত")
    repeat(1_200) {
        val source = buildString {
            repeat(random.nextInt(0, 80)) { append(alphabet[random.nextInt(alphabet.size)]) }
        }
        val cleaned = n(source)
        eq(n(cleaned), cleaned, "idempotent randomized Unicode")
        yes(cleaned.length <= 512, "bounded output")
    }
    println("NW70 standalone search-normalization smoke PASS $checks/$checks")
}
