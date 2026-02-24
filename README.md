# SilentZone 🤫📱

**SilentZone** is a smart Android automation app that intelligently manages your phone's ringer modes (Silent, Vibrate, Normal) based on your environment. Whether you are at the office, a library, or a specific geographic location, SilentZone ensures your phone behaves exactly how you want it to, without manual intervention.

---

## 🚀 Features

### 📶 WiFi-Based Automation
- **Auto-Silence**: Automatically switch to Silent or Vibrate mode when you connect to a specific WiFi network (e.g., Home, Office).
- **Auto-Restore**: Switches back to Normal mode as soon as you disconnect or leave the WiFi zone.

### 📍 Location-Based Geofencing
- **Map Zones**: Define custom circular zones on a map using Google Maps.
- **Smart Entry/Exit**: Uses Google Play Services Geofencing API to trigger mode changes when you enter or leave a predefined area.
- **Battery Efficient**: Uses optimized background location tracking to minimize battery drain.

### ✨ Visual & Interactive UI
- **Modern Design**: Built entirely with **Jetpack Compose** and **Material 3**.
- **Lottie Animations**: Beautiful, smooth animations that visually represent the current ringer state (Open/Closed Gates, Pulsing icons).
- **One-Tap Controls**: Manually override settings directly from the home screen.

### 🛡️ Smart Permissions
- **DND Access**: Seamlessly handles "Do Not Disturb" policy access required for ringer control.
- **Context Awareness**: Background location and WiFi scanning permissions are handled with modern `ActivityResultContracts`.

---

## 🛠️ Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Asynchronous Logic**: Kotlin Coroutines & StateFlow
- **Google Services**: 
  - [Geofencing API](https://developers.google.com/location-context/geofencing)
  - [Fused Location Provider](https://developer.android.com/training/location/retrieve-current)
  - [Google Maps Compose](https://github.com/googlemaps/android-maps-compose)
- **Animations**: [Lottie for Android](https://github.com/airbnb/lottie-android)
- **Data Persistence**: SharedPreferences (for simple settings & zone data)

---

## ⚙️ Setup & Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/yourusername/SilentZone.git
   ```

2. **Google Maps API Key**:
   - Obtain an API Key from the [Google Cloud Console](https://console.cloud.google.com/).
   - Add it to your `local.properties` or `AndroidManifest.xml`:
     ```xml
     <meta-data
         android:name="com.google.android.geo.API_KEY"
         android:value="YOUR_API_KEY_HERE" />
     ```

3. **Build the project**:
   - Open in **Android Studio (Ladybug or newer)**.
   - Sync Gradle and run on a physical device (recommended for testing Geofences).

---

## 📝 Required Permissions

To function correctly, the app requires:
- `ACCESS_FINE_LOCATION`: For precise Geofencing.
- `ACCESS_BACKGROUND_LOCATION`: To trigger zones while the app is closed.
- `NEARBY_WIFI_DEVICES`: To detect specific SSIDs on Android 13+.
- `ACCESS_NOTIFICATION_POLICY`: To change ringer modes (Do Not Disturb access).

---

## 🤝 Contributing
Contributions are welcome! If you find a bug or have a feature request, please open an issue or submit a pull request.

## 📄 License
This project is licensed under the MIT License.

---
*Developed with ❤️ by Sandeep Chauhan*
