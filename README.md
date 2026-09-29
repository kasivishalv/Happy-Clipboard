# 📋 Happy Clipboard

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" alt="Happy Clipboard Logo" width="80" height="80" style="border-radius: 20px;" />
</p>

<p align="center">
  <b>Enterprise-Grade, Privacy-First, Offline Android Clipboard Manager</b>
  <br />
  <i>Built with Jetpack Compose, Material Design 3, Room, and Hardware-Backed Cryptography</i>
</p>

<p align="center">
  <img src="screenshots/app_screenshot.png" alt="Happy Clipboard Inside App Screenshot" width="340" style="border-radius: 24px; box-shadow: 0 10px 30px rgba(0,0,0,0.3); border: 2px solid rgba(255,255,255,0.1);" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024%20--%2036)-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose%20(M3)-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Compose" />
  <img src="https://img.shields.io/badge/Storage-Room%20Database%20(SQLite)-009688?style=for-the-badge&logo=sqlite&logoColor=white" alt="Room" />
  <img src="https://img.shields.io/badge/Crypto-AES--256--GCM%20%7C%20PBKDF2-FF5722?style=for-the-badge&logo=security&logoColor=white" alt="Crypto" />
  <img src="https://img.shields.io/badge/Tests-100%25%20Passing-success?style=for-the-badge&logo=checkmarx&logoColor=white" alt="Tests Passing" />
</p>

---

## 📑 Table of Contents

