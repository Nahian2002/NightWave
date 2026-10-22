package com.nightwave.player.offline

/** Conservative signatures; Android MediaExtractor still validates full audio after download. */
object AudioFrameSignature {
    fun isMp3Frame(prefix: ByteArray): Boolean {
        if (prefix.size < 4) return false
        val a = prefix[0].toInt() and 255
        val b = prefix[1].toInt() and 255
        val c = prefix[2].toInt() and 255
        if (a != 255 || (b and 0xE0) != 0xE0) return false
        val version = (b ushr 3) and 3
        val layer = (b ushr 1) and 3
        val bitrateIndex = (c ushr 4) and 15
        val sampleIndex = (c ushr 2) and 3
        return version != 1 && layer != 0 && bitrateIndex in 1..14 && sampleIndex != 3
    }

    fun isAacAdts(prefix: ByteArray): Boolean {
        if (prefix.size < 7) return false
        val a = prefix[0].toInt() and 255
        val b = prefix[1].toInt() and 255
        val c = prefix[2].toInt() and 255
        val d = prefix[3].toInt() and 255
        val e = prefix[4].toInt() and 255
        val f = prefix[5].toInt() and 255
        if (a != 255 || (b and 0xF6) != 0xF0) return false
        val sampleIndex = (c ushr 2) and 15
        if (sampleIndex == 15) return false
        val headerBytes = if ((b and 1) == 1) 7 else 9
        val frameBytes = ((d and 3) shl 11) or (e shl 3) or ((f and 0xE0) ushr 5)
        return frameBytes >= headerBytes
    }
}
