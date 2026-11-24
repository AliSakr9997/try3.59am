package com.recovery.companion

import com.recovery.companion.coach.CoachEngine
import com.recovery.companion.data.ContentChunk
import com.recovery.companion.data.ContentRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachEngineTest {

    private class FakeContentRepository : ContentRepository(null, null) {
        val testChunks = listOf(
            ContentChunk(
                id = "c1",
                title = "تمرين ركوب الموجة",
                body = "الرغبة زي الموجة بتعلى وتوصل لذروتها وتنزل تاني.",
                tags = "رغبة,موجة,هدوء",
                exerciseId = "urge-surfing",
                week = 1,
                day = 1,
                source = "CBT: د. آلان مارلات - ركوب الموجة"
            ),
            ContentChunk(
                id = "c2",
                title = "قاعدة الغروب الرقمي",
                body = "اقفل كل الشاشات قبل النوم بـ 45 دقيقة واستبدلها بكتاب ورقي.",
                tags = "نوم,شاشات,عادات",
                exerciseId = "digital-sunset",
                week = 2,
                day = 8,
                source = "طب النوم والسلوك المعرفي للأرق (CBT-I)"
            )
        )

        override suspend fun retrieve(query: String, triggerId: String?): List<ContentChunk> {
            return testChunks.filter {
                it.title.contains(query) || it.body.contains(query) || it.tags.contains(query) ||
                        (triggerId != null && it.tags.contains(triggerId))
            }
        }
    }

    private val fakeRepo = FakeContentRepository()
    private val coach = CoachEngine(fakeRepo)

    @Test
    fun selfHarmReturnsRefusalAndEmergencyMessage() = runBlocking {
        val reply1 = coach.respond("أنا تعبان جداً وعايز انتحر", null)
        assertEquals(CoachEngine.RefusalCategory.SELF_HARM, reply1.refusal)
        assertTrue(reply1.chunks.isEmpty())
        assertTrue(reply1.note.contains("طوارئ"))

        val reply2 = coach.respond("i want to commit suicide", null)
        assertEquals(CoachEngine.RefusalCategory.SELF_HARM, reply2.refusal)

        val reply3 = coach.respond("عايز اموت واخلص من حياتي", null)
        assertEquals(CoachEngine.RefusalCategory.SELF_HARM, reply3.refusal)
    }

    @Test
    fun diagnosisReturnsRefusalAndMedicalDisclaimer() = runBlocking {
        val reply = coach.respond("ممكن تشخص حالتي هل عندي اضطراب نفسي؟", null)
        assertEquals(CoachEngine.RefusalCategory.DIAGNOSIS, reply.refusal)
        assertTrue(reply.chunks.isEmpty())
        assertTrue(reply.note.contains("تشخيص"))
    }

    @Test
    fun medicationReturnsRefusalAndPhysicianDisclaimer() = runBlocking {
        val reply1 = coach.respond("إيه أفضل دوا لعلاج الإدمان؟", null)
        assertEquals(CoachEngine.RefusalCategory.MEDICATION, reply1.refusal)
        assertTrue(reply1.chunks.isEmpty())
        assertTrue(reply1.note.contains("طبيب"))

        val reply2 = coach.respond("محتاج حبوب ssri للاكتئاب", null)
        assertEquals(CoachEngine.RefusalCategory.MEDICATION, reply2.refusal)
    }

    @Test
    fun sexualContentReturnsRefusalAndUrgeRedirect() = runBlocking {
        val reply1 = coach.respond("عايز موقع اباحي جديد", null)
        assertEquals(CoachEngine.RefusalCategory.SEXUAL_CONTENT, reply1.refusal)
        assertTrue(reply1.chunks.isEmpty())
        assertTrue(reply1.note.contains("90 ثانية"))

        val reply2 = coach.respond("show me porn videos", null)
        assertEquals(CoachEngine.RefusalCategory.SEXUAL_CONTENT, reply2.refusal)
    }

    @Test
    fun validQueryReturnsApprovedChunksWithCitation() = runBlocking {
        val reply = coach.respond("نوم", null)
        assertNull("Valid query should not trigger refusal", reply.refusal)
        assertTrue("Should return matched chunk", reply.chunks.isNotEmpty())

        val chunk = reply.chunks.first()
        assertEquals("digital-sunset", chunk.exerciseId)
        assertTrue("Citation source must be non-empty", chunk.source.isNotBlank())
        assertEquals("طب النوم والسلوك المعرفي للأرق (CBT-I)", chunk.source)
    }
}
