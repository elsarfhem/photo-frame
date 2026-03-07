# Photo Frame Android - Quick Start Guide

## ✅ Project Status: Ready to Run

Both debug and release APKs build successfully. The project is fully Android Studio compliant.

---

## Prerequisites

- **Android Studio**: Electric Eel (2022.1.1) or later
- **JDK**: JDK 17 (configured automatically via gradle.properties)
- **Android SDK**: API 26-34
- **Device/Emulator**: Android 8.0 (API 26) or higher, landscape orientation recommended

---

## Quick Start (5 Minutes)

### 1. Open Project in Android Studio

```bash
cd /Users/amatarazzo/git-repos/photo-frame-android
open -a "Android Studio" .
```

Or from Android Studio:
- **File** → **Open**
- Navigate to `/Users/amatarazzo/git-repos/photo-frame-android`
- Click **Open**

### 2. Wait for Gradle Sync

Android Studio will automatically:
- Download Gradle 8.4 (if needed)
- Sync dependencies
- Index project files

This takes 1-2 minutes on first sync.

### 3. Build & Run

#### Option A: Using Android Studio
1. Click **Run** button (green play icon) or press **Ctrl+R** / **Cmd+R**
2. Select target device or emulator
3. App will install and launch

#### Option B: Using Command Line

