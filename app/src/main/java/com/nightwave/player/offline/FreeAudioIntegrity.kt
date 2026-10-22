package com.nightwave.player.offline

import java.io.File
import java.math.BigInteger
import java.security.MessageDigest

/**
 * Wikimedia's original-file SHA-1 is encoded in base36 (31 digits).
 * This detects accidental corruption or a source changing during a transfer.
 * NW19 separately verifies the app-private copy with SHA-256.
 */
object FreeAudioIntegrity {
    private val mediaWikiSha1 = Regex("[0-9a-z]{31}")

    fun isValidSourceHash(expected: String): Boolean =
        mediaWikiSha1.matches(expected)

    fun sourceSha1Base36(file: File): String {
        val digest = MessageDigest.getInstance("SHA-1")
        file.inputStream().buffered(32_768).use { input ->
            val block = ByteArray(32_768)
            while (true) {
                val count = input.read(block)
                if (count < 0) break
                if (count > 0) digest.update(block, 0, count)
            }
        }
        return BigInteger(1, digest.digest()).toString(36).padStart(31, '0')
    }

    fun sourceMatches(file: File, expected: String): Boolean =
        file.isFile && isValidSourceHash(expected) &&
            sourceSha1Base36(file) == expected
}
