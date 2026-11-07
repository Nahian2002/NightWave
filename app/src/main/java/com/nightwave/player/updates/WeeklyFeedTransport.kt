package com.nightwave.player.updates

import java.net.HttpURLConnection
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Fetch bytes only. Only the separate signature check can authorize Disabled/Available. */
internal object WeeklyFeedTransport {
    suspend fun read(connection: HttpURLConnection, authenticatedGithub: Boolean = false): ByteArray = try {
        val status = connection.responseCode
        if (authenticatedGithub && PrivateGithubUpdatePolicy.needsAccess(status))
            throw UpdateCheckException(UpdateCheckStatus.PRIVATE_ACCESS)
        if (status != 200) throw UpdateCheckException(UpdateCheckStatus.UNAVAILABLE)
        connection.inputStream.use { input ->
            val output = ByteArrayOutputStream(); val buffer = ByteArray(4096)
            while (true) {
                currentCoroutineContext().ensureActive()
                val n = input.read(buffer); if (n < 0) break
                if (output.size() + n > 24 * 1024) throw UpdateCheckException(UpdateCheckStatus.INVALID_METADATA)
                output.write(buffer, 0, n)
            }
            output.toByteArray()
        }
    } finally { connection.disconnect() }
}
