package com.nightwave.player.v2.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nightwave.player.v2.support.BuildIdentity
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Compiling these tests is not execution proof. No accounts or library data are accessed. */
@RunWith(AndroidJUnit4::class)
class BuildIdentityInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    private fun packaged(): BuildIdentity {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        return context.assets.open("nightwave-build.json").use { BuildIdentity.read(it) }!!
    }

    @Test fun installedValidationApkContainsReadableBuildIdentity() {
        val identity = packaged()
        assertEquals(64, identity.sourceSha256.length)
        assertTrue(identity.inputCount >= 50)
        assertTrue(identity.displayId.startsWith("NW-debug-"))
    }

    @Test fun detailsShowSnapshotAndCopyTheFullReceipt() {
        val identity = packaged()
        val receipt = identity.supportText("instrumented", 35)
        var copied = ""
        compose.setContent { MaterialTheme {
            NightWaveBuildDetailsContent(identity, receipt, {}, { copied = it })
        } }
        compose.onNodeWithTag("nw126-build-id").assertExists()
        compose.onNodeWithText("Copy build details").performClick()
        assertEquals(receipt, copied)
    }
}
