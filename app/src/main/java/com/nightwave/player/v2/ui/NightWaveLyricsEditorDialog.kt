package com.nightwave.player.v2.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nightwave.player.v2.lyrics.LyricCue
import com.nightwave.player.v2.lyrics.LyricsEditorDraft
import com.nightwave.player.v2.lyrics.LyricsEditorPolicy
import com.nightwave.player.v2.lyrics.LyricsEditorNavigationPolicy
import com.nightwave.player.v2.lyrics.LyricsEditorSearchPolicy
import com.nightwave.player.v2.lyrics.LyricsSyncPolicy
import com.nightwave.player.v2.lyrics.TimedLyrics

/**
 * A draft-only full-screen editor. The Activity owns all saving decisions.
 * Nothing here writes lyrics, audio, checkpoints or cloud data.
 */
@Composable
fun NightWaveLyricsEditorDialog(
    songId: String,
    songTitle: String,
    draft: LyricsEditorDraft,
    dirty: Boolean,
    canUndo: Boolean,
    saving: Boolean,
    playbackMs: Long,
    offsetMs: Int,
    reduceMotion: Boolean,
    onApply: (LyricsEditorDraft) -> Unit,
    onUndo: () -> Unit,
    onSave: () -> Unit,
    onExport: () -> Unit,
    onDiscard: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    var selected by rememberSaveable(songId) { mutableStateOf(0) }
    var searchQuery by rememberSaveable(songId) { mutableStateOf("") }
    var editingCue by remember { mutableStateOf<LyricCue?>(null) }
    var deletingCue by remember { mutableStateOf<LyricCue?>(null) }
    var editingText by remember { mutableStateOf("") }
    var editingTime by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var editDialogError by remember { mutableStateOf<String?>(null) }
    var discardConfirm by remember { mutableStateOf(false) }
    val current = LyricsEditorNavigationPolicy.follow(draft, playbackMs, offsetMs)
    val searchMatches = remember(draft, searchQuery) {
        LyricsEditorSearchPolicy.matches(draft, searchQuery)
    }
    val searchMatchSet = remember(searchMatches) { searchMatches.toSet() }
    val searchOrdinal = LyricsEditorSearchPolicy.ordinal(searchMatches, selected)

    fun applySafe(
        action: () -> LyricsEditorDraft,
        selection: (LyricsEditorDraft) -> Int = { selected }
    ) {
        runCatching(action).onSuccess { revised ->
            error = null
            selected = LyricsEditorNavigationPolicy.clamp(selection(revised), revised.cues.size)
            onApply(revised)
        }.onFailure { error = it.message?.take(130) ?: "Invalid lyric edit" }
    }
    fun exitEditor() {
        if (saving) return
        if (dirty) discardConfirm = true else onDiscard()
    }

    Dialog(
        onDismissRequest = ::exitEditor,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().testTag("nightwave-lrc-editor"),
            color = colors.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("NIGHTWAVE · LYRICS EDITOR",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.primary)
                        Text(songTitle, maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = ::exitEditor, enabled = !saving) {
                        Text("Close")
                    }
                }
                Text(if (dirty) "Unsaved draft · Original LRC still intact"
                    else "No pending edits · Original LRC preserved",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (dirty) colors.primary else colors.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onUndo,
                        enabled = canUndo && !saving,
                        modifier = Modifier.weight(1f)) { Text("Undo") }
                    OutlinedButton(onClick = {
                        applySafe { LyricsEditorPolicy.add(draft, playbackMs) }
                    }, enabled = !saving && draft.cues.size < 1200,
                        modifier = Modifier.weight(1f)) { Text("Add at playhead") }
                }
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedButton(onClick = {
                        selected = LyricsEditorNavigationPolicy.step(selected, draft.cues.size, -1)
                    },
                        enabled = !saving && selected > 0,
                        modifier = Modifier.weight(1f)) { Text("Previous") }
                    OutlinedButton(onClick = {
                        selected = LyricsEditorNavigationPolicy.followOrKeep(
                            draft, playbackMs, offsetMs, selected)
                    },
                        enabled = !saving && current >= 0,
                        modifier = Modifier.weight(1f)) { Text("Playing") }
                    OutlinedButton(onClick = {
                        selected = LyricsEditorNavigationPolicy.step(selected, draft.cues.size, 1)
                    }, enabled = !saving && selected < draft.cues.lastIndex,
                        modifier = Modifier.weight(1f)) { Text("Next") }
                }
                // Search uses the current unsaved draft. It does not
                // open files, change the playing position or send network data.
                Surface(
                    color = colors.surfaceVariant.copy(alpha = 0.58f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("lyrics-search-panel")
                ) {
                    Column(Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { incoming ->
                                    if (incoming.length <= LyricsEditorSearchPolicy.MAX_QUERY_LENGTH)
                                        searchQuery = incoming
                                },
                                label = { Text("Search lyric words or 00:12") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("lyrics-search-input")
                            )
                            TextButton(onClick = { searchQuery = "" },
                                enabled = searchQuery.isNotEmpty(),
                                modifier = Modifier.heightIn(min = 48.dp)) {
                                Text("Clear")
                            }
                        }
                        if (searchQuery.isNotBlank()) {
                            Text(
                                if (searchMatches.isEmpty()) "No matching lyric lines"
                                else if (searchOrdinal != null)
                                    "Match $searchOrdinal of ${searchMatches.size} · ${draft.cues.size} lines"
                                else "${searchMatches.size} matches · Choose Next match",
                                color = if (searchMatches.isEmpty()) colors.onSurfaceVariant
                                    else colors.primary,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.testTag("lyrics-search-status")
                            )
                            Row(Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        LyricsEditorSearchPolicy.nearest(
                                            searchMatches, selected, false)?.let {
                                            selected = it
                                        }
                                    },
                                    enabled = !saving && searchMatches.isNotEmpty(),
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                        .testTag("lyrics-match-previous")
                                ) { Text("Previous match") }
                                OutlinedButton(
                                    onClick = {
                                        LyricsEditorSearchPolicy.nearest(
                                            searchMatches, selected, true)?.let {
                                            selected = it
                                        }
                                    },
                                    enabled = !saving && searchMatches.isNotEmpty(),
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                        .testTag("lyrics-match-next")
                                ) { Text("Next match") }
                            }
                        }
                    }
                }
                LaunchedEffect(draft.cues.size) {
                    selected = LyricsEditorNavigationPolicy.clamp(selected, draft.cues.size)
                }
                LaunchedEffect(selected, reduceMotion) {
                    if (selected in draft.cues.indices) {
                        if (reduceMotion) listState.scrollToItem(selected)
                        else listState.animateScrollToItem(selected)
                    }
                }
                error?.let {
                    Text(it, color = colors.error,
                        style = MaterialTheme.typography.labelSmall)
                }
                Text("${draft.cues.size} timed lines · Select a row to edit or nudge",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant)
                LazyColumn(state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(draft.cues, key = { index, _ -> index }) { index, cue ->
                        val searchHit = index in searchMatchSet
                        Surface(onClick = { selected = index }, enabled = !saving,
                            modifier = Modifier.fillMaxWidth()
                                .semantics {
                                    stateDescription = if (selected == index)
                                        "Selected lyric line" else "Not selected"
                                    contentDescription = "Lyric line " + (index + 1) +
                                        " of " + draft.cues.size + ", " +
                                        LyricsEditorPolicy.timeLabel(cue.timeMs) + ", " + cue.text +
                                        if (searchHit) ", search match" else ""
                                }
                                .testTag("lyrics-edit-row-$index"),
                            color = when {
                                index == selected -> colors.primaryContainer
                                searchHit -> colors.tertiaryContainer.copy(alpha = 0.62f)
                                else -> colors.surfaceVariant
                            },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                        ) {
                            Column(Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(LyricsEditorPolicy.timeLabel(cue.timeMs),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (index == selected)
                                            colors.onPrimaryContainer else colors.primary)
                                    Spacer(Modifier.weight(1f))
                                    if (searchHit) {
                                        Text("MATCH", color = if (index == selected)
                                            colors.onPrimaryContainer else colors.onTertiaryContainer,
                                            style = MaterialTheme.typography.labelSmall)
                                    }
                                    if (index == current) {
                                        Text("PLAYING", color = if (index == selected)
                                            colors.onPrimaryContainer else colors.primary,
                                            style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Text(cue.text, style = MaterialTheme.typography.bodyLarge,
                                    color = when {
                                        index == selected -> colors.onPrimaryContainer
                                        searchHit -> colors.onTertiaryContainer
                                        else -> colors.onSurface
                                    })
                                Row(Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    TextButton(onClick = {
                                        selected = index
                                        applySafe(
                                            { LyricsEditorPolicy.nudge(draft, index, -1) },
                                            { after ->
                                                val expected = cue.copy(
                                                    timeMs = (cue.timeMs - 250L).coerceAtLeast(0L))
                                                LyricsEditorNavigationPolicy.afterEdit(
                                                    after, expected, selected)
                                            }
                                        )
                                    }, enabled = !saving,
                                        modifier = Modifier.heightIn(min = 48.dp)) {
                                        Text("−.25s")
                                    }
                                    TextButton(onClick = {
                                        selected = index
                                        editingCue = cue
                                        editingText = cue.text
                                        editingTime = LyricsEditorPolicy.timeLabel(cue.timeMs)
                                        error = null
                                        editDialogError = null
                                    }, enabled = !saving,
                                        modifier = Modifier.heightIn(min = 48.dp)) {
                                        Text("Edit")
                                    }
                                    TextButton(onClick = {
                                        selected = index
                                        applySafe(
                                            { LyricsEditorPolicy.nudge(draft, index, 1) },
                                            { after ->
                                                val expected = cue.copy(
                                                    timeMs = (cue.timeMs + 250L)
                                                        .coerceAtMost(LyricsEditorPolicy.MAX_TIME_MS))
                                                LyricsEditorNavigationPolicy.afterEdit(
                                                    after, expected, selected)
                                            }
                                        )
                                    }, enabled = !saving,
                                        modifier = Modifier.heightIn(min = 48.dp)) {
                                        Text("+.25s")
                                    }
                                    TextButton(onClick = {
                                        selected = index
                                        deletingCue = cue
                                    }, enabled = !saving && draft.cues.size > 1,
                                        modifier = Modifier.heightIn(min = 48.dp)) {
                                        Text("Delete")
                                    }
                                }
                            }
                        }
                    }
                }
                HorizontalDivider()
                Text("Only Save changes writes to private LRC. Export creates a " +
                    "separate file you choose; it does not save this draft in NightWave.",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant)
                OutlinedButton(onClick = onExport, enabled = !saving,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("lyrics-export-lrc")) {
                    Text(if (dirty) "Export unsaved draft .lrc" else "Export saved .lrc")
                }
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedButton(onClick = ::exitEditor, enabled = !saving,
                        modifier = Modifier.weight(1f)) { Text("Discard") }
                    Button(onClick = onSave, enabled = dirty && !saving,
                        modifier = Modifier.weight(1f).testTag("lyrics-save-edits")) {
                        if (saving) CircularProgressIndicator(
                            modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                        else Text("Save changes")
                    }
                }
            }
        }
    }
    editingCue?.let { original ->
        AlertDialog(
            onDismissRequest = { if (!saving) editingCue = null },
            title = { Text("Edit lyric and timestamp") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = editingTime, onValueChange = {
                        if (it.length <= 12) editingTime = it
                    }, label = { Text("Time · mm:ss.SSS") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editingText, onValueChange = {
                        if (it.length <= LyricsEditorPolicy.MAX_TEXT_LENGTH) editingText = it
                    }, label = { Text("Lyric text") },
                        maxLines = 3, modifier = Modifier.fillMaxWidth())
                    editDialogError?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall,
                            color = colors.error)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val i = draft.cues.indexOf(original)
                    if (i < 0) editDialogError = "Cue changed. Select it again."
                    else {
                        val result = runCatching {
                            val textDraft = LyricsEditorPolicy.updateText(draft, i, editingText)
                            val revised = original.copy(text = editingText
                                .replace('\n', ' ').replace('\r', ' ').trim())
                            val newI = textDraft.cues.indexOf(revised)
                            require(newI >= 0) { "Cue changed. Select it again." }
                            LyricsEditorPolicy.updateTime(textDraft, newI, editingTime)
                        }
                        result.onSuccess { updated ->
                            error = null
                            editDialogError = null
                            val revised = original.copy(
                                text = editingText.replace('\n', ' ').replace('\r', ' ').trim(),
                                timeMs = LyricsEditorPolicy.parseTime(editingTime))
                            selected = LyricsEditorNavigationPolicy.afterEdit(
                                LyricsEditorDraft(updated.cues), revised, selected)
                            onApply(updated)
                            editingCue = null
                        }.onFailure {
                            editDialogError = it.message?.take(130)
                        }
                    }
                }, enabled = !saving) { Text("Apply to draft") }
            },
            dismissButton = {
                TextButton(onClick = { editingCue = null }, enabled = !saving) {
                    Text("Cancel")
                }
            }
        )
    }
    deletingCue?.let { original ->
        AlertDialog(
            onDismissRequest = { deletingCue = null },
            title = { Text("Remove this timed line?") },
            text = { Text("This removes the cue from the unsaved draft only.") },
            confirmButton = {
                Button(onClick = {
                    val index = draft.cues.indexOf(original)
                    if (index >= 0) applySafe {
                        LyricsEditorPolicy.delete(draft, index)
                    }
                    selected = selected.coerceAtMost((draft.cues.size - 2).coerceAtLeast(0))
                    deletingCue = null
                }, enabled = !saving) { Text("Remove line") }
            },
            dismissButton = {
                TextButton(onClick = { deletingCue = null }) { Text("Keep line") }
            }
        )
    }
    if (discardConfirm) {
        AlertDialog(
            onDismissRequest = { discardConfirm = false },
            title = { Text("Discard unsaved lyric edits?") },
            text = { Text("Your existing saved lyrics will remain unchanged.") },
            confirmButton = {
                Button(onClick = { discardConfirm = false; onDiscard() }) {
                    Text("Discard edits")
                }
            },
            dismissButton = {
                TextButton(onClick = { discardConfirm = false }) {
                    Text("Keep editing")
                }
            }
        )
    }
}
