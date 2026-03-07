# Troubleshooting Guide

## Common Build Issues

### 1. KAPT Module Export Error

**Error Message:**
```
java.lang.IllegalAccessError: superclass access check failed:
class org.jetbrains.kotlin.kapt3.base.javac.KaptJavaCompiler
cannot access class com.sun.tools.javac.main.JavaCompiler
because module jdk.compiler does not export...
```

**Root Cause:**
The Gradle/Kotlin daemon is running with old JVM arguments and hasn't picked up the module exports configuration from `gradle.properties`.

**Solution:**
Stop all daemons and force a fresh start:

```bash
# 1. Stop all Gradle daemons
./gradlew --stop

# 2. Kill any remaining Kotlin daemons
pkill -f "kotlin.*daemon"

# 3. Clear daemon caches (optional but recommended)
rm -rf ~/.gradle/daemon ~/.gradle/kotlin/daemon ~/.kotlin/daemon

# 4. Rebuild with fresh daemon
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.0.14+7/Contents/Home ./gradlew clean assembleDebug
```

**Prevention:**
After modifying `gradle.properties`, always restart the daemon:
```bash
./gradlew --stop
```

---

### 2. Wrong JDK Version

**Error Message:**
- Compilation errors mentioning Java 21 or incompatible JDK version
- Missing Java module exports

**Root Cause:**
Gradle is using a different JDK than the one configured in `gradle.properties`.

**Solution:**

