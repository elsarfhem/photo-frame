# Photo Frame Android

## Build & Run

- Build: `./gradlew assembleDebug`
- Install (tablet): `adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk`
- Launch: `adb -s emulator-5556 shell am start -n com.photoframe.app/.MainActivity`
- Logs: `adb -s emulator-5556 logcat | grep -E "SlideshowScreen|PanTransition|PhotoFrame"`
- Release bundle: `./gradlew bundleRelease` (output: `app/build/outputs/bundle/release/app-release.aab`)
- Tests: `./gradlew :core:testDebugUnitTest :tests:testDebugUnitTest` (CI runs these on every PR)

## Build Stack

AGP 9.1.0 | Kotlin 2.2.10 | KSP 2.3.6 | Gradle 9.3.1 | JDK 21 | minSdk 23 | compileSdk 35
Compose BOM 2024.09.00 | Firebase BOM 33.8.0 | Hilt 2.59.2 | Room 2.7.2 | axion-release 1.21.1

## Architecture

- **app/**: UI layer (Compose), ViewModels, navigation, transitions
- **core/**: Domain models, repositories, data sources, SMB client, Room DB
- **tests/**: Cross-module functional/integration tests (no Android runtime dependency)
- MVVM + Hilt DI + Repository pattern + StateFlow
- SMB photo loading with local caching (Coil 3 + jcifs-ng)
- Navigation: NavHost with type-safe @Serializable routes (Nav Compose 2.8.x)

## Key Patterns

- Pan animation is a display mode, NOT a transition type (orthogonal to Fade/Slide/Zoom)
- Concurrency: Review `.claude/CONCURRENCY_GUIDELINES.md` before touching ViewModel/Buffer code
- All annotation processing uses KSP (not KAPT)
- Version code: `git rev-list --count HEAD + 10000` (defined in root build.gradle.kts)

## Release Workflow

When work is done: push branch → create PR → merge → checkout main → pull → tag version → push tag → `./gradlew bundleRelease`

## Gotchas

- `extractNativeLibs="true"` in AndroidManifest — required for 16KB page alignment (Play Store)
- AGP 9.1.0 does not support `options.release.set()` — don't re-add it
- Configuration cache (`org.gradle.configuration-cache`) incompatible with git rev-list during config phase
- APK signature mismatch after major upgrades: `adb uninstall com.photoframe.app` first
- GitHub uses personal account (elsarfhem) — `gh` CLI won't work, use curl with PAT from remote URL
- Firebase BOM 33.8.0+ requires Kotlin 2.0+ (metadata version 2.1.0)
