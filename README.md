# SilentZone 🤫📱

**SilentZone** is an intelligent Android automation application designed to seamlessly manage your phone's ringer modes (Silent, Vibrate, Normal) based on environmental context, location, and user activity. Built with modern Android development standards, it ensures your device stays quiet when it matters most and remains audible when you need it.

---

## 🚀 Key Features

### 📶 Smart WiFi Automation
- **Network-Aware Silencing**: Automatically switch to Silent or Vibrate mode when connecting to specific WiFi networks (e.g., Office, Library, University).
- **Dynamic Restoration**: Automatically restores your previous ringer mode upon disconnecting from a designated "Silent WiFi."

### 📍 Location-Based Geofencing
- **Interactive Map Selection**: Define custom silent zones directly on an interactive Google Map.
- **Background Triggering**: Uses the **Google Play Services Geofencing API** to trigger mode changes even when the app is closed or the device is in your pocket.
- **Battery Optimized**: Leverages low-power location monitoring to ensure minimal impact on battery life.

### 📞 Whitelist & Call Bypass
- **Important Contacts**: Mark specific contacts as "Important" to ensure their calls always ring, even if the device is in Silent mode.
- **Real-Time Interception**: Uses a `BroadcastReceiver` to monitor incoming calls and intelligently bypass system silence for whitelisted numbers.

### 📅 Meeting Mode (Calendar Integration)
- **Contextual Awareness**: Automatically silences the phone if the user is currently in a "Busy" event on their Google Calendar.
- **Auto-SMS (Optional)**: Can be configured to notify callers that you are currently in a meeting.

### ✨ Premium Visual Experience
- **Modern Dashboard**: A clean, Material 3-based interface with a bottom navigation system for easy access to Home, Zones, and Whitelist.
- **Custom Pulse Animations**: A dynamic, pure Jetpack Compose-based header that uses pulse rings and floating icons to visually represent the active mode.
- **Mode-Based Themes**: The UI dynamically changes its color palette (Purple for Silent, Teal for Vibrate, Blue for Normal) to reflect the device's state.

---

## 🛠️ Tech Stack & Architecture

SilentZone is built using the latest Android ecosystem tools and follows a clean, maintainable architecture.

### **Core Stack**
- **Language**: [Kotlin](https://kotlinlang.org/) (100%)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3.
- **Architecture**: **MVVM (Model-View-ViewModel)** for clear separation of concerns.
- **Asynchronous Logic**: Kotlin Coroutines & StateFlow for reactive UI updates.

### **Integrations & Libraries**
- **Google Play Services**: Geofencing, Fused Location Provider, and Google Maps SDK.
- **Maps Compose**: For modern, declarative Map integration in Compose.
- **Gson**: For lightweight data serialization of user preferences and zones.
- **Lottie**: For high-quality vector animations (used in auxiliary UI components).

### **Architectural Components**
- **ViewModel**: Manages UI state and business logic.
- **Repository**: Acts as a single source of truth for data (WiFi SSIDs, Location Zones, Contacts).
- **Broadcast Receivers**: Handle system events like `RINGER_MODE_CHANGED`, `WIFI_STATE_CHANGED`, and `PHONE_STATE`.
- **Managers**: Specialized classes like `SilentZoneGeofenceManager` and `AgentActionManager` to handle specific hardware/API interactions.

---

## ⚙️ Setup & Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/dn-sandeeep/Silent-Zone.git
   ```

2. **Google Maps API Key**:
   - Obtain an API Key from the [Google Cloud Console](https://console.cloud.google.com/).
   - Create a `local.properties` file in the root directory and add:
     ```properties
     MAPS_API_KEY=YOUR_API_KEY_HERE
     ```

3. **Build & Run**:
   - Open the project in **Android Studio (Ladybug or newer)**.
   - Sync Gradle and deploy to a physical device (Geofencing and WiFi scanning require a physical device for accurate testing).

---

## 🛡️ Required Permissions

To provide its intelligent automation, SilentZone requires the following permissions:
- `ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION`: For defining and detecting Geofence zones.
- `ACCESS_BACKGROUND_LOCATION`: Essential for location triggers to work when the app is not in the foreground.
- `NEARBY_WIFI_DEVICES`: Required on Android 13+ to identify WiFi SSIDs without needing location access at all times.
- `READ_CONTACTS`, `READ_PHONE_STATE`, `READ_CALL_LOG`: To enable the Whitelist/Important Contacts feature.
- `READ_CALENDAR`: For the Meeting Mode/Calendar integration.
- `ACCESS_NOTIFICATION_POLICY`: Required to programmatically change ringer modes (Do Not Disturb access).

---

## 🤝 Contributing
Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---
*Developed with ❤️ by [Sandeep Chauhan](https://github.com/dn-sandeeep)*
