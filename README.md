# رفيق التعافي — Recovery Companion

**Free, Arabic-first, Android-first, on-device recovery companion.**
No server. No account. No tracking. All AI runs on the phone.

> **رفيق التعافي** تطبيق مستقل بذاته، عربي الأول، أندرويد الأول، والخصوصية فيه أساسية مش إضافة.
> مفيش سيرفر، مفيش حساب، مفيش تتبّع — كل الذكاء الاصطناعي بيشتغل على الموبايل نفسه.

**This app is educational and supportive — it is NOT a diagnosis, NOT a treatment, and NOT a substitute for a doctor or therapist.** If you feel you may harm yourself, seek human help immediately.

---

## What it is

A standalone recovery companion for problematic pornography use, built for Egypt and the Arab world:

- **Scripted CBT/ACT coach** — the core is clinician-approved Arabic content (Egyptian phrasing) + offline keyword retrieval (FTS5) with citations. It is **not** a free-text generative therapist.
- **90-second urge-surfing exercise** with coded logging (urge level 0–10, trigger from a fixed list). No free-text journal is stored.
- **6-week CBT/ACT programme** (40 approved content chunks, signed content pack, SHA-256 verified on load).
- **DNS-only blocking** (VpnService) with a curated on-device blocklist + Cloudflare Family (1.1.1.3) upstream, plus a Private DNS setup guide. Blocking is **friction, not a lock** — it slows access down and makes it visible; it cannot make a phone you own unbypassable, and the app says so in the UI.
- **24-hour delay + trusted-person approval** to disable protection — a *social* lock, not a cryptographic one.
- **On-device AI (opt-in, on-demand)** — a small language model only *paraphrases* a retrieved approved chunk (temp ≤ 0.3, ≤ 150 tokens, citation required). Lite mode (no LLM) is the default on 2–3 GB phones. Models are downloaded over Wi-Fi only, from a signed manifest, and are never bundled in the APK.
- **On-device NSFW classification (Tier 1: OpenNSFW2)** — only inside the app's own WebView and for files the user explicitly asks to scan. It cannot see inside Chrome, TikTok, Instagram or Telegram (TLS).

## Privacy model — the non-negotiables

- **$0 server cost, because there is no server.** No Firebase, no analytics, no ads, no crash SDK phoning home.
- **Encrypted local database**: Room + SQLCipher (AES-256). The passphrase is a random 32 bytes stored in EncryptedSharedPreferences backed by the Android Keystore.
- **Coded events only**: urge level, trigger code, exercise id, check-in flag, lapse flag. No journal text, no URLs, no search terms, no images — anywhere in the schema.
- **A lapse is data, not failure**: the streak is never reset to zero in shame. Lapses are logged as coded flags and debriefed (when / where / what were you feeling / what was missing).
- **No credentials in this repo — ever.** See [SECURITY.md](SECURITY.md). `.gitignore` blocks `local.properties`, `*.keystore`, `*.jks`, `google-services.json`, secrets and downloaded model files (`*.gguf`, `*.litertlm`, `*.onnx`, `*.tflite`).

## Tech stack (per the technical plan)

| Layer | Choice |
|---|---|
| Language / UI | Native Kotlin + Jetpack Compose |
| minSdk / targetSdk | 26 / 35 |
| Local DB | Room 2.8.1 + SQLCipher 4.6.1 |
| Secrets | Android Keystore + Tink Encrypted DataStore (migration tracked) |
| LLM runtime | LiteRT-LM (Gemma) / llama.cpp JNI (Qwen) — Phase 1, behind `LlmEngine` |
| LLM model | Qwen3-1.7B Q4_K_M (~1.1 GB, Apache-2.0, ≥ 4 GB RAM) / Gemma 3 1B INT4 (~620 MB) fallback |
| RAG | Offline FTS5 keyword retrieval over the signed content pack (vector RAG is Phase 2) |
| NSFW Tier 1 | OpenNSFW2 (~11 MB, on-demand) |
| Blocking | VpnService (local DNS-only) + Cloudflare Family 1.1.1.3 |
| CI | GitHub Actions — `gradle assembleDebug` (Gradle 8.11.1 via setup-gradle) |

