package com.nightwave.player.v2.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** A sibling of the weighted scrolling content, so Apply never scrolls away. */
@Composable
internal fun SoundStudioApplyBar(
    edited: Boolean, canApply: Boolean, busy: Boolean,
    onApply: () -> Unit, onDiscard: () -> Unit
) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Column(Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 8.dp)
        .testTag("nw138-apply-bar")) {
        Text(if (busy) "Waiting for playback confirmation"
            else if (!canApply) "Manual EQ unavailable · check audio compatibility"
            else if (edited) "Draft changes · not applied"
            else "Manual curve · Apply to use these levels",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        NightWaveMatteButton(onClick = onApply, enabled = canApply && !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("nw138-apply")) {
            Text("Apply manual EQ")
        }
        if (edited) NightWaveMatteTextButton(onClick = onDiscard, enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("nw138-discard")) {
            Text("Discard draft changes")
        }
    }
}
