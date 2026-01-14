# Samsung Test Autoload Mic Record App

An Android proof-of-concept application that demonstrates boot-triggered audio recording and keyword detection. The app automatically starts when the phone boots, continuously listens for the wake word "SuperDuper", and converts subsequent speech to text.

## Overview

This app serves as a technical showcase for:
- Auto-start capabilities on Android boot
- Continuous background audio monitoring via foreground service
- Wake word detection using Android SpeechRecognizer API
- Speech-to-text transcription
- Data persistence with Room database

## Features

### Core Functionality
- **Auto-Start on Boot**: App automatically starts when device finishes booting
- **Continuous Listening**: Foreground service keeps app running and listening
- **Wake Word Detection**: Listens for "SuperDuper" (case-insensitive)
- **Speech Transcription**: Converts speech to text after wake word detected
- **Silence Detection**: Automatically stops recording after 2 seconds of silence
- **History Management**: Stores last 20 transcriptions with timestamps
- **Permission Handling**: Runtime permission requests for microphone and notifications

### Technical Highlights
- **Architecture**: MVVM pattern with Repository
- **UI**: Jetpack Compose with Material Design 3
- **Database**: Room (SQLite) for local persistence
- **Service**: Foreground service with persistent notification
- **Speech Recognition**: Android SpeechRecognizer API (online)

## Prerequisites

### Development Environment
- **Android Studio**: Hedgehog (2023.1.1) or newer
- **JDK**: Version 17 or higher
- **Android SDK**: API Level 34 (Android 14)
- **Gradle**: 8.0 or higher (handled by wrapper)

### Testing Device Requirements
- **Android Version**: 8.0 (Oreo, API 26) or higher
- **Hardware**: Device with working microphone
- **Google Play Services**: Required for SpeechRecognizer functionality
- **Internet Connection**: Required for online speech recognition

## Setup Instructions

### 1. Clone and Setup

```bash
# Navigate to project directory
cd /path/to/project

# Run setup script
chmod +x init.sh
./init.sh
```

### 2. Open in Android Studio

1. Launch Android Studio
2. Select **File > Open**
3. Navigate to and select the `android-app` folder
4. Wait for Gradle sync to complete
5. Allow Android Studio to download any missing SDK components

### 3. Configure Device/Emulator

**For Physical Device:**
1. Enable Developer Options on your device
2. Enable USB Debugging
3. Connect device via USB
4. Accept debugging authorization on device

**For Emulator:**
1. Open AVD Manager in Android Studio
2. Create a new device (Pixel 5 or similar recommended)
3. Select system image: API 34 (Android 14)
4. Ensure "Google APIs" is included (for SpeechRecognizer)
5. Start the emulator

### 4. Build and Run

**Using Android Studio:**
1. Select your device/emulator from the device dropdown
2. Click the **Run** button (green play icon) or press **Shift+F10**
3. Wait for app to build and install

**Using Command Line:**
```bash
cd android-app
./gradlew clean build
./gradlew installDebug
```

## Usage

### First Launch

1. App will request **microphone permission** - tap **Allow**
2. On Android 13+, app will request **notification permission** - tap **Allow**
3. App will display "Listening..." status
4. Persistent notification will appear: "Samsung Mic Recorder - Listening for wake word..."

### Using the App

1. **Trigger Wake Word**: Say "SuperDuper" clearly
2. **Speak Your Command**: After wake word is detected, say your phrase
3. **Wait for Silence**: Stop speaking and wait 2 seconds
4. **View Transcription**: Your transcribed text will appear in the history list

### Example Usage

```
You: "SuperDuper"
[App detects wake word]

You: "Turn on the lights in the living room"
[2 seconds of silence]

[App displays: "Turn on the lights in the living room" with timestamp]
```

### Testing Auto-Start on Boot

1. Ensure app is installed and permissions are granted
2. Restart device:
   ```bash
   adb reboot
   ```
3. After boot completes, check notification drawer
4. Verify "Samsung Mic Recorder" notification is present
5. Say "SuperDuper" to test functionality

## Project Structure

```
android-app/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/samsung/micrecorder/
│   │   │   │   ├── MainActivity.kt              # Main UI activity
│   │   │   │   ├── MicRecorderService.kt        # Foreground service
│   │   │   │   ├── BootCompletedReceiver.kt     # Boot broadcast receiver
│   │   │   │   ├── viewmodel/
│   │   │   │   │   └── MainViewModel.kt         # UI state management
│   │   │   │   ├── repository/
│   │   │   │   │   └── TranscriptionRepository.kt
│   │   │   │   ├── database/
│   │   │   │   │   ├── AppDatabase.kt           # Room database
│   │   │   │   │   ├── TranscriptionHistory.kt  # Entity
│   │   │   │   │   └── TranscriptionDao.kt      # DAO
│   │   │   │   └── ui/
│   │   │   │       └── composables/             # Compose UI components
│   │   │   ├── res/
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   └── themes.xml
│   │   │   │   └── drawable/
│   │   │   └── AndroidManifest.xml              # Permissions & components
│   │   └── test/                                # Unit tests
│   ├── build.gradle.kts                         # App-level build config
│   └── proguard-rules.pro
├── gradle/
│   └── wrapper/
├── build.gradle.kts                             # Project-level build
├── settings.gradle.kts
└── gradle.properties
```

