package com.nightwave.player.v2.ui

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nightwave.player.R
import com.nightwave.player.v2.model.NightWaveTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/** Isolated configuration and callbacks only. No account, stored data or playback operations. */
@RunWith(AndroidJUnit4::class)
class NW141BilingualInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private fun localized(language: String, scale: Float = 1f): Context {
        val base: Context = ApplicationProvider.getApplicationContext()
        val config = Configuration(base.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(language)); fontScale = scale
        }
        return base.createConfigurationContext(config)
    }

    @Test fun resourcesReorderParametersAndPreserveFullUnicodeTitle() {
        val strings = NightWaveStrings(localized("bn-BD").resources)
        val title = "ভালোবাসি — mixed Café 👩🏽‍🎤 " + "ক্ষ্মী".repeat(30)
        val spoken = strings.queueAnnouncement(12, title, "শিল্পী", false)
        assertTrue(spoken.contains("১২")); assertTrue(spoken.contains(title))
        assertTrue(spoken.contains("শিল্পী")); assertFalse(spoken.contains("Play track"))
        assertEquals("নামহীন গান", strings.get(R.string.nw141_untitled_track))
    }

    @Test fun quantityAndDurationUseTheActiveResourceLocale() {
        val en = NightWaveStrings(localized("en").resources)
        val bn = NightWaveStrings(localized("bn-BD").resources)
        assertEquals("1 track", en.quantity(R.plurals.nw141_tracks, 1))
        assertEquals("2 tracks", en.quantity(R.plurals.nw141_tracks, 2))
        assertEquals("০টি গান", bn.quantity(R.plurals.nw141_tracks, 0))
        assertEquals("১:০৫", bn.duration(65_000))
        assertNotEquals(en.date(1234), bn.date(1234))
    }

    @Test fun actualNavigationSurvivesLocaleChangeAndKeepsSelectedAction() {
        val language = mutableStateOf("en")
        val selected = mutableStateOf(0)
        compose.setContent {
            val context = localized(language.value)
            CompositionLocalProvider(LocalContext provides context,
                LocalConfiguration provides context.resources.configuration) {
                NightWaveMaterialTheme(NightWaveTheme.MIDNIGHT, reduceMotion = true) {
                    NightWavePremiumNavigation(selected.value, true) { selected.value = it }
                }
            }
        }
        compose.onNodeWithTag("nightwave-tab-search").performClick().assertIsSelected()
        compose.runOnIdle { language.value = "bn-BD" }
        compose.onNodeWithContentDescription("অনুসন্ধান").assertIsSelected()
        compose.onNodeWithTag("nightwave-tab-library").performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(2, selected.value) }
    }

    @Test fun actualApplyAndDiscardRemainSeparateAtLargeBanglaText() {
        val context = localized("bn-BD", 3f)
        var applied = 0; var discarded = 0
        compose.setContent {
            CompositionLocalProvider(LocalContext provides context,
                LocalConfiguration provides context.resources.configuration) {
                NightWaveMaterialTheme(NightWaveTheme.PEARL, reduceMotion = true) {
                    SoundStudioApplyBar(true, true, false, { applied++ }, { discarded++ })
                }
            }
        }
        compose.onNodeWithTag("nw138-apply").assertIsDisplayed().performClick()
        compose.onNodeWithTag("nw138-discard").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, applied); assertEquals(1, discarded) }
    }
}
