package com.recovery.companion.peer

import com.google.crypto.tink.subtle.Ed25519Sign
import com.google.crypto.tink.subtle.Ed25519Verify
import com.recovery.companion.data.AppDatabase
import com.recovery.companion.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Base64
import java.util.concurrent.TimeUnit

data class WeeklyMetrics(
    val practiceDays: Int,
    val checkInDays: Int,
    val completedExercises: Int,
    val lapseCount: Int,
    val protectionActive: Boolean,
    val weekEndingEpoch: Long,
    val signatureB64: String? = null
) {
    /**
     * Formats the coded metrics into an Egyptian Arabic message
     * suitable for sharing via messaging apps with a trusted accountability partner.
     * Contains ZERO free-text notes, ZERO journals, and ZERO website history.
     */
    fun toSharableArabicText(): String {
        val protectionStatus = if (protectionActive) "درع الحماية: شغّال ومفعّل 🛡️" else "درع الحماية: متوقف ⚠️"
        return """
            تقرير التعافي الأسبوعي 📊
            • أيام الممارسة والوعي: $practiceDays من 7
            • أيام المتابعة اليومية: $checkInDays من 7
            • تمارين ركوب الموجة المكتملة: $completedExercises
            • مراجعات واعية مسجلة: $lapseCount
            • $protectionStatus
            
            (تم التوليد والتشفير محلياً على الجهاز بدون أي خوادم — بيانات رقمية فقط)
        """.trimIndent()
    }
}

object WeeklySummaryGenerator {

    suspend fun generate(
        db: AppDatabase,
        settings: SettingsStore,
        signer: Ed25519Sign? = null
    ): WeeklyMetrics = withContext(Dispatchers.IO) {
        val practiceDays = db.urgeDao().practiceDays()
        val checkInDays = db.dayDao().checkInDays()
        val completedUrges = db.urgeDao().recent().count { it.completed }
        val lapses = db.lapseDao().count()
        val protectionOn = settings.isProtectionEnabled()
        val nowEpoch = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())

        val rawMetrics = "summary:$practiceDays:$checkInDays:$completedUrges:$lapses:$protectionOn:$nowEpoch"
        val sigB64 = signer?.let {
            val sig = it.sign(rawMetrics.toByteArray(Charsets.UTF_8))
            Base64.getEncoder().encodeToString(sig)
        }

        WeeklyMetrics(
            practiceDays = practiceDays,
            checkInDays = checkInDays,
            completedExercises = completedUrges,
            lapseCount = lapses,
            protectionActive = protectionOn,
            weekEndingEpoch = nowEpoch,
            signatureB64 = sigB64
        )
    }

    fun verifySummary(metrics: WeeklyMetrics, peerPubKey: ByteArray): Boolean {
        val sigB64 = metrics.signatureB64 ?: return false
        return runCatching {
            val rawMetrics = "summary:${metrics.practiceDays}:${metrics.checkInDays}:${metrics.completedExercises}:${metrics.lapseCount}:${metrics.protectionActive}:${metrics.weekEndingEpoch}"
            val sigBytes = Base64.getDecoder().decode(sigB64)
            val verifier = Ed25519Verify(peerPubKey)
            verifier.verify(sigBytes, rawMetrics.toByteArray(Charsets.UTF_8))
            true
        }.getOrDefault(false)
    }
}
