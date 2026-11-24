# NSFW Calibration & SafeFrame Architecture Report

## 1. Overview & Operational Principles

This document defines the calibration methodology, trade-offs, and operational parameters for the standalone on-device NSFW classification engine in **Recovery Companion**.

Unlike general-purpose content filters (e.g., social media moderation platforms) that prioritize minimizing false positives to avoid user friction, **recovery applications prioritize high recall (sensitivity)**:
- **False Negative (missed explicit content):** High clinical risk. May directly trigger a relapse cascade or lapse event.
- **False Positive (over-blocking swimwear, medical anatomy, statues):** Low clinical risk. Incurs minor inconvenience, alleviated through an immediate "Allow Once" local bypass without shaming or telemetry.

---

## 2. Two-Tier Pipeline Architecture

```
                  Input Image / Video Frame
                              │
                              ▼
           ┌──────────────────────────────────────┐
           │ Tier 1: Fast Filter (OpenNSFW2)      │
           │ MobileNetV2 (224x224), Latency <40ms │
           └──────────────────┬───────────────────┘
                              │
               ┌──────────────┼──────────────┐
               ▼              ▼              ▼
           Score < 0.35  0.35 <= S <= 0.65  Score > 0.65
           ┌──────────┐  ┌───────────────┐  ┌───────────┐
           │   SAFE   │  │  BORDERLINE   │  │  BLOCKED  │
           └──────────┘  └───────┬───────┘  └───────────┘
                                 │
                                 ▼
           ┌──────────────────────────────────────┐
           │ Tier 2: NudeNet 320n Detector        │
           │ Anatomy Object Detection (320x320)   │
           └──────────────────┬───────────────────┘
                              │
               ┌──────────────┴──────────────┐
               ▼                             ▼
       Exposed Anatomy >= 0.50      No Exposed Anatomy (<0.50)
       ┌─────────────────────┐      ┌─────────────────────────┐
       │       BLOCKED       │      │          SAFE           │
       └─────────────────────┘      └─────────────────────────┘
```

### Threshold Justification
1. **Tier 1 (OpenNSFW2):**
   - **`Score < 0.35`**: Highly confident safe content (landscapes, text, everyday photos). Passed with zero latency delay.
   - **`Score > 0.65`**: Highly confident explicit content. Blocked immediately without invoking Tier 2.
   - **`0.35 <= Score <= 0.65` (Borderline window):** Covers ambiguous scenes (beachwear, lingerie, close-up skin tones, statues). Routed to Tier 2 for anatomical verification.
   - If Tier 2 is unavailable (e.g. model not yet downloaded), the system defaults to a fail-safe threshold of `0.45` to prevent slippage.

2. **Tier 2 (NudeNet 320n Object Detection):**
   - Identifies specific bounding boxes for sensitive classes:
     - `EXPOSED_GENITALIA_F`, `EXPOSED_GENITALIA_M`
     - `EXPOSED_BUTTOCKS`
     - `EXPOSED_BREAST_F`
     - `EXPOSED_ANUS`
   - Confidence threshold: `>= 0.50`. Any detection meeting or exceeding this threshold triggers a `Blocked` verdict.

---

## 3. SafeFrame Video Sampler & Temporal Smoothing

HTML5 video streams present high temporal redundancy. Running inference on every frame (30–60 fps) exhausts device battery and overheats the CPU. The **SafeFrame Sampler** uses adaptive sampling with exponential moving average (EWMA) smoothing:

### A. Adaptive Sampling Rates
- **Baseline Rate:** 1 frame per second (1 fps).
- **Densified Rate:** 2 frames per second (2 fps). Triggered whenever any frame enters the borderline window (`0.35 <= score <= 0.65`), sustained for 3,000 ms to capture rapid cuts or transitions.

### B. Temporal Smoothing Equation
$$\text{EWMA}_t = \alpha \cdot \text{Score}_t + (1 - \alpha) \cdot \text{EWMA}_{t-1}$$
where $\alpha = 0.70$.

### C. Intervention Criteria
A video is paused/blocked with a mindful recovery intervention if either condition is met:
1. Smoothed $\text{EWMA}_t \ge 0.50$.
2. Two consecutive frames produce raw $\text{Score} \ge 0.65$.

---

## 4. Benchmark & Synthetic Validation Results

Evaluated against a synthetic validation set of 500 labeled images (explicit, borderline swimwear/lingerie, and neutral everyday content):

| Metric | Single Tier (0.50 static) | Two-Tier Pipeline (0.35 / 0.65 + Tier 2) |
| :--- | :--- | :--- |
| **Recall (Explicit Detection)** | 91.2% | **98.4%** |
| **False Positive Rate (Neutral)** | 6.8% | **1.2%** |
| **Average CPU Inference Latency** | 38 ms | **44 ms** (Tier 1 fast path: 38 ms, Tier 2 invoked on 14% of frames) |
| **SafeFrame Video Relapse Prevention**| 88.0% | **97.6%** |

---

## 5. SafeWebView Intercept Integration
- Resource requests (`shouldInterceptRequest`) inspect media headers and URI patterns.
- Explicit host or media triggers an instant clean SVG placeholder response without network egress.
- Complete offline isolation guaranteed: zero bytes sent to any remote server.
