# Glyph Matrix Toy

An Android app for Nothing Phone 3 that displays real-time clock and battery status on the Glyph Matrix interface (16x32 display) using a pixelated retro style.

## Features

- **Real-time Clock Display**: Shows current time (hours and minutes) in a pixelated font on the 16x32 Glyph matrix
- **Battery Monitoring**: Displays battery percentage as a number and visual icon
- **Charging Animation**: Animated fill effect when the device is charging
- **Full Battery Pulse**: Pulsing effect when battery reaches 100%
- **Retro Aesthetic**: Matrix-style pixelated display matching classic digital displays

## Requirements

- Nothing Phone 3 with Glyph Matrix interface
- Android SDK 34 (Android 14) or higher
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 or higher
- Kotlin 1.9.20+
- Gradle 8.4+

## Project Structure

```
GlyphMatrixToy/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/wolfyslayer/glyphmatrixtoy/
│   │       │   ├── MainActivity.kt           # Main UI and logic
│   │       │   ├── MatrixRenderer.kt         # 16x32 matrix rendering engine
│   │       │   └── BatteryMonitor.kt         # Battery status monitoring
│   │       ├── res/
│   │       │   ├── layout/
│   │       │   │   └── activity_main.xml     # Main activity UI
│   │       │   ├── values/
│   │       │   │   ├── strings.xml
│   │       │   │   ├── colors.xml
│   │       │   │   └── themes.xml
│   │       │   └── mipmap-*/                 # App icons
│   │       └── AndroidManifest.xml
│   ├── build.gradle                          # App-level build config
│   └── proguard-rules.pro
├── build.gradle                              # Project-level build config
├── settings.gradle                           # Project settings
├── gradle.properties
└── README.md
```

## Building the Project

### Prerequisites

