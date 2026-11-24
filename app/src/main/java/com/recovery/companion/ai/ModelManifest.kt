package com.recovery.companion.ai

/**
 * Signed on-demand model manifest (plan §6.2).
 *
 * Models are NEVER bundled in the APK and NEVER downloaded from an unsigned
 * URL. The app downloads only the exact file named here, over HTTPS, on
 * Wi-Fi only, resumable — and refuses to load it unless its SHA-256 matches
 * the pinned value below.
 *
 * SHA-256 values are pinned at release time from the official Hugging Face
 * repo. A null sha256 means "not yet pinned — do NOT download". This is
 * deliberate: shipping a wrong hash would be worse than shipping none.
 */
data class OnDeviceModel(
    val id: String,
    val displayNameAr: String,
    val fileName: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val sha256: String?,          // null = not pinned yet, download refused
    val minRamGb: Long,
    val licence: String
)

object ModelManifest {
    /** Primary: Qwen3-1.7B Q4_K_M via llama.cpp — best Arabic, Apache-2.0. */
    val qwenPrimary = OnDeviceModel(
        id = "qwen3-1.7b-q4_k_m",
        displayNameAr = "كوين 3 — 1.7B (الأساسي)",
        fileName = "qwen3-1.7b-instruct-q4_k_m.gguf",
        downloadUrl = "https://huggingface.co/Qwen/Qwen3-1.7B-Instruct-GGUF/resolve/main/qwen3-1.7b-instruct-q4_k_m.gguf",
        sizeBytes = 1_100_000_000L,
        sha256 = null, // TODO(release): pin from Hugging Face before enabling download
        minRamGb = 4,
        licence = "Apache-2.0"
    )

    /** Fallback: Gemma 3 1B INT4 via LiteRT-LM — smallest, ~620 MB. */
    val gemmaFallback = OnDeviceModel(
        id = "gemma-3-1b-int4",
        displayNameAr = "جيما 3 — 1B (البديل الأصغر)",
        fileName = "gemma-3-1b-int4.litertlm",
        downloadUrl = "https://huggingface.co/litert-community/gemma-3-1b-it-litert-lm",
        sizeBytes = 620_000_000L,
        sha256 = null, // TODO(release): pin from Hugging Face; Gemma licence must be accepted first
        minRamGb = 4,
        licence = "Gemma (gated — licence acceptance required)"
    )

    /** NSFW Tier 1: OpenNSFW2 (~11 MB) — in-app WebView / gallery only. */
    val nsfwTier1 = OnDeviceModel(
        id = "opennsfw2",
        displayNameAr = "OpenNSFW2 — تصنيف الصور (الطبقة 1)",
        fileName = "opennsfw2.onnx",
        downloadUrl = "https://huggingface.co/",
        sizeBytes = 11_000_000L,
        sha256 = null, // TODO(release): pin from the official OpenNSFW2 release
        minRamGb = 2,
        licence = "BSD / Yahoo (verify per release)"
    )

    val all = listOf(qwenPrimary, gemmaFallback, nsfwTier1)
}
