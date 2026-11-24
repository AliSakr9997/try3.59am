# Privacy policy (summary — get Egyptian legal advice before launch)

## The short version

**Nothing leaves your phone.** There is no server, no account, no analytics,
no ads, and no tracking. The app works fully in airplane mode.

## What is stored, and where

All data is stored in an encrypted database (Room + SQLCipher, AES-256) on
your device. The encryption passphrase is a random 32 bytes held in the
Android Keystore. Data stored:

| Data | Form |
|---|---|
| Urge events | Coded only: urge level 0–10, trigger code (fixed list), exercise id, completed flag, day |
| Daily check-ins | Date, check-in flag, lapse flag, practice minutes |
| Disable requests | Timestamp, 6-digit peer code, approved/executed flags |
| Content progress | Which programme chunks were opened (ids only) |
| Settings | Onboarding done, Lite mode, protection on/off |

What is **never** stored: free-text journal entries, URLs, search terms,
images, contacts, location, or any identifier that could identify you.

## What leaves the phone

Nothing by default. The only network the app ever makes is an optional,
Wi-Fi-only download of a signed on-device AI model or content pack
(HTTPS, SHA-256 verified). That download contains no user data.

## Your controls

- Delete all data: Settings → "امسح كل بياناتي من الجهاز" wipes the
  encrypted database and its key. There is no backup and no recovery.
- Uninstalling the app deletes everything with it.
- Protection can be disabled only after a 24-hour delay and a trusted
  person's approval code (a social commitment device, not a lock).

## Legal note

Mental-health data is **sensitive personal data** under Egypt's Personal
Data Protection Law 151/2020. On-device-only processing reduces the scope
(no transfer, no cross-border flow), but it does not automatically exempt
the app. A written legal opinion is required before any public launch,
and this document is not legal advice.
