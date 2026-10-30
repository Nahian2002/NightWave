package com.nightwave.player.v2.ui

import com.nightwave.player.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.AlertDialog
import com.nightwave.player.v2.ui.NightWaveMatteButton as Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.nightwave.player.v2.ui.NightWaveMatteOutlinedButton as OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import com.nightwave.player.v2.ui.NightWaveMatteTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import com.nightwave.player.v2.cloud.InstalledApkOAuthIdentity
import com.nightwave.player.v2.cloud.CloudAuthorizationFailureStore
import com.nightwave.player.v2.cloud.CloudAuthorizationFailurePolicy
import com.nightwave.player.v2.cloud.CloudConnectionHelpPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nightwave.player.v2.cloud.CloudOperationPhase
import com.nightwave.player.v2.cloud.CloudRecoveryUiPolicy
import com.nightwave.player.v2.cloud.CloudAccountPresentation
import com.nightwave.player.v2.cloud.CloudBackupStatus
import com.nightwave.player.v2.cloud.CloudAccessPolicy
import com.nightwave.player.v2.cloud.CloudAccessState
import com.nightwave.player.v2.model.ProfileSettings

/** Local-first account overview. Drive scope authorization is not Google identity sign-in. */
@Composable
internal fun NightWaveAccountHubDialog(
    profile: ProfileSettings,
    cloud: CloudBackupStatus,
    busy: Boolean,
    phase: CloudOperationPhase = CloudOperationPhase.IDLE,
    onDismiss: () -> Unit,
    onConnect: () -> Unit,
    onBackUp: () -> Unit,
    onPreviewRestore: () -> Unit,
    onCompare: () -> Unit,
    onDisconnect: () -> Unit,
    onExportLocal: () -> Unit,
    onHistory: (Boolean) -> Unit,
    onLyrics: (Boolean) -> Unit,
    onPreferences: (Boolean) -> Unit,
    onWifiOnly: (Boolean) -> Unit
) {
    val strings = rememberNightWaveStrings()
    var confirmDisconnect by remember { mutableStateOf(false) }
    var confirmChangeAccount by remember { mutableStateOf(false) }
    var showBackupOptions by remember { mutableStateOf(false) }
    var showConnectionHelp by remember { mutableStateOf(false) }
    var showSetupDetails by remember { mutableStateOf(false) }
    // File pickers and native restore previews must not stack beneath a
    // second persistent dialog window; close this hub before opening them.
    fun openExternalFlow(callback: () -> Unit) {
        onDismiss()
        callback()
    }
    val stage = CloudAccountPresentation.stage(cloud)
    val connected = cloud.account.isNotBlank()
    val needsReview = cloud.accessState == CloudAccessState.NEEDS_REVIEW
    val canBackUp = CloudAccountPresentation.canBackUp(cloud, busy) && !needsReview
    val layout = AccountRecoveryPresentationPolicy.layout(
        LocalConfiguration.current.screenWidthDp * .96f, LocalDensity.current.fontScale)
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val appContext = LocalContext.current
    val apkFingerprint = remember(appContext) {
        InstalledApkOAuthIdentity.signingSha1(appContext)
    }
    var oauthCopied by remember { mutableStateOf(false) }
    val lastGoogleFailure = remember(cloud.lastResult, appContext) {
        CloudAuthorizationFailureStore(appContext).read()
    }
    val connectionGuidance = remember(lastGoogleFailure) {
        lastGoogleFailure?.let { CloudConnectionHelpPolicy.guidance(it.stage, it.code) }
    }

    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(.96f).fillMaxHeight(.92f)
                .testTag("account-backup-hub"),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            BoxWithConstraints {
            val headerLimit = NightWaveAdaptiveViewportPolicy.headerMaxHeightDp(maxHeight.value).dp
            Column {
                Box(Modifier.heightIn(max = headerLimit).verticalScroll(rememberScrollState())) {
                Row(Modifier.fillMaxWidth().padding(
                    start = 19.dp, end = 11.dp, top = 17.dp, bottom = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("NIGHTWAVE", color = primary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold)
                        Text(strings.get(R.string.nw141_account_backup_008a1), style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onDismiss) { Text(strings.get(R.string.nw141_close_7d9eb)) }
                }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().testTag("nw139-hub-content"),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = layout.paddingDp.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    item(key = "account-local-profile") {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(layout.paddingDp.dp),
                                verticalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                    Surface(shape = CircleShape,
                                        color = primary.copy(alpha = .13f),
                                        border = BorderStroke(1.dp, primary.copy(alpha = .22f))) {
                                        Icon(Icons.Outlined.PersonOutline,
                                            contentDescription = null, tint = primary,
                                            modifier = Modifier.size(layout.avatarDp.dp).padding(10.dp))
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(profile.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Text(strings.get(R.string.nw141_your_listening_profile_on_this_device_bfd75),
                                            color = muted,
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Text(strings.get(R.string.nw141_local_listening_works_without_a_google_account_af362),
                                    color = muted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    item(key = "account-drive-recovery") {
                        Card(shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                    Icon(if (stage == CloudAccountPresentation.BackupStage.VERIFIED_BACKUP)
                                        Icons.Outlined.CloudDone else Icons.Outlined.Cloud,
                                        contentDescription = null, tint = primary,
                                        modifier = Modifier.size(24.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(if (needsReview) strings.get(R.string.nw141_drive_needs_attention_ead38) else CloudAccountPresentation.title(stage),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold)
                                        Text(strings.get(R.string.nw141_library_metadata_backup_audio_files_stay_on_this_a80a5),
                                            color = muted,
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (busy) CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                }
                                if (connected) {
                                    Surface(shape = RoundedCornerShape(10.dp),
                                        color = if (needsReview) MaterialTheme.colorScheme.errorContainer
                                            else primary.copy(alpha = .10f)) {
                                        Text(CloudAccessPolicy.label(connected, cloud.accessState, cloud.lastAccessCheckMs),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                                .semantics { liveRegion = LiveRegionMode.Polite },
                                            color = if (needsReview) MaterialTheme.colorScheme.onErrorContainer else primary,
                                            style = MaterialTheme.typography.labelMedium)
                                    }
                                    Text(strings.get(R.string.nw141_saved_account, CloudAccountPresentation.maskedAccount(cloud.account)),
                                        style = MaterialTheme.typography.bodyMedium)
                                    if (cloud.lastAccessCheckMs > 0L) Text(
                                        strings.get(R.string.nw141_checked_date, strings.date(cloud.lastAccessCheckMs)),
                                        color = muted, style = MaterialTheme.typography.bodySmall)
                                }
                                if (cloud.lastSuccessMs > 0L &&
                                    stage in setOf(
                                        CloudAccountPresentation.BackupStage.VERIFIED_BACKUP,
                                        CloudAccountPresentation.BackupStage.PREVIOUS_BACKUP_NEEDS_REFRESH)) {
                                    val prior = stage ==
                                        CloudAccountPresentation.BackupStage.PREVIOUS_BACKUP_NEEDS_REFRESH
                                    Text(strings.get(if (prior) R.string.nw141_prior_date else R.string.nw141_verified_date, strings.date(cloud.lastSuccessMs)),
                                        color = muted,
                                        style = MaterialTheme.typography.bodySmall)
                                    if (prior) Text(
                                        strings.get(R.string.nw141_backup_stale),
                                        color = muted,
                                        style = MaterialTheme.typography.bodySmall)
                                }
                                Text(if (busy) strings.cloudBusy(phase)
                                    else cloud.lastResult.ifBlank { strings.get(R.string.nw141_no_backup_activity_recorded_c39fa) },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = muted,
                                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                                        .testTag("nw139-operation-status"))
                                Button(
                                    onClick = {
                                        if (connected && !needsReview) openExternalFlow(onBackUp)
                                        else openExternalFlow(onConnect)
                                    },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)
                                        .testTag("nw139-primary-recovery"),
                                    enabled = !busy
                                ) {
                                    Text(if (busy) strings.cloudBusy(phase) else
                                        strings.recoveryAction(
                                            connected, needsReview, cloud.lastSuccessMs > 0L))
                                }
                                if (connected) {
                                    OutlinedButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = { openExternalFlow(onPreviewRestore) },
                                        enabled = canBackUp
                                    ) { Text(strings.get(R.string.nw141_preview_a_restore_96277)) }
                                    OutlinedButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = { openExternalFlow(onCompare) },
                                        enabled = canBackUp
                                    ) { Text(strings.get(R.string.nw141_compare_merge_libraries_7e0d2)) }
                                    if (needsReview) Text(strings.get(R.string.nw141_reconnect_to_use_drive_recovery_your_local_libra_4248a),
                                        style = MaterialTheme.typography.bodySmall, color = muted)
                                    TextButton(enabled = !busy,
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                        onClick = { confirmChangeAccount = true }) { Text(strings.get(R.string.nw141_change_drive_account_2acb6)) }
                                    TextButton(
                                        enabled = !busy,
                                        onClick = { confirmDisconnect = true }
                                    ) { Text(strings.get(R.string.nw141_disconnect_drive_on_this_device_1072e)) }
                                }
                            }
                        }
                    }
                    item(key = "nw139-local-export") {
                        OutlinedButton(enabled = !busy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            onClick = { openExternalFlow(onExportLocal) }) {
                            Icon(Icons.Outlined.LibraryMusic, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.size(7.dp))
                            Text(strings.get(R.string.nw141_export_local_library_28753))
                        }
                        Text(strings.get(R.string.nw141_library_metadata_and_selected_options_only_audio_51ad4),
                            style = MaterialTheme.typography.bodySmall, color = muted)
                    }
                    item(key = "nw139-backup-options") {
                        OutlinedButton(onClick = { showBackupOptions = !showBackupOptions },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("nw139-backup-options")) {
                            Text(if (showBackupOptions) strings.get(R.string.nw141_hide_backup_options_20eff) else strings.get(R.string.nw141_backup_options_42799))
                        }
                        Text(strings.get(R.string.nw141_history_imported_lyrics_playback_preferences_and_778e0),
                            style = MaterialTheme.typography.bodySmall, color = muted)
                    }
                    if (showBackupOptions) {
                        item {
                            Text(strings.get(R.string.nw141_backup_preferences_d06bb), style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                        }
                        item {
                            AccountBackupToggle(strings.get(R.string.nw141_listening_history_efe6b),
                                strings.get(R.string.nw141_optional_off_by_default_f230e), cloud.includeHistory,
                                connected && !busy, onHistory)
                        }
                        item {
                            AccountBackupToggle(strings.get(R.string.nw141_imported_lyrics_53d8c),
                                strings.get(R.string.nw141_matched_local_lrc_only_public_video_lyrics_stay_ab9b2), cloud.includeLyrics,
                                connected && !busy, onLyrics)
                        }
                        item {
                            AccountBackupToggle(strings.get(R.string.nw141_playback_preferences_1969a),
                                strings.get(R.string.nw141_does_not_restore_account_permissions_or_privacy_42ead),
                                cloud.includePreferences, connected && !busy, onPreferences)
                        }
                        item {
                            AccountBackupToggle(strings.get(R.string.nw141_automatic_backups_on_wi_fi_f66af),
                                strings.get(R.string.nw141_after_a_verified_backup_scheduling_is_approximat_38a54),
                                cloud.wifiOnly, connected && !busy, onWifiOnly)
                        }
                        item {
                            Text(strings.get(R.string.nw141_explanation_453e36e6df),
                                color = muted,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    item(key = "nw139-connection-help") {
                        TextButton(onClick = { showConnectionHelp = !showConnectionHelp },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("nw139-connection-help")) {
                            Text(if (showConnectionHelp) strings.get(R.string.nw141_hide_connection_help_dcf0f) else strings.get(R.string.nw141_connection_help_b2814))
                        }
                    }
                    if (showConnectionHelp || CloudAccessPolicy.needsHelp(connected, cloud.accessState, lastGoogleFailure != null)) {
                        item(key = "google-connection-help") {
                            Card(shape = RoundedCornerShape(NightWavePremiumDesignTokens.CARD_CORNER_DP.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Column(Modifier.fillMaxWidth().padding(15.dp),
                                    verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                    Text(if (lastGoogleFailure == null && !connected)
                                        "Set up Google Drive backup" else "Google connection help",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold)
                                    Text(if (lastGoogleFailure == null && connected && cloud.accessState == CloudAccessState.CHECKED)
                                        "Drive access was checked. Review setup details if a later connection fails."
                                    else if (lastGoogleFailure == null && connected)
                                        "Your saved Drive account needs an access check. Reconnect or review connection details."
                                    else if (lastGoogleFailure == null)
                                        "Connect your Google account to back up library metadata. " +
                                            strings.get(R.string.nw141_your_local_music_stays_available_without_google_452fb)
                                    else CloudAuthorizationFailurePolicy.shortMessage(
                                        lastGoogleFailure.code, lastGoogleFailure.stage),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = muted)
                                    if (lastGoogleFailure != null) {
                                        Text("Step: " + CloudAuthorizationFailurePolicy.stageLabel(
                                            lastGoogleFailure.stage),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = muted)
                                    }
                                    connectionGuidance?.let { guidance ->
                                        Text(guidance.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold)
                                        Text(guidance.detail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = muted)
                                        Text(guidance.nextStep,
                                            style = MaterialTheme.typography.bodyMedium)
                                        if (guidance.offerSingleRetry) {
                                            OutlinedButton(
                                                enabled = !busy,
                                                modifier = Modifier.fillMaxWidth(),
                                                onClick = { openExternalFlow(onConnect) }
                                            ) {
                                                Text(strings.get(R.string.nw141_retry_google_connection_ee5a8))
                                            }
                                        }
                                    }
                                    TextButton(onClick = { showSetupDetails = !showSetupDetails },
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                        Text(if (showSetupDetails) strings.get(R.string.nw141_hide_connection_details_4376e) else strings.get(R.string.nw141_connection_details_51bff))
                                    }
                                    if (showSetupDetails) {
                                        Text(strings.get(R.string.nw141_installed_apk_signing_sha_1_3a846),
                                            style = MaterialTheme.typography.labelLarge)
                                        Text(apkFingerprint?.chunked(29)?.joinToString("\n")
                                                ?: strings.get(R.string.nw141_not_available_on_this_device_01c75),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace),
                                            color = primary)
                                        Text(strings.get(R.string.nw141_explanation_6053dac6e7),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = muted)
                                        OutlinedButton(
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = {
                                                oauthCopied = runCatching {
                                                    val clip = appContext.getSystemService(
                                                        Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clip.setPrimaryClip(ClipData.newPlainText(
                                                        "NightWave OAuth setup",
                                                        InstalledApkOAuthIdentity.report(appContext)))
                                                }.isSuccess
                                            }) {
                                            Text(if (oauthCopied)
                                                strings.get(R.string.nw141_setup_report_copied_7e002) else strings.get(R.string.nw141_copy_google_setup_report_66068))
                                        }
                                        TextButton(
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = {
                                                runCatching {
                                                    appContext.startActivity(
                                                        Intent(Intent.ACTION_VIEW,
                                                            Uri.parse("https://console.cloud.google.com/auth/clients"))
                                                    )
                                                }
                                            }) {
                                            Text(strings.get(R.string.nw141_open_google_oauth_settings_eec0c))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }

    if (confirmDisconnect) {
        AlertDialog(
            onDismissRequest = { confirmDisconnect = false },
            title = { Text(strings.get(R.string.nw141_disconnect_google_drive_ec257)) },
            text = { Text(strings.get(R.string.nw141_disconnect_explain)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDisconnect = false
                    onDisconnect()
                }) { Text(strings.get(R.string.nw141_disconnect_acfc5)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDisconnect = false }) { Text(strings.get(R.string.nw141_cancel_19766)) }
            }
        )
    }
    if (confirmChangeAccount) {
        AlertDialog(
            onDismissRequest = { confirmChangeAccount = false },
            title = { Text(strings.get(R.string.nw141_change_the_drive_account_f7d95)) },
            text = { Text(strings.get(R.string.nw141_change_explain)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmChangeAccount = false
                    openExternalFlow(onConnect)
                }) { Text(strings.get(R.string.nw141_continue_31fbe)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmChangeAccount = false }) { Text(strings.get(R.string.nw141_cancel_19766)) }
            }
        )
    }
}

@Composable
private fun AccountBackupToggle(
    title: String, explanation: String, enabled: Boolean,
    interactive: Boolean, onChange: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp)
        .toggleable(value = enabled, enabled = interactive, role = Role.Switch, onValueChange = onChange)
        .semantics(mergeDescendants = true) { }
        .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold)
            Text(explanation, color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = enabled, enabled = interactive, onCheckedChange = null)
    }
}
