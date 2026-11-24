# Google Play Console: VpnService Declaration & Video Script

## 1. Google Play Console Declaration Form Answers

### Core Question: Why does your application use `android.net.VpnService`?
- **Primary Category Selected:** Parental Control / Security Application
- **Declaration Statement:**
  > "Recovery Companion uses Android VpnService solely to establish a local, on-device loopback DNS filter interface (10.0.0.2). This loopback interface intercepts DNS queries to block access to adult and pornography domains based on a user-curated blocklist and forwards allowed queries to the privacy-preserving Cloudflare Family DNS resolver (1.1.1.3). The VPN interface handles only local UDP DNS packets. It does NOT route or inspect general IP traffic, does NOT decrypt HTTPS traffic, does NOT establish any remote VPN tunnel, and logs zero user network activity to any remote server."

### Required Disclosures:
- **Does the app route user traffic through a remote server?** NO.
- **Does the app collect personal or sensitive user data via the VPN?** NO.
- **Is the VPN service user-facing and prominent in the UI?** YES (dedicated "Protection" screen with explicit toggle, status indicator, and clear documentation of capabilities and limitations).

---

## 2. 90-Second Demonstration Video Script (Required for Play Console Review)

**Target Duration:** 80–90 seconds  
**Visual Style:** Screen recording of actual Android device with English and Arabic subtitles.

---

### [00:00 - 00:15] Section 1: In-App User Disclosure & Consent
- **Action:** User launches Recovery Companion, taps on the "الحماية (Protection)" navigation button.
- **Screen Display:** Dedicated Protection Screen displaying the explicit disclaimer:
  *"الحجب هنا احتكاك محلي مشفر لمساعدتك في لحظات الضعف — يعمل كفلتر DNS محلي فقط بدون أي سيرفرات وسيطة."*
- **Voiceover / Caption:**
  *"Recovery Companion provides an optional local DNS filter to assist users overcoming compulsive pornography habits. Protection is fully controlled by the user."*

---

### [00:15 - 00:35] Section 2: System VPN Permission Dialog
- **Action:** User taps the "شغّل الحماية (Enable Protection)" button. The standard Android system VPN consent dialog appears (`android.net.VpnService.prepare`).
- **Screen Display:** System dialog showing: *"Recovery Companion wants to set up a VPN connection that allows it to monitor network traffic..."*
- **Action:** User taps "OK" to grant consent.
- **Voiceover / Caption:**
  *"Upon user request, the application requests standard Android VpnService permission to initialize a local virtual network interface on the device."*

---

### [00:35 - 00:55] Section 3: Foreground Notification & Active State
- **Action:** Notification shade is pulled down.
- **Screen Display:** Ongoing system foreground notification with a shield icon:
  *"رفيق التعافي — الحماية شغالة (Local DNS Filter Active)"*.
- **Voiceover / Caption:**
  *"A persistent foreground notification is immediately displayed while protection is active, ensuring continuous user awareness as required by Google Play policy."*

---

### [00:55 - 01:15] Section 4: Local Filtering Demonstration
- **Action:** User opens a browser and attempts to navigate to an explicit domain (e.g. `pornhub.com`).
- **Screen Display:** The request fails instantly with an `NXDOMAIN` (domain blocked locally) or redirects to a soothing mindful intervention without loading explicit media.
- **Action:** User navigates to a benign educational site (e.g. `wikipedia.org`). The page loads normally via Cloudflare Family upstream (1.1.1.3).
- **Voiceover / Caption:**
  *"DNS queries are matched against the on-device curated blocklist. Adult domains are blocked locally, while neutral traffic resolves directly without remote rerouting."*

---

### [01:15 - 01:30] Section 5: Transparent Disablement & Social Friction Lock
- **Action:** User returns to the app and reviews the disable procedure (24-hour mindful delay and trusted peer approval code).
- **Voiceover / Caption:**
  *"To prevent impulsive disabling during urges, a 24-hour delay is enforced. Zero data is shared or transmitted externally throughout the entire lifecycle."*
