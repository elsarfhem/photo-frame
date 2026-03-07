# Product Requirements Document (PRD) - Digital Photo Frame App

**Document Version**: 1.0
**Created**: 2026-03-01
**Feature Directory**: `docs/features/photo-frame-app-initial/`
**Story Size**: XL
**Project Type**: Greenfield (New Application)

---

## 1. Executive Summary

### Feature Name
**Digital Photo Frame - Android Tablet Application (MVP Phase 1)**

### Jira Reference
**N/A** - Greenfield project, no existing Jira story

### Description
A dedicated Android tablet application that transforms any Android tablet into a digital photo frame and slideshow player. The app streams photos from local network SMB/Samba shares, displaying them in a continuous slideshow with customizable transitions, timing controls, and automated scheduling. Designed for "set it and forget it" kiosk-style operation.

### Primary User Benefit
Users can repurpose existing Android tablets as elegant, automated digital photo frames that display their personal photo collections from network storage without requiring manual intervention. The app provides a low-cost alternative to dedicated photo frame hardware while offering greater flexibility and customization.

### Target Release
**MVP Phase 1** - Q2 2026 (3-4 month development cycle)

### Key Success Criteria
- Seamless SMB photo streaming with smooth transitions
- Zero-touch operation after initial setup
- Stable 24/7 operation with automated scheduling
- Professional photo frame experience on commodity Android hardware

---

## 2. Background & Context

### Why This Feature Is Needed

#### Problem Statement
Users have extensive personal photo collections stored on home network drives (NAS, network shares) but lack an affordable, flexible way to display them as ambient digital art in their homes. Dedicated photo frame hardware is expensive, limited in features, and often locked to proprietary cloud services.

#### Current State
- Users manually cycle through photos on tablets or use basic gallery apps (poor UX)
- Dedicated photo frame hardware costs $150-300 and has limited customization
- Existing Android slideshow apps are buggy, ad-supported, or lack network storage support
- No good solution exists for SMB/Samba network share integration on Android

#### Desired State
- Smooth, professional slideshow experience on any Android tablet ($100-200 hardware)
- Direct integration with home network storage (NAS, SMB shares)
- Configurable transitions, timing, and scheduling
- Reliable 24/7 operation with automated screen control
- Clean, distraction-free display focused on photos

### Business Value & Metrics

#### Target Audience
- **Primary**: Home users with network-attached storage (NAS, home server, SMB shares)
- **Secondary**: Small businesses (waiting rooms, lobbies), photographers (portfolio display)
- **Device Profile**: Android tablets 7" and larger, typically landscape orientation

#### Expected Usage Pattern
- Initial setup: 15-30 minutes (SMB configuration)
- Ongoing: Zero interaction after setup (automated kiosk mode)
- Settings adjustments: Monthly or less (occasional tweaking of timing/transitions)

#### Success Metrics (Defined in Section 9)
- 95%+ uptime during scheduled hours
- <2s average photo load time
- Zero crashes per 24-hour period
- 80%+ user satisfaction in post-launch survey

### Related Context
- **Reference Implementations**: Google Photos screensaver (limited to cloud), Dayframe (discontinued), PhotoSync (lacks SMB)
- **Similar Features**: Apple Photos screensaver on Apple TV
- **Design References**: Material Design 3, tablet-optimized layouts

---

## 3. User Stories

### Primary User Stories (P0 - Must Have)

#### User Story 1.1: Configure SMB Network Share (P0)
**Story**:
As a photo frame user
I want to connect the app to my network SMB share with manual configuration
So that the app can access my photo collection stored on my home NAS or network drive

**Priority**: P0 (Critical Path)
**Dependencies**: None
**Estimate**: 5 story points

---

#### User Story 1.2: Discover SMB Shares on Network (P0)
**Story**:
As a less technical user
I want the app to automatically discover available SMB shares on my local network
So that I can easily connect without knowing the exact SMB path syntax

**Priority**: P0 (Critical Path)
**Dependencies**: None
**Estimate**: 5 story points

---

#### User Story 2.1: View Random Slideshow (P0)
**Story**:
As a photo frame user
I want the app to display photos in random order with configurable timing
So that I see varied content and can control how long each photo is displayed

**Priority**: P0 (Core Feature)
**Dependencies**: US 1.1 or US 1.2 (SMB configured)
**Estimate**: 8 story points

---

#### User Story 2.2: Smooth Photo Transitions (P0)
**Story**:
As a photo frame user
I want seamless transitions between photos without delays or glitches
So that the slideshow feels polished and professional

**Priority**: P0 (Core Feature)
**Dependencies**: US 2.1
**Estimate**: 8 story points

---

#### User Story 3.1: Select Transition Effects (P0)
**Story**:
As a photo frame user
I want to choose between multiple transition effects (Fade, Slide, Zoom/Ken Burns)
So that I can customize the visual style to my preference

**Priority**: P0 (Core Feature)
**Dependencies**: US 2.2
**Estimate**: 8 story points

---

#### User Story 4.1: Configure Slideshow Schedule (P0)
**Story**:
As a photo frame user
I want to set daily on/off times for the slideshow
So that the screen automatically turns off at night and back on in the morning, saving power

**Priority**: P0 (Core Feature)
**Dependencies**: US 2.1
**Estimate**: 8 story points

---

#### User Story 5.1: Access Settings Interface (P0)
**Story**:
As a photo frame user
I want a simple, consolidated settings screen
So that I can configure all app options in one place without complexity

**Priority**: P0 (Core Feature)
**Dependencies**: None
**Estimate**: 5 story points

---

### Secondary User Stories (P1 - Should Have)

#### User Story 6.1: Handle Network Interruptions Gracefully (P1)
**Story**:
As a photo frame user
I want the app to handle temporary network issues without crashing
So that the slideshow recovers automatically when connectivity is restored

**Priority**: P1 (Quality)
**Dependencies**: US 2.1
**Estimate**: 5 story points

---

#### User Story 6.2: Display Mixed Orientation Photos (P1)
**Story**:
As a photo frame user
I want portrait and landscape photos to display correctly on my landscape tablet
So that all photos are visible without cropping important content

**Priority**: P1 (Quality)
**Dependencies**: US 2.1
**Estimate**: 3 story points

---

#### User Story 6.3: Skip Corrupted Photos (P1)
**Story**:
As a photo frame user
I want the app to automatically skip corrupted or unreadable photos
So that the slideshow continues without interruption

**Priority**: P1 (Quality)
**Dependencies**: US 2.1
**Estimate**: 3 story points

---

### Edge Case Stories (P2 - Nice to Have)

#### User Story 7.1: Optimize for Large Photo Collections (P2)
**Story**:
As a user with 10,000+ photos
I want the app to efficiently scan and randomize large photo libraries
So that initial load times remain reasonable

**Priority**: P2 (Performance Edge Case)
**Dependencies**: US 2.1
**Estimate**: 5 story points

---

#### User Story 7.2: Handle Empty Photo Directories (P2)
**Story**:
As a user who may misconfigure the SMB path
I want clear error messaging when no photos are found
So that I know how to fix the issue

**Priority**: P2 (User Experience)
**Dependencies**: US 1.1, US 1.2
**Estimate**: 2 story points

---

## 4. Acceptance Criteria

### User Story 1.1: Configure SMB Network Share

**Acceptance Criteria**:

**Functional Requirements**:
- [ ] Settings screen provides input fields for: SMB host/IP, share name, path, username, password
- [ ] "Test Connection" button validates SMB credentials before saving
- [ ] Successful connection shows checkmark/success message
- [ ] Failed connection shows specific error: authentication failure, network unreachable, share not found, permission denied
- [ ] Credentials are securely stored using Android Keystore (no plaintext passwords)
- [ ] Save button disabled until connection test passes
- [ ] Support for SMB v2 and v3 protocols

**Edge Case Handling**:
- [ ] Invalid IP address format: Show inline validation error
- [ ] Timeout after 10 seconds: Show "Connection timeout - check network" message
- [ ] Special characters in password: Handle URL encoding correctly
- [ ] Domain authentication: Support DOMAIN\username format
- [ ] Missing share name: Show "Share name required" validation error

**Error Handling**:
- [ ] Network unavailable: "No network connection. Please connect to WiFi."
- [ ] DNS resolution failure: "Cannot resolve hostname. Use IP address instead."
- [ ] Port 445 blocked: "SMB port blocked. Check firewall settings."
- [ ] Authentication loops: Limit retry attempts to 3, then show "Invalid credentials"

