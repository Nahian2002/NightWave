import com.nightwave.player.v2.library.SmartDiscoveryPoolPolicy
import kotlin.random.Random

/** Pure Kotlin reproduction, requiring no Android SDK/network/user files.
 *
 * kotlinc app/src/main/java/com/nightwave/player/v2/library/SmartDiscoveryPoolPolicy.kt tools/NW71SmartDiscoverySmoke.kt -include-runtime -d nw71-smoke.jar
 * java -jar nw71-smoke.jar
 */
private var checks = 0
private fun expect(condition: Boolean, label: String) {
    checks++
    check(condition) { label }
}
fun main() {
    val p = SmartDiscoveryPoolPolicy
    val likes = (0 until 700).map { "favorite-$it" }
    val recent = (0 until 600).map { "recent-$it" }
    val playlists = listOf((0 until 500).map { "playlist-$it" },
        (500 until 1000).map { "playlist-$it" })
    val pool = p.gather(likes, recent, playlists, { it }, { true })
    expect(pool.size == 600, "bounded full pool")
    expect(pool.count { it.startsWith("favorite-") } == 200, "favorites quota")
    expect(pool.count { it.startsWith("recent-") } == 150, "recent quota")
    expect(pool.count { it.startsWith("playlist-") } == 250, "playlist quota")
    expect(pool.distinct().size == pool.size, "unique IDs")
    val old = p.rediscover(pool, likes.toSet(), recent.toSet(), { it })
    expect(old.isNotEmpty(), "rediscover liked but not recent")
    expect(old.all { it in likes && it !in recent }, "rediscovery never repeats history")
    expect(p.rediscover(pool, recent.toSet(), recent.toSet(), { it }).isEmpty(),
        "no fake rediscovery when all favorites were recently heard")
    val filtered = p.gather(listOf("a", "a", "blocked", "b"), listOf("b", "c"),
        listOf(listOf("c", "d")), { it }, { it != "blocked" })
    expect(filtered == listOf("a", "b", "c", "d"), "stable distinct safe order")
    expect(p.gather(emptyList<String>(), emptyList(), emptyList(), { it }, { true }).isEmpty(),
        "empty library creates nothing")
    val random = Random(71)
    repeat(2_000) { seed ->
        val favorites = (0 until random.nextInt(0, 700)).map { random.nextInt(1500) }
        val played = (0 until random.nextInt(0, 500)).map { random.nextInt(1500) }
        val folders = (0 until random.nextInt(0, 14)).map {
            (0 until random.nextInt(0, 240)).map { random.nextInt(1500) }
        }
        val a = p.gather(favorites, played, folders, Int::toString, { it >= 0 })
        val b = p.gather(favorites, played, folders, Int::toString, { it >= 0 })
        expect(a == b, "deterministic pool $seed")
        expect(a.size <= 600, "global cap $seed")
        expect(a.distinct().size == a.size, "no duplicate $seed")
        expect(a.toSet().all { it in (favorites + played + folders.flatten()).toSet() },
            "only input references $seed")
        val previous = played.toSet()
        val rediscover = p.rediscover(a, favorites.toSet(), previous, Int::toString,
            limit = random.nextInt(0, 80))
        expect(rediscover.all { it in favorites && it !in previous },
            "real unplayed-liked $seed")
        expect(rediscover.size <= 60, "rediscovery capped $seed")
    }
    println("NW71 Smart Library standalone Kotlin smoke PASS $checks/$checks")
}
