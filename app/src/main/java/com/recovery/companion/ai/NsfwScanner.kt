package com.recovery.companion.ai

import android.content.Context

/**
 * On-device NSFW classification — honest scope (plan §2).
 *
 * What it CAN do: score an image inside our own WebView, or a file the user
 * explicitly asks us to scan. What it CANNOT do: see inside Chrome, TikTok,
 * Instagram, Telegram or any other app — TLS encrypts that traffic, and a
 * VPN service only sees domain names, never image bytes. Any app claiming
 * otherwise is misrepresenting how TLS works.
 *
 * The classifier runs on-device (LiteRT / ONNX Runtime). Images are never
 * uploaded, never stored — only a score / boolean is kept, locally.
 * Tier 2 (NudeNet 320n, AGPL — legal review required) runs only on
 * borderline frames (score 0.35–0.65). Thresholds are tuned for recall:
 * a blocked swimwear photo is annoying; a missed explicit image defeats
 * the purpose. False positives get an "allow once" appeal that logs nothing.
 */
sealed interface NsfwVerdict {
    data object ModelNotDownloaded : NsfwVerdict
    data class Safe(val score: Float) : NsfwVerdict
    data class Uncertain(val score: Float) : NsfwVerdict
    data class Blocked(val score: Float) : NsfwVerdict
}

class NsfwScanner(private val context: Context) {
    companion object {
        /** Lowered for recall (plan §2.1). Calibrate on your own test set. */
        const val BLOCK_THRESHOLD = 0.45f
        const val UNCERTAIN_LOW = 0.35f
    }

    fun isModelReady(): Boolean {
        val f = context.filesDir.resolve("models/${ModelManifest.nsfwTier1.fileName}")
        return f.exists()
    }

    /** Phase 0: no model file is bundled, so classification is unavailable. */
    fun classifyImageBytes(@Suppress("UNUSED_PARAMETER") bytes: ByteArray): NsfwVerdict {
        if (!isModelReady()) return NsfwVerdict.ModelNotDownloaded
        // Phase 1 wires LiteRT here (224x224, OpenNSFW2). Until then, the
        // WebView layer blocks by domain (see SafeWebView) — never guess.
        return NsfwVerdict.ModelNotDownloaded
    }
}
