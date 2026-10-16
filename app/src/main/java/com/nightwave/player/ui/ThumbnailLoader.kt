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

    fun load(url: String, into: ImageView) {
        if (!url.startsWith("https://")) return
        into.tag = url
        cache.get(url)?.let { into.setImageBitmap(it); return }
        executor.execute {
            val bitmap = try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 6_000
                connection.readTimeout = 6_000
                connection.instanceFollowRedirects = true
                try {
                    if (connection.responseCode !in 200..299 || connection.contentLengthLong > 3_000_000) null
                    else connection.inputStream.use { input ->
                        BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply { inSampleSize = 2 })
                    }
                } finally { connection.disconnect() }
            } catch (_: Exception) { null }
            if (bitmap != null) {
                cache.put(url, bitmap)
                ui.post { if (into.tag == url) into.setImageBitmap(bitmap) }
            }
        }
    }

    fun shutdown() = executor.shutdownNow()
}
