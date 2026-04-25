# SilentZone 🤫📱

<p align="center">
  <img src="https://raw.githubusercontent.com/dn-sandeeep/Silent-Zone/main/assets/silent_zone_app_icon_1775393820851.png" width="180" alt="SilentZone Icon">
  <br>
  <b>Intelligent Ringer Automation for a Quieter, Meticulous Life.</b>
  <br>
  <i>Built with Jetpack Compose, Material 3, and cutting-edge Android 16 APIs 🚀</i>
  <br><br>
  <a href="YOUR_PLAY_STORE_LINK_HERE">
    <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" height="60" alt="Get it on Google Play">
  </a>
</p>

---

## 📖 Overview

**SilentZone** is an intelligent Android application designed to manage your device's ringer state based on your environment. Whether you're entering a library, arriving at the office, or connecting to your home WiFi, SilentZone ensures your device behaves exactly as it should—silently or with a ring—without you ever lifting a finger.

---

## 📺 Project Walkthrough & Demo

> [!TIP]
> **Experience SilentZone in Action!** The dashboard provides real-time feedback on your active silent zones and device status.

<p align="center">
  <img src="https://raw.githubusercontent.com/dn-sandeeep/Silent-Zone/main/assets/silent_zone_dashboard_mockup_1775393847082.png" width="800" alt="SilentZone Dashboard Mockup">
  <br>
  <i>(Dashboard showing active WiFi and Location zones with real-time status pulses)</i>
</p>

---

## 🌟 Key Features

### 📶 1. Smart WiFi Automation
*   **SSID Awareness**: Automatically switch to **Silent** or **Vibrate** mode when you connect to designated "Quiet Networks" (e.g., *University_5G*, *Office_Corp*).
*   **Intelligent Restoration**: Once you disconnect, SilentZone restores your device to its **previous ringer state**, ensuring you never miss a call after leaving a meeting.
*   **Low Impact**: Optimized to use system broadcasts, minimizing battery consumption while maintaining responsiveness.

<p align="center">
  <img src="YOUR_WIFI_SCREENSHOT_URL" width="300" alt="WiFi Automation Screenshot">
</p>

### 📍 2. Proactive Geofencing
*   **Interactive Map Selection**: Use the integrated Google Maps interface to drop pins and define radii for custom Silent Zones.
*   **Background Precision**: Powered by the **Google Play Services Geofencing API**, SilentZone triggers mode changes even when the app is in the background.
*   **Battery-Aware Tracking**: Leverages the Fused Location Provider with optimized polling intervals.

<p align="center">
  <img src="YOUR_GEOFENCE_SCREENSHOT_URL" width="300" alt="Geofencing Screenshot">
</p>

### 📊 3. Usage Analytics (Peaceful Time)
*   **Silent Duration Tracking**: Monitor how long your device has been in a silent state.
*   **Daily Insights**: View actionable insights on your "Peaceful Time" directly on the home dashboard.
*   **Historical Logs**: All entry and exit events are logged in a local database for long-term trend analysis.

<p align="center">
  <img src="YOUR_ANALYTICS_SCREENSHOT_URL" width="300" alt="Analytics Screenshot">
</p>

### 🔋 4. Battery Impact Dashboard
*   **Smart Estimation**: SilentZone calculates its own power consumption based on active monitoring duration for WiFi and Location.
*   **Transparent Metrics**: View a detailed breakdown of battery impact (WiFi vs. Location vs. System Overhead) to three decimal places.
*   **Efficiency First**: Designed to run with minimal overhead, even as a foreground service.

<p align="center">
  <img src="YOUR_BATTERY_SCREENSHOT_URL" width="300" alt="Battery Impact Screenshot">
</p>

### 💬 5. In-App Feedback & Sharing
*   **Direct Feedback**: Built-in feedback system to report issues or suggest features directly from the app.
*   **Native Sharing**: Easily share SilentZone with friends and colleagues using the system's native share sheet.

---

## 🛠️ Tech Stack & Architecture

SilentZone is built using modern Android development best practices and the latest APIs.

### Core Stack
*   **Language**: 100% Kotlin
*   **UI Framework**: Jetpack Compose (Material 3) with Adaptive Edge-to-Edge support.
*   **Target SDK**: 36 (Android 16)
*   **Architecture**: MVVM (Model-View-ViewModel) with Repository Pattern.

### Libraries & Integrations
*   **Dependency Injection**: Hilt (Dagger)
*   **Persistence**: Room Database (SQLite)
*   **Networking/Service**: WorkManager for background tasks and Foreground Services for persistent monitoring.
*   **Google Play Services**: Geofencing, Fused Location, Google Maps.
*   **Analytics & Stability**: Firebase (Analytics, Crashlytics) and Microsoft Clarity.
*   **Animations**: Lottie for high-quality, lightweight micro-animations.

---

## ⚙️ Setup & Installation

### 1. Prerequisites
*   Android Studio **Ladybug** or newer.
*   Android SDK 36 (Android 16 preview) installed.

### 2. Configuration
1.  **Clone the repository**:
    ```bash
    git clone https://github.com/dn-sandeeep/Silent-Zone.git
    ```
2.  **API Keys**:
    Create a `local.properties` file in the root directory and add your Google Maps API Key:
    ```properties
    MAPS_API_KEY=YOUR_GOOGLE_MAPS_API_KEY
    CLARITY_PROJECT_ID=YOUR_CLARITY_ID (Optional)
    ```

### 3. Build & Run
*   Sync Gradle and deploy to a physical device.
*   **Note**: Geofencing and WiFi detection are best tested on physical hardware.

---

## 🛡️ Permissions

SilentZone requires specific permissions to automate your ringer mode accurately:
*   `ACCESS_FINE_LOCATION` & `ACCESS_BACKGROUND_LOCATION`: For geofencing triggers.
*   `NEARBY_WIFI_DEVICES`: To identify SSIDs on Android 13+.
*   `ACCESS_NOTIFICATION_POLICY`: To manage system Do Not Disturb and ringer states.
*   `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_LOCATION`: To ensure monitoring remains active in the background.

---

## 🔒 Privacy & Security

*   **Offline First**: All sensitive data (Location history, WiFi SSIDs, Analytics) is processed and stored locally on your device using Room.
*   **Transparency**: Users can audit their battery and usage stats at any time within the app.
*   **No Data Harvesting**: SilentZone does not upload your location or personal network data to any external server.

---

## 🤝 Contributing

Contributions are welcome! If you have a feature request or found a bug, please open an issue or submit a pull request.

1.  Fork the Project.
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`).
3.  Commit your Changes (`git commit -m 'Add some AmazingFeature'`).
4.  Push to the Branch (`git push origin feature/AmazingFeature`).
5.  Open a Pull Request.

---

<p align="center">
  Made with ❤️ by Sandeep | <a href="https://github.com/dn-sandeeep/Silent-Zone">GitHub Repository</a>
</p>

