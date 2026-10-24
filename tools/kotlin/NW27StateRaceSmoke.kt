package com.nightwave.player.nw27

import android.content.Context
import com.nightwave.player.v2.cloud.CloudBackupState
import com.nightwave.player.v2.cloud.consentSnapshot

/** The *unmodified* production state class is compiled alongside a test-only Android stub. */
object NW27StateRaceSmoke {
    @JvmStatic
    fun main(args: Array<String>) {
        val context = Context()
        val accountUi = CloudBackupState(context)
        val delayedWorker = CloudBackupState(context)
        check(accountUi.connect("user@example.com"))
        check(accountUi.success("user@example.com", 100L))
        val staleConsent = delayedWorker.read().consentSnapshot()
        check(staleConsent.enabled)

        check(accountUi.disconnect())
        check(accountUi.connect("user@example.com"))
        check(accountUi.success("user@example.com", 200L))
        check(!delayedWorker.successIfCurrent(staleConsent, 999L, true))
        check(!delayedWorker.statusIfCurrent(staleConsent, "stale status"))
        check(accountUi.read().lastSuccessMs == 200L)
        check(accountUi.read().lastResult == "Backup verified")

        val secondConsent = accountUi.read().consentSnapshot()
        check(secondConsent.account.equals(staleConsent.account, ignoreCase = true))
        check(secondConsent.revision != staleConsent.revision)

        check(accountUi.setHistory(true))
        check(!delayedWorker.successIfCurrent(secondConsent, 333L, false))
        check(!delayedWorker.statusIfCurrent(secondConsent, "stale after privacy change"))
        check(accountUi.read().lastSuccessMs == 200L)

        val currentConsent = accountUi.read().consentSnapshot()
        check(delayedWorker.successIfCurrent(currentConsent, 444L, false))
        check(accountUi.read().lastResult == "Already backed up; library unchanged")
        check(delayedWorker.statusIfCurrent(currentConsent, "current worker okay"))
        check(accountUi.read().lastResult == "current worker okay")

        check(accountUi.disconnect())
        check(!delayedWorker.statusIfCurrent(currentConsent, "stale after disconnect"))
        check(accountUi.read().account.isBlank())
        check(accountUi.read().lastResult.startsWith("Disconnected"))
        println("PASS: NW27 exact-source CloudBackupState reconnect/privacy race smoke")
        println("LIMIT: fake Android preferences; no WorkManager, network or APK execution")
    }
}
