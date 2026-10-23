package com.nightwave.player.offline

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/** Deterministic per-source queue key. Never includes a readable URL or title. */
object CatalogWorkIdentity {
    fun uniqueName(url: String, sha1Base36: String): String {
        require(FreeAudioCatalogPolicy.safeMediaUrl(url)) { "Untrusted catalog host" }
        require(FreeAudioIntegrity.isValidSourceHash(sha1Base36)) { "Missing source checksum" }
        val source = (url + "\n" + sha1Base36).toByteArray(StandardCharsets.UTF_8)
        val bytes = MessageDigest.getInstance("SHA-256").digest(source)
        return "nw23-" + bytes.take(18).joinToString("") {
            "%02x".format(it.toInt() and 0xff)
        }
    }
}
