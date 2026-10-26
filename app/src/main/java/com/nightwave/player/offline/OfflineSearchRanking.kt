package com.nightwave.player.offline

import java.text.Normalizer
import java.util.Locale

/**
 * Offline-only, deterministic score for already-imported titles.
 * Accent folding applies to Latin combining marks only. Bengali combining
 * vowels and other non-Latin writing systems must remain searchable.
 */
object OfflineSearchRanking {
    private val boundaries = Regex("[^\\p{L}\\p{M}\\p{N}]+")
    private val latinDiacritic = Regex("(?<=\\p{IsLatin})\\p{M}+")

    fun normalized(input: String): String {
        val lowered = Normalizer.normalize(input.take(300), Normalizer.Form.NFKD)
            .lowercase(Locale.ROOT)
        return latinDiacritic.replace(lowered, "")
            .replace(boundaries, " ").trim()
    }

    fun terms(query: String): List<String> =
        normalized(query.take(160)).split(' ').filter(String::isNotBlank).take(12)

    /** A nonmatch returns -1. Exact title and prefix outrank extension-only hits. */
    fun score(queryTerms: List<String>, originalTitle: String, extension: String): Int {
        if (queryTerms.isEmpty()) return -1
        val title = normalized(originalTitle)
        val ext = normalized(extension)
        val words = (title + " " + ext).split(' ').filter(String::isNotBlank)
        // Align offline search with NW82's token boundaries. Short "alo"
        // cannot match "alone" just because its letters are a substring.
        if (!queryTerms.all { term ->
                words.any { word ->
                    word == term || (term.length >= 4 && word.startsWith(term))
                }
            }) return -1
        val query = queryTerms.joinToString(" ")
        return when {
            title == query -> 1000
            title.startsWith(query) -> 800
            title.contains(query) -> 650
            queryTerms.all { it in title } -> 440
            queryTerms.all { it in ext } -> 200
            else -> 120
        }
    }
}
