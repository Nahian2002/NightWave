package com.nightwave.player

import android.app.Application
import org.schabi.newpipe.extractor.NewPipe
import com.nightwave.player.net.ExtractorDownloader

class NightWaveApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NewPipe.init(ExtractorDownloader())
    }
}
