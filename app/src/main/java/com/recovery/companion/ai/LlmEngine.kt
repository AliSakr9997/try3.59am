package com.recovery.companion.ai

import android.content.Context
import com.recovery.companion.data.SettingsStore
import org.json.JSONObject

/**
 * On-device SLM Engine & Guardrails (PR7).
 *
 * THE 7 STRICT GUARDRAIL RULES (plan §14):
 * 1. Closed Corpus Only — Prompt provides ONLY the retrieved approved chunk.
 * 2. Strict System Prompt — Egyptian Arabic, empathetic recovery framing, no external clinical advice.
 * 3. Sampling Constraints — Temperature <= 0.3, Top-P <= 0.9, Max tokens <= 150.
 * 4. Structured Schema Output — JSON output {"exercise_id": "...", "paraphrase": "...", "source_ids": ["..."]}.
 * 5. Citation Requirement — source_ids must match the retrieved chunk id.
 * 6. Verbatim Fallback — If schema, citation, length, or model fails, discard output and show verbatim chunk.
 * 7. Crisis Refusal Intercept — Medical, self-harm, diagnostic, and sexual queries are intercepted before the LLM.
 */
sealed interface LlmStatus {
    data object NotDownloaded : LlmStatus
    data object LiteMode : LlmStatus
    data object InsufficientRam : LlmStatus
    data object Ready : LlmStatus
}

data class ParaphraseRequest(
    val chunkId: String,
    val chunkTitle: String = "",
    val chunkText: String,
    val source: String = "",
    val userQuestion: String
)

data class ParaphraseResponse(
    val exerciseId: String,
    val paraphrase: String,
    val sourceIds: List<String>
)

interface LlmEngine {
    suspend fun status(): LlmStatus
    suspend fun paraphrase(request: ParaphraseRequest): String?
}

class StubLlmEngine(private val context: Context) : LlmEngine {
    private val settings = SettingsStore(context)

    override suspend fun status(): LlmStatus {
        if (settings.isLiteMode()) return LlmStatus.LiteMode
        if (settings.totalRamGb() < 4) return LlmStatus.InsufficientRam
        val model = ModelManifest.qwenPrimary
        val file = context.filesDir.resolve("models/${model.fileName}")
        return if (file.exists() && file.length() == model.sizeBytes) LlmStatus.Ready
        else LlmStatus.NotDownloaded
    }

    override suspend fun paraphrase(request: ParaphraseRequest): String? {
        // Fallback to verbatim: returning null signals the caller to show the verbatim approved chunk.
        return null
    }
}

/**
 * Encapsulates the 7 Guardrail Rules for SLM Paraphrasing.
 */
object LlmGuardrails {
    const val MAX_TOKENS = 150
    const val TEMPERATURE = 0.3f
    const val TOP_P = 0.9f
    const val MAX_OUTPUT_CHARS = 1200

    /**
     * Rule 1 & Rule 2: Builds a closed-corpus prompt in Egyptian Arabic.
     */
    fun buildClosedCorpusPrompt(request: ParaphraseRequest): String {
        return """
            أنت مساعد تعافي متخصص بالعامية المصرية. مهمتك الوحيدة هي صياغة وتلخيص المحتوى المعتمد التالي لدعم المستخدم في لحظة الرغبة أو التوتر.
            
            القواعد الصارمة:
            1. لا تضف أي نصائح طبية أو تشخيصات أو أدوية من خارج هذا النص.
            2. حافظ على لهجة دافئة ومشجعة بالعامية المصرية بدون خزي أو لوم.
            3. أجب بصيغة JSON فقط بهذا الشكل تماماً ولا تضف أي نص قبله أو بعده:
            {"exercise_id": "${request.chunkId}", "paraphrase": "صياغة المقتطف...", "source_ids": ["${request.chunkId}"]}
            
            المحتوى المعتمد سريرياً:
            المعرف: ${request.chunkId}
            العنوان: ${request.chunkTitle}
            المحتوى: ${request.chunkText}
            المصدر: ${request.source}
            
            سؤال المستخدم: ${request.userQuestion}
        """.trimIndent()
    }

    /**
     * Rule 4 & Rule 5: Validates structured JSON schema and citation matching.
     * Rule 6: Returns null if validation fails, instructing the caller to fallback to verbatim.
     */
    fun validateOutput(rawLlmOutput: String?, expectedChunkId: String): ParaphraseResponse? {
        if (rawLlmOutput.isNullOrBlank()) return null
        if (rawLlmOutput.length > MAX_OUTPUT_CHARS) return null

        return runCatching {
            // Find JSON block if model produced markdown code fences
            val jsonStr = if (rawLlmOutput.contains("{") && rawLlmOutput.contains("}")) {
                val start = rawLlmOutput.indexOf('{')
                val end = rawLlmOutput.lastIndexOf('}')
                rawLlmOutput.substring(start, end + 1)
            } else {
                rawLlmOutput
            }

            val obj = JSONObject(jsonStr)
            val exerciseId = obj.optString("exercise_id", "")
            val paraphrase = obj.optString("paraphrase", "").trim()

            if (paraphrase.isBlank()) return null

            val sourceArray = obj.optJSONArray("source_ids") ?: return null
            val sourceIds = mutableListOf<String>()
            for (i in 0 until sourceArray.length()) {
                sourceIds.add(sourceArray.getString(i))
            }

            // Rule 5: Citation Requirement — Must cite the expected chunk id
            if (!sourceIds.contains(expectedChunkId)) {
                return null
            }

            ParaphraseResponse(
                exerciseId = exerciseId,
                paraphrase = paraphrase,
                sourceIds = sourceIds
            )
        }.getOrNull()
    }
}