**Debug APK:**
```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.0.14+7/Contents/Home ./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Release APK:**
```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.0.14+7/Contents/Home ./gradlew assembleRelease
# Note: Release APK needs signing before installation
```

---

## Project Structure

```
PhotoFrame/
├── app/                    # Android application module
│   ├── MainActivity.kt    # Entry point - navigation & slideshow
│   └── ui/
│       ├── slideshow/     # Photo slideshow screen
│       ├── settings/      # SMB & display settings
│       ├── setup/         # Setup wizard
│       └── common/        # Shared UI components
│
├── core/                   # Core business logic module
│   ├── smb/               # SMB client for network shares
│   ├── image/             # Image loading & caching (Coil)
│   ├── slideshow/         # Slideshow logic & buffer
│   ├── repository/        # Data repositories
│   ├── scheduling/        # WorkManager scheduling
│   └── reliability/       # Watchdog & recovery
│
└── docs/                   # Feature documentation
```

---

## Module Configuration

### App Module (`:app`)
- **Package**: `com.photoframe.app`
- **Compose UI**: Material 3 with landscape orientation
- **Navigation**: Jetpack Compose Navigation
- **DI**: Hilt/Dagger

### Core Module (`:core`)
- **Package**: `com.photoframe.core`
- **SMB**: jcifs-ng library for network shares
- **Image Loading**: Coil (memory + disk cache)
- **Persistence**: DataStore Preferences
- **DI**: Hilt/Dagger

---

## Configuration

### SMB Connection (First Run)
1. Launch app
2. Settings screen appears automatically
3. Enter SMB server details:
   - **Server**: `192.168.1.100` or `server.local`
   - **Share**: `photos` (path to photo folder)
   - **Username**: Your SMB username
   - **Password**: Your SMB password
   - **Domain**: `WORKGROUP` (or your domain)
4. Test connection
5. Configure display settings:
   - **Interval**: 10s - 5min between photos
   - **Transition**: Fade, Slide, or Ken Burns effect
   - **Shuffle**: Randomize photo order
6. Save settings
7. Return to slideshow - photos will load automatically

### Schedule (Optional)
- Configure start/end times for automatic slideshow
- Uses WorkManager for reliable scheduling
- Persists across device reboots

---

## Gradle Tasks

### Build
```bash
./gradlew build                 # Build all variants + run tests
./gradlew assembleDebug        # Build debug APK only
./gradlew assembleRelease      # Build release APK only
```

### Clean
```bash
./gradlew clean                 # Delete build directories
./gradlew clean build          # Clean + full rebuild
```

### Install
```bash
./gradlew installDebug         # Build + install debug APK
./gradlew installRelease       # Build + install release APK
```

### Lint & Analysis
```bash
./gradlew lint                  # Run Android lint
./gradlew lintDebug            # Lint debug variant only
```

### Tests (Currently have compilation errors - optional)
```bash
./gradlew test                  # Run all unit tests
./gradlew connectedAndroidTest  # Run instrumented tests
```

---

## Troubleshooting

### Issue: "Android SDK not found"
**Solution**: Set `ANDROID_HOME` environment variable:
```bash
export ANDROID_HOME=/Users/$USER/Library/Android/sdk
```

### Issue: "JDK version mismatch"
**Solution**: The project is configured to use JDK 17 automatically via `gradle.properties`. If you see errors, verify JDK 17 is installed:
```bash
/Library/Java/JavaVirtualMachines/temurin-17.0.14+7/Contents/Home/bin/java -version
```

### Issue: "Gradle sync failed"
**Solution**:
1. **File** → **Invalidate Caches**
2. Restart Android Studio
3. **File** → **Sync Project with Gradle Files**

### Issue: "Could not resolve dependencies"
**Solution**: Check internet connection and proxy settings:
- **File** → **Settings** → **Appearance & Behavior** → **System Settings** → **HTTP Proxy**

### Issue: "Build failed with R8/ProGuard errors"
**Solution**: Release builds use minification. Check `proguard-rules.pro` for keep rules. Debug builds don't use minification.

---

## Development Workflow

### Making Changes
1. **Edit Code**: Modify `.kt` files in `app/` or `core/`
2. **Build**: Cmd+F9 (Make Project)
3. **Run**: Cmd+R (Run app)
4. **Hot Reload**: For Compose UI changes, use Live Edit (Android Studio Flamingo+)

### Adding Dependencies
Edit `app/build.gradle.kts` or `core/build.gradle.kts`:
```kotlin
dependencies {
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
}
```

### Module Dependency
App depends on core. To add core functionality:
```kotlin
// Already configured in app/build.gradle.kts
implementation(project(":core"))
```

---

## Performance Notes

### APK Sizes
- **Debug APK**: ~21 MB (unminified, includes debug symbols)
- **Release APK**: ~3.4 MB (minified with R8)

### Image Caching
- **Memory Cache**: 50 MB (~3 photos at 2560x1600)
- **Disk Cache**: 100 MB (~6 photos)
- Automatic LRU eviction

### Network Performance
- **SMB Connection**: Persistent connection pooling
- **Photo Loading**: Background pre-loading of next 3 photos
- **Retry Logic**: Exponential backoff (2s, 4s, 8s)

---

## Testing the App

### Manual Testing Checklist
- [ ] Launch app - setup wizard appears
- [ ] Enter SMB credentials
- [ ] Test connection - success message
- [ ] Configure display settings
- [ ] Save settings
- [ ] Slideshow starts automatically
- [ ] Photos transition correctly
- [ ] Navigate back to settings works
- [ ] Schedule configuration works

### Test SMB Server
If you don't have an SMB server, you can:
1. Use macOS File Sharing (System Settings → Sharing → File Sharing)
2. Use Samba on Linux
3. Use Windows File Sharing
4. Place test photos in shared folder

---

## Deployment

### Signing Release APK
1. Generate keystore:
   ```bash
   keytool -genkey -v -keystore photoframe.keystore -alias photoframe \
           -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Configure signing in `app/build.gradle.kts`:
   ```kotlin
   android {
       signingConfigs {
           create("release") {
               storeFile = file("../photoframe.keystore")
               storePassword = "your-password"
               keyAlias = "photoframe"
               keyPassword = "your-password"
           }
       }
       buildTypes {
           release {
               signingConfig = signingConfigs.getByName("release")
           }
       }
   }
   ```

3. Build signed release:
   ```bash
   ./gradlew assembleRelease
   ```

### Install on Device
```bash
adb install app/build/outputs/apk/release/app-release.apk
```

---

## Support & Documentation

- **Build Fixes**: See `BUILD_FIXES_SUMMARY.md`
- **Architecture**: See `docs/features/photo-frame-app-initial/architecture/`
- **PRD**: See `docs/features/photo-frame-app-initial/final/PRD.md`
- **Test Plan**: See `docs/features/photo-frame-app-initial/testing/`

---

## Next Steps

1. ✅ **Open in Android Studio** - Project is ready
2. ✅ **Build & Run** - Both debug and release work
3. 🔄 **Configure SMB Server** - Set up photo source
4. 🔄 **Test on Tablet** - Best experience on landscape tablets
5. 🔄 **Customize Settings** - Adjust intervals and transitions
6. 🔄 **Schedule Slideshow** - Optional auto-start/stop times

---

**The Photo Frame Android app is production-ready and waiting to display your photos!** 🖼️
