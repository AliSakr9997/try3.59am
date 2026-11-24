package com.recovery.companion.data

import com.google.crypto.tink.subtle.Ed25519Verify
import java.security.MessageDigest
import java.util.Base64

/**
 * On-device Ed25519 cryptographic verifier for clinician-approved content packs (PR4).
 *
 * Before any therapeutic content is parsed or seeded into the encrypted database,
 * this verifier checks:
 * 1. Exact SHA-256 checksum of content_pack_v1.json matches the manifest.
 * 2. Manifest Ed25519 digital signature is cryptographically valid over the pack bytes.
 * 3. The signing key matches the pinned clinician authority root public key.
 */
object ContentPackVerifier {

    /** Pinned clinical content authority public key (Ed25519, raw 32 bytes base64-encoded). */
    const val PINNED_ROOT_PUBLIC_KEY = "Nat6KEIo/6ki+wcUYf089MxMg1gD2P+PG2UxQfeKBow="

    fun verifyPack(
        packBytes: ByteArray,
        manifestJson: String,
        pinnedKeyB64: String? = PINNED_ROOT_PUBLIC_KEY
    ): Boolean {
        return runCatching {
            val expectedSha = Regex("\"sha256\"\\s*:\\s*\"([^\"]+)\"").find(manifestJson)?.groupValues?.get(1)
                ?: return false

            val actualSha = MessageDigest.getInstance("SHA-256").digest(packBytes)
                .joinToString("") { "%02x".format(it) }

            if (!actualSha.equals(expectedSha, ignoreCase = true)) {
                return false
            }

            val sigB64 = Regex("\"signature_ed25519\"\\s*:\\s*\"([^\"]+)\"").find(manifestJson)?.groupValues?.get(1)
                ?: return false
            val pubB64 = Regex("\"public_key_ed25519\"\\s*:\\s*\"([^\"]+)\"").find(manifestJson)?.groupValues?.get(1)
                ?: return false

            // If a pinned key is configured, reject any pack signed by an unknown key
            if (pinnedKeyB64 != null && pubB64 != pinnedKeyB64) {
                return false
            }

            val sigBytes = Base64.getDecoder().decode(sigB64)
            val pubBytes = Base64.getDecoder().decode(pubB64)

            // Ed25519 signature is exactly 64 bytes; public key is exactly 32 bytes
            if (sigBytes.size != 64 || pubBytes.size != 32) {
                return false
            }

            val verifier = Ed25519Verify(pubBytes)
            verifier.verify(sigBytes, packBytes)
            true
        }.getOrDefault(false)
    }
}
