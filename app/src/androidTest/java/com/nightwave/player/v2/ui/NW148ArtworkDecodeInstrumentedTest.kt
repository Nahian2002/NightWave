package com.nightwave.player.v2.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.memory.MemoryCache
import coil.request.ImageRequest
import coil.request.Options
import coil.request.SuccessResult
import coil.size.Precision
import coil.size.Scale
import com.nightwave.player.v2.model.NightWaveTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import okio.Buffer
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

/** Actual Coil2.7 decoding/cache and production CoverArt, entirely synthetic and network-free. */
class NW148ArtworkDecodeInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val fetches = AtomicInteger()
    private val started = CompletableDeferred<Unit>()
    private val release = CompletableDeferred<Unit>()
    private val loaders = mutableListOf<ImageLoader>()
    private fun png(width: Int, height: Int, color: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        return ByteArrayOutputStream().use {
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)); bitmap.recycle(); it.toByteArray()
        }
    }
    private inner class FixtureFetcher : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val fallback = data.host == "i.ytimg.com" && data.path?.startsWith("/vi/00000000001/") == true
            if (data.host != "art.fixture.invalid" && !fallback) return null
            return Fetcher {
                fetches.incrementAndGet()
                if (data.path == "/slow.png") { started.complete(Unit); release.await() }
                if (fallback && data.path?.endsWith("sddefault.jpg") == true) throw IOException("Synthetic missing still")
                val bytes = if (fallback && data.path?.endsWith("maxresdefault.jpg") == true) png(120, 90, Color.GRAY)
                    else if (fallback) png(480, 360, Color.GREEN)
                    else png(1280, 720, if (data.path == "/slow.png") Color.RED else Color.BLUE)
                SourceResult(ImageSource(Buffer().write(bytes), context), "image/png", DataSource.NETWORK)
            }
        }
    }
    private fun loader(): ImageLoader = ImageLoader.Builder(context)
        .components { add(FixtureFetcher()) }.diskCache(null)
        .memoryCache { MemoryCache.Builder(context).maxSizeBytes(16 * 1024 * 1024).build() }
        .build().also { loaders.add(it) }
    @After fun close() { release.complete(Unit); loaders.forEach { it.shutdown() } }

    @Test fun smallCachedSampleDoesNotReplaceLargerRectangularDecode() = runBlocking {
        val loader = loader()
        fun request(w: Int, h: Int) = ImageRequest.Builder(context).data("https://art.fixture.invalid/wide.png")
            .size(w, h).scale(Scale.FIT).precision(Precision.INEXACT).allowHardware(false).build()
        val small = loader.execute(request(96, 96)) as SuccessResult
        val large = loader.execute(request(1056, 594)) as SuccessResult
        val smallBitmap = (small.drawable as BitmapDrawable).bitmap
        val largeBitmap = (large.drawable as BitmapDrawable).bitmap
        assertTrue(largeBitmap.width >= 1055 && largeBitmap.height >= 593)
        assertTrue(largeBitmap.width > smallBitmap.width)
        val cached = loader.execute(request(1056, 594)) as SuccessResult
        assertEquals(DataSource.MEMORY_CACHE, cached.dataSource)
        assertEquals(2, fetches.get())
    }
    @Test fun actualCoverReportsItsMeasuredRectangle() {
        val reports = Collections.synchronizedList(mutableListOf<ArtworkDecodeReport>())
        val loader = loader()
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                NightWaveMaterialTheme(NightWaveTheme.MIDNIGHT, reduceMotion = true) {
                    Box(Modifier.requiredSize(512.dp, 288.dp)) {
                        CoverArt("https://art.fixture.invalid/wide.png", Modifier.fillMaxSize(),
                            trackIdentity = "fixture:wide", large = true, imageLoader = loader, onDecode = { reports.add(it) })
                    }
                }
            }
        }
        compose.waitUntil(10_000) { reports.isNotEmpty() }
        val report = reports.last()
        assertEquals(512, report.viewportWidth); assertEquals(288, report.viewportHeight)
        assertEquals(512, report.requestWidth); assertEquals(288, report.requestHeight)
        assertTrue(report.decodedWidth >= 511 && report.decodedHeight >= 287)
    }
    @Test fun sameSourcePlaceholderAndMissingStillFallBackWithoutLooping() {
        val reports = Collections.synchronizedList(mutableListOf<ArtworkDecodeReport>())
        val loader = loader()
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                NightWaveMaterialTheme(NightWaveTheme.MIDNIGHT, reduceMotion = true) {
                    Box(Modifier.requiredSize(512.dp, 288.dp)) {
                        CoverArt("https://i.ytimg.com/vi/00000000001/default.jpg", Modifier.fillMaxSize(),
                            trackIdentity = "fixture:fallback", large = true, imageLoader = loader, onDecode = { reports.add(it) })
                    }
                }
            }
        }
        compose.waitUntil(10_000) { reports.isNotEmpty() }
        assertEquals(2, reports.last().candidate); assertEquals(3, fetches.get())
    }
    @Test fun supersededSourceAndRemovedCoverCannotPublishDelayedRedBitmap() {
        val colors = Collections.synchronizedList(mutableListOf<Int>())
        val loader = loader(); val selected = mutableStateOf("slow.png")
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                NightWaveMaterialTheme(NightWaveTheme.MIDNIGHT, reduceMotion = true) {
                    Box(Modifier.requiredSize(512.dp, 288.dp)) {
                        if (selected.value.isNotBlank()) CoverArt("https://art.fixture.invalid/" + selected.value,
                            Modifier.fillMaxSize().testTag("fixture-cover"), trackIdentity = selected.value,
                            large = true, imageLoader = loader,
                            onResolved = { colors.add((it as BitmapDrawable).bitmap.getPixel(0, 0)) })
                    }
                }
            }
        }
        compose.waitUntil(10_000) { started.isCompleted }
        compose.runOnIdle { selected.value = "fast.png" }
        compose.waitUntil(10_000) { colors.contains(Color.BLUE) }
        release.complete(Unit)
        compose.runOnIdle { selected.value = "" }
        compose.waitForIdle()
        assertFalse(colors.contains(Color.RED))
    }
}