1. Install [Android Studio](https://developer.android.com/studio)
2. Install Android SDK with minimum API level 34
3. Install JDK 17 or higher
4. Install Kotlin plugin (usually bundled with Android Studio)

### CI/CD

This project includes a GitHub Actions workflow that automatically builds the app on every push to main and on pull requests. The workflow:
- Validates the Gradle wrapper
- Builds both debug and release APKs
- Runs lint checks
- Uploads build artifacts for download

You can download the latest build artifacts from the [Actions tab](https://github.com/Wolfyslayer/Glyph-matrix-toy/actions) in GitHub.

### Steps

1. **Clone the repository**:
   ```bash
   git clone https://github.com/Wolfyslayer/Glyph-matrix-toy.git
   cd Glyph-matrix-toy
   ```

2. **Open in Android Studio**:
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory and select it
   - Wait for Gradle sync to complete

3. **Build the APK**:
   - From menu: `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
   - Or use Gradle command:
     ```bash
     ./gradlew assembleDebug
     ```
   - APK will be located at: `app/build/outputs/apk/debug/app-debug.apk`

4. **Build signed release APK** (optional):
   - From menu: `Build` → `Generate Signed Bundle / APK`
   - Follow the wizard to create/use a keystore
   - Or configure signing in `app/build.gradle` and run:
     ```bash
     ./gradlew assembleRelease
     ```

## Installation

### Install via Android Studio

1. Connect your Nothing Phone via USB
2. Enable USB debugging in Developer Options
3. Click the "Run" button (green play icon) in Android Studio
4. Select your device from the list

### Install APK Manually

1. Transfer the APK to your Nothing Phone
2. Open the file manager and locate the APK
3. Tap to install (you may need to enable "Install from Unknown Sources")
4. Grant required permissions when prompted

### Required Permissions

The app requires the following permissions:
- **Glyph Access** (`com.nothing.ketchum.permission.LIGHT_GLYPH`): To control the Glyph interface
- **Battery Stats**: To read battery level and charging status

These are requested automatically on first run.

## Usage

1. **Launch the app** on your Nothing Phone
2. **Wait for initialization**: The app will initialize the Glyph interface (takes 1-2 seconds)
3. **Tap "Start Display"**: The Glyph matrix will begin showing:
   - Current time (HH:MM format) at the top
   - Battery percentage and icon at the bottom
4. **Observe animations**:
   - When charging: Battery icon fills from bottom to top in a loop
   - When full (100%): Battery icon pulses on and off
5. **Tap "Stop Display"**: Turns off the Glyph display
6. The app can run in the background and continue updating the display

## Technical Details

### Matrix Specifications

- **Size**: 16 rows × 32 columns (512 LEDs total)
- **Refresh Rate**: Updates every 1 second
- **Font**: Custom 3×5 pixel font for digits and symbols
- **Brightness**: Uses Glyph SDK brightness levels (0-4095)

### Dependencies

- **Nothing Glyph SDK** (local AAR in libs/): Official SDK for Glyph control
- **AndroidX Core KTX** (1.12.0): Kotlin extensions for Android
- **AndroidX AppCompat** (1.6.1): Backward-compatible Android APIs
- **Material Components** (1.11.0): Material Design UI components
- **ConstraintLayout** (2.1.4): Flexible layout management
- **Lifecycle Runtime KTX** (2.7.0): Lifecycle-aware components

### Build Configuration

- **Android Gradle Plugin**: 8.1.4
- **Kotlin**: 1.9.20
- **Gradle**: 8.4
- **Java Target**: 17
- **Min SDK**: 34 (Android 14)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34

### Architecture

- **MainActivity**: Handles UI, lifecycle, and coordinates updates
- **BatteryMonitor**: Listens to battery broadcast intents and provides status updates
- **MatrixRenderer**: Renders time/battery data to a 16×32 boolean matrix with animations

## Customization

### Changing Matrix Size

If you want to modify the matrix dimensions, edit `MatrixRenderer.kt`:

```kotlin
const val MATRIX_WIDTH = 32  // Change width
const val MATRIX_HEIGHT = 16 // Change height
```

### Modifying Update Interval

To change how often the display updates, edit `MainActivity.kt`:

```kotlin
private const val UPDATE_INTERVAL_MS = 1000L // Change to desired milliseconds
```

### Customizing Font

The 3×5 pixel font is defined in `MatrixRenderer.kt` in the `FONT_3x5` map. You can add or modify character glyphs:

```kotlin
'A' to listOf(
    0b111,
    0b101,
    0b111,
    0b101,
    0b101
),
```

## Contributing

Contributions are welcome! Please feel free to:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Ideas for Contributions

- Add more display modes (date, notifications, custom messages)
- Implement different animation patterns
- Add user preferences/settings
- Support for custom fonts
- Additional battery icon styles
- Weather display integration
- Music visualization mode

## Troubleshooting

### App won't install
- Ensure you're using a Nothing Phone 3 with Glyph Matrix interface
- Check that "Install from Unknown Sources" is enabled
- Verify USB debugging is enabled for ADB installation

### Glyph doesn't light up
- Make sure you granted the Glyph permission
- Check that no other app is currently using the Glyph interface
- Try restarting the app
- Ensure your Nothing Phone firmware is up to date

### App crashes on start
- Verify you're running Android 14 (API 34) or higher
- Check logcat in Android Studio for error messages
- Ensure the Glyph SDK version is compatible with your device

## Documentation

- **[BUILD.md](BUILD.md)**: Detailed build configuration and CI/CD documentation
- **[LICENSE](LICENSE)**: MIT License

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Nothing Technology Limited for the Glyph SDK
- Nothing Phone community for inspiration and support

## Contact

For issues, questions, or suggestions, please open an issue on GitHub.

---

**Note**: This app is designed specifically for Nothing Phone 3 with its Glyph Matrix interface (16x32 display). It will not function on Nothing Phone 1/2 (which have LED strip Glyph interfaces) or other devices.