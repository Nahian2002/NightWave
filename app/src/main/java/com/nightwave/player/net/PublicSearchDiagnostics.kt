package com.nightwave.player.net

import java.util.concurrent.TimeUnit

/** Numeric timings only. Never retains queries, URLs, response bodies, IPs or accounts. */
internal class PublicSearchDiagnostics(private val clockNanos: () -> Long = System::nanoTime) {
    private val started = clockNanos()
    private var phase: PublicSearchPhase? = null
    private var phaseAt = started
    private var direct = 0L
    private var community = 0L
    private var httpNanos = 0L
    private var calls = 0
    private var cached = false
    private fun finishPhase(now: Long) {
        val elapsed = (now - phaseAt).coerceAtLeast(0)
        when (phase) { PublicSearchPhase.DIRECT -> direct += elapsed
            PublicSearchPhase.COMMUNITY -> community += elapsed
            null -> Unit }
        phaseAt = now
    }
    @Synchronized fun begin(value: PublicSearchPhase) { val now = clockNanos(); finishPhase(now); phase = value }
    @Synchronized fun cacheHit() { cached = true }
    @Synchronized fun httpCompleted(elapsed: Long) { calls++; httpNanos += elapsed.coerceAtLeast(0) }
    @Synchronized fun snapshot(): String {
        val now = clockNanos(); finishPhase(now)
        fun ms(n: Long) = TimeUnit.NANOSECONDS.toMillis(n.coerceAtLeast(0))
        return "Elapsed ${ms(now - started)} ms · ${if (cached) "Recent results" else "Live lookup"}\n" +
            "Direct ${ms(direct)} ms · Community ${ms(community)} ms\n" +
            "HTTP calls $calls · Cumulative HTTP work ${ms(httpNanos)} ms\n" +
            "Provider phases include extraction and parsing. HTTP work includes connection, response read and processing. " +
            "Local music matching and browser search are separate."
    }
}
