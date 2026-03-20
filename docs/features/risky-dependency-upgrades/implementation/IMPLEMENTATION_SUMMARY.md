# Risky Dependency Upgrades - Implementation Summary

## Upgrades Completed

### 1. Kotlin 2.0.20 -> 2.1.10 + KSP 2.0.20-1.0.25 -> 2.1.10-1.0.29
- Updated 4 plugin versions in root `build.gradle.kts`
- Migrated `kotlinOptions` to `compilerOptions` in root allprojects block
- Removed redundant per-module `kotlinOptions` blocks (root handles it)

### 2. Hilt 2.51.1 -> 2.53.1
- Combined with Kotlin upgrade (Hilt 2.51.1 cannot read Kotlin 2.1.x metadata)
- Updated plugin version in root and all `hilt-android`/`hilt-android-compiler` in app/core
- Note: Hilt 2.52 also fails; 2.53.1 is the minimum version compatible with Kotlin 2.1.10

### 3. Coil 2.7.0 -> 3.1.0
- Maven coordinates: `io.coil-kt` -> `io.coil-kt.coil3`
- Package: `coil` -> `coil3`
- ImageCache changes:
  - `SuccessResult.drawable` -> `SuccessResult.image.toBitmap()` with defensive try/catch + Crashlytics
  - `DiskCache.Builder.directory()` takes `okio.Path` via `File.toOkioPath()`
  - `MemoryCache.Builder()` no longer takes context; `.maxSizeBytes()` takes Long directly
  - Removed `respectCacheHeaders(false)` (no built-in HTTP fetcher in Coil 3)
  - Removed `BitmapFactoryDecoder` (built-in in Coil 3)
  - Added one-time disk cache clear via SharedPreferences version flag
- SmbFetcher changes:
  - `SourceResult` -> `SourceFetchResult`
  - `Fetcher.Factory<android.net.Uri>` -> `Fetcher.Factory<coil3.Uri>`
  - `ImageSource(source, context)` -> `ImageSource(source, FileSystem.SYSTEM)`
- ProGuard: `coil.**` -> `coil3.**`
- Version 3.1.0 chosen (not 3.4.0) because Coil 3.4.0 pulls kotlin-stdlib 2.3.10 which is incompatible with Kotlin 2.1.10
- No Ktor networking modules added (app uses custom SmbFetcher)

### 4. Navigation Compose 2.7.7 -> 2.8.9
- Replaced `sealed class Screen` with `@Serializable object` routes
- Replaced manual `when(currentScreen)` with `NavHost` + `NavController`
- Added serialization plugin to app module
- Added `kotlinx-serialization-json` dependency to app module
- Navigation patterns:
  - Loading -> Slideshow/Sources: `popUpTo<LoadingRoute> { inclusive = true }`
  - Slideshow -> Settings: push on top (Slideshow preserved in back stack)
  - Settings -> Slideshow: `popUpTo<SlideshowRoute> { inclusive = true }` + navigate (recreates ViewModel)
  - Sources -> back: `popBackStack()`
- `slideshowReloadTrigger` kept as top-level `mutableStateOf` outside NavHost

## Key Decisions

| Decision | Rationale |
|----------|-----------|
| Kotlin + Hilt combined commit | Hilt 2.51.1 cannot read Kotlin 2.1.x metadata; must upgrade together |
| Hilt 2.53.1 (not 2.52) | Hilt 2.52 also fails with Kotlin 2.1.10 LazyMapKey generation |
| Coil 3.1.0 (not 3.4.0) | Coil 3.4.0 requires kotlin-stdlib 2.3.10, incompatible with Kotlin 2.1.10 |
| Nav Compose 2.8.9 | Latest stable 2.8.x, supports type-safe @Serializable routes |

## Files Modified

- `build.gradle.kts` (root) - Plugin versions, compilerOptions migration
- `app/build.gradle.kts` - Dependencies, serialization plugin
- `core/build.gradle.kts` - Dependencies
- `core/.../image/ImageCache.kt` - Full Coil 3 API migration
- `core/.../image/SmbFetcher.kt` - Full Coil 3 API migration
- `app/.../MainActivity.kt` - NavHost with type-safe routes
- `app/proguard-rules.pro` - Coil 3 ProGuard rules
- `CLAUDE.md` - Updated build stack versions
