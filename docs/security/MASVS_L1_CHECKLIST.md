# OWASP MASVS Level 1 (L1) Audit Checklist & MobSF Verification

This document provides the security audit checklist against the **OWASP Mobile Application Security Verification Standard (MASVS) Level 1 (Standard Security)**.

---

## 1. MASVS-STORAGE: Data Storage and Privacy

| ID | Requirement | Verification in Codebase | Status |
| :--- | :--- | :--- | :--- |
| **MASVS-STORAGE-1** | System credential storage facilities used for sensitive data. | Database passphrases generated via `SecureRandom` (32 bytes) and stored via `KeyManager` using Android Keystore-backed `EncryptedSharedPreferences`. | **PASS** |
| **MASVS-STORAGE-2** | No sensitive data written to application logs. | `android.util.Log` calls are completely absent for sensitive metrics. No user text or queries are ever printed. | **PASS** |
| **MASVS-STORAGE-3** | No sensitive data shared with third parties. | Zero external analytics, telemetry, crash reporting, or advertising SDKs. | **PASS** |
| **MASVS-STORAGE-4** | Local databases encrypted with robust crypto. | Room encrypted with **SQLCipher 4.6.1 (net.zetetic:sqlcipher-android)** using AES-256 in CBC mode with HMAC-SHA512. | **PASS** |
| **MASVS-STORAGE-5** | Sensitive data cleared when no longer needed. | Instant data wipe implemented in Settings (`KeyManager.wipeAll()` + database deletion). | **PASS** |

---

## 2. MASVS-CRYPTO: Cryptography Architecture

| ID | Requirement | Verification in Codebase | Status |
| :--- | :--- | :--- | :--- |
| **MASVS-CRYPTO-1** | App uses proven implementations of cryptographic algorithms. | Uses Google Tink (`com.google.crypto.tink:tink-android:1.15.0`) for Ed25519 signatures and Android Keystore for AES-256 keys. | **PASS** |
| **MASVS-CRYPTO-2** | Strong random numbers generated securely. | Uses `java.security.SecureRandom` for all passphrase and peer code generation. | **PASS** |
| **MASVS-CRYPTO-3** | No hardcoded encryption keys. | Pinned public keys are asymmetric public verification keys only; private signing keys are generated at runtime and never committed. | **PASS** |

---

## 3. MASVS-NETWORK: Network Communication

| ID | Requirement | Verification in Codebase | Status |
| :--- | :--- | :--- | :--- |
| **MASVS-NETWORK-1** | Cleartext traffic disabled. | Manifest specifies `android:usesCleartextTraffic="false"` and enforces TLS for model downloads. | **PASS** |
| **MASVS-NETWORK-2** | TLS certificates and hashes verified. | Model downloads and content packs enforce cryptographic SHA-256 hash validation before loading. | **PASS** |

---

## 4. MASVS-PLATFORM: Platform Interaction

| ID | Requirement | Verification in Codebase | Status |
| :--- | :--- | :--- | :--- |
| **MASVS-PLATFORM-1** | App permissions minimized to essential scope. | Manifest requests only `INTERNET` (for user-requested downloads/web view) and `FOREGROUND_SERVICE_SYSTEM_EXEMPTED` (for local VPN). No location, SMS, contacts, or microphone permissions. | **PASS** |
| **MASVS-PLATFORM-2** | IPC components not exported unless required. | `MainActivity` is exported with `MAIN`/`LAUNCHER` intent filter; `RecoveryVpnService` is protected with `android.permission.BIND_VPN_SERVICE`. All other components unexported (`exported="false"`). | **PASS** |

---

## 5. MobSF (Mobile Security Framework) Automated Scan Steps

To verify the release APK with MobSF locally or in CI:

```bash
# 1. Build the production debug or release APK
./gradlew assembleDebug

# 2. Run MobSF via Docker
docker run -it --rm -p 8000:8000 opensecurity/mobsf:latest

# 3. Upload and scan APK
# Target file: app/build/outputs/apk/debug/app-debug.apk
# Expected result:
# - Security Score: >= 90 / 100
# - High Severity Vulnerabilities: 0
# - Cleartext Traffic: Disabled
# - Trackers Detected: 0
```
