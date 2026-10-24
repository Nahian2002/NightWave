package com.nightwave.player.offline

/**
 * Display only bounded, non-sensitive categories. Extractor/HTTP failures
 * can contain signed CDN URLs or server details; never echo the raw exception.
 */
object SearchOfflineFailurePolicy {
    fun message(details: String?, networkInterrupted: Boolean): String {
        val detail = details.orEmpty().lowercase(java.util.Locale.ROOT)
        return when {
            "not enough device storage" in detail ||
                "device storage is too low" in detail ->
                "Not enough device storage. Free space and try again."
            "http 403" in detail || "http 401" in detail ->
                "Audio source denied access. This recording was not saved."
            "http 429" in detail ->
                "Audio source is busy or rate-limited. Retry later."
            "no independent audio-only stream" in detail ||
                "no supported public audio stream" in detail ->
                "No supported standalone audio recording is available."
            "could not validate the audio file" in detail ||
                "invalid audio" in detail ||
                "no readable audio samples" in detail ||
                "contains video" in detail ->
                "The source returned unplayable or unsupported audio."
            "unsafe" in detail || "untrusted" in detail ||
                "not permitted" in detail ->
                "The audio source failed a security or permission check."
            networkInterrupted ->
                "Connection interrupted before audio was saved. Retry when online."
            else ->
                "This recording could not be stored. It is not available offline."
        }
    }
}
