package com.nightwave.player.v2.insights

/**
 * Counts only time the Media3 player reports isPlaying=true, using a monotonic
 * clock. A play is qualified once per continuous track session after 30 s of
 * confirmed playback, not when a track is merely selected or buffering.
 *
 * Pure state machine; only the playback service may own an instance.
 */
data class ListeningPulse(
    val trackId: String,
    val title: String,
    val artist: String,
    val listenedMs: Long,
    val qualifyingPlay: Boolean,
    val recordedAtMs: Long
)

class ListeningSessionMeter {
    companion object {
        const val MIN_PLAY_MS = 30_000L
        const val FLUSH_MS = 10_000L
        // Avoid charging the app for hours if Android suspends a runnable.
        const val MAX_OBSERVED_GAP_MS = 5_000L
    }

    private var id: String? = null
    private var title = ""
    private var artist = ""
    private var previousElapsed: Long? = null
    private var wasPlaying = false
    private var sessionMs = 0L
    private var pendingMs = 0L
    private var pendingQualifyingPlay = false
    private var playCredited = false

    fun reset() {
        id = null
        title = ""
        artist = ""
        previousElapsed = null
        wasPlaying = false
        sessionMs = 0L
        pendingMs = 0L
        pendingQualifyingPlay = false
        playCredited = false
    }

    fun finish(nowElapsedMs: Long, nowWallMs: Long): List<ListeningPulse> {
        val ended = observe(id, title, artist, false, nowElapsedMs, nowWallMs)
        reset()
        return ended
    }

    fun observe(
        trackId: String?,
        trackTitle: String,
        trackArtist: String,
        playing: Boolean,
        nowElapsedMs: Long,
        nowWallMs: Long
    ): List<ListeningPulse> {
        val emitted = mutableListOf<ListeningPulse>()
        if (id != trackId) {
            // Commit the elapsed tail of the outgoing song before updating its
            // identity. A transition must never attribute old audio to new art.
            accrue(nowElapsedMs)
            emitPending(nowWallMs)?.let(emitted::add)
            reset()
            id = trackId?.takeIf { it.isNotBlank() }
            title = trackTitle.take(180)
            artist = trackArtist.take(120)
        }
        if (id == null) return emitted
        accrue(nowElapsedMs)
        wasPlaying = playing
        previousElapsed = nowElapsedMs
        if (!playing || pendingMs >= FLUSH_MS || pendingQualifyingPlay) {
            emitPending(nowWallMs)?.let(emitted::add)
        }
        return emitted
    }

    private fun accrue(nowElapsedMs: Long) {
        val prev = previousElapsed
        if (wasPlaying && prev != null) {
            val elapsed = (nowElapsedMs - prev).coerceIn(0L, MAX_OBSERVED_GAP_MS)
            pendingMs += elapsed
            sessionMs += elapsed
            if (!playCredited && sessionMs >= MIN_PLAY_MS) {
                pendingQualifyingPlay = true
                playCredited = true
            }
        }
    }

    private fun emitPending(wallMs: Long): ListeningPulse? {
        val current = id ?: return null
        if (pendingMs <= 0L && !pendingQualifyingPlay) return null
        val item = ListeningPulse(
            trackId = current, title = title, artist = artist,
            listenedMs = pendingMs, qualifyingPlay = pendingQualifyingPlay,
            recordedAtMs = wallMs.coerceAtLeast(0L)
        )
        pendingMs = 0L
        pendingQualifyingPlay = false
        return item
    }
}
