import com.nightwave.player.v2.cloud.CloudBackupFreshnessPolicy
import com.nightwave.player.v2.cloud.CloudAuthorizationFailurePolicy
import com.nightwave.player.v2.cloud.CloudConnectionHelpPolicy

/** Reproduce without Android or credentials:
 * kotlinc app/src/main/java/com/nightwave/player/v2/cloud/CloudBackupFreshnessPolicy.kt \
 * app/src/main/java/com/nightwave/player/v2/cloud/CloudAuthorizationFailurePolicy.kt \
 * app/src/main/java/com/nightwave/player/v2/cloud/CloudConnectionHelpPolicy.kt \
 * tools/NW73CloudStatusSmoke.kt -include-runtime -d nw73-cloud.jar
 * java -jar nw73-cloud.jar
 */
private var checks = 0
private fun checkCase(condition: Boolean, why: String) {
    checks++
    check(condition) { why }
}
fun main() {
    val p = CloudBackupFreshnessPolicy
    val request = CloudAuthorizationFailurePolicy.Stage.REQUEST
    val picker = CloudAuthorizationFailurePolicy.Stage.ACCOUNT_PICKER

    checkCase(p.classify(false, true, 1000, 5, 5) == CloudBackupFreshnessPolicy.Freshness.LOCAL_ONLY,
        "disconnected always local")
    checkCase(p.classify(true, false, 1000, 5, 5) == CloudBackupFreshnessPolicy.Freshness.CONNECTED_PENDING,
        "authorization alone not backup")
    checkCase(p.classify(true, true, 0, 5, 5) == CloudBackupFreshnessPolicy.Freshness.CONNECTED_PENDING,
        "never verified without actual timestamp")
    checkCase(p.classify(true, true, 1000, 5, 4) == CloudBackupFreshnessPolicy.Freshness.PREVIOUSLY_VERIFIED,
        "privacy change invalidates current badge")
    checkCase(p.classify(true, true, 1000, 5, -1) == CloudBackupFreshnessPolicy.Freshness.PREVIOUSLY_VERIFIED,
        "legacy unknown verified consent is inconclusive")
    checkCase(p.classify(true, true, 1000, 5, 5) == CloudBackupFreshnessPolicy.Freshness.CURRENT_VERIFIED,
        "exact successful revision ready")
    checkCase(!p.changed(true, true) && !p.changed(false, false),
        "noop switches preserve verified receipt")
    checkCase(p.changed(true, false) && p.changed(false, true),
        "real setting changes rotate consent revision")
    val before = CloudConnectionHelpPolicy.guidance(request, 8)
    val after = CloudConnectionHelpPolicy.guidance(picker, 8)
    checkCase(!before.detail.contains("picker appeared"), "request error no false picker")
    checkCase(before.detail.contains("before a verified account selection"),
        "request error phase precise")
    checkCase(after.detail.contains("picker appeared"),
        "picker error describes the actual stage")
    checkCase(after.needsSetupReport && before.needsSetupReport,
        "diagnostics needed but not auth success")
    checkCase(before.offerSingleRetry && after.offerSingleRetry,
        "only manual retry hint, not automatic login")
    for (revision in 0L..2_000L) {
        val freshness = p.classify(true, true, 600L, revision, 12L)
        checkCase(freshness == if (revision == 12L) CloudBackupFreshnessPolicy.Freshness.CURRENT_VERIFIED
            else CloudBackupFreshnessPolicy.Freshness.PREVIOUSLY_VERIFIED, "revision verified only when exact")
        checkCase(p.classify(true, false, 600L, revision, revision)
            == CloudBackupFreshnessPolicy.Freshness.CONNECTED_PENDING, "disabled backup never shown current")
        checkCase(p.classify(false, true, 600L, revision, revision)
            == CloudBackupFreshnessPolicy.Freshness.LOCAL_ONLY, "no connected account")
    }
    println("NW73 cloud freshness + stage-aware help PASS $checks/$checks")
}
