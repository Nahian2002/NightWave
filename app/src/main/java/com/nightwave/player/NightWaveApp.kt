package com.nightwave.player

import android.app.Application
import org.schabi.newpipe.extractor.NewPipe
import com.nightwave.player.net.ExtractorDownloader
import com.nightwave.player.diagnostics.NightWaveCrashRecorder

class NightWaveApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NightWaveCrashRecorder.install(this)
        NewPipe.init(ExtractorDownloader())
    }
}
