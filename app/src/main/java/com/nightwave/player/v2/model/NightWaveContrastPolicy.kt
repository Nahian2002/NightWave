package com.nightwave.player.v2.model

/** WCAG contrast calculation for the existing 32-bit ARGB theme palettes.
 * This is a source/test policy, not automatic recoloring of user artwork.
 */
object NightWaveContrastPolicy {
    private fun linear(value: Int): Double {
        val channel = value.coerceIn(0, 255) / 255.0
        return if (channel <= 0.04045) channel / 12.92
        else Math.pow((channel + 0.055) / 1.055, 2.4)
    }

    fun luminance(argb: Long): Double {
        val red = ((argb ushr 16) and 0xFF).toInt()
        val green = ((argb ushr 8) and 0xFF).toInt()
        val blue = (argb and 0xFF).toInt()
        return 0.2126 * linear(red) + 0.7152 * linear(green) +
            0.0722 * linear(blue)
    }

    fun ratio(foreground: Long, background: Long): Double {
        val l1 = luminance(foreground)
        val l2 = luminance(background)
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    /** Normal-sized text contrast target; larger bold text can use 3.0. */
    const val NORMAL_TEXT_RATIO = 4.5

    fun normalTextPasses(foreground: Long, background: Long): Boolean =
        ratio(foreground, background) >= NORMAL_TEXT_RATIO
}
