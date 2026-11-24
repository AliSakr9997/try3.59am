package com.recovery.companion

import com.recovery.companion.ai.NsfwScanner
import com.recovery.companion.ai.NsfwVerdict
import com.recovery.companion.ai.NudeNetDetection
import com.recovery.companion.ai.SafeFrameSampler
import com.recovery.companion.ai.VideoFrameVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NsfwScannerTest {

    private lateinit var scanner: NsfwScanner
    private lateinit var sampler: SafeFrameSampler

    @Before
    fun setup() {
        scanner = NsfwScanner(null)
        sampler = SafeFrameSampler()
    }

    @Test
    fun tier1SafeScoresReturnSafeVerdict() {
        val verdict = scanner.evaluateScore(0.15f)
        assertTrue("Score < 0.35 must be Safe", verdict is NsfwVerdict.Safe)
        assertEquals(0.15f, (verdict as NsfwVerdict.Safe).score, 0.001f)
        assertEquals(1, verdict.tier)
    }

    @Test
    fun tier1HighScoresReturnBlockedVerdict() {
        val verdict = scanner.evaluateScore(0.85f)
        assertTrue("Score > 0.65 must be Blocked", verdict is NsfwVerdict.Blocked)
        assertEquals(0.85f, (verdict as NsfwVerdict.Blocked).score, 0.001f)
        assertEquals(1, verdict.tier)
    }

    @Test
    fun borderlineScoreWithExplicitAnatomyReturnsBlocked() {
        // Borderline score 0.50 with exposed anatomy detected by Tier 2
        val detections = listOf(
            NudeNetDetection(label = "EXPOSED_GENITALIA_F", score = 0.82f)
        )
        val verdict = scanner.evaluateScore(0.50f, detections)
        assertTrue("Borderline with exposed anatomy must be Blocked", verdict is NsfwVerdict.Blocked)
        assertEquals(2, (verdict as NsfwVerdict.Blocked).tier)
        assertTrue(verdict.reason.contains("EXPOSED_GENITALIA_F"))
    }

    @Test
    fun borderlineScoreWithoutExplicitAnatomyReturnsSafe() {
        // Borderline score 0.40 with covered anatomy (swimwear)
        val detections = listOf(
            NudeNetDetection(label = "COVERED_BUTTOCKS", score = 0.65f)
        )
        val verdict = scanner.evaluateScore(0.40f, detections)
        assertTrue("Borderline with non-explicit anatomy must be Safe", verdict is NsfwVerdict.Safe)
        assertEquals(2, (verdict as NsfwVerdict.Safe).tier)
    }

    @Test
    fun safeFrameSamplerCalculatesEwmaAndDensifies() {
        // First frame: EWMA matches score, FPS = 1
        val v1 = sampler.processFrame(0.10f, 1000L)
        assertTrue(v1 is VideoFrameVerdict.Allow)
        assertEquals(0.10f, (v1 as VideoFrameVerdict.Allow).ewmaScore, 0.001f)
        assertEquals(1, v1.sampleRateFps)

        // Second frame: Borderline score 0.50 triggers densification to 2 fps
        val v2 = sampler.processFrame(0.50f, 2000L)
        assertTrue(v2 is VideoFrameVerdict.Allow)
        // EWMA: 0.7 * 0.50 + 0.3 * 0.10 = 0.35 + 0.03 = 0.38
        assertEquals(0.38f, (v2 as VideoFrameVerdict.Allow).ewmaScore, 0.01f)
        assertEquals(2, v2.sampleRateFps)
    }

    @Test
    fun safeFrameIntervenesOnHighEwmaScore() {
        sampler.processFrame(0.40f, 1000L)
        // Next frame very high -> EWMA exceeds 0.50
        val v = sampler.processFrame(0.80f, 2000L)
        // EWMA: 0.7 * 0.80 + 0.3 * 0.40 = 0.56 + 0.12 = 0.68 >= 0.50
        assertTrue("EWMA >= 0.50 must trigger Intervene", v is VideoFrameVerdict.Intervene)
        assertTrue((v as VideoFrameVerdict.Intervene).message.contains("إيقاف"))
    }

    @Test
    fun safeFrameIntervenesOnTwoConsecutiveHighFrames() {
        // Two consecutive frames >= 0.65
        sampler.processFrame(0.70f, 1000L)
        val v2 = sampler.processFrame(0.70f, 1500L)
        assertTrue("Two consecutive high frames must trigger Intervene", v2 is VideoFrameVerdict.Intervene)
    }
}
