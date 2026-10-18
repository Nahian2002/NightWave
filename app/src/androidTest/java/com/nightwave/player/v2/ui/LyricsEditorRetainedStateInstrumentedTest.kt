package com.nightwave.player.v2.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nightwave.player.v2.lyrics.LyricsEditorPolicy
import com.nightwave.player.v2.lyrics.TimedLyricsCodec
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Uses an isolated ViewModelStore; never opens or writes real user lyrics. */
@RunWith(AndroidJUnit4::class)
class LyricsEditorRetainedStateInstrumentedTest {
    @Test fun rotationStyleRecreationPreservesUnsavedDraftOnlyInMemory() {
        val scope = ViewModelStore()
        val owner = object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = scope
        }
        try {
            val first = ViewModelProvider(owner)[LyricsEditorRetainedState::class.java]
            val original = LyricsEditorPolicy.begin(TimedLyricsCodec.parse(
                "[00:01.000]Original".toByteArray()))
            val edited = LyricsEditorPolicy.updateText(original, 0, "Unsaved edit")
            first.epoch = 12
            first.session = LyricsEditorSession(
                requestId = 12, songId = "example.mp3", songTitle = "Test",
                expectedFingerprint = null, baseline = original, draft = edited)
            val recreated = ViewModelProvider(owner)[LyricsEditorRetainedState::class.java]
            assertSame(first, recreated)
            assertEquals(12, recreated.epoch)
            assertEquals("Unsaved edit", recreated.session?.draft?.cues?.single()?.text)
            assertEquals("Original", original.cues.single().text)
        } finally {
            scope.clear()
        }
    }

    @Test fun explicitDiscardClearsOnlyRetainedDraftNotBaseline() {
        val scope = ViewModelStore()
        val owner = object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = scope
        }
        try {
            val state = ViewModelProvider(owner)[LyricsEditorRetainedState::class.java]
            val original = LyricsEditorPolicy.begin(TimedLyricsCodec.parse(
                "[00:05.000]Saved".toByteArray()))
            state.session = LyricsEditorSession(
                requestId = 1, songId = "example.mp3", songTitle = "Test",
                expectedFingerprint = null, baseline = original,
                draft = LyricsEditorPolicy.updateText(original, 0, "Draft"))
            state.session = null
            assertNull(state.session)
            assertEquals("Saved", original.cues.single().text)
        } finally {
            scope.clear()
        }
    }
}
