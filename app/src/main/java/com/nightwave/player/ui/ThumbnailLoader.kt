package com.nightwave.player.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.widget.ImageView
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/** Bounded, memory-cached thumbnail fetcher; no arbitrary file-system access. */
class ThumbnailLoader(context: Context) {
    private val executor = Executors.newFixedThreadPool(3)
    private val maxKb = (Runtime.getRuntime().maxMemory() / 1024L / 32L).coerceIn(2048, 8192).toInt()
    private val cache = object : LruCache<String, Bitmap>(maxKb) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }
    private val ui = android.os.Handler(context.mainLooper)

    @Volatile private var closed = false
    fun load(url: String, into: ImageView) {
        into.tag = null
        into.setImageDrawable(null)
        if (!url.startsWith("https://") || closed) return
        val request = Any()
        into.tag = request
        into.post {
            if (closed || into.tag !== request) return@post
            val target = NativeThumbnailPolicy.target(into.width, into.height)
            val key = "$target:$url"
            cache.get(key)?.let { into.setImageBitmap(it); return@post }
            runCatching { executor.execute {
                val bitmap = try {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.connectTimeout = 6_000
                    connection.readTimeout = 6_000
                    connection.instanceFollowRedirects = true
                    try {
                        val declaredBytes = connection.getHeaderField("Content-Length")?.trim()?.toLongOrNull() ?: -1L
                        if (connection.responseCode !in 200..299 || declaredBytes > 3_000_000) null
                        else connection.inputStream.use { input ->
                            val bytes = NativeThumbnailPolicy.read(input)
                            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                            val sample = NativeThumbnailPolicy.sample(bounds.outWidth, bounds.outHeight, target)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
                                BitmapFactory.Options().apply { inSampleSize = sample })
                        }
                    } finally { connection.disconnect() }
                } catch (_: Exception) { null }
                if (bitmap != null && !closed) {
                    cache.put(key, bitmap)
                    ui.post { if (!closed && into.tag === request) into.setImageBitmap(bitmap) }
                }
            } }
        }
    }

    fun shutdown() { closed = true; executor.shutdownNow() }
}
