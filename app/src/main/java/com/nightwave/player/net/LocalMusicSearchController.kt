package com.nightwave.player.net

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest

/** The same production typing runner is used by Compose and opt-in JVM measurements. */
object LocalMusicQueryRunner {
    suspend fun run(index: LocalMusicSearchIndex, query: String): LocalMusicSearchResult {
        delay(120)
        return withContext(Dispatchers.Default) {
            val task = currentCoroutineContext()
            index.search(query) { task.ensureActive() }
        }
    }
}

data class LocalMusicCorpusState(val revision: Long, val refresh: Int,
    val index: LocalMusicSearchIndex? = null, val failed: Boolean = false)

/** One conflated builder: cancel AND join old work before starting newer work.
 * Injected readers dispatch IO themselves; preparation always uses Default.
 * No global index, personal-data persistence or polling timer.
 */
class LocalMusicSearchController(scope: CoroutineScope, private val read: suspend () -> LocalMusicSnapshot) {
    private data class Request(val revision: Long = 0, val refresh: Int = Int.MIN_VALUE, val active: Boolean = false)
    private val lock = Any()
    private val requests = MutableStateFlow(Request())
    private val mutable = MutableStateFlow(LocalMusicCorpusState(0, Int.MIN_VALUE))
    val state: StateFlow<LocalMusicCorpusState> = mutable.asStateFlow()
    private val job = scope.launch {
        requests.collectLatest { request ->
            if (!request.active) return@collectLatest
            if (mutable.value.index != null && mutable.value.revision == request.revision) return@collectLatest
            try {
                delay(80) // coalesce multi-key commits; old results were already invalidated.
                val snapshot = read()
                val index = withContext(Dispatchers.Default) {
                    val task = currentCoroutineContext()
                    LocalMusicSearchIndex.build(snapshot) { task.ensureActive() }
                }
                publish(request, LocalMusicCorpusState(request.revision, request.refresh, index))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                publish(request, LocalMusicCorpusState(request.revision, request.refresh, failed = true))
            }
        }
    }

    private fun publish(request: Request, state: LocalMusicCorpusState) = synchronized(lock) {
        if (requests.value == request) mutable.value = state
    }

    fun configure(active: Boolean, refresh: Int) = synchronized(lock) {
        val before = requests.value
        val revision = if (refresh != before.refresh) before.revision + 1 else before.revision
        if (revision != before.revision) mutable.value = LocalMusicCorpusState(revision, refresh)
        requests.value = Request(revision, refresh, active)
    }

    fun invalidate() = synchronized(lock) {
        val next = requests.value.copy(revision = requests.value.revision + 1)
        // This happens synchronously, before any debounce/background read.
        mutable.value = LocalMusicCorpusState(next.revision, next.refresh)
        requests.value = next
    }
    fun close() { job.cancel() }
}
