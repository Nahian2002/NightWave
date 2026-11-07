package com.nightwave.player.net

import java.util.concurrent.TimeUnit

/** Short-lived health evidence. It never records a search term or stores anything on disk. */
internal class PublicSearchRouting(
    private val clockMillis: () -> Long = { TimeUnit.NANOSECONDS.toMillis(System.nanoTime()) }
) {
    @Volatile private var recoveredAt: Long? = null

    fun order(): List<PublicSearchPhase> {
        val recovered = recoveredAt
        val communityFirst = recovered != null && clockMillis() - recovered in 0L until RECOVERY_MS
        return if (communityFirst) COMMUNITY_FIRST else DIRECT_FIRST
    }

    fun recordSuccess(phase: PublicSearchPhase, directFailed: Boolean) {
        when {
            phase == PublicSearchPhase.DIRECT -> recoveredAt = null
            directFailed -> recoveredAt = clockMillis()
        }
    }

    private companion object {
        const val RECOVERY_MS = 60_000L
        val DIRECT_FIRST = listOf(PublicSearchPhase.DIRECT, PublicSearchPhase.COMMUNITY)
        val COMMUNITY_FIRST = listOf(PublicSearchPhase.COMMUNITY, PublicSearchPhase.DIRECT)
    }
}
