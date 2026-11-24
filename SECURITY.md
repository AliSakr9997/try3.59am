# Security policy

## No credentials in this repo — ever

This is a **public** repository for a sensitive health app. The following
must never be committed, and `.gitignore` blocks them:

- `local.properties`, `*.keystore`, `*.jks`, `*.p12`, `*.pem`, `*.key`
- `google-services.json` (there is no Firebase in this app — do not add it)
- `secrets.properties`, `*.env`, `keystore.properties`
- Downloaded on-device model files (`*.gguf`, `*.litertlm`, `*.task`,
  `*.onnx`, `*.tflite`) — models are downloaded on-demand by the app from
  the signed manifest, never stored in git.

Before every push, check `git status` for anything that looks like a
secret. If a secret is ever committed by mistake, rotate it immediately
and rewrite history — do not just delete the file in a later commit.

## How secrets are handled in the app

- The SQLCipher passphrase is a random 32 bytes generated on first run
  and stored in EncryptedSharedPreferences backed by the Android Keystore.
  It never appears in code, logs, or the repo.
- Model/content downloads verify SHA-256 against the pinned manifest
  (`ModelManifest.kt`, `content_pack_manifest.json`,
  `blocklist_manifest.json`) before loading. A mismatch refuses the file.
- No analytics, ads, or crash SDKs. The only network the app makes is an
  optional, Wi-Fi-only, signed model/content download over HTTPS.

## Reporting a vulnerability

If you find a security issue (especially anything that could expose user
data — this app handles sensitive health behaviour in a context where a
leak can mean family, employment, or legal harm in Egypt), please report
it privately to the repository owner instead of opening a public issue.
Do not include any real user data in a report.

## Pre-launch audit checklist (from the technical plan, §6.6)

- [ ] OWASP MASVS L1 (L2 for crypto/storage items)
- [ ] MobSF static scan: no `allowBackup` on the encrypted DB, no
      `debuggable`, no exported components leaking data
- [ ] Dynamic test on a rooted device: dump the DB file, confirm ciphertext
- [ ] Network proxy test: zero requests except signed model/content download
- [ ] NSFW: no image bytes stored or transmitted — score/boolean only
- [ ] LLM: prompts not logged, model unloaded on background
