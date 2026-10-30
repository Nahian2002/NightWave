package com.nightwave.player.updates

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

internal data class ApprovedUpdate(val code: Long, val name: String, val url: String,
    val bytes: Long, val sha: String, val notes: String)

internal object WeeklyUpdates {
    private const val FEED = "https://raw.githubusercontent.com/Nahian2002/NightWave/main/updates/stable.json"
    private fun prefs(c: Context) = c.getSharedPreferences("nightwave_weekly_updates_v1", Context.MODE_PRIVATE)
    private fun code(c: Context): Long = version(c.packageManager.getPackageInfo(c.packageName, 0))
    private fun version(p: android.content.pm.PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= 28) p.longVersionCode else @Suppress("DEPRECATION") p.versionCode.toLong()

    fun onOpen(activity: ComponentActivity) {
        activity.lifecycleScope.launch {
            val now = System.currentTimeMillis()
            val p = prefs(activity)
            if (!WeeklyUpdatePolicy.due(now, p.getLong("checked", 0)) ||
                !WeeklyUpdatePolicy.due(now, p.getLong("attempt", 0), WeeklyUpdatePolicy.RETRY_MS)) return@launch
            p.edit().putLong("attempt", now).apply()
            try {
                val update = withContext(Dispatchers.IO) { latest(activity) }
                // Failed/invalid feeds never become a successful weekly check.
                p.edit().putLong("checked", now).apply()
                if (update != null && activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    activity.startActivity(Intent(activity, WeeklyUpdateActivity::class.java))
                } else if (update != null) p.edit().remove("checked").apply()
            } catch (_: CancellationException) { throw CancellationException() }
            catch (_: Exception) { /* No startup error popup or provider/user information logged. */ }
        }
    }

    suspend fun latest(c: Context): ApprovedUpdate? = withContext(Dispatchers.IO) {
        val connection = URL(FEED).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000; connection.readTimeout = 20_000
        connection.instanceFollowRedirects = false
        val raw = try {
            require(connection.responseCode == 200)
            connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream(); val buffer = ByteArray(4096)
                while (true) { currentCoroutineContext().ensureActive(); val n = input.read(buffer); if (n < 0) break
                    require(output.size() + n <= 24 * 1024); output.write(buffer, 0, n) }
                output.toByteArray()
            }
        } finally { connection.disconnect() }
        val envelope = JSONObject(String(raw, Charsets.UTF_8))
        val payload = Base64.decode(envelope.getString("payload"), Base64.DEFAULT)
        val signature = Base64.decode(envelope.getString("signature"), Base64.DEFAULT)
        require(payload.size <= 16 * 1024 && signature.size <= 1024)
        val key = JSONObject(c.assets.open("weekly-update-key.json").bufferedReader().use { it.readText() })
        require(WeeklyUpdatePolicy.verify(payload, signature,
            Base64.decode(key.getString("modulus"), Base64.DEFAULT), Base64.decode(key.getString("exponent"), Base64.DEFAULT)))
        val m = JSONObject(String(payload, Charsets.UTF_8))
        require(m.getInt("schema") == 1 && m.getString("channel") == "stable")
        if (!m.getBoolean("enabled")) return@withContext null
        val update = ApprovedUpdate(m.getLong("versionCode"), m.getString("versionName").take(80),
            m.getString("url"), m.getLong("bytes"), m.getString("sha256"), m.optString("notes").take(3000))
        require(WeeklyUpdatePolicy.allowedApk(update.url) && update.bytes in 1..WeeklyUpdatePolicy.MAX_APK_BYTES)
        require(update.sha.matches(Regex("[A-Fa-f0-9]{64}")))
        update.takeIf { it.code > code(c) }
    }

    @Suppress("DEPRECATION")
    private fun certificates(p: android.content.pm.PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= 28) p.signingInfo?.apkContentsSigners else p.signatures
        return signatures.orEmpty().map { digest(it.toByteArray()) }.toSet()
    }
    private fun digest(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 255) }

    suspend fun download(c: Context, u: ApprovedUpdate, progress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        val folder = File(c.cacheDir, "approved-updates").apply { mkdirs() }
        val part = File(folder, "download.part"); val apk = File(folder, "approved.apk")
        part.delete(); apk.delete()
        try {
            var url = u.url
            var connection: HttpURLConnection? = null
            for (hop in 0..4) {
                val next = URL(url)
                require(next.protocol == "https" && next.userInfo == null &&
                    next.host in setOf("github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com"))
                val active = next.openConnection() as HttpURLConnection
                active.connectTimeout = 15_000; active.readTimeout = 20_000; active.instanceFollowRedirects = false
                if (active.responseCode in listOf(301, 302, 303, 307, 308)) {
                    val location = active.getHeaderField("Location"); active.disconnect()
                    require(hop < 4 && location != null); url = URL(next, location).toString()
                } else { if (active.responseCode != 200) { active.disconnect(); error("Update unavailable") }; connection = active; break }
            }
            val active = requireNotNull(connection)
            var received = 0L; val hash = MessageDigest.getInstance("SHA-256")
            try {
                active.inputStream.use { input -> part.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024); var lastPercent = -1
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val n = input.read(buffer); if (n < 0) break
                        received += n; require(received <= u.bytes)
                        output.write(buffer, 0, n); hash.update(buffer, 0, n)
                        val percent = (received * 100 / u.bytes).toInt()
                        if (percent != lastPercent) { progress(percent); lastPercent = percent }
                    }
                } }
            } finally { active.disconnect() }
            require(received == u.bytes && hash.digest().joinToString("") { "%02x".format(it.toInt() and 255) }.equals(u.sha, true))
            val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES
            val candidate = requireNotNull(c.packageManager.getPackageArchiveInfo(part.absolutePath, flags))
            val installed = c.packageManager.getPackageInfo(c.packageName, flags)
            require(candidate.packageName == c.packageName && version(candidate) == u.code && u.code > version(installed))
            require(certificates(candidate).isNotEmpty() && certificates(candidate) == certificates(installed))
            require(requireNotNull(candidate.applicationInfo).minSdkVersion <= Build.VERSION.SDK_INT)
            require(part.renameTo(apk)); apk
        } catch (e: Exception) { part.delete(); apk.delete(); throw e }
    }
}
