import com.nightwave.player.v2.cloud.CloudAccountSessionPolicy
import com.nightwave.player.v2.cloud.CloudBackupConsent

/** Android-free source check:
 * kotlinc app/src/main/java/com/nightwave/player/v2/cloud/CloudBackupConsentPolicy.kt \
 *   app/src/main/java/com/nightwave/player/v2/cloud/CloudAccountSessionPolicy.kt \
 *   tools/NW72CloudSessionSmoke.kt -include-runtime -d nw72-cloud.jar
 * java -jar nw72-cloud.jar
 * No OAuth login, Drive call, account secret, or saved data is accessed.
 */
private var count = 0
private fun requireCheck(value: Boolean, description: String) {
    count++
    check(value) { description }
}

fun main() {
    val base = CloudBackupConsent("example@example.invalid", false, false, false,
        false, true, revision = 20L)
    val p = CloudAccountSessionPolicy
    requireCheck(p.same(base, base.copy()), "unchanged session")
    requireCheck(p.same(base, base.copy(account = "EXAMPLE@EXAMPLE.INVALID")),
        "same session, case-folded account")
    requireCheck(p.mayApply(base, base, false), "current inactive operation")
    requireCheck(!p.mayApply(base, base, true), "concurrent operation blocked")
    requireCheck(!p.same(base, base.copy(account = "")), "disconnected")
    requireCheck(!p.same(base.copy(account = ""), base.copy(account = "")),
        "two empty accounts cannot match")
    requireCheck(!p.same(base, base.copy(account = "other@example.invalid")),
        "other account")
    requireCheck(!p.same(base, base.copy(revision = 21)),
        "same-account reconnect invalidates")
    requireCheck(!p.mayApply(base, base.copy(revision = 21), false),
        "old preview cannot apply after same-account reconnect")
    requireCheck(p.same(base, base.copy(enabled = true)),
        "verified backup does not itself rotate account session")
    for (revision in 0L..2_000L) {
        val current = base.copy(revision = revision)
        requireCheck(p.same(base, current) == (revision == base.revision),
            "exact revision match $revision")
        requireCheck(p.mayApply(base, current, false) == (revision == base.revision),
            "revisioned confirmation $revision")
        requireCheck(!p.mayApply(base, current, true),
            "busy receipt denied $revision")
    }
    println("NW72 cloud session standalone Kotlin smoke PASS $count/$count")
}
