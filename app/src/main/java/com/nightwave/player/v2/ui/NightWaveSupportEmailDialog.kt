package com.nightwave.player.v2.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nightwave.player.R
import com.nightwave.player.v2.ui.NightWaveMatteTextButton as TextButton

// Maintainer-approved public contact in SECURITY.md.
private const val SUPPORT_EMAIL = "jayednahian2002@gmail.com"

@Composable
internal fun NightWaveSupportEmailDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val strings = rememberNightWaveStrings()
    var unavailable by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.get(R.string.nw158_email_support)) },
        text = {
            Column(
                Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(strings.get(R.string.nw158_email_explanation))
                SelectionContainer { Text(SUPPORT_EMAIL) }
                if (unavailable) Text(strings.get(R.string.nw158_email_unavailable))
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(ClipData.newPlainText(SUPPORT_EMAIL, SUPPORT_EMAIL))
                        Toast.makeText(context, strings.get(R.string.nw158_email_copied), Toast.LENGTH_SHORT).show()
                    }
                }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(strings.get(R.string.nw158_copy_address))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val subject = Uri.encode(strings.get(R.string.nw158_email_subject))
                val body = Uri.encode(strings.get(R.string.nw158_email_body))
                val draft = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$SUPPORT_EMAIL?subject=$subject&body=$body"))
                try {
                    context.startActivity(draft)
                    onDismiss()
                } catch (_: ActivityNotFoundException) {
                    unavailable = true
                } catch (_: SecurityException) {
                    unavailable = true
                }
            }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(strings.get(R.string.nw158_open_email))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(strings.get(R.string.nw141_close_7d9eb))
            }
        }
    )
}
