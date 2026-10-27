package com.nightwave.player.v2.ui

/**
 * NW123: pure accessibility labels and minimum dimensions. The original
 * Unicode (including Bangla combining marks) is never rewritten or truncated.
 */
object NightWaveAccessibilityPolicy {
    private fun safeFontScale(fontScale: Float): Float =
        if (fontScale.isFinite()) fontScale.coerceIn(0.7f, 3f) else 1f

    /** Compose's Tab role and selected state remain independent. */
    fun navigationAnnouncement(label: String): String =
        (label.ifBlank { "Destination" }) + " tab"

    /** Queue reorder/remove actions remain separate accessibility nodes. */
    fun queueAnnouncement(
        number: Int, title: String, artist: String, playing: Boolean
    ): String {
        val verb = if (playing) "Playing" else "Play"
        val name = title.ifBlank { "Untitled track" }
        val performer = if (artist.isBlank()) "" else ", " + artist
        return verb + " track " + number.coerceAtLeast(1) + ": " + name + performer
    }

    fun featureCardMinHeightDp(fontScale: Float): Int = when {
        safeFontScale(fontScale) >= 2.4f -> 324
        safeFontScale(fontScale) >= 1.8f -> 270
        safeFontScale(fontScale) >= 1.3f -> 214
        else -> 164
    }

    fun mixCardMinHeightDp(compact: Boolean, fontScale: Float): Int {
        val scale = safeFontScale(fontScale)
        return when {
            scale >= 2.4f -> if (compact) 252 else 282
            scale >= 1.8f -> if (compact) 192 else 222
            scale >= 1.3f -> if (compact) 152 else 180
            else -> if (compact) 118 else 146
        }
    }

    fun moodCardMinHeightDp(fontScale: Float): Int = when {
        safeFontScale(fontScale) >= 2.4f -> 224
        safeFontScale(fontScale) >= 1.8f -> 184
        safeFontScale(fontScale) >= 1.3f -> 146
        else -> 118
    }
}
