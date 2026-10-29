package com.nightwave.player.offline

import okhttp3.Headers
import java.security.MessageDigest
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** RFC 9110 single ranges; no multipart, weak validators or encoded offsets. */
internal object AudioRangePolicy {
    const val CHUNK_BYTES = 1024L * 1024L
    const val MAX_BYTES = 250L * 1024L * 1024L
    data class Range(val start: Long, val end: Long, val total: Long)

    fun parse(value: String?): Range? {
        val match = Regex("bytes ([0-9]{1,12})-([0-9]{1,12})/([0-9]{1,12})")
            .matchEntire(value.orEmpty()) ?: return null
        val (start, end, total) = match.destructured
        val r = Range(start.toLong(), end.toLong(), total.toLong())
        return r.takeIf { it.total in 1..MAX_BYTES && it.start <= it.end &&
            it.start >= 0 && it.end < it.total }
    }

    fun validator(headers: Headers): String? {
        val etag = headers["ETag"]
        if (etag != null) return etag.takeIf {
            it.length in 2..256 && it.startsWith('"') && it.endsWith('"') &&
                it.substring(1, it.lastIndex).all { c -> c.code in 33..126 && c != '"' }
        }
        // A client may use a date only in the absence of any ETag. Require
        // a conservative 60-second separation, not a future/coarse timestamp.
        val modified = headers["Last-Modified"] ?: return null
        val date = headers["Date"] ?: return null
        val age = runCatching {
            val m = ZonedDateTime.parse(modified, DateTimeFormatter.RFC_1123_DATE_TIME)
            val d = ZonedDateTime.parse(date, DateTimeFormatter.RFC_1123_DATE_TIME)
            java.time.Duration.between(m, d).seconds
        }.getOrNull() ?: return null
        return modified.takeIf { it.length <= 128 && age >= 60 }
    }

    fun key(identity: String): String = MessageDigest.getInstance("SHA-256")
        .digest(identity.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 255) }

    fun validStoredValidator(value: String): Boolean = if (value.startsWith('"')) {
        validator(Headers.headersOf("ETag", value)) == value
    } else value.length in 1..128 && runCatching {
        ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
    }.isSuccess
}
