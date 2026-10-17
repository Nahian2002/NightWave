package com.nightwave.player.offline

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/** Verified, app-private IDs only; no path, URI, YouTube stream or signed link is saved. */
data class OfflineSession(
    val ids: List<String>,
    val selectedId: String,
    val positionMs: Long
)

/**
 * Deterministic v1 session encoding. SHA-256 detects accidental corruption;
 * it is not an authentication mechanism (data lives in app-private preferences).
 */
object OfflineSessionCodec {
    private const val VERSION = "NWOS1"
    private const val MAX_BYTES = 24_000
    const val MAX_POSITION_MS = 7L * 24 * 60 * 60 * 1000
    private val safeId = Regex(
        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}" +
            "\\.(mp3|m4a|aac|flac|wav|ogg|opus|mp4|webm|3gp)"
    )

    fun isValid(session: OfflineSession): Boolean {
        val ids = session.ids
        return ids.isNotEmpty() && ids.size <= OfflinePlaybackQueue.MAX_ITEMS &&
            ids.size == ids.distinct().size &&
            ids.all { safeId.matches(it) } && session.selectedId in ids &&
            session.positionMs in 0L..MAX_POSITION_MS
    }

    private fun digest(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    fun encode(session: OfflineSession): String {
        require(isValid(session)) { "Invalid offline listening session" }
        val body = (listOf(VERSION, session.selectedId, session.positionMs.toString(),
            session.ids.size.toString()) + session.ids).joinToString("\n")
        val encoded = body + "\n" + digest(body)
        require(encoded.toByteArray(StandardCharsets.UTF_8).size <= MAX_BYTES)
        return encoded
    }

    fun decode(raw: String?): OfflineSession? = runCatching {
        if (raw.isNullOrBlank() ||
            raw.toByteArray(StandardCharsets.UTF_8).size > MAX_BYTES) return@runCatching null
        val lines = raw.split('\n')
        if (lines.size < 6 || lines[0] != VERSION) return@runCatching null
        val count = lines[3].toIntOrNull() ?: return@runCatching null
        if (count !in 1..OfflinePlaybackQueue.MAX_ITEMS ||
            lines.size != count + 5) return@runCatching null
        val body = lines.dropLast(1).joinToString("\n")
        if (!digest(body).equals(lines.last(), ignoreCase = false)) return@runCatching null
        val position = lines[2].toLongOrNull() ?: return@runCatching null
        OfflineSession(lines.subList(4, 4 + count), lines[1], position)
            .takeIf(::isValid)
    }.getOrNull()
}
