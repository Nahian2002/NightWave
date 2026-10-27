package com.nightwave.player.v2.support

import org.json.JSONObject
import java.io.InputStream
import java.security.MessageDigest

/** Build-only metadata. Revision is context; the complete byte digest identifies the source snapshot. */
class BuildIdentity private constructor(
    val variant: String, val sourceSha256: String, val inputCount: Int, val revisionContext: String
) {
    val displayId: String get() = "NW-$variant-${sourceSha256.take(16)}"

    fun supportText(versionName: String, versionCode: Long, apkSha256: String? = null,
        signerSha256: List<String> = emptyList(), splitCount: Int = 0): String {
        val validHash = Regex("[A-F0-9]{64}")
        val apk = apkSha256?.takeIf { validHash.matches(it) } ?: "unavailable"
        val signers = signerSha256.filter { validHash.matches(it) }.distinct().sorted()
            .joinToString(", ").ifEmpty { "unavailable" }
        return "NightWave ${versionName.take(64)} ($versionCode)\n" +
            "Build ID: $displayId\nSource SHA256: $sourceSha256\n" +
            "Source inputs: $inputCount\nRevision context: $revisionContext\n" +
            (if (splitCount > 0) "Base APK SHA256: $apk\nSplit APK count: $splitCount (base hash only)\n"
                else "APK SHA256: $apk\n") +
            "Installed signer SHA256: $signers\n" +
            "Identity describes this app build; it does not certify device or account acceptance."
    }

    companion object {
        const val MAX_BYTES = 4096
        fun read(input: InputStream): BuildIdentity? = runCatching {
            val bytes = ByteArray(MAX_BYTES + 1)
            var size = 0
            var emptyReads = 0
            while (size < bytes.size) {
                val count = input.read(bytes, size, bytes.size - size)
                if (count < 0) break
                if (count == 0) {
                    if (++emptyReads >= 8) return null
                } else {
                    emptyReads = 0
                    size += count
                }
            }
            if (size > MAX_BYTES) null else decode(String(bytes, 0, size, Charsets.UTF_8))
        }.getOrNull()
        fun decode(text: String): BuildIdentity? = runCatching {
            require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES)
            val obj = JSONObject(text)
            require(obj.opt("schema") == 1 && obj.opt("algorithm") == "NightWave-source-v1")
            val variant = obj.opt("variant") as? String ?: return null
            require(variant == "debug" || variant == "release")
            val hash = obj.opt("sourceSha256") as? String ?: return null
            require(hash.matches(Regex("[A-F0-9]{64}")))
            val count = obj.opt("inputCount")
            require(count is Int && count in 50..100_000)
            val revision = obj.opt("revisionContext") as? String ?: return null
            require(revision == "unavailable" || revision.matches(Regex("[0-9a-f]{40}")))
            BuildIdentity(variant, hash, count, revision)
        }.getOrNull()
    }
}

/** Bounded streaming hash for the installed base APK or its public certificate bytes. */
internal object InstalledArtifactHash {
    fun read(input: InputStream, maxBytes: Long = 512L * 1024 * 1024): String? = runCatching {
        require(maxBytes in 1..(512L * 1024 * 1024))
        val hash = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var total = 0L
        var emptyReads = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count == 0) {
                if (++emptyReads >= 8) return null
                continue
            }
            emptyReads = 0
            total += count
            if (total > maxBytes) return null
            hash.update(buffer, 0, count)
        }
        hash.digest().joinToString("") { "%02X".format(it.toInt() and 255) }
    }.getOrNull()
}
