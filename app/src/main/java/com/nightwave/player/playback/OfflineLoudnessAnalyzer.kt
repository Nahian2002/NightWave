package com.nightwave.player.playback

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.SystemClock
import android.util.Log
import java.io.File
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Decodes at most the first 12s of an already-verified private offline file.
 * Execute ONLY on a bounded background worker, never the Media3/service looper.
 * This is RMS-based attenuation, not ReplayGain / ITU-R BS.1770 / true-peak.
 */
class OfflineLoudnessAnalyzer {
    fun measure(file: File): Float? {
        if (!file.isFile || !file.canRead() || file.length() <= 0L) return null
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        var started = false
        return try {
            extractor.setDataSource(file.absolutePath)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)
                    ?.startsWith("audio/") == true
            } ?: return null
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            extractor.selectTrack(trackIndex)
            codec = MediaCodec.createDecoderByType(mime)
            val decoder = codec ?: return null
            decoder.configure(format, null, null, 0)
            decoder.start()
            started = true
            val info = MediaCodec.BufferInfo()
            val startMs = SystemClock.elapsedRealtime()
            var inputEnded = false
            var completed = false
            var count = 0L
            var sumSquares = 0.0
            var peak = 0f
            var outputFormat = format
            while (!completed && count < OfflineNormalizationPolicy.MAX_SAMPLES &&
                SystemClock.elapsedRealtime() - startMs <
                    OfflineNormalizationPolicy.MAX_ANALYSIS_WALL_MS) {
                if (!inputEnded) {
                    val slot = decoder.dequeueInputBuffer(10_000L)
                    if (slot >= 0) {
                        val input = decoder.getInputBuffer(slot)
                        val size = if (input == null) -1 else {
                            input.clear()
                            extractor.readSampleData(input, 0)
                        }
                        val stamp = extractor.sampleTime
                        if (size < 0 || stamp > OfflineNormalizationPolicy.MAX_SAMPLED_MS * 1000L) {
                            decoder.queueInputBuffer(slot, 0, 0, 0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputEnded = true
                        } else {
                            decoder.queueInputBuffer(slot, 0, size,
                                stamp.coerceAtLeast(0L), 0)
                            extractor.advance()
                        }
                    }
                }
                when (val slot = decoder.dequeueOutputBuffer(info, 10_000L)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> outputFormat = decoder.outputFormat
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    else -> if (slot >= 0) {
                        val buffer = decoder.getOutputBuffer(slot)
                        val encoding = if (outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING))
                            outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                            else AudioFormat.ENCODING_PCM_16BIT
                        if (info.size > 0 && buffer != null &&
                            info.offset >= 0 && info.offset + info.size <= buffer.capacity()) {
                            val pcm = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
                            pcm.position(info.offset)
                            pcm.limit(info.offset + info.size)
                            if (encoding == AudioFormat.ENCODING_PCM_16BIT) {
                                while (pcm.remaining() >= 2 &&
                                    count < OfflineNormalizationPolicy.MAX_SAMPLES) {
                                    val x = pcm.short.toInt() / 32768f
                                    sumSquares += x.toDouble() * x.toDouble()
                                    peak = maxOf(peak, abs(x))
                                    count++
                                }
                            } else if (encoding == AudioFormat.ENCODING_PCM_FLOAT) {
                                while (pcm.remaining() >= 4 &&
                                    count < OfflineNormalizationPolicy.MAX_SAMPLES) {
                                    val x = pcm.float
                                    if (x.isFinite()) {
                                        sumSquares += x.toDouble() * x.toDouble()
                                        peak = maxOf(peak, abs(x))
                                        count++
                                    }
                                }
                            } else {
                                completed = true // unknown PCM encoding: fail closed
                                count = 0L
                            }
                        }
                        decoder.releaseOutputBuffer(slot, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0)
                            completed = true
                    }
                }
            }
            if (count < 4_096L || peak <= 0f) null
            else OfflineNormalizationPolicy.attenuation(
                sqrt(sumSquares / count).toFloat(), peak)
        } catch (error: Exception) {
            Log.w("NightWaveLoudness", "Local analysis unavailable: " +
                error.javaClass.simpleName)
            null
        } finally {
            if (started) runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }
}
