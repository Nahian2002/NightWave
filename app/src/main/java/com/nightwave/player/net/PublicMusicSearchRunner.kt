package com.nightwave.player.net

import kotlinx.coroutines.*
import okhttp3.Call
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

enum class PublicSearchPhase(val label: String) {
    DIRECT("Searching public videos…"), COMMUNITY("Trying independent public search…")
}

class PublicSearchDeadlineException : IOException("Public search timed out")
class PublicSearchPhaseDeadlineException : IOException("Public search provider phase timed out")

/** One search owns only its own HTTP calls. Playback/download requests are never cancelled. */
class PublicSearchRequest(timeoutMillis: Long = 20_000) {
    private val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
    private var phaseDeadline = deadline
    private val diagnostics = PublicSearchDiagnostics()
    private val calls = mutableSetOf<Call>()
    @Volatile private var cancelled = false

    fun beginPhase(phase: PublicSearchPhase, directBudgetMillis: Long = 8_000L) {
        checkActive()
        phaseDeadline = if (phase == PublicSearchPhase.DIRECT)
            minOf(deadline, System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(directBudgetMillis.coerceIn(1, PublicMusicSearchRunner.TIMEOUT_MS)))
            else deadline
        diagnostics.begin(phase)
    }
    fun cacheHit() = diagnostics.cacheHit()
    fun details(): String = diagnostics.snapshot()
    internal fun <T> commitIfActive(block: () -> T): T = synchronized(calls) { checkActive(); block() }

    fun checkActive() {
        if (cancelled) throw CancellationException("Public search cancelled")
        if (System.nanoTime() >= deadline) throw PublicSearchDeadlineException()
    }

    fun cancel() = synchronized(calls) {
        cancelled = true
        calls.toList().forEach { it.cancel() }
    }

    internal fun <T> execute(call: Call, consume: (Response) -> T): T {
        synchronized(calls) {
            checkActive()
            if (System.nanoTime() >= phaseDeadline) throw PublicSearchPhaseDeadlineException()
            val remaining = TimeUnit.NANOSECONDS.toMillis(minOf(deadline, phaseDeadline) - System.nanoTime()).coerceAtLeast(1)
            val configured = call.timeout().timeoutNanos()
            val budget = if (configured > 0) minOf(TimeUnit.NANOSECONDS.toMillis(configured).coerceAtLeast(1), remaining) else remaining
            call.timeout().timeout(budget, TimeUnit.MILLISECONDS)
            calls.add(call)
        }
        val started = System.nanoTime()
        try {
            return call.execute().use { response -> checkActive(); consume(response).also { checkActive() } }
        } finally {
            diagnostics.httpCompleted(System.nanoTime() - started)
            synchronized(calls) { calls.remove(call) }
        }
    }

    internal fun <T> scoped(block: () -> T): T {
        val previous = current.get()
        current.set(this)
        try { checkActive(); return block().also { checkActive() } }
        finally { current.set(previous) }
    }

    companion object {
        // NewPipe's synchronous downloader runs on the same IO thread. Never global call cancellation.
        internal val current = ThreadLocal<PublicSearchRequest?>()
        internal fun <T> executeHttp(call: Call, consume: (Response) -> T): T =
            current.get()?.execute(call, consume) ?: call.execute().use(consume)
    }
}

object PublicMusicSearchRunner {
    const val TIMEOUT_MS = 20_000L

    /** The watcher cancels active sockets even while a synchronous extractor blocks its IO thread. */
    suspend fun run(timeoutMillis: Long = TIMEOUT_MS, search: (PublicSearchRequest) -> VideoSearchResults): VideoSearchResults {
        val request = PublicSearchRequest(timeoutMillis)
        return withTimeout(timeoutMillis) {
            coroutineScope {
                val watcher = launch(start = CoroutineStart.UNDISPATCHED) {
                    try { awaitCancellation() } finally { request.cancel() }
                }
                try { withContext(Dispatchers.IO) { request.scoped { search(request) } } }
                finally { watcher.cancel() }
            }
        }
    }
}
