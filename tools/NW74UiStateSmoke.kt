import com.nightwave.player.v2.ui.StudioUiStateSafetyPolicy
import kotlin.random.Random

/** NW74 Android-independent regression smoke:
 * kotlinc app/src/main/java/com/nightwave/player/v2/ui/StudioUiStateSafetyPolicy.kt \
 *     tools/NW74UiStateSmoke.kt -include-runtime -d nw74-ui.jar
 * java -jar nw74-ui.jar
 */
private var passed = 0
private fun yes(value: Boolean, label: String) {
    passed++
    check(value) { label }
}
fun main() {
    val p = StudioUiStateSafetyPolicy
    val saved = listOf("Dusk", "Midnight", "Morning")
    val smart = listOf("smart:day", "smart:night")
    yes(p.resolveCollection(null, saved, smart, "liked", "recent") == null, "empty selection")
    yes(p.resolveCollection("Dusk", saved, smart, "liked", "recent") == "Dusk", "saved")
    yes(p.resolveCollection("liked", saved, smart, "liked", "recent") == "liked", "liked")
    yes(p.resolveCollection("recent", saved, smart, "liked", "recent") == "recent", "recent")
    yes(p.resolveCollection("smart:day", saved, smart, "liked", "recent") == "smart:day", "smart")
    yes(p.resolveCollection("Deleted", saved, smart, "liked", "recent") == null, "removed")
    yes(p.createdPlaylistName(listOf("Existing", "Last"), listOf(
        "Existing", "New", "Last")) == "New", "new name not necessarily last")
    yes(p.createdPlaylistName(listOf("Old"), emptyList()) == null, "empty result")
    yes(p.createdPlaylistName(listOf("Old"), listOf("Old")) == null, "no invented playlist")
    val random = Random(74)
    repeat(2500) { i ->
        val size = random.nextInt(0, 70)
        val options = (0 until size).map { "row-$it" }
        val index = random.nextInt(-5, 75)
        yes(p.itemAt(options, index) == options.getOrNull(index), "safe dialog index $i")
        val selected = when (random.nextInt(5)) {
            0 -> "liked"
            1 -> "recent"
            2 -> "row-${random.nextInt(0, 70)}"
            3 -> "smart:day"
            else -> "removed"
        }
        val resolved = p.resolveCollection(selected, options, smart, "liked", "recent")
        yes(resolved == selected.takeIf {
            it == "liked" || it == "recent" || it in options || it in smart
        }, "safe collection selection $i")
        val before = options.take(random.nextInt(0, size + 1))
        val created = p.createdPlaylistName(before, options)
        yes(created == options.lastOrNull { it !in before.toSet() },
            "actual newly created name $i")
    }
    println("NW74 UI-state standalone Kotlin smoke PASS $passed/$passed")
}
