package com.nightwave.player.v2.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nightwave.player.R
import com.nightwave.player.net.DiscoveryItem
import com.nightwave.player.net.DiscoveryKind
import com.nightwave.player.net.DiscoveryOutcome
import com.nightwave.player.net.DiscoveryPermission
import com.nightwave.player.net.DiscoverySearch
import com.nightwave.player.net.DiscoverySource
import com.nightwave.player.net.DiscoverySources

private class SourcesRequest(val query: String, val sources: Set<DiscoverySource>)
private val sourceSearch = DiscoverySearch()

@Composable
internal fun NightWaveSourcesDialog(initialQuery: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val strings = rememberNightWaveStrings()
    var query by remember { mutableStateOf(initialQuery.take(120)) }
    var selected by remember { mutableStateOf(emptySet<DiscoverySource>()) }
    var request by remember { mutableStateOf<SourcesRequest?>(null) }
    val active = request
    var outcomes by remember(active) { mutableStateOf(emptyMap<DiscoverySource, DiscoveryOutcome>()) }
    val pending = active != null && outcomes.size < active.sources.size
    val stopPending by rememberUpdatedState({ if (pending) request = null })
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) stopPending() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(active) {
        if (active != null) sourceSearch.search(active.query, active.sources).collect { update ->
            if (request === active) outcomes = outcomes + (update.source to update.outcome)
        }
    }
    val submitted = DiscoverySources.query(query)?.takeIf { selected.isNotEmpty() && !pending }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val focus = LocalFocusManager.current
        val keyboard = LocalSoftwareKeyboardController.current
        val submit = {
            if (submitted != null) {
                focus.clearFocus()
                keyboard?.hide()
                request = SourcesRequest(submitted, selected)
            }
        }
        Surface(
            Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
                .widthIn(max = 560.dp).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            LazyColumn(
                modifier = Modifier.heightIn(max = 640.dp).testTag("multisource-list"),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    NightWaveSectionActions(
                        heading = {
                            Text(strings.get(R.string.multisource_title), Modifier.semantics { heading() },
                                style = MaterialTheme.typography.headlineSmall)
                        },
                        action = {
                            NightWaveMatteTextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text(strings.get(R.string.nw141_close_7d9eb))
                            }
                        }
                    )
                    Text(strings.get(R.string.multisource_consent), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { text ->
                            val next = text.take(120)
                            if (next != query) {
                                query = next
                                request = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("multisource-query"),
                        label = { Text(strings.get(R.string.multisource_query)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { submit() })
                    )
                }
                item {
                    NightWaveCompactActions {
                        DiscoverySource.entries.filter { it.available }.forEach { source ->
                            FilterChip(
                                selected = source in selected,
                                onClick = {
                                    request = null
                                    selected = if (source in selected) selected - source else selected + source
                                },
                                modifier = Modifier.heightIn(min = 48.dp).testTag("multisource-${source.name}"),
                                label = {
                                    Text(if (source == DiscoverySource.RADIO) strings.get(R.string.multisource_radio)
                                        else source.displayName)
                                }
                            )
                        }
                    }
                }
                item {
                    NightWaveCompactActions {
                        Button(onClick = submit, enabled = submitted != null, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(strings.get(R.string.multisource_submit))
                        }
                        if (pending) NightWaveMatteTextButton(onClick = { request = null },
                            modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(strings.get(R.string.nw141_cancel_search_b5456))
                        }
                    }
                }
                active?.sources?.forEach { source ->
                    val outcome = outcomes[source]
                    item(key = "provider-${source.name}") {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(source.displayName, Modifier.semantics { heading() },
                                style = MaterialTheme.typography.titleMedium)
                            val status = when (outcome) {
                                null -> R.string.multisource_loading
                                DiscoveryOutcome.Empty -> R.string.multisource_empty
                                is DiscoveryOutcome.Results -> null
                                DiscoveryOutcome.Problem.QUOTA -> R.string.multisource_quota
                                DiscoveryOutcome.Problem.NETWORK -> R.string.multisource_network
                                DiscoveryOutcome.Problem.TIMEOUT -> R.string.multisource_timeout
                                DiscoveryOutcome.Problem.AUTH, DiscoveryOutcome.Problem.PERMISSION ->
                                    R.string.multisource_restricted
                                else -> R.string.multisource_unavailable
                            }
                            if (status != null) Text(strings.get(status), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (outcome is DiscoveryOutcome.Results) {
                        items(outcome.items, key = { "${it.id.source.name}:${it.id.remoteId}" }) { result ->
                            DiscoveryResultRow(result) {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, result.canonicalUrl.toString().toUri())
                                        .addCategory(Intent.CATEGORY_BROWSABLE))
                                } catch (_: ActivityNotFoundException) {
                                    Toast.makeText(context, strings.get(R.string.multisource_open_failed),
                                        Toast.LENGTH_SHORT).show()
                                } catch (_: SecurityException) {
                                    Toast.makeText(context, strings.get(R.string.multisource_open_failed),
                                        Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
                item(key = "jamendo-status") {
                    Text(strings.get(R.string.multisource_jamendo), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun DiscoveryResultRow(result: DiscoveryItem, onOpen: () -> Unit) {
    val strings = rememberNightWaveStrings()
    val type = when (result.id.source.kind) {
        DiscoveryKind.TRACK -> R.string.multisource_track
        DiscoveryKind.VIDEO -> R.string.multisource_video
        DiscoveryKind.LIVE_RADIO -> R.string.multisource_radio
    }
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(result.title, style = MaterialTheme.typography.titleSmall)
            Text(listOfNotNull(result.creator, strings.get(type),
                result.durationSeconds?.let { strings.duration(it * 1_000L) }).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(result.canonicalUrl.host, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (result.capabilities.openExternally == DiscoveryPermission.AVAILABLE) {
                OutlinedButton(onClick = onOpen, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(strings.get(if (result.id.source == DiscoverySource.RADIO) R.string.multisource_open_station
                        else R.string.multisource_open_provider))
                }
            }
        }
    }
}
