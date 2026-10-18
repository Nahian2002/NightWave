package com.nightwave.player.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/** JVM-only lock behavior. No Android account, SharedPreferences, or real audio. */
class OfflineLibraryIndexLockTest {
    @Test fun sameSharedMonitorIsRetained() {
        assertTrue(OfflineLibraryIndexLock.monitor === OfflineLibraryIndexLock.monitor)
    }

    @Test fun aSecondScreenCannotEnterAnotherScreensCriticalSection() {
        val attempted = CountDownLatch(1)
        val entered = CountDownLatch(1)
        lateinit var competing: Thread
        synchronized(OfflineLibraryIndexLock.monitor) {
            competing = thread(start = true, isDaemon = true) {
                attempted.countDown()
                synchronized(OfflineLibraryIndexLock.monitor) {
                    entered.countDown()
                }
            }
            assertTrue(attempted.await(2, TimeUnit.SECONDS))
            assertFalse("Index lock allowed overlapping mutations",
                entered.await(60, TimeUnit.MILLISECONDS))
        }
        assertTrue("Queued index mutation never proceeded",
            entered.await(2, TimeUnit.SECONDS))
        competing.join(2000)
    }

    @Test fun criticalSectionsNeverRunSimultaneouslyAcrossWorkers() {
        val active = AtomicInteger(0)
        val highest = AtomicInteger(0)
        val start = CountDownLatch(1)
        val finished = CountDownLatch(8)
        val threads = (0 until 8).map {
            thread(start = true, isDaemon = true) {
                try {
                    start.await()
                    repeat(12) {
                        synchronized(OfflineLibraryIndexLock.monitor) {
                            val now = active.incrementAndGet()
                            highest.updateAndGet { peak -> maxOf(now, peak) }
                            Thread.yield()
                            active.decrementAndGet()
                        }
                    }
                } finally {
                    finished.countDown()
                }
            }
        }
        start.countDown()
        assertTrue("Workers did not finish", finished.await(10, TimeUnit.SECONDS))
        threads.forEach { it.join(1000) }
        assertEquals(1, highest.get())
        assertEquals(0, active.get())
    }
}
