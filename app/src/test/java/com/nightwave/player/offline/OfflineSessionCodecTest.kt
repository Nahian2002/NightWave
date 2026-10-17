package com.nightwave.player.offline

import org.junit.Assert.*
import org.junit.Test

class OfflineSessionCodecTest {
    private fun id(n: Int) = "00000000-0000-0000-0000-${n.toString().padStart(12, '0')}.mp3"
    private fun session() = OfflineSession(listOf(id(1), id(2)), id(2), 9_123L)

    @Test fun roundTripPreservesSelectedTrackAndPosition() {
        assertEquals(session(), OfflineSessionCodec.decode(OfflineSessionCodec.encode(session())))
    }

    @Test fun rejectsTruncatedAndCorruptedDataWithoutThrowing() {
        val raw = OfflineSessionCodec.encode(session())
        assertNull(OfflineSessionCodec.decode(raw.dropLast(2)))
        assertNull(OfflineSessionCodec.decode(raw.replace(id(2), id(3))))
        assertNull(OfflineSessionCodec.decode("not a session"))
    }

    @Test fun rejectsPathsUrisDuplicateIdsAndMissingSelection() {
        assertFalse(OfflineSessionCodec.isValid(OfflineSession(listOf("../../foo.mp3"), "../../foo.mp3", 0)))
        assertFalse(OfflineSessionCodec.isValid(OfflineSession(listOf("content://private"), "content://private", 0)))
        assertFalse(OfflineSessionCodec.isValid(OfflineSession(listOf(id(1), id(1)), id(1), 0)))
        assertFalse(OfflineSessionCodec.isValid(OfflineSession(listOf(id(1)), id(2), 0)))
    }

    @Test fun boundsPositionAndQueueSize() {
        assertFalse(OfflineSessionCodec.isValid(OfflineSession(listOf(id(1)), id(1), -1)))
        assertFalse(OfflineSessionCodec.isValid(OfflineSession(listOf(id(1)), id(1),
            OfflineSessionCodec.MAX_POSITION_MS + 1)))
        val all = (1..401).map(::id)
        assertFalse(OfflineSessionCodec.isValid(OfflineSession(all, id(1), 0)))
    }

    @Test fun rejectsUnknownVersionAndInvalidCount() {
        val raw = OfflineSessionCodec.encode(session())
        assertNull(OfflineSessionCodec.decode(raw.replace("NWOS1", "NWOS2")))
        assertNull(OfflineSessionCodec.decode(raw.replace("\n2\n", "\n401\n")))
    }

    @Test fun longOfflineLibraryRoundTripsUnderSizeCap() {
        val ids = (1..400).map(::id)
        val original = OfflineSession(ids, id(400), 12345678L)
        assertEquals(original, OfflineSessionCodec.decode(OfflineSessionCodec.encode(original)))
    }
}
