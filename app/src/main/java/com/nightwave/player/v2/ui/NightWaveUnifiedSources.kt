package com.nightwave.player.v2.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.nightwave.player.net.SavedSourceLink
import com.nightwave.player.net.SavedSourceLinks
import com.nightwave.player.net.UnifiedSearchScope
import com.nightwave.player.net.UnifiedSourceRequest
import com.nightwave.player.net.rankUnifiedSourceResults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl

private val sourceSearch = DiscoverySearch()
private const val PREVIEW_RESULTS = 3

internal class UnifiedSourceState(private val store: SavedSourceLinks, private val owner: CoroutineScope) {
    var request by mutableStateOf<UnifiedSourceRequest?>(null)
        private set
    var outcomes by mutableStateOf(emptyMap<DiscoverySource, DiscoveryOutcome>())
        private set
    var expanded by mutableStateOf(emptySet<DiscoverySource>())
        private set
    var links by mutableStateOf<List<SavedSourceLink>?>(null)
        private set
    var linkError by mutableStateOf(false)
        private set
    var saving by mutableStateOf(false)
        private set

    val pending: Boolean get() = request?.let { it.scope.providers.size > outcomes.size } == true

    fun submit(scope: UnifiedSearchScope, query: String) {
        val submitted = UnifiedSourceRequest.create(scope, query) ?: return
        cancel()
        request = submitted
    }

    fun cancel() {
        request = null
        outcomes = emptyMap()
        expanded = emptySet()
    }

    fun toggleSection(source: DiscoverySource) {
        expanded = if (source in expanded) expanded - source else expanded + source
    }

    suspend fun collect(submitted: UnifiedSourceRequest) {
        sourceSearch.search(submitted.query, submitted.scope.providers).collect { update ->
            if (request === submitted) {
                val outcome = update.outcome
                val ranked = if (outcome is DiscoveryOutcome.Results) {
                    DiscoveryOutcome.Results(rankUnifiedSourceResults(submitted.query, outcome.items))
                } else outcome
                outcomes = outcomes + (update.source to ranked)
            }
        }
    }

    suspend fun readLinks() {
        links = withContext(Dispatchers.IO) { store.read() }
        linkError = links == null
    }

    fun toggleLink(link: SavedSourceLink) {
        if (saving || links == null) return
        saving = true
        owner.launch {
            try {
                val updated = withContext(Dispatchers.IO) { store.toggle(link) }
                linkError = updated == null
                if (updated != null) links = updated
            } finally {
                saving = false
            }
        }
    }
}

@Composable
internal fun rememberUnifiedSourceState(scope: UnifiedSearchScope, query: String, visible: Boolean): UnifiedSourceState {
    val context = LocalContext.current
    val owner = rememberCoroutineScope()
    val state = remember(context, owner) { UnifiedSourceState(SavedSourceLinks(context), owner) }
    val current = state.request
    LaunchedEffect(state) { state.readLinks() }
    LaunchedEffect(state, current, scope, query, visible) {
        if (current != null) {
            if (!current.matches(scope, query) || !visible && state.pending) state.cancel()
            else if (visible && state.pending) state.collect(current)
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, state) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && state.pending) state.cancel()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return state
}

