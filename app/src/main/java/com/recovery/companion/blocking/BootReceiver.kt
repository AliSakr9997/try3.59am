package com.recovery.companion.blocking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.recovery.companion.data.SettingsStore

/**
 * After a reboot, Android requires VPN consent again via an Activity, so a
 * receiver cannot silently restart the tunnel. Best effort: if the user had
 * protection on AND VpnService.prepare() already returns null (still
 * authorised), restart the service. Otherwise the app shows a prominent
 * "protection is off — tap to re-enable" banner on next launch.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        CoroutineScope(Dispatchers.IO).launch {
            val settings = SettingsStore(context)
            if (!settings.isProtectionEnabled()) return@launch
            if (VpnService.prepare(context) == null) {
                context.startService(Intent(context, RecoveryVpnService::class.java))
            }
        }
    }
}
