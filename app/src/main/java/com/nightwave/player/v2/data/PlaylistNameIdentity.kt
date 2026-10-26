package com.nightwave.player.v2.data

import java.util.Locale

/**
 * Canonical identity for names stored in NightWave archive and Drive backups.
 * Device language must not change what counts as the same playlist. Existing
 * ambiguous legacy archives must fail validation rather than lose a playlist.
 */
object PlaylistNameIdentity {
    fun key(name: String): String = name.lowercase(Locale.ROOT)

    fun same(first: String, second: String): Boolean = key(first) == key(second)

    fun allDistinct(names: List<String>): Boolean {
        val seen = HashSet<String>(names.size)
        return names.all { seen.add(key(it)) }
    }
}
