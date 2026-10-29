package com.nightwave.player.v2.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.nightwave.player.net.VideoPreview
import com.nightwave.player.playback.QueueEditSnapshot
import com.nightwave.player.v2.playback.StudioPlayerState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** In-memory callbacks only. Real service, audio and restart tests remain independent. */
class NightWaveQueueEditingInstrumentedTest {
    @get:Rule val compose=createComposeRule()
    private fun song(i:Int)=VideoPreview("Queue song $i","Creator",
        "https://www.youtube.com/watch?v="+i.toString().padStart(11,'0'),"",120L)
    private val songs=(1..3).map(::song)
    private fun state()=StudioPlayerState(connected=true,videoUrl=songs[0].url,queue=songs,queueIndex=0)

    @Test fun clearRequiresConfirmationAndRetainsTheReviewedSnapshotAcrossAnUpdate() {
        var shown by mutableStateOf(state())
        var received:QueueEditSnapshot?=null
        compose.setContent { MaterialTheme {
            StudioQueueSheet(shown,{}, {}, {_,_->null}, {}, true,
                onClearUpcoming={ received=it; true })
        } }
        compose.onNodeWithTag("nw134-clear-upcoming").performClick()
        assertNull(received)
        compose.runOnIdle { shown=shown.copy(queue=listOf(songs[0],songs[2])) }
        compose.onNodeWithTag("nw134-confirm-clear").performClick()
        compose.runOnIdle { assertEquals(songs.map { it.url },received!!.urls) }
    }
    @Test fun editingAnUpcomingSongSendsItsAbsoluteSlotsAndExactIdentities() {
        var received:Triple<Int,Int,QueueEditSnapshot>?=null
        compose.setContent { MaterialTheme {
            StudioQueueSheet(state(),{}, {}, {_,_->null}, {}, true,
                onMoveUpcoming={ from,to,snapshot -> received=Triple(from,to,snapshot); true })
        } }
        compose.onNodeWithTag("nw134-edit-order").performClick()
        compose.onNodeWithContentDescription("Queue options for Queue song 2").performClick()
        compose.onNodeWithText("Move later").performClick()
        compose.runOnIdle {
            assertEquals(1,received!!.first); assertEquals(2,received!!.second)
            assertEquals(songs.map { it.url },received!!.third.urls)
            assertEquals(songs[0].url,received!!.third.playingUrl)
        }
    }
    @Test fun shuffledQueueDoesNotOfferLinearDestructiveEdits() {
        compose.setContent { MaterialTheme {
            StudioQueueSheet(state().copy(shuffle=true),{}, {}, {_,_->null}, {}, true,
                onMoveUpcoming={_,_,_->error("Unexpected edit")},onClearUpcoming={error("Unexpected clear")})
        } }
        compose.onNodeWithTag("nw134-edit-order").assertDoesNotExist()
        compose.onNodeWithTag("nw134-clear-upcoming").assertDoesNotExist()
        compose.onNodeWithText("Turn Shuffle off to reorder, remove or clear upcoming songs.").assertExists()
    }
}
