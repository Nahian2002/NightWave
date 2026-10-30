package com.nightwave.player.v2.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.nightwave.player.v2.ui.NightWaveMatteTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.nightwave.player.v2.support.BuildIdentity
import com.nightwave.player.v2.support.InstalledArtifactHash
import java.io.ByteArrayInputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun NightWaveBuildDetailsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val details by produceState<Pair<BuildIdentity?, String>?>(initialValue = null) {
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
            identity to (identity?.supportText(version.versionName.orEmpty(), code,
                apkHash, signerHashes, context.applicationInfo.splitSourceDirs?.size ?: 0)
                ?: "Build identity unavailable. Keep the APK and build receipt for support.")
        }
    }
    NightWaveBuildDetailsContent(details?.first, details?.second, onDismiss) { text ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("NightWave build details", text))
        Toast.makeText(context, "Build details copied", Toast.LENGTH_SHORT).show()
    }
}

@Composable
internal fun NightWaveBuildDetailsContent(identity: BuildIdentity?, text: String?,
    onDismiss: () -> Unit, onCopy: (String) -> Unit) {
    val updateContext = LocalContext.current
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("App details") },
        text = {
            Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = {
                    updateContext.startActivity(android.content.Intent(updateContext,
                        com.nightwave.player.updates.WeeklyUpdateActivity::class.java))
                    onDismiss()
                }) { Text(updateContext.getString(com.nightwave.player.R.string.weekly_check)) }
                Text("Use this build ID when reporting an issue or recording a test.")
                if (identity != null) Text(identity.displayId, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag("nw126-build-id"))
                Text(text ?: "Reading build details…", style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("nw126-build-details"))
            }
        },
        confirmButton = { TextButton(enabled = identity != null && text != null,
            onClick = { if (text != null) onCopy(text) }) { Text("Copy build details") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}
