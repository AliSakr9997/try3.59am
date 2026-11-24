package com.recovery.companion

import com.recovery.companion.coach.CoachEngine
import com.recovery.companion.data.ContentChunk
import com.recovery.companion.data.ContentRepository
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class Egyptian200EvalTest {

    private class FakeRepo : ContentRepository(null, null) {
        override suspend fun retrieve(query: String, triggerId: String?): List<ContentChunk> {
            return listOf(
                ContentChunk(
                    id = "chunk-default",
                    title = "تمرين ركوب الموجة والتنفس",
                    body = "تنفس بهدوء واسترخاء، الرغبة مجرد موجة مؤقتة.",
                    tags = "رغبة,موجة,تنفس,عامية",
                    exerciseId = "urge-surfing",
                    week = 1,
                    day = 1,
                    source = "CBT: د. آلان مارلات"
                )
            )
        }
    }

    private lateinit var coach: CoachEngine

    @Before
    fun setup() {
        coach = CoachEngine(FakeRepo())
    }

    @Test
    fun egyptian200BenchmarkEvaluatesAccurately() = runBlocking {
        val candidates = listOf(
            File("tests/egyptian-200/egyptian_200_eval.json"),
            File("../tests/egyptian-200/egyptian_200_eval.json"),
            File("app/../tests/egyptian-200/egyptian_200_eval.json")
        )
        val file = candidates.firstOrNull { it.exists() }
        assertTrue("Egyptian-200 evaluation dataset file must exist", file != null && file.exists())

        val jsonArray = JSONArray(file!!.readText(Charsets.UTF_8))
        assertEquals("Benchmark must contain exactly 200 items", 200, jsonArray.length())

        var refusalCount = 0
        var retrievalCount = 0

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            val prompt = item.getString("prompt")
            val expectedBehavior = item.getString("expected_behavior")

            val reply = coach.respond(prompt, null)

            if (expectedBehavior == "refusal") {
                refusalCount++
                assertNotNull("Prompt '$prompt' must be refused", reply.refusal)
                val expectedCategory = item.getString("refusal_category")
                assertEquals("Refusal category mismatch for '$prompt'",
                    expectedCategory, reply.refusal?.name)
            } else {
                retrievalCount++
                assertNull("Prompt '$prompt' should be retrieved, not refused", reply.refusal)
                assertTrue("Reply for '$prompt' must contain chunks", reply.chunks.isNotEmpty())
            }
        }

        assertEquals("Must test 30 hard refusal prompts", 30, refusalCount)
        assertEquals("Must test 170 retrieval/in-scope prompts", 170, retrievalCount)
    }
}
