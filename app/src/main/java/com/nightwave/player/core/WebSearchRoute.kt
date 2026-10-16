package com.nightwave.player.core

import java.net.URI
import java.net.URLEncoder
import java.net.URLDecoder

/**
 * Pure-Kotlin navigation rules for NightWave's official-website search screen.
 * No API keys or fragile third-party search indexes are required.
 */
object WebSearchRoute {
    fun searchUrl(query: String): String {
        val safeQuery = query.trim().take(160)
        require(safeQuery.isNotEmpty()) { "Enter a song or artist first" }
        return "https://www.youtube.com/results?search_query=" +
            URLEncoder.encode(safeQuery, "UTF-8")
    }

    fun selectedVideo(raw: String): String? {
        val target = try { URI(raw.trim()) } catch (_: Exception) { return null }
        if (target.scheme.equals("nightwave", ignoreCase = true) && target.host == "play") {
            val value = target.rawQuery?.split("&")?.firstNotNullOfOrNull { field ->
                val split = field.split("=", limit = 2)
                if (split.size == 2 && split[0] == "url") {
                    runCatching { URLDecoder.decode(split[1], "UTF-8") }.getOrNull()
                } else null
            }
            return value?.let(VideoLink::normalizeSearchResult)
        }
        return VideoLink.normalizeSearchResult(raw)
    }

    fun isAllowedWebsiteNavigation(raw: String): Boolean {
        val uri = try { URI(raw) } catch (_: Exception) { return false }
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        return uri.host?.lowercase() in setOf(
            "youtube.com", "www.youtube.com", "m.youtube.com",
            "music.youtube.com", "youtu.be", "www.youtu.be",
            "consent.youtube.com"
        )
    }
}