**Platform Considerations**:
- Android: Use jcifs-ng or smbj library for SMB client implementation
- Minimum Android 8.0 (API 26) for background network access restrictions
- Requires INTERNET permission
- Handle Android network security config for cleartext traffic (SMB)

**UI/UX Requirements**:
- Material Design 3 text fields with floating labels
- Password field with show/hide toggle
- "Advanced" section for SMB version selection, port override
- Loading spinner during connection test
- Success/error states with color-coded messages (green/red)

**Security Requirements**:
- [ ] Password field masked by default
- [ ] Credentials stored in EncryptedSharedPreferences backed by Android Keystore
- [ ] No credentials logged or sent to analytics
- [ ] SMB connection attempts over local network only (no internet SMB)

**Dependencies**:
- SMB client library integrated (jcifs-ng or smbj)
- Android Keystore initialization for secure credential storage

---

### User Story 1.2: Discover SMB Shares on Network

**Acceptance Criteria**:

**Functional Requirements**:
- [ ] "Scan Network" button in settings initiates SMB discovery
- [ ] Discovery scans local subnet for NetBIOS broadcasts and SMB servers
- [ ] Results display in list: server name, IP address, available shares
- [ ] Tapping discovered share auto-populates SMB configuration fields
- [ ] Discovery limited to 30-second timeout
- [ ] Show progress indicator during scan: "Scanning... X devices found"
- [ ] Support NetBIOS name resolution and mDNS discovery

**Edge Case Handling**:
- [ ] No shares found: "No SMB shares detected. Try manual configuration."
- [ ] Network not connected: Disable scan button, show "Connect to WiFi first"
- [ ] Partial discovery (some timeouts): Show found shares with warning icon for unreachable hosts
- [ ] Duplicate server names: Disambiguate with IP address in parentheses
- [ ] Password-protected shares: Show lock icon, prompt for credentials on selection

**Error Handling**:
- [ ] Permission denied on network scan: "Network discovery failed. Check WiFi permissions."
- [ ] Scan timeout: "Discovery incomplete. Found X shares. Scan again or use manual config."
- [ ] Network change during scan: Abort scan, show "Network changed. Please try again."

**Platform Considerations**:
- Android: Requires ACCESS_NETWORK_STATE permission
- Android 13+: May require NEARBY_WIFI_DEVICES for local network discovery
- Background discovery may be restricted - run on foreground service during scan
- WiFi multicast filtering may block NetBIOS - document limitation

**UI/UX Requirements**:
- Material Design 3 list items with server icon, primary text (name), secondary text (IP)
- Pull-to-refresh gesture to re-scan
- Empty state illustration: "No shares found - try manual setup"
- Share detail modal: Share name, path, available folders
- Scan progress: Indeterminate progress bar with device count

**Performance Requirements**:
- [ ] Scan completes in <30 seconds for typical home networks (10-20 devices)
- [ ] UI remains responsive during scan (run on background thread)
- [ ] Cache discovery results for 5 minutes to avoid repeated scans

**Dependencies**:
- NetBIOS and mDNS libraries for network discovery
- Background coroutine/job for async scanning

---

### User Story 2.1: View Random Slideshow

**Acceptance Criteria**:

**Functional Requirements**:
- [ ] Slideshow displays photos from configured SMB share in random order
- [ ] Random algorithm shuffles entire photo list on slideshow start
- [ ] Re-shuffle when all photos shown (avoid immediate repeats)
- [ ] Configurable display duration: 5s, 10s, 15s, 30s, 1min, 2min (default: 10s)
- [ ] Photo advances automatically after configured duration
- [ ] Full-screen immersive mode (hide status bar, navigation bar)
- [ ] Recursive folder scanning on SMB share (include subdirectories)
- [ ] Support JPEG (.jpg, .jpeg), PNG (.png), HEIC (.heic, .heif) formats
- [ ] Read-ahead buffering: Preload next 2-3 photos in background

**Edge Case Handling**:
- [ ] Empty photo list: Show message "No photos found. Check SMB configuration."
- [ ] Single photo: Display repeatedly with configured timing
- [ ] Less than 10 photos: Allow repeats in shuffle (warn user in settings)
- [ ] Very large collections (10,000+ photos): Show progress on initial scan, cache file list
- [ ] New photos added to share: Re-scan folder on app restart (no live updates in MVP)

**Error Handling**:
- [ ] SMB share unreachable: Show overlay "Connection lost. Reconnecting..." (pause slideshow)
- [ ] Photo load failure: Skip to next photo, log error, show toast "Skipped 1 photo"
- [ ] Buffer exhausted (slow network): Show loading spinner, pause slideshow until next photo loads
- [ ] Out of memory: Downsize images to screen resolution, clear previous image from memory

**Platform Considerations**:
- Android: Use Coil or Glide for efficient image loading and caching
- Jetpack Compose: Image composable with ContentScale.Fit
- Coroutines: Flow-based photo queue with buffered channel
- Memory: Load images at screen resolution (max 2560x1600 for typical tablets)
- Foreground service: Keep slideshow active, prevent system from killing app

**UI/UX Requirements**:
- Black background for letterboxing/pillarboxing
- Crossfade between photos for smooth transition
- No UI chrome during slideshow (fully immersive)
- Swipe from edge or long-press to access settings (subtle gesture hint on first launch)
- Loading indicator: Circular progress in center if photo delayed >2s

**Performance Requirements**:
- [ ] Smooth 60fps during transitions
- [ ] Photo load time <2s average (local network)
- [ ] Memory usage <300MB with 2-3 photos buffered
- [ ] No UI jank or stuttering during photo advance

**Dependencies**:
- SMB configuration completed (US 1.1 or US 1.2)
- Image loading library (Coil/Glide)
- File scanning logic for SMB share

---

### User Story 2.2: Smooth Photo Transitions

**Acceptance Criteria**:

**Functional Requirements**:
- [ ] Transitions are smooth with no visible frame drops or stuttering
- [ ] Default transition is crossfade (simple, reliable)
- [ ] Transition duration: 500ms for fade, 1000ms for slide/zoom
- [ ] Next photo fully loaded before transition starts (no black frames)
- [ ] Previous photo released from memory after transition completes
- [ ] Transition timing independent of photo load time (buffering ensures readiness)

**Edge Case Handling**:
- [ ] Next photo not ready: Extend current photo display until ready (no abrupt cuts)
- [ ] Very large photo (>10MB): Downsample before transition to avoid memory spike
- [ ] Device thermal throttling: Gracefully degrade transition complexity if frame rate drops
- [ ] App returning to foreground: Resume slideshow without jarring transition

**Error Handling**:
- [ ] Transition animation exception: Fall back to instant swap (log error)
- [ ] GPU overdraw: Optimize layer composition for smooth rendering
- [ ] Memory pressure during transition: Clear old photo immediately, skip preload

**Platform Considerations**:
- Jetpack Compose: Use animateContentSize() and Crossfade composable
- Hardware acceleration: Enable for smooth animation
- Composition: Avoid recomposition during transition
- Frame pacing: Use Choreographer for smooth 60fps timing

**UI/UX Requirements**:
- Imperceptible transition timing (feels natural, not mechanical)
- No flash of background color between photos
- Consistent transition feel across all photo aspect ratios
- Visual quality maintained during transition (no artifacts)

**Performance Requirements**:
- [ ] 60fps during all transitions (16ms frame time)
- [ ] <100ms pre-transition prep time
- [ ] No memory allocation during transition (pre-allocate buffers)

**Dependencies**:
- US 2.1 (slideshow functional)
- Image preloading system
- Compose animation APIs

---

### User Story 3.1: Select Transition Effects

**Acceptance Criteria**:

**Functional Requirements**:
- [ ] Settings screen provides transition effect selector
- [ ] Three transition types available:
  1. **Fade**: Crossfade (alpha blend) between photos (default)
  2. **Slide**: Pan from left/right/top/bottom (random direction per transition)
  3. **Zoom/Ken Burns**: Slow zoom and pan effect (start/end positions randomized)
- [ ] Preview animation shown when selecting transition type (3-second demo)
- [ ] Selection saved and applied immediately to slideshow
- [ ] Transition effect consistent throughout slideshow session

