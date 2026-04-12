# 🤖 Gemini Project Handbook: SilentZone 🤫

This document serves as a high-level technical and contextual guide for **Gemini** (and other AI agents) to understand, maintain, and evolve the **SilentZone** Android project.

---

## 🎯 Project Overview
**SilentZone** is a premium Android application designed for intelligent ringer automation. It leverages environmental signals (WiFi, Geofences, Calendar events) to seamlessly transition device ringer modes, ensuring a quieter world without manual intervention.

- **Corpus Name**: `dn-sandeeep/Silent-Zone`
- **Primary Objective**: Automate silence while ensuring "what matters" (emergency contacts) still gets through.

---

## 🛠️ Tech Stack & Architecture
SilentZone follows modern Android development practices:

- **UI Framework**: Jetpack Compose (Material 3) with a "Chrome Edition" aesthetic.
- **Architecture**: MVVM + Clean Architecture.
- **Dependency Injection**: Dagger Hilt.
- **Persistence**: Room Database (for Zones, Contacts, and Analytics).
- **Automation Engines**:
    - **Geofencing API**: Google Play Services for location triggers.
    - **BroadcastReceivers**: For WiFi changes (`ConnectivityManager`) and Phone State (`TelephonyManager`).
    - **Notification Policy**: To manage "Do Not Disturb" (DND) across different Android versions.

---

## 📂 Key Codebase Map

### 🏎️ Core Repositories
- [SilentModeRepository.kt](file:///d:/Projects/Android/SilentZone/app/src/main/java/com/sandeep/silentzone/SilentModeRepository.kt): The "Brain" of the app. Handles mode transitions, state preservation, and coordinate/WiFi logic.

### 📡 Automation Triggers
- [GeofenceBroadcastReceiver.kt](file:///d:/Projects/Android/SilentZone/app/src/main/java/com/sandeep/silentzone/GeofenceBroadcastReceiver.kt): Entry point for location-based ringer changes.
- `WifiConnectivityReceiver.kt` (Planned/Implemented): Entry point for SSID-based triggers.

### 🎨 UI & State
- [SilentModeViewModel.kt](file:///d:/Projects/Android/SilentZone/app/src/main/java/com/sandeep/silentzone/SilentModeViewModel.kt): Manages the reactive state for the dashboard and settings.
- `DashboardScreen.kt`: The primary visual interface with dynamic mode indicators.

### 💾 Data Layer
- [SilentZoneDao.kt](file:///d:/Projects/Android/SilentZone/app/src/main/java/com/sandeep/silentzone/data/SilentZoneDao.kt): Database operations for zones and whitelisted contacts.

---

## 📈 Current Implementation Status

| Feature | Status | Note |
| :--- | :--- | :--- |
| **Geofencing** | ✅ Active | Supports multi-zone entry/exit detection. |
| **WiFi Triggers** | ✅ Active | SSID-based mode switching with restoration. |
| **Emergency Whitelist** | ✅ Active | Bypasses silence for priority contacts. |
| **Meeting Mode** | 🚧 Beta | Google Calendar integration for "Busy" status. |
| **DND Fallback** | ✅ Active | Switches to Vibrate if DND permission is missing. |
| **Focus Analytics** | 📅 Roadmap | Tracking "Zen Hours" and focus trends. |

---

## 💡 Development & Testing Tips (For AI)

> [!IMPORTANT]
> **Ringer Mode Permissions**: Android 6.0+ requires `ACCESS_NOTIFICATION_POLICY` for `SILENT` mode. Always check `hasPolicyAccess()` before calling `setSilent()`.

1. **Testing Geofences**: Use "Simulate Location" in Android Studio or an Emulator. Geofences trigger best on *transitions*.
2. **WiFi SSIDs**: On Android 13+, SSID extraction requires `NEARBY_WIFI_DEVICES`.
3. **State Restoration**: The app stores the "Original Mode" when entering a zone. It ONLY restores when *all* active zones (WiFi and Location) are exited.
4. **DND Access**: If the user hasn't granted DND access, the app should gracefully fallback to `VIBRATE` instead of failing.


---
*Created by Gemini for the SilentZone Project.*
