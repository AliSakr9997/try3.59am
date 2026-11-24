package com.recovery.companion.ai

import android.content.Context

sealed interface NsfwVerdict {
    data object ModelNotDownloaded : NsfwVerdict
    data class Safe(val score: Float, val tier: Int = 1) : NsfwVerdict
    data class Borderline(val score: Float, val tier: Int = 1) : NsfwVerdict
    data class Blocked(val score: Float, val reason: String, val tier: Int = 1) : NsfwVerdict
}

sealed interface VideoFrameVerdict {
    data class Allow(val ewmaScore: Float, val sampleRateFps: Int) : VideoFrameVerdict
    data class Intervene(val ewmaScore: Float, val message: String) : VideoFrameVerdict
}

data class NudeNetDetection(
    val label: String,
    val score: Float,
    val box: List<Float> = emptyList() // [ymin, xmin, ymax, xmax]
)

/**
 * SafeFrame Video Sampler:
 * - Default 1 fps sampling
 * - Densifies to 2 fps on borderline frames (0.35 - 0.65)
 * - EWMA smoothing (alpha = 0.7)
 * - Triggers intervention if smoothed score >= 0.50 or 2 consecutive high frames (>= 0.65)
 */
class SafeFrameSampler {
    var ewmaScore: Float = 0.0f
        private set
    var currentFps: Int = 1
        private set
    private var isFirstFrame: Boolean = true
    private var consecutiveHighFrames: Int = 0
    private var densifiedUntilTimestamp: Long = 0L

    companion object {
        const val BASE_FPS = 1
        const val DENSIFIED_FPS = 2
        const val EWMA_ALPHA = 0.7f
        const val INTERVENTION_THRESHOLD = 0.50f
        const val HIGH_FRAME_THRESHOLD = 0.65f
        const val BORDERLINE_LOW = 0.35f
        const val DENSIFY_DURATION_MS = 3000L
    }

    fun processFrame(frameScore: Float, timestampMs: Long): VideoFrameVerdict {
        ewmaScore = if (isFirstFrame) {
            isFirstFrame = false
            frameScore
        } else {
            EWMA_ALPHA * frameScore + (1.0f - EWMA_ALPHA) * ewmaScore
        }

        // Sampling rate adaptation: densify to 2 fps on borderline frames
        if (frameScore in BORDERLINE_LOW..HIGH_FRAME_THRESHOLD) {
            densifiedUntilTimestamp = timestampMs + DENSIFY_DURATION_MS
        }

        currentFps = if (timestampMs <= densifiedUntilTimestamp) DENSIFIED_FPS else BASE_FPS

        if (frameScore >= HIGH_FRAME_THRESHOLD) {
            consecutiveHighFrames++
        } else {
            consecutiveHighFrames = 0
        }

        return if (ewmaScore >= INTERVENTION_THRESHOLD || consecutiveHighFrames >= 2) {
            VideoFrameVerdict.Intervene(
                ewmaScore = ewmaScore,
                message = "تم إيقاف المقطع مؤقتاً لتجنب الانزلاق. خد نفس عميق وفكّر في هدفك."
            )
        } else {
            VideoFrameVerdict.Allow(
                ewmaScore = ewmaScore,
                sampleRateFps = currentFps
            )
        }
    }

    fun reset() {
        ewmaScore = 0.0f
        currentFps = BASE_FPS
        isFirstFrame = true
        consecutiveHighFrames = 0
        densifiedUntilTimestamp = 0L
    }
}

/**
 * On-device NSFW classification — two-tier pipeline (plan §13).
 *
 * Tier 1: Fast filter (OpenNSFW2, 224x224, < 40ms CPU/NNAPI).
 * - score < 0.35: Safe
 * - score > 0.65: Blocked
 * - score 0.35 - 0.65: Borderline -> routed to Tier 2
 *
 * Tier 2: NudeNet 320n detector (evaluates exposed anatomy).
 * If exposed genitalia/buttocks/breast confidence >= 0.50 -> Blocked.
 */
class NsfwScanner(private val context: Context? = null) {
    companion object {
        const val BORDERLINE_LOW = 0.35f
        const val BORDERLINE_HIGH = 0.65f
        const val DEFAULT_BLOCK_THRESHOLD = 0.45f
    }

    val sampler = SafeFrameSampler()

    fun isModelReady(): Boolean {
        val dir = context?.filesDir?.resolve("models") ?: return false
        val f = dir.resolve(ModelManifest.nsfwTier1.fileName)
        return f.exists()
    }

    fun evaluateScore(
        tier1Score: Float,
        tier2Detections: List<NudeNetDetection> = emptyList()
    ): NsfwVerdict {
        if (tier1Score < BORDERLINE_LOW) {
            return NsfwVerdict.Safe(score = tier1Score, tier = 1)
        }
        if (tier1Score > BORDERLINE_HIGH) {
            return NsfwVerdict.Blocked(score = tier1Score, reason = "Explicit content detected (Tier 1)", tier = 1)
        }

        // Borderline score (0.35 .. 0.65) -> routed to Tier 2 (NudeNet 320n)
        val exposedLabels = setOf(
            "EXPOSED_GENITALIA_F", "EXPOSED_GENITALIA_M", "EXPOSED_BUTTOCKS",
            "EXPOSED_BREAST_F", "EXPOSED_ANUS", "exposed_genitalia", "exposed_buttocks", "exposed_breast"
        )
        val explicitDetection = tier2Detections.find { d ->
            exposedLabels.any { it.equals(d.label, ignoreCase = true) } && d.score >= 0.50f
        }

        return if (explicitDetection != null) {
            NsfwVerdict.Blocked(
                score = explicitDetection.score,
                reason = "Explicit anatomy detected (Tier 2: ${explicitDetection.label})",
                tier = 2
            )
        } else {
            if (tier1Score > DEFAULT_BLOCK_THRESHOLD && tier2Detections.isEmpty()) {
                // Fail-safe to conservative block threshold if Tier 2 detector is unavailable
                NsfwVerdict.Blocked(score = tier1Score, reason = "Borderline content exceeding threshold", tier = 1)
            } else {
                NsfwVerdict.Safe(score = tier1Score, tier = 2)
            }
        }
    }

    fun classifyImageBytes(bytes: ByteArray): NsfwVerdict {
        if (!isModelReady()) return NsfwVerdict.ModelNotDownloaded
        return NsfwVerdict.ModelNotDownloaded
    }
}
