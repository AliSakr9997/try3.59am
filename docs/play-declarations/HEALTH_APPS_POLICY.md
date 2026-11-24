# Google Play Health Apps Policy Declaration

## 1. Non-Medical Device & Educational Disclaimer

**Recovery Companion** is classified under **Health & Fitness / Self-Care** as a supportive habit-change educational companion.

### Regulatory Classification Statement:
> "Recovery Companion is NOT a medical device, Software as a Medical Device (SaMD), or licensed clinical telemedicine service. The application does not provide clinical diagnosis, medical treatment, prescription advice, psychiatric evaluations, or crisis emergency intervention. All therapeutic content provided within the application consists of clinician-approved educational psychoeducation based on Acceptance and Commitment Therapy (ACT) and Cognitive Behavioral Therapy (CBT) principles for voluntary self-help and habit modification."

---

## 2. Mandatory User Warnings Displayed in the Application

The following notice is displayed prominently on the Home Screen, Onboarding Screen, and Coach Screen:

> **بالعربية:**  
> "تطبيق 'رفيق التعافي' هو أداة تعليمية ودعم سلوكي ذاتي، وليس بديلاً عن الطبيب النفسي أو المعالج المتخصص. التطبيق لا يقدم تشخيصات طبية أو أدوية. إذا كنت تشعر بأفكار إيذاء النفس أو أزمة حادة، يرجى التواصل فوراً مع خطوط المساعدة النفسية أو طلب المساعدة الطبية الطارئة."

> **In English:**  
> "Recovery Companion is a self-help educational tool and is not a substitute for professional medical or psychological advice, diagnosis, or treatment. If you are experiencing suicidal thoughts, self-harm impulses, or a psychiatric crisis, please contact your local emergency services or a mental health crisis hotline immediately."

---

## 3. Clinical Content Governance & Provenance

1. **Approved Source Base:** All 105 core therapeutic modules are authored and reviewed by certified behavioral health clinicians specializing in compulsive behavioral recovery.
2. **Cryptographic Sealing:** Content packs are cryptographically signed with Ed25519 digital signatures (`content_pack_manifest.json`) and verified by the application on-device before any module is loaded.
3. **Hard Guardrail Intercepts:** Automated heuristic and pattern guards strictly refuse to answer prompts requesting medication dosages, clinical diagnosis, or self-harm methods.
