package com.nightwave.player.playback

import java.security.MessageDigest

/** Ephemeral device/session identity, never a saved EQ curve or audible-output claim. */
internal object EqualizerCommandPolicy {
    fun token(
        sessionId: Int, centersHz: List<Int>, minimumMb: Int, maximumMb: Int,
        presetNames: List<String>
    ): String? {
        if (sessionId <= 0 || centersHz.size !in 1..32 ||
            centersHz.any { it !in 1..100_000 } ||
            centersHz.distinct().size != centersHz.size ||
            minimumMb !in Short.MIN_VALUE..Short.MAX_VALUE ||
            maximumMb !in Short.MIN_VALUE..Short.MAX_VALUE || minimumMb > maximumMb ||
            presetNames.size > 20 || presetNames.any { it.length > 60 }) return null
        // Length-prefix names so separators in manufacturer-defined names
        // cannot alias another preset table. Band order is significant.
        val identity = "$sessionId|$minimumMb:$maximumMb|" +
            centersHz.joinToString(",") + "|" +
            presetNames.joinToString("") { "${it.length}:$it" }
        return MessageDigest.getInstance("SHA-256")
            .digest(identity.toByteArray(Charsets.UTF_8))
            .joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
    }

    fun accepts(expected: String?, current: String?): Boolean =
        expected != null && expected.length == 64 &&
            expected.all { it in '0'..'9' || it in 'a'..'f' } &&
            current != null && expected == current

    fun shouldResetDraft(previous: String?, current: String?): Boolean =
        current == null || previous != current
}
