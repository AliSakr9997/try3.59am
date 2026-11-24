package com.recovery.companion.ai

import android.content.Context
import com.recovery.companion.data.SettingsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * On-device SLM, Phase 0 scaffold.
 *
 * HARD RULES (plan §1.6):
 * - The SLM NEVER answers from its own weights on therapeutic questions.
 * - It may only paraphrase a retrieved, approved chunk.
 * - temperature <= 0.3, max 150 tokens, output must cite the chunk id.
 * - If the model is not downloaded, RAM < 4 GB, or Lite mode is on, the
 *   caller gets [LlmStatus.NOT_AVAILABLE] and shows the verbatim chunk.
 * - The model file is downloaded on-demand over Wi-Fi only, from the signed
 *   manifest (see ModelManifest.kt). It is NEVER bundled in the APK.
 *
 * The real runtime (LiteRT-LM for Gemma, llama.cpp JNI for Qwen) is wired
 * in Phase 1 behind this interface, after a 2-week spike on 3 test devices
 * (plan §7.2). Until then this interface is the contract — and the app is
 * fully usable without any LLM at all.
 */
sealed interface LlmStatus {
    data object NotDownloaded : LlmStatus
    data object LiteMode : LlmStatus
    data object InsufficientRam : LlmStatus
    data object Ready : LlmStatus
}

data class ParaphraseRequest(
    val chunkId: String,
    val chunkText: String,
    val userQuestion: String
)

interface LlmEngine {
    suspend fun status(): LlmStatus
    /** Returns the paraphrase, or null if validation fails (caller shows verbatim). */
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
        // Phase 0: no runtime wired yet. Returning null is the safe behaviour:
        // the UI falls back to the verbatim approved chunk. Never invent text.
        return null
    }
}

/** Guardrails shared by any future runtime implementation. */
object LlmGuardrails {
    const val MAX_TOKENS = 150
    const val TEMPERATURE = 0.3f
    const val TOP_P = 0.9f

    /** A paraphrase is only shown if it cites the chunk it came from. */
    fun isValidParaphrase(text: String?, chunkId: String): Boolean {
        if (text.isNullOrBlank()) return false
        if (text.length > 1200) return false
        return text.contains(chunkId) || text.contains("المصدر")
    }
}
