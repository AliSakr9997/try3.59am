package com.recovery.companion.coach

import com.recovery.companion.data.ContentChunk
import com.recovery.companion.data.ContentRepository

/**
 * The coach is SCRIPTED + retrieval-based. It never generates therapy from
 * a language model's weights.
 *
 * Flow: user picks a feeling/trigger (coded) -> offline FTS5 retrieval over
 * the approved content pack -> the app shows the retrieved chunk VERBATIM
 * with its citation. An on-device SLM may ONLY paraphrase that chunk
 * (temp <= 0.3, <= 150 tokens) and only when the model is downloaded and
 * the device has >= 4 GB RAM; if anything is off, the verbatim chunk is
 * shown instead. There is no server, so nothing is ever sent anywhere.
 */
class CoachEngine(private val content: ContentRepository) {

    /** Topics the user can refuse / ask for help with — hard refusal, verbatim reply. */
    enum class RefusalCategory { DIAGNOSIS, MEDICATION, SELF_HARM, SEXUAL_CONTENT, OUT_OF_SCOPE }

    data class CoachReply(
        val chunks: List<ContentChunk>,
        val refusal: RefusalCategory? = null,
        val note: String
    )

    suspend fun respond(userText: String, triggerId: String?): CoachReply {
        val lower = userText.lowercase()
        val refusal = detectRefusal(lower)
        if (refusal != null) {
            return CoachReply(
                chunks = emptyList(),
                refusal = refusal,
                note = refusalMessage(refusal)
            )
        }
        val hits = content.retrieve(userText, triggerId)
        return CoachReply(
            chunks = hits,
            note = "دي مقتطفات من المحتوى المعتمد، مع مصدر كل واحدة. الموديل — لو نزلته — بيعيد صياغتها بس، مش بيألف علاج."
        )
    }

    private fun detectRefusal(text: String): RefusalCategory? = when {
        listOf(
            "انتحر", "أنتحر", "انتحار", "اقتل نفسي", "أقتل نفسي", "عايز اموت", "عايز أموت",
            "اخلص من حياتي", "أخلص من حياتي", "suicide", "kill myself", "end my life"
        ).any { text.contains(it) } -> RefusalCategory.SELF_HARM
        
        listOf(
            "شخّص", "شخصني", "تشخيص", "عندي ايه", "عندي إيه", "هل انا مريض", "هل أنا مريض",
            "مرض نفسي", "اضطراب نفسي", "diagnos"
        ).any { text.contains(it) } -> RefusalCategory.DIAGNOSIS
        
        listOf(
            "دوا", "دواء", "أدوية", "ادوية", "حبوب", "علاج دوائي", "مضاد اكتئاب",
            "جرعة", "منشطات", "medication", "medicine", "pill", "ssri", "naltrexone"
        ).any { text.contains(it) } -> RefusalCategory.MEDICATION
        
        listOf(
            "فيديو", "صور", "اباحي", "إباحي", "سكس", "أفلام", "افلام", "مواقع اباحية",
            "مواقع إباحية", "موقع جنسي", "porn", "xxx", "nude", "erotic"
        ).any { text.contains(it) } -> RefusalCategory.SEXUAL_CONTENT
        
        else -> null
    }

    fun refusalMessage(category: RefusalCategory): String = when (category) {
        RefusalCategory.SELF_HARM ->
            "لو حاسس إنك ممكن تأذي نفسك، اطلب مساعدة إنسان دلوقتي فورًا — كلّم شخص قريب منك أو خدمة طوارئ في بلدك. التطبيق مش أداة طوارئ ومش بديل عن مساعدة بشرية."
        RefusalCategory.DIAGNOSIS ->
            "مقدرش أشخّص أي حالة — التطبيق تعليمي وداعم بس. لو عايز تشخيص أو خطة علاج، ده شغل معالج أو طبيب نفسي."
        RefusalCategory.MEDICATION ->
            "مقدرش أنصح بأي دوا أو جرعة. أي قرار دوائي لازم يكون مع طبيب."
        RefusalCategory.SEXUAL_CONTENT ->
            "مش هقدر أساعد في الوصول لمحتوى إباحي أو أوصفه. لو الرغبة شديدة، جرّب تمرين الـ 90 ثانية من الشاشة الرئيسية."
        RefusalCategory.OUT_OF_SCOPE ->
            "السؤال ده برّه المحتوى المعتمد اللي عندي. جرّب تختار موضوع من القايمة، أو اسأل بصياغة تانية."
    }
}
