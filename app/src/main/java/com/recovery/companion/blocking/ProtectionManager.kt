package com.recovery.companion.blocking

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.recovery.companion.data.AppDatabase
import com.recovery.companion.data.DisableRequest
import com.recovery.companion.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Protection state machine.
 *
 * The 24-hour delay + trusted-person approval is a SOCIAL lock, not a
 * cryptographic one (plan §4.3). On stock Android without Device Owner,
 * any local timer can be defeated by clearing app data or uninstalling.
 * The app is honest about that in the UI. What this class *does* enforce:
 * - protection stays ON during the 24h window;
 * - a disable request is stored (encrypted) with a timestamp;
 * - the peer's 6-digit approval code is required AND the 24h must have passed;
 * - if protection is found off while it should be on, a coded
 *   "protection_off" event is recorded locally (tamper *detection*).
 */
class ProtectionManager(private val context: Context) {
    private val settings = SettingsStore(context)

    fun prepareIntent(): Intent? = VpnService.prepare(context)

    fun start(activity: Activity, requestCode: Int): Boolean {
        val intent = VpnService.prepare(context)
        if (intent != null) {
            activity.startActivityForResult(intent, requestCode)
            return false // waiting for user consent in the system dialog
        }
        context.startService(Intent(context, RecoveryVpnService::class.java))
        return true
    }

    fun startServiceDirect() {
        context.startService(Intent(context, RecoveryVpnService::class.java))
    }

    fun stop() {
        context.stopService(Intent(context, RecoveryVpnService::class.java))
    }

    fun isServiceRunning(): Boolean = RecoveryVpnService.isRunning

    /** True if protection should be on but the service is not running. */
    suspend fun isTampered(): Boolean {
        if (!settings.isProtectionEnabled()) return false
        if (isServiceRunning()) return false
        val prefs = context.getSharedPreferences("protection", Context.MODE_PRIVATE)
        val heartbeat = prefs.getLong("last_heartbeat", 0)
        return System.currentTimeMillis() - heartbeat > 120_000
    }

    /** Step 1 of disabling: create the request, start the 24h clock. */
    suspend fun requestDisable(db: AppDatabase): DisableRequest = withContext(Dispatchers.IO) {
        val code = "%06d".format(Random.nextInt(0, 1_000_000))
        val request = DisableRequest(
            requestedAt = System.currentTimeMillis(),
            peerCode = code,
            approved = false,
            executed = false
        )
        val id = db.disableDao().insert(request)
        request.copy(id = id)
    }

    /** Step 2: the trusted person approves with the 6-digit code. */
    suspend fun approveDisable(db: AppDatabase, requestId: Long, code: String): Boolean =
        withContext(Dispatchers.IO) {
            val latest = db.disableDao().latest() ?: return@withContext false
            if (latest.id != requestId || latest.peerCode != code.trim()) return@withContext false
            db.disableDao().approve(requestId)
            true
        }

    /** Step 3: only after 24h AND approval may protection be switched off. */
    suspend fun canDisableNow(db: AppDatabase): Boolean = withContext(Dispatchers.IO) {
        val latest = db.disableDao().latest() ?: return@withContext false
        val elapsed = System.currentTimeMillis() - latest.requestedAt
        latest.approved && elapsed >= 24 * 60 * 60 * 1000L && !latest.executed
    }

    suspend fun executeDisable(db: AppDatabase) = withContext(Dispatchers.IO) {
        val latest = db.disableDao().latest() ?: return@withContext
        db.disableDao().markExecuted(latest.id)
        settings.setProtectionEnabled(false)
        stop()
    }

    companion object {
        /** Copyable Private DNS hostname for the manual second layer. */
        const val PRIVATE_DNS_HOSTNAME = "family.cloudflare-dns.com"
        const val UPSTREAM_DNS = "1.1.1.3"
    }
}