@Composable
internal fun NightWaveSourceFilters(selected: UnifiedSearchScope, onSelect: (UnifiedSearchScope) -> Unit) {
    val strings = rememberNightWaveStrings()
    val showsProviders = selected.providers.isNotEmpty()
    Column(Modifier.fillMaxWidth().testTag("nw57-source-filters"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        NightWaveCompactActions {
            SourceScopeChip(UnifiedSearchScope.MUSIC, strings.get(R.string.nw57_music_public), selected == UnifiedSearchScope.MUSIC, onSelect)
            SourceScopeChip(UnifiedSearchScope.LOCAL, strings.get(R.string.nw57_your_music), selected == UnifiedSearchScope.LOCAL, onSelect)
            SourceScopeChip(UnifiedSearchScope.ALL, strings.get(R.string.nw58_sources_tab), showsProviders, onSelect,
                tag = "nw58-sources-tab")
            SourceScopeChip(UnifiedSearchScope.SAVED, strings.get(R.string.nw57_saved_links), selected == UnifiedSearchScope.SAVED, onSelect)
        }
        if (showsProviders) {
            Text(strings.get(R.string.nw58_provider_label), Modifier.semantics { heading() },
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            NightWaveCompactActions {
                SourceScopeChip(UnifiedSearchScope.ALL, strings.get(R.string.nw57_all_sources), selected == UnifiedSearchScope.ALL, onSelect)
                SourceScopeChip(UnifiedSearchScope.AUDIUS, DiscoverySource.AUDIUS.displayName, selected == UnifiedSearchScope.AUDIUS, onSelect)
                SourceScopeChip(UnifiedSearchScope.PEERTUBE, DiscoverySource.PEERTUBE.displayName, selected == UnifiedSearchScope.PEERTUBE, onSelect)
                SourceScopeChip(UnifiedSearchScope.RADIO, strings.get(R.string.multisource_radio), selected == UnifiedSearchScope.RADIO, onSelect)
                SourceScopeChip(UnifiedSearchScope.OPENVERSE, DiscoverySource.OPENVERSE.displayName, selected == UnifiedSearchScope.OPENVERSE, onSelect)
            }
        }
    }
}

@Composable
private fun SourceScopeChip(
    scope: UnifiedSearchScope,
    label: String,
    selected: Boolean,
    onSelect: (UnifiedSearchScope) -> Unit,
    tag: String = "nw57-scope-${scope.name}"
) {
    FilterChip(selected = selected, onClick = { onSelect(scope) },
        modifier = Modifier.heightIn(min = 48.dp).testTag(tag), label = { Text(label) })
}

internal fun LazyListScope.unifiedSourceResults(
    scope: UnifiedSearchScope, query: String, state: UnifiedSourceState, onSubmit: () -> Unit
) {
    val request = state.request?.takeIf { it.matches(scope, query) }
    item(key = "nw57-provider-actions") {
        val strings = rememberNightWaveStrings()
        var showSourceInfo by rememberSaveable { mutableStateOf(false) }
        Text(strings.get(R.string.nw58_source_intro), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        NightWaveCompactActions {
            Button(onClick = onSubmit, enabled = UnifiedSourceRequest.create(scope, query) != null && !state.pending,
                modifier = Modifier.heightIn(min = 48.dp)) { Text(strings.get(R.string.multisource_submit)) }
            if (state.pending) NightWaveMatteTextButton(onClick = state::cancel, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(strings.get(R.string.nw141_cancel_search_b5456))
            }
            NightWaveMatteTextButton(onClick = { showSourceInfo = !showSourceInfo }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(strings.get(if (showSourceInfo) R.string.nw58_hide_source_info else R.string.nw58_about_sources))
            }
        }
        if (showSourceInfo) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (message in listOf(R.string.nw57_provider_disclosure, R.string.nw58_openverse_disclosure,
                    R.string.multisource_jamendo)) {
                    Text(strings.get(message), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (state.linkError) Text(strings.get(R.string.nw57_link_error), color = MaterialTheme.colorScheme.error)
    }
    request?.scope?.providers?.forEach { source ->
        val outcome = state.outcomes[source]
        val results = (outcome as? DiscoveryOutcome.Results)?.items.orEmpty()
        val expanded = source in state.expanded
        item(key = "nw57-provider-${source.name}") {
            val strings = rememberNightWaveStrings()
            Text(source.displayName, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
            val message = when (outcome) {
                null -> R.string.multisource_loading
                DiscoveryOutcome.Empty -> R.string.multisource_empty
                is DiscoveryOutcome.Results -> null
                DiscoveryOutcome.Problem.QUOTA -> R.string.multisource_quota
                DiscoveryOutcome.Problem.NETWORK -> R.string.multisource_network
                DiscoveryOutcome.Problem.TIMEOUT -> R.string.multisource_timeout
                DiscoveryOutcome.Problem.AUTH, DiscoveryOutcome.Problem.PERMISSION -> R.string.multisource_restricted
                else -> R.string.multisource_unavailable
            }
            if (message != null) Text(strings.get(message), style = MaterialTheme.typography.bodySmall)
        }
        items(if (expanded) results else results.take(PREVIEW_RESULTS), key = { "nw57:${it.id.source}:${it.id.remoteId}" }) { item ->
            UnifiedSourceResult(item, state)
        }
        if (results.size > PREVIEW_RESULTS) item(key = "nw57-more-${source.name}") {
            val strings = rememberNightWaveStrings()
            NightWaveMatteTextButton(onClick = { state.toggleSection(source) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(if (expanded) strings.get(R.string.nw57_show_fewer, source.displayName)
                    else strings.get(R.string.nw57_show_all, strings.number(results.size), source.displayName))
            }
        }
    }
}

internal fun LazyListScope.unifiedSavedLinks(query: String, state: UnifiedSourceState) {
    val all = state.links.orEmpty()
    val shown = all.filter { query.isBlank() || it.url.toString().contains(query.trim(), ignoreCase = true) ||
        it.source.displayName.contains(query.trim(), ignoreCase = true) }
    item(key = "nw57-saved-intro") {
        val strings = rememberNightWaveStrings()
        Text(strings.get(R.string.nw57_saved_explanation), style = MaterialTheme.typography.bodySmall)
        val message = when {
            state.linkError -> R.string.nw57_link_error
            state.links == null -> R.string.nw57_links_loading
            shown.isEmpty() -> R.string.nw57_no_links
            else -> null
        }
        if (message != null) Text(strings.get(message), style = MaterialTheme.typography.bodySmall)
    }
    items(shown, key = { "nw57-saved:${it.source}:${it.url}" }) { link ->
        val strings = rememberNightWaveStrings()
        SourceCard(link.source.displayName, link.url.toString()) {
            SourceOpenButton(link.url, link.source)
            NightWaveMatteTextButton(onClick = { state.toggleLink(link) }, enabled = !state.saving,
                modifier = Modifier.heightIn(min = 48.dp)) { Text(strings.get(R.string.nw57_remove_link)) }
        }
    }
}

@Composable
private fun UnifiedSourceResult(result: DiscoveryItem, state: UnifiedSourceState) {
    val strings = rememberNightWaveStrings()
    val source = result.id.source
    val type = when (source.kind) {
        DiscoveryKind.TRACK -> R.string.multisource_track
        DiscoveryKind.VIDEO -> R.string.multisource_video
        DiscoveryKind.LIVE_RADIO -> R.string.multisource_radio
    }
    val subtitle = listOfNotNull(result.creator, strings.get(type),
        result.durationSeconds?.let { strings.duration(it * 1_000L) }).joinToString(" · ")
    val attribution = listOfNotNull(result.licenseLabel, result.canonicalUrl.host).joinToString(" · ")
    SourceCard(result.title, subtitle, attribution) {
        if (result.capabilities.openExternally == DiscoveryPermission.AVAILABLE) {
            SourceOpenButton(result.canonicalUrl, source)
        }
        val link = SavedSourceLink.from(source, result.canonicalUrl.toString())
        if (link != null) NightWaveMatteTextButton(onClick = { state.toggleLink(link) },
            enabled = state.links != null && !state.saving, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(strings.get(if (link in state.links.orEmpty()) R.string.nw57_remove_link else R.string.nw57_save_link))
        }
    }
}

@Composable
private fun SourceCard(title: String, detail: String, attribution: String? = null, actions: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(detail, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (attribution != null) Text(attribution, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            NightWaveCompactActions(actions)
        }
    }
}

@Composable
private fun SourceOpenButton(url: HttpUrl, source: DiscoverySource) {
    val context = LocalContext.current
    val strings = rememberNightWaveStrings()
    OutlinedButton(onClick = {
        val intent = Intent(Intent.ACTION_VIEW, url.toString().toUri()).addCategory(Intent.CATEGORY_BROWSABLE)
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, strings.get(R.string.multisource_open_failed), Toast.LENGTH_SHORT).show()
        } catch (_: SecurityException) {
            Toast.makeText(context, strings.get(R.string.multisource_open_failed), Toast.LENGTH_SHORT).show()
        }
    }, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(strings.get(if (source == DiscoverySource.RADIO) R.string.multisource_open_station else R.string.multisource_open_provider))
    }
}
