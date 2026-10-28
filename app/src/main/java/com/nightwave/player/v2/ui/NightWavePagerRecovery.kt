package com.nightwave.player.v2.ui

import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/** A shelf owns horizontal edge leftovers; vertical scrolling and page gestures outside it remain normal. */
@Composable
internal fun Modifier.nightWaveShelfBoundary(): Modifier {
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource) =
                Offset(available.x, 0f)
            override suspend fun onPostFling(consumed: Velocity, available: Velocity) =
                Velocity(available.x, 0f)
        }
    }
    val gestures = LocalNightWaveShelfGestures.current
    val owner = remember { Any() }
    DisposableEffect(gestures, owner) { onDispose { gestures?.end(owner) } }
    if (gestures == null) return nestedScroll(connection)
    return nestedScroll(connection).pointerInput(gestures, owner) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            gestures.begin(owner)
            try {
                do {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                } while (event.changes.any { it.pressed })
            } finally { gestures.end(owner) }
        }
    }
}

/** Repairs restored/cancelled/nested-scroll offsets only after scrolling is idle. Never fights an active drag. */
@Composable
internal fun NightWavePagerRecovery(state: PagerState, shelfActive: Boolean = false) {
    val shelfTouch by rememberUpdatedState(shelfActive)
    val dragging by state.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(state) {
        snapshotFlow {
            NightWavePagerRecoveryPolicy.repairPage(state.currentPage, state.currentPageOffsetFraction,
                state.isScrollInProgress, dragging || shelfTouch, state.pageCount)
        }.collectLatest { page ->
            if (page != null) {
                delay(100L)
                if (NightWavePagerRecoveryPolicy.repairPage(state.currentPage, state.currentPageOffsetFraction,
                        state.isScrollInProgress, dragging || shelfTouch, state.pageCount) == page) {
                    state.scrollToPage(page)
                }
            }
        }
    }
}
