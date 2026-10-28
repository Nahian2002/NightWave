package com.nightwave.player.v2.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Existing explicit library actions; opening/dismissing the menu never performs an edit. */
@Composable
internal fun NightWaveLibraryOptions(title: String, tag: String, actions: List<Pair<String, () -> Unit>>) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true },
            modifier = Modifier.heightIn(min = 48.dp).testTag(tag)) {
            Icon(Icons.Outlined.MoreVert, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(title)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            actions.forEach { (label, action) ->
                DropdownMenuItem(text = { Text(label) }, onClick = {
                    expanded = false
                    action()
                }, modifier = Modifier.heightIn(min = 48.dp))
            }
        }
    }
}
