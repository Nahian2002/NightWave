package com.nightwave.player.playback

import android.content.Context
import android.net.Uri
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Disposable local source routing. Actual remote throughput/playback is a separate phone run. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlaybackMediaTransportInstrumentedTest {
    @Test fun localSourcesStillReadAndSeekWithoutAnyNetworkCall() {
        val context:Context=ApplicationProvider.getApplicationContext()
        val file=File.createTempFile("nw-network-local-",".fixture",context.cacheDir)
        val data=ByteArray(4096) { it.toByte() }; file.writeBytes(data)
        val client=PlaybackMediaNetwork.client.newBuilder().addInterceptor { error("Local file routed to network") }.build()
        val source=DefaultDataSource.Factory(context,OkHttpDataSource.Factory(client)).createDataSource()
        try {
            assertEquals(512L,source.open(DataSpec.Builder().setUri(Uri.fromFile(file)).setPosition(128).setLength(512).build()))
            val actual=ByteArray(512); var count=0
            while (count<actual.size) { val n=source.read(actual,count,actual.size-count); assertTrue(n>0); count+=n }
            assertArrayEquals(data.copyOfRange(128,640),actual)
            assertEquals(-1,source.read(ByteArray(1),0,1))
        } finally { source.close(); file.delete() }
    }
}
