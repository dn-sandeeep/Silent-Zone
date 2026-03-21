# SilentZone Project Plan & Roadmap

SilentZone is an intelligent Android application designed to automate phone ringer modes (Silent, Vibrate, Normal) based on environmental context, location, and user activity.

## 🚀 Current Features (Implemented)

### 1. Geofencing (Location-Based)
*   **Automatic Transitions:** Switches ringer mode when entering/exiting predefined physical zones.
*   **Zone Management:** Users can define specific coordinates and radii for "Silent Zones" (e.g., Office, Library, Cinema).

### 2. WiFi Connectivity Triggers
*   **SSID Awareness:** Automatically silences the phone when connected to specific WiFi networks.
*   **Reversion:** Restores normal mode when disconnected from the "Silent WiFi."

### 3. Calendar-Driven "Meeting Mode"
*   **Context Engine:** Analyzes Google Calendar events to detect "Busy" status.
*   **Auto-Silence:** Intercepts incoming calls during meetings and silences the ringer.
*   **Smart Auto-Reply:** Sends automated SMS responses to callers while the user is in a meeting.

### 4. Core Architecture
*   **Centralized Repository:** `SilentModeRepository` manages interaction with `AudioManager`.
*   **Action Manager:** `AgentActionManager` handles complex sequences like silencing + SMS auto-reply.
*   **UI Components:** Modern Jetpack Compose UI with interactive map selection and status animations.

---

## 🛠️ Upcoming Features (Roadmap)

### Phase 1: Enhanced Control & Customization
*   **Time-Based Scheduling:**
    *   "Quiet Hours" feature (e.g., Sleep Mode from 11 PM to 7 AM).
    *   Weekly recurring schedules (Work hours vs. Weekend).
*   **Important Whitelist (Emergency Bypass):**
    *   Allow specific contacts (Family, Boss) to ring through even in Silent/DND mode.
    *   "Repeat Caller" bypass (if someone calls 3 times in 5 minutes).

### Phase 2: Sensor & Peripheral Triggers
*   **Bluetooth Connectivity:**
    *   Switch to "Vibrate" when connected to Car Bluetooth.
    *   Switch to "Normal" when connected to Home Audio.
*   **Flip-to-Shhh:**
    *   Use the accelerometer/proximity sensor to silence the phone when placed face-down.

### Phase 3: Intelligent Messaging
*   **Zone-Specific Auto-Replies:**
    *   "I'm at the library, text me" (Library Zone).
    *   "Driving, will call back later" (Car Bluetooth).
*   **AI Context Analysis:** Use on-device NLP to determine if a calendar event is a "Meeting" vs. "Social" to decide whether to silence.

---

## 🏗️ Technical Architecture Improvements
*   **Dagger/Hilt Integration:** Refactor for better Dependency Injection.
*   **WorkManager:** Migrate background tasks (WiFi scanning, Geofence registration) to `WorkManager` for better battery efficiency.
*   **Room Database:** Persist all zones, schedules, and Important lists locally with robust data migration.

---

## ✅ Development Checklist
- [x] Geofencing Engine
- [x] WiFi Trigger Logic
- [x] Calendar Context Engine
- [ ] Time-Based Scheduler UI & Logic
- [x] Important Whitelist Contact Picker
- [ ] Bluetooth Receiver Implementation
- [ ] Sensor-based "Flip-to-Shhh"
- [ ] Enhanced Room Database Migration
