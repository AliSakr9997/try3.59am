package com.recovery.companion.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

// ---------------------------------------------------------------------------
// Coded events only. There is deliberately NO free-text field anywhere in
// this schema: no journal text, no URLs, no search terms, no images.
// A lapse is stored as a coded flag, never as a story.
// ---------------------------------------------------------------------------

@Entity(tableName = "urge_events")
data class UrgeEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayEpoch: Long,
    val urgeLevel: Int,          // 0..10
    val triggerId: String,       // fixed list, see Triggers.kt
    val exerciseId: String?,
    val completed: Boolean,
    val createdAt: Long
)

@Entity(tableName = "day_logs")
data class DayLog(
    @PrimaryKey val date: String, // yyyy-MM-dd, device-local
    val checkInDone: Boolean,
    val lapseFlag: Boolean,       // a lapse is data, not a failure — see README
    val practiceMinutes: Int
)

@Entity(tableName = "disable_requests")
data class DisableRequest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val requestedAt: Long,
    val peerCode: String,         // 6 digits, given to the trusted person
    val approved: Boolean,
    val executed: Boolean
)

@Entity(tableName = "lapse_events")
data class LapseEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayEpoch: Long,
    val triggerId: String?,
    val noteCode: String,          // fixed codes only, never user free text
    val createdAt: Long
)

@Entity(tableName = "streak_state")
data class StreakState(
    @PrimaryKey val id: Int = 1,
    val startDayEpoch: Long,
    val currentCount: Int,
    val bestCount: Int
)

@Entity(tableName = "peer_links")
data class PeerLink(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val peerPubKey: ByteArray,     // X25519 public key from QR pairing
    val peerName: String,
    val pairedDayEpoch: Long,
    val revoked: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PeerLink
        if (id != other.id) return false
        if (!peerPubKey.contentEquals(other.peerPubKey)) return false
        if (peerName != other.peerName) return false
        if (pairedDayEpoch != other.pairedDayEpoch) return false
        if (revoked != other.revoked) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + peerPubKey.contentHashCode()
        result = 31 * result + peerName.hashCode()
        result = 31 * result + pairedDayEpoch.hashCode()
        result = 31 * result + revoked.hashCode()
        return result
    }
}

/** One clinician-approved content chunk (Arabic, MSA + Egyptian phrasing). */
@Entity(tableName = "content_chunks")
data class ContentChunk(
    @PrimaryKey val id: String,   // e.g. "w1d3-urge-surfing"
    val title: String,
    val body: String,
    val tags: String,             // comma-separated, e.g. "urge,loneliness,night"
    val exerciseId: String,
    val week: Int,                // 1..6
    val day: Int,                 // 1..42
    val source: String            // citation, e.g. "CBT: urge surfing (ACT)"
)

@Dao
interface UrgeDao {
    @Insert fun insert(event: UrgeEvent): Long
    @Query("SELECT * FROM urge_events ORDER BY createdAt DESC LIMIT 50")
    fun recent(): List<UrgeEvent>
    @Query("SELECT COUNT(DISTINCT dayEpoch) FROM urge_events WHERE completed = 1")
    fun practiceDays(): Int
    @Query("SELECT COUNT(*) FROM day_logs WHERE lapseFlag = 1")
    fun lapseCount(): Int
}

@Dao
interface DayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(log: DayLog)
    @Query("SELECT * FROM day_logs WHERE date = :date LIMIT 1")
    fun get(date: String): DayLog?
    @Query("SELECT COUNT(*) FROM day_logs WHERE checkInDone = 1")
    fun checkInDays(): Int
}

@Dao
interface DisableDao {
    @Insert fun insert(request: DisableRequest): Long
    @Query("SELECT * FROM disable_requests ORDER BY requestedAt DESC LIMIT 1")
    fun latest(): DisableRequest?
    @Query("UPDATE disable_requests SET approved = 1 WHERE id = :id")
    fun approve(id: Long)
    @Query("UPDATE disable_requests SET executed = 1 WHERE id = :id")
    fun markExecuted(id: Long)
}

