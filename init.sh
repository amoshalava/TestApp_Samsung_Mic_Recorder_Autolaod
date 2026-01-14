#!/bin/bash

# init.sh - Samsung Test Autoload Mic Record App
# This script sets up the development environment for the Android app

set -e

echo "=================================="
echo "Samsung Test Autoload Mic Record App"
echo "Android Development Setup"
echo "=================================="
echo ""

# Check if we're in the right directory
if [ ! -f "app_spec.txt" ]; then
    echo "Error: app_spec.txt not found. Please run this script from the project root."
    exit 1
fi

echo "Step 1: Checking prerequisites..."
echo ""

# Check for Java/JDK
if ! command -v java &> /dev/null; then
    echo "⚠️  WARNING: Java not found. Please install JDK 17 or higher."
    echo "   Download from: https://adoptium.net/"
else
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    echo "✓ Java found (version $JAVA_VERSION)"
fi

# Check for Android Studio / SDK
if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
    echo "⚠️  WARNING: ANDROID_HOME not set. Please install Android Studio and set up SDK."
    echo "   Download from: https://developer.android.com/studio"
    echo ""
    echo "   After installing, add to your ~/.bashrc or ~/.zshrc:"
    echo "   export ANDROID_HOME=\$HOME/Android/Sdk"
    echo "   export PATH=\$PATH:\$ANDROID_HOME/emulator:\$ANDROID_HOME/platform-tools"
else
    echo "✓ ANDROID_HOME set to: ${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
fi

# Check for adb
if ! command -v adb &> /dev/null; then
    echo "⚠️  WARNING: adb not found. Add Android SDK platform-tools to PATH."
else
    echo "✓ adb found"
fi

echo ""
echo "Step 2: Project structure..."
echo ""

# Create Android project directory structure if it doesn't exist
if [ ! -d "android-app" ]; then
    echo "Creating Android project structure..."
    mkdir -p android-app/app/src/main/{java/com/samsung/micrecorder,res/values,res/drawable}
    mkdir -p android-app/app/src/main/res/{layout,mipmap-hdpi,mipmap-mdpi,mipmap-xhdpi,mipmap-xxhdpi,mipmap-xxxhdpi}
    mkdir -p android-app/gradle/wrapper
    echo "✓ Project structure created"
else
    echo "✓ Android project structure exists"
fi

echo ""
echo "Step 3: Build instructions..."
echo ""

echo "To build and run this app:"
echo ""
echo "Option 1 - Using Android Studio (Recommended):"
echo "  1. Open Android Studio"
echo "  2. File > Open > Select 'android-app' folder"
echo "  3. Wait for Gradle sync to complete"
echo "  4. Connect Android device or start emulator"
echo "  5. Click 'Run' button (green play icon)"
echo ""
echo "Option 2 - Using Command Line:"
echo "  cd android-app"
echo "  ./gradlew build              # Build the project"
echo "  ./gradlew installDebug       # Install on connected device"
echo "  ./gradlew assembleDebug      # Create APK (output in app/build/outputs/apk/)"
echo ""
echo "Step 4: Testing the app..."
echo ""
echo "After installation:"
echo "  1. Grant microphone permission when prompted"
echo "  2. Grant notification permission (Android 13+)"
echo "  3. App will start listening for wake word: 'SuperDuper'"
echo "  4. Say 'SuperDuper' followed by your command"
echo "  5. Transcription will appear in the history list"
echo ""
echo "To test boot functionality:"
echo "  adb reboot    # Restart the device"
echo "  # After boot, app should start automatically"
echo ""
echo "Step 5: Development tools..."
echo ""
echo "Useful adb commands:"
echo "  adb logcat                           # View logs"
echo "  adb logcat | grep MicRecorder        # Filter app logs"
echo "  adb shell pm list packages | grep samsung  # Verify app installed"
echo "  adb shell dumpsys activity services  # Check service status"
echo "  adb shell am force-stop <package>    # Force stop app"
echo ""
echo "=================================="
echo "Setup complete! Ready to develop."
echo "=================================="
echo ""
echo "Required Android SDK Components:"
echo "  - Android SDK Platform 34 (Android 14)"
echo "  - Android SDK Build-Tools 34.0.0 or higher"
echo "  - Android Emulator (if testing on emulator)"
echo ""
echo "Physical Device Requirements:"
echo "  - Android 8.0 (API 26) or higher"
echo "  - Microphone capability"
echo "  - Google Play Services (for SpeechRecognizer)"
echo ""
echo "Next Steps:"
echo "  1. Review README.md for detailed documentation"
echo "  2. Check app_spec.txt for full specifications"
echo "  3. Open android-app in Android Studio to begin development"
echo ""
