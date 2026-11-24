package com.recovery.companion

import android.app.Application
import com.recovery.companion.ai.NsfwScanner
import com.recovery.companion.ai.StubLlmEngine
import com.recovery.companion.blocking.Blocklist
import com.recovery.companion.blocking.ProtectionManager
import com.recovery.companion.coach.CoachEngine
import com.recovery.companion.data.AppDatabase
import com.recovery.companion.data.ContentRepository
import com.recovery.companion.data.KeyManager
import com.recovery.companion.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application holder. Everything is created lazily and locally:
 * encrypted Room database, content pack, scripted coach, protection
 * manager, on-device AI stubs. There is no network client, no analytics
 * SDK, no account system. The app works fully in airplane mode.
 */
class RecoveryApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var keyManager: KeyManager
        private set
    lateinit var db: AppDatabase
        private set
    lateinit var settings: SettingsStore
        private set
    lateinit var content: ContentRepository
        private set
    lateinit var coach: CoachEngine
        private set
    lateinit var protection: ProtectionManager
        private set
    lateinit var llm: StubLlmEngine
        private set
    lateinit var nsfw: NsfwScanner
        private set

    override fun onCreate() {
        super.onCreate()
        keyManager = KeyManager(this)
        db = AppDatabase.get(this, keyManager.databasePassphrase())
        settings = SettingsStore(this)
        content = ContentRepository(this, db)
        coach = CoachEngine(content)
        protection = ProtectionManager(this)
        llm = StubLlmEngine(this)
        nsfw = NsfwScanner(this)
        Blocklist.load(this)
        scope.launch {
            runCatching { content.ensureSeeded() }
        }
    }
}
