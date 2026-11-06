package com.nightwave.player.net

import org.junit.Assert.assertEquals
import org.junit.Test

class PublicSearchRoutingTest {
    @Test fun recoveredDirectFailureTemporarilyPromotesCommunityWithoutExtendingTheWindow() {
        var now = 100L
        val routing = PublicSearchRouting { now }
        assertEquals(listOf(PublicSearchPhase.DIRECT, PublicSearchPhase.COMMUNITY), routing.order())

        routing.recordSuccess(PublicSearchPhase.COMMUNITY, directFailed = false)
        assertEquals(PublicSearchPhase.DIRECT, routing.order().first())
        routing.recordSuccess(PublicSearchPhase.COMMUNITY, directFailed = true)
        assertEquals(listOf(PublicSearchPhase.COMMUNITY, PublicSearchPhase.DIRECT), routing.order())

        now += 59_999
        routing.recordSuccess(PublicSearchPhase.COMMUNITY, directFailed = false)
        assertEquals(PublicSearchPhase.COMMUNITY, routing.order().first())
        now++
        assertEquals(PublicSearchPhase.DIRECT, routing.order().first())
    }

    @Test fun aSuccessfulDirectRetryRestoresItsPriorityImmediately() {
        val routing = PublicSearchRouting { 100L }
        routing.recordSuccess(PublicSearchPhase.COMMUNITY, directFailed = true)
        routing.recordSuccess(PublicSearchPhase.DIRECT, directFailed = false)
        assertEquals(listOf(PublicSearchPhase.DIRECT, PublicSearchPhase.COMMUNITY), routing.order())
    }
}
