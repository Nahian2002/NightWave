package com.nightwave.player.net

/**
 * NW82: token-boundary evidence for music search. Inputs must already be
 * SearchScriptNormalization-normalized. Never edits source titles or IDs.
 *
 * Very short prefixes like "alo" must not match "alone". Typo matching is
 * strictly optional, ASCII Latin only, and limited to one edit on words >=5.
 * Bangla vowel marks are never removed or fuzzily substituted.
 */
internal object SearchTokenMatchingPolicy {
    private val asciiLatin = Regex("^[a-z]+$")

    fun tokens(normalized: String): List<String> =
        normalized.split(' ').filter { it.isNotBlank() }

    /** 3 = exact word, 2 = safe prefix, 1 = single Latin typo, 0 = unrelated. */
    fun evidence(term: String, words: List<String>, allowTypo: Boolean = false): Int {
        if (term.isBlank() || words.isEmpty()) return 0
        if (words.any { it == term }) return 3
        // Short partials generated severe "Alo" vs "Alone" false matches.
        if (term.length >= 4 && words.any { it.startsWith(term) }) return 2
        if (allowTypo && term.length >= 5 && asciiLatin.matches(term) &&
            words.any { it.length >= 5 && asciiLatin.matches(it) &&
                oneEditApart(term, it) }) return 1
        return 0
    }

    fun allCovered(terms: List<String>, words: List<String>, allowTypo: Boolean = false): Boolean =
        terms.isNotEmpty() && terms.all { evidence(it, words, allowTypo) > 0 }

    /** Strictly one insertion, deletion, replacement or adjacent transposition. */
    private fun oneEditApart(a: String, b: String): Boolean {
        if (a == b || kotlin.math.abs(a.length - b.length) > 1) return false
        if (a.length == b.length) {
            val differences = a.indices.filter { a[it] != b[it] }
            if (differences.size == 1) return true
            return differences.size == 2 && differences[1] == differences[0] + 1 &&
                a[differences[0]] == b[differences[1]] &&
                a[differences[1]] == b[differences[0]]
        }
        val longer = if (a.length > b.length) a else b
        val shorter = if (a.length > b.length) b else a
        var i = 0
        var j = 0
        var skipped = false
        while (i < longer.length && j < shorter.length) {
            if (longer[i] == shorter[j]) {
                i++
                j++
            } else if (!skipped) {
                skipped = true
                i++
            } else return false
        }
        return true
    }
}
