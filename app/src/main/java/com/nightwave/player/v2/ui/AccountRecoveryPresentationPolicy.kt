package com.nightwave.player.v2.ui

/** Presentation only: actions still require the existing authorization and restore gates. */
object AccountRecoveryPresentationPolicy {
    data class Layout(val paddingDp: Int, val avatarDp: Int, val compact: Boolean)

    fun layout(widthDp: Float, fontScale: Float): Layout {
        val width = widthDp.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
        val scale = fontScale.takeIf { it.isFinite() }?.coerceIn(.7f, 3f) ?: 1f
        val compact = width < 360f || scale >= 1.5f
        return Layout(if (compact) 14 else 20, if (compact) 44 else 56, compact)
    }

    fun primaryAction(connected: Boolean, needsReview: Boolean, hasBackup: Boolean): String = when {
        !connected -> "Connect Google Drive"
        needsReview -> "Reconnect Google Drive"
        !hasBackup -> "Create first backup"
        else -> "Back up library now"
    }

    fun protectionTitle(connected: Boolean, needsReview: Boolean, hasBackup: Boolean,
        needsRefresh: Boolean): String = when {
        needsReview && connected -> "Drive needs attention"
        !connected -> "Keep your library close"
        !hasBackup -> "Create your recovery copy"
        needsRefresh -> "Refresh your backup"
        else -> "Last backup verified"
    }
}
