package com.nightwave.player.net

/**
 * NW112: a tiny, explicitly curated cross-script/romanization aid for search
 * ranking. It is NOT a general transliterator or a verified song-identity
 * directory. Only whole normalized words in the curated groups can change.
 *
 * Keep normalized Bangla letters and vowel marks intact. Never rewrite URLs,
 * original track titles, stored library values or the video provider query.
 */
internal object SearchMultilingualAliasPolicy {
    private const val MAX_QUERY_WORDS = 8
    private const val MAX_FORMS = 16

    // A group represents alternate written forms of one common search term,
    // not the identity or ownership of a particular audio recording.
    private val groups = listOf(
        listOf("jhum", "jhoom", "ঝুম"),
        listOf("bhalobasha", "valobasha", "ভালোবাসা"),
        listOf("bhalobashi", "valobashi", "ভালোবাসি")
    )

    private val alternativesByWord: Map<String, List<String>> = buildMap {
        groups.forEach { spellings ->
            spellings.forEach { word ->
                put(word, spellings.filterNot { it == word })
            }
        }
    }

    /** Returns at most 15 deterministic, whole-word substitutions.
     * Call with SearchRelevancePolicy.prepare(raw).text, never raw user text.
     * An exact unmodified match must always outrank an alias-scored match.
     */
    fun alternatives(normalizedQuery: String): List<String> {
        val words = SearchTokenMatchingPolicy.tokens(normalizedQuery)
        if (words.isEmpty() || words.size > MAX_QUERY_WORDS) return emptyList()
        var forms: List<List<String>> = listOf(words)
        for (index in words.indices) {
            val aliases = alternativesByWord[words[index]] ?: continue
            val next = linkedSetOf<List<String>>()
            for (form in forms) {
                for (candidate in listOf(words[index]) + aliases) {
                    val changed = form.toMutableList()
                    changed[index] = candidate
                    next.add(changed)
                    if (next.size >= MAX_FORMS) break
                }
                if (next.size >= MAX_FORMS) break
            }
            forms = next.toList()
        }
        return forms.asSequence()
            .map { it.joinToString(" ") }
            .filter { it != normalizedQuery }
            .distinct()
            .take(MAX_FORMS - 1)
            .toList()
    }
}
