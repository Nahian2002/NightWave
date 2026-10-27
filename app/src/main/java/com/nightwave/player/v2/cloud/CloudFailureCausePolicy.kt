package com.nightwave.player.v2.cloud

import java.util.Collections
import java.util.IdentityHashMap

/** Tasks.await wraps API failures. Inspect bounded causes without publishing their messages. */
object CloudFailureCausePolicy {
    fun <T : Throwable> find(error: Throwable, kind: Class<T>): T? {
        val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
        var current: Throwable? = error
        repeat(8) {
            val next = current ?: return null
            if (!seen.add(next)) return null
            if (kind.isInstance(next)) return kind.cast(next)
            current = next.cause
        }
        return null
    }
}
