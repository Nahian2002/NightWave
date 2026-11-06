package com.nightwave.player.v2.ui

internal object NightWaveArtworkFramePolicy {
    data class SideTrim(val left: Int, val right: Int)

    /** Keep edge detail unless both sides are solid, matching and nearly symmetric. */
    fun sideTrim(width: Int, height: Int, pixel: (Int, Int) -> Int): SideTrim? {
        if (width < 120 || height < 80 || width.toFloat() / height !in 1.45f..2.1f) return null
        val rows = (1..9).map { height * it / 10 }
        val edge = pixel(0, height / 2)
        if ((edge ushr 24) != 255) return null
        fun closeChannel(color: Int, shift: Int): Boolean =
            kotlin.math.abs(((edge shr shift) and 255) - ((color shr shift) and 255)) <= 12
        fun sameColor(color: Int): Boolean =
            (color ushr 24) == 255 && closeChannel(color, 0) &&
                closeChannel(color, 8) && closeChannel(color, 16)
        fun count(fromRight: Boolean): Int {
            val cap = (width * 0.27f).toInt()
            for (step in 0..cap) {
                val x = if (fromRight) width - 1 - step else step
                if (rows.count { sameColor(pixel(x, it)) } < 8) return step
            }
            return 0
        }
        val left = count(false)
        val right = count(true)
        val contentWidth = width - left - right
        val minSide = (width * 0.08f).toInt()
        if (left < minSide || right < minSide || kotlin.math.abs(left - right) > width * 0.03f ||
            contentWidth.toFloat() / height !in 0.88f..1.36f) return null
        if (rows.count { !sameColor(pixel(width / 2, it)) } < 6) return null
        return SideTrim(left, right)
    }

    /** Only landscape images shorten the frame. Portrait/square art retains safe square containment. */
    fun heightDp(widthDp: Float, sourceWidth: Int, sourceHeight: Int, offline: Boolean): Float {
        val width = widthDp.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
        val ratio = if (sourceWidth > 0 && sourceHeight > 0)
            (sourceWidth.toFloat() / sourceHeight).coerceIn(1f, 1.9f)
        else if (offline) 1f else 4f / 3f
        return width / ratio
    }
}
