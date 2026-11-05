package com.nightwave.player.ui

internal object NativeThumbnailPolicy {
    const val MAX_BYTES = 3_000_000
    fun read(input: java.io.InputStream): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            if (n == 0) {
                val single = input.read()
                if (single < 0) break
                require(output.size() < MAX_BYTES)
                output.write(single)
            } else {
                require(output.size().toLong() + n <= MAX_BYTES)
                output.write(buffer, 0, n)
            }
        }
        return output.toByteArray()
    }
    fun target(width: Int, height: Int): Int = maxOf(width, height).takeIf { it > 0 }
        ?.coerceIn(64, 1024)?.let { ((it + 63) / 64) * 64 } ?: 512
    fun sample(width: Int, height: Int, target: Int): Int {
        require(width > 0 && height > 0 && target in 64..1024)
        var sample = 1
        while (maxOf(width, height) / (sample * 2) >= target ||
            ((width.toLong() + sample - 1) / sample) * ((height.toLong() + sample - 1) / sample) > 1_048_576L) sample *= 2
        return sample
    }
}
