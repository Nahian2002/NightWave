import com.nightwave.player.v2.library.SmartDiscoveryPoolPolicy
import com.nightwave.player.v2.ui.StudioUiStateSafetyPolicy
import com.nightwave.player.v2.cloud.CloudBackupConsent
import com.nightwave.player.v2.cloud.CloudAccountSessionPolicy
import com.nightwave.player.v2.cloud.CloudBackupFreshnessPolicy
import kotlin.random.Random

/** Run after checking hashes of the FIVE unchanged production Kotlin policies.
 * Android-free integration smoke; this is NOT an APK compilation or device test.
 */
private var assertions = 0
private fun checkThat(ok: Boolean, reason: String) {
    assertions++
    check(ok) { reason }
}

fun main() {
    // Reproduce the corrected NW71 fixture: 200/playlist scan, 250 total quota.
    val favorites = (0 until 600).map { "favorite-$it" }
    val recent = (0 until 500).map { "recent-$it" }
    val saved = (0 until 400).map { "playlist-$it" }
    val original = saved.toList()
    val mix = SmartDiscoveryPoolPolicy.gather(favorites, recent,
        listOf(saved.take(200), saved.drop(200)), { it }, { true })
    checkThat(mix.size == 600, "600 entries from reserved quotas")
    checkThat(mix.count { it.startsWith("favorite-") } == 200, "200 favorites")
    checkThat(mix.count { it.startsWith("recent-") } == 150, "150 recent")
    checkThat(mix.count { it.startsWith("playlist-") } == 250, "250 across two playlists")
    checkThat(saved == original, "source data unchanged")
    val single = SmartDiscoveryPoolPolicy.gather(emptyList(), emptyList(), listOf(saved),
        { it }, { true })
    checkThat(single.size == 200, "per-playlist 200-entry cap")
    val consent = CloudBackupConsent("account@example.invalid", true, false, false,
        false, true, revision = 12)
    val policy = CloudBackupFreshnessPolicy
    checkThat(CloudAccountSessionPolicy.mayApply(consent, consent, false), "valid restore session")
    checkThat(!CloudAccountSessionPolicy.mayApply(consent, consent, true), "no concurrent restore")
    checkThat(!CloudAccountSessionPolicy.mayApply(consent,
        consent.copy(revision = 13), false), "reconnect revokes old preview")
    checkThat(policy.classify(true, true, 900, 12, 12) ==
        CloudBackupFreshnessPolicy.Freshness.CURRENT_VERIFIED, "current verified snapshot")
    checkThat(policy.classify(true, true, 900, 13, 12) ==
        CloudBackupFreshnessPolicy.Freshness.PREVIOUSLY_VERIFIED, "privacy change stales backup")
    checkThat(policy.preservesVerifiedSnapshot("wifi_only"), "network-only choice safe")
    checkThat(!policy.preservesVerifiedSnapshot("lyrics"), "lyrics choice changes payload")
    checkThat(StudioUiStateSafetyPolicy.resolveCollection("obsolete", listOf("current"),
        emptyList(), "liked", "recent") == null, "obsolete UI selection removed")
    checkThat(StudioUiStateSafetyPolicy.itemAt(listOf("A", "B"), 2) == null,
        "native dialog bounds checked")
    checkThat(StudioUiStateSafetyPolicy.createdPlaylistName(listOf("Earlier"),
        listOf("New", "Earlier")) == "New", "imported playlist identified")

    val random = Random(75)
    repeat(3_000) { round ->
        val liked = List(random.nextInt(0, 500)) { "f-${random.nextInt(0, 900)}" }
        val played = List(random.nextInt(0, 400)) { "r-${random.nextInt(0, 900)}" }
        val playlists = List(random.nextInt(0, 7)) {
            List(random.nextInt(0, 300)) { "p-${random.nextInt(0, 900)}" }
        }
        val snapshot = Triple(liked.toList(), played.toList(), playlists.map { it.toList() })
        val collected = SmartDiscoveryPoolPolicy.gather(liked, played, playlists,
            { it }, { true })
        checkThat(collected.size <= 600, "total cap $round")
        checkThat(collected.distinct().size == collected.size, "deduplicated pool $round")
        checkThat(collected.all { it in liked || it in played ||
            playlists.any { group -> it in group } }, "not inventing tracks $round")
        checkThat(snapshot.first == liked && snapshot.second == played &&
            snapshot.third == playlists, "never modifying saved sources $round")
        val seen = played.toSet()
        val discover = SmartDiscoveryPoolPolicy.rediscover(collected,
            liked.toSet(), seen, { it })
        checkThat(discover.all { it in liked && it !in seen }, "rediscover safely $round")
        val names = List(random.nextInt(0, 40)) { "playlist-$it" }
        val index = random.nextInt(-5, 50)
        checkThat(StudioUiStateSafetyPolicy.itemAt(names, index) == names.getOrNull(index),
            "index safety $round")
        val desired = if (names.isEmpty()) "not-exist" else names[random.nextInt(names.size)]
        checkThat(StudioUiStateSafetyPolicy.resolveCollection(desired, names, emptyList(),
            "liked", "recent") == desired.takeIf { it in names }, "library selection $round")
        checkThat(CloudAccountSessionPolicy.same(consent,
            consent.copy(revision = round.toLong())) == (round == 12), "session receipt $round")
        checkThat(policy.classify(true, true, 999, round.toLong(), 12) ==
            if (round == 12) CloudBackupFreshnessPolicy.Freshness.CURRENT_VERIFIED
            else CloudBackupFreshnessPolicy.Freshness.PREVIOUSLY_VERIFIED,
            "backup freshness $round")
    }
    println("NW75 integrated pure Kotlin source smoke PASS $assertions/$assertions")
}
