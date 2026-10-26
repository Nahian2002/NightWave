package com.nightwave.player.offline

/**
 * Test-only enum stub. The Android production declaration is located in
 * OfflineLibraryHealth.kt. Compile this tools file ONLY with the three pure
 * presentation policies below, never with the full Android app.
 */
enum class OfflineFileHealth {
    AVAILABLE, MISSING, SIZE_CHANGED, UNREADABLE, UNSAFE_METADATA
}

/**
 * Standalone pure Kotlin smoke for the exact GitHub policies:
 *
 * kotlinc app/src/main/java/com/nightwave/player/offline/DownloadCenterViewPolicy.kt \
 *   app/src/main/java/com/nightwave/player/offline/DownloadCenterDashboardPolicy.kt \
 *   app/src/main/java/com/nightwave/player/offline/OfflineLibraryRecoveryGuide.kt \
 *   tools/NW87TransferSmoke.kt -include-runtime -d nw87-transfer.jar
 * java -cp nw87-transfer.jar com.nightwave.player.offline.NW87TransferSmoke
 *
 * This is NOT a Gradle build, WorkManager execution or Android device QA.
 */
object NW87TransferSmoke {
    @JvmStatic fun main(args: Array<String>) {
        val p = DownloadCenterDashboardPolicy
        val v = DownloadCenterViewPolicy.Filter
        check(p.tab(v.READY, 0, true) == "Ready · …")
        check(p.tab(v.ATTENTION, 0, true) == "Issues · …")
        check(p.tab(v.ACTIVE, 3, true) == "Active · 3")
        check(p.tab(v.ALL, 101, false) == "All · 99+")
        check(p.overview(3, 2, 1, 10, false, false)
            .contains("2 ready offline"))
        check(p.overview(3, 0, 0, 10, true, false)
            .contains("Checking saved audio"))
        check(p.helpLine().contains("restarts from zero"))
        val recovery = OfflineLibraryRecoveryGuide
        check(OfflineFileHealth.entries.map(recovery::title).distinct().size == 5)
        check(recovery.help(OfflineFileHealth.MISSING).contains("index"))
        check(recovery.help(OfflineFileHealth.UNREADABLE).contains("readable"))
        check(recovery.emptyIssuesMessage(1).contains("entry needs"))
        check(recovery.emptyIssuesMessage(3).contains("entries need"))
        println("NW87_TRANSFER_OFFLINE_PRESENTATION_SMOKE_PASS")
    }
}
