package com.nightwave.player.offline

import com.nightwave.player.v2.ui.NightWaveArtworkPolicy
import java.net.URI

/** Bounded optional presentation work; never a decision about audio readiness. */
internal object OfflineArtworkRecoveryPolicy {
    const val MAX_BATCH = 24
    const val BUDGET_MS = 2_000L
    private val sourceKey = Regex("v_[A-Za-z0-9_-]{11}")
    private val localId = Regex("[0-9a-f-]{36}\\.(mp3|m4a|aac|flac|wav|ogg|opus|mp4|webm|3gp)")

    fun uniqueLinks(entries: List<Pair<String, String>>): Map<String, String> = entries
        .asSequence().filter { sourceKey.matches(it.first) && localId.matches(it.second) }
        .take(OfflineAudioLibrary.MAX_SONGS).groupBy({ it.second }, { it.first.removePrefix("v_") })
        .mapNotNull { (id, videos) -> videos.distinct().singleOrNull()?.let {
            id to "https://www.youtube.com/watch?v=$it"
        } }.toMap()

    fun batch(songs: List<OfflineSong>, attempted: Set<String>): List<OfflineSong> = songs.asSequence()
        .filter { localId.matches(it.id) && it.id !in attempted }.distinctBy { it.id }.take(MAX_BATCH).toList()

    fun cacheCandidates(videoId: String, original: String): List<String> {
        if (!Regex("[A-Za-z0-9_-]{11}").matches(videoId)) return emptyList()
        val base = "https://i.ytimg.com/vi/$videoId/"
        val safeOriginal = runCatching {
            val uri = URI(original)
            if (uri.rawUserInfo != null || uri.scheme != "https") ""
            else if (uri.host?.lowercase(java.util.Locale.ROOT) in setOf("i.ytimg.com", "img.youtube.com") &&
                (uri.path.split('/').getOrNull(2) != videoId || uri.rawQuery != null || uri.rawFragment != null)) ""
            else original
        }.getOrDefault("")
        return (listOf(safeOriginal, base + "hqdefault.jpg", base + "mqdefault.jpg",
            base + "default.jpg", base + "0.jpg").flatMap {
            NightWaveArtworkPolicy.requestCandidates(it, true) +
                NightWaveArtworkPolicy.requestCandidates(it, false)
        }).distinct().take(12)
    }
}

internal data class OfflineArtworkCommitFence(
    val song: OfflineSong, val metadata: String?, val videoId: String, val requiresLink: Boolean
) {
    fun allows(current: OfflineSong?, currentMetadata: String?, linkedId: String?, active: Boolean): Boolean =
        active && song == current && metadata == currentMetadata && (!requiresLink || linkedId == song.id)
}

/** A completion for an old selection/index/revision may never update the player. */
internal object OfflineArtworkPlaybackFence {
    fun allows(expectedMedia: String, currentMedia: String?, selectedId: String?,
        expectedGeneration: Int, generation: Int, expectedIndex: String?, currentIndex: String?,
        expectedRevision: Long, revision: Long, offline: Boolean): Boolean =
        offline && expectedMedia.startsWith("offline:") && expectedMedia == currentMedia &&
            expectedMedia == "offline:$selectedId" && expectedGeneration == generation &&
            expectedIndex == currentIndex && expectedRevision == revision
}
