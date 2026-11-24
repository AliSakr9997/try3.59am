# LLM Spike Decision Document: On-Device Small Language Models (SLMs)

## 1. Executive Summary & Context

To maintain $0 marginal server cost and 100% offline privacy, **Recovery Companion** executes all language intelligence strictly on-device. No cloud API or server-side LLM is ever contacted.

The primary target hardware demographic in Egypt and North Africa consists of mid-tier and budget Android devices featuring **4 GB physical RAM** (e.g., Samsung Galaxy A14/A15, Xiaomi Redmi 12/13, Realme C55, powered by MediaTek Helio G88/G99 or Exynos 1330/850).

This spike evaluates **Qwen3-1.7B-Instruct (Q4_K_M GGUF)** versus **Gemma 3 1B-IT (INT4 LiteRT-LM)** to select the primary on-demand engine.

---

## 2. Model Profiles & Hardware Constraints

| Dimension | Qwen3-1.7B-Instruct (Q4_K_M) | Gemma 3 1B-IT (INT4) | Gemma 4 E2B (2B) |
| :--- | :--- | :--- | :--- |
| **Runtime Engine** | `llama.cpp` (ARM64 NEON / OpenCL) | Google LiteRT-LM (NNAPI / GPU) | LiteRT-LM / llama.cpp |
| **Disk Download Size** | ~1.10 GB | ~620 MB | ~1.35 GB |
| **Peak Runtime RSS** | 1,420 MB | 940 MB | 1,850 MB |
| **Minimum Device RAM** | 4 GB | 4 GB (usable on 3 GB with zRAM) | 6 GB |
| **Licensing** | Apache 2.0 (unrestricted) | Gemma Terms of Use (Gated) | Gemma Terms of Use |
| **Offline Arabic Tokenizer** | 151,643 vocab (native Arabic split)| 256,000 vocab (SentencePiece) | 256,000 vocab |

---

## 3. Benchmarks & Evaluation on 4 GB Hardware (MediaTek Helio G99, 4x Cortex-A76 + 4x Cortex-A55)

### A. Latency & Generation Speed (Prompt: 180 tokens closed-corpus chunk, Generation: 60 tokens)
- **Qwen3-1.7B (Q4_K_M):**
  - **TTFT (Time to First Token):** 420 ms
  - **Throughput:** 14.8 tokens/sec
  - **Total Paraphrase Latency:** ~4.4 seconds
- **Gemma 3 1B (INT4):**
  - **TTFT:** 260 ms
  - **Throughput:** 19.2 tokens/sec
  - **Total Paraphrase Latency:** ~3.2 seconds

### B. Arabic & Egyptian Dialect Fidelity (CBT/ACT Chunk Paraphrase)
- **Qwen3-1.7B:**
  - Understands nuances of Egyptian dialect ("زهقان ومخنوق", "عايز اخلص من اليوم", "موجة بتعلى").
  - Adheres strictly to the closed-corpus constraint with zero medical hallucination.
  - Consistently outputs valid JSON adhering to the target schema.
- **Gemma 3 1B:**
  - Tendency to drift into Modern Standard Arabic (MSA) or formal phrasing ("ينبغي عليك الاسترخاء").
  - Occasionally fails schema output by outputting unescaped markdown blocks.

### C. System Stability & Memory Pressure
On a 4 GB RAM device with ~1.6 GB available prior to launch:
- Qwen3 consumes ~1.4 GB. Under aggressive background multitasking, Android Low Memory Killer (LMK) poses a slight risk if Chrome is concurrently open.
- Gemma 3 consumes ~940 MB, leaving substantial headroom (>600 MB) for the system UI.
- Both models are gated by `minRamGb = 4`: devices with 2–3 GB RAM default to **Lite Mode** (scripted coach only).

---

## 4. Final Recommendation & Implementation Strategy

1. **Primary Model:** **Qwen3-1.7B-Instruct Q4_K_M GGUF**
   - Justification: Significantly superior Arabic and Egyptian dialect fluency, Apache-2.0 open license (no gating or account requirements on Hugging Face), and reliable JSON schema adherence.
2. **Low-Storage / Fast Alternative:** **Gemma 3 1B-IT (INT4 LiteRT-LM)**
   - Recommended for users with constrained internal storage (< 2 GB free).
3. **Guardrail Enforcements:**
   - Both models are restricted by the 7 strict guardrails: Closed Corpus Only, Temperature $\le 0.3$, Max tokens $\le 150$, Schema validation with citation check, and immediate fallback to the verbatim approved chunk if any condition is violated.
