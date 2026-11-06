package com.nightwave.player.v2.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import com.nightwave.player.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.nightwave.player.v2.ui.NightWaveMatteTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nightwave.player.v2.support.BuildIdentity
import com.nightwave.player.v2.support.InstalledArtifactHash
import java.io.ByteArrayInputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class InstalledBuildDetails(
    val identity: BuildIdentity?,
    val versionName: String,
    val versionCode: Long,
    val receipt: String?
)

@Composable
fun NightWaveBuildDetailsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val strings = rememberNightWaveStrings()
    val details by produceState<InstalledBuildDetails?>(initialValue = null, context) {
        value = withContext(Dispatchers.IO) {
            val identity = runCatching {
                context.assets.open("nightwave-build.json").use { input ->
                    BuildIdentity.read(input)
                }
            }.getOrNull()
            @Suppress("DEPRECATION")
            val version = context.packageManager.getPackageInfo(context.packageName,
                if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES)
            @Suppress("DEPRECATION")
            val code = if (Build.VERSION.SDK_INT >= 28) version.longVersionCode else version.versionCode.toLong()
            val apkHash = runCatching { File(context.applicationInfo.sourceDir).inputStream().use {
                InstalledArtifactHash.read(it)
            } }.getOrNull()
            @Suppress("DEPRECATION")
            val signatures = if (Build.VERSION.SDK_INT >= 28) version.signingInfo?.apkContentsSigners
                else version.signatures
            val signerHashes = signatures.orEmpty().mapNotNull {
                InstalledArtifactHash.read(ByteArrayInputStream(it.toByteArray()))
            }
            InstalledBuildDetails(
                identity, version.versionName.orEmpty(), code,
                identity?.supportText(version.versionName.orEmpty(), code,
                    apkHash, signerHashes, context.applicationInfo.splitSourceDirs?.size ?: 0)
            )
        }
    }
    val installed = details
    NightWaveBuildDetailsContent(
        installed?.identity, installed?.receipt,
        installed?.let { strings.get(R.string.nw157_version, it.versionName, it.versionCode.toString()) },
        onDismiss
    ) { text ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(strings.get(R.string.nw157_copy_details), text))
            Toast.makeText(context, strings.get(R.string.nw157_copied), Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
internal fun NightWaveBuildDetailsContent(
    identity: BuildIdentity?, text: String?, versionLabel: String?,
    onDismiss: () -> Unit, onCopy: (String) -> Unit
) {
    val updateContext = LocalContext.current
    val strings = rememberNightWaveStrings()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
                .widthIn(max = 520.dp).fillMaxWidth().testTag("nw157-app-details"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(strings.get(R.string.nw141_app_details_2945d),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() })
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(versionLabel ?: strings.get(R.string.nw157_reading),
                            style = MaterialTheme.typography.titleMedium)
                        if (identity != null) {
                            Text(identity.displayId, fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.testTag("nw126-build-id"))
                        } else if (versionLabel != null) {
                            Text(strings.get(R.string.nw157_identity_unavailable),
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                OutlinedButton(onClick = {
                    updateContext.startActivity(android.content.Intent(updateContext,
                        com.nightwave.player.updates.WeeklyUpdateActivity::class.java))
                    onDismiss()
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(strings.get(R.string.weekly_check))
                }
                NightWaveCompactActions {
                    TextButton(enabled = text != null, onClick = { text?.let(onCopy) },
                        modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(strings.get(R.string.nw157_copy_details))
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(strings.get(R.string.nw141_close_7d9eb))
                    }
                }
                if (text != null) NightWaveTechnicalDetails(strings.get(R.string.nw157_technical_details)) {
                    Text(text, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("nw126-build-details"))
                }
            }
        }
    }
}
