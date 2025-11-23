# Build Configuration Guide

This document describes the build configuration for the Glyph Matrix Toy Android application.

## Build System

### Gradle

- **Gradle Version**: 8.4
- **Gradle Wrapper**: Validated and configured for reproducible builds
- **Distribution URL**: `https://services.gradle.org/distributions/gradle-8.4-bin.zip`

### Android Gradle Plugin (AGP)

- **Version**: 8.1.4
- **Compatibility**: Fully compatible with Gradle 8.4 and JDK 17

### Kotlin

- **Version**: 1.9.20
- **JVM Target**: 17
- **Compiler Optimizations**: Enabled with `-opt-in=kotlin.RequiresOptIn`

## SDK Requirements

### Target SDK Levels

- **minSdkVersion**: 33 (Android 13)
  - Supports Nothing Phone 3 on Android 13 and higher
  - Uses manifest merger to override Glyph SDK's minSdk requirement
  - Compatible with current and future Android versions
  
- **targetSdkVersion**: 35 (Android 15)
  - Targets latest stable Android version
  - Ensures app follows modern Android best practices
  
- **compileSdkVersion**: 35 (Android 15)
  - Compiles against latest Android APIs

### Java Version

- **Source Compatibility**: Java 17
- **Target Compatibility**: Java 17
- **Required JDK**: 17 or higher

## Build Features

### Enabled Features

- **View Binding**: Yes - Type-safe view access
- **Build Config**: Yes - Access build configuration in code
- **Vector Drawables**: Support library enabled

### Build Types

#### Debug Build

- **Debuggable**: Yes
- **Minification**: Disabled
- **Shrinking**: Disabled
- **Application ID Suffix**: `.debug`
- **Version Name Suffix**: `-debug`

#### Release Build

- **Debuggable**: No
- **Minification**: Enabled (R8)
- **Shrinking**: Resources shrunk
- **ProGuard**: Uses `proguard-android-optimize.txt` + custom rules
- **R8 Full Mode**: Enabled for maximum optimization

## ProGuard/R8 Rules

The project includes comprehensive ProGuard rules in `app/proguard-rules.pro`:

### Protected Classes

1. **Nothing Glyph SDK**
   - All classes in `com.nothing.ketchum.**` are kept
   - Required for proper SDK functionality

2. **Android Components**
   - Parcelable implementations
   - Serializable classes
   - Native methods
   - View constructors

3. **View Binding**
   - All ViewBinding classes are preserved
   - Required for layout inflation

4. **Application Classes**
   - All app classes in `com.wolfyslayer.glyphmatrixtoy.**` are kept
   - Ensures callbacks and reflection work correctly

### Debugging Attributes

- Source file information preserved
- Line numbers kept for stack traces
- Annotations maintained
- Generic signatures preserved for reflection

## Gradle Properties

### Performance Optimization

```properties
org.gradle.parallel=true          # Parallel project execution
org.gradle.caching=true           # Build cache enabled
org.gradle.configuration-cache=true  # Configuration cache for faster builds
org.gradle.jvmargs=-Xmx2048m      # 2GB heap for Gradle daemon
```

### Android-Specific

```properties
android.useAndroidX=true                    # Use AndroidX libraries
android.nonTransitiveRClass=true            # Optimized R class generation
android.enableR8.fullMode=true              # R8 full mode for better optimization
android.suppressUnsupportedCompileSdk=34    # Suppress SDK compatibility warnings
```

## Repository Management

### Plugin Management

Uses `pluginManagement` in `settings.gradle` with repositories:
- Google Maven (`google()`)
- Maven Central (`mavenCentral()`)
- Gradle Plugin Portal (`gradlePluginPortal()`)

### Dependency Resolution

Configured with `dependencyResolutionManagement`:
- **Mode**: `FAIL_ON_PROJECT_REPOS` - Enforces centralized repository declaration
- **Repositories**:
  - Google Maven
  - Maven Central
  - Nothing Glyph Developer Kit (GitHub Packages)

### Nothing Glyph SDK

The Glyph SDK is included as a local AAR file:
- **Location**: `app/libs/glyph-matrix-sdk-1.0.aar`
- **Reason**: Easier distribution and no authentication required
- **Alternative**: Can be fetched from GitHub Packages with authentication

## Dependencies

### Core Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| androidx.core:core-ktx | 1.12.0 | Kotlin extensions for Android APIs |
| androidx.appcompat:appcompat | 1.6.1 | Backward-compatible support |
| androidx.lifecycle:lifecycle-runtime-ktx | 2.7.0 | Lifecycle-aware components |

