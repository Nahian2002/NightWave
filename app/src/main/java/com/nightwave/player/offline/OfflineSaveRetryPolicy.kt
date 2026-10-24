package com.nightwave.player.offline

/**
 * WorkManager exposes tags, but not the original WorkRequest input. Keep the
 * public 11-character video identity (not signed audio URLs) as a bounded tag
 * so a failed user-approved save can be explicitly retried in Downloads.
 * Existing older jobs without the identity tag remain safe to display.
 */
object OfflineSaveRetryPolicy {
    const val SOURCE_TAG_PREFIX = "nightwave-search-video-id:"
    const val CREATED_TAG_PREFIX = "nightwave-created-ms:"
    private val idPattern = Regex("^[A-Za-z0-9_-]{11}$")

    fun sourceTag(canonicalUrl: String): String {
        val video = SearchTrackSavePolicy.canonicalTrack(canonicalUrl)
            ?: throw IllegalArgumentException("Unsupported search track")
        val id = video.substringAfter("v=", "")
        require(idPattern.matches(id)) { "Invalid video identity" }
        return SOURCE_TAG_PREFIX + id
    }

    fun retryUrl(tags: Set<String>): String? {
        val ids = tags.filter { it.startsWith(SOURCE_TAG_PREFIX) }
        if (ids.size != 1) return null
        val id = ids.single().removePrefix(SOURCE_TAG_PREFIX)
        return id.takeIf(idPattern::matches)?.let {
            "https://www.youtube.com/watch?v=$it"
        }
    }

    fun requestedAt(tags: Set<String>): Long =
        tags.asSequence()
            .filter { it.startsWith(CREATED_TAG_PREFIX) }
            .mapNotNull { it.removePrefix(CREATED_TAG_PREFIX).toLongOrNull() }
            .filter { it in 1L..4102444800000L }
            .maxOrNull() ?: 0L

    fun canOfferRetry(finished: Boolean, ready: Boolean, tags: Set<String>): Boolean =
        finished && !ready && retryUrl(tags) != null
}