**Edge Case Handling**:
- [ ] Portrait photos with Slide transition: Pan vertically (top/bottom) for better effect
- [ ] Landscape photos with Slide transition: Pan horizontally (left/right)
- [ ] Ken Burns on small photos: Limit zoom factor to avoid pixelation (max 1.5x)
- [ ] Rapid setting changes: Debounce preview to avoid animation stutter

**Error Handling**:
- [ ] Animation rendering failure: Fall back to Fade transition (most stable)
- [ ] Low-end device performance: Detect frame drops, suggest Fade in settings
- [ ] GPU incompatibility: Graceful degradation to simple crossfade

**Platform Considerations**:
- Jetpack Compose: Custom AnimatedContent for Slide and Ken Burns
- OpenGL/Vulkan: Not required for MVP (Compose animations sufficient)
- Hardware acceleration: Required for smooth Ken Burns effect
- Testing: Verify on low-end tablets (e.g., Fire HD 8)

**UI/UX Requirements**:
- Transition selector: Radio button group with preview thumbnails
- Preview: Side-by-side photo pairs with "Preview" button
- Description text under each option:
  - Fade: "Gentle crossfade (recommended)"
  - Slide: "Dynamic panning motion"
  - Ken Burns: "Slow zoom and pan effect"
- Preview uses sample photos (bundled assets)

**Performance Requirements**:
- [ ] Fade: 60fps on all devices
- [ ] Slide: 60fps on mid-range+ tablets
- [ ] Ken Burns: 60fps on high-end tablets, 30fps acceptable on low-end

**Implementation Details**:
- **Fade**: Compose Crossfade with 500ms duration
- **Slide**: Animated offset with easing (DecelerateEasing)
- **Ken Burns**: Scale and offset animations over 10-15s per photo
  - Random start position (scaled 1.0x, corner-aligned)
  - Random end position (scaled 1.3x, opposite corner)
  - Smooth easing (CubicBezierEasing)

**Dependencies**:
- US 2.2 (transition system functional)
- Compose animation APIs
- Sample preview assets

---

### User Story 4.1: Configure Slideshow Schedule

**Acceptance Criteria**:

**Functional Requirements**:
- [ ] Settings screen provides schedule configuration:
  - Enable/disable scheduling toggle
  - Start time picker (default: 7:00 AM)
  - End time picker (default: 11:00 PM)
  - Days of week selector (default: all days)
- [ ] When schedule enabled, app automatically:
  - Wakes screen and starts slideshow at start time
  - Stops slideshow and sleeps screen at end time
- [ ] Schedule applied every day based on device's local timezone
- [ ] Screen brightness set to configured level during active hours (default: 50%)
- [ ] App must remain in foreground service to maintain schedule

**Edge Case Handling**:
- [ ] Overnight schedule (e.g., 8 PM - 2 AM): Handle day boundary correctly
- [ ] Same start and end time: Treat as 24-hour schedule (always on)
- [ ] App manually stopped during scheduled hours: Respect user override, don't auto-restart until next scheduled start
- [ ] Device reboot during scheduled hours: Auto-start slideshow within 5 minutes of boot
- [ ] System sleep/doze mode: Use AlarmManager to wake from doze

**Error Handling**:
- [ ] WAKE_LOCK permission denied: Show settings error, link to app settings
- [ ] SCHEDULE_EXACT_ALARM permission denied (Android 12+): Show permission prompt
- [ ] AlarmManager failure: Log error, fall back to approximate timing (WorkManager)
- [ ] Screen lock override failure: Show warning "Unable to control screen. Check battery optimization settings."

**Platform Considerations**:
- Android 12+ (API 31+): Requires SCHEDULE_EXACT_ALARM permission
- AlarmManager: Use setExactAndAllowWhileIdle() for reliable scheduling
- WAKE_LOCK: Required to turn screen on from sleep
- Foreground service: Required for background scheduling (show persistent notification)
- Battery optimization: App must be excluded from Doze restrictions (document in settings)
- Screen control: Use PowerManager.WakeLock and Window flags

**UI/UX Requirements**:
- Material Design 3 time pickers (12-hour or 24-hour based on device locale)
- Toggle switches for enable/disable and day selection
- Visual schedule preview: "Active 7:00 AM - 11:00 PM daily"
- Warning text: "Battery optimization must be disabled for reliable scheduling"
- Link to system battery settings
- Persistent notification during active hours: "Photo Frame active"

**Performance Requirements**:
- [ ] Wake from sleep in <10 seconds
- [ ] Slideshow starts automatically within 30 seconds of scheduled time
- [ ] No battery drain during sleep hours (app suspended)

**Security/Privacy Considerations**:
- [ ] Notifications can be disabled by user (optional)
- [ ] Screen lock does not interfere with slideshow (app keeps screen awake)
- [ ] No sensitive data in notifications

**Implementation Details**:
- Use AlarmManager for scheduling (more reliable than WorkManager for exact timing)
- Foreground service with WAKE_LOCK for screen control
- BroadcastReceiver for boot completion (auto-start on device reboot)
- Notification channel: "Slideshow Schedule" (low priority, non-intrusive)

**Dependencies**:
- US 2.1 (slideshow functional)
- Android permissions: WAKE_LOCK, SCHEDULE_EXACT_ALARM, FOREGROUND_SERVICE, RECEIVE_BOOT_COMPLETED

---

### User Story 5.1: Access Settings Interface

**Acceptance Criteria**:

**Functional Requirements**:
- [ ] Single consolidated settings screen accessible from:
  1. Swipe from left edge during slideshow (drawer gesture)
  2. Long-press (2 seconds) on screen during slideshow
  3. App launcher icon (if stopped)
- [ ] Settings organized into logical sections:
  1. **SMB Configuration**: Host, share, credentials, test connection, discover shares
  2. **Slideshow Settings**: Display duration, transition effect, random/sequential
  3. **Schedule Settings**: Enable schedule, start time, end time, days of week
  4. **Display Settings**: Brightness, screen timeout, orientation lock
  5. **Advanced**: Photo formats, folder exclusions, cache management
- [ ] All settings saved automatically on change (no "Save" button required)
- [ ] Settings take effect immediately (no app restart required)
- [ ] Back button or "Close" returns to slideshow

**Edge Case Handling**:
- [ ] Settings opened during transition: Pause slideshow, overlay settings
- [ ] Multiple rapid setting changes: Debounce updates to avoid thrashing
- [ ] Invalid configuration: Disable "Start Slideshow" button until valid
- [ ] First launch: Show settings screen with setup wizard overlay

**Error Handling**:
- [ ] Settings save failure: Retry 3 times, show error toast if persistent
- [ ] Corrupted settings file: Reset to defaults, show warning
- [ ] Missing required fields: Show inline validation errors

**Platform Considerations**:
- Jetpack Compose: Scaffold with TopAppBar and LazyColumn
- Material Design 3: Use standard preference components (Switch, Slider, DropdownMenu)
- DataStore: Use Preferences DataStore for settings persistence (not SharedPreferences)
- Accessibility: All settings keyboard navigable, screen reader compatible

**UI/UX Requirements**:
- Material Design 3 settings layout (two-pane on large tablets)
- Section headers with dividers
- Descriptive labels and helper text for each setting
- Inline previews where applicable (e.g., transition preview)
- Settings search bar for large option lists (future enhancement)
- Dark mode support (follows system theme)

**Performance Requirements**:
- [ ] Settings screen opens in <500ms
- [ ] Setting changes applied in <100ms
- [ ] No lag when scrolling settings list

**Implementation Details**:
- Compose Scaffold with TopAppBar
- LazyColumn for scrollable settings list
- PreferenceDataStore for persistence
- Settings ViewModel for state management

**Dependencies**:
- DataStore library for settings persistence
- Compose UI components

---

### User Story 6.1: Handle Network Interruptions Gracefully

**Acceptance Criteria**:

**Functional Requirements**:
- [ ] Network disconnection detected within 5 seconds
- [ ] Slideshow continues for 2-3 buffered photos (read-ahead buffer)
- [ ] After buffer exhausted, show overlay: "Connection lost. Reconnecting..."
- [ ] Automatic reconnection attempts every 10 seconds
- [ ] When reconnected, resume slideshow without user intervention
- [ ] Network status icon in corner during slideshow (optional, can be disabled)

