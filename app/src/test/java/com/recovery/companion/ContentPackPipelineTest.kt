package com.recovery.companion

import com.google.crypto.tink.subtle.Ed25519Sign
import com.google.crypto.tink.subtle.Ed25519Verify
import com.recovery.companion.data.ContentPackVerifier
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest
import java.util.Base64

class ContentPackPipelineTest {

    private fun getAssetFile(name: String): File {
        val candidates = listOf(
            File("src/main/assets/$name"),
            File("app/src/main/assets/$name"),
            File("../app/src/main/assets/$name")
        )
        return candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Asset $name not found in candidates: $candidates")
    }

    @Test
    fun officialAssetPackPassesVerification() {
        val packFile = getAssetFile("content_pack_v1.json")
        val manifestFile = getAssetFile("content_pack_manifest.json")

        val packBytes = packFile.readBytes()
        val manifestJson = manifestFile.readText(Charsets.UTF_8)

        val isValid = ContentPackVerifier.verifyPack(packBytes, manifestJson)
        assertTrue("Official content pack must pass Ed25519 & SHA-256 verification", isValid)
    }

    @Test
    fun tamperedPackBytesFailVerification() {
        val packFile = getAssetFile("content_pack_v1.json")
        val manifestFile = getAssetFile("content_pack_manifest.json")
        val packBytes = packFile.readBytes()
        val manifestJson = manifestFile.readText(Charsets.UTF_8)

        // Alter 1 byte in pack bytes
        val tamperedBytes = packBytes.copyOf()
        tamperedBytes[10] = (tamperedBytes[10].toInt() xor 0xFF).toByte()

        assertFalse(
            "Tampered content pack bytes must fail verification",
            ContentPackVerifier.verifyPack(tamperedBytes, manifestJson)
        )
    }

    @Test
    fun tamperedManifestSignatureFailsVerification() {
        val packFile = getAssetFile("content_pack_v1.json")
        val manifestFile = getAssetFile("content_pack_manifest.json")
        val packBytes = packFile.readBytes()
        val manifestObj = JSONObject(manifestFile.readText(Charsets.UTF_8))

        // Corrupt signature
        val corruptedSig = Base64.getEncoder().encodeToString(ByteArray(64) { 0x42 })
        manifestObj.put("signature_ed25519", corruptedSig)

        assertFalse(
            "Tampered signature must fail verification",
            ContentPackVerifier.verifyPack(packBytes, manifestObj.toString())
        )
    }

    @Test
    fun tamperedManifestSha256FailsVerification() {
        val packFile = getAssetFile("content_pack_v1.json")
        val manifestFile = getAssetFile("content_pack_manifest.json")
        val packBytes = packFile.readBytes()
        val manifestObj = JSONObject(manifestFile.readText(Charsets.UTF_8))

        // Corrupt sha256
        manifestObj.put("sha256", "0000000000000000000000000000000000000000000000000000000000000000")

        assertFalse(
            "Tampered SHA-256 must fail verification",
            ContentPackVerifier.verifyPack(packBytes, manifestObj.toString())
        )
    }

    @Test
    fun unauthorizedPublicKeyFailsVerification() {
        val packFile = getAssetFile("content_pack_v1.json")
        val packBytes = packFile.readBytes()

        // Generate an alternative key pair
        val altKeyPair = Ed25519Sign.KeyPair.newKeyPair()
        val signer = Ed25519Sign(altKeyPair.privateKey)
        val altSig = signer.sign(packBytes)
        val altPubB64 = Base64.getEncoder().encodeToString(altKeyPair.publicKey)
        val altSigB64 = Base64.getEncoder().encodeToString(altSig)

        val actualSha = MessageDigest.getInstance("SHA-256").digest(packBytes)
            .joinToString("") { "%02x".format(it) }

        val altManifest = JSONObject().apply {
            put("sha256", actualSha)
            put("signature_ed25519", altSigB64)
            put("public_key_ed25519", altPubB64)
        }

        // Must fail because public key doesn't match pinned clinical root key
        assertFalse(
            "Pack signed by unpinned key must be rejected",
            ContentPackVerifier.verifyPack(packBytes, altManifest.toString())
        )

        // But when verified explicitly against the alt key, it passes
        assertTrue(
            "Pack verified against its own key passes",
            ContentPackVerifier.verifyPack(packBytes, altManifest.toString(), pinnedKeyB64 = altPubB64)
        )
    }
}
