package com.nightwave.player.net

import java.text.Normalizer
import java.util.Locale

/**
 * NW70: script-aware, device-local search key. Fold Latin diacritics but
 * preserve marks used as letters/vowels in Bengali and other scripts.
 *
 * This is search indexing only; it never edits a song title, filename or URL.
 */
internal object SearchScriptNormalization {
    private val latinDiacritic = Regex("(?<=\\p{IsLatin})\\p{M}+")
    private val boundaries = Regex("[^\\p{L}\\p{M}\\p{N}]+")
    private val spaces = Regex("\\s+")

    fun normalize(value: String): String {
        val plain = Normalizer.normalize(
            value.take(512).lowercase(Locale.ROOT), Normalizer.Form.NFKD)
        return latinDiacritic.replace(plain, "")
            .replace(boundaries, " ").replace(spaces, " ").trim()
    }
}
