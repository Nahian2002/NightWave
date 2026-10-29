package com.nightwave.player.offline

import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.nio.ByteBuffer

/** Read-only Android container/sample probe. No full-decode, checksum or codec-support promise. */
internal object OfflineAudioVerifier {
    fun verify(file: File) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.absolutePath)
            val mimes = (0 until extractor.trackCount).map {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME).orEmpty()
            }
            // User-owned MP4/WebM/3GP with an audio track remains supported.
            // Direct-download audio-only restrictions stay with their existing validator.
            val audio = OfflineAudioValidationPolicy.firstAudioTrack(mimes)
            require(audio >= 0) { "No readable audio track. Choose a supported audio file." }
            extractor.selectTrack(audio)
            require(OfflineAudioValidationPolicy.hasReadableSample(audio,
                extractor.readSampleData(ByteBuffer.allocate(1_048_576), 0))) {
                "No readable audio samples. Re-import a trusted copy."
            }
        } catch (error: Exception) {
            throw IllegalArgumentException("Audio could not be checked. Use a trusted supported file; nothing was changed.", error)
        } finally {
            extractor.release()
        }
    }
}
