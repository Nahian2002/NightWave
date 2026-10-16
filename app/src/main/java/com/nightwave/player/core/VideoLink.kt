package com.nightwave.player.core

import java.net.URI
import java.net.URLDecoder

/** Strict parsing prevents deceptive domains from being treated as YouTube. */
object VideoLink {
    private val idPattern = Regex("^[A-Za-z0-9_-]{11}$")
    private val sharedLink = Regex("https?://[^\\s<>\"']+", RegexOption.IGNORE_CASE)

    fun normalize(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        val candidate = sharedLink.find(trimmed)?.value
            ?: if (trimmed.startsWith("youtu.be/") || trimmed.startsWith("youtube.com/") ||
                trimmed.startsWith("www.youtube.com/") || trimmed.startsWith("m.youtube.com/") ||
                trimmed.startsWith("music.youtube.com/")) "https://$trimmed" else trimmed
        val uri = try { URI(candidate) } catch (_: Exception) { return null }
        if (uri.scheme?.lowercase() !in setOf("http", "https")) return null
        val host = uri.host?.lowercase() ?: return null
        val path = uri.path ?: return null
        val id = when (host) {
            "youtu.be", "www.youtu.be" -> path.trim('/').substringBefore('/')
            "youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com" -> {
                val first = path.trim('/').split('/')
                when (first.firstOrNull()) {
                    "watch" -> uri.rawQuery.orEmpty().split('&').firstNotNullOfOrNull { part ->
                        val fields = part.split('=', limit = 2)
                        if (fields.firstOrNull() == "v" && fields.size == 2) {
                            try { URLDecoder.decode(fields[1], "UTF-8") } catch (_: Exception) { null }
                        } else null
                    }
                    "shorts", "live", "embed" -> first.getOrNull(1)
                    else -> null
                }
            }
            else -> null
        }
        return if (id != null && idPattern.matches(id)) "https://www.youtube.com/watch?v=$id" else null
    }

    /**
     * NewPipe search results can provide relative /watch?v=... or watch?v=...
     * URLs rather than full YouTube URLs. Keep the same strict video-ID parser.
     */
    fun normalizeSearchResult(input: String): String? {
        val value = input.trim().replace("&amp;", "&")
        normalize(value)?.let { return it }
        val adjusted = when {
            value.startsWith("/watch?") || value.startsWith("/shorts/") ||
                value.startsWith("/live/") -> "https://www.youtube.com" + value
            value.startsWith("watch?") || value.startsWith("shorts/") ||
                value.startsWith("live/") -> "https://www.youtube.com/" + value
            idPattern.matches(value) -> "https://www.youtube.com/watch?v=" + value
            else -> return null
        }
        return normalize(adjusted)
    }

    /** A plain search term isn't a URL; a likely but invalid link is reported to the user. */
    fun resemblesLink(input: String): Boolean =
        input.contains("youtube.com", ignoreCase = true) ||
            input.contains("youtu.be", ignoreCase = true) ||
            input.trim().startsWith("http://", ignoreCase = true) ||
            input.trim().startsWith("https://", ignoreCase = true)
}
