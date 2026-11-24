package com.recovery.companion.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.security.MessageDigest

/**
 * Loads the signed, clinician-approved content pack (assets) into the
 * encrypted database on first run, and runs offline keyword retrieval.
 *
 * Verification: the SHA-256 of content_pack_v1.json must match
 * content_pack_manifest.json before any chunk is inserted. A mismatch means
 * the pack was tampered with — the app refuses to load it and keeps the
 * previous (or empty) content rather than showing unverified therapy text.
 */
open class ContentRepository(private val context: Context? = null, private val db: AppDatabase? = null) {

    open suspend fun ensureSeeded(): Int = withContext(Dispatchers.IO) {
        val database = db ?: return@withContext 0
        val ctx = context ?: return@withContext 0
        val dao = database.contentDao()
        val existing = dao.count()
        if (existing > 0) return@withContext existing
        val jsonBytes = ctx.assets.open("content_pack_v1.json").readBytes()
        val manifest = ctx.assets.open("content_pack_manifest.json").readBytes().decodeToString()
        if (!ContentPackVerifier.verifyPack(jsonBytes, manifest)) {
            throw IllegalStateException("Content pack failed cryptographic signature check — refusing to load.")
        }
        val chunks = parse(jsonBytes.decodeToString())
        dao.insertAll(chunks)
        chunks.size
    }

    private fun parse(json: String): List<ContentChunk> {
        val arr = JSONArray(json)
        val out = ArrayList<ContentChunk>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += ContentChunk(
                id = o.getString("id"),
                title = o.getString("title"),
                body = o.getString("body"),
                tags = o.getString("tags"),
                exerciseId = o.getString("exercise_id"),
                week = o.getInt("week"),
                day = o.getInt("day"),
                source = o.getString("source")
            )
        }
        return out
    }

    /** Offline retrieval: FTS5 first, LIKE fallback if the FTS query is empty. */
    open suspend fun retrieve(query: String, triggerId: String? = null): List<ContentChunk> =
        withContext(Dispatchers.IO) {
            val database = db ?: return@withContext emptyList()
            val terms = query.split(Regex("[^\\p{L}\\p{N}]+"))
                .filter { it.length > 1 }.distinct().take(5)
            val dao = database.contentDao()
            if (terms.isNotEmpty()) {
                val fts = terms.joinToString(" OR ") { "\"$it\"" }
                val query = androidx.sqlite.db.SimpleSQLiteQuery(
                    "SELECT c.* FROM content_chunks c JOIN content_fts f " +
                    "ON f.rowid = c.rowid WHERE content_fts MATCH ? LIMIT 5",
                    arrayOf<Any>(fts)
                )
                val hits = runCatching { dao.searchFtsRaw(query) }.getOrNull().orEmpty()
                if (hits.isNotEmpty()) return@withContext hits
            }
            val byTrigger = triggerId?.let { dao.searchLike(it) }.orEmpty()
            val byTerm = terms.firstOrNull()?.let { dao.searchLike(it) }.orEmpty()
            (byTrigger + byTerm).distinctBy { it.id }.take(5)
        }

    open suspend fun week(week: Int): List<ContentChunk> = withContext(Dispatchers.IO) {
        db?.contentDao()?.byWeek(week).orEmpty()
    }
}
