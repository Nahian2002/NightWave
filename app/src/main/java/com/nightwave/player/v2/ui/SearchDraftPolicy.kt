package com.nightwave.player.v2.ui

/** A transient explicit input request is consumed once, including across pager recreation. */
internal object SearchDraftPolicy {
    fun shouldApply(requestToken: String, consumedToken: String): Boolean =
        requestToken.isNotBlank() && requestToken != consumedToken
}