### UI Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| com.google.android.material:material | 1.11.0 | Material Design components |
| androidx.constraintlayout:constraintlayout | 2.1.4 | Flexible layouts |

### Testing Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| junit:junit | 4.13.2 | Unit testing |
| androidx.test.ext:junit | 1.1.5 | Android JUnit extensions |
| androidx.test.espresso:espresso-core | 3.5.1 | UI testing |

## CI/CD Configuration

### GitHub Actions Workflow

The project uses GitHub Actions for continuous integration:

#### Job: `build`

- **Runner**: `ubuntu-latest`
- **Timeout**: 30 minutes
- **JDK**: Temurin 17 with Gradle caching

#### Build Steps

1. **Checkout**: Uses `actions/checkout@v4`
2. **Java Setup**: Uses `actions/setup-java@v4`
3. **Android SDK**: Uses `android-actions/setup-android@v3`
4. **Wrapper Validation**: Uses `gradle/wrapper-validation-action@v1`
5. **Clean Build**: Runs `./gradlew clean`
6. **Debug APK**: Builds with `./gradlew assembleDebug`
7. **Release APK**: Builds with `./gradlew assembleRelease`
8. **Lint Check**: Runs `./gradlew lintDebug` (continues on error)

#### Artifacts

All builds upload artifacts with different retention periods:
- **Debug APK**: 30 days retention
- **Release APK**: 30 days retention
- **Lint Results**: 7 days retention

#### Environment Variables

- `GITHUB_ACTOR`: GitHub username for package access
- `GITHUB_TOKEN`: Automatically provided by GitHub Actions

## Building Locally

### Prerequisites

1. Install JDK 17 or higher
2. Install Android SDK (API 31-34)
3. Clone the repository

### Command Line Build

```bash
# Debug build
./gradlew assembleDebug

# Release build (unsigned)
./gradlew assembleRelease

# Run tests
./gradlew test

# Run lint
./gradlew lint

# Clean build
./gradlew clean
```

### Android Studio Build

1. Open the project in Android Studio Hedgehog or newer
2. Wait for Gradle sync to complete
3. Select build variant (debug/release)
4. Build → Build Bundle(s) / APK(s) → Build APK(s)

## Troubleshooting

### Build Fails with "Could not resolve..."

- Check internet connectivity
- Verify GitHub token if using GitHub Packages
- Try cleaning Gradle cache: `./gradlew clean --refresh-dependencies`

### "Unsupported class file version"

- Ensure JDK 17 is installed and selected
- Check `JAVA_HOME` environment variable

### ProGuard/R8 Issues

- Review rules in `app/proguard-rules.pro`
- Add `-dontobfuscate` temporarily for debugging
- Check stack traces for missing keep rules

### Gradle Daemon Issues

- Kill daemon: `./gradlew --stop`
- Clear cache: `rm -rf ~/.gradle/caches/`

## Best Practices

1. **Always validate wrapper**: Use `gradle/wrapper-validation-action` in CI
2. **Use Gradle caching**: Speeds up builds significantly
3. **Enable R8 full mode**: Better optimization and smaller APKs
4. **Keep ProGuard rules updated**: Add rules when using reflection or dynamic code
5. **Test release builds**: Always test minified/optimized builds before release
6. **Update dependencies regularly**: Keep libraries up-to-date for security and features

## Security Considerations

1. **No secrets in code**: Never commit API keys or passwords
2. **ProGuard enabled**: Obfuscates code in release builds
3. **Signed releases**: Always sign release APKs with a secure keystore
4. **Dependency scanning**: Regularly check for vulnerable dependencies
5. **Permissions**: Only request necessary Android permissions

## Version History

### Current Configuration (Latest)

- Gradle: 8.4
- AGP: 8.1.4
- Kotlin: 1.9.20
- JDK Target: 17
- Min SDK: 33
- Target SDK: 35
- Compile SDK: 35

## Resources

- [Android Gradle Plugin Release Notes](https://developer.android.com/build/releases/gradle-plugin)
- [Gradle Documentation](https://docs.gradle.org/)
- [Kotlin Releases](https://kotlinlang.org/docs/releases.html)
- [R8 Full Mode](https://developer.android.com/studio/build/shrink-code#full-mode)
- [ProGuard Manual](https://www.guardsquare.com/manual/configuration/usage)
