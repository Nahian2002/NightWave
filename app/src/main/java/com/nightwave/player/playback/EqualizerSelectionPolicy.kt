package com.nightwave.player.playback

/**
 * A user's durable EQ intent is distinct from Android audio-effect activity.
 * No device/session ID, raw audio, account or private file is persisted.
 */
internal data class EqualizerSelection(
    val preset: Int,
    val centersHz: List<Int> = emptyList(),
    val levelsMb: List<Int> = emptyList()
)

internal object EqualizerSelectionPolicy {
    /** Presets 1..20 are hardware-defined; 21 represents a full manual curve. */
    fun valid(selection: EqualizerSelection): Boolean =
        when (selection.preset) {
            0 -> selection.centersHz.isEmpty() && selection.levelsMb.isEmpty()
            in 1..20 -> selection.centersHz.isEmpty() && selection.levelsMb.isEmpty()
            21 -> selection.centersHz.size in 1..32 &&
                selection.levelsMb.size == selection.centersHz.size &&
                selection.centersHz.all { it in 1..100_000 } &&
                selection.centersHz.distinct().size == selection.centersHz.size &&
                selection.levelsMb.all { it in -1500..1500 }
            else -> false
        }

    /**
     * Equal band count alone is not safe: 5-band outputs can have completely
     * different frequency centers. Never map bass gain onto a treble band.
     */
    fun matchesSession(selection: EqualizerSelection, centersHz: List<Int>): Boolean =
        valid(selection) && when (selection.preset) {
            21 -> centersHz == selection.centersHz
            else -> true
        }
}
