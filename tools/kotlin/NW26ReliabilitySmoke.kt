package com.nightwave.player.nw26

import com.nightwave.player.offline.OfflineSearchRanking
import com.nightwave.player.v2.cloud.CloudBackupConsent
import com.nightwave.player.v2.cloud.CloudBackupConsentPolicy
import com.nightwave.player.v2.cloud.DriveHttpFailure
import com.nightwave.player.v2.cloud.DriveHttpFailurePolicy

/** No Android APIs, network, account sign-in or personal files are accessed. */
object NW26ReliabilitySmoke {
    @JvmStatic fun main(args: Array<String>) {
        var passed = 0
        fun checkRule(ok: Boolean) {
            check(ok) { "NW26 rule " + (passed + 1) + " failed" }
            passed++
        }
        val base = CloudBackupConsent("a@example.com", true, false, true, false, true)
        checkRule(CloudBackupConsentPolicy.same(base, base.copy()))
        checkRule(CloudBackupConsentPolicy.same(base, base.copy(account="A@EXAMPLE.COM")))
        checkRule(!CloudBackupConsentPolicy.same(base, base.copy(account="other@example.com")))
        checkRule(!CloudBackupConsentPolicy.same(base, base.copy(account="")))
        checkRule(!CloudBackupConsentPolicy.same(base, base.copy(enabled=false)))
        checkRule(!CloudBackupConsentPolicy.same(base, base.copy(history=true)))
        checkRule(!CloudBackupConsentPolicy.same(base, base.copy(lyrics=false)))
        checkRule(!CloudBackupConsentPolicy.same(base, base.copy(preferences=true)))
        checkRule(!CloudBackupConsentPolicy.same(base, base.copy(wifiOnly=false)))

        checkRule(DriveHttpFailurePolicy.needsAccountAttention(401))
        checkRule(DriveHttpFailurePolicy.needsAccountAttention(403))
        checkRule(!DriveHttpFailurePolicy.needsAccountAttention(503))
        checkRule(!DriveHttpFailurePolicy.mayRetry(401))
        checkRule(!DriveHttpFailurePolicy.mayRetry(403))
        checkRule(!DriveHttpFailurePolicy.mayRetry(404))
        checkRule(DriveHttpFailurePolicy.mayRetry(408))
        checkRule(DriveHttpFailurePolicy.mayRetry(429))
        checkRule(DriveHttpFailurePolicy.mayRetry(500))
        checkRule(DriveHttpFailurePolicy.mayRetry(503))
        checkRule(DriveHttpFailurePolicy.mayRetry(599))
        checkRule(!DriveHttpFailure(403).message.orEmpty().contains("Bearer"))
        checkRule(DriveHttpFailurePolicy.message(401).contains("Reconnect"))
        checkRule(DriveHttpFailurePolicy.message(403).contains("OAuth"))

        val q = OfflineSearchRanking.terms("Beyonce Halo")
        checkRule(OfflineSearchRanking.score(q, "Beyoncé — Halo", "mp3") > 0)
        checkRule(OfflineSearchRanking.normalized("Beyoncé") == "beyonce")
        val bengali = OfflineSearchRanking.terms("ফিরে দেখা")
        checkRule(OfflineSearchRanking.score(bengali, "ফিরে দেখা - Live", "mp3") > 0)
        checkRule(OfflineSearchRanking.normalized("ফিরে").contains("ফিরে"))
        val morning = OfflineSearchRanking.terms("morning")
        checkRule(OfflineSearchRanking.score(morning, "Morning", "mp3") >
            OfflineSearchRanking.score(morning, "Morning Chill", "mp3"))
        checkRule(OfflineSearchRanking.score(morning, "Morning Chill", "mp3") >
            OfflineSearchRanking.score(morning, "A Beautiful Morning", "mp3"))
        checkRule(OfflineSearchRanking.score(OfflineSearchRanking.terms("flac"),
            "Local Recording", "flac") > 0)
        checkRule(OfflineSearchRanking.score(OfflineSearchRanking.terms("xyz"),
            "Morning Chill", "mp3") == -1)
        checkRule(OfflineSearchRanking.terms("!!!").isEmpty())
        println("PASS: NW26 " + passed + " exact-production Kotlin Drive/search assertions")
        println("LIMIT: Android OAuth, actual Drive upload and UI lyrics not tested")
    }
}
