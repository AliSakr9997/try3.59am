package com.recovery.companion.blocking

import android.content.Context
import java.security.MessageDigest

/**
 * Curated DNS blocklist, v1 (curated set).
 *
 * Loaded from assets/blocklist_v1.txt (one domain per line) and verified
 * against assets/blocklist_manifest.json (SHA-256) before use.
 *
 * Matching is suffix-based: "example.com" also blocks "www.example.com",
 * "sub.media.example.com".
 *
 * Layered with Cloudflare Family upstream resolver (1.1.1.3), which enforces
 * its own large filter list at the DNS level.
 */
object Blocklist {
    private var domains: Set<String> = emptySet()
    var version: Int = 1
        private set
    var isVerified: Boolean = false
        private set

    fun load(context: Context): Boolean {
        return runCatching {
            val bytes = context.assets.open("blocklist_v1.txt").readBytes()
            val manifestBytes = context.assets.open("blocklist_manifest.json").readBytes().decodeToString()
            val expectedSha = Regex("\"sha256\"\\s*:\\s*\"([0-9a-f]{64})\"").find(manifestBytes)?.groupValues?.get(1)
            loadFromBytes(bytes, expectedSha)
        }.getOrDefault(false)
    }

    fun loadFromBytes(bytes: ByteArray, expectedSha: String? = null): Boolean {
        if (expectedSha != null) {
            val actualSha = MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it) }
            if (!actualSha.equals(expectedSha, ignoreCase = true)) {
                isVerified = false
                return false
            }
        }
        val text = bytes.decodeToString()
        domains = text.lineSequence()
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .toSet()
        isVerified = true
        return true
    }

    /**
     * Suffix-based domain check:
     * e.g. for host "sub.media.pornhub.com", checks:
     * - "sub.media.pornhub.com"
     * - "media.pornhub.com"
     * - "pornhub.com"
     */
    fun isBlocked(host: String): Boolean {
        var h = host.trim().lowercase().removeSuffix(".")
        if (h.isEmpty()) return false
        while (h.isNotEmpty()) {
            if (domains.contains(h)) return true
            val dot = h.indexOf('.')
            if (dot < 0) break
            h = h.substring(dot + 1)
        }
        return false
    }

    fun count(): Int = domains.size

    fun resetForTest(testDomains: Set<String>) {
        domains = testDomains.map { it.trim().lowercase().removeSuffix(".") }.toSet()
        isVerified = true
    }
}