@Dao
interface ContentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(chunks: List<ContentChunk>)
    @Query("SELECT COUNT(*) FROM content_chunks")
    fun count(): Int
    @Query("SELECT * FROM content_chunks WHERE week = :week ORDER BY day")
    fun byWeek(week: Int): List<ContentChunk>
    @Query("SELECT * FROM content_chunks WHERE id = :id LIMIT 1")
    fun byId(id: String): ContentChunk?

    /**
     * Offline keyword retrieval over the FTS5 index (built in onCreate).
     * Raw query on purpose: Room cannot validate SQL against the FTS5
     * virtual table because it is created by the onCreate callback, not by
     * a Room entity. The query is sanitised by [ContentRepository] before
     * it reaches here: only letters/digits/spaces are kept, terms are
     * joined with OR, and the terms go through a bound parameter.
     */
    @RawQuery(observedEntities = [ContentChunk::class])
    fun searchFtsRaw(query: SupportSQLiteQuery): List<ContentChunk>

    @Query(
        "SELECT * FROM content_chunks WHERE title LIKE '%' || :term || '%' " +
        "OR body LIKE '%' || :term || '%' OR tags LIKE '%' || :term || '%' LIMIT 5"
    )
    fun searchLike(term: String): List<ContentChunk>
}

@Dao
interface LapseDao {
    @Insert fun insert(event: LapseEvent): Long
    @Query("SELECT * FROM lapse_events ORDER BY createdAt DESC LIMIT 50")
    fun recent(): List<LapseEvent>
    @Query("SELECT COUNT(*) FROM lapse_events")
    fun count(): Int
}

@Dao
interface StreakDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(state: StreakState)
    @Query("SELECT * FROM streak_state WHERE id = 1 LIMIT 1")
    fun get(): StreakState?
}

@Dao
interface PeerDao {
    @Insert fun insert(peer: PeerLink): Long
    @Query("SELECT * FROM peer_links WHERE revoked = 0 ORDER BY pairedDayEpoch DESC")
    fun activePeers(): List<PeerLink>
    @Query("UPDATE peer_links SET revoked = 1 WHERE id = :id")
    fun revoke(id: Long)
    @Query("SELECT COUNT(*) FROM peer_links WHERE revoked = 0")
    fun activePeerCount(): Int
}

@Database(
    entities = [
        UrgeEvent::class,
        DayLog::class,
        DisableRequest::class,
        ContentChunk::class,
        LapseEvent::class,
        StreakState::class,
        PeerLink::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun urgeDao(): UrgeDao
    abstract fun dayDao(): DayDao
    abstract fun disableDao(): DisableDao
    abstract fun contentDao(): ContentDao
    abstract fun lapseDao(): LapseDao
    abstract fun streakDao(): StreakDao
    abstract fun peerDao(): PeerDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context, passphrase: ByteArray): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "recovery.db"
                )
                    .openHelperFactory(SupportOpenHelperFactory(passphrase))
                    .addCallback(FtsCallback)
                    .build()
                    .also { instance = it }
            }

        /** Creates the FTS5 external-content index for offline retrieval. */
        private object FtsCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS content_fts USING fts5(" +
                    "title, body, tags, content='content_chunks', content_rowid='rowid')"
                )
                db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS content_fts_ai AFTER INSERT ON content_chunks BEGIN " +
                    "INSERT INTO content_fts(rowid, title, body, tags) " +
                    "VALUES (new.rowid, new.title, new.body, new.tags); END"
                )
                db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS content_fts_ad AFTER DELETE ON content_chunks BEGIN " +
                    "INSERT INTO content_fts(content_fts, rowid, title, body, tags) " +
                    "VALUES ('delete', old.rowid, old.title, old.body, old.tags); END"
                )
                db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS content_fts_au AFTER UPDATE ON content_chunks BEGIN " +
                    "INSERT INTO content_fts(content_fts, rowid, title, body, tags) " +
                    "VALUES ('delete', old.rowid, old.title, old.body, old.tags); " +
                    "INSERT INTO content_fts(rowid, title, body, tags) " +
                    "VALUES (new.rowid, new.title, new.body, new.tags); END"
                )
            }
        }
    }
}
