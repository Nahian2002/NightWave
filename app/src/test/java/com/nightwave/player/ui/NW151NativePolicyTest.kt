package com.nightwave.player.ui

import org.junit.Assert.*
import org.junit.Test

class NW151NativePolicyTest {
    @Test fun unknownLengthStreamsAreActuallyBoundedAndExactLimitIsAccepted() {
        val bytes=ByteArray(NativeThumbnailPolicy.MAX_BYTES) { (it % 251).toByte() }
        assertArrayEquals(bytes,NativeThumbnailPolicy.read(bytes.inputStream()))
        try { NativeThumbnailPolicy.read(ByteArray(bytes.size+1).inputStream());fail() }
        catch (_:IllegalArgumentException) { }
    }
    @Test fun titleChoosesLargestFitAndNeverDropsBelowReadableMinimum() {
        assertEquals(20, ClassicTextSizePolicy.choose(16,23) { it <= 20 })
        assertEquals(16, ClassicTextSizePolicy.choose(16,23) { false })
        assertEquals(23, ClassicTextSizePolicy.choose(16,23) { true })
    }
    @Test fun fontScaleIsAppliedByLayoutNotByLoweringSpBounds() {
        for (scale in listOf(.8f,1f,1.3f,2f)) {
            val size=ClassicTextSizePolicy.choose(16,23) { it * scale <= 30f }
            assertTrue(size in 16..23)
        }
    }
    @Test fun invalidTitleBoundsFailClosed() {
        for ((min,max) in listOf(0 to 23,23 to 16,16 to 100)) {
            try { ClassicTextSizePolicy.choose(min,max) { true }; fail() } catch (_:IllegalArgumentException) { }
        }
    }
    @Test fun smallOriginalArtIsNotNeedlesslyHalved() {
        assertEquals(1,NativeThumbnailPolicy.sample(168,94,128))
        assertEquals(1,NativeThumbnailPolicy.sample(64,64,512))
    }
    @Test fun requestedViewportDeterminesSamplingAndCacheBucket() {
        assertEquals(8,NativeThumbnailPolicy.sample(1600,900,128))
        assertEquals(2,NativeThumbnailPolicy.sample(1600,900,1024))
        assertEquals(128,NativeThumbnailPolicy.target(100,80))
        assertEquals(512,NativeThumbnailPolicy.target(0,0))
        assertEquals(1024,NativeThumbnailPolicy.target(3000,2000))
    }
    @Test fun decodedPixelBudgetBoundsSquareAndExtremeImages() {
        for ((w,h) in listOf(4096 to 4096,16000 to 16000,4097 to 4097,32768 to 1)) {
            val s=NativeThumbnailPolicy.sample(w,h,1024)
            assertTrue(((w.toLong()+s-1)/s)*((h.toLong()+s-1)/s)<=1_048_576L)
        }
    }
    @Test fun invalidImageBoundsAreRejectedBeforeDecode() {
        for ((w,h) in listOf(-1 to -1,0 to 100,100 to 0)) {
            try { NativeThumbnailPolicy.sample(w,h,128); fail() } catch (_:IllegalArgumentException) { }
        }
    }
}
