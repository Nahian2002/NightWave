package com.nightwave.player.v2.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Brush
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
import com.nightwave.player.v2.cloud.CloudAccountPresentation
import com.nightwave.player.v2.cloud.CloudBackupStatus
import com.nightwave.player.v2.cloud.CloudAccessPolicy
import com.nightwave.player.v2.cloud.CloudAccessState
import com.nightwave.player.v2.model.ProfileSettings
import java.text.DateFormat
import java.util.Date

/** Local-first account overview. Drive scope authorization is not Google identity sign-in. */
@Composable
internal fun NightWaveAccountHubDialog(
    profile: ProfileSettings,
    cloud: CloudBackupStatus,
    busy: Boolean,
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
    var confirmDisconnect by remember { mutableStateOf(false) }
    var confirmChangeAccount by remember { mutableStateOf(false) }
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
            tonalElevation = 5.dp
        ) {
            Column {
                Row(Modifier.fillMaxWidth().padding(
                    start = 19.dp, end = 11.dp, top = 17.dp, bottom = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("NIGHTWAVE", color = primary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold)
                        Text("Account & backup", style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
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
                                Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(
                                    primary.copy(alpha = .16f),
                                    MaterialTheme.colorScheme.surfaceVariant))).padding(layout.paddingDp.dp),
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
                                        Text("Your listening profile · on this device",
                                            color = muted,
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Text("Your music. Your library.",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold)
                                Text("Favorites, playlists and your profile stay on this device. " +
                                    "Add an optional recovery copy whenever you're ready.",
                                    color = muted,
                                    style = MaterialTheme.typography.bodySmall)
                                OutlinedButton(enabled = !busy,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                                    onClick = { openExternalFlow(onExportLocal) }) {
                                    Icon(Icons.Outlined.LibraryMusic, contentDescription = null,
                                        modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.size(7.dp))
                                    Text("Export local library")
                                }
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
                                        Text(AccountRecoveryPresentationPolicy.protectionTitle(
                                            connected, needsReview, cloud.lastSuccessMs > 0L,
                                            stage == CloudAccountPresentation.BackupStage.PREVIOUS_BACKUP_NEEDS_REFRESH),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold)
                                        Text(CloudAccountPresentation.details(stage),
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
                                    Text("Saved Google account: " +
                                        CloudAccountPresentation.maskedAccount(cloud.account),
                                        style = MaterialTheme.typography.bodyMedium)
                                    if (cloud.lastAccessCheckMs > 0L) Text(
                                        "Last account check · " + DateFormat.getDateTimeInstance().format(
                                            Date(cloud.lastAccessCheckMs)),
                                        color = muted, style = MaterialTheme.typography.bodySmall)
                                }
                                if (cloud.lastSuccessMs > 0L &&
                                    stage in setOf(
                                        CloudAccountPresentation.BackupStage.VERIFIED_BACKUP,
                                        CloudAccountPresentation.BackupStage.PREVIOUS_BACKUP_NEEDS_REFRESH)) {
                                    val prior = stage ==
                                        CloudAccountPresentation.BackupStage.PREVIOUS_BACKUP_NEEDS_REFRESH
                                    Text((if (prior) "Previous verified backup · "
                                        else "Last verified backup · ") +
                                        DateFormat.getDateTimeInstance().format(
                                            Date(cloud.lastSuccessMs)),
                                        color = muted,
                                        style = MaterialTheme.typography.bodySmall)
                                    if (prior) Text(
                                        "New privacy or backup settings aren't verified in that snapshot. " +
                                            "Choose Back up library now to refresh it.",
                                        color = muted,
                                        style = MaterialTheme.typography.bodySmall)
                                }
                                Text(cloud.lastResult.ifBlank { "No backup activity recorded." },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = muted)
                                Button(
                                    onClick = {
                                        if (connected && !needsReview) openExternalFlow(onBackUp)
                                        else openExternalFlow(onConnect)
                                    },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                                    enabled = !busy
                                ) {
                                    Text(if (busy) "Please wait…" else
                                        AccountRecoveryPresentationPolicy.primaryAction(
                                            connected, needsReview, cloud.lastSuccessMs > 0L))
                                }
                                if (connected) {
                                    OutlinedButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = { openExternalFlow(onPreviewRestore) },
                                        enabled = canBackUp
                                    ) { Text("Preview a restore") }
                                    OutlinedButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = { openExternalFlow(onCompare) },
                                        enabled = canBackUp
                                    ) { Text("Compare & merge libraries") }
                                    if (needsReview) Text("Reconnect to use Drive recovery. Your local library is ready.",
                                        style = MaterialTheme.typography.bodySmall, color = muted)
                                    TextButton(enabled = !busy,
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                        onClick = { confirmChangeAccount = true }) { Text("Change Drive account") }
                                    TextButton(
                                        enabled = !busy,
                                        onClick = { confirmDisconnect = true }
                                    ) { Text("Disconnect Drive on this device") }
                                }
                            }
                        }
                    }
                    item {
                        Text("Backup preferences", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                    }
                    item {
                        AccountBackupToggle("Listening history",
                            "Optional · off by default", cloud.includeHistory,
                            connected && !busy, onHistory)
                    }
                    item {
                        AccountBackupToggle("Imported lyrics",
                            "Private LRC files, not audio recordings", cloud.includeLyrics,
                            connected && !busy, onLyrics)
                    }
                    item {
                        AccountBackupToggle("Playback preferences",
                            "Does not restore account permissions or privacy decisions",
                            cloud.includePreferences, connected && !busy, onPreferences)
                    }
                    item {
                        AccountBackupToggle("Automatic backups on Wi-Fi",
                            "After a verified backup; scheduling is approximate",
                            cloud.wifiOnly, connected && !busy, onWifiOnly)
                    }
                    item {
                        Text("This backup contains library metadata and the options you choose. " +
                            "Audio files are not included. Every restore starts with a review.",
                            color = muted,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    if (CloudAccessPolicy.needsHelp(connected, cloud.accessState, lastGoogleFailure != null)) {
                        item(key = "google-connection-help") {
                            Card(shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Column(Modifier.fillMaxWidth().padding(15.dp),
                                    verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                    Text(if (lastGoogleFailure == null && !connected)
                                        "Set up Google Drive backup" else "Google connection help",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold)
                                    Text(if (lastGoogleFailure == null && connected)
                                        "Your saved Drive account needs an access check. Reconnect or review connection details."
                                    else if (lastGoogleFailure == null)
                                        "Connect your Google account to back up library metadata. " +
                                            "Your local music stays available without Google."
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
                                                Text("Retry Google connection")
                                            }
                                        }
                                    }
                                    TextButton(onClick = { showSetupDetails = !showSetupDetails },
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                        Text(if (showSetupDetails) "Hide connection details" else "Connection details")
                                    }
                                    if (showSetupDetails) {
                                        Text("Installed APK signing SHA-1",
                                            style = MaterialTheme.typography.labelLarge)
                                        Text(apkFingerprint?.chunked(29)?.joinToString("\n")
                                                ?: "Not available on this device",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace),
                                            color = primary)
                                        Text("If the error repeats, compare this APK's package and " +
                                            "SHA-1 with the Android OAuth client. Check Drive API, " +
                                            "Google Play services and OAuth test users.",
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
                                                "Setup report copied" else "Copy Google setup report")
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
                                            Text("Open Google OAuth settings")
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
            title = { Text("Disconnect Google Drive?") },
            text = { Text("Your guest profile, offline music and local playlists stay " +
                "on this phone. Existing Drive backups are not deleted. " +
                "Automatic backup is stopped.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDisconnect = false
                    onDisconnect()
                }) { Text("Disconnect") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDisconnect = false }) { Text("Cancel") }
            }
        )
    }
    if (confirmChangeAccount) {
        AlertDialog(
            onDismissRequest = { confirmChangeAccount = false },
            title = { Text("Change the Drive account?") },
            text = { Text("A different Google Drive account requires its own first " +
                "verified backup. Your guest library stays unchanged, and " +
                "existing backups remain in the previous account.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmChangeAccount = false
                    openExternalFlow(onConnect)
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { confirmChangeAccount = false }) { Text("Cancel") }
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
