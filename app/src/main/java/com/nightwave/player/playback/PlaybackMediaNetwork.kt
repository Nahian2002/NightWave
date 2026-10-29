package com.nightwave.player.playback

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Playback pool is separate from extractor metadata and authenticated Drive operations. */
internal object PlaybackMediaNetwork {
    const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 Chrome/121.0 Mobile Safari/537.36"
    const val MIN_BUFFER_MS = 50_000
    const val MAX_BUFFER_MS = 120_000
    const val START_BUFFER_MS = 2_500
    const val REBUFFER_MS = 5_000
    const val TARGET_BYTES = 16 * 1024 * 1024

    // HTTP/2 negotiation with ordinary HTTP/1.1 fallback. No cookies, disk cache,
    // credentials, fixed Wi-Fi binding, permanent media URLs or whole-call deadline.
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true).followSslRedirects(true).build()
    }
}
