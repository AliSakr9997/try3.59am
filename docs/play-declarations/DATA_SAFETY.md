# Google Play Console: Data Safety Form Answers

## Complete Submission Guide for "Data Safety" Section

### Overview
**Recovery Companion is an offline-first, zero-telemetry application.**  
It has **NO backend server**, **NO analytics SDKs**, **NO advertising SDKs**, and **NO cloud account infrastructure**. All personal data (urge logs, check-ins, exercise completions) is stored exclusively on the user's physical device inside an encrypted SQLCipher database.

---

### Question 1: Does your app collect or share any of the required user data types?
- **Answer:** **NO**
  *(Select "No, the app does not collect or share any user data".)*

---

### Detailed Question Walkthrough:

#### 1. Data Collection
- **Location:** NOT COLLECTED
- **Personal Info (Name, Email, Phone, User IDs, Address):** NOT COLLECTED
  *(No user registration or account creation exists in the app).*
- **Financial Info:** NOT COLLECTED
- **Health & Fitness:** NOT COLLECTED
  *(All CBT/ACT check-ins and urge counts are stored locally on-device in encrypted storage and never transmitted to the developer or any third party).*
- **Messages & Emails:** NOT COLLECTED
- **Photos & Videos:** NOT COLLECTED
- **Audio Files:** NOT COLLECTED
- **Files & Docs:** NOT COLLECTED
- **Calendar:** NOT COLLECTED
- **Contacts:** NOT COLLECTED
- **App Activity (App interactions, in-app search, installed apps):** NOT COLLECTED
  *(FTS5 keyword search executes strictly against local SQLite FTS5 index; search terms are never logged or stored).*
- **Web Browsing (Web history):** NOT COLLECTED
  *(DNS queries processed by the local VpnService loopback interface are resolved in memory and discarded; zero browsing history is stored).*
- **App Info & Performance (Crash logs, diagnostics, device performance):** NOT COLLECTED
  *(Zero crash reporting SDKs like Firebase Crashlytics or Sentry are included).*
- **Device or Other IDs:** NOT COLLECTED
  *(No Android Advertising ID, IMEI, or hardware serial numbers are accessed or stored).*

---

### Question 2: Is all user data collected by your app encrypted in transit?
- **Answer:** **N/A (No data is transmitted over the internet).**
  *(If the form requires a selection, select YES, noting that all local storage uses Keystore-backed AES-256 SQLCipher).*

---

### Question 3: Do you provide a way for users to request that their data be deleted?
- **Answer:** **YES**
  *(The user can delete 100% of their local data at any time directly in the app via "Settings -> مسح كل البيانات المحلية (Wipe Local Data)", or by clearing the app data in Android system settings).*