- [🌟 Overview & Core Philosophy](#-overview--core-philosophy)
- [✨ Key Features](#-key-features)
  - [1. Automatic Offline Clipboard Capture](#1-automatic-offline-clipboard-capture)
  - [2. Intelligent Content Classification](#2-intelligent-content-classification)
  - [3. Organization, Collections & Bookmarks](#3-organization-collections--bookmarks)
  - [4. Calendar & Date Range Explorer](#4-calendar--date-range-explorer)
  - [5. Bulk Selection & Batch Operations](#5-bulk-selection--batch-operations)
  - [6. Encrypted Backup & Cross-Device Migration](#6-encrypted-backup--cross-device-migration)
  - [7. App Security, Screen Defense & PIN Guard](#7-app-security-screen-defense--pin-guard)
- [🔐 Deep Dive: Phase 1 Cryptographic Architecture](#-deep-dive-phase-1-cryptographic-architecture)
  - [Cryptographic Hierarchy Diagram](#cryptographic-hierarchy-diagram)
  - [1. Master Key Life Cycle](#1-master-key-life-cycle)
  - [2. Device-Local Keystore Wrapping](#2-device-local-keystore-wrapping)
  - [3. Disaster Recovery Key Derivation & Checksum](#3-disaster-recovery-key-derivation--checksum)
  - [4. Factory Reset & Disaster Recovery Flow](#4-factory-reset--disaster-recovery-flow)
  - [5. Memory Zeroization](#5-memory-zeroization)
- [📱 Interactive Navigation & Screen Breakdown](#-interactive-navigation--screen-breakdown)
- [⚙️ Configurable Preferences & Security Settings](#-configurable-preferences--security-settings)
- [🏗️ Technical Stack & Architectural Design](#️-technical-stack--architectural-design)
  - [Architecture Pattern (MVVM + Clean)](#architecture-pattern-mvvm--clean)
  - [Database Schema (Room v1)](#database-schema-room-v1)
- [📂 Project Directory Structure](#-project-directory-structure)
- [🧪 Test Suite & Verification](#-test-suite--verification)
- [🚀 Quick Start & Build Instructions](#-quick-start--build-instructions)
- [🛡️ Threat Model & Privacy Guarantees](#️-threat-model--privacy-guarantees)
- [❓ Frequently Asked Questions (FAQ)](#-frequently-asked-questions-faq)

---

## 🌟 Overview & Core Philosophy

**Happy Clipboard** is engineered from the ground up to solve a fundamental problem with modern mobile operating systems: **loss of copied content without compromising personal privacy**. 

Unlike standard cloud-tethered clipboard tools that route user clips through proprietary external servers, **Happy Clipboard operates with a strict 100% offline-first model**:

* **Zero Cloud Dependence for Daily Operations:** All clips, collections, history, and metadata reside entirely on-device inside a local SQLite/Room database.
* **No Telemetry or Analytics SDKs:** Zero tracking, zero diagnostics beacons, zero advertising libraries.
* **Hardware-Backed Cryptographic Roots:** Sensitive keys never exist unencrypted on flash memory; keys are protected by the hardware TEE (Trusted Execution Environment) or StrongBox via AndroidKeyStore.
* **OS-Level Privacy Defense:** Full integration with Android 13+ sensitive clipboard flags and Android `FLAG_SECURE` screen protection.

---

## ✨ Key Features

### 1. Automatic Offline Clipboard Capture
* **Foreground Service with Persistent Protection:** `ClipboardMonitorService` runs quietly with a low-priority, ongoing notification, listening to primary clip events.
* **Smart Deduplication:** Configurable between:
  * **MERGE:** Automatically merges repeated copies of identical text, incrementing the copy counter (`copyCount`) and updating the `lastCopiedAt` timestamp to move it to the top of your feed.
  * **KEEP_ALL:** Records every single copy event as an independent entry.
* **Share Sheet Receiver:** Direct integration with Android's system share menu (`ACTION_SEND`, `text/*`), allowing you to send selected text into Happy Clipboard from any browser or application.
* **Excluded Packages Filter:** Prevents automatic capture when copying from security-critical apps (e.g., Settings, KeePass, 1Password, Bitwarden, Google Keep).

### 2. Intelligent Content Classification
Every captured snippet is analyzed instantly on-device by `ContentClassifier`:

| Content Type | Classification Criteria | Visual Indicator | Contextual Action |
|:---|:---|:---:|:---|
| **Text** | Standard alphanumeric content, sentences, notes | 📄 Text | One-tap copy, edit, note assignment |
| **Link (URL)** | Valid Web URLs starting with `http://`, `https://`, or `www.` | 🔗 Link | Direct URL preview, browser launch |
| **Email** | RFC-compliant email addresses | ✉️ Email | Direct email client launch |
| **Phone** | International and localized phone number patterns | 📞 Phone | Direct dialer shortcut |
| **Code / CLI** | Shell commands (`docker`, `git`, `curl`, `npm`, `kubectl`, `apt`, `sudo`), SQL queries, or code syntax (`val`, `func`, `class`, `import`, JSON objects/arrays) | 💻 Code | Monospace syntax container, quick copy |
| **Color** | Hexadecimal codes (`#FFFFFF`, `#3F51B5`) and RGB/RGBA CSS definitions | 🎨 Color | Real-time live color swatch preview chip |
| **Number** | Standalone numeric digits, calculations, and floats | 🔢 Number | Formatted display |
| **Sensitive** | Credit card numbers, API keys (`sk_live_`, `AIzaSy`, `ghp_`, Bearer tokens, JWT tokens), RSA private keys (`-----BEGIN PRIVATE KEY-----`), and OTP verification codes | 🔒 Sensitive | Masked preview (`••••••••`), OS privacy flag, auto-deletion timer |

### 3. Organization, Collections & Bookmarks
* **Custom Collections:** Group related snippets (e.g., *Docker & DevOps*, *Programming & SQL*, *Web Resources*, *Personal Notes*).
* **Color & Icon Customization:** Assign custom icon identifiers (`folder`, `terminal`, `code`, `star`, `person`, `link`, etc.) and brand colors.
* **Favorites (Starred Clips):** Bookmark important credentials or frequent snippets with one tap. Favorite items are immune to automated capacity pruning or bulk history purges.
* **Notes Annotation:** Attach personalized context or reminders to any snippet without modifying the underlying copied text.

### 4. Calendar & Date Range Explorer
* **Time-Machine Navigation:** Navigate your entire clip history day by day with interactive calendar controls.
* **Daily Clip Counters:** Visual badges reveal copy frequency per calendar date.
* **Quick Jumps:** Instantly jump between "Today", "Yesterday", and custom historical date ranges.

### 5. Bulk Selection & Batch Operations
* **Long-Press Multi-Select:** Trigger multi-selection mode with long press or dedicated toolbar action.
* **Select All / Invert:** Instantly toggle selection across all displayed clips.
* **Batch Operations:**
  * Bulk Delete selected items in a single atomic database transaction.
  * Bulk Move/Assign selected items to a collection.
  * Clear Non-Favorites while leaving starred items untouched.

### 6. Encrypted Backup & Cross-Device Migration
* **HCB Version 2 (`HCB_V2`) Authenticated Container:**
  * Password-derived 256-bit encryption key via **PBKDF2-HMAC-SHA256 with 600,000 iterations**.
  * Authenticated **AES-256-GCM** encryption with fresh 16-byte random salt and 12-byte random IV.
  * Tamper-evident: Alteration of any bit in ciphertext or parameters aborts decryption.
* **Plain JSON Backup:** Human-readable `.json` export formatted to the open Happy Clipboard v1 schema.
* **Flexible Import Modes:**
  * **MERGE:** Imports items from backup without altering existing clips.
  * **REPLACE:** Atomically wipes current non-favorite history and replaces it with the backup content.

### 7. App Security, Screen Defense & PIN Guard
* **Salted PIN Authentication:** 4-digit PIN hashed using **PBKDF2-HMAC-SHA256 (100,000 iterations)** with a 16-byte random salt, verified in constant-time to resist timing attacks.
* **Android 13+ OS Overlay Masking:** When copying sensitive clips (passwords, OTPs, API keys), Happy Clipboard applies `ClipDescription.EXTRA_IS_SENSITIVE = true`. Android 13+ clipboard preview windows automatically conceal the content from screen snooping.
* **`FLAG_SECURE` Window Shield:** When enabled, suppresses Android system screenshots, screen recordings, and hides app contents from the Android Recent Apps / Task Switcher thumbnail.
* **OTP Auto-Purge:** Automatic background deletion of one-time passwords and verification codes after 1, 5, or 15 minutes.

---

## 🔐 Deep Dive: Phase 1 Cryptographic Architecture

Happy Clipboard implements a multi-tier cryptographic key hierarchy aligning with OWASP and NIST guidelines.

```
                  ┌───────────────────────────────────────────────────────────┐
                  │                 256-bit Cloud Master Key                  │
                  │              (Generated via SecureRandom)                 │
                  └─────────────┬───────────────────────────────┬─────────────┘
                                │                               │
             ┌──────────────────┴──────────────┐ ┌──────────────┴──────────────────┐
             │       Hardware Device Wrap      │ │      Disaster Recovery Wrap     │
             │   (AndroidKeyStore Provider)    │ │   (Crockford Base32 + Checksum) │
             └──────────────────┬──────────────┘ └──────────────┬──────────────────┘
                                │                               │
                                ▼                               ▼
                  ┌───────────────────────────┐   ┌───────────────────────────┐
                  │  AES-256-GCM (NoPadding)  │   │  PBKDF2-HMAC-SHA256       │
                  │  Alias: "happy_clipboard  │   │  600,000 iterations       │
                  │  _master_key_device_wrap" │   │  16-byte random salt      │
                  │  Tag: 128-bit, 12-byte IV │   │  12-byte random nonce     │
                  └─────────────┬─────────────┘   └─────────────┬─────────────┘
                                │                               │
                                ▼                               ▼
                  ┌───────────────────────────┐   ┌───────────────────────────┐
                  │ KEYSTORE_WRAP_V1:         │   │ RECOVERY_WRAP_V1:         │
                  │ <iv_b64>:<ciphertext_b64> │   │ <base64(container_json)>  │
                  └───────────────────────────┘   └───────────────────────────┘
```

<details>
<summary><b>🔍 Click to view Cryptographic Specifications & Algorithms</b></summary>

### 1. Master Key Life Cycle
* **Length:** 256 bits (32 bytes) of true cryptographic entropy from `java.security.SecureRandom`.
* **Purpose:** The root encryption key for all cloud and local sync containers.

### 2. Device-Local Keystore Wrapping
* **Provider:** `AndroidKeyStore`
* **Key Specifications:**
  * Algorithm: `AES`
  * Block Mode: `KeyProperties.BLOCK_MODE_GCM`
  * Padding: `KeyProperties.ENCRYPTION_PADDING_NONE`
  * Key Size: 256 bits
  * Hardware Backing: Key is retained within the hardware Security Module (TEE/StrongBox) where supported.
* **Payload Format:** `KEYSTORE_WRAP_V1:<base64_iv>:<base64_ciphertext>`

### 3. Disaster Recovery Key Derivation & Checksum
* **Entropy:** 256 bits (32 raw random bytes).
* **Encoding Alphabet:** Crockford Base32 (`0123456789ABCDEFGHJKMNPQRSTVWXYZ`) — excluding ambiguous characters `I`, `L`, `O`, `U`.
* **Checksum:** 20-bit checksum derived from SHA-256 hash of the 32-byte entropy (4 Crockford characters).
* **Format:** Exactly 56 characters grouped into 14 hyphen-separated blocks:
  ```
  XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX
  ```
* **Input Normalization:** Tolerates case variations, hyphens, whitespace, and maps `O -> 0`, `I -> 1`, `L -> 1`.
* **KDF:** PBKDF2-HMAC-SHA256 with **600,000 iterations** and 16-byte random salt.
* **Wrapping Cipher:** AES-256-GCM (128-bit authentication tag, 12-byte fresh random nonce).
* **Container Header:** `RECOVERY_WRAP_V1:<base64_json>`

### 4. Factory Reset & Disaster Recovery Flow
1. User enters the 56-character Recovery Key on a new or reset device.
2. `RecoveryKeyHelper.decodeAndValidateRecoveryKey` validates the 20-bit SHA-256 checksum in constant time.
3. PBKDF2 derives the 256-bit decryption key.
4. AES-256-GCM decrypts and authenticates the wrapped payload. Any bit tampering throws a `SecurityException`.
5. The recovered Master Key is **immediately re-wrapped** by the new device's local `AndroidKeyStore` hardware key.
6. The raw Master Key bytes in RAM are wiped.

### 5. Memory Zeroization
Raw key byte arrays (`ByteArray`) are wiped immediately after use via `.fill(0)` within `try ... finally` blocks to minimize exposure to memory dump attacks.

</details>

---

## 📱 Interactive Navigation & Screen Breakdown

<p align="center">
  <img src="screenshots/app_screenshot.png" alt="Happy Clipboard Screen Capture" width="300" style="border-radius: 20px; box-shadow: 0 6px 20px rgba(0,0,0,0.25);" />
</p>

Happy Clipboard provides a bottom navigation bar with 5 primary tabs:

```
┌─────────────────────────────────────────────────────────────┐
│                       HAPPY CLIPBOARD                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   [ 📋 History ]    Browse, search, and manage all clips    │
│                                                             │
│   [ 📅 Calendar ]   Filter and explore clips by date        │
│                                                             │
│   [ 📁 Collections] Organize clips by categories & tags     │
│                                                             │
│   [ ⭐ Favorites ]   Quick access to starred snippets        │
│                                                             │
│   [ ⚙️ Settings ]   Security, capacity, PIN & backups       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

<details>
<summary><b>📋 1. History Screen (Home)</b></summary>

* **Top Search Bar:** Live debounced search querying text content, notes, collection names, and content types.
* **Filter Chips Carousel:** Quick filtering by content type: `All`, `Code`, `Links`, `Colors`, `Sensitive`, `Emails`, `Phone`.
* **Active Status Banner:** Displays clipboard monitoring status, current clip count, and system clipboard purge action.
* **Interactive Clipboard Cards:**
  * **Quick Copy:** One-tap to copy back to Android clipboard.
  * **Favorite Toggle:** Star or unstar with immediate persistence.
  * **Collection Badge:** Shows assigned collection with color dot.
  * **Masking Toggle:** Eye icon to view/hide sensitive content.
  * **Context Menu:** Edit, Add Notes, Assign Collection, Share, View Metadata, Delete.
* **Bulk Selection Mode:** Triggered on long-press; enables batch deletion and collection assignment.
* **Floating Action Button (FAB):** Manually insert clips with custom category and notes.

</details>

<details>
<summary><b>📅 2. Calendar Screen</b></summary>

* **Date Header:** Shows selected date formatted with day of week.
* **Month / Day Navigation:** Forward/backward navigation between days.
* **Calendar Grid:** Displays the current month with highlight dots indicating days with recorded clips.
* **Date-Filtered Feed:** Displays all snippets recorded during the selected 24-hour window.
* **Quick Today Button:** Immediately jumps to the current day.

</details>

<details>
<summary><b>📁 3. Collections Screen</b></summary>

* **Collection Grid / List:** Visual cards showing collection name, icon, color accent, and active snippet count.
* **Create Collection Dialog:** Name input, color picker (palette of 8 distinct hues), and icon selector (Folder, Terminal, Code, Star, Person, Link, Work, Bookmark).
* **Filtered Collection View:** Tap any collection card to inspect all snippets categorized within that collection.
* **Collection Management:** Rename or delete collections (associated clips revert to unassigned).

</details>

<details>
<summary><b>⭐ 4. Favorites Screen</b></summary>

* **Dedicated Bookmarks View:** Shows only starred clips.
* **Zero-Prune Guarantee:** Clips marked as Favorite are permanently excluded from automated retention pruning or capacity trimming.
* **Full Search & Filter:** Dedicated search bar to find starred snippets in seconds.

</details>

<details>
<summary><b>⚙️ 5. Settings Screen</b></summary>

* **Monitoring Controls:** Toggle background clipboard listener and service.
* **Deduplication Strategy:** Radio selection between `MERGE` (update count) and `KEEP_ALL`.
* **History Capacity Management:**
  * Preset limits: 200, 500, 1,000, 5,000, 10,000, 25,000, 50,000, or **UNLIMITED**.
  * Automatic FIFO trimming for non-favorite clips.
* **Retention Policy:** Auto-delete clips older than 7 days, 30 days, 90 days, or Never.
* **Sensitive Content Handling:**
  * `SAVE_AND_MASK`: Save with sensitive mask and hide from OS preview.
  * `DONT_SAVE`: Never persist sensitive data to disk.
  * `SAVE_ALL`: Save as standard text.
* **OTP Auto-Delete:** Auto-purge verification codes after 1 min, 5 mins, 15 mins, or Never.
* **App Lock & PIN Security:** Set, verify, or change 4-digit PIN.
* **Secure Screens:** Toggle `FLAG_SECURE` window protection.
* **System Clipboard Purge:** Instantly zero out the active Android clipboard.
* **Backup & Restore:**
  * Export plain `.json` or encrypted `.hcb` (HCB_V2 container).
  * Import backup files with `MERGE` or `REPLACE` modes.
* **Theme Selection:** System Default, Light Mode, Dark Mode.
* **Compact Card View:** Toggle high-density list layout.

</details>

---

## ⚙️ Configurable Preferences & Security Settings

All configuration options are managed reactively via `SecurityPreferences`:

| Preference Key | Type | Default Value | Available Options | Description |
|:---|:---:|:---:|:---|:---|
| `auto_capture` | Boolean | `true` | `true`, `false` | Enable/disable automatic clipboard monitoring. |
| `duplicate_handling` | String | `"MERGE"` | `"MERGE"`, `"KEEP_ALL"` | Merge duplicates and increment counter, or save separate copies. |
| `max_history_capacity` | String | `"500"` | `200`, `500`, `1000`, `5000`, `10000`, `25000`, `50000`, `UNLIMITED` | Cap on maximum non-favorite clips stored in SQLite. |
| `retention_days` | Int | `30` | `0` (unlimited), `7`, `30`, `90` | Automatic deletion threshold for older non-favorite clips. |
| `sensitive_handling` | String | `"SAVE_AND_MASK"` | `"SAVE_AND_MASK"`, `"DONT_SAVE"`, `"SAVE_ALL"` | Policy applied when passwords, cards, or API keys are detected. |
| `otp_auto_delete_minutes` | Int | `5` | `0` (never), `1`, `5`, `15` | Expiration window for detected OTP and 2FA tokens. |
| `app_lock_enabled` | Boolean | `false` | `true`, `false` | Gate app launch behind PIN authentication overlay. |
| `app_lock_pin_hash` | String | `""` | Salted PBKDF2 hash | Formatted as `PIN_V1:<salt_b64>:<hash_b64>`. |
| `secure_screens` | Boolean | `false` | `true`, `false` | Sets `FLAG_SECURE` to block screenshots and recents preview. |
| `theme_mode` | String | `"SYSTEM"` | `"SYSTEM"`, `"LIGHT"`, `"DARK"` | Visual theme preference. |
| `compact_view` | Boolean | `false` | `true`, `false` | High-density card layout for compact visual scanning. |
| `cloud_sync_enabled` | Boolean | `false` | `true`, `false` | Flag indicating whether Cloud Master Key is active. |

---

## 🏗️ Technical Stack & Architectural Design

### Architecture Pattern (MVVM + Clean)

```
┌─────────────────────────────────────────────────────────────┐
│                      UI / View Layer                        │
│    Jetpack Compose Composables (M3) + MaterialRipple        │
│    (HomeScreen, CalendarScreen, CollectionsScreen, etc.)    │
└──────────────────────────────▲──────────────────────────────┘
                               │ StateFlow<ClipboardUiState>
                               ▼ UI Events / User Actions
┌─────────────────────────────────────────────────────────────┐
│                    ViewModel Layer                          │
│               MainViewModel (Jetpack ViewModel)            │
│       Combines StateFlows, Manages Overlays & Filters       │
└──────────────────────────────▲──────────────────────────────┘
                               │ Coroutines & Flow<List<T>>
                               ▼ Suspend Calls / Mutations
┌─────────────────────────────────────────────────────────────┐
│                   Repository Layer                          │
│                  ClipboardRepository                        │
│  Orchestrates Room DAOs, ContentClassifier, & CryptoHelper  │
└──────────────┬───────────────────────────────┬──────────────┘
               │                               │
               ▼                               ▼
┌─────────────────────────────┐ ┌─────────────────────────────┐
│        Room Database        │ │       Security Core         │
│     (AppDatabase v1)        │ │  (KeyManager, CryptoHelper, │
│  - clipboard_items (Table)  │ │   RecoveryKeyHelper,        │
│  - collections (Table)      │ │   AndroidKeystoreHelper)    │
└─────────────────────────────┘ └─────────────────────────────┘
```

### Database Schema (Room v1)

<details>
<summary><b>💾 Table: <code>clipboard_items</code></b></summary>

```sql
CREATE TABLE IF NOT EXISTS clipboard_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    content TEXT NOT NULL,
    contentType TEXT NOT NULL,
    contentHash TEXT NOT NULL,
    createdAt INTEGER NOT NULL,
    lastCopiedAt INTEGER NOT NULL,
    copyCount INTEGER NOT NULL DEFAULT 1,
    isFavorite INTEGER NOT NULL DEFAULT 0,
    collectionId TEXT,
    collectionName TEXT,
    sourceApp TEXT,
    isSensitive INTEGER NOT NULL DEFAULT 0,
    expiresAt INTEGER,
    notes TEXT
);

CREATE INDEX IF NOT EXISTS index_clipboard_items_contentHash ON clipboard_items(contentHash);
CREATE INDEX IF NOT EXISTS index_clipboard_items_lastCopiedAt ON clipboard_items(lastCopiedAt);
CREATE INDEX IF NOT EXISTS index_clipboard_items_isFavorite ON clipboard_items(isFavorite);
CREATE INDEX IF NOT EXISTS index_clipboard_items_collectionId ON clipboard_items(collectionId);
```

</details>

<details>
<summary><b>📁 Table: <code>collections</code></b></summary>

```sql
CREATE TABLE IF NOT EXISTS collections (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    iconName TEXT NOT NULL DEFAULT 'folder',
    colorHex INTEGER NOT NULL DEFAULT 4282331573,
    createdAt INTEGER NOT NULL
);
```

</details>

---

## 📂 Project Directory Structure

```
HappyClipboard/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml          # Service, Intent filters, Permissions
│   │   │   ├── java/com/example/
│   │   │   │   ├── HappyClipboardApp.kt     # Application class, Singleton & Seed
│   │   │   │   ├── MainActivity.kt          # Single-activity container, Navigation
│   │   │   │   ├── core/
│   │   │   │   │   ├── classifier/
│   │   │   │   │   │   └── ContentClassifier.kt   # Regex & regex heuristics classifier
│   │   │   │   │   └── security/
│   │   │   │   │       ├── AndroidKeystoreHelper.kt # Hardware Keystore wrapper
│   │   │   │   │       ├── CryptoHelper.kt          # HCB_V2 backup AES-GCM & PIN KDF
│   │   │   │   │       ├── KeyManager.kt            # Cloud Master Key orchestrator
│   │   │   │   │       ├── RecoveryKeyHelper.kt     # Base32 56-char Recovery Key
│   │   │   │   │       └── SecurityPreferences.kt   # SharedPreferences manager
│   │   │   │   ├── data/
│   │   │   │   │   ├── db/
│   │   │   │   │   │   ├── AppDatabase.kt       # RoomDatabase definition
│   │   │   │   │   │   ├── ClipboardDao.kt      # ClipboardItem DAO operations
│   │   │   │   │   │   ├── CollectionDao.kt     # CollectionEntity DAO operations
│   │   │   │   │   │   └── Converters.kt        # TypeConverters (ContentType)
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── ClipboardItem.kt     # Clipboard item data entity
│   │   │   │   │   │   ├── CollectionEntity.kt  # Collection data entity
│   │   │   │   │   │   └── ContentType.kt       # ContentType enum definition
│   │   │   │   │   └── repository/
│   │   │   │   │       └── ClipboardRepository.kt # Business repository
│   │   │   │   ├── service/
│   │   │   │   │   └── ClipboardMonitorService.kt # Foreground listener service
│   │   │   │   └── ui/
│   │   │   │       ├── MainViewModel.kt         # Jetpack ViewModel & StateFlows
│   │   │   │       ├── components/
│   │   │   │       │   ├── AppLockOverlay.kt    # Full-screen PIN entry overlay
│   │   │   │       │   └── ClipboardItemCard.kt # Composable snippet card
│   │   │   │       ├── dialogs/
│   │   │   │       │   ├── AddClipDialog.kt     # Manual clip creation dialog
│   │   │   │       │   ├── CollectionPickerDialog.kt # Move to collection
│   │   │   │       │   ├── EditClipDialog.kt    # Edit snippet or note dialog
│   │   │   │       │   ├── ExportBackupDialog.kt# JSON / HCB_V2 export modal
│   │   │   │       │   ├── ImportBackupDialog.kt# JSON / HCB_V2 import modal
│   │   │   │       │   ├── ItemDetailsDialog.kt # Metadata inspection modal
│   │   │   │       │   └── NewCollectionDialog.kt # Create collection modal
│   │   │   │       ├── screens/
│   │   │   │       │   ├── CalendarScreen.kt    # Date navigation & calendar view
│   │   │   │       │   ├── CollectionsScreen.kt # Collection grid & filter view
│   │   │   │       │   ├── FavoritesScreen.kt   # Starred snippets feed
│   │   │   │       │   ├── HomeScreen.kt        # Primary history stream & search
│   │   │   │       │   └── SettingsScreen.kt    # Comprehensive configuration
│   │   │   │       ├── theme/
│   │   │   │       │   ├── Color.kt             # Material 3 Color definitions
│   │   │   │       │   ├── Theme.kt             # MyApplicationTheme composable
│   │   │   │       │   └── Type.kt              # JetBrains Mono & Plus Jakarta Sans
│   │   │   │       └── util/
│   │   │   │           └── CollectionIcons.kt   # Material icon mapping helper
│   │   │   └── res/                             # Vector drawables, fonts, mipmaps
│   │   └── test/
│   │       ├── java/com/example/
│   │       │   ├── CollectionIconsTest.kt       # Icon resolver tests
│   │       │   ├── ExampleRobolectricTest.kt    # Robolectric baseline tests
│   │       │   ├── HomeScreenScreenshotTest.kt  # Compose Roborazzi screenshot tests
│   │       │   ├── KeyManagementTest.kt         # Master Key & Keystore tests
│   │       │   ├── MainActivityLaunchTest.kt    # Activity lifecycle tests
│   │       │   ├── MaxHistoryCapacityTest.kt    # Capacity limit & FIFO tests
│   │       │   ├── PinSecurityTest.kt           # Salted PIN & constant-time tests
│   │       │   └── RecoveryKeyTest.kt           # Recovery Key wrap & tamper tests
│   │       └── screenshots/                     # Test-generated screen captures
│   └── build.gradle.kts                         # App module Gradle configuration
├── screenshots/                                 # Documentation & README screenshot assets
│   ├── app_screenshot.png                       # Pixel 8 native resolution app UI
│   └── home_screen.png                          # Roborazzi captured inside UI
├── gradle/                                      # Gradle wrapper & version catalogs
├── build.gradle.kts                             # Root project Gradle configuration
├── metadata.json                                # Google AI Studio project metadata
├── settings.gradle.kts                          # Project settings & repositories
└── README.md                                    # Comprehensive documentation
```

---

## 🧪 Test Suite & Verification

The project includes an automated unit and Robolectric test suite verifying all critical paths, cryptographic boundaries, and UI logic:

| Test File | Key Coverage Areas | Status |
|:---|:---|:---:|
| `RecoveryKeyTest.kt` | 256-bit entropy verification, Crockford Base32 encoding/decoding, 20-bit SHA-256 checksum validation, case/whitespace normalization, PBKDF2 600,000 iterations, AES-256-GCM wrapping/unwrapping, and tamper rejection tests (tampered salt, nonce, ciphertext) | ✅ PASS |
| `KeyManagementTest.kt` | End-to-end Master Key generation, local Keystore wrapping, disaster recovery unwrap, factory-reset simulation without old Keystore, and memory zeroization checks | ✅ PASS |
| `PinSecurityTest.kt` | Salted PBKDF2 PIN hashing (100,000 iterations), constant-time equality check, invalid length/character rejection, and legacy plaintext PIN migration | ✅ PASS |
| `MaxHistoryCapacityTest.kt` | Preset capacity options (200, 500, 1000, UNLIMITED), FIFO trimming of non-favorite items, and preservation of starred clips | ✅ PASS |
| `CollectionIconsTest.kt` | Icon registry resolution, fallback defaults, and color mapping | ✅ PASS |
| `MainActivityLaunchTest.kt` | Activity launch lifecycle under Robolectric, initial UI composition, and intent handling | ✅ PASS |
| `HomeScreenScreenshotTest.kt` | Pixel 8 Roborazzi native UI rendering, Compose tree validation, and visual regression capture | ✅ PASS |

To execute the full test suite locally:
```bash
gradle :app:testDebugUnitTest --rerun-tasks
```

---

## 🚀 Quick Start & Build Instructions

### Prerequisites
* **Java Development Kit (JDK):** Version 17 or Version 21 recommended
* **Android SDK:** Compile SDK 36 (target SDK 36, min SDK 24)
* **Gradle:** Version 8.x+

### Build Commands

```bash
# 1. Clean build & compile Kotlin debug sources
gradle :app:compileDebugKotlin

# 2. Run all unit and Robolectric tests
gradle :app:testDebugUnitTest

# 3. Assemble Debug APK
gradle :app:assembleDebug

# The generated APK will be located at:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 🛡️ Threat Model & Privacy Guarantees

```
                   ┌──────────────────────────────────────┐
                   │           THREAT LANDSCAPE           │
                   └──────────────────┬───────────────────┘
                                      │
        ┌─────────────────────────────┼─────────────────────────────┐
        ▼                             ▼                             ▼
┌───────────────┐             ┌───────────────┐             ┌───────────────┐
│ Memory Snooping│            │ Cloud Leakage │             │ Screen Spy    │
└───────┬───────┘             └───────┬───────┘             └───────┬───────┘
        │                             │                             │
        ▼                             ▼                             ▼
┌───────────────┐             ┌───────────────┐             ┌───────────────┐
│ Key Zeroing:  │             │ 100% Offline: │             │ FLAG_SECURE + │
│ ByteArray     │             │ Zero Internet │             │ OS Sensitive  │
│ .fill(0)      │             │ Permissions   │             │ Clip Flag     │
└───────────────┘             └───────────────┘             └───────────────┘
```

1. **Zero Internet Exposure:** Happy Clipboard does not declare `android.permission.INTERNET` in its manifest. It is physically impossible for the app to transmit data over Wi-Fi or cellular networks.
2. **Device Stealing / Flash Extraction:** Plaintext clips in SQLite are guarded behind Android's file-based encryption (FBE). Exported backups use AES-256-GCM authenticated encryption.
3. **OS Clipboard Snooping:** Android 13+ sensitive tags hide passwords and tokens from the system preview popup.
4. **App Switcher Snooping:** `FLAG_SECURE` blocks third-party screenshot utilities and blanks the app thumbnail in the Recents carousel.
5. **Memory Dump Attacks:** Ephemeral keys are overwritten with zeros immediately following encryption or decryption operations.

---

## ❓ Frequently Asked Questions (FAQ)

<details>
<summary><b>Q1: Does Happy Clipboard transmit any data to external cloud servers?</b></summary>
<b>No.</b> Happy Clipboard has zero network communication capabilities. The application operates strictly offline. All cryptographic keys and clip databases are stored entirely on the physical device.
</details>

<details>
<summary><b>Q2: What happens if I forget my App Lock PIN?</b></summary>
If you set a PIN, access to the UI requires authentication. To protect against unauthorized physical access, there is no backdoor bypass. If you forget your PIN, you can restore your data from an encrypted HCB backup using your backup password.
</details>

<details>
<summary><b>Q3: What is the purpose of the 56-character Recovery Key?</b></summary>
The Recovery Key provides disaster recovery for your Cloud Master Key. If your device undergoes a factory reset or you upgrade to a new phone, the old hardware Keystore key is destroyed. Entering your Recovery Key allows you to decrypt the Master Key and automatically wrap it into the new phone's hardware Keystore.
</details>

<details>
<summary><b>Q4: How does Happy Clipboard identify sensitive items like OTPs or API keys?</b></summary>
The on-device <code>ContentClassifier</code> executes high-performance regular expressions for known credential signatures (JWTs, Stripe keys, GitHub tokens, Google API keys, credit cards, private keys) and OTP SMS patterns.
</details>

<details>
<summary><b>Q5: Will clearing my history delete my Starred/Favorite clips?</b></summary>
<b>No.</b> By default, "Clear History" only removes unstarred clips. Your Favorites are strictly preserved unless you explicitly delete them individually.
</details>

---

<p align="center">
  <b>Happy Clipboard</b> &bull; <i>Privacy, Simplicity, & Security for Android</i>
</p>
