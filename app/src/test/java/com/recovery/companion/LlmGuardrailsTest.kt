package com.recovery.companion

import com.recovery.companion.ai.DownloadError
import com.recovery.companion.ai.LlmGuardrails
import com.recovery.companion.ai.ModelDownloadManager
import com.recovery.companion.ai.ModelManifest
import com.recovery.companion.ai.ParaphraseRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class LlmGuardrailsTest {

    private lateinit var downloadManager: ModelDownloadManager

    @Before
    fun setup() {
        downloadManager = ModelDownloadManager(null, null)
    }

    @Test
    fun closedCorpusPromptEnforcesConstraints() {
        val req = ParaphraseRequest(
            chunkId = "w1d1-urge-wave",
            chunkTitle = "ركوب الموجة",
            chunkText = "الرغبة زي موجة البحر بتعلى وتوصل لذروتها وتنزل تاني.",
            source = "CBT: د. آلان مارلات",
            userQuestion = "أنا حاسس برغبة قوية دلوقتي ومش قادر أقاوم"
        )

        val prompt = LlmGuardrails.buildClosedCorpusPrompt(req)
        assertTrue(prompt.contains("w1d1-urge-wave"))
        assertTrue(prompt.contains("ركوب الموجة"))
        assertTrue(prompt.contains("CBT: د. آلان مارلات"))
        assertTrue(prompt.contains("لا تضف أي نصائح طبية"))
        assertTrue(prompt.contains("JSON"))
    }

    @Test
    fun validStructuredOutputPassesValidation() {
        val chunkId = "w1d1-urge-wave"
        val rawJson = """
            {
              "exercise_id": "$chunkId",
              "paraphrase": "الرغبة اللي عندك طبيعية ومؤقتة. خليك هادي وتنفس بالراحة.",
              "source_ids": ["$chunkId"]
            }
        """.trimIndent()

        val resp = LlmGuardrails.validateOutput(rawJson, chunkId)
        assertNotNull("Valid structured output must pass validation", resp)
        assertEquals(chunkId, resp?.exerciseId)
        assertEquals("الرغبة اللي عندك طبيعية ومؤقتة. خليك هادي وتنفس بالراحة.", resp?.paraphrase)
        assertTrue(resp?.sourceIds?.contains(chunkId) == true)
    }

    @Test
    fun missingCitationTriggersVerbatimFallback() {
        val chunkId = "w1d1-urge-wave"
        // Model hallucinated source_ids or omitted the required chunk
        val rawJson = """
            {
              "exercise_id": "$chunkId",
              "paraphrase": "نصيحة عامة بدون مصدر معتمد.",
              "source_ids": ["unknown-source-123"]
            }
        """.trimIndent()

        val resp = LlmGuardrails.validateOutput(rawJson, chunkId)
        assertNull("Missing required citation must return null to trigger verbatim fallback", resp)
    }

    @Test
    fun invalidJsonTriggersVerbatimFallback() {
        val chunkId = "w1d1-urge-wave"
        val brokenJson = "Here is your response: {\"exercise_id\": \"$chunkId\", \"paraphrase\": " // truncated

        val resp = LlmGuardrails.validateOutput(brokenJson, chunkId)
        assertNull("Malformed JSON must return null to trigger verbatim fallback", resp)
    }

    @Test
    fun excessiveLengthTriggersVerbatimFallback() {
        val chunkId = "w1d1-urge-wave"
        val hugeParaphrase = "كلمة ".repeat(400) // exceeds max output length
        val rawJson = """
            {
              "exercise_id": "$chunkId",
              "paraphrase": "$hugeParaphrase",
              "source_ids": ["$chunkId"]
            }
        """.trimIndent()

        val resp = LlmGuardrails.validateOutput(rawJson, chunkId)
        assertNull("Excessive output length must return null to trigger verbatim fallback", resp)
    }

    @Test
    fun downloadManagerRejectsInsufficientRam() = runBlocking {
        downloadManager.isWifiOverrideForTest = true
        downloadManager.ramOverrideGbForTest = 2L // Device has 2 GB RAM

        val (canDownload, error) = downloadManager.canDownloadModel(ModelManifest.qwenPrimary)
        assertFalse("Devices with < 4 GB RAM must not download 1.7B SLM", canDownload)
        assertEquals(DownloadError.INSUFFICIENT_RAM, error)
    }

    @Test
    fun downloadManagerRejectsNonWifi() = runBlocking {
        downloadManager.isWifiOverrideForTest = false // Metered cellular data
        downloadManager.ramOverrideGbForTest = 4L

        val (canDownload, error) = downloadManager.canDownloadModel(ModelManifest.qwenPrimary)
        assertFalse("Cellular connection must be rejected", canDownload)
        assertEquals(DownloadError.WIFI_REQUIRED, error)
    }

    @Test
    fun downloadManagerVerifiesFileChecksum() {
        val tempFile = File.createTempFile("model_test", ".gguf")
        try {
            val content = "dummy-model-weights-bytes-42".toByteArray()
            tempFile.writeBytes(content)
            val expectedSha = MessageDigest.getInstance("SHA-256").digest(content)
                .joinToString("") { "%02x".format(it) }

            assertTrue("Matching checksum must verify",
                downloadManager.verifyFileChecksum(tempFile, expectedSha))
            assertFalse("Tampered checksum must fail",
                downloadManager.verifyFileChecksum(tempFile, "badhash0000000000000000000000000000000000000000000000000000000000"))
        } finally {
            tempFile.delete()
        }
    }
}
