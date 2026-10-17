package com.nightwave.player.v2.backup

import com.nightwave.player.core.VideoLink
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.v2.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Locale

/**
 * Strict, portable, metadata-only backup. No account tokens, signed streaming
 * URLs, local device identifiers, audio content or unapproved fields.
 *
 * JSON envelope v1: { kind, checksum_sha256, payload: String (JSON) }
 * SHA-256 detects accidental corruption, NOT malicious tampering/authenticity.
 */
data class MusicCollection(
    val favorites: List<VideoPreview>,
    val recent: List<VideoPreview>,
    val playlists: LinkedHashMap<String, List<VideoPreview>>
)
data class BackupPreferences(
    val displayName: String,
    val avatarStyle: String,
    val theme: NightWaveTheme,
    val motion: MotionPreference,
    val quality: AudioQualityPreference,
    val historyPrivacy: ListeningPrivacy,
    val crossfadeSeconds: Int,
    val lockscreenArtwork: Boolean,
    val stayAwake: Boolean
) {
    companion object {
        fun from(profile: ProfileSettings) = BackupPreferences(
            profile.displayName, profile.avatarStyle, profile.theme, profile.motion,
            profile.quality, profile.historyPrivacy, profile.crossfadeSeconds,
            profile.showArtworkOnLockscreen, profile.keepScreenAwakeWhileBrowsing
        )
    }
    fun onto(local: ProfileSettings): ProfileSettings = local.copy(
        displayName = displayName, avatarStyle = avatarStyle, theme = theme,
        motion = motion, quality = quality, historyPrivacy = historyPrivacy,
        crossfadeSeconds = crossfadeSeconds,
        showArtworkOnLockscreen = lockscreenArtwork,
        keepScreenAwakeWhileBrowsing = stayAwake
    ).normalized()
}
data class PortableBackup(
    val profile: BackupPreferences,
    val library: MusicCollection,
    val createdAtMs: Long,
    val includesHistory: Boolean
)

class InvalidBackupException(message: String) : IllegalArgumentException(message)

object BackupFormat {
    const val FORMAT_VERSION = 1
    const val MAX_BYTES = 10 * 1024 * 1024
    const val MAX_PLAYLISTS = 50
    const val MAX_TRACKS_PER_PLAYLIST = 300

    fun export(data: PortableBackup): ByteArray {
        validate(data)
        val profile = data.profile
        val prefs = JSONObject()
            .put("display_name", profile.displayName)
            .put("avatar_style", profile.avatarStyle)
            .put("theme", profile.theme.name)
            .put("motion", profile.motion.name)
            .put("quality", profile.quality.name)
            .put("history_privacy", profile.historyPrivacy.name)
            .put("crossfade_seconds", profile.crossfadeSeconds)
            .put("lockscreen_artwork", profile.lockscreenArtwork)
            .put("stay_awake", profile.stayAwake)
        val lists = JSONArray()
        data.library.playlists.forEach { (name, tracks) ->
            lists.put(JSONObject().put("name", name).put("tracks", videos(tracks)))
        }
        val collection = JSONObject()
            .put("favorites", videos(data.library.favorites))
            .put("playlists", lists)
        if (data.includesHistory) collection.put("recent", videos(data.library.recent))
        val payload = JSONObject()
            .put("format_version", FORMAT_VERSION)
            .put("created_at_ms", data.createdAtMs)
            .put("history_included", data.includesHistory)
            .put("profile", prefs)
            .put("library", collection)
            .toString()
        val checksum = sha256(payload.toByteArray(Charsets.UTF_8))
        val output = JSONObject()
            .put("kind", "nightwave-guest-backup")
            .put("checksum_sha256", checksum)
            .put("payload", payload).toString().toByteArray(Charsets.UTF_8)
        if (output.size > MAX_BYTES) throw InvalidBackupException("Backup exceeds the supported size limit.")
        return output
    }

