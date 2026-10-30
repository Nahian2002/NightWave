package com.nightwave.player.updates

import java.io.File
import java.util.Base64
import org.junit.Assert.*
import org.junit.Test

class WeeklyFeedSignatureTest {
    @Test fun actualDisabledFeedMatchesEmbeddedPublicKeyAndRejectsMutation() {
        val root = listOf(File("."), File("..")).first { File(it, "updates/stable.json").isFile }
        val key = File(root, "app/src/main/assets/weekly-update-key.json").readText()
        val feed = File(root, "updates/stable.json").readText()
        fun field(json: String, name: String) = Base64.getDecoder().decode(
            Regex("\"" + name + "\"\\s*:\\s*\"([^\"]+)\"").find(json)!!.groupValues[1])
        val payload = field(feed, "payload"); val signature = field(feed, "signature")
        assertTrue(WeeklyUpdatePolicy.verify(payload, signature, field(key, "modulus"), field(key, "exponent")))
        assertTrue(String(payload).contains("\"enabled\":false"))
        payload[0] = (payload[0].toInt() xor 1).toByte()
        assertFalse(WeeklyUpdatePolicy.verify(payload, signature, field(key, "modulus"), field(key, "exponent")))
    }
}
