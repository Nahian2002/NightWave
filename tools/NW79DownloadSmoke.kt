package com.nightwave.player.offline

/**
 * Standalone Kotlin smoke for exact pure policies (no Android/Gradle SDK).
 *
 * From the repo root, with a compatible kotlinc/java installation:
 * kotlinc app/src/main/java/com/nightwave/player/offline/AudioDownloadNetworkPolicy.kt
 *         app/src/main/java/com/nightwave/player/offline/DownloadCenterViewPolicy.kt
 *         app/src/main/java/com/nightwave/player/offline/DownloadRecoveryPolicy.kt
 *         app/src/main/java/com/nightwave/player/offline/SearchOfflineFailurePolicy.kt
 *         tools/NW79DownloadSmoke.kt -include-runtime -d nw79-smoke.jar
 * java -jar nw79-smoke.jar
 *
 * Do not mistake this limited pure-policy smoke for an Android APK build.
 */
object NW79DownloadSmoke {
    @JvmStatic fun main(args: Array<String>) {
        for (code in listOf(408, 425, 429, 500, 502, 503, 504)) {
            check(AudioDownloadNetworkPolicy.shouldBackOff(code))
        }
        for (code in listOf(200, 400, 401, 403, 404, 410, 416, 451)) {
            check(!AudioDownloadNetworkPolicy.shouldBackOff(code))
        }
        check(AudioDownloadNetworkPolicy.MAX_TRANSFER_MINUTES == 180L)
        check(RetryableAudioSourceResponse(429).message == "HTTP 429")
        check(DownloadCenterViewPolicy.isVerifying("RUNNING", true))
        check(!DownloadCenterViewPolicy.isVerifying("CANCELLED", true))
        check(!DownloadCenterViewPolicy.isReady("RUNNING", true))
        check(!DownloadCenterViewPolicy.isReady("SUCCEEDED", false))
        check(DownloadCenterViewPolicy.isReady("SUCCEEDED", true))
        check(DownloadRecoveryPolicy.failedMessage("HTTP 503") ==
            "Audio source is temporarily unavailable. Retry later.")
        check(DownloadRecoveryPolicy.failedMessage("HTTP 429") ==
            "Audio source is rate-limited. Retry later.")
        check(SearchOfflineFailurePolicy.message("HTTP 503", true) ==
            "Audio source is temporarily unavailable. Retry later.")
        check(DownloadCenterViewPolicy.percent(50L, 100L) == 50)
        check(DownloadCenterViewPolicy.recentRate(42_000L, 1_000L, 9_001L) == null)
        println("NW79_DOWNLOAD_POLICY_SMOKE_PASS")
    }
}