## Permissions

The app requires the following permissions:

| Permission | Type | Purpose |
|------------|------|---------|
| `RECEIVE_BOOT_COMPLETED` | Normal | Start service on device boot |
| `RECORD_AUDIO` | Dangerous | Access microphone for speech recognition |
| `FOREGROUND_SERVICE` | Normal | Run service in foreground |
| `FOREGROUND_SERVICE_MICROPHONE` | Normal | Specify microphone use in foreground service (Android 14+) |
| `POST_NOTIFICATIONS` | Dangerous | Show persistent notification (Android 13+) |
| `INTERNET` | Normal | Online speech recognition via Google services |

## Design Specifications

### Color Palette
- **Primary**: Light Blue (#03A9F4)
- **Background**: Black (#000000)
- **Text Primary**: White (#FFFFFF)
- **Text Secondary**: Gray (#9E9E9E)
- **Success/Active**: Green (#4CAF50) / Light Blue (#03A9F4)
- **Error**: Red (#F44336)
- **Warning**: Yellow (#FFC107)

### Typography
- **App Title**: 24sp, Bold, White
- **Version**: 14sp, Regular, Gray
- **Status Text**: 16sp, Medium, White
- **History Text**: 16sp, Regular, White
- **Timestamp**: 12sp, Regular, Gray
- **Error Text**: 16sp, Regular, Red

### Spacing
- **Screen Padding**: 16dp
- **Section Spacing**: 24dp
- **Item Spacing**: 12dp
- **Icon-Text Spacing**: 8dp

## Troubleshooting

### App Not Starting on Boot
- **Check permission**: Ensure RECEIVE_BOOT_COMPLETED is granted
- **Verify registration**: Check AndroidManifest.xml has receiver properly declared
- **Test broadcast**: Use `adb shell am broadcast -a android.intent.action.BOOT_COMPLETED` to test

### Speech Recognition Not Working
- **Internet connection**: SpeechRecognizer requires active internet
- **Google Play Services**: Ensure device has Google Play Services installed
- **Microphone permission**: Verify permission is granted in app settings
- **Check logs**: Run `adb logcat | grep SpeechRecognizer` for errors

### Service Keeps Stopping
- **Battery optimization**: Disable battery optimization for this app in device settings
- **Foreground service**: Verify notification is showing (indicates service is in foreground)
- **Check return value**: Ensure onStartCommand returns START_STICKY

### Wake Word Not Detected
- **Speak clearly**: Say "SuperDuper" clearly with good pronunciation
- **Check volume**: Ensure device volume is adequate and microphone isn't blocked
- **Background noise**: Reduce ambient noise for better recognition
- **Case sensitivity**: Detection is case-insensitive, but clarity matters

## Development Commands

### ADB Useful Commands
```bash
# View all logs
adb logcat

# Filter app logs
adb logcat | grep MicRecorder

# Clear logs
adb logcat -c

# Check installed packages
adb shell pm list packages | grep samsung

# Check running services
adb shell dumpsys activity services

# Force stop app
adb shell am force-stop com.samsung.micrecorder

# Uninstall app
adb uninstall com.samsung.micrecorder

# Install APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Check memory usage
adb shell dumpsys meminfo com.samsung.micrecorder

# Simulate boot (requires root)
adb shell am broadcast -a android.intent.action.BOOT_COMPLETED
```

### Gradle Commands
```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Check dependencies
./gradlew app:dependencies
```

## Known Limitations

1. **Online Recognition**: Requires internet connection for speech-to-text
2. **Google Play Services**: Won't work on devices without Google Play Services
3. **English Only**: Currently configured for English (en-US) only
4. **Battery Usage**: Continuous listening may impact battery life
5. **Recognition Accuracy**: Depends on Google's SpeechRecognizer API accuracy
6. **Background Restrictions**: Some Android OEMs aggressively kill background services

## Future Enhancements

- Offline speech recognition support
- Multi-language support
- Customizable wake word
- Export history to file
- Search and filter in history
- Voice feedback after command
- Integration with smart home APIs

## Testing Checklist

- [ ] App builds without errors
- [ ] All permissions declared in manifest
- [ ] Boot receiver starts service
- [ ] Foreground notification displays
- [ ] Microphone permission requested and handled
- [ ] Wake word "SuperDuper" detected
- [ ] Speech transcribed accurately
- [ ] History persists across app restarts
- [ ] History limited to 20 entries
- [ ] Timestamps formatted correctly
- [ ] Errors displayed in red
- [ ] Service survives app being swiped away
- [ ] Works on Android 8.0 through 14
- [ ] Memory usage remains stable

## License

This is a proof-of-concept demonstration application for Samsung.

## Support

For issues or questions, please refer to:
- Project specification: `app_spec.txt`
- Setup script: `init.sh`
- Feature list: Check features.db via MCP tools

---

**Version**: 1.0.0
**Last Updated**: January 2025
**Target SDK**: Android 14 (API 34)
**Minimum SDK**: Android 8.0 (API 26)
