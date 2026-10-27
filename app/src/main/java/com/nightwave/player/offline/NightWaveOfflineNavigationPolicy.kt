package com.nightwave.player.offline

/** UI routes only. WorkManager, file ownership and MediaSession own their data. */
enum class NightWaveOfflineRoute(val label: String, val activityName: String) {
    LIBRARY("On device", "OfflineLibraryActivity"),
    CATALOG("Find audio", "FreeAudioCatalogActivity"),
    DOWNLOADS("Downloads", "CatalogDownloadsActivity"),
    LINK("Audio link", "PermittedAudioDownloadActivity");

    val componentName: String get() = "com.nightwave.player.offline.$activityName"
}

data class NightWaveMainNavigationRequest(val id: String, val tab: Int, val openPlayer: Boolean)
data class NightWaveCatalogDraft(val query: String, val originTitle: String, val originArtist: String)

object NightWaveOfflineNavigationPolicy {
    const val MAX_QUERY = 160
    fun route(component: String?): NightWaveOfflineRoute? =
        NightWaveOfflineRoute.entries.firstOrNull { it.componentName == component }

    /** Bound UI state only; never persist provider results, URIs or permission consent. */
    fun query(raw: String?): String = raw.orEmpty().trim().take(MAX_QUERY)
    fun page(raw: Int): Int = raw.coerceIn(0, 100_000)
    fun scroll(raw: Int): Int = raw.coerceIn(0, 1_000_000)
    fun sort(raw: Int): Int = raw.takeIf { it in 0..3 } ?: 0

    /** An edited query is not a replacement for the source title/artist pair. */
    fun catalogDraft(savedQuery: String?, savedTitle: String?, savedArtist: String?,
        incomingTitle: String?, incomingArtist: String?): NightWaveCatalogDraft =
        NightWaveCatalogDraft(query(savedQuery ?: incomingTitle),
            query(savedTitle ?: incomingTitle), query(savedArtist ?: incomingArtist))

    fun needsMainRoot(baseComponent: String?): Boolean =
        baseComponent == "com.nightwave.player.ui.MainActivity" || route(baseComponent) != null

    fun mainRequest(id: String?, tab: Int, player: Boolean,
        lastApplied: String?): NightWaveMainNavigationRequest? {
        if (id == null || !id.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) ||
            id == lastApplied || tab !in 0..3) return null
        return NightWaveMainNavigationRequest(id, tab, player)
    }
}
