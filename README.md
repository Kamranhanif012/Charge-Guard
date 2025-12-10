# Battery Alarm Monitor - Android App

A lightweight Android application that monitors your device's battery level and sounds an alarm when it reaches 100% to prevent overcharging.

## Features

✅ **Battery Monitoring** - Continuously monitors battery level in the background
✅ **Smart Alarm** - Triggers only when battery reaches exactly 100%
✅ **Foreground Service** - Runs reliably in background without interrupting other apps
✅ **Custom Volume Control** - Adjust alarm volume to your preference
✅ **Persistent Notification** - Shows status notifications while monitoring
✅ **Stop/Snooze Controls** - Easy to dismiss the alarm
✅ **Lightweight** - Minimal battery and memory footprint
✅ **Android 8.0+** - Supports all modern Android versions

## Requirements

- Android SDK: API 26 (Android 8.0) - API 34 (Android 14)
- Kotlin 1.9.10
- Jetpack Compose 1.6.0

## Permissions

- `BATTERY_STATS` - Read battery information
- `FOREGROUND_SERVICE` - Run service in background
- `SCHEDULE_EXACT_ALARM` - Trigger alarms
- `POST_NOTIFICATIONS` - Show notifications

## Installation

1. Clone or download this project
2. Open in Android Studio (2023.1 or later)
3. Build and run on your device

## How to Use

1. Open the app
2. Tap "Start Monitoring" to begin battery monitoring
3. The app will run in the background
4. When your battery reaches 100%, the alarm will sound
5. Tap "Stop Alarm" to dismiss the notification and stop the sound
6. Adjust volume slider to set your preferred alarm volume

## Architecture

- **MainActivity** - UI layer with Jetpack Compose
- **BatteryMonitorService** - Foreground service for background monitoring
- **BatteryReceiver** - Listens for battery level changes
- **Theme** - Material Design 3 styling

## Technical Details

- Uses `BroadcastReceiver` for real-time battery updates
- Implements `ForegroundService` for Android 12+ compliance
- Stores preferences in `SharedPreferences`
- Plays system alarm tone or user-selected tone
- Respects system volume settings and Doze mode restrictions

## Build & Run

```bash
./gradlew build
./gradlew installDebug
```

## License

Open source project - Free to use and modify