**Edge Case Handling**:
- [ ] WiFi drops, then reconnects to different network: Re-validate SMB share accessibility
- [ ] SMB server reboots: Wait for server to become available (retry up to 5 minutes)
- [ ] Network switches from WiFi to mobile data: Show warning "SMB requires WiFi" (pause slideshow)
- [ ] Partial connectivity (WiFi connected but no internet): Continue if SMB share is local
- [ ] Slow network (high latency): Show buffering indicator, slow down slideshow timing

**Error Handling**:
- [ ] Repeated connection failures (>5 minutes): Show error screen with "Retry" and "Settings" buttons
- [ ] SMB authentication expires: Prompt for credentials again
- [ ] Network permission revoked: Show error, link to app settings

**Platform Considerations**:
- ConnectivityManager: Monitor network state changes
- NetworkCallback: Real-time network status updates
- Coroutines: Retry logic with exponential backoff
- Room database: Cache photo list and metadata for offline reference

**UI/UX Requirements**:
- Non-intrusive overlay (semi-transparent banner at top)
- Auto-dismiss when reconnected
- Progress indicator during reconnection attempts
- Error screen: Clear explanation and actionable steps

**Performance Requirements**:
- [ ] Network state detection: <1 second latency
- [ ] Reconnection attempt: <2 seconds per retry
- [ ] No UI blocking during reconnection

**Dependencies**:
- US 2.1, US 2.2 (slideshow functional)
- ConnectivityManager integration

---

### User Story 6.2: Display Mixed Orientation Photos

**Acceptance Criteria**:

**Functional Requirements**:
- [ ] Portrait photos displayed with black bars on left/right (pillarbox)
- [ ] Landscape photos displayed with black bars on top/bottom (letterbox) if taller aspect ratio
- [ ] Photos scaled to fit screen while maintaining original aspect ratio
- [ ] No cropping of photo content
- [ ] Black background used for bars (not white or custom color)
- [ ] EXIF orientation metadata respected (auto-rotate photos)

**Edge Case Handling**:
- [ ] Square photos: Centered with equal bars on all sides
- [ ] Panoramic photos (ultra-wide): Fit to width, large top/bottom bars
- [ ] Ultra-tall portrait photos: Fit to height, large left/right bars
- [ ] Rotated photos (90°, 180°, 270°): Apply EXIF rotation before display

