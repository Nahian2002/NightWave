package com.nightwave.player.v2.ui

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.annotation.PluralsRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.nightwave.player.R
import com.nightwave.player.v2.playback.PlaybackHealth
import com.nightwave.player.v2.playback.PlaybackPhase
import com.nightwave.player.v2.cloud.CloudOperationPhase
import java.util.Locale

/** Resource reader captured during composition; safe in callbacks/semantics lambdas. */
internal class NightWaveStrings(private val resources: Resources) {
    private val locale: Locale = resources.configuration.locales[0] ?: Locale.ENGLISH
    fun get(@StringRes id: Int, vararg args: Any): String =
        if (args.isEmpty()) resources.getString(id) else resources.getString(id, *args)
    fun quantity(@PluralsRes id: Int, count: Int): String =
        resources.getQuantityString(id, count.coerceAtLeast(0), number(count.coerceAtLeast(0)))
    fun number(value: Int): String = NightWaveLocaleFormat.number(value.toLong(), locale)
    fun duration(ms: Long): String = NightWaveLocaleFormat.duration(ms, locale)
    fun date(ms: Long): String = java.text.DateFormat.getDateTimeInstance(
        java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT, locale).format(java.util.Date(ms))
    fun queueAnnouncement(number: Int, title: String, artist: String, playing: Boolean): String =
        get(if (playing) R.string.nw141_queue_playing else R.string.nw141_queue_play,
            this.number(number.coerceAtLeast(1)), title.ifBlank { get(R.string.nw141_untitled_track) },
            if (artist.isBlank()) "" else get(R.string.nw141_artist_suffix, artist))
    fun playbackStatus(health: PlaybackHealth): String = get(when (health.phase) {
        PlaybackPhase.IDLE -> R.string.nw141_phase_idle
        PlaybackPhase.DISCONNECTED -> R.string.nw141_phase_disconnected
        PlaybackPhase.ERROR -> R.string.nw141_phase_error
        PlaybackPhase.LOOKING_UP -> R.string.nw141_phase_lookup
        PlaybackPhase.BUFFERING -> R.string.nw141_phase_buffering
        PlaybackPhase.PLAYING -> R.string.nw141_phase_playing
        PlaybackPhase.PAUSED -> R.string.nw141_phase_paused
        PlaybackPhase.READY -> R.string.nw141_phase_ready
    })
    fun likeStatus(liked: Boolean, offline: Boolean): String = get(when {
        offline -> R.string.nw141_liked_offline
        liked -> R.string.nw141_liked_yes
        else -> R.string.nw141_liked_no
    })
    fun greeting(hour: Int): String = get(when (hour) {
        in 5..11 -> R.string.nw141_greeting_morning
        in 12..17 -> R.string.nw141_greeting_afternoon
        else -> R.string.nw141_greeting_evening
    })
    fun lyricsSubtitle(offline: Boolean, importedLyrics: Boolean): String = get(when {
        importedLyrics -> R.string.nw141_lyrics_ready
        !offline -> R.string.nw141_lyrics_attach
        else -> R.string.nw141_lyrics_import
    })
    fun recoveryAction(connected: Boolean, needsReview: Boolean, hasBackup: Boolean): String = get(when {
        !connected -> R.string.nw141_backup_connect
        needsReview -> R.string.nw141_backup_reconnect
        !hasBackup -> R.string.nw141_backup_first
        else -> R.string.nw141_backup_now
    })
    fun protectionTitle(connected: Boolean, needsReview: Boolean, hasBackup: Boolean,
        needsRefresh: Boolean): String = get(when {
        needsReview && connected -> R.string.nw141_protect_attention
        !connected -> R.string.nw141_protect_local
        !hasBackup -> R.string.nw141_protect_create
        needsRefresh -> R.string.nw141_protect_refresh
        else -> R.string.nw141_protect_verified
    })
    fun cloudBusy(phase: CloudOperationPhase): String = get(when (phase) {
        CloudOperationPhase.IDLE -> R.string.nw141_cloud_idle
        CloudOperationPhase.AUTHORIZING -> R.string.nw141_cloud_authorizing
        CloudOperationPhase.AWAITING_PERMISSION -> R.string.nw141_cloud_awaiting_permission
        CloudOperationPhase.CHECKING_ACCOUNT -> R.string.nw141_cloud_checking_account
        CloudOperationPhase.PREPARING_BACKUP -> R.string.nw141_cloud_preparing_backup
        CloudOperationPhase.TRANSFERRING_BACKUP -> R.string.nw141_cloud_transferring_backup
        CloudOperationPhase.LISTING_BACKUPS -> R.string.nw141_cloud_listing_backups
        CloudOperationPhase.OPENING_BACKUP -> R.string.nw141_cloud_opening_backup
        CloudOperationPhase.MERGING -> R.string.nw141_cloud_merging
        CloudOperationPhase.RESTORING -> R.string.nw141_cloud_restoring
        CloudOperationPhase.APPLYING_OPTIONS -> R.string.nw141_cloud_applying_options
    })

}

@Composable
internal fun rememberNightWaveStrings(): NightWaveStrings {
    val context = LocalContext.current
    // Reading configuration invalidates this reader on system/per-app locale changes.
    val configuration = LocalConfiguration.current
    return remember(context, configuration) { NightWaveStrings(context.resources) }
}
