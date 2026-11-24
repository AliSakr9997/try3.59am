# NSFW Calibration & SafeFrame Architecture Report

See detailed technical report at [CALIBRATION_REPORT.md](file:///E:/Vli/PROJECTS/3.59/tests/nsfw-calibration/CALIBRATION_REPORT.md).

## Summary:
- **Two-Tier Model**: OpenNSFW2 MobileNetV2 (Tier 1) + NudeNet 320n (Tier 2).
- **Thresholds**:
  - Safe: `< 0.35`
  - Borderline / Routed to Tier 2: `0.35 .. 0.65`
  - Blocked: `> 0.65`
  - Tier 2 Anatomical Confidence: `0.50`
- **SafeFrame Sampler**:
  - 1 fps baseline, 2 fps on borderline.
  - EWMA smoothing $\alpha = 0.70$.
  - Intervene when $\text{EWMA} \ge 0.50$ or 2 consecutive frames $\ge 0.65$.
