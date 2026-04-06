# SilentZone 🤫📱

<p align="center">
  <img src="https://raw.githubusercontent.com/dn-sandeeep/Silent-Zone/main/assets/silent_zone_app_icon_1775393820851.png" width="180" alt="SilentZone Icon">
  <br>
  <b>Intelligent Ringer Automation for a Quieter, Meticulous Life.</b>
  <br>
  <i>Built with Jetpack Compose, Material 3, and Google Play Services 🚀</i>
</p>

---

## 📺 Project Walkthrough & Demo
> [!TIP]
> **Experience SilentZone in Action!** Click the preview below to see how SilentZone seamlessly transitions between modes based on your environment.

<p align="center">
  <a href="YOUR_VIDEO_URL_HERE">
    <img src="https://raw.githubusercontent.com/dn-sandeeep/Silent-Zone/main/assets/silent_zone_dashboard_mockup_1775393847082.png" width="800" alt="SilentZone Dashboard Mockup">
  </a>
  <br>
  <i>(Replace with your screen recording or GIF)</i>
</p>

---

## 🌟 Key Features (Detailed)

### 📶 1. Smart WiFi Engine
SilentZone doesn't just switch modes; it remembers.
- **SSID Awareness**: Automatically switch to **Silent** or **Vibrate** mode when you connect to designated "Quiet Networks" (e.g., *University_5G*, *Office_Corp*).
- **Intelligent Restoration**: Once you disconnect from a "Silent WiFi," the app restores your device to its **previous ringer state** (Normal, Vibrate, or Silent), ensuring you never miss a call after leaving a meeting.
- **Low Impact**: Monitors `WIFI_STATE_CHANGED` broadcasts to minimize battery drain.

### 📍 2. Proactive Geofencing
Define physical zones where silence is mandatory.
- **Interactive Map Selection**: Use the integrated Google Maps interface to drop pins and define radii (50m to 1km) for custom Silent Zones.
- **Background Transitions**: Powered by the **Google Play Services Geofencing API**, SilentZone triggers mode changes even when the app is killed or the device is in your pocket.
- **Fused Location Provider**: Leverages high-accuracy location data with optimized battery algorithms.

### 📅 3. Meeting Mode (Context Engine)
Your calendar is the master of your ringer.
- **Calendar Observer**: Automatically silences the phone if your Google Calendar shows a "Busy" event.
- **Smart SMS Auto-Reply**: Optionally send an automated text message to callers while you are in a meeting.
- **Zero Configuration**: Once enabled, it works silently in the background, syncing with your primary calendar.

### 📞 4. Emergency Whitelist (Dynamic Bypass)
Never miss what truly matters.
- **Priority Contacts**: Add family or emergency contacts to your "Important List."
- **Call Interception**: Uses a `BroadcastReceiver` to monitor incoming `PHONE_STATE`. If a whitelisted number calls, SilentZone temporarily overrides system silence to let the call ring through.

---

## 🎨 Premium Visual Experience
SilentZone isn't just functional; it's a delight to look at.
- **Pulse Status Header**: A dynamic, pure Jetpack Compose-based dashboard that uses pulsing rings and floating icons to visually represent your active mode.
- **Adaptive Theming**: The entire UI color palette shifts based on your state (Purple for Silent, Teal for Vibrate, Blue for Normal).
- **Material 3 Design**: Fully compliant with modern Android design standards, featuring smooth transitions and haptic feedback.

---

## 🛠️ Tech Stack & Architecture
SilentZone follows a clean, maintainable architecture.
- **Language**: Kotlin (100%)
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM with Clean Architecture principles.
- **Integrations**: 
    - **Google Play Services**: Geofencing, Fused Location, Maps.
    - **Maps Compose**: For modern Map integration.
    - **Coroutines & StateFlow**: For reactive state management.

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
   - Open in **Android Studio (Ladybug or newer)**.
   - Sync Gradle and deploy to a physical device (recommended for WiFi/Geofencing testing).

---

## 🛡️ Required Permissions
SilentZone requires the following for its automation:
- `ACCESS_FINE_LOCATION` & `ACCESS_BACKGROUND_LOCATION`: For Geofence triggers.
- `NEARBY_WIFI_DEVICES`: To identify SSIDs on Android 13+.
- `READ_CONTACTS` & `READ_PHONE_STATE`: For the Whitelist feature.
- `READ_CALENDAR`: For Meeting Mode.
- `ACCESS_NOTIFICATION_POLICY`: To manage Do Not Disturb modes.

---

## 🛡️ Privacy First
**SilentZone processes all location, WiFi, and contact data offline on your device.** No personal data is ever uploaded to external servers.

---

## 🚀 Roadmap & Future Plans

SilentZone aims to go beyond basic automation. We are planning to implement several "Agentic" features to make the app a true personal assistant:

- **📡 WiFi Proximity (Signal-Based)**: Trigger silent mode as soon as an office/university WiFi signal is detected, *before* your device actually connects.
- **🔊 Adaptive Smart Volume**: Use the device's microphone to sense ambient noise levels. Automatically increase ring volume in loud environments and soften it in quiet libraries.
- **✉️ NLP Notification Filtering**: Local AI analysis of notification content. Silence everything except messages containing "Urgent", "OTP", or "Emergency".
- **🎒 Pocket-Aware Haptics**: Detect if the phone is in a bag or pocket using proximity and light sensors. Switch to high-intensity "Burst Vibration" to ensure calls aren't missed.
- **🎬 App-Based Silence (Theater Mode)**: Automatically mute media volume when specific entertainment apps (YouTube, Netflix) are opened in public environments.
- **🚪 Exit Confirmation**: When leaving a "Silent Zone," the app will ask for confirmation before restoring the volume to prevent embarrassing loud-ring moments.
- **🚆 Commute Intelligence**: Motion-based social media muting. If traveling at high speeds, social notifications are muted to keep you focused.

---

## 🤝 Contributing

---
<p align="center">Made with ❤️ for a quieter world.</p>
