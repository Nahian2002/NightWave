package com.nightwave.player.v2.cloud

/**
 * NW73: "a backup was once verified" is not proof the snapshot covers the
 * CURRENT optional privacy choices. Never call a revised consent verified
 * until a successful checked Drive readback records that revision.
 *
 * This policy neither uploads data nor writes preferences or credentials.
 */
object CloudBackupFreshnessPolicy {
    enum class Freshness { LOCAL_ONLY, CONNECTED_PENDING, PREVIOUSLY_VERIFIED, CURRENT_VERIFIED }

    fun classify(
        connected: Boolean, automaticEnabled: Boolean, lastSuccessMs: Long,
        currentConsentRevision: Long, lastVerifiedRevision: Long
    ): Freshness = when {
        !connected -> Freshness.LOCAL_ONLY
        !automaticEnabled || lastSuccessMs <= 0L -> Freshness.CONNECTED_PENDING
        currentConsentRevision != lastVerifiedRevision -> Freshness.PREVIOUSLY_VERIFIED
        else -> Freshness.CURRENT_VERIFIED
    }

    /** No-op preference taps must not invalidate an actually verified upload. */
    fun changed(previous: Boolean, requested: Boolean): Boolean = previous != requested
}
