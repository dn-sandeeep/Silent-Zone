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

### Phase 1: Deep Engagement & Gamification (Extreme Priority)
*   **Focus Analytics & Zen Journey:**
    *   **Zen Levels & Progression:** Leveling system (Seedling -> Sprout -> Tree) based on total focus hours.
    *   **Interactive Dashboard:** Weekly comparison graphs showing "Time Saved" and "Interruption Heatmaps."
    *   **Focus Streaks:** Daily streak counters and rewards to motivate daily usage.
    *   **Smart Nudges:** Gentle notifications if the user uses the phone too much while in a Silent Zone.
*   **Deep Work Tools:**
    *   **Deep Work Timer:** Integration of a Pomodoro-style timer when entering a Silent Zone.
    *   **Ambient Focus Sounds:** Optional background White Noise, Rain, or Lo-fi beats while in Focus Mode.
*   **Social & Personalization:**
    *   **Adaptive Themes:** Unlocking special app skins (e.g., "Golden Zen") as the user levels up.
    *   **Zen Leaderboard:** Compare focus streaks and hours with friends.
    *   **Shareable Achievements:** Beautifully designed focus summaries for social media.
*   **Calendar Sync (Meeting Mode):**
    *   Automatic silence based on "Busy" status in Google Calendar.

### Phase 2: Enhanced Control & Customization
*   **Time-Based Scheduling:**
    *   "Quiet Hours" feature (e.g., Sleep Mode from 11 PM to 7 AM).
    *   Weekly recurring schedules (Work hours vs. Weekend).
*   **Important Whitelist (Emergency Bypass):**
    *   Allow specific contacts to ring through.
    *   "Repeat Caller" bypass logic.

### Phase 3: Sensor & Peripheral Triggers
*   **Bluetooth Connectivity:** Auto-switch modes based on connected Bluetooth devices (Car, Home Audio).
*   **Flip-to-Shhh:** Silence phone when placed face-down on a surface.

---

## 🏗️ Technical Architecture Improvements
*   **Dagger/Hilt Integration:** Refactor for better Dependency Injection.
*   **WorkManager:** Background task optimization for battery efficiency.
*   **Room Database:** Persistent storage for Focus analytics, Zones, and Whitelists.

---

## ✅ Development Checklist
- [x] Geofencing Engine
- [x] WiFi Trigger Logic
- [x] Calendar Context Engine
- [ ] Time-Based Scheduler UI & Logic
- [x] Important Whitelist Contact Picker
- [ ] Deep Work Timer Logic
- [ ] Focus Analytics Database (Room)
- [ ] Ambient Sound Player Implementation
- [ ] Sensor-based "Flip-to-Shhh"
