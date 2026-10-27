package com.nightwave.player.v2.support

import java.io.ByteArrayInputStream
import java.io.InputStream
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class BuildIdentityTest {
    private val hash = "A1".repeat(32)
    private fun fixture() = JSONObject().put("schema", 1).put("algorithm", "NightWave-source-v1")
        .put("variant", "debug").put("sourceSha256", hash).put("inputCount", 810)
        .put("revisionContext", "a".repeat(40))

    @Test fun displaysVariantAndSnapshotInsteadOfReusedVersionLabel() {
        val first = BuildIdentity.decode(fixture().toString())!!
        val changed = BuildIdentity.decode(fixture().put("sourceSha256", "B2".repeat(32)).toString())!!
        assertEquals("NW-debug-${hash.take(16)}", first.displayId)
        assertNotEquals(first.displayId, changed.displayId)
        assertEquals(hash, first.sourceSha256)
    }

    @Test fun archiveBuildRetainsExactSourceWithoutInventingCommit() {
        val identity = BuildIdentity.decode(fixture().put("revisionContext", "unavailable").toString())!!
        assertEquals("unavailable", identity.revisionContext)
        assertEquals(hash, identity.sourceSha256)
    }

    @Test fun rejectsEveryMissingRequiredField() {
        for (field in listOf("schema", "algorithm", "variant", "sourceSha256", "inputCount", "revisionContext")) {
            val obj = fixture().apply { remove(field) }
            assertNull(field, BuildIdentity.decode(obj.toString()))
        }
    }

    @Test fun refusesUnknownSchemaAlgorithmAndVariants() {
        for ((field, value) in listOf("schema" to 2, "schema" to "1", "schema" to 1.5,
            "algorithm" to "other", "variant" to "production-debug-override")) {
            assertNull(BuildIdentity.decode(fixture().put(field, value).toString()))
        }
        assertNotNull(BuildIdentity.decode(fixture().put("variant", "release").toString()))
    }

    @Test fun rejectsMalformedOrPrivateLookingHashAndRevisionValues() {
        for (value in listOf("", "A".repeat(63), "G".repeat(64), "a1".repeat(32), "C:/private/project")) {
            assertNull(BuildIdentity.decode(fixture().put("sourceSha256", value).toString()))
        }
        for (value in listOf("HEAD", "a".repeat(39), "https://account.example/token", "a".repeat(41))) {
            assertNull(BuildIdentity.decode(fixture().put("revisionContext", value).toString()))
        }
    }

    @Test fun inputCountsMustBeBoundedIntegerValues() {
        for (value in listOf(-1, 0, 49, 100001, 810.5, "810", Long.MAX_VALUE)) {
            assertNull(BuildIdentity.decode(fixture().put("inputCount", value).toString()))
        }
    }

    @Test fun supportCopyIncludesFullDigestAndDoesNotClaimDeviceAcceptance() {
        val text = BuildIdentity.decode(fixture().toString())!!.supportText("2.0.0-alpha5s", 35)
        assertTrue(text.contains("NightWave 2.0.0-alpha5s (35)"))
        assertTrue(text.contains(hash))
        assertTrue(text.contains("Revision context: " + "a".repeat(40)))
        assertTrue(text.contains("does not certify device or account acceptance"))
    }

    @Test fun oversizedAndInvalidAssetCannotExposeAnIdentity() {
        assertNull(BuildIdentity.decode("not JSON"))
        assertNull(BuildIdentity.decode(" ".repeat(BuildIdentity.MAX_BYTES + 1)))
        assertNull(BuildIdentity.read(ByteArrayInputStream(ByteArray(BuildIdentity.MAX_BYTES + 1) { 32 })))
        assertNotNull(BuildIdentity.read(ByteArrayInputStream(fixture().toString().toByteArray())))
    }

    @Test fun stalledAndThrowingAssetReadsFailClosed() {
        var calls = 0
        val stalled = object : InputStream() {
            override fun read(): Int = 0
            override fun read(b: ByteArray, off: Int, len: Int): Int { calls++; return 0 }
        }
        assertNull(BuildIdentity.read(stalled))
        assertEquals(8, calls)
        assertNull(BuildIdentity.read(object : InputStream() {
            override fun read(): Int = throw java.io.IOException("synthetic")
        }))
    }

    @Test fun installedArtifactUsesTheStandardCompleteSha256Vector() {
        assertEquals("BA7816BF8F01CFEA414140DE5DAE2223B00361A396177A9CB410FF61F20015AD",
            InstalledArtifactHash.read(ByteArrayInputStream("abc".toByteArray())))
        assertNotEquals(InstalledArtifactHash.read(ByteArrayInputStream("abc".toByteArray())),
            InstalledArtifactHash.read(ByteArrayInputStream("abd".toByteArray())))
    }

    @Test fun installedArtifactHashIsBoundedAndCannotSpinOnEmptyReads() {
        assertNull(InstalledArtifactHash.read(ByteArrayInputStream(ByteArray(10)), 9))
        assertNull(InstalledArtifactHash.read(ByteArrayInputStream(ByteArray(1)), 0))
        var calls = 0
        assertNull(InstalledArtifactHash.read(object : InputStream() {
            override fun read(): Int = 0
            override fun read(b: ByteArray): Int { calls++; return 0 }
        }))
        assertEquals(8, calls)
        assertNull(InstalledArtifactHash.read(object : InputStream() {
            override fun read(): Int = throw java.io.IOException("synthetic")
        }))
    }

    @Test fun supportReceiptBindsArtifactAndSignersAndLabelsSplitBaseHashes() {
        val identity = BuildIdentity.decode(fixture().toString())!!
        val text = identity.supportText("test", 35, "B".repeat(64),
            listOf("C".repeat(64), "invalid", "C".repeat(64)), splitCount = 2)
        assertTrue(text.contains("Base APK SHA256: " + "B".repeat(64)))
        assertTrue(text.contains("Split APK count: 2 (base hash only)"))
        assertTrue(text.contains("Installed signer SHA256: " + "C".repeat(64)))
        assertFalse(text.contains("invalid"))
        assertTrue(identity.supportText("test", 35, "bad").contains("APK SHA256: unavailable"))
    }
}
