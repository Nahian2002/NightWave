package com.nightwave.player.v2.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.FrameMetrics
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nightwave.player.library.MusicLibrary
import com.nightwave.player.net.LocalMusicQueryRunner
import com.nightwave.player.net.LocalMusicSearchIndex
import com.nightwave.player.net.LocalMusicSnapshot
import com.nightwave.player.offline.OfflineAudioLibrary
import com.nightwave.player.v2.model.NightWaveTheme
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/** Explicit diagnostic only. No startup, audible-next, release-speed or battery certification. */
@RunWith(AndroidJUnit4::class)
class NW140PerformanceDiagnostic {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val base = instrumentation.targetContext
    private val suffix = "nw140-fixture-" + UUID.randomUUID()
    private val names = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val isolated = object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
            names += name
            return base.getSharedPreferences("$suffix-$name", mode)
        }
    }
    private val actions = AtomicInteger(0)
    private val queries = listOf("latin_tail" to 9998, "bangla_tail" to 9999)
    private fun title(i: Int) = if (i % 2 == 0) "Fixture probe${i.toString().padStart(5, '0')}"
        else "পরীক্ষা নমুনা${i.toString().padStart(5, '0')}"
    private fun query(i: Int) = title(i).substringAfter(' ')
    private fun url(i: Int) = "https://www.youtube.com/watch?v=${i.toString().padStart(11, '0')}"
    private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02X".format(it.toInt() and 255) }
    private fun fileSha(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(1024 * 1024)
            while (true) { val count = stream.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
        }
        return digest.digest().joinToString("") { "%02X".format(it.toInt() and 255) }
    }

    @Before fun requireExplicitDiagnosticOptIn() {
        assumeTrue("Requires -e nw140Measure true; not a normal correctness timing test",
            InstrumentationRegistry.getArguments().getString("nw140Measure") == "true")
    }
    @After fun cleanupOnlyThisFixture() {
        names.forEach { base.deleteSharedPreferences("$suffix-$it") }
    }
    private fun seed() {
        val prefs = isolated.getSharedPreferences("nightwave_library_v1", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val playlists = JSONArray()
        repeat(34) { list ->
            val name = "Fixture ${list + 1}"; playlists.put(name)
            val rows = JSONArray()
            for (i in list * 300 until minOf((list + 1) * 300, 10_000)) rows.put(JSONObject()
                .put("title", title(i)).put("creator", "Fixture creator").put("url", url(i))
                .put("artwork", "").put("duration", 180))
            editor.putString("list_$name", rows.toString())
        }
        assertTrue(editor.putString("playlist_names", playlists.toString())
            .putString("favorites", "[]").putString("recent", "[]").commit())
    }
    private fun writeReceipt(metric: String, clock: String, samples: JSONArray,
        frames: JSONArray = JSONArray(), dropped: Long = 0L, truncated: Boolean = false) {
        assertEquals(0, actions.get())
        val source = JSONObject(base.assets.open("nightwave-build.json").bufferedReader().use { it.readText() })
        val display = base.resources.displayMetrics
        val dataset = buildString { repeat(10_000) { append("$it|${title(it)}|${url(it)}\n") } }
        val receipt = JSONObject().put("schema", 1).put("kind", "NW140_DIAGNOSTIC_ONLY")
            .put("metric", metric).put("clock_policy", clock).put("runtime", "ANDROID_ART")
            .put("source_sha256", source.getString("sourceSha256").uppercase())
            .put("input_count", source.getInt("inputCount")).put("variant", source.getString("variant"))
            .put("apk_sha256", fileSha(File(base.applicationInfo.sourceDir)))
            .put("test_apk_sha256", fileSha(File(instrumentation.context.applicationInfo.sourceDir)))
            .put("fixture_id", "NW140_SYNTHETIC_10K_V1").put("fixture_count", 10_000)
            .put("fixture_sha256", sha(dataset.toByteArray(Charsets.UTF_8))).put("external_actions", actions.get())
            .put("warmups_per_case", 4).put("samples_per_case", 20).put("samples", samples)
            .put("frames_scope", if (metric.startsWith("art_")) "NONE" else "WHOLE_FIXTURE_INCLUDING_SETUP_WARMUPS")
            .put("frames", frames).put("frame_callbacks_dropped", dropped).put("frames_truncated", truncated)
            .put("environment", JSONObject().put("model", Build.MODEL).put("sdk", Build.VERSION.SDK_INT)
                .put("abis", Build.SUPPORTED_ABIS.joinToString(",")).put("vm", System.getProperty("java.vm.name", ""))
                .put("width_px", display.widthPixels).put("height_px", display.heightPixels)
                .put("density_dpi", display.densityDpi).put("font_scale", base.resources.configuration.fontScale.toDouble())
                .put("theme", "MIDNIGHT").put("reduce_motion", true).put("compilation", "UNCONTROLLED_DEBUG"))
        val folder = File(base.cacheDir, "nw140-diagnostics").also { check(it.isDirectory || it.mkdirs()) }
        val output = File(folder, UUID.randomUUID().toString() + ".json")
        output.writeText(receipt.toString(2), Charsets.UTF_8)
        instrumentation.sendStatus(0, Bundle().apply { putString("nw140_receipt", "nw140-diagnostics/${output.name}") })
    }

    @Test fun artRunnerMeasuresRealWallClockIncludingProductionDebounce() {
        seed()
        val index = LocalMusicSearchIndex.build(LocalMusicSnapshot(
            MusicLibrary(isolated).searchSnapshot(), OfflineAudioLibrary(isolated).songs()))
        assertEquals(10_000, index.publicCount)
        val samples = JSONArray()
        for ((case, target) in queries) repeat(24) { iteration ->
            val started = System.nanoTime()
            // runBlocking owns a real event loop outside Compose's test clock.
            val result = runBlocking { LocalMusicQueryRunner.run(index, query(target)) }
            val elapsed = System.nanoTime() - started
            assertEquals(listOf(url(target)), result.saved.map { it.url })
            assertTrue(result.accepts(query(target), index))
            if (iteration >= 4) samples.put(JSONObject().put("case_id", case).put("index", iteration - 4)
                .put("elapsed_ns", elapsed).put("correct", true))
        }
        writeReceipt("art_query_runner_wall_including_debounce", "REAL_MONOTONIC_RUN_BLOCKING", samples)
    }

    @Test fun composeFixtureRecordsCorrectVisibleRowAndBoundedFrameDiagnostics() {
        seed()
        val frames = JSONArray(); var dropped = 0L; var truncated = false
        val listener = Window.OnFrameMetricsAvailableListener { _, supplied, lost ->
            // Copy Android's reused metrics before retaining numeric values.
            val frame = FrameMetrics(supplied)
            synchronized(frames) {
                dropped += lost.coerceAtLeast(0).toLong()
                if (frames.length() >= 10_000) truncated = true else frames.put(JSONObject()
                    .put("total_ns", frame.getMetric(FrameMetrics.TOTAL_DURATION))
                    .put("first_draw", frame.getMetric(FrameMetrics.FIRST_DRAW_FRAME))
                    .put("deadline_ns", if (Build.VERSION.SDK_INT >= 31) frame.getMetric(FrameMetrics.DEADLINE) else -1L))
            }
        }
        compose.runOnUiThread { compose.activity.window.addOnFrameMetricsAvailableListener(listener, Handler(Looper.getMainLooper())) }
        var listenerAttached = true
        try {
            compose.setContent {
                val (corpus, retry) = rememberNightWaveLocalCorpus(isolated, active = true, refresh = 1)
                CompositionLocalProvider(LocalContext provides isolated) {
                NightWaveMaterialTheme(NightWaveTheme.MIDNIGHT, reduceMotion = true) {
                    StudioSearch(Modifier, StudioSearchSnapshot(),
                        onPrimarySearch = { _, _ -> actions.incrementAndGet() },
                        onNativeSearch = { _, _ -> actions.incrementAndGet() }, onWebSearch = { actions.incrementAndGet() },
                        onPlay = { _, _ -> actions.incrementAndGet() }, onSongOptions = { actions.incrementAndGet() },
                        playingUrl = "", corpus = corpus, onRetryCorpus = retry, onOfflineSearch = { actions.incrementAndGet() },
                        searchHistory = emptyList(), canUndoSearchRemoval = false, onRecordSearch = {},
                        onRemoveSearch = {}, onUndoSearchRemoval = {}, onClearSearch = {})
                }
                }
            }
            fun ready() = compose.waitUntil(30_000) {
                compose.onAllNodesWithTag("nw129-local-loading").fetchSemanticsNodes().isEmpty()
            }
            ready()
            val samples = JSONArray()
            for ((case, target) in queries) repeat(24) { iteration ->
                compose.onNodeWithTag("nw129-search-query").performScrollTo().performTextReplacement("")
                ready()
                val started = System.nanoTime()
                compose.onNodeWithTag("nw129-search-query").performTextReplacement(query(target))
                ready()
                val row = hasTestTag("nightwave-track-search") and hasText(title(target))
                compose.onNodeWithTag("nw129-search-list").performScrollToNode(row)
                compose.onNode(row).assertIsDisplayed()
                val elapsed = System.nanoTime() - started
                if (iteration >= 4) samples.put(JSONObject().put("case_id", case).put("index", iteration - 4)
                    .put("elapsed_ns", elapsed).put("correct", true))
            }
            compose.waitForIdle()
            compose.runOnUiThread {
                compose.activity.window.removeOnFrameMetricsAvailableListener(listener)
                listenerAttached = false
            }
            synchronized(frames) {
                writeReceipt("compose_fixture_dispatch_to_verified_visible_row", "COMPOSE_AUTO_ADVANCE_DIAGNOSTIC", samples,
                    frames, dropped, truncated)
            }
        } finally {
            if (listenerAttached) compose.runOnUiThread {
                compose.activity.window.removeOnFrameMetricsAvailableListener(listener)
            }
        }
    }
}
