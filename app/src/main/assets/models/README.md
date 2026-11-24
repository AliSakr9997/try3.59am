# On-device models (downloaded on demand, never committed)

Large model files are NOT stored in this repo. The app downloads them
on-demand, over HTTPS, on Wi-Fi only, and refuses to load a file unless
its SHA-256 matches the pinned value in
`app/src/main/java/com/recovery/companion/ai/ModelManifest.kt`.

- Qwen3-1.7B Q4_K_M (~1.1 GB) — primary SLM, Apache-2.0, >= 4 GB RAM
- Gemma 3 1B INT4 (~620 MB) — fallback, Gemma licence (gated)
- OpenNSFW2 (~11 MB) — NSFW Tier 1, in-app WebView / gallery only

This directory stays empty in git on purpose (see .gitignore).
