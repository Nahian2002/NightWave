package com.nightwave.player.v2.lyrics

import com.nightwave.player.core.VideoLink
import java.security.MessageDigest
import java.util.Locale

/** Exact source identity only. A matching title never creates a binding. */
internal object LyricsTrackBindingPolicy {
    private val local = Regex("[0-9a-f-]{36}\\.(mp3|m4a|aac|flac|wav|ogg|opus|mp4|webm|3gp)")
    private val publicPattern = Regex("youtube:[A-Za-z0-9_-]{11}")
    fun isLocal(id: String): Boolean = local.matches(id)
    fun isPublic(id: String): Boolean = publicPattern.matches(id)
    fun storageId(mediaId: String): String? = when {
        mediaId.startsWith("offline:") -> mediaId.removePrefix("offline:").takeIf(::isLocal)
        VideoLink.normalize(mediaId) == mediaId -> ("youtube:" + mediaId.substringAfter("v="))
            .takeIf(::isPublic)
        else -> null
    }
    fun mediaId(id: String): String? = when {
        isLocal(id) -> "offline:$id"
        isPublic(id) -> "https://www.youtube.com/watch?v=" + id.removePrefix("youtube:")
        else -> null
    }
    fun matches(id: String, mediaId: String): Boolean = LyricsTrackBindingPolicy.mediaId(id) == mediaId
    fun sourceLabel(id: String): String = when {
        isLocal(id) -> "This on-device audio copy"
        isPublic(id) -> "YouTube video · " + id.removePrefix("youtube:") + " · Stored only on this device"
        else -> "Unsupported source"
    }
}

internal object OwnedLyricsContentPolicy {
    const val MAX_PUBLIC_TRACKS = 128
    const val MAX_PUBLIC_BYTES = 4 * 1024 * 1024
    fun normalized(bytes: ByteArray): String = LyricsEditorPolicy.serialize(
        LyricsEditorPolicy.begin(TimedLyricsCodec.parse(bytes))).toString(Charsets.UTF_8)
    fun fingerprint(text: String): String = MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }
    fun withinQuota(existing: Map<String, String>, id: String, replacement: String): Boolean {
        if (!LyricsTrackBindingPolicy.isPublic(id)) return false
        val nextCount = existing.size + if (id in existing) 0 else 1
        val bytes = existing.asSequence().filter { it.key != id }.sumOf { it.value.toByteArray(Charsets.UTF_8).size.toLong() } +
            replacement.toByteArray(Charsets.UTF_8).size
        return nextCount <= MAX_PUBLIC_TRACKS && bytes <= MAX_PUBLIC_BYTES
    }
}
