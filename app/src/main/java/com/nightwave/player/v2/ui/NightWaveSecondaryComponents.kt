package com.nightwave.player.v2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nightwave.player.R

/** Shared real settings row. Narrow/large-text actions get their own lane. */
@Composable
internal fun NightWaveSettingsRow(
    title: String, detail: String, icon: ImageVector, onClick: () -> Unit,
    modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null
) {
    val scale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier.fillMaxWidth()
        .heightIn(min = NightWaveSecondaryLayoutPolicy.SETTINGS_MIN_DP.dp)
        .clickable(role = Role.Button, onClick = onClick)
        .padding(horizontal = 12.dp, vertical = NightWaveSecondaryLayoutPolicy.SETTINGS_VERTICAL_DP.dp)) {
        val identity: @Composable (Modifier) -> Unit = { lane ->
            Row(lane, horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = .09f)),
                    contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = .90f))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(detail, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (trailing != null && NightWaveSecondaryLayoutPolicy.stackSettingAction(maxWidth.value, scale)) {
            Column(Modifier.fillMaxWidth()) {
                identity(Modifier.fillMaxWidth())
                Box(Modifier.align(Alignment.End).heightIn(min = 48.dp).widthIn(min = 48.dp)
                    .testTag("nw147-settings-action"), contentAlignment = Alignment.Center) { trailing() }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                identity(Modifier.weight(1f))
                if (trailing != null) Box(Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp)
                    .testTag("nw147-settings-action"), contentAlignment = Alignment.Center) { trailing() }
                else Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

/** Optional explanations only; live readback/errors and all editing actions stay outside. */
@Composable
internal fun NightWaveTechnicalDetails(title: String, content: @Composable ColumnScope.() -> Unit) {
    val strings = rememberNightWaveStrings()
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        NightWaveMatteTextButton(onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .testTag("nw147-technical-disclosure").semantics {
                    stateDescription = strings.get(if (expanded) R.string.nw147_details_expanded else R.string.nw147_details_collapsed)
                }) {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            Text(if (expanded) "−" else "+", modifier = Modifier.padding(start = 8.dp))
        }
        if (expanded) Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)
            .testTag("nw147-technical-content"), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}
