package com.nightwave.player.offline

import com.nightwave.player.core.VideoLink
import org.json.JSONObject

internal data class OfflineTrackIdentity(val videoId: String, val title: String, val creator: String)

/** Optional metadata only; never changes the audio index, IDs or original URL. */
internal object OfflineTrackIdentityPolicy {
    private val id = Regex("[A-Za-z0-9_-]{11}")
    fun from(url: String, title: String, creator: String): OfflineTrackIdentity? {
        val video = VideoLink.normalize(url)?.substringAfter("v=") ?: return null
        if (!id.matches(video)) return null
        fun label(value: String, limit: Int) = value.filterNot { it.isISOControl() }.trim().take(limit)
        val cleanTitle = label(title, 320)
        if (cleanTitle.isBlank()) return null
        return OfflineTrackIdentity(video, cleanTitle, label(creator, 180))
    }
    fun encode(value: OfflineTrackIdentity): String = JSONObject().put("version", 1)
        .put("video", value.videoId).put("title", value.title).put("creator", value.creator).toString()
    fun decode(raw: String?): OfflineTrackIdentity? = runCatching {
        if (raw == null || raw.length > 4096) return null
        val json = JSONObject(raw)
        if (json.getInt("version") != 1 || !id.matches(json.getString("video"))) return null
        from("https://www.youtube.com/watch?v=" + json.getString("video"),
            json.getString("title"), json.getString("creator"))
    }.getOrNull()
}
