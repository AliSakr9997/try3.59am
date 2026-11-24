<div align="center">

# رفيق التعافي — Recovery Companion

**Free, Arabic-first, Android-first, on-device recovery companion.**  
*Zero server. Zero cloud account. Zero telemetry. All intelligence runs 100% on-device.*

<p align="center">
  <a href="#english"><strong>English</strong></a> •
  <a href="#العربية"><strong>العربية</strong></a>
</p>

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-green.svg)](app)
[![Tests](https://img.shields.io/badge/Unit%20Tests-44%2F44%20Passing-brightgreen.svg)](app/src/test)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20On--Device%20(Zero%20Servers)-orange.svg)](PRIVACY.md)

</div>

---

<a name="english"></a>
## English

### Overview & Philosophy
**Recovery Companion** is an open-source, standalone mobile application engineered to assist individuals overcoming compulsive pornography use. Designed primarily for Egypt and the Arab world, the app is built on an uncompromising **offline-first, zero-telemetry architecture**:
- **$0 Marginal Server Cost**: No remote servers, no central databases, and no cloud accounts.
- **100% Local Intelligence**: Psychoeducation search, content verification, NSFW detection, and language paraphrasing execute strictly on the device hardware.
- **Flight-Mode Ready**: Fully functional in Airplane Mode with zero internet access required for core therapeutic features.

> [!IMPORTANT]
> **Medical & Educational Disclaimer:**  
> Recovery Companion is a supportive self-care tool based on Acceptance and Commitment Therapy (ACT) and Cognitive Behavioral Therapy (CBT). **It is NOT a medical device, does not provide clinical diagnosis or pharmaceutical prescriptions, and is not a substitute for a licensed mental health professional.** If you are experiencing suicidal thoughts or severe psychiatric crisis, please contact emergency services immediately.

---

### Key Modules & Capabilities

1. **Scripted CBT/ACT Coach**:
   - **105 Clinician-Approved Modules**: Structured in Egyptian Arabic and simplified MSA covering 6-week curriculum reviews, urge surfing, trigger deep-dives (boredom, loneliness, stress, late night), sleep hygiene (CBT-I), and HALT emotional regulation.
   - **Offline FTS5 Retrieval**: High-speed keyword search over encrypted tables without arbitrary text generation.
   - **Hard Guardrail Refusals**: Instant compassionate refusals for self-harm, diagnosis, medication requests, and sexual content queries.
   - **Shame-Free Lapse Debrief**: Coded context logging (`alone_screen`, `late_night`, `stress_relief`, `autopilot`, `boredom`) with immediate actionable recovery steps to mitigate the *Chaser Effect*.

2. **Local DNS-only VpnService**:
   - **Curated Blocklist**: 100+ high-traffic adult and proxy domains verified via SHA-256 in [blocklist_manifest.json](app/src/main/assets/blocklist_manifest.json).
   - **$O(k)$ Suffix Matcher**: Subdomains are blocked automatically with zero linear scanning overhead.
   - **Cloudflare Family Upstream (1.1.1.3)**: Resolves permitted DNS queries while filtering adult content at the upstream level.
   - **Social Friction Lock**: 24-hour mandatory delay + 6-digit peer approval code required to disable protection, featuring active tamper detection.

3. **Cryptographic Content Pipeline (Ed25519)**:
   - Build-time signing tool [sign_pack.py](content-packs/tools/sign_pack.py) signs content packs with an Ed25519 clinical authority key.
   - On-device verifier [ContentPackVerifier.kt](app/src/main/java/com/recovery/companion/data/ContentPackVerifier.kt) uses Google Tink to ensure zero unapproved or modified therapeutic content can ever be seeded into the database.

4. **On-Device NSFW Scanner & SafeFrame**:
   - **Two-Tier Pipeline**: OpenNSFW2 MobileNetV2 Tier 1 (< 0.35 Safe, > 0.65 Blocked) routing borderline images (0.35–0.65) to NudeNet 320n anatomy detection.
   - **SafeFrame Video Sampler**: 1 fps baseline sampling, adaptive densification to 2 fps on borderline scenes, and EWMA smoothing ($\alpha = 0.70$) for real-time video pause interventions.
   - **SafeWebView**: In-app browser intercepts image resources and replaces explicit content with calming mindful SVG placeholders.

5. **Peer Accountability v1 (Zero-Server)**:
   - Asymmetric Ed25519 keypair generation for offline QR code pairing.
   - Weekly summary generation featuring strictly coded metrics (practice days, check-in count, surfing count, protection status) with zero free-text journals, zero search terms, and zero web history.
   - Cryptographically signed mutual unpair messages.

6. **On-Demand SLM Engine & The 7 Guardrail Rules**:
   - Gated for devices with $\ge 4\text{ GB}$ RAM (Qwen3-1.7B Q4_K_M GGUF / Gemma 3 1B INT4); 2–3 GB RAM devices default to Lite Mode.
   - **7 Strict Rules**: Closed corpus only, Egyptian Arabic system prompt, temperature $\le 0.3$, max tokens $\le 150$, strict JSON schema (`exercise_id`, `paraphrase`, `source_ids`), citation verification, and instant fallback to verbatim approved chunks if any rule is violated.
   - [ModelDownloadManager.kt](app/src/main/java/com/recovery/companion/ai/ModelDownloadManager.kt) enforces Wi-Fi-only downloading, RAM verification, and SHA-256 checksum integrity.

---

### How to Install

#### System Requirements
- **Operating System:** Android 8.0 (API Level 26) or higher.
- **Memory (RAM):**
  - **Lite Mode (Scripted Coach, FTS5 RAG, DNS Blocker):** 2 GB to 3 GB RAM.
  - **Full Mode (Optional On-Demand 1.7B SLM):** 4 GB RAM or higher.
- **Storage:** ~45 MB for initial install (additional ~1.1 GB only if optional local SLM is downloaded).

---

#### Method 1: Direct APK Sideload (Recommended for Users)
1. Download the latest `recovery-companion-v1.0.0.apk` (or `app-debug.apk`) from the [Releases](https://github.com/AliSakr9997/try3.59am/releases) page or build it locally.
2. Transfer the APK to your Android device via USB, messaging app, or browser download.
3. Open the downloaded file using your phone's File Manager.
4. When prompted by Android:
   - Tap **Settings** and enable **Allow from this source** (تثبيت التطبيقات غير المعروفة).
5. Tap **Install** and open **رفيق التعافي**.
6. The app is ready to use immediately in complete offline mode.

---

#### Method 2: Install via ADB (Developers & Testers)
Connect your Android handset with **USB Debugging** enabled in Developer Options:

```bash
# Check connected device
adb devices

# Install APK directly
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

#### Method 3: Build from Source
To compile the APK yourself using Gradle:

```bash
# 1. Clone the repository
git clone https://github.com/AliSakr9997/try3.59am.git
cd try3.59am

# 2. Run all 44 unit tests (100% passing)
./gradlew testDebugUnitTest       # Linux / macOS
.\gradlew.bat testDebugUnitTest   # Windows PowerShell

# 3. Assemble the Debug APK
./gradlew assembleDebug           # Linux / macOS
.\gradlew.bat assembleDebug       # Windows PowerShell

# The resulting APK is located at:
# app/build/outputs/apk/debug/app-debug.apk
```

---

### Privacy & Security Verification

- **OWASP MASVS L1**: Fully audited against Mobile Application Security Verification Standard Level 1. Full checklist documented in [MASVS_L1_CHECKLIST.md](docs/security/MASVS_L1_CHECKLIST.md).
- **Egyptian Law 151/2020 Compliance**: Comprehensive statutory legal assessment detailed in [EGYPT_LAW_151_2020.md](docs/legal/EGYPT_LAW_151_2020.md).
- **Google Play Console Declarations**:
  - [VPNSERVICE_DECLARATION.md](docs/play-declarations/VPNSERVICE_DECLARATION.md) (90-second video review script).
  - [DATA_SAFETY.md](docs/play-declarations/DATA_SAFETY.md) (Exact Play Console answers: zero data collected, zero shared).
  - [HEALTH_APPS_POLICY.md](docs/play-declarations/HEALTH_APPS_POLICY.md) (Medical disclaimer & clinical provenance).
  - [PRIVACY_POLICY_PLAY.md](docs/play-declarations/PRIVACY_POLICY_PLAY.md) (Bilingual official privacy policy).

---

<br />

---

<a name="العربية"></a>
## العربية

### المقدمة والرسالة
**رفيق التعافي** تطبيق مجاني ومفتوح المصدر، مُصمم كرفيق سلوكي داعم للأفراد المتعافين من السلوكيات الإباحية القهرية في مصر والعالم العربي.
يرتكز التطبيق بالكامل على **معمارية محلية دون أي خوادم وسيطة (Zero-Server, Offline-First)**:
- **انعدام تكلفة التشغيل ($0 Marginal Cost)**: لا توجد أي خوادم سحابية، ولا اشتراكات مدفوعة، ولا إعلانات.
- **ذكاء اصطناعي محلي 100%**: جميع أدوات البحث، وتصنيف الصور، ونماذج التلخيص اللغوية تعمل داخل معالج الهاتف محلياً.
- **جاهزية تامة في وضع الطيران**: التطبيق يعمل بكفاءة تامة دون الحاجة لأي اتصال بالإنترنت في ميزاته السلوكية الأساسية.

> [!IMPORTANT]
> **إخلاء مسؤولية طبي وتعليمي:**  
> رفيق التعافي هو أداة دعم سلوكي وتعليم ذاتي مبنية على العلاج المعرفي السلوكي (CBT) وعلاج القبول والالتزام (ACT). **التطبيق ليس أداة تشخيص طبي، ولا يقدم وصفات دوائية، ولا يُعد بديلاً عن استشارة الطبيب النفسي أو المعالج المتخصص.** في حال شعورك بأي أفكار لإيذاء النفس أو أزمة طارئة، يرجى التوجه فوراً لطلب المساعدة الطبية أو الاتصال بخطوط نجدة الطوارئ في بلدك.

---

### الميزات والمنظومة التقنية

1. **المدرب السلوكي المقيد بالمحتوى المعتمد**:
   - **105 مقتطفات علاجية معتمدة**: صُممت بالعامية المصرية والفصحى المبسطة لتغطية مراجعات الأسابيع الستة، والتعامل مع المحفزات (الملل، العزلة، الضغوط، الليل)، وقواعد تنظيم النوم (CBT-I)، والوعي الانفعالي (HALT).
   - **استرجاع محلي عبر FTS5**: بحث فوري داخل قاعدة البيانات المشفرة دون تأليف أو هلوسة علاجية.
   - **حواجز منع ورفض صارمة**: اعتراض فوري لأي أسئلة تتعلق بالتشخيص، أو الأدوية، أو إيذاء النفس، أو طلب المحتوى الإباحي.
   - **مراجعة واعية للانتكاسة بدون خزي**: تسجيل سياق التعثر عبر وسوم رقمية مشفرة (`alone_screen`, `late_night`, `stress_relief`, `autopilot`, `boredom`) وعرض خطوات فورية لتفادي الانتكاس المتتابع.

2. **منظومة الحجب المحلي عبر DNS (VpnService)**:
   - **قائمة حجب منسقة**: أكثر من 100 نطاق إباحي وبروكسي مفحوصة بتجزئة SHA-256 في [blocklist_manifest.json](app/src/main/assets/blocklist_manifest.json).
   - **مطابقة نطاقات فائقة السرعة**: خوارزمية هرمية $O(k)$ تحجب النطاقات الفرعية تلقائياً.
   - **خادم Cloudflare Family (1.1.1.3)**: توجيه استعلامات DNS المسموحة مباشرة لخوادم Cloudflare لتنقية المحتوى على مستوى الشبكة.
   - **قفل اجتماعي**: مهلة 24 ساعة + كود موافقة من 6 أرقام لشخص موثوق لتعطيل الحماية، مع كشف محاولات الإيقاف القسري (Tamper Detection).

3. **خط إنتاج وتوقيع المحتوى (Ed25519)**:
   - أداة بايثون [sign_pack.py](content-packs/tools/sign_pack.py) لتوقيع المحتوى بمفتاح Ed25519 المعتمد.
   - محرك تحقق كوتلن [ContentPackVerifier.kt](app/src/main/java/com/recovery/companion/data/ContentPackVerifier.kt) باستخدام Google Tink لرفض أي محتوى غير موثق.

4. **تصنيف وفحص الصور محلياً ومحلل SafeFrame**:
   - **معمارية ثنائية الطبقات**: الطبقة الأولى (OpenNSFW2) لفحص فوري سريع (< 40ms)، والطبقة الثانية (NudeNet 320n) لفحص التشريح الحساس في الحالات الرمادية (0.35 - 0.65).
   - **محلل الفيديو SafeFrame**: استهلاك بطارية منخفض عبر أخذ عينة إطار واحد في الثانية (1 fps)، يرتفع إلى 2 fps عند المشاهد الحرجة مع تنعيم النتائج عبر المتوسط المتحرك الأسي (EWMA $\alpha=0.70$).
   - **متصفح آمن (SafeWebView)**: اعتراض موارد الوسائط وحجب المواقع المحظورة واستبدالها بشاشات توعوية هادئة (SVG).

5. **المسؤولية المشتركة مع صديق موثوق (بدون خوادم)**:
   - اقتران محلي عبر رموز QR بدون أي خوادم وسيطة.
   - تقارير أسبوعية مشفرة وموقعة تتضمن أرقاماً مجردة فقط (أيام الالتزام، التمارين) بدون أي نصوص أو مذكرات أو تاريخ تصفح.
   - رسائل فك اقتران موقعة رقمياً.

6. **النماذج اللغوية المحلية وفق 7 قواعد أمان صارمة**:
   - دعم الأجهزة بذاكرة $\ge 4\text{ GB}$ (نموذج Qwen3-1.7B Q4_K_M GGUF أو Gemma 3 1B INT4)؛ الأجهزة بذاكرة 2-3 جيجابايت تعمل تلقائياً في "الوضع الخفيف".
   - **قواعد الأمان السبع**: متن مغلق حصرياً، توجيه بالعامية المصرية، محددات دقيقة (حرارة $\le 0.3$، رمزيات $\le 150$)، مخرجات JSON صارمة مع التوثيق المصدري، والتراجع التلقائي للنص المعتمد حرفياً عند أي خطأ.
   - مدير التحميل [ModelDownloadManager.kt](app/src/main/java/com/recovery/companion/ai/ModelDownloadManager.kt) يضمن التحميل عبر Wi-Fi فقط والتحقق من تجزئة SHA-256.

---

### كيفية التثبيت (How to Install)

#### متطلبات التشغيل
- **نظام التشغيل:** أندرويد 8.0 (أوريو - API 26) أو أحدث.
- **الذاكرة العشوائية (RAM):**
  - **الوضع الخفيف (المدرب السلوكي، الحجب، المتابعة):** 2 إلى 3 جيجابايت.
  - **الوضع الكامل (النموذج اللغوي المحلي الاختياري):** 4 جيجابايت فما فوق.
- **المساحة التخزينية:** ~45 ميجابايت للتثبيت الأولي.

---

#### الطريقة الأولى: التثبيت المباشر عبر ملف APK (موصى بها للمستخدمين)
1. حمّل أحدث إصدار من ملف `recovery-companion-v1.0.0.apk` (أو `app-debug.apk`) من صفحة [الإصدارات (Releases)](https://github.com/AliSakr9997/try3.59am/releases).
2. افتح الملف المحمّل من خلال مدير الملفات على هاتفك.
3. إذا ظهرت لك رسالة أمان أندرويد:
   - اضغط على **الإعدادات (Settings)** ثم فعّل خيار **السماح بتثبيت التطبيقات من هذا المصدر (Allow from this source)**.
4. اضغط على **تثبيت (Install)** ثم افتح تطبيق **رفيق التعافي**.
5. سيبدأ التطبيق فوراً في العمل بكامل طاقته ومحلياً دون الحاجة لإنشاء أي حساب.

---

#### الطريقة الثانية: التثبيت عبر ADB (للمطورين والفاحصين)
وصّل هاتفك بالكمبيوتر مع تفعيل **تصحيح أخطاء USB (USB Debugging)**:

```bash
# التأكد من اتصال الهاتف
adb devices

# تثبيت الحزمة مباشرة
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

#### الطريقة الثالثة: البناء من المصدر (Build from Source)
إذا أردت فحص وتجميع الشفرة المصدرية بنفسك باستخدام أداة Gradle:

```bash
# 1. استنساخ المستودع
git clone https://github.com/AliSakr9997/try3.59am.git
cd try3.59am

# 2. تشغيل جميع اختبارات الوحدة (44 اختباراً بنجاح 100%)
.\gradlew.bat testDebugUnitTest    # نظام ويندوز
./gradlew testDebugUnitTest        # نظام لينكس / ماك

# 3. بناء حزمة التطبيق للتجربة
.\gradlew.bat assembleDebug        # نظام ويندوز
./gradlew assembleDebug            # نظام لينكس / ماك

# ستجد ملف الـ APK الناتج في المسار التالي:
# app/build/outputs/apk/debug/app-debug.apk
```

---

### الأمان والامتثال القانوني

- **معايير OWASP MASVS L1**: تدقيق أمان كامل وفق المستوى الأول؛ انظر القائمة في [MASVS_L1_CHECKLIST.md](docs/security/MASVS_L1_CHECKLIST.md).
- **قانون حماية البيانات المصري 151/2020**: توافق كامل نتيجة انعدام الخوادم ونقل البيانات خارج مصر؛ انظر [EGYPT_LAW_151_2020.md](docs/legal/EGYPT_LAW_151_2020.md).
- **وثائق متجر جوجل بلاي**:
  - [VPNSERVICE_DECLARATION.md](docs/play-declarations/VPNSERVICE_DECLARATION.md) (سكريبت فيديو المراجعة).
  - [DATA_SAFETY.md](docs/play-declarations/DATA_SAFETY.md) (إجابات نموذج أمان البيانات).
  - [HEALTH_APPS_POLICY.md](docs/play-declarations/HEALTH_APPS_POLICY.md) (إخلاء المسؤولية الصحية).
  - [PRIVACY_POLICY_PLAY.md](docs/play-declarations/PRIVACY_POLICY_PLAY.md) (سياسة الخصوصية الرسمية).

---

## 📜 الترخيص (License)
مرخص بموجب رخصة أباتشي 2.0 (Apache License 2.0) — انظر ملف [LICENSE](LICENSE) للمزيد من التفاصيل.
