package com.nightwave.player.offline

import java.util.Locale

/**
 * NW65 presentation-only recovery policy. WorkManager outputData may contain
 * exception text with signed audio URLs, local paths or tokens. Classify it
 * without ever interpolating or logging that untrusted text.
 *
 * This does not change download eligibility, retries, cancellation or media.
 */
internal object DownloadRecoveryPolicy {
    fun failedMessage(raw: String?): String {
        val text = raw.orEmpty().lowercase(Locale.ROOT)
        return when {
            "saved audio could not be verified" in text ||
            "saved audio needs verification" in text ||
                "saved file could not be verified" in text ->
                "Saved audio needs verification. Check Offline Library before retrying."
            "audio may already be in offline library" in text ||
            "search link was not recorded" in text ||
                "search association failed" in text ->
                "Audio may already be in Offline Library. Check there before retrying."
            "not enough device storage" in text ||
                "not enough private storage" in text ||
                "not enough storage" in text ||
                "device storage is too low" in text ||
                "no space left" in text || "enospc" in text ||
                "insufficient storage" in text ->
                "Not enough storage. Free device space, then retry."
            "partial audio" in text ->
                "Partial audio response. No offline copy was saved; retry with a full permitted recording."
            "http 429" in text || "rate-limit" in text ||
                "too many requests" in text ->
                "Audio source is rate-limited. Retry later."
            "http 500" in text || "http 502" in text ||
                "http 503" in text || "http 504" in text ||
                "source is temporarily unavailable" in text ->
                "Audio source is temporarily unavailable. Retry later."
            "http 408" in text || "http 425" in text ->
                "Connection interrupted. Check internet and retry."
            "http 401" in text || "http 403" in text ||
                "access denied" in text || "denied access" in text ||
                "unauthorized" in text || "forbidden" in text ->
                "Audio source denied access. Try a permitted alternative."
            "no independent audio-only stream" in text ||
                "no supported public audio stream" in text ||
                "no supported standalone audio" in text ||
                "no supported playable audio" in text ||
                "unsupported format" in text ||
                "invalid audio" in text || "unplayable" in text ||
                "no readable audio samples" in text ||
                "contains video" in text ->
                "No supported playable audio. Try another authorized recording."
            "unsafe" in text || "untrusted" in text ||
                "not permitted" in text || "permission check" in text ->
                "Audio failed a safety or permission check."
            "http 404" in text || "not found" in text ||
                "recording unavailable" in text ||
                "no longer available" in text ->
                "Recording is no longer available. Try another source."
            "connection interrupted" in text || "connection reset" in text ||
                "timeout" in text || "timed out" in text ||
                "network" in text || "offline" in text ->
                "Connection interrupted. Check internet and retry."
            else -> "Download failed. This recording is not available offline."
        }
    }

    /** A retry is a new clean transfer, never persisted partial bytes. */
    fun queuedLabel(previousAttempts: Int): String =
        if (previousAttempts > 0)
            "Retry scheduled · Waiting for network/backoff · Starts from zero"
        else "Queued · Waiting for network"

    /** Only a WorkManager cancellation with a retained pause marker is paused. */
    fun isPaused(state: String, pauseMarked: Boolean): Boolean =
        state == "CANCELLED" && pauseMarked

    /** Pause is a cancellation request, never a stored partial byte checkpoint. */
    fun isPausing(state: String, pauseMarked: Boolean): Boolean =
        pauseMarked && state in setOf("ENQUEUED", "RUNNING", "BLOCKED")

    fun mayRequestPause(state: String, pauseMarked: Boolean,
                        eligibleSource: Boolean): Boolean =
        eligibleSource && !pauseMarked &&
            state in setOf("ENQUEUED", "RUNNING", "BLOCKED")

    /** The user may retry only after the old attempt has actually finished. */
    fun retryLabel(state: String, pauseMarked: Boolean): String =
        if (isPaused(state, pauseMarked)) "Resume (restarts from zero)"
        else "Retry saving this song"
}
