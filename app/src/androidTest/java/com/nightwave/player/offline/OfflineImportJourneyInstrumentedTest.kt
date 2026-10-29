package com.nightwave.player.offline

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Actual MediaExtractor/SAF file import with disposable local WAVs. No network, real data or audio output. */
class OfflineImportJourneyInstrumentedTest {
    private val context=OfflineImportTestContext(ApplicationProvider.getApplicationContext())
    private val library=OfflineAudioLibrary(context)
    @After fun close() { context.close() }
    private fun source(bytes: ByteArray=OfflineImportTestFixtures.wav()) =
        File.createTempFile("nw135-owned-",".wav",context.cacheDir).apply { writeBytes(bytes) }

    @Test fun validOwnedAudioIsCheckedBeforeSavingAndSurvivesReaderRecreation() {
        val original=source(); val phases=mutableListOf<OfflineImportPhase>()
        val saved=library.import(Uri.fromFile(original)) { phases.add(it) }
        assertEquals(listOf(OfflineImportPhase.COPYING,OfflineImportPhase.CHECKING_AUDIO,OfflineImportPhase.SAVING),phases)
        assertArrayEquals(original.readBytes(),library.fileFor(saved)!!.readBytes())
        val restored=OfflineAudioLibrary(context)
        assertEquals(saved,restored.find(saved.id)); restored.verifyAudio(saved)
        assertTrue(original.exists())
    }
    @Test fun htmlDisguisedAsAudioNeverReachesTheIndexAndLeavesOriginalIntact() {
        val original=source("<html>not music</html>".toByteArray()); val before=library.songs()
        assertThrows(IllegalArgumentException::class.java) { library.import(Uri.fromFile(original)) }
        assertEquals(before,library.songs()); assertTrue(original.exists())
        assertTrue(File(context.filesDir,"offline_audio").listFiles().orEmpty().isEmpty())
    }
    @Test fun sameLengthDamageFailsTheReadOnlyAudioCheckWithoutDeletingItsEntry() {
        val saved=library.import(Uri.fromFile(source())); val stored=library.fileFor(saved)!!
        stored.writeBytes(ByteArray(saved.bytes.toInt())) // Only this isolated fixture is damaged.
        val before=library.songs(); val damaged=stored.readBytes()
        assertThrows(IllegalArgumentException::class.java) { library.verifyAudio(saved) }
        assertEquals(before,library.songs()); assertArrayEquals(damaged,stored.readBytes())
        assertEquals(OfflineFileHealth.AVAILABLE,library.healthReport().entries.single().health)
        // Metadata availability is intentionally distinct from an audio probe.
    }
    @Test fun missingSourceAndEmptyAudioCannotBecomeSavedCopies() {
        val empty=source(ByteArray(0)); val absent=source().apply { assertTrue(delete()) }
        assertThrows(IllegalArgumentException::class.java) { library.import(Uri.fromFile(empty)) }
        assertThrows(Exception::class.java) { library.import(Uri.fromFile(absent)) }
        assertTrue(library.songs().isEmpty()); assertTrue(empty.exists())
    }
    @Test fun repeatedValidImportsReuseTheExactExistingIdentity() {
        val original=source(); val saved=library.import(Uri.fromFile(original))
        assertEquals(saved,library.import(Uri.fromFile(original)))
        assertEquals(1,library.songs().size); assertTrue(original.exists())
    }
}
