package com.nightwave.player.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import com.nightwave.player.core.VideoLink
import com.nightwave.player.core.WebSearchRoute

internal object OfficialYouTube {
    fun search(activity: Activity, query: String): Boolean =
        open(activity, WebSearchRoute.searchUrl(query))

    fun track(activity: Activity, url: String): Boolean =
        VideoLink.normalize(url)?.let { open(activity, it) } ?: false

    private fun open(activity: Activity, url: String): Boolean = try {
        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