**Error Handling**:
- [ ] Missing EXIF data: Display as-is (don't auto-rotate)
- [ ] Corrupted EXIF: Ignore orientation tag, display raw image
- [ ] Aspect ratio calculation failure: Default to ContentScale.Fit

**Platform Considerations**:
- Jetpack Compose: Image composable with ContentScale.Fit
- Coil/Glide: Respect EXIF orientation during load
- Background: Compose Box with black background

**UI/UX Requirements**:
- Seamless presentation (bars feel intentional, not like errors)
- Consistent bar color (pure black #000000)
- No visible edges or borders around photo

**Performance Requirements**:
- [ ] No performance difference between orientations
- [ ] EXIF parsing <50ms per photo

**Dependencies**:
- US 2.1 (slideshow functional)
- Image loading library with EXIF support

---

### User Story 6.3: Skip Corrupted Photos

**Acceptance Criteria**:

**Functional Requirements**:
- [ ] Corrupted or unreadable photos detected during load
- [ ] App skips to next photo automatically (no crash)
- [ ] Brief toast notification: "Skipped 1 corrupted photo"
- [ ] Error logged to debug log with file path and exception
- [ ] Corrupted photo excluded from future slideshow iterations (blacklisted)
- [ ] User can view error log in advanced settings

**Edge Case Handling**:
- [ ] All photos corrupted: Show error screen "No valid photos found"
- [ ] Intermittent read failures (network glitch): Retry once before blacklisting
- [ ] Partially loaded image: Display what's available or skip (configurable in settings)
- [ ] Multiple consecutive failures: Don't spam toast notifications (rate limit)

**Error Handling**:
- [ ] OutOfMemoryError: Skip photo, clear cache, continue
- [ ] IOException: Log error, skip photo
- [ ] ImageDecodeException: Log error, skip photo
- [ ] Unknown errors: Catch all exceptions, skip photo, log stack trace

**Platform Considerations**:
- Coil/Glide error handling: Use error listener callbacks
- Coroutines: Wrap image load in try-catch
- Logging: Use Timber or Android Logcat

**UI/UX Requirements**:
- Toast notification: Non-intrusive, bottom of screen, 2-second duration
- Error log screen: List of skipped files with timestamps and error types
- Clear log button: Allow user to reset blacklist (re-scan files)

**Performance Requirements**:
- [ ] Error detection <500ms
- [ ] Skip to next photo <1 second

**Dependencies**:
- US 2.1 (slideshow functional)
- Error handling framework

---

### User Story 7.1: Optimize for Large Photo Collections

**Acceptance Criteria**:

**Functional Requirements**:
- [ ] Initial folder scan shows progress indicator: "Scanning... X photos found"
- [ ] Scan limited to 30-second timeout, then display available photos
- [ ] Photo list cached to database (Room) after scan
- [ ] Incremental loading: Load first 100 photos, continue scanning in background
- [ ] Shuffle algorithm efficient for large lists (Fisher-Yates O(n))
- [ ] Memory-efficient photo queue (don't load all metadata at once)

**Edge Case Handling**:
- [ ] 10,000+ photos: Scan completes in <60 seconds, use pagination
- [ ] 100,000+ photos: Show warning "Large collection detected - may take several minutes"
- [ ] Scan interrupted (app backgrounded): Resume scan when foregrounded
- [ ] Scan failure: Show partial results with option to retry

**Error Handling**:
- [ ] Timeout during scan: Use partial results, log error
- [ ] Database write failure: Fall back to in-memory list
- [ ] Out of memory during scan: Process in smaller batches

**Platform Considerations**:
- Room database: Cache file paths and metadata
- Coroutines: Background scanning with Flow for progress updates
- Pagination: Load photos in chunks of 500

**UI/UX Requirements**:
- Progress bar with percentage and count
- "Skip to slideshow" button during long scans
- Background scan indicator in settings

**Performance Requirements**:
- [ ] Scan rate: 100+ files per second
- [ ] Database write: Batch inserts for efficiency
- [ ] UI responsive during scan

**Dependencies**:
- US 2.1 (slideshow functional)
- Room database for caching

---

### User Story 7.2: Handle Empty Photo Directories

**Acceptance Criteria**:

**Functional Requirements**:
- [ ] Empty directory detected after scan completes
- [ ] Clear error message displayed: "No photos found in configured location"
- [ ] Helpful guidance: "Please check your SMB path and ensure JPEG/PNG/HEIC files exist"
- [ ] "Open Settings" button to reconfigure SMB path
- [ ] "Retry Scan" button to re-scan current path

**Edge Case Handling**:
- [ ] Directory exists but no image files: Show error (not crash)
- [ ] Directory has images but wrong format (e.g., only TIFF): Show "No supported photos found (JPEG/PNG/HEIC)"
- [ ] Permission denied on directory: Show "Access denied - check SMB permissions"

**Error Handling**:
- [ ] Scan timeout: Treat as empty directory
- [ ] Network error during scan: Show network error (not empty directory error)

**Platform Considerations**:
- Error screen: Compose UI with Material Design empty state
- Illustration: Optional icon (folder with X)

**UI/UX Requirements**:
- Empty state screen: Centered content with icon, message, buttons
- Clear, actionable error message
- Consistent with Material Design patterns

**Performance Requirements**:
- [ ] Empty directory detection: <5 seconds

**Dependencies**:
- US 1.1, US 1.2 (SMB configuration)
- File scanning logic

---

## 5. Functional Requirements

### 5.1 SMB Network Integration

#### 5.1.1 SMB Client Implementation
- Use **jcifs-ng** or **smbj** library for SMB v2/v3 protocol support
- Support SMB v2.1, v3.0, v3.1.1 (prefer latest version supported by server)
- Implement connection pooling for efficient file access
- Support both IP address and hostname/NetBIOS name resolution
- Default to port 445 (allow custom port in advanced settings)

#### 5.1.2 Authentication & Security
- Support three authentication methods:
  1. Username/password (most common)
  2. Domain authentication (DOMAIN\username)
  3. Guest access (no credentials)
- Store credentials securely:
  - Use **EncryptedSharedPreferences** (Jetpack Security)
  - Backed by **Android Keystore** system
  - Never log or transmit credentials
- Encrypt SMB connection if server supports SMB signing (configurable)

#### 5.1.3 Network Discovery
- Implement NetBIOS name service (NBNS) discovery for legacy servers
- Implement mDNS/DNS-SD for modern servers
- Scan local subnet (192.168.x.0/24, 10.0.x.0/24, etc.)
- Discovery timeout: 30 seconds maximum
- Return list of discovered servers with:
  - Server name
  - IP address
  - Available shares (enumerate if guest access allowed)
  - Server OS/version (if available)

#### 5.1.4 File Operations
- Recursive directory traversal (follow subdirectories)
- File filtering by extension: .jpg, .jpeg, .png, .heic, .heif (case-insensitive)
- File metadata extraction: filename, path, size, modified date
- Directory exclusions: Skip hidden folders (starting with .)
- Symlink handling: Follow symlinks (prevent infinite loops)

### 5.2 Photo Management

#### 5.2.1 Photo Scanning
- **Initial Scan Flow**:
  1. Connect to SMB share
  2. Enumerate all files recursively
  3. Filter by supported extensions
  4. Extract metadata (filename, path, size)
  5. Store in Room database for fast access
  6. Show progress indicator during scan

- **Supported Formats**:
  - JPEG: .jpg, .jpeg (baseline and progressive)
  - PNG: .png (8-bit, 24-bit, 32-bit with alpha)
  - HEIC/HEIF: .heic, .heif (iOS photos)

- **Unsupported Formats** (explicitly excluded):
  - RAW formats: .cr2, .nef, .arw, .dng
  - TIFF: .tif, .tiff
  - BMP: .bmp
  - WebP: .webp (future consideration)

#### 5.2.2 Photo Loading & Caching
- **Image Loading Library**: Use **Coil** (Compose-native) or **Glide**
- **Loading Strategy**:
  - Load images at screen resolution (downsample if larger)
  - Respect EXIF orientation tags
  - Apply aspect ratio calculation
  - Cache decoded bitmaps in memory (LRU cache, 100MB max)
- **Read-Ahead Buffer**:
  - Preload next 2-3 photos in background coroutine
  - Buffer queue uses Flow with buffered channel
  - Clear buffer on network error or settings change

#### 5.2.3 Photo Randomization
- **Shuffle Algorithm**: Fisher-Yates shuffle (O(n) complexity)
- **Shuffle Timing**: On slideshow start and when all photos shown
- **Shuffle Scope**: All photos in collection (no weighting or recency bias)
- **Re-shuffle Behavior**: Prevent immediate repeats (last 10% of previous shuffle excluded from start of new shuffle)

### 5.3 Slideshow Display

#### 5.3.1 Display Configuration
- **Screen Mode**: Full-screen immersive (hide status bar, navigation bar)
- **Orientation**: Landscape locked (no auto-rotate)
- **Background**: Pure black (#000000)
- **Aspect Ratio Handling**: ContentScale.Fit (letterbox/pillarbox)

#### 5.3.2 Transition Effects

**1. Fade Transition** (Default):
- Crossfade between photos using alpha blending
- Duration: 500ms
- Easing: LinearEasing
- Implementation: Compose Crossfade composable

**2. Slide Transition**:
- Pan from edge to edge
- Direction: Random (left, right, top, bottom)
- Duration: 1000ms
- Easing: DecelerateEasing (starts fast, ends slow)
- Implementation: Compose animateOffsetAsState

**3. Zoom/Ken Burns Transition**:
- Slow zoom and pan effect inspired by Ken Burns documentary style
- Duration: 10-15 seconds per photo (longer than display duration)
- Start: Scale 1.0x, random corner position
- End: Scale 1.3x, opposite corner position
- Easing: CubicBezierEasing (0.4, 0.0, 0.2, 1.0)
- Implementation: Compose scale and offset animations

#### 5.3.3 Display Timing
- **Configurable Durations**: 5s, 10s (default), 15s, 30s, 60s, 120s
- **Timing Precision**: Use Choreographer for frame-perfect timing
- **Timing Independence**: Photo load time does not affect display duration (buffering decouples loading from display)

### 5.4 Scheduling & Automation

#### 5.4.1 Schedule Configuration
- **Schedule Parameters**:
  - Enable/disable toggle
  - Start time (default: 7:00 AM)
  - End time (default: 11:00 PM)
  - Days of week (default: all days, Mon-Sun)
  - Timezone: Device local timezone

#### 5.4.2 Schedule Execution
- **Implementation**:
  - Use **AlarmManager** with setExactAndAllowWhileIdle()
  - BroadcastReceiver for alarm intent
  - Foreground service to keep app alive during active hours
  - WakeLock to control screen on/off

- **Wake Behavior** (at start time):
  1. Acquire WAKE_LOCK (screen turns on)
  2. Start foreground service with notification
  3. Launch slideshow activity
  4. Set screen brightness to configured level

- **Sleep Behavior** (at end time):
  1. Stop slideshow
  2. Release WAKE_LOCK (screen turns off)
  3. Stop foreground service
  4. App goes to background

#### 5.4.3 Boot & Persistence
- **Boot Receiver**: RECEIVE_BOOT_COMPLETED to restart schedule after device reboot
- **Schedule Persistence**: Schedule settings saved in DataStore, reapplied on boot
- **Crash Recovery**: If app crashes during active hours, auto-restart within 5 minutes

### 5.5 Settings & Configuration

#### 5.5.1 Settings Organization
**SMB Configuration Section**:
- SMB host/IP
- Share name
- Path (optional, default: /)
- Username
- Password (masked)
- Test Connection button
- Scan Network button

**Slideshow Settings Section**:
- Display duration (dropdown)
- Transition effect (radio buttons with preview)
- Random vs. Sequential (toggle, default: random)

**Schedule Settings Section**:
- Enable schedule (toggle)
- Start time (time picker)
- End time (time picker)
- Days of week (multi-select checkboxes)

**Display Settings Section**:
- Brightness level (slider, 0-100%)
- Screen timeout (never, 1min, 5min, 15min)
- Orientation lock (toggle, default: landscape)

**Advanced Settings Section**:
- Supported file formats (info only)
- Cache management (clear cache button)
- Error log (view skipped photos)
- About (app version, licenses)

#### 5.5.2 Settings Persistence
- Use **Preferences DataStore** (Jetpack)
- Automatic save on change (no explicit save button)
- Settings validation on input (inline error messages)
- Default values defined for all settings

#### 5.5.3 First-Run Experience
- On first launch, show settings screen with setup checklist:
  1. Configure SMB connection
  2. Test connection (must succeed)
  3. Select transition effect
  4. Configure schedule (optional)
  5. Start slideshow

### 5.6 Error Handling & Recovery

#### 5.6.1 Network Errors
- **Connection Lost**:
  - Continue slideshow for 2-3 buffered photos
  - Show overlay: "Connection lost. Reconnecting..."
  - Retry connection every 10 seconds (exponential backoff)
  - Resume slideshow when reconnected

- **SMB Authentication Failure**:
  - Pause slideshow
  - Show error: "Authentication failed. Check credentials."
  - Button: "Open Settings"

- **Timeout Errors**:
  - Retry up to 3 times with 5-second intervals
  - If persistent, show error screen

#### 5.6.2 Photo Loading Errors
- **Corrupted Photo**:
  - Skip to next photo
  - Show toast: "Skipped 1 corrupted photo"
  - Log error with file path
  - Blacklist photo from future iterations

- **Out of Memory**:
  - Clear image cache
  - Reduce buffer size to 1 photo
  - Show warning toast: "Memory low - reducing quality"

- **Unsupported Format**:
  - Skip photo during scan (don't include in database)

#### 5.6.3 System Errors
- **App Crash**:
  - Android crash handler shows standard error dialog
  - On restart, resume slideshow if within scheduled hours

- **Foreground Service Killed**:
  - Schedule automatically restarts service within 5 minutes
  - Show notification: "Slideshow restarting..."

---

## 6. Non-Functional Requirements (High-Level)

### 6.1 Performance

#### 6.1.1 Responsiveness
- **UI Responsiveness**: All UI interactions <100ms response time
- **Photo Load Time**: Average <2 seconds per photo (local network)
- **Transition Smoothness**: 60fps during all transitions (16ms frame time)
- **Scan Speed**: 100+ files per second during initial folder scan

#### 6.1.2 Resource Usage
- **Memory**: <300MB resident memory with 2-3 photos buffered
- **CPU**: <10% average CPU usage during slideshow (idle between transitions)
- **Network**: <5Mbps sustained bandwidth (typical for 4K JPEG streaming)
- **Battery**: Screen-on power consumption only (no excessive background processing)

#### 6.1.3 Scalability
- Support photo collections up to 50,000 photos
- Initial scan of 10,000 photos completes in <60 seconds
- Database queries return results in <100ms

### 6.2 Security

#### 6.2.1 Credential Protection
- SMB credentials encrypted at rest (EncryptedSharedPreferences + Keystore)
- No credentials in logs, crash reports, or analytics
- Credentials never transmitted outside local network

#### 6.2.2 Network Security
- SMB connection restricted to local network (RFC 1918 addresses)
- No internet-based SMB connections allowed (security risk)
- Optional SMB signing for encrypted connections

#### 6.2.3 App Security
- No sensitive permissions requested (camera, microphone, contacts, etc.)
- Minimal permission set: INTERNET, ACCESS_NETWORK_STATE, WAKE_LOCK, SCHEDULE_EXACT_ALARM

### 6.3 Accessibility

#### 6.3.1 Screen Reader Support
- All settings accessible via TalkBack
- Content descriptions for all interactive elements
- Semantic grouping for settings sections

#### 6.3.2 Visual Accessibility
- High-contrast settings UI (Material Design 3 standards)
- Large touch targets (48dp minimum)
- Clear, readable font sizes (Material Design type scale)

#### 6.3.3 Alternative Interactions
- Gesture access to settings (swipe, long-press)
- Keyboard navigation support (for external keyboards)

### 6.4 Reliability

#### 6.4.1 Stability
- **Target**: Zero crashes per 24-hour period
- **Uptime**: 95%+ during scheduled hours
- **Recovery**: Auto-restart after crash within 5 minutes

#### 6.4.2 Data Integrity
- Settings persisted atomically (no partial writes)
- Database transactions for photo list updates
- Graceful degradation on storage errors

### 6.5 Compatibility

#### 6.5.1 Android Version Support
- **Target SDK**: Android 13 (API 33)
- **Minimum SDK**: Android 8.0 (API 26)
- **Testing Matrix**: Android 8.0, 10, 11, 12, 13, 14

#### 6.5.2 Device Support
- **Primary**: Android tablets 7" and larger
- **Screen Resolutions**: 1024x600 up to 2560x1600
- **Aspect Ratios**: 16:10, 16:9, 4:3
- **Tested Devices**: Samsung Galaxy Tab A, Lenovo Tab M10, Amazon Fire HD 10

#### 6.5.3 SMB Server Compatibility
- **Protocols**: SMB v2.1, v3.0, v3.1.1
- **Servers**: Samba 3.6+, Windows Server 2012+, NAS devices (Synology, QNAP, WD MyCloud)
- **Authentication**: NTLM, NTLMv2, Kerberos (optional)

### 6.6 Usability

#### 6.6.1 Ease of Setup
- First-time setup completable in <15 minutes
- Network discovery simplifies SMB configuration
- Clear error messages with actionable guidance

#### 6.6.2 Ease of Use
- Zero-touch operation after setup (kiosk mode)
- Intuitive settings organization (single screen)
- Helpful defaults (10s duration, fade transition, 7am-11pm schedule)

---

## 7. Constraints & Dependencies

### 7.1 Technical Constraints

#### 7.1.1 Platform Constraints
- **Android Only**: No iOS, web, or desktop versions in MVP
- **Tablet Focus**: Phone UI not optimized (tablet 7"+ required)
- **Network Dependency**: Requires local network connectivity (no offline mode)
- **SMB Protocol**: Limited by SMB client library capabilities (jcifs-ng or smbj)

#### 7.1.2 Library Dependencies
- **Jetpack Compose**: 1.5+ required
- **Kotlin**: 1.9+ required
- **Coroutines & Flow**: Kotlin 1.7+ required
- **Hilt/Dagger**: Dependency injection framework
- **Room**: Database library for caching
- **Coil or Glide**: Image loading (decision pending)
- **jcifs-ng or smbj**: SMB client library (decision pending)

#### 7.1.3 Android API Constraints
- **Doze Mode**: AlarmManager limited on Android 6+ (requires SCHEDULE_EXACT_ALARM)
- **Background Restrictions**: Foreground service required for reliable scheduling (Android 8+)
- **Scoped Storage**: SMB files external to app, not subject to scoped storage
- **Network Security**: Cleartext traffic must be allowed for SMB (network_security_config.xml)

### 7.2 Resource Constraints

#### 7.2.1 Development Resources
- **Timeline**: 3-4 month development cycle (Q2 2026)
- **Team Size**: Assume small team (1-2 developers)
- **Design Resources**: Material Design 3 guidelines (no custom design)

#### 7.2.2 Testing Resources
- **Device Lab**: Limited to 3-5 test devices
- **Network Lab**: Home network setup for SMB testing
- **User Testing**: Small beta group (10-20 users)

### 7.3 External Dependencies

#### 7.3.1 Network Infrastructure
- **WiFi Network**: Users must have stable WiFi network
- **SMB Server**: Users must have SMB-compatible storage (NAS, Windows share, Samba)
- **Router Configuration**: No dependencies on router settings (port forwarding, etc.)

#### 7.3.2 Third-Party Services
- **No Cloud Dependencies**: MVP does not use cloud services (Google Photos deferred to Phase 2)
- **No Analytics**: No third-party analytics in MVP (optional in future)
- **No Crash Reporting**: Use Android built-in crash reports (no Crashlytics/Sentry in MVP)

### 7.4 Compliance & Policy

#### 7.4.1 Privacy
- **No Data Collection**: App does not collect user data or photos
- **Local Processing**: All processing on-device (no server uploads)
- **Privacy Policy**: Required for Google Play Store (document local data handling)

#### 7.4.2 Permissions
- **Minimal Permissions**: Only request essential permissions
- **Permission Rationale**: Explain why each permission is needed
- **Runtime Permissions**: Request SCHEDULE_EXACT_ALARM at runtime (Android 12+)

#### 7.4.3 Google Play Store
- **Target API**: Must target API 33+ (Google Play requirement)
- **64-bit Support**: Required for Play Store
- **Content Rating**: Rated for all ages (no sensitive content)

### 7.5 Known Limitations

#### 7.5.1 Feature Limitations
- **No Cloud Storage**: Google Photos, Dropbox, OneDrive deferred to Phase 2
- **No Video Support**: Only static photos (no video/GIF support)
- **No Audio**: No background music or audio playback
- **No Overlays**: No weather, time, or caption overlays
- **No Touch Interactions**: No photo manipulation (zoom, pan, rotate) during slideshow

#### 7.5.2 Technical Limitations
- **No Offline Mode**: Requires network connection (no cached slideshow)
- **No Live Updates**: Photo list not updated in real-time (requires app restart)
- **No Multi-Source**: Only one SMB share at a time (no merging multiple sources)
- **No User Profiles**: Single global configuration (no per-user or per-location profiles)

---

## 8. Out of Scope (Future Enhancements)

### 8.1 Phase 2 Features (Future Consideration)

#### 8.1.1 Cloud Storage Integration
- Google Photos API integration
- Dropbox API integration
- OneDrive API integration
- iCloud Photos (via web API)
- OAuth 2.0 authentication flows

#### 8.1.2 Advanced Display Features
- Weather overlays (temperature, conditions)
- Time/date overlays
- Custom caption overlays (photo metadata, location)
- Clock screensaver mode
- Multi-photo layouts (collage mode)

#### 8.1.3 Media Enhancements
- Video file support (.mp4, .mov)
- Animated GIF support
- Background music playback (ambient audio)
- Audio narration (text-to-speech captions)

#### 8.1.4 Content Curation
- Smart albums (face recognition, location grouping)
- Manual photo selection/favorites
- Photo ratings/likes
- Curated playlists
- Time-based filtering (show only recent photos)

### 8.2 Advanced Features (Long-Term)

#### 8.2.1 Multi-Device & Sync
- Multi-device sync (shared configuration)
- Remote control via companion mobile app
- Web dashboard for management
- Multi-zone support (different slideshows in different rooms)

#### 8.2.2 Interactive Features
- Touch interaction (swipe to skip, tap for info)
- Photo zooming and panning (pinch gesture)
- Photo sharing (via QR code or AirDrop)
- Guest mode (temporary photo uploads)

#### 8.2.3 Smart Features
- AI-powered photo curation (quality scoring)
- Face recognition (prioritize photos with people)
- Scene detection (show landscape photos in morning, portraits at night)
- Adaptive timing (slow down for interesting photos)

#### 8.2.4 Platform Expansion
- iOS/iPadOS version
- Web version (progressive web app)
- Desktop version (Windows, macOS, Linux)
- Smart TV app (Android TV, Apple TV)

### 8.3 Explicitly Not Included

#### 8.3.1 Content Creation
- No photo editing or filters
- No collage creation tools
- No text overlay editor
- No drawing/annotation tools

#### 8.3.2 Social Features
- No social media integration
- No photo sharing to social networks
- No comments or likes
- No multi-user accounts

#### 8.3.3 E-Commerce
- No in-app purchases
- No premium tier (free app)
- No advertising
- No sponsored content

#### 8.3.4 Hardware Integration
- No smart home integration (HomeKit, Google Home)
- No voice control (Alexa, Google Assistant)
- No motion sensors or presence detection
- No external display output (HDMI)

---

## 9. Success Metrics

### 9.1 Launch Criteria (Go/No-Go Metrics)

#### 9.1.1 Functional Completeness
- [ ] All P0 user stories implemented and tested
- [ ] SMB connection success rate >90% in testing
- [ ] Zero known critical bugs
- [ ] All acceptance criteria met for P0 stories

#### 9.1.2 Performance Benchmarks
- [ ] Photo load time <2s average (tested on 3 devices)
- [ ] Transition frame rate 60fps on mid-range tablets
- [ ] App startup time <3 seconds
- [ ] Memory usage <300MB during slideshow

#### 9.1.3 Stability
- [ ] Zero crashes during 24-hour stress test
- [ ] Schedule reliability >95% (wake/sleep timing within 1 minute)
- [ ] Network reconnection success rate >90%

#### 9.1.4 Usability
- [ ] First-time setup completable by non-technical users in <20 minutes (user testing)
- [ ] Settings navigation intuitive (no user confusion in testing)
- [ ] Error messages clear and actionable (user testing validation)

### 9.2 Post-Launch Success Metrics

#### 9.2.1 User Engagement (30 Days Post-Launch)
- **Daily Active Users (DAU)**: Target 80% of installs (kiosk mode = high daily usage)
- **Average Session Duration**: Target 8+ hours per day (scheduled hours)
- **Settings Changes**: <1 per week per user (indicates "set and forget" success)
- **App Retention**: 90% 30-day retention (users keep app installed)

#### 9.2.2 Technical Performance (30 Days Post-Launch)
- **Crash-Free Sessions**: >99.5% (Google Play Console metric)
- **ANR Rate**: <0.1% (Android Not Responding errors)
- **Average Photo Load Time**: <2 seconds (in-app analytics)
- **Schedule Reliability**: >95% wake/sleep success rate

#### 9.2.3 User Satisfaction (60 Days Post-Launch)
- **User Ratings**: Average 4.0+ stars on Google Play Store
- **Review Sentiment**: 70%+ positive reviews
- **Support Tickets**: <5% of users require support
- **Feature Requests**: Track top 10 requested features for Phase 2

#### 9.2.4 Growth Metrics (90 Days Post-Launch)
- **Install Growth**: 20% month-over-month growth
- **Organic Discovery**: 50%+ installs from search (not referrals)
- **User Referrals**: 10% of installs from word-of-mouth

### 9.3 Key Performance Indicators (KPIs)

#### 9.3.1 Technical KPIs
1. **Uptime**: 95%+ during scheduled hours
2. **Crash Rate**: <0.5% of sessions
3. **Photo Load Success Rate**: >98% of photo loads succeed
4. **Network Reconnection Time**: <30 seconds average

#### 9.3.2 User Experience KPIs
1. **Setup Completion Rate**: >80% of installs complete setup
2. **Feature Adoption**: 60%+ users enable scheduling
3. **Transition Usage**: 40%+ users change from default transition
4. **Settings Adjustments**: <5 per user lifetime (low = good)

#### 9.3.3 Business KPIs
1. **User Retention**: 90% 30-day retention, 70% 90-day retention
2. **Rating & Reviews**: 4.0+ average, 100+ reviews in first 90 days
3. **Support Burden**: <3% of users contact support
4. **Development Velocity**: Phase 2 start within 30 days of Phase 1 launch

### 9.4 Monitoring & Measurement

#### 9.4.1 Analytics Instrumentation
- **No Third-Party Analytics in MVP**: Use Android Vitals and Play Console only
- **Optional Future Analytics**: Firebase Analytics for detailed event tracking (Phase 2)

#### 9.4.2 Tracked Events (Future, Optional)
- App launch and session duration
- Settings changes (which settings, how often)
- Photo load times and failures
- Network errors and reconnections
- Transition effect usage
- Schedule adherence (wake/sleep events)

#### 9.4.3 User Feedback Collection
- In-app feedback form (optional, non-intrusive)
- Google Play Store review monitoring
- Beta tester feedback surveys (Google Forms)
- Support email for bug reports

---

## 10. Open Questions & Risks

### 10.1 Open Questions

#### 10.1.1 Technical Questions
**Q1: SMB Library Selection**
- **Question**: Use jcifs-ng or smbj for SMB client?
- **Context**: Both libraries support SMB v2/v3, but differ in performance and stability
- **Impact**: Performance, compatibility, maintenance burden
- **Resolution Plan**: Prototype both libraries, benchmark performance, make decision in Architecture phase
- **Blocker**: No (decision can be made during implementation)

**Q2: Image Loading Library**
- **Question**: Use Coil (Compose-native) or Glide (mature, widely used)?
- **Context**: Coil is newer and Compose-friendly, Glide is battle-tested
- **Impact**: Development velocity, image loading performance
- **Resolution Plan**: Test both with SMB image loading, evaluate ease of integration
- **Blocker**: No

**Q3: Database Schema Design**
- **Question**: Store full photo metadata in Room or just file paths?
- **Context**: More metadata enables future features (filtering, sorting) but increases complexity
- **Impact**: Database size, scan performance, future extensibility
- **Resolution Plan**: Define minimal schema for MVP (paths only), design for future expansion
- **Blocker**: No

#### 10.1.2 UX Questions
**Q4: Settings Access Gesture**
- **Question**: Swipe from edge, long-press, or both?
- **Context**: Need intuitive but non-intrusive way to access settings during slideshow
- **Impact**: Discoverability, accidental activations
- **Resolution Plan**: User testing with beta users, collect feedback on preferred gesture
- **Blocker**: No (can iterate post-launch)

**Q5: First-Run Experience**
- **Question**: Show interactive tutorial or just settings screen?
- **Context**: Balance between onboarding and simplicity
- **Impact**: Setup completion rate
- **Resolution Plan**: A/B test with beta users (tutorial vs. no tutorial)
- **Blocker**: No

#### 10.1.3 Business Questions
**Q6: Monetization Strategy**
- **Question**: Free app or paid app? In-app purchases for premium features?
- **Context**: MVP scope suggests free app, but Phase 2 features could be premium
- **Impact**: Revenue, user adoption
- **Resolution Plan**: Launch MVP as free app, evaluate monetization for Phase 2
- **Blocker**: No

### 10.2 Known Risks

#### 10.2.1 Technical Risks

**Risk 1: SMB Library Compatibility Issues**
- **Description**: jcifs-ng or smbj may not support all NAS devices or SMB configurations
- **Likelihood**: Medium
- **Impact**: High (core feature failure)
- **Mitigation**:
  - Test with wide variety of NAS devices (Synology, QNAP, WD, Windows shares)
  - Implement fallback to alternative library if one fails
  - Provide detailed error messages for unsupported configurations
- **Contingency**: Add explicit list of supported/tested NAS devices in documentation

**Risk 2: Android Doze Mode Interference**
- **Description**: Aggressive battery optimization on some devices may kill scheduled wakes
- **Likelihood**: Medium
- **Impact**: High (schedule feature unreliable)
- **Mitigation**:
  - Use AlarmManager with setExactAndAllowWhileIdle()
  - Implement foreground service with WAKE_LOCK
  - Document battery optimization exclusion requirement
- **Contingency**: Add in-app check for battery optimization settings, prompt user to disable

**Risk 3: Memory Issues with Large Photos**
- **Description**: Loading 4K+ photos may cause OutOfMemoryError on low-end tablets
- **Likelihood**: Low (downsampling mitigates)
- **Impact**: High (app crash)
- **Mitigation**:
  - Aggressive downsampling to screen resolution
  - Reduce buffer size if memory pressure detected
  - Use Bitmap recycling and memory-efficient image formats
- **Contingency**: Add "Low Memory Mode" setting to disable buffering

#### 10.2.2 User Experience Risks

**Risk 4: Complex SMB Setup Deters Users**
- **Description**: Non-technical users may struggle with SMB host/share/credentials setup
- **Likelihood**: Medium
- **Impact**: Medium (high abandon rate during setup)
- **Mitigation**:
  - Network discovery simplifies setup (auto-detect shares)
  - Clear help text and examples in settings
  - Video tutorial or illustrated guide
- **Contingency**: Add simplified "Quick Setup" wizard with step-by-step guidance

**Risk 5: Transition Effects Cause Motion Sickness**
- **Description**: Ken Burns effect may be too fast or disorienting for some users
- **Likelihood**: Low
- **Impact**: Low (users can disable)
- **Mitigation**:
  - Default to simple Fade transition
  - Make transition speed configurable (slow/medium/fast)
  - Add warning in settings for motion-sensitive users
- **Contingency**: Add "Reduced Motion" accessibility option

#### 10.2.3 External Risks

**Risk 6: Google Play Store Policy Changes**
- **Description**: Google may change policies around permissions, background services, or network access
- **Likelihood**: Low
- **Impact**: High (app may be rejected or removed)
- **Mitigation**:
  - Follow all current Play Store guidelines
  - Monitor Google Play policy updates
  - Maintain clear privacy policy
- **Contingency**: Adapt app to policy changes, may require feature removal

**Risk 7: SMB Protocol Security Vulnerabilities**
- **Description**: SMB protocol has history of security issues (e.g., WannaCry)
- **Likelihood**: Low (local network only)
- **Impact**: Medium (reputation risk)
- **Mitigation**:
  - Restrict SMB connections to local network only (RFC 1918 addresses)
  - Keep SMB library updated with security patches
  - Document security best practices for users
- **Contingency**: Add security warning in settings, require user acknowledgment

#### 10.2.4 Project Risks

**Risk 8: Timeline Slippage**
- **Description**: 3-4 month timeline aggressive for XL story with small team
- **Likelihood**: Medium
- **Impact**: Medium (delayed launch)
- **Mitigation**:
  - Prioritize P0 features strictly
  - Defer P1/P2 features to post-MVP
  - Use agile sprints with weekly check-ins
- **Contingency**: Extend timeline or reduce scope (cut P1 features)

**Risk 9: Limited Testing Coverage**
- **Description**: Small device lab may not cover all tablet models and Android versions
- **Likelihood**: High
- **Impact**: Medium (bugs on untested devices)
- **Mitigation**:
  - Test on most popular tablet models (Samsung, Lenovo)
  - Use Android Emulator for version coverage
  - Recruit beta testers with diverse devices
- **Contingency**: Staged rollout (1% -> 10% -> 100%) to catch device-specific issues

### 10.3 Risk Monitoring Plan

#### 10.3.1 Risk Review Cadence
- **Weekly**: Review risks in sprint planning
- **Milestone-based**: Re-evaluate risks after each major milestone (SMB integration complete, slideshow functional, etc.)
- **Pre-launch**: Final risk assessment before Play Store submission

#### 10.3.2 Risk Escalation
- **Critical risks** (High likelihood + High impact): Escalate immediately, consider scope changes
- **Major risks** (Medium/High likelihood or impact): Implement mitigation plan, monitor closely
- **Minor risks** (Low likelihood and impact): Accept risk, document contingency

---

## 11. Appendices

### 11.1 Glossary

- **SMB**: Server Message Block, network protocol for file sharing
- **Samba**: Open-source SMB server implementation for Linux/Unix
- **NAS**: Network-Attached Storage, dedicated file storage device
- **NetBIOS**: Network Basic Input/Output System, legacy network naming protocol
- **mDNS**: Multicast DNS, zero-configuration networking protocol
- **Ken Burns Effect**: Slow zoom and pan effect named after documentary filmmaker
- **Letterbox**: Black bars on top/bottom of image
- **Pillarbox**: Black bars on left/right of image
- **HEIC**: High Efficiency Image Container, iOS photo format
- **EXIF**: Exchangeable Image File Format, photo metadata standard
- **Doze Mode**: Android battery optimization feature that restricts background activity
- **AlarmManager**: Android API for scheduling tasks at specific times
- **WakeLock**: Android API for keeping device awake
- **Foreground Service**: Android service that runs with user awareness (persistent notification)
- **DataStore**: Jetpack library for key-value data storage (successor to SharedPreferences)
- **Room**: Jetpack library for SQLite database access
- **Coil**: Compose Image Loading library
- **Glide**: Image loading and caching library for Android
- **Hilt**: Dependency injection library (based on Dagger)
- **Jetpack Compose**: Modern declarative UI toolkit for Android

### 11.2 References

#### 11.2.1 Technical Documentation
- **Jetpack Compose**: https://developer.android.com/jetpack/compose
- **Android AlarmManager**: https://developer.android.com/reference/android/app/AlarmManager
- **SMB Protocol (MS-SMB2)**: https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-smb2/
- **jcifs-ng Library**: https://github.com/AgNO3/jcifs-ng
- **smbj Library**: https://github.com/hierynomus/smbj
- **Coil Image Loading**: https://coil-kt.github.io/coil/
- **Material Design 3**: https://m3.material.io/

#### 11.2.2 Related Features & Apps
- **Google Photos Screensaver**: https://support.google.com/photos/answer/6128850
- **Dayframe (Discontinued)**: Historical reference for photo frame app UX
- **PhotoSync**: Example of cloud photo sync app

#### 11.2.3 Design Assets
- **Material Design Icons**: https://fonts.google.com/icons
- **Material Design Type Scale**: https://m3.material.io/styles/typography/type-scale-tokens

### 11.3 Change Log

| Version | Date       | Author            | Changes                                      |
|---------|------------|-------------------|----------------------------------------------|
| 1.0     | 2026-03-01 | Product Owner Agent | Initial PRD draft based on Refinement Q&A  |

### 11.4 Approval & Sign-Off

| Role               | Name                | Signature | Date       |
|--------------------|---------------------|-----------|------------|
| Product Owner      | [TBD]               |           |            |
| Technical Lead     | [TBD]               |           |            |
| UX Designer        | [TBD]               |           |            |
| Stakeholder        | [TBD]               |           |            |

---

## 12. Next Steps

Upon approval of this PRD:

1. **Phase 3: Architecture Design** (1-2 weeks)
   - Architect reviews PRD
   - Designs system architecture
   - Creates technical design document
   - Defines component structure and data flow

2. **Phase 4: Implementation Plan** (1 week)
   - Break down into implementation tasks
   - Define sprint structure (2-week sprints)
   - Estimate story points for each user story
   - Create development schedule

3. **Phase 5: NFR Validation** (Parallel with Architecture)
   - Validate performance requirements
   - Define testing strategy
   - Identify performance bottlenecks
   - Plan optimization approach

4. **Development Kickoff** (Week 4)
   - Sprint 1: SMB integration and settings UI
   - Sprint 2: Photo scanning and slideshow display
   - Sprint 3: Transition effects and buffering
   - Sprint 4: Scheduling and automation
   - Sprint 5: Error handling and polish
   - Sprint 6: Testing and bug fixes

---

**Document Status**: DRAFT - Pending Review
**Next Review Date**: TBD
**Contact**: [Product Owner Email]

---

*This PRD represents the MVP scope for Phase 1 of the Digital Photo Frame project. Cloud integration (Google Photos, Dropbox, OneDrive) and advanced features are deferred to Phase 2. All decisions documented herein are based on the Requirements Refinement Q&A conducted on 2026-03-01.*
