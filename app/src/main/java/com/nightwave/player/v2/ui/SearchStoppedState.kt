package com.nightwave.player.v2.ui

/** Keep successful source-bound results and diagnostics when only their refresh is stopped. */
internal fun StudioSearchSnapshot.stop(reason: SearchStopReason): StudioSearchSnapshot =
    if (!loading) this else copy(loading = false, error = "", stopped = reason)
