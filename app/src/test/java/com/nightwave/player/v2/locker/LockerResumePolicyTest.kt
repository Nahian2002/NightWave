package com.nightwave.player.v2.locker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.EOFException
import java.io.IOException

/** HTTP Range/offset/retry policy only: no Google tokens, Android UI, or personal media. */
class LockerResumePolicyTest {
    private fun span(
        offset: Long = 0, total: Long = 1000, code: Int = 200,
        range: String? = null, declared: Long = total
    ) = LockerResumePolicy.verifiedSpan(offset, total, code, range, declared)

    @Test fun freshFullResponseIsAccepted() {
        assertEquals(1000L, span())
    }

    @Test fun unknownContentLengthAcceptedOnlyWithVerifiedBodyCount() {
        assertEquals(1000L, span(declared = -1))
    }

    @Test fun freshResponseWithWrongLengthRejected() {
        assertThrows(IllegalArgumentException::class.java) { span(declared = 999) }
    }

    @Test fun validRangedResponseAcceptedAtCheckpoint() {
        assertEquals(700L, span(offset = 300, code = 206,
            range = "bytes 300-999/1000", declared = 700))
    }

    @Test fun validShortRangedResponseAllowedForAnotherRound() {
        assertEquals(200L, span(offset = 300, code = 206,
            range = "bytes 300-499/1000", declared = 200))
    }

    @Test fun resumedDownloadCannotAcceptUnexpectedFull200() {
        assertThrows(IllegalArgumentException::class.java) {
            span(offset = 300, code = 200, declared = 1000)
        }
    }

    @Test fun missingContentRangeRejected() {
        assertThrows(IllegalStateException::class.java) {
            span(offset = 300, code = 206, range = null, declared = 700)
        }
    }

    @Test fun wrongRangeStartRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            span(offset = 300, code = 206, range = "bytes 0-999/1000", declared = 1000)
        }
    }

    @Test fun wrongRangeTotalRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            span(offset = 300, code = 206, range = "bytes 300-999/1200", declared = 700)
        }
    }

    @Test fun rangeEndOutsideTotalRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            span(offset = 300, code = 206, range = "bytes 300-1000/1000", declared = 701)
        }
    }

    @Test fun rangeHeaderWithInvalidGrammarRejected() {
        assertThrows(IllegalStateException::class.java) {
            span(offset = 300, code = 206, range = "bytes */1000", declared = 700)
        }
    }

    @Test fun declaredRangedBodySizeMustEqualSpan() {
        assertThrows(IllegalArgumentException::class.java) {
            span(offset = 300, code = 206, range = "bytes 300-999/1000", declared = 600)
        }
    }

    @Test fun completeOffsetIsNotAValidRangeRequest() {
        assertThrows(IllegalArgumentException::class.java) {
            span(offset = 1000, code = 206, range = "bytes 1000-1000/1000", declared = 1)
        }
    }

    @Test fun fileCannotExceedOfflineFileLimit() {
        assertThrows(IllegalArgumentException::class.java) {
            span(total = LockerAudioPolicy.MAX_FILE_BYTES + 1)
        }
    }

    @Test fun unknownPartialLengthAcceptedWithRangeHeader() {
        assertEquals(700L, span(offset = 300, code = 206,
            range = "bytes 300-999/1000", declared = -1))
    }

    @Test fun zeroCheckpointProgressIsZeroPercent() {
        assertEquals(0, LockerResumePolicy.percent(0, 100))
    }

    @Test fun intermediateCheckpointProgressIsAccurate() {
        assertEquals(53, LockerResumePolicy.percent(535, 1000))
    }

    @Test fun completeCheckpointIsNotLabeledDoneBeforeHashing() {
        assertEquals(99, LockerResumePolicy.percent(1000, 1000))
    }

    @Test fun negativeProgressRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            LockerResumePolicy.percent(-1, 100)
        }
    }

    @Test fun retryOnlyTransientIoAndOnlyBeforeThirdFailure() {
        assertTrue(LockerResumePolicy.shouldRetry(1, EOFException("interrupted")))
        assertTrue(LockerResumePolicy.shouldRetry(2, IOException("temporary")))
        assertFalse(LockerResumePolicy.shouldRetry(3, IOException("third failure")))
        assertFalse(LockerResumePolicy.shouldRetry(1, IllegalArgumentException("bad range")))
    }
}
