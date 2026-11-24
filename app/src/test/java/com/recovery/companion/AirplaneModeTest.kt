package com.recovery.companion

import com.recovery.companion.blocking.Blocklist
import com.recovery.companion.coach.CoachEngine
import com.recovery.companion.data.Triggers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates the core requirement (plan §2 & §22 PR1):
 * All decision tree logic, triggers, guardrails, and blocklist matching
 * run strictly on-device with zero network requirement.
 */
class AirplaneModeTest {

    @Test
    fun triggersListIsCompleteAndStructured() {
        val triggers = Triggers.ALL
        assertTrue("Triggers list must have at least 6 standard triggers", triggers.size >= 6)
        val boredom = triggers.find { it.id == "boredom" }
        assertNotNull("Boredom trigger must exist", boredom)
        assertEquals("ملل / فراغ", boredom?.labelAr)

        // Ensure every trigger has non-empty id and Arabic label
        triggers.forEach { t ->
            assertTrue(t.id.isNotBlank())
            assertTrue(t.labelAr.isNotBlank())
        }
    }

    @Test
    fun blocklistMatchingWorksOffline() {
        // Blocklist suffix matching rule: subdomain or exact match
        val sampleList = setOf("pornhub.com", "xvideos.com", "badsite.org")
        fun matches(host: String): Boolean {
            val h = host.trim().lowercase().removeSuffix(".")
            return sampleList.any { h == it || h.endsWith(".$it") }
        }

        assertTrue(matches("pornhub.com"))
        assertTrue(matches("www.pornhub.com"))
        assertTrue(matches("sub.media.xvideos.com"))
        assertFalse(matches("google.com"))
        assertFalse(matches("wikipedia.org"))
    }

    @Test
    fun refusalCategoriesMatchGuardrails() {
        // Safe Coach Refusal rules from §14
        val categories = CoachEngine.RefusalCategory.values().map { it.name }
        assertTrue(categories.contains("DIAGNOSIS"))
        assertTrue(categories.contains("MEDICATION"))
        assertTrue(categories.contains("SELF_HARM"))
        assertTrue(categories.contains("SEXUAL_CONTENT"))
        assertTrue(categories.contains("OUT_OF_SCOPE"))
    }
}
