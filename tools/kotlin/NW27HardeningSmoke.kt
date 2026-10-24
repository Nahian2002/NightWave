package com.nightwave.player.nw27

import com.nightwave.player.diagnostics.NightWaveCrashPrivacyPolicy
import com.nightwave.player.v2.cloud.DriveResponseReadPolicy

/**
 * Runs against unmodified production policy files with no Android runtime,
 * accounts, network, local library, external storage, or keystore access.
 */
object NW27HardeningSmoke {
    @JvmStatic
    fun main(args: Array<String>) {
        val privacy = NightWaveCrashPrivacyPolicy
        val body = DriveResponseReadPolicy
        val cases = listOf(
            privacy.threadLabel("main") == "main",
            privacy.threadLabel("RenderThread") == "render",
            privacy.threadLabel("FinalizerDaemon") == "runtime",
            privacy.threadLabel("ReferenceQueueDaemon") == "runtime",
            privacy.threadLabel("user@example.com") == "background",
            privacy.threadLabel("https://host.invalid/path") == "background",
            privacy.safeSourceSymbol("player.Ui.Screen", 130) == "player.Ui.Screen",
            privacy.safeSourceSymbol("A/B\\C", 130) == "A_B_C",
            privacy.safeSourceSymbol("abcdef", 4) == "abcd",
            privacy.safeSourceSymbol("xy", -15) == "x",
            privacy.safeSourceSymbol("Code\$Inner", 130) == "Code\$Inner",
            privacy.safeFrameLine(-1) == 0,
            privacy.safeFrameLine(0) == 0,
            privacy.safeFrameLine(17) == 17,
            privacy.safeFrameLine(Int.MAX_VALUE) == 0,
            body.canAppend(0, 1, 10),
            body.canAppend(9, 1, 10),
            !body.canAppend(10, 1, 10),
            !body.canAppend(8, 3, 10),
            !body.canAppend(-1, 1, 10),
            !body.canAppend(1, -1, 10),
            !body.canAppend(1, 0, 10),
            !body.canAppend(1, Int.MAX_VALUE, Int.MAX_VALUE),
            !body.canAppend(Int.MAX_VALUE, 1, Int.MAX_VALUE),
            !body.canAppend(1, 1, -10),
            !body.stalled(0),
            !body.stalled(8),
            body.stalled(9)
        )
        cases.forEachIndexed { index, pass ->
            check(pass) { "NW27 hardening assertion " + (index + 1) + " failed" }
        }
        println("PASS: NW27 " + cases.size + " exact-source Kotlin safety assertions")
        println("LIMIT: Android Gradle compilation and device behavior not tested")
    }
}
