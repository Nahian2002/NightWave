package com.nightwave.player.offline

import org.junit.Assert.*
import org.junit.Test

class OfflineImportJourneyPolicyTest {
    @Test fun brokenIndexCannotBecomeEmptyOnboardingOrMassDeletion() {
        assertFalse(OfflineImportJourneyPolicy.empty(false,0))
        assertFalse(OfflineImportJourneyPolicy.empty(true,1))
        assertTrue(OfflineImportJourneyPolicy.empty(true,0))
    }
    @Test fun progressSeparatesCopyingAudioCheckAndSavingWithinTheBatchLimit() {
        assertEquals("Checking audio 2 of 5",OfflineImportJourneyPolicy.progress(2,5,OfflineImportPhase.CHECKING_AUDIO))
        assertEquals("Copying 100 of 100",OfflineImportJourneyPolicy.progress(101,200,OfflineImportPhase.COPYING))
        assertEquals("Saving 1 of 1",OfflineImportJourneyPolicy.progress(0,0,OfflineImportPhase.SAVING))
    }
    @Test fun partialAndOversizedSelectionsDoNotClaimEverythingWasImported() {
        val label=OfflineImportJourneyPolicy.result(1,2,4)
        assertTrue(label.startsWith("1 file checked and saved")); assertTrue(label.contains("2 failed"))
        assertTrue(label.contains("4 not imported")); assertFalse(label.contains("all"))
        assertTrue(OfflineImportJourneyPolicy.result(0,1,0).startsWith("0 files"))
    }
    @Test fun sourceGuidancePreservesOwnershipOriginalsAndAllSupportedFormats() {
        assertTrue(OfflineImportJourneyPolicy.SOURCE_HELP.contains("original stays"))
        for (type in listOf("MP3","M4A","AAC","FLAC","WAV","OGG","OPUS","MP4","WebM","3GP"))
            assertTrue(type,OfflineImportJourneyPolicy.FORMAT_HELP.contains(type))
    }
    @Test fun extensionOrVideoTrackAloneCannotProveReadableAudio() {
        assertEquals(-1,OfflineAudioValidationPolicy.firstAudioTrack(listOf("video/mp4","text/html")))
        assertFalse(OfflineAudioValidationPolicy.hasReadableSample(-1,4096))
        assertFalse(OfflineAudioValidationPolicy.hasReadableSample(0,-1))
        assertFalse(OfflineAudioValidationPolicy.hasReadableSample(0,0))
    }
    @Test fun ownedContainersWithActualAudioRemainSupportedWithoutInventingAlbumIdentity() {
        val index=OfflineAudioValidationPolicy.firstAudioTrack(listOf("video/avc","audio/mp4a-latm"))
        assertEquals(1,index); assertTrue(OfflineAudioValidationPolicy.hasReadableSample(index,512))
        assertEquals(0,OfflineAudioValidationPolicy.firstAudioTrack(listOf("audio/raw")))
    }
}
