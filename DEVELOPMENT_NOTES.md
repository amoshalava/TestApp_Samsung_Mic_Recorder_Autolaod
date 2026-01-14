# Development Notes for Future Coding Agents

## Project Type: Android Native Application

This project is **NOT a web application**. It is an **Android mobile application** written in Kotlin using Jetpack Compose.

### Important Considerations for Testing

The standard coding prompt assumes browser-based testing with tools like Playwright. However, this is an **Android app** that must be:

1. **Built using Android Studio or Gradle** - not a web server
2. **Tested on Android devices or emulators** - not in a web browser
3. **Verified using Android-specific tools** - ADB, Android Studio, etc.

### Testing Methodology

Since browser automation tools are not applicable, testing should follow Android best practices:

#### Manual Testing Approach
- Install app on Android device/emulator using `adb install` or Android Studio
- Use `adb logcat` to monitor logs and debug output
- Verify features through actual device interaction
- Check service status with `adb shell dumpsys`
- Test boot behavior with `adb reboot`

#### Instrumented Testing (Future)
- Create instrumented tests using Espresso for UI testing
- Use JUnit for unit tests of logic components
- Test service behavior with ServiceTestRule
- Mock SpeechRecognizer for automated tests

### Current Development Stage

**Status**: Initial setup complete with 55 features defined

**Completed**:
- Created 55 test features covering all aspects of the app
- Set up Android project structure with proper directory layout
- Created build configuration files (Gradle)
- Configured AndroidManifest.xml with all required permissions
- Created resource files (strings, colors, themes)
- Initialized git repository

**Not Yet Started**:
- Actual Kotlin code implementation
- Room database entity and DAO classes
- Foreground service (MicRecorderService)
- Boot receiver (BootCompletedReceiver)
- UI components with Jetpack Compose
- ViewModel and Repository layers
- SpeechRecognizer integration
- Wake word detection logic

### Key Technical Requirements

1. **Permissions**: Must handle runtime permissions for RECORD_AUDIO and POST_NOTIFICATIONS
2. **Foreground Service**: Must create persistent notification to keep service alive
3. **Boot Detection**: BroadcastReceiver must start service on BOOT_COMPLETED
4. **Speech Recognition**: Uses Android SpeechRecognizer API (requires internet and Google Play Services)
5. **Database**: Room database limited to 20 entries with automatic cleanup
6. **Wake Word**: Continuous listening for "SuperDuper" before capturing speech

### Testing Features Without Browser Automation

Since the 55 features were designed with testing in mind, here's how to verify them:

**For Functional Features**:
- Code review: Verify implementation matches specification
- Manual testing: Install and test on actual device
- Log verification: Check logcat output for expected behavior
- Database inspection: Use ADB shell to query SQLite database
- Service status: Verify with `adb shell dumpsys activity services`

**For Style Features**:
- Visual inspection: Launch app and verify UI appearance
- Screenshot comparison: Take screenshots and verify colors, spacing, fonts
- Layout inspection: Use Android Studio Layout Inspector
- Accessibility: Use Android Accessibility Scanner

### Development Workflow for Next Sessions

1. **Start with core infrastructure**:
   - Room database (entity, DAO, database class)
   - Repository pattern
   - ViewModel for UI state

2. **Build the service layer**:
   - MicRecorderService with foreground notification
   - BootCompletedReceiver
   - SpeechRecognizer integration

3. **Implement UI**:
   - MainActivity with Compose
   - Permission handling UI
   - History list display
   - Status indicators

4. **Test incrementally**:
   - Test each component as it's built
   - Use adb logcat to verify behavior
   - Install on device frequently to catch issues early

### Important Notes

- **No mock data allowed**: All transcriptions must come from real speech recognition
- **Real device strongly recommended**: Emulator may have issues with microphone/speech recognition
- **Google Play Services required**: Speech recognition won't work without it
- **Internet required**: SpeechRecognizer uses online recognition by default

### Resources for Future Agents

- Android Developer Documentation: https://developer.android.com
- Jetpack Compose: https://developer.android.com/jetpack/compose
- Room Database: https://developer.android.com/training/data-storage/room
- SpeechRecognizer: https://developer.android.com/reference/android/speech/SpeechRecognizer
- Foreground Services: https://developer.android.com/guide/components/foreground-services

### Feature Verification Strategy

Since automated browser testing isn't available, use this approach:

1. **Get next feature** using `feature_get_next`
2. **Implement the feature** in Kotlin code
3. **Build and install** on device: `./gradlew installDebug`
4. **Manually verify** the feature works as specified
5. **Document verification** in progress notes with:
   - What was tested
   - How it was verified (e.g., "Checked logcat output", "Visually verified UI")
   - Screenshot or log snippet if relevant
6. **Mark as passing** using `feature_mark_passing` only after thorough verification

This approach ensures quality while acknowledging the Android-specific testing constraints.
