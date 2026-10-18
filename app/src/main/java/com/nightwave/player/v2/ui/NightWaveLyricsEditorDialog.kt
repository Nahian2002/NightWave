package com.nightwave.player.v2.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nightwave.player.v2.lyrics.LyricCue
import com.nightwave.player.v2.lyrics.LyricsEditorDraft
import com.nightwave.player.v2.lyrics.LyricsEditorPolicy
import com.nightwave.player.v2.lyrics.LyricsSyncPolicy
import com.nightwave.player.v2.lyrics.TimedLyrics

/**
 * A draft-only full-screen editor. The Activity owns all saving decisions.
 * Nothing here writes lyrics, audio, checkpoints or cloud data.
 */
@Composable
fun NightWaveLyricsEditorDialog(
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
    onDiscard: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    var selected by remember { mutableStateOf(0) }
    var editingCue by remember { mutableStateOf<LyricCue?>(null) }
    var deletingCue by remember { mutableStateOf<LyricCue?>(null) }
    var editingText by remember { mutableStateOf("") }
    var editingTime by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var editDialogError by remember { mutableStateOf<String?>(null) }
    var discardConfirm by remember { mutableStateOf(false) }
    val current = LyricsSyncPolicy.activeIndex(TimedLyrics(draft.cues), playbackMs, offsetMs)

    fun applySafe(action: () -> LyricsEditorDraft) {
        runCatching(action).onSuccess {
            error = null
            onApply(it)
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
                    OutlinedButton(onClick = { selected = (selected - 1).coerceAtLeast(0) },
                        enabled = !saving && selected > 0,
                        modifier = Modifier.weight(1f)) { Text("Previous") }
                    OutlinedButton(onClick = { selected = current.coerceIn(0, draft.cues.lastIndex) },
                        enabled = !saving && current >= 0,
                        modifier = Modifier.weight(1f)) { Text("Playing") }
                    OutlinedButton(onClick = {
                        selected = (selected + 1).coerceAtMost(draft.cues.lastIndex)
                    }, enabled = !saving && selected < draft.cues.lastIndex,
                        modifier = Modifier.weight(1f)) { Text("Next") }
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
                        Surface(onClick = { selected = index }, enabled = !saving,
                            modifier = Modifier.fillMaxWidth()
                                .testTag("lyrics-edit-row-$index"),
                            color = if (index == selected) colors.primaryContainer
                                else colors.surfaceVariant,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                        ) {
                            Column(Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(LyricsEditorPolicy.timeLabel(cue.timeMs),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.primary)
                                    Spacer(Modifier.weight(1f))
                                    if (index == current) {
                                        Text("PLAYING", color = colors.primary,
                                            style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Text(cue.text, style = MaterialTheme.typography.bodyLarge,
                                    color = if (index == selected)
                                        colors.onPrimaryContainer else colors.onSurface)
                                Row(Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    TextButton(onClick = {
                                        selected = index
                                        applySafe { LyricsEditorPolicy.nudge(draft, index, -1) }
                                    }, enabled = !saving) { Text("−.25s") }
                                    TextButton(onClick = {
                                        selected = index
                                        editingCue = cue
                                        editingText = cue.text
                                        editingTime = LyricsEditorPolicy.timeLabel(cue.timeMs)
                                        error = null
                                        editDialogError = null
                                    }, enabled = !saving) { Text("Edit") }
                                    TextButton(onClick = {
                                        selected = index
                                        applySafe { LyricsEditorPolicy.nudge(draft, index, 1) }
                                    }, enabled = !saving) { Text("+.25s") }
                                    TextButton(onClick = {
                                        selected = index
                                        deletingCue = cue
                                    }, enabled = !saving && draft.cues.size > 1) {
                                        Text("Delete")
                                    }
                                }
                            }
                        }
                    }
                }
                HorizontalDivider()
                Text("Only Save changes writes to private LRC. Your audio and " +
                    "cloud backups stay untouched.",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant)
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
                        result.onSuccess {
                            error = null
                            editDialogError = null
                            onApply(it)
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
