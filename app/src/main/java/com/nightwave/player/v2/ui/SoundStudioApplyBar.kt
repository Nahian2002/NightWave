package com.nightwave.player.v2.ui

import com.nightwave.player.R

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
    val strings = rememberNightWaveStrings()
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Column(Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 8.dp)
        .testTag("nw138-apply-bar")) {
        Text(if (busy) strings.get(R.string.nw141_waiting_for_playback_confirmation_9371b)
            else if (!canApply) strings.get(R.string.nw141_manual_eq_unavailable_check_audio_compatibility_e46e6)
            else if (edited) strings.get(R.string.nw141_draft_changes_not_applied_47162)
            else strings.get(R.string.nw141_manual_curve_apply_to_use_these_levels_84ef9),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        NightWaveMatteButton(onClick = onApply, enabled = canApply && !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("nw138-apply")) {
            Text(strings.get(R.string.nw141_apply_manual_eq_bab04))
        }
        if (edited) NightWaveMatteTextButton(onClick = onDiscard, enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("nw138-discard")) {
            Text(strings.get(R.string.nw141_discard_draft_changes_43a75))
        }
    }
}
