package com.nightwave.player.diagnostics

/**
 * Only low-entropy thread categories may enter shareable crash summaries.
 * Custom Java thread names can include URLs, email addresses or file paths.
 */
object NightWaveCrashPrivacyPolicy {
    fun threadLabel(name: String): String = when (name) {
        "main" -> "main"
        "RenderThread" -> "render"
        "FinalizerDaemon", "ReferenceQueueDaemon" -> "runtime"
        else -> "background"
    }

    fun safeFrameLine(line: Int): Int = if (line in 1..1_000_000) line else 0

    fun safeSourceSymbol(symbol: String, limit: Int): String =
        symbol.take(limit.coerceIn(1, 130)).map { character ->
            if (character.isLetterOrDigit() || character in "._$") character else '_'
        }.joinToString("")
}
