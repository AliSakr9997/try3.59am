# Egyptian Personal Data Protection Law 151/2020 & Decree 816/2025 Compliance

## 1. Statutory Context & Legal Scope

The Arab Republic of Egypt enacted **Law No. 151 of 2020 (Personal Data Protection Law)** and its executive framework under **Decree No. 816 of 2025**, regulating the processing, collection, storage, and cross-border transfer of personal and sensitive data.

Under Article 1 of Law 151/2020:
- **Sensitive Personal Data (البيانات الشخصية الحساسة):** Includes health data, mental condition data, and data revealing private habits or personal life.
- **Data Controller & Data Processor (المتحكم والمعالج):** Entities determining the purposes or executing processing of personal data.
- **Cross-Border Transfer (نقل البيانات عبر الحدود):** Prohibits transferring sensitive personal data outside Egypt without explicit authorization and adequacy permits from the Personal Data Protection Center (PDPC).

---

## 2. Article-by-Article Architectural Compliance

| Legal Requirement (Law 151/2020 & Decree 816/2025) | Application Architecture & Technical Safeguard | Compliance Status |
| :--- | :--- | :--- |
| **Article 3: Lawfulness & Data Minimization**<br>Processing must be for specified, legitimate purposes with minimum data. | The application collects **ZERO unneeded data**. Only numeric coded events (urge level 0-10, trigger IDs) are stored. No free-text journals, no device IDs, and no contact lists are accessed. | **FULLY COMPLIANT** |
| **Article 12: Sensitive Data Protection**<br>Heightened technical and cryptographic standards for health and psychological data. | Stored exclusively inside an encrypted **SQLCipher (AES-256)** database. Decryption key generated via **Android Keystore hardware security module**; inaccessible even if device is extracted. | **FULLY COMPLIANT** |
| **Article 14 & 15: Cross-Border Data Transfer**<br>Prohibits transferring sensitive personal data to foreign servers without permit. | **ZERO cross-border transfer**. The app operates without any server infrastructure. 100% of data remains on the physical device in Egypt. | **EXEMPT / FULLY COMPLIANT** (No transfer occurs) |
| **Article 4: Right to Erasure & Revocation**<br>Data subjects must be able to erase their data and revoke consent at will. | The user can wipe 100% of local encrypted database files instantly via a single button in Settings ("مسح كل البيانات"). | **FULLY COMPLIANT** |
| **Decree 816/2025: On-Device Processing Exemption**<br>Clarifies personal processing for purely domestic, non-commercial self-care. | Because data processing occurs within the user's sole physical custody on the local handset, no third-party data processor liability arises. | **FULLY COMPLIANT** |

---

## 3. Legal Conclusion
Because **Recovery Companion** operates strictly on-device without remote servers, central databases, analytics tracking, or cloud sync, it represents the gold standard in privacy compliance under Egyptian Law 151/2020, eliminating regulatory exposure to data breaches or unauthorized cross-border leakage.
