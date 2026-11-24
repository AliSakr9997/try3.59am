package com.recovery.companion.data

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

/**
 * Holds the SQLCipher database passphrase.
 *
 * The passphrase is a random 32 bytes generated once per install and stored
 * in EncryptedSharedPreferences, whose key is backed by the Android Keystore
 * (TEE / StrongBox where available). The passphrase never leaves the device
 * and is never written to a file, a log, or a server — there is no server.
 *
 * NOTE (plan §3.2): Jetpack Security deprecates EncryptedSharedPreferences
 * in favour of a Tink-backed Encrypted DataStore. A migration to
 * com.google.crypto.tink:tink-android is tracked for Phase 2; the storage
 * contract (one random passphrase, Keystore-backed) does not change.
 */
class KeyManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "recovery_keys",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /** Returns the existing passphrase, or generates and stores one. */
    fun databasePassphrase(): ByteArray {
        val stored = prefs.getString(KEY_DB_PASSPHRASE, null)
        if (stored != null) return Base64.decode(stored, Base64.NO_WRAP)
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.edit().putString(KEY_DB_PASSPHRASE, Base64.encodeToString(bytes, Base64.NO_WRAP)).apply()
        return bytes
    }

    /** Irreversible: wipes the passphrase, which makes the encrypted DB unreadable. */
    fun wipeAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_DB_PASSPHRASE = "db_passphrase_b64"
    }
}
