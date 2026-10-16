package com.nightwave.player.v2.model

/** Backup-safe profile settings; no account tokens, emails or signed audio URLs. */
enum class NightWaveTheme { MIDNIGHT, AURORA, PEARL, AMOLED }
enum class MotionPreference { SYSTEM, FULL, REDUCED }
enum class AudioQualityPreference { AUTO, DATA_SAVER, BALANCED, HIGH }
enum class ListeningPrivacy { DEVICE_ONLY, INCLUDE_IN_BACKUP }

data class ProfileSettings(
    val localId: String,
    val displayName: String = "NightWave Listener",
    val avatarStyle: String = "orbit",
    val theme: NightWaveTheme = NightWaveTheme.MIDNIGHT,
    val motion: MotionPreference = MotionPreference.SYSTEM,
    val quality: AudioQualityPreference = AudioQualityPreference.AUTO,
    val historyPrivacy: ListeningPrivacy = ListeningPrivacy.DEVICE_ONLY,
    val crossfadeSeconds: Int = 0,
    val showArtworkOnLockscreen: Boolean = true,
    val keepScreenAwakeWhileBrowsing: Boolean = false
) {
    fun normalized(): ProfileSettings = copy(
        displayName = displayName.trim().take(40).ifBlank { "NightWave Listener" },
        avatarStyle = avatarStyle.takeIf { it in allowedAvatars } ?: "orbit",
        crossfadeSeconds = crossfadeSeconds.coerceIn(0, 12)
    )

    companion object {
        val allowedAvatars = setOf("orbit", "wave", "prism", "moon", "vinyl", "spark")
    }
}

enum class AccountMode { GUEST, GOOGLE_CONNECTED }

/** Explicit merge decision required before guest and cloud libraries touch each other. */
enum class RestoreChoice { KEEP_LOCAL, RESTORE_CLOUD, MERGE_WITH_CONFIRMATION }

data class BackupPlan(
    val formatVersion: Int,
    val localRevision: Long,
    val cloudRevision: Long?,
    val hasUnuploadedChanges: Boolean,
    val restoreChoice: RestoreChoice? = null
) {
    fun requiresUserChoice(): Boolean =
        cloudRevision != null && (hasUnuploadedChanges || localRevision > 0L) && restoreChoice == null
}

object ProfilePolicy {
    const val BACKUP_FORMAT_VERSION = 1
    const val MAX_DISPLAY_NAME_LENGTH = 40
    /** Never place OAuth tokens, passwords, Google identifiers or temporary streaming URLs in backups. */
    val prohibitedBackupFields = setOf(
        "access_token", "refresh_token", "password", "id_token",
        "session_cookie", "audio_url", "stream_url", "oauth_code"
    )

    fun isBackupFieldAllowed(key: String): Boolean =
        key.lowercase() !in prohibitedBackupFields &&
            !key.contains("token", ignoreCase = true) &&
            !key.contains("password", ignoreCase = true) &&
            !key.contains("cookie", ignoreCase = true)
}
