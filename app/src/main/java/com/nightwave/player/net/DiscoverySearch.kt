package com.nightwave.player.net

import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException

internal sealed interface DiscoveryOutcome {
    data class Results(val items: List<DiscoveryItem>) : DiscoveryOutcome
    data object Empty : DiscoveryOutcome
    enum class Problem : DiscoveryOutcome { QUOTA, AUTH, PERMISSION, NETWORK, TIMEOUT, MALFORMED, UNAVAILABLE, UNSUPPORTED }
}

internal data class DiscoveryUpdate(val source: DiscoverySource, val outcome: DiscoveryOutcome)
internal data class DiscoveryResponse(val code: Int, val payload: String?, val retryAfterSeconds: Long? = null)

internal class DiscoverySearch(
    private val fetch: suspend (HttpUrl) -> DiscoveryResponse = DiscoveryHttp::fetch,
    private val timeoutMs: Long = 6_000,
    private val nowNanos: () -> Long = System::nanoTime
) {
    private val quotaUntil = mutableMapOf<DiscoverySource, Long>()

    /** At most one call per selected source. Each outcome is emitted as soon as it finishes. */
    fun search(rawQuery: String, sources: Set<DiscoverySource>): Flow<DiscoveryUpdate> = channelFlow {
        val query = DiscoverySources.query(rawQuery) ?: return@channelFlow
        sources.forEach { source ->
            launch {
                val outcome = withTimeoutOrNull(timeoutMs) { searchOne(query, source) }
                    ?: DiscoveryOutcome.Problem.TIMEOUT
                send(DiscoveryUpdate(source, outcome))
            }
        }
    }

    private suspend fun searchOne(query: String, source: DiscoverySource): DiscoveryOutcome {
        val endpoint = DiscoverySources.endpoint(source, query) ?: return DiscoveryOutcome.Problem.UNSUPPORTED
        if (synchronized(quotaUntil) { quotaUntil[source]?.let { it - nowNanos() > 0 } == true }) {
            return DiscoveryOutcome.Problem.QUOTA
        }
        return try {
            val response = fetch(endpoint)
            when (response.code) {
                200 -> {
                    val payload = response.payload ?: return DiscoveryOutcome.Problem.MALFORMED
                    val items = DiscoverySources.decode(source, payload)
                    if (items.isEmpty()) DiscoveryOutcome.Empty else DiscoveryOutcome.Results(items)
                }
                401 -> DiscoveryOutcome.Problem.AUTH
                403, 451 -> DiscoveryOutcome.Problem.PERMISSION
                429 -> {
                    val seconds = (response.retryAfterSeconds ?: 30L).coerceIn(2L, 300L)
                    synchronized(quotaUntil) { quotaUntil[source] = nowNanos() + TimeUnit.SECONDS.toNanos(seconds) }
                    DiscoveryOutcome.Problem.QUOTA
                }
                else -> DiscoveryOutcome.Problem.UNAVAILABLE
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: JSONException) {
            DiscoveryOutcome.Problem.MALFORMED
        } catch (_: SSLException) {
            DiscoveryOutcome.Problem.UNAVAILABLE
        } catch (_: InterruptedIOException) {
            DiscoveryOutcome.Problem.TIMEOUT
        } catch (_: IOException) {
            DiscoveryOutcome.Problem.NETWORK
        }
    }
}

internal object DiscoveryHttp {
    private const val MAX_BODY_BYTES = 768L * 1024
    private val client = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .retryOnConnectionFailure(false)
        .callTimeout(6, TimeUnit.SECONDS)
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    suspend fun fetch(url: HttpUrl): DiscoveryResponse = suspendCancellableCoroutine { continuation ->
        require(url.isHttps)
        val call = client.newCall(Request.Builder().url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "NightWave/2.0 (Android; source discovery)")
            .build())
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val result = response.use {
                        val payload = if (it.code == 200) it.body?.source()?.let { source ->
                            if (source.request(MAX_BODY_BYTES + 1)) null else source.buffer.readUtf8()
                        } else null
                        DiscoveryResponse(it.code, payload, it.header("Retry-After")?.toLongOrNull())
                    }
                    continuation.resume(result)
                } catch (e: IOException) {
                    continuation.resumeWithException(e)
                }
            }
        })
    }
}