    fun import(raw: ByteArray): PortableBackup {
        if (raw.isEmpty() || raw.size > MAX_BYTES) throw InvalidBackupException("Invalid backup size.")
        val envelope = runCatching { JSONObject(String(raw, Charsets.UTF_8)) }
            .getOrElse { throw InvalidBackupException("Backup is not valid JSON.") }
        requireKeys(envelope, setOf("kind", "checksum_sha256", "payload"))
        if (envelope.optString("kind") != "nightwave-guest-backup") {
            throw InvalidBackupException("Not a NightWave guest backup.")
        }
        val payloadText = envelope.optString("payload")
        val expected = envelope.optString("checksum_sha256")
        if (expected.length != 64 || !MessageDigest.isEqual(
                expected.lowercase(Locale.ROOT).toByteArray(Charsets.US_ASCII),
                sha256(payloadText.toByteArray(Charsets.UTF_8)).toByteArray(Charsets.US_ASCII)
            )) throw InvalidBackupException("Backup checksum mismatch or corrupted file.")
        val payload = runCatching { JSONObject(payloadText) }
            .getOrElse { throw InvalidBackupException("Backup content is invalid.") }
        requireKeys(payload, setOf("format_version", "created_at_ms", "history_included", "profile", "library"))
        if (payload.optInt("format_version", -1) != FORMAT_VERSION)
            throw InvalidBackupException("Unsupported backup version.")
        val createdAt = payload.optLong("created_at_ms", -1)
        if (createdAt < 0) throw InvalidBackupException("Invalid backup timestamp.")
        val hasHistory = payload.optBoolean("history_included", false)
        val rawProfile = payload.optJSONObject("profile") ?: throw InvalidBackupException("Missing profile.")
        val profileKeys = setOf("display_name", "avatar_style", "theme", "motion", "quality",
            "history_privacy", "crossfade_seconds", "lockscreen_artwork", "stay_awake")
        requireKeys(rawProfile, profileKeys)
        val preferences = try {
            BackupPreferences(
                displayName = rawProfile.getString("display_name"),
                avatarStyle = rawProfile.getString("avatar_style"),
                theme = NightWaveTheme.valueOf(rawProfile.getString("theme")),
                motion = MotionPreference.valueOf(rawProfile.getString("motion")),
                quality = AudioQualityPreference.valueOf(rawProfile.getString("quality")),
                historyPrivacy = ListeningPrivacy.valueOf(rawProfile.getString("history_privacy")),
                crossfadeSeconds = rawProfile.getInt("crossfade_seconds"),
                lockscreenArtwork = rawProfile.getBoolean("lockscreen_artwork"),
                stayAwake = rawProfile.getBoolean("stay_awake")
            )
        } catch (_: Exception) {
            throw InvalidBackupException("Profile settings are invalid.")
        }
        val collection = payload.optJSONObject("library") ?: throw InvalidBackupException("Missing music library.")
        requireKeys(collection, if (hasHistory) setOf("favorites", "recent", "playlists") else setOf("favorites", "playlists"))
        val favorites = parseVideos(collection.optJSONArray("favorites") ?: throw InvalidBackupException("Missing favorites."), 500)
        val recent = if (hasHistory) parseVideos(
            collection.optJSONArray("recent") ?: throw InvalidBackupException("Missing history."), 100
        ) else emptyList()
        val rawPlaylists = collection.optJSONArray("playlists") ?: throw InvalidBackupException("Missing playlists.")
        if (rawPlaylists.length() > MAX_PLAYLISTS) throw InvalidBackupException("Too many playlists.")
        val playlists = linkedMapOf<String, List<VideoPreview>>()
        for (i in 0 until rawPlaylists.length()) {
            val item = rawPlaylists.optJSONObject(i) ?: throw InvalidBackupException("Invalid playlist.")
            requireKeys(item, setOf("name", "tracks"))
            val name = item.optString("name")
            if (name.isBlank() || name.length > 60 || playlists.keys.any { it.equals(name, true) })
                throw InvalidBackupException("Invalid or duplicate playlist name.")
            playlists[name] = parseVideos(item.optJSONArray("tracks")
                ?: throw InvalidBackupException("Invalid playlist songs."), MAX_TRACKS_PER_PLAYLIST)
        }
        val data = PortableBackup(preferences, MusicCollection(favorites, recent, LinkedHashMap(playlists)),
            createdAt, hasHistory)
        validate(data)
        return data
    }

