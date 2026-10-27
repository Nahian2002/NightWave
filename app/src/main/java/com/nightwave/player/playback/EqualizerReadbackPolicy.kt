package com.nightwave.player.playback

import kotlin.math.abs

/**
 * NW88 — verify what Android reports after manual Equalizer.setBandLevel.
 *
 * An enabled Android audio effect can still be bypassed by an output device;
 * this only verifies the framework's current-session band-level readback.
 * Never claim acoustic/route verification from this result.
 */
internal object EqualizerReadbackPolicy {
    enum class Result { VERIFIED, DEVICE_QUANTIZED, MISMATCH, INVALID }

    fun inspectPreset(
        requestedPreset: Int, currentPreset: Int, actualMb: List<Int>,
        minimumMb: Int, maximumMb: Int
    ): Result {
        if (requestedPreset !in 1..20 || actualMb.size !in 1..32 ||
            minimumMb > maximumMb || actualMb.any { it !in minimumMb..maximumMb })
            return Result.INVALID
        return if (currentPreset == requestedPreset - 1) Result.VERIFIED else Result.MISMATCH
    }

    fun inspect(requestedMb: List<Int>, actualMb: List<Int>): Result {
        if (requestedMb.isEmpty() || requestedMb.size !in 1..32 ||
            requestedMb.size != actualMb.size ||
            requestedMb.any { it !in -1500..1500 } ||
            actualMb.any { it !in -1500..1500 }) return Result.INVALID
        val deltas = requestedMb.indices.map {
            abs(requestedMb[it] - actualMb[it])
        }
        // Up to 1 dB of device stepping may be legitimate. Do not call
        // a 3 dB request that actually remains flat a successful apply.
        return when {
            deltas.all { it == 0 } -> Result.VERIFIED
            deltas.all { it <= 100 } -> Result.DEVICE_QUANTIZED
            else -> Result.MISMATCH
        }
    }

    fun activeLabel(result: Result): String = when (result) {
        Result.VERIFIED ->
            "EQ levels verified on Android playback session. Output route may bypass effects."
        Result.DEVICE_QUANTIZED ->
            "EQ applied with device-rounded band levels. Output route may bypass effects."
        Result.MISMATCH ->
            "EQ band readback does not match the requested levels; effect disabled."
        Result.INVALID ->
            "EQ verification unavailable for this audio session."
    }
}
