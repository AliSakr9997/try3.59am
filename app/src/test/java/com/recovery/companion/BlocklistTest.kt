package com.recovery.companion

import com.recovery.companion.blocking.Blocklist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.MessageDigest

class BlocklistTest {

    @Before
    fun setup() {
        val testDomains = setOf(
            "pornhub.com",
            "xvideos.com",
            "livejasmin.com",
            "badproxy.org"
        )
        Blocklist.resetForTest(testDomains)
    }

    @Test
    fun exactDomainMatchIsBlocked() {
        assertTrue(Blocklist.isBlocked("pornhub.com"))
        assertTrue(Blocklist.isBlocked("xvideos.com"))
        assertTrue(Blocklist.isBlocked("livejasmin.com"))
    }

    @Test
    fun subdomainAndDeepSubdomainAreBlocked() {
        assertTrue(Blocklist.isBlocked("www.pornhub.com"))
        assertTrue(Blocklist.isBlocked("m.pornhub.com"))
        assertTrue(Blocklist.isBlocked("sub.media.cdn.xvideos.com"))
        assertTrue(Blocklist.isBlocked("stream.livejasmin.com"))
    }

    @Test
    fun caseInsensitiveAndTrailingDotsAreHandled() {
        assertTrue(Blocklist.isBlocked("PORNHUB.COM"))
        assertTrue(Blocklist.isBlocked("Www.PornHub.Com."))
        assertTrue(Blocklist.isBlocked("XVIDEOS.COM."))
    }

    @Test
    fun nonMatchingDomainsAreAllowed() {
        assertFalse(Blocklist.isBlocked("google.com"))
        assertFalse(Blocklist.isBlocked("wikipedia.org"))
        assertFalse(Blocklist.isBlocked("github.com"))
        // Substring that is NOT a subdomain must not be blocked:
        assertFalse(Blocklist.isBlocked("notpornhub.com"))
        assertFalse(Blocklist.isBlocked("xvideos.commercial.org"))
    }

    @Test
    fun emptyOrInvalidHostsAreNotBlocked() {
        assertFalse(Blocklist.isBlocked(""))
        assertFalse(Blocklist.isBlocked("   "))
        assertFalse(Blocklist.isBlocked("."))
    }

    @Test
    fun manifestSha256ValidationRejectsTamperedContent() {
        val validText = "example.com\nmalicious.org\n"
        val validBytes = validText.toByteArray(Charsets.UTF_8)
        val validSha = MessageDigest.getInstance("SHA-256").digest(validBytes)
            .joinToString("") { "%02x".format(it) }

        // Test with matching hash
        val loadedValid = Blocklist.loadFromBytes(validBytes, validSha)
        assertTrue("Valid SHA-256 must be accepted", loadedValid)
        assertTrue(Blocklist.isBlocked("example.com"))

        // Test with tampered content
        val tamperedBytes = "example.com\nmalicious.org\ntampered.com\n".toByteArray(Charsets.UTF_8)
        val loadedTampered = Blocklist.loadFromBytes(tamperedBytes, validSha)
        assertFalse("Tampered content must be rejected", loadedTampered)
    }
}