**Option A: Use JAVA_HOME environment variable (recommended)**
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.0.14+7/Contents/Home
./gradlew assembleDebug
```

**Option B: Verify and set in gradle.properties**
Check that `gradle.properties` contains:
```properties
org.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-17.0.14+7/Contents/Home
```

**Verify JDK version:**
```bash
./gradlew --version
# Should show: JVM: 17.0.14
```

---

### 3. Android Studio Gradle Sync Failure

**Error Message:**
- "Gradle sync failed"
- "Could not resolve dependencies"
- SDK version mismatch warnings

**Solution:**

**Step 1: Invalidate Android Studio caches**
1. File → Invalidate Caches
2. Select "Invalidate and Restart"
3. Wait for Android Studio to restart

**Step 2: Sync Gradle files**
1. File → Sync Project with Gradle Files
2. Wait for sync to complete

**Step 3: Rebuild project**
1. Build → Clean Project
2. Build → Rebuild Project

**Step 4: If still failing, delete build directories**
```bash
./gradlew clean
rm -rf .gradle build app/build core/build
```
Then reopen in Android Studio.

---

### 4. Missing Dependencies

**Error Message:**
- "Could not resolve dependency"
- "Failed to resolve: [package name]"

**Solution:**

**Check internet connection:**
Dependencies are downloaded from Maven Central and Google Maven.

**Configure proxy (if behind firewall):**
Add to `gradle.properties`:
```properties
systemProp.http.proxyHost=proxy.company.com
systemProp.http.proxyPort=8080
systemProp.https.proxyHost=proxy.company.com
systemProp.https.proxyPort=8080
```

**Clear dependency cache:**
```bash
./gradlew clean --refresh-dependencies
```

**Force dependency download:**
```bash
rm -rf ~/.gradle/caches
./gradlew assembleDebug
```

---

### 5. R8/ProGuard Minification Errors (Release Build)

**Error Message:**
```
Missing class org.slf4j.impl.StaticLoggerBinder
R8: Missing class [classname]
```

**Solution:**

**Step 1: Check ProGuard rules**
Verify `app/proguard-rules.pro` contains the necessary keep rules.

**Current rules include:**
- SLF4J warnings suppression
- JCIFS SMB library keep rules
- Hilt/Dagger keep rules

**Step 2: Add missing keep rules**
If R8 reports missing classes, add to `proguard-rules.pro`:
```proguard
-dontwarn [package.name].**
-keep class [package.name].** { *; }
```

**Step 3: Disable minification temporarily (debugging)**
In `app/build.gradle.kts`:
```kotlin
buildTypes {
    release {
        isMinifyEnabled = false  // Temporarily disable
    }
}
```

---

### 6. Out of Memory Errors

**Error Message:**
- "Java heap space"
- "GC overhead limit exceeded"

**Solution:**

**Increase Gradle JVM memory:**
Edit `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8 ...
```

**Increase Android Studio memory:**
1. Help → Edit Custom VM Options
2. Change `-Xmx` value:
   ```
   -Xmx4096m
   ```

**Build with daemon:**
```bash
./gradlew assembleDebug --max-workers=2
```

---

### 7. Launcher Icon Not Found

**Error Message:**
```
AAPT: error: resource mipmap/ic_launcher not found
```

**Solution:**

Verify icon files exist:
```bash
ls -la app/src/main/res/drawable/ic_launcher_*.xml
ls -la app/src/main/res/mipmap-anydpi-v26/ic_launcher*.xml
```

Required files:
- `drawable/ic_launcher_background.xml`
- `drawable/ic_launcher_foreground.xml`
- `mipmap-anydpi-v26/ic_launcher.xml`
- `mipmap-anydpi-v26/ic_launcher_round.xml`

If missing, regenerate with Android Studio:
1. Right-click `app/src/main/res`
2. New → Image Asset
3. Configure launcher icon
4. Finish

---

### 8. Kotlin Compilation Errors

**Error Messages:**
- "Unresolved reference"
- "Type mismatch"
- "Cannot access [class]"

**Solution:**

**Step 1: Clean and rebuild**
```bash
./gradlew clean
./gradlew assembleDebug
```

**Step 2: Verify Kotlin version**
Check `build.gradle.kts`:
```kotlin
id("org.jetbrains.kotlin.android") version "1.9.0"
```

**Step 3: Invalidate Android Studio caches**
File → Invalidate Caches → Invalidate and Restart

**Step 4: Check module dependencies**
Verify `app/build.gradle.kts` includes:
```kotlin
implementation(project(":core"))
```

---

### 9. SMB Library (jcifs-ng) Issues

**Error Message:**
- "Could not resolve jcifs"
- SLF4J warnings

**Solution:**

**Verify dependency in core/build.gradle.kts:**
```kotlin
implementation("eu.agno3.jcifs:jcifs-ng:2.1.10")
```

**Add to app/proguard-rules.pro:**
```proguard
-keep class jcifs.** { *; }
-dontwarn jcifs.**
-dontwarn org.slf4j.**
-dontwarn javax.naming.**
```

---

### 10. Hilt/Dagger Errors

**Error Messages:**
- "Hilt processors must be applied"
- "Dependency cannot be provided"
- "@Inject constructor not found"

**Solution:**

**Verify kapt is enabled:**
Both modules should have in `build.gradle.kts`:
```kotlin
plugins {
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

dependencies {
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-android-compiler:2.50")
}

kapt {
    correctErrorTypes = true
}
```

**Check Application class:**
Verify `PhotoFrameApplication` has `@HiltAndroidApp`:
```kotlin
@HiltAndroidApp
class PhotoFrameApplication : Application()
```

**Check AndroidManifest.xml:**
```xml
<application
    android:name=".PhotoFrameApplication"
    ...
```

**Rebuild:**
```bash
./gradlew clean assembleDebug
```

---

## Build Performance Tips

### Speed up builds:

1. **Enable Gradle daemon** (default in gradle.properties)
2. **Use parallel builds:**
   ```properties
   org.gradle.parallel=true
   ```
3. **Configure on-demand:**
   ```properties
   org.gradle.configureondemand=true
   ```
4. **Increase worker count:**
   ```bash
   ./gradlew assembleDebug --max-workers=4
   ```
5. **Use build cache:**
   ```properties
   org.gradle.caching=true
   ```

### Reduce build time:

- **Skip tests during development:**
  ```bash
  ./gradlew assembleDebug -x test
  ```
- **Build only debug variant:**
  ```bash
  ./gradlew assembleDebug
  ```
- **Use incremental compilation** (already enabled)

---

## Android Studio Tips

### Import Project Issues:

**If project doesn't import correctly:**
1. Close Android Studio
2. Delete `.idea` directory
3. Delete all `.iml` files
4. Reopen Android Studio and import project

### Kotlin Not Recognized:

**File → Settings → Languages & Frameworks → Kotlin**
- Ensure Kotlin plugin is enabled
- Verify Kotlin version matches `build.gradle.kts`

### Gradle Version Mismatch:

**File → Settings → Build → Gradle**
- Use "Gradle wrapper" (recommended)
- JDK: Select JDK 17

---

## Quick Commands Cheat Sheet

```bash
# Stop all daemons and rebuild fresh
./gradlew --stop && ./gradlew clean assembleDebug

# Verify Gradle/JDK versions
./gradlew --version

# Check project structure
./gradlew projects

# List all available tasks
./gradlew tasks

# Build both variants
./gradlew assemble

# Install on device
./gradlew installDebug

# View dependencies
./gradlew :app:dependencies

# Refresh dependencies
./gradlew --refresh-dependencies

# Clean build directories
./gradlew clean

# Run with debug info
./gradlew assembleDebug --debug
```

---

## Getting Help

If you encounter an issue not covered here:

1. **Check build output** - Read the full error message
2. **Search Gradle logs** - `build/outputs/logs/`
3. **Check Android Studio Event Log** - Bottom right corner
4. **Verify configuration files:**
   - `gradle.properties`
   - `settings.gradle.kts`
   - `build.gradle.kts` (both root and module)
5. **Try clean rebuild:**
   ```bash
   ./gradlew --stop
   ./gradlew clean assembleDebug
   ```

---

## Emergency Recovery

If everything is broken and nothing works:

```bash
# 1. Stop all daemons
./gradlew --stop
pkill -f gradle
pkill -f kotlin

# 2. Delete all build artifacts
./gradlew clean
rm -rf .gradle build app/build core/build

# 3. Clear all caches
rm -rf ~/.gradle/caches
rm -rf ~/.gradle/daemon
rm -rf ~/.gradle/kotlin/daemon
rm -rf ~/.kotlin/daemon
rm -rf ~/.android/build-cache

# 4. Delete Android Studio project files
rm -rf .idea
find . -name "*.iml" -delete

# 5. Rebuild from scratch
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.0.14+7/Contents/Home ./gradlew clean assembleDebug

# 6. If still failing, reimport in Android Studio
# File → Close Project
# Delete .idea directory
# File → Open → Select project
# Wait for Gradle sync
```

This should resolve 99% of build issues.

---

## Still Having Issues?

Check the following in order:

1. ✅ JDK 17 installed and configured
2. ✅ Android SDK installed (API 26-34)
3. ✅ Internet connection working
4. ✅ Enough disk space (10GB+ free)
5. ✅ Enough RAM (8GB+ recommended)
6. ✅ No antivirus blocking Gradle/Android Studio
7. ✅ Latest Android Studio version (recommended)

If all else fails, refer to:
- BUILD_FIXES_SUMMARY.md
- QUICKSTART.md
- Official Android documentation: https://developer.android.com
