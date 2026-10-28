package com.nightwave.player.net

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class LocalMusicSearchControllerTest {
    private fun small(title: String = "Moonlight") = NW129SearchFixture.snapshot(1).let {
        it.copy(publicLibrary = it.publicLibrary.copy(favorites = listOf(NW129SearchFixture.track(1, title)),
            recent = emptyList(), playlists = emptyList()), offline = emptyList())
    }

    @Test fun inactiveSessionDoesNoReadAndActiveSessionPublishesCompleteSnapshot() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val calls = AtomicInteger()
        val controller = LocalMusicSearchController(scope) { calls.incrementAndGet(); small() }
        try {
            controller.configure(false, 1)
            delay(120)
            assertEquals(0, calls.get())
            controller.configure(true, 1)
            val ready = withTimeout(5_000) { controller.state.first { it.index != null } }
            assertEquals(1, ready.refresh)
            assertEquals(1, calls.get())
            assertEquals("Moonlight", ready.index!!.search("Moonlight").saved.single().title)
        } finally { controller.close(); scope.cancel() }
    }

    @Test fun invalidationClearsVisibleIndexSynchronouslyBeforeDebounce() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val controller = LocalMusicSearchController(scope) { small() }
        try {
            controller.configure(true, 1)
            withTimeout(5_000) { controller.state.first { it.index != null } }
            controller.invalidate()
            assertNull(controller.state.value.index)
            assertFalse(controller.state.value.failed)
        } finally { controller.close(); scope.cancel() }
    }

    @Test fun supersededBlockingReadIsJoinedAndNeverPublishedOverANewerRevision() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        val calls = AtomicInteger(); val active = AtomicInteger(); val maximum = AtomicInteger()
        val controller = LocalMusicSearchController(scope) {
            val now = active.incrementAndGet(); maximum.updateAndGet { maxOf(it, now) }
            val number = calls.incrementAndGet()
            try {
                if (number == 1) withContext(NonCancellable) { entered.complete(Unit); release.await() }
                small(if (number == 1) "Old title" else "New title")
            } finally { active.decrementAndGet() }
        }
        try {
            controller.configure(true, 1)
            withTimeout(5_000) { entered.await() }
            repeat(30) { controller.invalidate() }
            assertNull(controller.state.value.index)
            release.complete(Unit)
            val ready = withTimeout(5_000) { controller.state.first { it.index != null } }
            assertEquals("New title", ready.index!!.search("New title").saved.single().title)
            assertTrue(ready.index!!.search("Old title").saved.isEmpty())
            assertEquals(1, maximum.get())
            assertEquals(2, calls.get())
        } finally { release.complete(Unit); controller.close(); scope.cancel() }
    }

    @Test fun sourceFailureIsUnavailableRatherThanAFalseEmptySuccessfulLibrary() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val controller = LocalMusicSearchController(scope) { throw IllegalArgumentException("fixture") }
        try {
            controller.configure(true, 7)
            val failed = withTimeout(5_000) { controller.state.first { it.failed } }
            assertNull(failed.index); assertEquals(7, failed.refresh)
        } finally { controller.close(); scope.cancel() }
    }

    @Test fun manualRefreshCannotAcceptThePreviousRevision() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val controller = LocalMusicSearchController(scope) { small() }
        try {
            controller.configure(true, 1)
            val before = withTimeout(5_000) { controller.state.first { it.index != null } }
            controller.configure(true, 2)
            assertNull(controller.state.value.index)
            val after = withTimeout(5_000) { controller.state.first { it.index != null && it.refresh == 2 } }
            assertTrue(after.revision > before.revision)
            assertNotSame(before.index, after.index)
        } finally { controller.close(); scope.cancel() }
    }

    @Test fun cancelledTypingRunnerNeverReturnsTheCancelledQuery() = runBlocking {
        val index = LocalMusicSearchIndex.build(small())
        val old = async { LocalMusicQueryRunner.run(index, "Old query") }
        yield(); old.cancelAndJoin()
        assertTrue(old.isCancelled)
        assertEquals("Moonlight", LocalMusicQueryRunner.run(index, "Moonlight").saved.single().title)
    }
}