    private fun videos(list: List<VideoPreview>) = JSONArray().apply {
        for (song in list) put(JSONObject()
            .put("url", song.url).put("title", song.title).put("author", song.author)
            .put("thumbnail", song.thumbnail).put("duration", song.durationSeconds))
    }

    private fun parseVideos(items: JSONArray, maximum: Int): List<VideoPreview> {
        if (items.length() > maximum) throw InvalidBackupException("Too many tracks in collection.")
        val result = ArrayList<VideoPreview>(items.length())
        val seen = HashSet<String>()
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: throw InvalidBackupException("Invalid track record.")
            requireKeys(item, setOf("url", "title", "author", "thumbnail", "duration"))
            val url = VideoLink.normalize(item.optString("url"))
                ?: throw InvalidBackupException("Unsupported video URL in backup.")
            if (!seen.add(url)) throw InvalidBackupException("Duplicate tracks in backup.")
            val title = item.optString("title")
            val author = item.optString("author")
            val image = item.optString("thumbnail")
            val seconds = item.optLong("duration", -1)
            if (title.length > 250 || author.length > 150 || image.length > 2000 ||
                seconds !in 0..86400 || !validArtwork(image))
                throw InvalidBackupException("Invalid track metadata.")
            result.add(VideoPreview(title, author, url, image, seconds))
        }
        return result
    }

    private fun validate(data: PortableBackup) {
        if (data.profile.displayName.length > 40 || data.profile.displayName.isBlank() ||
            data.profile.avatarStyle !in ProfileSettings.allowedAvatars ||
            data.profile.crossfadeSeconds !in 0..12)
            throw InvalidBackupException("Profile metadata is out of range.")
        if (data.library.favorites.size > 500 || data.library.recent.size > 100 ||
            data.library.playlists.size > MAX_PLAYLISTS)
            throw InvalidBackupException("Library size exceeds safe limits.")
        if (!data.includesHistory && data.library.recent.isNotEmpty())
            throw InvalidBackupException("History was not authorized for backup.")
        for ((name, videos) in data.library.playlists) {
            if (name.isBlank() || name.length > 60 || videos.size > MAX_TRACKS_PER_PLAYLIST)
                throw InvalidBackupException("Invalid playlist.")
        }
        // Re-run decoder validation for export data (never include unsafe URLs or signed links).
        for (list in listOf(data.library.favorites, data.library.recent) + data.library.playlists.values) {
            val ids = HashSet<String>()
            for (video in list) {
                val canonical = VideoLink.normalize(video.url)
                if (canonical == null || !ids.add(canonical) ||
                    video.title.length > 250 || video.author.length > 150 ||
                    video.durationSeconds !in 0..86400 || !validArtwork(video.thumbnail))
                    throw InvalidBackupException("Invalid track in library.")
            }
        }
    }

    private fun validArtwork(url: String): Boolean {
        if (url.isBlank()) return true
        if (url.length > 2000 || !url.startsWith("https://")) return false
        val host = runCatching { java.net.URI(url).host?.lowercase(Locale.ROOT) }.getOrNull() ?: return false
        return host == "i.ytimg.com" || host == "img.youtube.com" ||
            host.endsWith(".ytimg.com") || host == "yt3.ggpht.com"
    }

    private fun requireKeys(value: JSONObject, allowed: Set<String>) {
        val keys = value.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key !in allowed) throw InvalidBackupException("Unexpected field in backup.")
        }
        if (allowed.any { !value.has(it) }) throw InvalidBackupException("Missing backup field.")
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
