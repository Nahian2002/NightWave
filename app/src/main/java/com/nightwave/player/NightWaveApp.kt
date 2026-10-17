package com.nightwave.player

import android.app.Application
import android.util.Log
import org.schabi.newpipe.extractor.NewPipe
import com.nightwave.player.net.ExtractorDownloader
import com.nightwave.player.diagnostics.NightWaveCrashRecorder

class NightWaveApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // The offline library must remain usable even when a network
        // extractor fails to initialize on a particular Android device.
        runCatching { NightWaveCrashRecorder.install(this) }
            .onFailure { Log.w("NightWaveStartup", "Crash recorder setup unavailable") }
        try {
            NewPipe.init(ExtractorDownloader())
        } catch (error: Exception) {
            // Playback/search paths already report extractor errors on demand.
            // Do not terminate the process before the local UI can open.
            Log.w("NightWaveStartup", "Public-video extractor initialization unavailable: " +
                error.javaClass.simpleName)
        }
    }
}
