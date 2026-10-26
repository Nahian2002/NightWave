package com.nightwave.player.net

/** Compile-only stand-in for the real Android app model. Not included in APK. */
data class VideoPreview(
    val title: String, val author: String, val url: String,
    val thumbnail: String = "", val durationSeconds: Long = 0L
)

/**
 * NW86 independent Kotlin policy smoke, no Android, no server/network.
 *
 * From repository root:
 * kotlinc app/src/main/java/com/nightwave/player/v2/library/LibraryBulkSelectionPolicy.kt \
 *   tools/NW86LibrarySmoke.kt -include-runtime -d nw86-library.jar
 * java -cp nw86-library.jar com.nightwave.player.net.NW86LibrarySmoke
 *
 * Not a replacement for full JUnit, library transaction or Android UI tests.
 */
object NW86LibrarySmoke {
    @JvmStatic fun main(args: Array<String>) {
        val policy = com.nightwave.player.v2.library.LibraryBulkSelectionPolicy
        val all = (0..29).map { i ->
            VideoPreview("Track $i", "Artist", "music:${i.toString().padStart(4, '0')}")
        }
        check(policy.MAX_SELECTED == 20)
        val first = policy.toggleVisible(emptySet(), all.take(3), all)
        check(first == all.take(3).map { it.url }.toSet())
        check(policy.visibleActionLabel(first, all.take(3)) == "Deselect visible")
        val second = policy.toggleVisible(first, all.drop(3).take(3), all)
        check(second.size == 6)
        check(policy.hiddenSelectionCount(second, all.drop(3).take(3)) == 3)
        val third = policy.toggleVisible(second, all.take(3), all)
        check(third == all.drop(3).take(3).map { it.url }.toSet())
        val capped = policy.toggleVisible(third, all, all)
        check(capped.size == 20)
        check(capped.containsAll(third))
        check(policy.selected(all, capped).size == 20)
        check(policy.toggleVisible(setOf("unknown"), emptyList(), all).isEmpty())
        check(policy.selected(all, capped).all { it in all })
        println("NW86_LIBRARY_SELECTION_POLICY_SMOKE_PASS")
    }
}
