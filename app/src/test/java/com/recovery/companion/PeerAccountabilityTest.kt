package com.recovery.companion

import com.recovery.companion.peer.PeerManager
import com.recovery.companion.peer.WeeklyMetrics
import com.recovery.companion.peer.WeeklySummaryGenerator
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Base64

class PeerAccountabilityTest {

    private lateinit var peerManager: PeerManager

    @Before
    fun setup() {
        peerManager = PeerManager(null)
    }

    @Test
    fun pairingPayloadContainsRequiredFields() {
        val payload = peerManager.createPairingPayload("أحمد")
        assertNotNull(payload)

        val obj = JSONObject(payload)
        assertEquals(1, obj.getInt("v"))
        assertEquals("أحمد", obj.getString("alias"))
        assertTrue("peer_id must start with urn:uuid:", obj.getString("peer_id").startsWith("urn:uuid:"))

        val pubKeyB64 = obj.getString("pubkey_b64")
        val pubBytes = Base64.getDecoder().decode(pubKeyB64)
        assertEquals("Ed25519 public key must be exactly 32 bytes", 32, pubBytes.size)
    }

    @Test
    fun unpairMessageSignsAndVerifies() {
        val now = 1729000000000L
        val peerLinkId = 42L
        val rawPayload = "unpair:$peerLinkId:$now"
        val sig = peerManager.getSigner().sign(rawPayload.toByteArray(Charsets.UTF_8))
        val sigB64 = Base64.getEncoder().encodeToString(sig)

        val messageJson = JSONObject().apply {
            put("action", "unpair")
            put("peer_link_id", peerLinkId)
            put("timestamp", now)
            put("public_key_b64", peerManager.getPublicKeyB64())
            put("signature_b64", sigB64)
        }.toString()

        val isValid = peerManager.verifyUnpairMessage(messageJson, peerManager.getPublicKeyBytes())
        assertTrue("Genuine signed unpair message must verify", isValid)

        // Corrupted payload must fail
        val tamperedJson = JSONObject(messageJson).apply {
            put("peer_link_id", 999L)
        }.toString()
        assertFalse("Tampered unpair message must fail verification",
            peerManager.verifyUnpairMessage(tamperedJson, peerManager.getPublicKeyBytes()))
    }

    @Test
    fun weeklyMetricsContainZeroSensitiveFreeText() {
        val metrics = WeeklyMetrics(
            practiceDays = 5,
            checkInDays = 6,
            completedExercises = 8,
            lapseCount = 1,
            protectionActive = true,
            weekEndingEpoch = 20000L
        )

        val text = metrics.toSharableArabicText()
        assertTrue(text.contains("تقرير التعافي الأسبوعي"))
        assertTrue(text.contains("5 من 7"))
        assertTrue(text.contains("6 من 7"))
        assertTrue(text.contains("درع الحماية: شغّال"))

        // Verify zero sensitive free text or web traces
        assertFalse(text.contains("http://"))
        assertFalse(text.contains("https://"))
        assertFalse(text.contains(".com"))
        assertFalse(text.contains("diary"))
        assertFalse(text.contains("journal"))
    }

    @Test
    fun weeklySummaryAttestationDetectsTampering() {
        val signer = peerManager.getSigner()
        val practiceDays = 6
        val checkInDays = 7
        val completed = 10
        val lapses = 0
        val protectionOn = true
        val epoch = 20050L

        val raw = "summary:$practiceDays:$checkInDays:$completed:$lapses:$protectionOn:$epoch"
        val sig = signer.sign(raw.toByteArray(Charsets.UTF_8))
        val sigB64 = Base64.getEncoder().encodeToString(sig)

        val metrics = WeeklyMetrics(
            practiceDays = practiceDays,
            checkInDays = checkInDays,
            completedExercises = completed,
            lapseCount = lapses,
            protectionActive = protectionOn,
            weekEndingEpoch = epoch,
            signatureB64 = sigB64
        )

        val verified = WeeklySummaryGenerator.verifySummary(metrics, peerManager.getPublicKeyBytes())
        assertTrue("Genuine signed summary must verify", verified)

        // Tamper with lapse count
        val tampered = metrics.copy(lapseCount = 3)
        val tamperedVerified = WeeklySummaryGenerator.verifySummary(tampered, peerManager.getPublicKeyBytes())
        assertFalse("Tampered summary metrics must fail cryptographic verification", tamperedVerified)
    }
}
