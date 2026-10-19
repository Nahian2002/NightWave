package com.nightwave.player.playback

import kotlin.math.min

/**
 * Conservative offline loudness balancing: attenuate loud tracks only.
 * Never requests gain > unity, alters the system volume or guesses streaming LUFS.
 * RMS is from a bounded on-device decoded PCM sample, NOT integrated EBU R128 loudness.
 */
object OfflineNormalizationPolicy {
    const val TARGET_RMS = 0.13f
    const val MAX_SAMPLED_MS = 12_000L
    const val MAX_ANALYSIS_WALL_MS = 3_000L
    const val MAX_SAMPLES = 350_000L

    fun attenuation(rms: Float, peak: Float): Float? {
        if (!rms.isFinite() || !peak.isFinite() || rms <= 0f ||
            peak <= 0f || rms > 1.1f || peak > 1.1f || rms > peak + .001f) return null
        return min(1f, min(TARGET_RMS / rms, .98f / peak)).coerceIn(.25f, 1f)
    }

    fun gain(enabled: Boolean, measured: Float?): Float =
        if (enabled && measured != null && measured.isFinite() &&
            measured in .25f..1f) measured else 1f

    fun combined(listeningGain: Float, normalizedGain: Float): Float =
        (listeningGain.coerceIn(0f, 1f) * normalizedGain.coerceIn(0f, 1f))
            .coerceIn(0f, 1f)
}
