package com.nightwave.player.offline

enum class OfflineImportPhase { COPYING, CHECKING_AUDIO, SAVING }

/** Presentation and bounded batching only; no storage, permissions or provider eligibility changes. */
internal object OfflineImportJourneyPolicy {
    const val MAX_BATCH = 100
    const val FORMAT_HELP = "MP3, M4A, AAC, FLAC, WAV, OGG, OPUS, MP4, WebM and 3GP · up to 250 MB per file"
    const val SOURCE_HELP = "Import audio you own or may store. NightWave copies it to private storage; the original stays in place."
    fun empty(indexReadable: Boolean, indexedCount: Int): Boolean = indexReadable && indexedCount == 0
    fun progress(position: Int, total: Int, phase: OfflineImportPhase): String {
        val verb = when (phase) {
            OfflineImportPhase.COPYING -> "Copying"
            OfflineImportPhase.CHECKING_AUDIO -> "Checking audio"
            OfflineImportPhase.SAVING -> "Saving"
        }
        val boundedTotal = total.coerceIn(1, MAX_BATCH)
        return "$verb ${position.coerceIn(1, boundedTotal)} of $boundedTotal"
    }
    fun result(saved: Int, failed: Int, skipped: Int): String =
        "$saved " + (if (saved == 1) "file" else "files") + " checked and saved" +
            (if (failed > 0) " · $failed failed" else "") +
            (if (skipped > 0) " · $skipped not imported (100-file batch limit)" else "")
}

/** A readable sample is stronger than an extension, but does not certify an entire playback/seek. */
internal object OfflineAudioValidationPolicy {
    fun firstAudioTrack(mimes: List<String>): Int = mimes.indexOfFirst { it.startsWith("audio/") }
    fun hasReadableSample(trackIndex: Int, sampleBytes: Int): Boolean = trackIndex >= 0 && sampleBytes > 0
}
