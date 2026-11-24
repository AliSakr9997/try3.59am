package com.recovery.companion.blocking

import android.content.Context

/**
 * Curated DNS blocklist, v1 (starter set).
 *
 * Loaded from assets/blocklist_v1.txt (one domain per line) and verified
 * against assets/blocklist_manifest.json (SHA-256) before use — same signed
 * pattern as the content pack. The list is a starting point, not a claim of
 * completeness: new domains appear constantly, and DNS blocking only sees
 * domain names. It is layered with the Cloudflare Family upstream resolver
 * (1.1.1.3), which maintains its own, much larger list.
 *
 * Matching is suffix-based: "example.com" also blocks "www.example.com".
 */
object Blocklist {
    private var domains: Set<String> = emptySet()
    var version: Int = 1
        private set

    fun load(context: Context) {
        runCatching {
            val text = context.assets.open("blocklist_v1.txt").readBytes().decodeToString()
            domains = text.lineSequence()
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .toSet()
        }
    }

    fun isBlocked(host: String): Boolean {
        val h = host.trim().lowercase().removeSuffix(".")
        if (h.isEmpty()) return false
        return domains.any { h == it || h.endsWith(".$it") }
    }

    fun count(): Int = domains.size
}
