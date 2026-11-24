package com.recovery.companion.data

import android.app.ActivityManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("settings")

/**
 * Non-sensitive settings only (DataStore, plaintext). Anything health-related
 * lives in the encrypted Room database, never here.
 */
class SettingsStore(private val context: Context) {
    private val onboardingDone = booleanPreferencesKey("onboarding_done")
    private val liteMode = booleanPreferencesKey("lite_mode")
    private val protectionEnabled = booleanPreferencesKey("protection_enabled")
    private val startDate = stringPreferencesKey("start_date")

    suspend fun isOnboardingDone(): Boolean =
        context.settingsDataStore.data.map { it[onboardingDone] ?: false }.first()

    suspend fun setOnboardingDone(done: Boolean) {
        context.settingsDataStore.edit { it[onboardingDone] = done }
    }

    /** Lite mode (no LLM) is the default on 2–3 GB phones — see plan §1. */
    fun defaultLiteMode(): Boolean = totalRamGb() < 4

    suspend fun isLiteMode(): Boolean =
        context.settingsDataStore.data.map { it[liteMode] ?: defaultLiteMode() }.first()

    suspend fun setLiteMode(lite: Boolean) {
        context.settingsDataStore.edit { it[liteMode] = lite }
    }

    suspend fun isProtectionEnabled(): Boolean =
        context.settingsDataStore.data.map { it[protectionEnabled] ?: false }.first()

    suspend fun setProtectionEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[protectionEnabled] = enabled }
    }

    suspend fun startDate(): String? =
        context.settingsDataStore.data.map { it[startDate] }.first()

    suspend fun setStartDate(date: String) {
        context.settingsDataStore.edit { it[startDate] = date }
    }

    fun totalRamGb(): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.totalMem / (1024L * 1024L * 1024L)
    }
}
