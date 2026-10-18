package com.nightwave.player.v2.cloud

import com.nightwave.player.v2.lyrics.TimedLyricsCodec
import com.nightwave.player.v2.model.AudioQualityPreference
import com.nightwave.player.v2.model.MotionPreference
import com.nightwave.player.v2.model.ProfileSettings
import org.json.JSONObject
import java.util.Locale

/** Explicit allowlist. Never exports IDs, account, history-privacy or credentials. */
data class CloudPlaybackPreferences(
    val motion: MotionPreference,
    val quality: AudioQualityPreference,
    val crossfadeSeconds: Int,
    val lockscreenArtwork: Boolean,
    val keepScreenAwake: Boolean
) {
    fun validated(): CloudPlaybackPreferences {
        require(crossfadeSeconds in 0..12) { "Invalid crossfade" }
        return this
    }

    fun applyTo(local: ProfileSettings): ProfileSettings = local.copy(
        motion = motion, quality = quality, crossfadeSeconds = crossfadeSeconds,
        showArtworkOnLockscreen = lockscreenArtwork,
        keepScreenAwakeWhileBrowsing = keepScreenAwake
    ).normalized()

    fun toJson(): JSONObject = JSONObject().put("motion", motion.name)
        .put("quality", quality.name).put("crossfade", crossfadeSeconds)
        .put("artwork", lockscreenArtwork).put("awake", keepScreenAwake)

    companion object {
        fun from(local: ProfileSettings) = CloudPlaybackPreferences(
            local.motion, local.quality, local.crossfadeSeconds,
            local.showArtworkOnLockscreen, local.keepScreenAwakeWhileBrowsing
        ).validated()

        fun parse(json: JSONObject): CloudPlaybackPreferences {
            require(json.keys().asSequence().toSet() ==
                setOf("motion", "quality", "crossfade", "artwork", "awake"))
            return CloudPlaybackPreferences(
                MotionPreference.valueOf(json.getString("motion")),
                AudioQualityPreference.valueOf(json.getString("quality")),
                json.getInt("crossfade"),
                json.getBoolean("artwork"),
                json.getBoolean("awake")
            ).validated()
        }
    }
}

/** Local-only LRC is keyed to its existing private song ID, never a media path. */
data class CloudLyricEntry(val localSongId: String, val lrc: String)

object CloudLyricsPolicy {
    const val MAX_ENTRIES = 20
    const val MAX_TOTAL_BYTES = 512_000
    private val id = Regex("[0-9a-f-]{36}\\.(mp3|m4a|aac|flac|wav|ogg|opus|mp4|webm|3gp)")

    fun validated(input: List<CloudLyricEntry>): List<CloudLyricEntry> {
        require(input.size <= MAX_ENTRIES) { "Too many lyrics attachments" }
        require(input.map { it.localSongId }.distinct().size == input.size) {
            "Duplicated lyrics attachment"
        }
        var total = 0
        return input.map { entry ->
            require(id.matches(entry.localSongId)) { "Invalid local lyrics identifier" }
            val bytes = entry.lrc.toByteArray(Charsets.UTF_8)
            require(bytes.size in 1..TimedLyricsCodec.MAX_BYTES)
            total += bytes.size
            require(total <= MAX_TOTAL_BYTES) { "Lyrics backup exceeds safety limit" }
            TimedLyricsCodec.parse(bytes)
            entry
        }
    }
}
