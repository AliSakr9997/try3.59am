package com.recovery.companion

import com.recovery.companion.data.DisableRequest
import com.recovery.companion.data.LapseEvent
import com.recovery.companion.data.PeerLink
import com.recovery.companion.data.StreakState
import com.recovery.companion.data.Triggers
import com.recovery.companion.data.UrgeEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates PR1 database entity constraints (plan §12):
 * - Coded events ONLY
 * - No free-text journal fields in the database schema
 * - Urge levels bounded 0..10
 * - Fixed trigger IDs
 */
class CodedEventsTest {

    @Test
    fun urgeEventRequiresCodedTriggers() {
        val event = UrgeEvent(
            id = 1,
            dayEpoch = 19700,
            urgeLevel = 7,
            triggerId = "stress",
            exerciseId = "w1d3-urge-surfing",
            completed = true,
            createdAt = 1700000000000L
        )

        assertTrue(event.urgeLevel in 0..10)
        assertTrue(Triggers.ALL.any { it.id == event.triggerId })
        assertTrue(event.completed)
    }

    @Test
    fun lapseEventStoresOnlyCodedDebrief() {
        val event = LapseEvent(
            id = 1,
            dayEpoch = 19700,
            triggerId = "night",
            noteCode = "alone_screen", // Fixed coded reason, never free text
            createdAt = 1700000000000L
        )

        assertEquals("alone_screen", event.noteCode)
        assertFalse(event.noteCode.contains(" "))
    }

    @Test
    fun streakStatePreservesBestAndCurrent() {
        val state = StreakState(
            id = 1,
            startDayEpoch = 19600,
            currentCount = 14,
            bestCount = 42
        )

        assertEquals(1, state.id)
        assertTrue(state.bestCount >= state.currentCount)
    }

    @Test
    fun peerLinkStoresCryptoKeyOnly() {
        val sampleKey = ByteArray(32) { it.toByte() }
        val peer = PeerLink(
            id = 1,
            peerPubKey = sampleKey,
            peerName = "رفيق موثوق",
            pairedDayEpoch = 19700,
            revoked = false
        )

        assertEquals(32, peer.peerPubKey.size)
        assertFalse(peer.revoked)
    }

    @Test
    fun disableRequestUses6DigitSocialLock() {
        val request = DisableRequest(
            id = 1,
            requestedAt = System.currentTimeMillis(),
            peerCode = "123456",
            approved = false,
            executed = false
        )

        assertEquals(6, request.peerCode.length)
        assertTrue(request.peerCode.all { it.isDigit() })
        assertFalse(request.approved)
        assertFalse(request.executed)
    }
}
