package com.nightwave.player.net

internal enum class UnifiedSearchScope(val providers: Set<DiscoverySource> = emptySet()) {
    MUSIC(setOf(DiscoverySource.AUDIUS, DiscoverySource.PEERTUBE, DiscoverySource.OPENVERSE)),
    LOCAL,
    ALL(setOf(DiscoverySource.AUDIUS, DiscoverySource.PEERTUBE, DiscoverySource.RADIO, DiscoverySource.OPENVERSE)),
    AUDIUS(setOf(DiscoverySource.AUDIUS)),
    PEERTUBE(setOf(DiscoverySource.PEERTUBE)),
    RADIO(setOf(DiscoverySource.RADIO)),
    SAVED,
    OPENVERSE(setOf(DiscoverySource.OPENVERSE));

    val showsLocal: Boolean get() = this == LOCAL || this == ALL
}

internal class UnifiedSourceRequest private constructor(
    val scope: UnifiedSearchScope,
    val query: String
) {
    fun matches(scope: UnifiedSearchScope, draft: String): Boolean =
        this.scope == scope && query == DiscoverySources.query(draft)

    companion object {
        fun create(scope: UnifiedSearchScope, draft: String): UnifiedSourceRequest? {
            if (scope.providers.isEmpty()) return null
            return DiscoverySources.query(draft)?.let { UnifiedSourceRequest(scope, it) }
        }
    }
}

/** Reorder inside one provider only. Same-title recordings and versions retain their exact IDs. */
internal fun rankUnifiedSourceResults(
    query: String, items: List<DiscoveryItem>, requireMatch: Boolean = false
): List<DiscoveryItem> {
    val wanted = SearchRelevancePolicy.prepare(query)
    if (!wanted.valid) return if (requireMatch) emptyList() else items
    val candidates = items.map { item ->
        val title = SearchRelevancePolicy.normalize(item.title)
        val creator = SearchRelevancePolicy.normalize(item.creator.orEmpty())
        val identity = SearchRelevancePolicy.SavedIdentity(
            title, creator, SearchTokenMatchingPolicy.tokens("$title $creator"))
        item to identity
    }
    val ranked = candidates.map { (item, identity) -> item to SearchRelevancePolicy.savedScore(wanted, identity) }
    val matches = if (requireMatch) ranked.filter { it.second > 0 } else ranked
    val results = if (requireMatch && matches.isEmpty()) {
        candidates.map { (item, identity) ->
            item to SearchRelevancePolicy.savedScore(wanted, identity, allowTypo = true)
        }.filter { it.second > 0 }
    } else matches
    return results.sortedByDescending { it.second }.map { it.first }
}
