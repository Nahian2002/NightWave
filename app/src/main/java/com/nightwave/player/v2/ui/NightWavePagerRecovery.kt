package com.nightwave.player.v2.ui

import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
    return nestedScroll(connection)
}

/** Repairs restored/cancelled/nested-scroll offsets only after scrolling is idle. Never fights an active drag. */
@Composable
internal fun NightWavePagerRecovery(state: PagerState) {
    val dragging by state.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(state) {
        snapshotFlow {
            NightWavePagerRecoveryPolicy.repairPage(state.currentPage, state.currentPageOffsetFraction,
                state.isScrollInProgress, dragging, state.pageCount)
        }.collectLatest { page ->
            if (page != null) {
                delay(100L)
                if (NightWavePagerRecoveryPolicy.repairPage(state.currentPage, state.currentPageOffsetFraction,
                        state.isScrollInProgress, dragging, state.pageCount) == page) {
                    state.scrollToPage(page)
                }
            }
        }
    }
}