## Project structure

```
app/
  src/main/java/com/recovery/companion/
    MainActivity.kt        # Navigation + all Compose screens (RTL Arabic)
    RecoveryApp.kt         # App holder: DB, coach, protection, AI stubs
    data/                  # Room entities/DAOs, SQLCipher, KeyManager, content pack
    coach/CoachEngine.kt   # Scripted coach + refusal categories
    ai/                    # LlmEngine (stub), ModelManifest (signed, on-demand), NsfwScanner
    blocking/              # Blocklist, RecoveryVpnService (DNS-only), ProtectionManager
    ui/SafeWebView.kt      # In-app browser with domain blocking
  src/main/assets/
    content_pack_v1.json + content_pack_manifest.json  # 40 chunks, SHA-256 verified
    blocklist_v1.txt + blocklist_manifest.json         # starter DNS blocklist
  src/main/AndroidManifest.xml
.github/workflows/android.yml   # CI: build debug APK
```

## Build

Requirements: JDK 17, Android SDK (API 35, build-tools 35), Gradle 8.11.1.

```bash
gradle assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

The Gradle wrapper script (`gradlew`) is included, but the wrapper jar
(`gradle/wrapper/gradle-wrapper.jar`) is intentionally not committed yet:
generate it once on a machine with normal Maven Central access with
`gradle wrapper --gradle-version 8.11.1`, then `./gradlew assembleDebug`
works as usual. GitHub Actions does not need it — CI installs Gradle 8.11.1
directly (see `.github/workflows/android.yml`).

Install on a phone with `adb install app/build/outputs/apk/debug/app-debug.apk`.
The app works fully in airplane mode (Lite mode). The VPN filter and the
optional model download need network; the model download is Wi-Fi-only.

## Honest limits (read before you promise anything)

- No blocker, app, or programme has proven long-term effectiveness for
  problematic pornography use; CBT/ACT psychotherapy has the strongest
  (still limited) evidence. This app delivers CBT/ACT *skills*, it does not
  claim to cure anything.
- Blocking on stock Android is bypassable by a determined device owner.
  The only truly unremovable layer (Device Owner) needs a PC, risks
  permanent lockout, and is **not** the default — it is a Phase 3 opt-in.
- No on-device NSFW classifier is 95%+ accurate on real-world content.
  Thresholds here are tuned for recall; false positives get an appeal
  flow that logs nothing.
- The therapeutic content is educational material, not clinical advice.
  It must be reviewed by Egyptian clinicians before any public launch,
  and the app must get Egyptian legal advice (Personal Data Protection
  Law 151/2020; health data is sensitive data even when processed
  on-device).

## Roadmap

- **Phase 0 (this repo now):** foundations — Compose UI, encrypted DB, scripted coach + FTS5, DNS blocker scaffold, model manifest, CI.
- **Phase 1 (MVP):** wire LiteRT-LM / llama.cpp behind `LlmEngine` after a spike on 3 low-end devices; NSFW Tier 1 in the WebView; local peer pairing (QR/code) + 24h disable flow; MASVS L1 audit; Play Store + sideload APK.
- **Phase 2:** vector RAG (only if FTS5 is measurably worse), dumb-relay peer alerts (sealed boxes, FCM data-only), content pack v2.
- **Phase 3:** Device Owner "Hard Mode" (opt-in, with a safety review), on-device video pre-scan, iOS re-evaluation.

## Contributing

Contributions are welcome — especially Arabic content review by clinicians,
low-end-device testing in Egypt, and security review. Please read
[SECURITY.md](SECURITY.md) and [PRIVACY.md](PRIVACY.md) first. Do not submit
real pornographic material in any test set; use licensed/synthetic data only.

## License

Apache License 2.0 — see [LICENSE](LICENSE).
