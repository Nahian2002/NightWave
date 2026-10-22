package com.nightwave.player.v2.cloud

/**
 * Values safe to keep across Android Activity recreation while a Google
 * permission sheet is open. Never save an access token or account identifier.
 */
object CloudAuthorizationRecoveryPolicy {
    const val TIMEOUT_MS = 20_000L

    fun safeAction(action: String?): String? = when (action) {
        "connect", "backup", "preview", "merge" -> action
        else -> null
    }

    fun shouldRestore(action: String?, awaitingResolution: Boolean): Boolean =
        awaitingResolution && safeAction(action) != null

    fun requestIsCurrent(expected: Int, active: Int, busy: Boolean,
        awaitingResolution: Boolean): Boolean =
        expected == active && busy && !awaitingResolution

    fun timeoutMs(): Long = TIMEOUT_MS
}
