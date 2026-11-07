package com.nightwave.player.net

import com.nightwave.player.core.VideoLink

/** Short prefixes are only evidence in the on-device typing preview. */
internal class SearchTypingQuery private constructor(
    private val key: String, private val prefix: String
) {
    private val wordPrefix = " $prefix"

    fun score(title: String): Int = when {
        title == key -> 400
        title.startsWith(prefix) -> 300
        key.length >= 2 && wordPrefix in title -> 200
        else -> 0
    }

    /** Finish the current word, or extend the draft by exactly one word. */
    fun completions(candidates: List<String>): List<String> {
        val seen = mutableSetOf<String>()
        val typedWords = key.split(' ')
        return candidates.asSequence().mapNotNull { candidate ->
            val label = candidate.trim().take(160)
            if (VideoLink.resemblesLink(label) || label.any(Char::isISOControl)) return@mapNotNull null
            val normalized = SearchRelevancePolicy.normalize(label)
            if (normalized == key || !normalized.startsWith(prefix)) return@mapNotNull null
            val candidateWords = normalized.split(' ')
            val completeWord = candidateWords.getOrNull(typedWords.lastIndex) == typedWords.last()
            val end = words.findAll(label).elementAtOrNull(
                if (completeWord) typedWords.size else typedWords.lastIndex
            )?.range?.last ?: return@mapNotNull null
            label.substring(0, end + 1).takeIf {
                val completionKey = SearchRelevancePolicy.normalize(it)
                completionKey != key && seen.add(completionKey)
            }
        }.take(4).toList()
    }

    /** Keep the best few rows without allocating or sorting the full result set. */
    fun <T, K> top(items: List<T>, checkpoint: () -> Unit, identity: (T) -> K,
        points: (Int) -> Int): List<T> {
        val hits = ArrayList<Triple<T, K, Int>>(LIMIT)
        items.forEachIndexed { index, item ->
            if (index % 128 == 0) checkpoint()
            val score = points(index)
            if (score <= 0 || (hits.size == LIMIT && hits.last().third >= score)) return@forEachIndexed
            val key = identity(item)
            val duplicate = hits.indexOfFirst { it.second == key }
            if (duplicate >= 0) {
                if (hits[duplicate].third >= score) return@forEachIndexed
                hits.removeAt(duplicate)
            }
            val insertion = hits.indexOfFirst { it.third < score }
            hits.add(if (insertion < 0) hits.size else insertion, Triple(item, key, score))
            if (hits.size > LIMIT) hits.removeAt(hits.lastIndex)
        }
        checkpoint()
        return hits.map { it.first }
    }

    companion object {
        private const val LIMIT = 3
        private val words = Regex("""[\p{L}\p{M}\p{N}]+""")

        fun parse(raw: String): SearchTypingQuery? {
            if (VideoLink.resemblesLink(raw)) return null
            val key = SearchRelevancePolicy.normalize(raw.take(160))
            if (key.isEmpty()) return null
            return SearchTypingQuery(key, if (raw.lastOrNull()?.isWhitespace() == true) "$key " else key)
        }
    }
}
