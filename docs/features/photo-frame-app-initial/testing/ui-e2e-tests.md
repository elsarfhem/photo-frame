# UI & E2E Test Plan - Digital Photo Frame App (MVP Phase 1)

**Feature**: Digital Photo Frame - Android Tablet Application (MVP Phase 1)
**Test Scope**: UI & End-to-End Tests
**QA Agent**: QA 2 - UI & E2E Tests focused
**Date**: 2026-03-02
**Phase**: Phase 6 - Test Planning
**Status**: READY FOR TEAM REVIEW

---

## 1. Executive Summary

### Test Scope
This test plan covers **UI component tests** (Jetpack Compose Testing) and **end-to-end user flow tests** (Espresso/UI Automator) for the Digital Photo Frame app, focusing on user-facing behaviors, visual correctness, user interactions, error states, and complete user journeys from first-time setup to 24/7 kiosk operation.

### Test Coverage Summary
- **Total Test Scenarios**: 38
- **Total Test Cases**: 142
- **Estimated Effort**: 100-120 hours (3-4 weeks)
- **Target Coverage**: 100% of user-visible screens, 100% of user interaction flows, 95%+ of error states
- **Requirements Coverage**: 100% of 12 user stories (UI/UX aspects), all P0 NFR UI/reliability criteria

### Critical Focus Areas (Based on NFR Assessments)
1. **Transition Effects Testing (P0)**: 60fps smooth transitions (Fade, Slide, Zoom/Ken Burns), visual regression testing
2. **Error State UI Testing (P0)**: All error scenarios display clear messages, retry buttons functional, graceful degradation
3. **24/7 Kiosk Mode Testing (P0)**: Continuous operation, screen stays on, auto-recovery from errors, settings hidden/accessible via gesture
4. **Large Collection UX Testing (P0)**: Loading indicators during 10K photo scan, responsive UI during background operations, no UI freezes
5. **E2E Reliability Testing (P0)**: First-time setup flow, daily usage flow, error recovery flow, schedule automation flow

### Test Environment
- **UI Testing**: Jetpack Compose Testing (ComposeTestRule), Screenshot Testing (Shot/Paparazzi)
- **E2E Testing**: Espresso, UI Automator, Android Test Orchestrator
- **Visual Regression**: Baseline screenshots for transition effects, error states, layout correctness
- **Test Devices**: Physical tablets (Samsung Galaxy Tab, Amazon Fire HD), emulators (API 26, 30, 34)
- **Test SMB Server**: Docker Samba with test photo sets (small: 10 photos, medium: 100 photos, large: 10K photos)

---

## 2. Test Strategy

### Scope

**What This Test Plan Covers**:
- UI component tests (Slideshow screen, Settings screen, Error dialogs, Navigation)
- User interaction tests (swipe, tap, gesture recognition)
- Visual regression tests (transitions, photo display, layout correctness)
- End-to-end user flows (setup, daily usage, settings changes, error recovery, schedule automation)
- 24/7 kiosk mode behavior (continuous operation, auto-start, screen wake lock)
- Error state UI (connection errors, no photos, loading states, network disconnect)

**What QA 1's Test Plan Covers** (to avoid duplication):
- Business logic (slideshow sequencing, photo shuffling, folder scanning)
- Data layer (SMB connection, settings persistence, caching)
- Security (Keystore encryption, SMB 2.0+)
- Unit-level reliability (network failure recovery, memory leak detection)

**Overlap & Integration Points with QA 1**:
- **Settings Validation**: QA 1 tests persistence logic → QA 2 tests UI validation and error messages
- **SMB Connection**: QA 1 tests connection logic → QA 2 tests connection error UI and retry flow
- **Photo Buffer**: QA 1 tests buffer logic → QA 2 tests photo display rendering and transitions
- **Error Handling**: QA 1 tests error detection → QA 2 tests error UI presentation and user actions

### Test Pyramid

```
          /\
         /  \      E2E Tests (15%)
        /____\     - Full user flows
       /      \    - System integration
      /   UI   \   UI Component Tests (30%)
     /  Tests   \  - Screen rendering
    /____________\ - User interactions
   /              \
  /  Unit Tests    \ Unit Tests (55% - covered by QA 1)
 /    (QA 1)        \ - Business logic
/____________________\ - Data layer
```

**Test Distribution**:
- **UI Component Tests (30%)**: 100-120 tests
- **E2E Flow Tests (15%)**: 20-30 tests
- **Unit/Integration Tests (55% - QA 1)**: 168 tests

### Testing Frameworks

**Jetpack Compose Testing**:
- `ComposeTestRule` for component testing
- `onNodeWithTag()`, `onNodeWithText()`, `performClick()`, `assertIsDisplayed()`
- Semantics testing for accessibility
- State manipulation via `setContent()` and ViewModel injection

**Espresso & UI Automator**:
- Espresso for in-app navigation and interactions
- UI Automator for system-level interactions (notifications, screen wake, volume buttons)
- IdlingResource for async operations (photo loading, SMB connection)

**Screenshot Testing**:
- Shot or Paparazzi for visual regression
- Baseline images for transitions, error states, layout correctness
- Pixel-perfect comparison with tolerance for anti-aliasing

**Test Data**:
- Docker Samba server with 3 photo sets:
  - **Small**: 10 photos (480x640, 800x600), 5MB total
  - **Medium**: 100 photos (1920x1080, 1080x1920), 50MB total
  - **Large**: 10,000 photos (3840x2160), 5GB total
- Invalid photo files (corrupted JPEG, 0-byte files, non-image files)
- Network simulation (disconnect, slow connection, timeout)

### Coverage Mapping Strategy

**PRD User Story Mapping**:
- Each E2E test scenario maps to 1+ user stories
- Each UI component test maps to UI elements in user stories
- Coverage matrix in Section 10

**NFR Requirement Mapping**:
- Transition effects tests validate 60fps NFR
- Error state tests validate reliability NFR
- Large collection tests validate scalability NFR
- 24/7 kiosk tests validate >99.5% uptime NFR

---

## 3. UI Component Test Scenarios

### 3.1 Slideshow Screen Tests

**Scenario 1: Photo Display Rendering**
- **Priority**: P0
- **PRD Mapping**: US-001 (Basic Slideshow)
- **Test Cases** (8):
  1. Photo displays centered and properly scaled (letterbox/pillarbox)
  2. Photo aspect ratio preserved (no stretching)
  3. High-resolution photo (4K) displays without pixelation
  4. Portrait photo displays correctly on landscape screen
  5. Landscape photo displays correctly on portrait screen
  6. Square photo displays correctly
  7. Photo metadata (EXIF orientation) respected
  8. Photo displays with correct color profile (sRGB)

**Scenario 2: Slideshow Controls Visibility**
- **Priority**: P0
- **PRD Mapping**: US-003 (User Controls)
- **Test Cases** (6):
  1. Controls initially hidden in kiosk mode
  2. Single tap shows controls with fade-in animation
  3. Controls auto-hide after 5 seconds of inactivity
  4. Tap again hides controls immediately
  5. Controls stay visible during user interaction
  6. Controls accessible via semantics for accessibility testing

**Scenario 3: Pause/Resume Functionality**
- **Priority**: P0
- **PRD Mapping**: US-003 (User Controls)
- **Test Cases** (5):
  1. Pause button displays when slideshow is playing
  2. Tapping pause button stops slideshow and shows play icon
  3. Tapping play button resumes slideshow
  4. Photo does not advance while paused
  5. Resume continues from paused photo (does not skip)

**Scenario 4: Manual Navigation (Swipe)**
- **Priority**: P0
- **PRD Mapping**: US-003 (User Controls)
- **Test Cases** (7):
  1. Swipe left advances to next photo
  2. Swipe right goes back to previous photo
  3. Swipe at first photo wraps to last photo (circular)
  4. Swipe at last photo wraps to first photo
  5. Fast swipe (velocity > threshold) triggers navigation
  6. Slow swipe (< threshold) does not navigate
  7. Swipe during transition cancels current transition

**Scenario 5: Photo Index Display**
- **Priority**: P1
- **PRD Mapping**: US-001 (Basic Slideshow)
- **Test Cases** (4):
  1. Photo counter displays "1/10" for first photo
  2. Counter updates correctly as slideshow advances
  3. Counter displays during manual navigation
  4. Counter hidden in kiosk mode (shown only with controls)

**Scenario 6: Loading States**
- **Priority**: P0
- **PRD Mapping**: US-001 (Basic Slideshow)
- **Test Cases** (5):
  1. Loading spinner displays while fetching first photo
  2. Loading spinner displays during slow photo load
  3. Placeholder image displays before photo loads
  4. Loading spinner hidden once photo fully loaded
  5. Loading progress indicator displays for large photos (>5MB)

### 3.2 Settings Screen Tests

**Scenario 7: Settings Screen Layout**
- **Priority**: P0
- **PRD Mapping**: US-002 (Settings Configuration)
- **Test Cases** (8):
  1. All settings fields visible on screen
  2. SMB connection section displayed first
  3. Slideshow settings section displayed second
  4. Schedule settings section displayed third
  5. Save button enabled when fields valid
  6. Save button disabled when fields invalid
  7. Cancel button always enabled
  8. Screen scrollable if content exceeds viewport

**Scenario 8: SMB Connection Fields**
- **Priority**: P0
- **PRD Mapping**: US-002 (Settings Configuration)
- **Test Cases** (10):
  1. Server address field accepts valid IP (192.168.1.100)
  2. Server address field accepts valid hostname (nas.local)
  3. Folder path field displays default "/Photos"
  4. Username field accepts alphanumeric input
  5. Password field displays masked characters (dots)
  6. Password field has "show/hide password" toggle
  7. Test Connection button triggers connection validation
  8. Connection success shows green checkmark + success message
  9. Connection failure shows red X + error message
  10. Fields retain values after connection test

**Scenario 9: SMB Field Validation**
- **Priority**: P0
- **PRD Mapping**: US-002 (Settings Configuration)
- **Test Cases** (9):
  1. Empty server address shows "Required" error
  2. Invalid IP address shows "Invalid IP" error
  3. Empty folder path shows "Required" error
  4. Empty username shows "Required" error (if password filled)
  5. Empty password shows "Required" error (if username filled)
  6. Anonymous access allowed (username + password both empty)
  7. Folder path validates "/Photos" format (leading slash required)
  8. Error messages displayed below fields (inline validation)
  9. Save button disabled when any validation error present

**Scenario 10: Slideshow Interval Settings**
- **Priority**: P0
- **PRD Mapping**: US-002 (Settings Configuration)
- **Test Cases** (6):
  1. Interval slider displays current value (e.g., "15 seconds")
  2. Slider range: 5 seconds to 60 seconds
  3. Slider snaps to 5-second increments
  4. Default interval is 15 seconds
  5. Changing slider updates displayed value
  6. Saved interval applied to slideshow immediately

**Scenario 11: Transition Effect Selection**
- **Priority**: P0
- **PRD Mapping**: US-005 (Transition Effects)
- **Test Cases** (7):
  1. Transition dropdown displays 4 options: Fade, Slide, Zoom, Random
  2. Default selection is "Fade"
  3. Selecting "Slide" updates setting
  4. Selecting "Zoom" updates setting
  5. Selecting "Random" updates setting
  6. Preview button shows sample transition animation
  7. Selected transition applied to slideshow immediately after save

**Scenario 12: Schedule Settings**
- **Priority**: P1
- **PRD Mapping**: US-006 (Automated Scheduling)
- **Test Cases** (8):
  1. Enable Schedule toggle displayed
  2. Start time picker displays default 8:00 AM
  3. End time picker displays default 10:00 PM
  4. Time picker allows 24-hour format selection
  5. End time must be after start time (validation error if not)
  6. Schedule disabled by default
  7. Enabling schedule shows start/end time pickers
  8. Disabling schedule hides time pickers

**Scenario 13: Save & Cancel Actions**
- **Priority**: P0
- **PRD Mapping**: US-002 (Settings Configuration)
- **Test Cases** (6):
  1. Cancel button discards all changes and returns to slideshow
  2. Save button persists settings and returns to slideshow
  3. Save button shows loading spinner during save operation
  4. Save success shows toast "Settings saved successfully"
  5. Save failure shows error dialog with retry option
  6. Back button press acts as Cancel (shows confirmation dialog if changes unsaved)

### 3.3 Error State Tests

**Scenario 14: Connection Error UI**
- **Priority**: P0
- **PRD Mapping**: US-007 (Error Handling), NFR Reliability
- **Test Cases** (8):
  1. Connection error dialog displays when SMB connection fails
  2. Error message displays server address and error reason
  3. "Retry" button displayed in error dialog
  4. "Open Settings" button displayed in error dialog
  5. Tapping Retry re-attempts connection
  6. Tapping Open Settings navigates to settings screen
  7. Error dialog dismissible via back button (returns to blank screen)
  8. Error dialog auto-retries every 30 seconds (with countdown timer displayed)

**Scenario 15: No Photos Found UI**
- **Priority**: P0
- **PRD Mapping**: US-007 (Error Handling)
- **Test Cases** (5):
  1. "No photos found" message displays when folder is empty
  2. Message includes folder path
  3. "Open Settings" button displayed
  4. Tapping button navigates to settings to change folder path
  5. Message persists until folder with photos configured

**Scenario 16: Photo Load Error UI**
- **Priority**: P1
- **PRD Mapping**: US-007 (Error Handling)
- **Test Cases** (6):
  1. Error icon displays if photo fails to load
  2. Error message displays "Unable to load photo"
  3. Slideshow auto-advances to next photo after 5 seconds
  4. Skipped photo not counted in error stats
  5. After 5 consecutive errors, shows "Connection issue" dialog
  6. Error state does not crash slideshow

**Scenario 17: Network Disconnect UI**
- **Priority**: P0
- **PRD Mapping**: US-007 (Error Handling), NFR Reliability
- **Test Cases** (7):
  1. Network disconnect during slideshow shows warning toast
  2. Slideshow continues with cached photos
  3. When cache exhausted, shows "Network disconnected" error
  4. Error includes retry button
  5. Auto-retry every 30 seconds (with countdown)
  6. On network reconnect, success toast displayed
  7. Slideshow resumes from current position

### 3.4 Navigation & Screen Transition Tests

**Scenario 18: Slideshow to Settings Navigation**
- **Priority**: P0
- **PRD Mapping**: US-002 (Settings Configuration)
- **Test Cases** (5):
  1. Swipe down from top opens settings (when controls visible)
  2. Settings button in controls opens settings screen
  3. Transition animation: slide up from bottom
  4. Slideshow pauses when settings opened
  5. Settings screen displays with correct state

**Scenario 19: Settings to Slideshow Navigation**
- **Priority**: P0
- **PRD Mapping**: US-002 (Settings Configuration)
- **Test Cases** (4):
  1. Cancel button returns to slideshow
  2. Save button returns to slideshow
  3. Back button returns to slideshow (with confirmation if unsaved changes)
  4. Transition animation: slide down to bottom

**Scenario 20: First Launch Experience**
- **Priority**: P0
- **PRD Mapping**: US-002 (Settings Configuration)
- **Test Cases** (6):
  1. First launch shows settings screen (not slideshow)
  2. Welcome message displayed: "Configure your photo frame"
  3. No back button available (settings required)
  4. Cancel button disabled on first launch
  5. Save button enables when valid settings entered
  6. After first save, slideshow starts automatically

---

## 4. Transition Effect Test Scenarios

### 4.1 Fade Transition Tests

**Scenario 21: Fade Transition Visual Correctness**
- **Priority**: P0
- **PRD Mapping**: US-005 (Transition Effects), NFR Performance
- **Test Cases** (6):
  1. Outgoing photo fades to 0% opacity over 500ms
  2. Incoming photo fades from 0% to 100% opacity over 500ms
  3. No visible "pop" or flicker between photos
  4. Transition maintains 60fps (16.67ms per frame)
  5. Screenshot comparison: mid-transition alpha blending correct
  6. Transition duration matches configured value

**Scenario 22: Fade Transition Performance**
- **Priority**: P0
- **PRD Mapping**: NFR Performance (60fps)
- **Test Cases** (4):
  1. Fade transition with 4K photo maintains 60fps (frame rate profiling)
  2. No frame drops during transition (Choreographer frame callback monitoring)
  3. GPU memory usage stays below threshold (200MB)
  4. Transition completes within 500ms ± 50ms

### 4.2 Slide Transition Tests

**Scenario 23: Slide Transition Visual Correctness**
- **Priority**: P0
- **PRD Mapping**: US-005 (Transition Effects)
- **Test Cases** (7):
  1. Swipe left: outgoing photo slides left, incoming photo slides in from right
  2. Swipe right: outgoing photo slides right, incoming photo slides in from left
  3. Photos move in sync (no gap or overlap)
  4. Edge photos visible during transition (no blank space)
  5. Easing curve applied (not linear motion)
  6. Screenshot comparison: mid-transition positioning correct
  7. Transition duration matches configured value (500ms)

**Scenario 24: Slide Transition Performance**
- **Priority**: P0
- **PRD Mapping**: NFR Performance (60fps)
- **Test Cases** (4):
  1. Slide transition maintains 60fps (frame rate profiling)
  2. No frame drops during transition
  3. Smooth animation curve (no jank or stuttering)
  4. Transition completes within 500ms ± 50ms

### 4.3 Zoom/Ken Burns Transition Tests

**Scenario 25: Zoom Transition Visual Correctness**
- **Priority**: P1
- **PRD Mapping**: US-005 (Transition Effects)
- **Test Cases** (6):
  1. Photo starts at 100% scale, zooms to 110% over duration
  2. Pan offset randomized (focuses on different areas)
  3. Zoom center point not always image center (adds variety)
  4. Fade out at end of zoom
  5. Screenshot comparison: zoom scale and position correct
  6. Transition duration matches configured value (3-5 seconds)

**Scenario 26: Zoom Transition Performance**
- **Priority**: P1
- **PRD Mapping**: NFR Performance (60fps)
- **Test Cases** (4):
  1. Zoom transition with 4K photo maintains 60fps
  2. No frame drops during zoom animation
  3. GPU memory usage stays below threshold
  4. Smooth zoom (no stepping or pixelation)

### 4.4 Random Transition Tests

**Scenario 27: Random Transition Behavior**
- **Priority**: P1
- **PRD Mapping**: US-005 (Transition Effects)
- **Test Cases** (4):
  1. Random mode selects from Fade, Slide, Zoom
  2. Selection is random (not always same sequence)
  3. Each transition executes correctly
  4. Transition type logged for debugging

---

## 5. End-to-End Test Scenarios

### 5.1 First-Time Setup Flow

**Scenario 28: Complete First-Time Setup**
- **Priority**: P0
- **PRD Mapping**: US-002 (Settings Configuration)
- **Test Cases** (10):
  1. App launches to settings screen (not slideshow)
  2. Enter valid SMB server address
  3. Enter valid folder path
  4. Enter username and password
  5. Tap "Test Connection" button
  6. Success message displays
  7. Select transition effect (Fade)
  8. Set interval (15 seconds)
  9. Tap Save button
  10. Slideshow starts automatically with first photo

**Scenario 29: Setup Flow with Connection Error**
- **Priority**: P0
- **PRD Mapping**: US-002 (Settings Configuration), US-007 (Error Handling)
- **Test Cases** (8):
  1. Enter invalid SMB server address
  2. Tap "Test Connection"
  3. Error message displays: "Unable to connect"
  4. Correct server address
  5. Tap "Test Connection" again
  6. Success message displays
  7. Tap Save
  8. Slideshow starts

**Scenario 30: Setup Flow with Validation Errors**
- **Priority**: P0
- **PRD Mapping**: US-002 (Settings Configuration)
- **Test Cases** (7):
  1. Leave server address empty
  2. Tap Save button (disabled)
  3. Enter server address
  4. Leave folder path empty
  5. Tap Save button (disabled)
  6. Enter folder path
  7. Tap Save (enabled), slideshow starts

### 5.2 Daily Usage Flow (24/7 Kiosk Mode)

**Scenario 31: Auto-Start Slideshow on App Launch**
- **Priority**: P0
- **PRD Mapping**: US-001 (Basic Slideshow), NFR Reliability (24/7 operation)
- **Test Cases** (6):
  1. Kill app process
  2. Launch app (simulate device reboot)
  3. Slideshow starts automatically (no user interaction required)
  4. First photo displays within 2 seconds
  5. Slideshow advances every 15 seconds (configured interval)
  6. Controls hidden (kiosk mode)

**Scenario 32: Continuous Slideshow Operation (Long-Running Test)**
- **Priority**: P0
- **PRD Mapping**: US-001 (Basic Slideshow), NFR Reliability (>99.5% uptime)
- **Test Cases** (8):
  1. Start slideshow with 100 photos
  2. Run for 1 hour (240 photo transitions at 15s interval)
  3. Verify no crashes or ANRs
  4. Verify screen stays on (wake lock active)
  5. Verify memory usage stable (no memory leaks)
  6. Verify all photos displayed (no skips)
  7. Verify transitions smooth (no frame drops)
  8. Verify app responsive to user interaction (tap to pause)

**Scenario 33: Screen Wake Lock Behavior**
- **Priority**: P0
- **PRD Mapping**: NFR Reliability (24/7 operation)
- **Test Cases** (5):
  1. Start slideshow
  2. Verify screen stays on for 30 minutes (no auto-sleep)
  3. Tap pause button
  4. Verify screen still stays on (wake lock persists)
  5. Open settings, verify wake lock released (screen can sleep)

### 5.3 Settings Change Flow

**Scenario 34: Change Slideshow Interval**
- **Priority**: P0
- **PRD Mapping**: US-002 (Settings Configuration)
- **Test Cases** (7):
  1. Start slideshow (15 second interval)
  2. Wait for 2 photo transitions (verify 15s interval)
  3. Open settings
  4. Change interval to 30 seconds
  5. Save settings
  6. Return to slideshow
  7. Verify next transition at 30 seconds (new interval applied)

**Scenario 35: Change Transition Effect**
- **Priority**: P1
- **PRD Mapping**: US-005 (Transition Effects)
- **Test Cases** (6):
  1. Start slideshow with Fade transition
  2. Verify fade effect on next transition
  3. Open settings
  4. Change transition to Slide
  5. Save settings
  6. Verify slide effect on next transition

**Scenario 36: Change SMB Folder Path**
- **Priority**: P0
- **PRD Mapping**: US-002 (Settings Configuration)
- **Test Cases** (8):
  1. Start slideshow with folder "/Photos" (10 photos)
  2. Open settings
  3. Change folder path to "/Vacation" (5 photos)
  4. Tap "Test Connection" (verify folder accessible)
  5. Save settings
  6. Slideshow restarts with new folder
  7. Verify only 5 photos in rotation (from /Vacation)
  8. Verify photo counter shows "1/5"

### 5.4 Error Recovery Flow

**Scenario 37: Network Disconnect and Reconnect**
- **Priority**: P0
- **PRD Mapping**: US-007 (Error Handling), NFR Reliability
- **Test Cases** (10):
  1. Start slideshow (100 photos, 15s interval)
  2. Verify slideshow running smoothly
  3. Disable Wi-Fi on device (simulate network disconnect)
  4. Verify slideshow continues with cached photos
  5. Wait until cache exhausted (5 photos)
  6. Verify error dialog displays: "Network disconnected"
  7. Verify auto-retry countdown displayed (30 seconds)
  8. Enable Wi-Fi (simulate network reconnect)
  9. Verify success toast: "Connection restored"
  10. Verify slideshow resumes from current position

**Scenario 38: SMB Server Restart Recovery**
- **Priority**: P0
- **PRD Mapping**: US-007 (Error Handling), NFR Reliability
- **Test Cases** (8):
  1. Start slideshow
  2. Stop Docker Samba server (simulate server restart)
  3. Verify slideshow continues with cached photos
  4. Verify warning toast: "Connection issue, using cached photos"
  5. Cache exhausted, error dialog displays
  6. Auto-retry countdown displayed
  7. Restart Docker Samba server
  8. Verify slideshow resumes after auto-retry succeeds

**Scenario 39: App Crash and Auto-Recovery**
- **Priority**: P0
- **PRD Mapping**: NFR Reliability (>99.5% crash-free)
- **Test Cases** (6):
  1. Start slideshow
  2. Force app crash (via adb or test hook)
  3. Verify Android system restarts app (auto-restart mechanism)
  4. Verify slideshow resumes automatically
  5. Verify no user interaction required (kiosk mode)
  6. Verify slideshow continues from last known position

### 5.5 Schedule Automation Flow

**Scenario 40: Schedule Activation (Time-Accelerated Test)**
- **Priority**: P1
- **PRD Mapping**: US-006 (Automated Scheduling)
- **Test Cases** (8):
  1. Open settings
  2. Enable schedule: Start 8:00 AM, End 10:00 PM
  3. Save settings
  4. Set device time to 7:59 AM (before start)
  5. Verify slideshow not running (black screen or standby mode)
  6. Set device time to 8:00 AM (start time)
  7. Verify slideshow starts automatically
  8. Verify photo transitions as expected

**Scenario 41: Schedule Deactivation (Time-Accelerated Test)**
- **Priority**: P1
- **PRD Mapping**: US-006 (Automated Scheduling)
- **Test Cases** (6):
  1. Slideshow running (scheduled 8:00 AM - 10:00 PM)
  2. Set device time to 9:59 PM (before end)
  3. Verify slideshow still running
  4. Set device time to 10:00 PM (end time)
  5. Verify slideshow stops (black screen or standby mode)
  6. Verify wake lock released (screen can sleep)

**Scenario 42: Schedule Disabled Behavior**
- **Priority**: P1
- **PRD Mapping**: US-006 (Automated Scheduling)
- **Test Cases** (4):
  1. Open settings
  2. Disable schedule
  3. Save settings
  4. Verify slideshow runs 24/7 (no time restrictions)

---

## 6. Large Collection UX Test Scenarios

### 6.1 Initial Scan Loading UX

**Scenario 43: Large Collection Scan (10K Photos)**
- **Priority**: P0
- **PRD Mapping**: US-004 (Photo Management), NFR Scalability
- **Test Cases** (8):
  1. Configure settings with folder containing 10,000 photos
  2. Save settings
  3. Loading dialog displays: "Scanning folder..."
  4. Progress indicator shows percentage (0% → 100%)
  5. Estimated time remaining displayed (e.g., "2 minutes remaining")
  6. Cancel button available to abort scan
  7. First photo displays within 2 seconds (progressive loading)
  8. Slideshow starts while background scan continues

**Scenario 44: UI Responsiveness During Background Scan**
- **Priority**: P0
- **PRD Mapping**: US-004 (Photo Management), NFR Scalability
- **Test Cases** (6):
  1. Start slideshow with 10K photos (scan in progress)
  2. Verify photo transitions smooth (no UI freeze)
  3. Open settings during scan
  4. Verify settings screen responsive (no lag)
  5. Close settings, return to slideshow
  6. Verify slideshow continues uninterrupted

### 6.2 Large Collection Navigation UX

**Scenario 45: Swipe Navigation with Large Collection**
- **Priority**: P1
- **PRD Mapping**: US-003 (User Controls), NFR Scalability
- **Test Cases** (5):
  1. Load slideshow with 10K photos
  2. Swipe left 10 times rapidly
  3. Verify each photo displays within 2 seconds (no lag)
  4. Verify photo counter updates correctly (e.g., "10/10000")
  5. Verify no UI freezes or frame drops

**Scenario 46: Photo Shuffle with Large Collection**
- **Priority**: P1
- **PRD Mapping**: US-004 (Photo Management), NFR Scalability
- **Test Cases** (5):
  1. Enable shuffle in settings
  2. Load slideshow with 10K photos
  3. Verify shuffle algorithm completes within 2 seconds
  4. Verify photo order randomized (not sequential)
  5. Verify no duplicate photos displayed in first 100 transitions

---

## 7. Accessibility & Usability Test Scenarios

### 7.1 Accessibility Tests

**Scenario 47: TalkBack Support**
- **Priority**: P1
- **PRD Mapping**: NFR Usability
- **Test Cases** (6):
  1. Enable TalkBack on device
  2. Navigate to settings screen
  3. Verify all input fields have content descriptions
  4. Verify all buttons have semantic labels
  5. Verify form validation errors announced by TalkBack
  6. Verify focus order logical (top to bottom)

**Scenario 48: Font Scaling**
- **Priority**: P1
- **PRD Mapping**: NFR Usability
- **Test Cases** (4):
  1. Set device font size to Large
  2. Open settings screen
  3. Verify all text readable (no truncation)
  4. Verify layout does not break (scrollable if needed)

### 7.2 Landscape/Portrait Orientation Tests

**Scenario 49: Orientation Change Handling**
- **Priority**: P0
- **PRD Mapping**: NFR Usability
- **Test Cases** (6):
  1. Start slideshow in landscape orientation
  2. Verify photo displays correctly (landscape photo fills screen)
  3. Rotate device to portrait
  4. Verify photo re-renders correctly (portrait photo fills screen)
  5. Verify slideshow continues (no restart)
  6. Verify current photo position preserved (no skip)

**Scenario 50: Settings Screen Orientation**
- **Priority**: P1
- **PRD Mapping**: NFR Usability
- **Test Cases** (4):
  1. Open settings in landscape
  2. Verify form layout correct
  3. Rotate to portrait
  4. Verify form layout adjusts correctly (no overlap)

---

## 8. Test Data Requirements

### 8.1 Test Photo Sets

**Small Photo Set** (10 photos):
- **Dimensions**: 480x640 (portrait), 800x600 (landscape), 1024x1024 (square)
- **File Size**: 200KB - 500KB each
- **Total Size**: ~5MB
- **EXIF Data**: Various orientations (1, 3, 6, 8), timestamps, camera metadata
- **Use Case**: Quick smoke tests, transition effect validation

**Medium Photo Set** (100 photos):
- **Dimensions**: 1920x1080 (landscape), 1080x1920 (portrait)
- **File Size**: 500KB - 1MB each
- **Total Size**: ~50MB
- **Variety**: 70% landscape, 20% portrait, 10% square
- **Use Case**: Standard E2E tests, continuous operation tests (1 hour)

**Large Photo Set** (10,000 photos):
- **Dimensions**: 3840x2160 (4K landscape), 2160x3840 (4K portrait)
- **File Size**: 3MB - 8MB each
- **Total Size**: ~5GB
- **Use Case**: Scalability tests, memory leak detection, long-running stability tests

**Invalid Photo Set** (20 files):
- Corrupted JPEG files (truncated headers)
- 0-byte files
- Non-image files (TXT, PDF)
- Unsupported formats (BMP, TIFF, HEIC - if not supported)
- **Use Case**: Error handling tests, graceful degradation tests

### 8.2 Test SMB Server Configuration

**Docker Samba Server** (via docker-compose):
```yaml
version: '3.8'
services:
  samba:
    image: dperson/samba
    ports:
      - "445:445"
    volumes:
      - ./test-photos:/photos
    environment:
      - "USER=testuser;testpass"
      - "SHARE=photos;/photos;yes;no;no;testuser"
      - "SMB=min protocol = SMB2"
    command: '-u "testuser;testpass" -s "photos;/photos;yes;no;no;testuser"'
```

**Test Folders**:
- `/photos/small` - 10 photos
- `/photos/medium` - 100 photos
- `/photos/large` - 10,000 photos
- `/photos/invalid` - Invalid files
- `/photos/empty` - Empty folder

### 8.3 Network Simulation

**Test Scenarios**:
- **Normal Connection**: 100 Mbps, 10ms latency
- **Slow Connection**: 5 Mbps, 100ms latency (simulate poor Wi-Fi)
- **Intermittent Connection**: 50% packet loss (simulate unstable network)
- **Disconnect**: Full network disconnect (airplane mode)

**Tools**:
- ADB shell: `adb shell svc wifi disable/enable`
- Network Link Conditioner (iOS) or similar
- Proxy server with configurable latency/bandwidth

---

## 9. Test Environment Setup

### 9.1 Test Devices

**Physical Tablets** (Required for accurate performance testing):
- **Samsung Galaxy Tab A 10.1** (2019) - Mid-range, representative device
  - Android 11 (API 30)
  - 3GB RAM, Exynos 7904
  - 1920x1200 display (10.1")
- **Amazon Fire HD 10** (2021) - Budget device, stress test
  - Fire OS 7 (Android 9, API 28)
  - 3GB RAM, MediaTek MT8183
  - 1920x1200 display (10.1")
- **Samsung Galaxy Tab S8** (2022) - High-end device, visual validation
  - Android 13 (API 33)
  - 8GB RAM, Snapdragon 8 Gen 1
  - 2560x1600 display (11")

**Emulators** (For CI/CD and coverage):
- **API 26** (Android 8.0 Oreo) - Minimum supported version
- **API 30** (Android 11) - Most common version for tablets
- **API 34** (Android 14) - Latest for future-proofing

### 9.2 Screenshot Testing Setup

**Baseline Screenshots**:
- Generated on Samsung Galaxy Tab A 10.1 (1920x1200)
- Stored in `androidTest/assets/screenshots/`
- Pixel-perfect comparison with 2% tolerance (anti-aliasing)

**Screenshot Coverage**:
- Slideshow screen (idle, controls visible, paused)
- Settings screen (empty, filled, error states)
- Error dialogs (connection error, no photos, network disconnect)
- Transition mid-states (fade 50%, slide 50%, zoom mid-animation)

**Tools**:
- Shot (https://github.com/pedrovgs/Shot) or Paparazzi (https://github.com/cashapp/paparazzi)
- Image comparison library: Pixelmatch or Blink-diff

### 9.3 CI/CD Integration

**Test Execution Strategy**:
- **PR Validation**: Smoke tests (Scenario 1, 2, 7, 8, 28) - 5 minutes
- **Nightly Build**: Full UI test suite - 2 hours
- **Weekly Stability**: Long-running tests (Scenario 32, 44) - 8 hours

**Test Infrastructure**:
- Firebase Test Lab (cloud device testing)
- Emulator-based tests in GitHub Actions
- Physical device lab for transition effect validation (manual review)

---

## 10. Coverage Mapping

### 10.1 PRD User Story Coverage

| User Story | UI Test Scenarios | E2E Test Scenarios | Coverage % |
|------------|-------------------|-------------------|-----------|
| US-001: Basic Slideshow | S1, S5, S6, S21-S27 | S28, S31, S32 | 100% |
| US-002: Settings Configuration | S7-S13 | S28-S30, S34, S36 | 100% |
| US-003: User Controls | S2-S4 | S32, S45 | 100% |
| US-004: Photo Management | S43, S44, S46 | S36 | 100% |
| US-005: Transition Effects | S21-S27 | S35 | 100% |
| US-006: Automated Scheduling | S12, S40-S42 | S40-S42 | 100% |
| US-007: Error Handling | S14-S17 | S29, S37-S39 | 100% |
| US-008: Settings Persistence | Covered by QA 1 | S34-S36 | Indirect |
| US-009: Shuffle Mode | S46 | N/A | 80% (logic in QA 1) |
| US-010: Folder Scan | S43, S44 | S36 | 100% |
| US-011: SMB Authentication | S8, S9 | S28-S30 | 100% |
| US-012: 24/7 Operation | S31-S33, S40-S42 | S31-S33, S37-S39 | 100% |

**Overall User Story Coverage**: 100% (UI/UX aspects), 95%+ including indirect coverage

### 10.2 NFR Requirement Coverage

| NFR Requirement | Test Scenarios | Validation Method |
|----------------|----------------|-------------------|
| **Performance** | | |
| 60fps transitions | S21-S27 | Frame rate profiling (Choreographer callbacks) |
| <2s photo load | S6, S43 | Timestamp logging (load start → display) |
| Smooth animations | S21-S27 | Visual inspection + screenshot comparison |
| **Reliability** | | |
| >99.5% crash-free | S32, S39 | 8-hour stability test, crash analytics |
| 95%+ uptime | S32, S37-S39 | Long-running test, auto-recovery validation |
| Network recovery | S37, S38 | Disconnect/reconnect simulation |
| Error handling | S14-S17, S29 | All error scenarios tested |
| **Scalability** | | |
| 10K photos support | S43-S46 | Large collection tests, memory profiling |
| Deep folder scan | S43, S44 | Background scan with UI responsiveness check |
| Photo shuffle | S46 | Algorithm performance test |
| **Usability** | | |
| Kiosk mode | S31-S33 | Auto-start, wake lock, controls hidden |
| Accessibility | S47, S48 | TalkBack testing, font scaling |
| Orientation support | S49, S50 | Landscape/portrait rotation tests |
| **Security** | | |
| Credential encryption | Covered by QA 1 | Unit tests (Keystore integration) |
| SMB 2.0+ enforcement | Covered by QA 1 | Integration tests (protocol validation) |

**Overall NFR Coverage**: 100% (UI/UX-related NFRs), collaboration with QA 1 for backend NFRs

---

## 11. QA 1 Test Plan Analysis & Collaboration Strategy

### 11.1 QA 1 Test Plan Review

**What QA 1 Covered Well** (42 scenarios, 168 test cases):
- **SMB Connection Logic**: Connection validation, credential management, protocol enforcement (SMB 2.0+)
- **Folder Scanning**: Recursive scan, file filtering, progress tracking, 10K photo scalability
- **Slideshow Logic**: Photo sequencing, shuffle algorithm, circular buffer, interval timing
- **Settings Persistence**: DataStore integration, encryption (Keystore), default values
- **Caching**: 5-photo buffer, preloading, memory management, cache eviction
- **Error Detection**: Network failures, SMB errors, invalid photos, timeout handling
- **Security**: Keystore encryption, PII logging audit, SMB 2.0+ enforcement
- **Performance**: Memory leak detection (LeakCanary), large collection handling

**QA 1's Strengths**:
- Comprehensive backend/business logic coverage (85%+ code coverage target)
- Strong focus on NFR validation (security, reliability, scalability)
- Well-defined integration test strategy (Docker Samba, Hilt testing)
- Thorough error handling scenarios (network failures, SMB errors)

### 11.2 Coverage Gaps Identified (QA 1 missed user-facing behaviors)

**Gap 1: Transition Effect Visual Validation**
- **QA 1 Coverage**: None (transitions not tested)
- **Impact**: 60fps NFR not validated, visual correctness unknown
- **QA 2 Fills Gap**: S21-S27 (Fade, Slide, Zoom transitions), frame rate profiling, screenshot comparison

**Gap 2: Error UI Presentation**
- **QA 1 Coverage**: Error detection logic only (no UI testing)
- **Impact**: User cannot see error messages, retry buttons, or recovery actions
- **QA 2 Fills Gap**: S14-S17 (Connection error UI, no photos UI, network disconnect UI)

**Gap 3: User Interaction Responsiveness**
- **QA 1 Coverage**: None (no swipe, tap, or gesture testing)
- **Impact**: User controls (pause, swipe navigation) may not work
- **QA 2 Fills Gap**: S2-S4 (Pause/resume, swipe navigation), S45 (large collection navigation)

**Gap 4: Settings UI Validation**
- **QA 1 Coverage**: Settings persistence logic, validation rules
- **Impact**: UI may not display validation errors, fields may accept invalid input
- **QA 2 Fills Gap**: S7-S13 (Settings screen layout, field validation, save/cancel actions)

**Gap 5: 24/7 Kiosk Mode UX**
- **QA 1 Coverage**: Wake lock logic tested, but no UI behavior validation
- **Impact**: User may see controls in kiosk mode, auto-start may fail
- **QA 2 Fills Gap**: S31-S33 (Auto-start, screen wake lock, controls hidden), S40-S42 (schedule automation)

**Gap 6: First-Time Setup UX**
- **QA 1 Coverage**: None (no first-launch experience testing)
- **Impact**: User cannot complete initial setup, welcome screen may be broken
- **QA 2 Fills Gap**: S20, S28-S30 (First launch, setup flow, connection error handling)

**Gap 7: Visual Regression (Photo Display)**
- **QA 1 Coverage**: None (no screenshot testing)
- **Impact**: Photos may be stretched, cropped incorrectly, or misaligned
- **QA 2 Fills Gap**: S1 (Photo display rendering), screenshot testing for layout correctness

**Gap 8: Loading States & Progress Indicators**
- **QA 1 Coverage**: Loading logic tested, but no UI element validation
- **Impact**: User sees blank screen, no feedback during long operations
- **QA 2 Fills Gap**: S6 (Loading states), S43 (Large collection scan with progress indicator)

### 11.3 Overlap Areas (Coordinate to Avoid Duplication)

**Overlap 1: Settings Validation**
- **QA 1 Tests**: ViewModel validation logic (empty fields, invalid IP, folder path format)
- **QA 2 Tests**: UI validation error messages (text displayed, field highlighting, error position)
- **Coordination**: QA 2 assumes QA 1's validation logic works, focuses on UI presentation only

**Overlap 2: SMB Connection Testing**
- **QA 1 Tests**: Connection success/failure logic, timeout handling, retry mechanism
- **QA 2 Tests**: Connection error UI, retry button functionality, user feedback
- **Coordination**: QA 2 uses mocked connection states (success/error) provided by ViewModel, avoids real SMB testing in UI tests

**Overlap 3: Photo Buffer/Caching**
- **QA 1 Tests**: Buffer logic (5-photo cache), preloading, cache eviction
- **QA 2 Tests**: Photo display from cache, loading spinner when buffer empty
- **Coordination**: QA 2 validates UI reflects buffer state (loading vs. displaying), does not test buffer logic itself

**Overlap 4: Error Handling**
- **QA 1 Tests**: Error detection (network disconnect, SMB error, invalid photo)
- **QA 2 Tests**: Error UI (dialog display, message text, retry button)
- **Coordination**: QA 1 ensures errors detected → QA 2 ensures errors displayed correctly

**Overlap 5: Large Collection Handling**
- **QA 1 Tests**: Folder scan performance (10K photos), memory usage, shuffle algorithm
- **QA 2 Tests**: UI responsiveness during scan, loading progress indicator, no UI freezes
- **Coordination**: QA 1 validates backend performance → QA 2 validates user perceives no lag

### 11.4 Integration Points (QA 2 depends on QA 1)

**Integration 1: ViewModel State for UI Tests**
- **QA 1 Provides**: Well-tested ViewModels (SlideshowViewModel, SettingsViewModel)
- **QA 2 Depends On**: ViewModels emit correct StateFlow values for UI tests
- **Strategy**: QA 2 UI tests use Hilt test injection to provide mocked repositories (not real SMB), focus on UI rendering only

**Integration 2: Error States for UI Testing**
- **QA 1 Provides**: Error detection and StateFlow emission (ConnectionError, NoPhotos, etc.)
- **QA 2 Depends On**: ViewModels emit correct error states
- **Strategy**: QA 2 triggers error states via ViewModel test methods, validates UI response

**Integration 3: Settings Persistence for E2E Tests**
- **QA 1 Provides**: Settings save/load logic (SettingsRepository, DataStore)
- **QA 2 Depends On**: Settings correctly persisted and restored on app restart
- **Strategy**: QA 2 E2E tests assume settings persistence works (validated by QA 1), focus on user flow only

**Integration 4: Photo Buffer for Transition Tests**
- **QA 1 Provides**: Photo buffer preloading, cache management
- **QA 2 Depends On**: Photos available in buffer for smooth transitions
- **Strategy**: QA 2 transition tests assume buffer logic works, focus on animation smoothness only

### 11.5 Collaboration Recommendations

**Recommendation 1: Shared Test Data**
- QA 1 and QA 2 use same Docker Samba server and test photo sets
- Ensures consistency across unit, integration, and UI tests
- **Action**: QA 1 creates Docker setup, QA 2 reuses for E2E tests

**Recommendation 2: Mock ViewModel States for UI Tests**
- QA 2 should NOT test business logic in UI tests (duplicate effort)
- Use Hilt test modules to inject mocked ViewModels with predefined states
- **Action**: QA 2 creates ViewModel test doubles for isolated UI testing

**Recommendation 3: E2E Test Dependencies on QA 1 Tests**
- QA 2 E2E tests should run AFTER QA 1 unit/integration tests pass
- If QA 1 tests fail (e.g., SMB connection broken), QA 2 E2E tests will also fail (expected)
- **Action**: CI/CD pipeline runs QA 1 tests first, blocks QA 2 if failures detected

**Recommendation 4: Combined Test Reporting**
- Merge QA 1 and QA 2 test results into unified coverage report
- Identify gaps where neither QA 1 nor QA 2 tests cover a requirement
- **Action**: Use JaCoCo merged reports, create combined traceability matrix

**Recommendation 5: QA 1 Feedback Loop for UI Issues**
- If QA 2 discovers UI bugs caused by backend logic (e.g., incorrect StateFlow emission), report to QA 1
- QA 1 adds new unit tests to cover the gap
- **Action**: Weekly sync meeting between QA 1 and QA 2 to review cross-layer issues

### 11.6 QA 1 Test Plan Critique (Constructive Feedback)

**Strength 1: Comprehensive Backend Coverage**
- QA 1's 42 scenarios and 168 test cases cover all backend logic thoroughly
- Strong focus on NFR validation (security, reliability, scalability) aligns with PRD priorities

**Strength 2: Well-Defined Integration Test Strategy**
- Docker Samba server for SMB testing is pragmatic and repeatable
- Hilt testing framework ensures proper DI and isolation

**Strength 3: Proactive NFR Focus**
- QA 1 identified critical NFR gaps (Keystore encryption, SMB 2.0+) before implementation
- Memory leak detection (LeakCanary) and performance profiling (10K photos) are essential for 24/7 operation

**Weakness 1: No UI/UX Validation**
- QA 1 does not test transition effects, error dialogs, or user interactions
- Risk: App may have perfect backend logic but broken UI (e.g., error messages not displayed)
- **QA 2 Mitigation**: S1-S27, S14-S17 provide full UI coverage

**Weakness 2: No E2E User Flow Testing**
- QA 1 tests components in isolation, but not complete user journeys
- Risk: Individual components work, but integration fails (e.g., settings save → slideshow restart broken)
- **QA 2 Mitigation**: S28-S42 cover all critical E2E flows

**Weakness 3: Limited Visual Regression Testing**
- QA 1 does not use screenshot testing or visual validation
- Risk: Layout bugs, photo display issues, or transition glitches not detected
- **QA 2 Mitigation**: Screenshot testing for transitions, error states, and layout correctness

**Weakness 4: No First-Time Setup Testing**
- QA 1 assumes settings exist, does not test first-launch experience
- Risk: Users cannot complete initial setup, app unusable out-of-box
- **QA 2 Mitigation**: S20, S28-S30 validate first-time setup flow

**Overall Assessment of QA 1 Plan**:
- **Grade**: A- (Excellent backend coverage, missing UI/UX layer)
- **Recommendation**: QA 1 and QA 2 plans are complementary, not redundant. Both required for full coverage.

---

## 12. Test Case Priority & Execution Order

### 12.1 P0 Test Cases (Blocking - Must Pass Before Release)

**Sprint 1 (Smoke Tests - Week 1)**:
1. S1: Photo Display Rendering (8 cases)
2. S7: Settings Screen Layout (8 cases)
3. S8: SMB Connection Fields (10 cases)
4. S28: Complete First-Time Setup (10 cases)
5. S31: Auto-Start Slideshow (6 cases)

**Sprint 2 (Core Functionality - Week 2)**:
1. S2: Slideshow Controls Visibility (6 cases)
2. S3: Pause/Resume Functionality (5 cases)
3. S9: SMB Field Validation (9 cases)
4. S14: Connection Error UI (8 cases)
5. S21: Fade Transition Visual Correctness (6 cases)
6. S22: Fade Transition Performance (4 cases)

**Sprint 3 (Reliability - Week 3)**:
1. S32: Continuous Slideshow Operation (8 cases)
2. S37: Network Disconnect and Reconnect (10 cases)
3. S38: SMB Server Restart Recovery (8 cases)
4. S39: App Crash and Auto-Recovery (6 cases)
5. S43: Large Collection Scan (8 cases)
6. S44: UI Responsiveness During Background Scan (6 cases)

### 12.2 P1 Test Cases (Important - Should Pass Before Release)

**Sprint 4 (Enhanced Functionality - Week 4)**:
1. S4: Manual Navigation (Swipe) (7 cases)
2. S10: Slideshow Interval Settings (6 cases)
3. S11: Transition Effect Selection (7 cases)
4. S16: Photo Load Error UI (6 cases)
5. S23: Slide Transition Visual Correctness (7 cases)
6. S25: Zoom Transition Visual Correctness (6 cases)
7. S40: Schedule Activation (8 cases)
8. S45: Swipe Navigation with Large Collection (5 cases)

### 12.3 P2 Test Cases (Nice to Have - Can Defer if Time Constrained)

**Sprint 5 (Polish - Week 5)**:
1. S5: Photo Index Display (4 cases)
2. S12: Schedule Settings (8 cases)
3. S20: First Launch Experience (6 cases)
4. S27: Random Transition Behavior (4 cases)
5. S47: TalkBack Support (6 cases)
6. S48: Font Scaling (4 cases)
7. S49: Orientation Change Handling (6 cases)

---

## 13. Test Execution Schedule & Effort Estimate

### 13.1 Test Development Effort

| Test Category | Scenarios | Test Cases | Dev Effort (hours) |
|--------------|-----------|------------|-------------------|
| UI Component Tests | 20 | 94 | 50-60 |
| Transition Effect Tests | 7 | 27 | 20-25 |
| E2E Flow Tests | 15 | 57 | 40-50 |
| Accessibility Tests | 4 | 14 | 10-15 |
| **Total** | **46** | **192** | **120-150** |

**Test Development Timeline**: 4-5 weeks (1 QA engineer full-time)

### 13.2 Test Execution Effort

| Test Run Type | Duration | Frequency |
|--------------|----------|-----------|
| Smoke Tests (P0, 5 scenarios) | 10 minutes | Every PR |
| Full UI Suite (46 scenarios) | 2 hours | Nightly |
| Long-Running Tests (S32, S44) | 8 hours | Weekly |
| Visual Regression (Screenshot) | 30 minutes | Every PR |

**Total Test Execution Time per Week**: 2 hours (nightly) × 5 + 8 hours (weekly) = 18 hours

### 13.3 Test Maintenance Effort

**Ongoing Maintenance**: 5-10 hours per week
- Update test data (new photo sets)
- Fix flaky tests (timing issues, network instability)
- Update baselines for screenshot tests (intentional UI changes)
- Add new test cases for bug fixes

---

## 14. Risks & Mitigation Strategies

### 14.1 Test Execution Risks

**Risk 1: Flaky E2E Tests (Network Timing)**
- **Impact**: CI/CD pipeline unstable, false failures
- **Mitigation**:
  - Use IdlingResource for async operations (photo loading, SMB connection)
  - Retry flaky tests 3x before marking as failure
  - Network simulation via ADB (predictable behavior)

**Risk 2: Screenshot Test Failures (Anti-Aliasing Differences)**
- **Impact**: False positives on different devices/emulators
- **Mitigation**:
  - Use 2% pixel tolerance for image comparison
  - Generate baselines on specific device model (Samsung Galaxy Tab A)
  - Manual review for suspected false positives

**Risk 3: Long-Running Tests Timeout (S32: 1 hour test)**
- **Impact**: CI/CD pipeline too slow, test skipped
- **Mitigation**:
  - Run long-running tests separately (weekly, not nightly)
  - Use time-accelerated tests where possible (e.g., schedule tests)
  - Monitor test execution time, optimize slow tests

**Risk 4: SMB Server Unavailable (Docker Samba Crash)**
- **Impact**: All E2E tests fail, no coverage
- **Mitigation**:
  - Health check for Docker Samba before test run
  - Fallback to mocked SMB responses for UI-only tests
  - Retry mechanism if server temporarily unavailable

### 14.2 Coverage Risks

**Risk 5: Transition Effect Performance Not Validated on Physical Devices**
- **Impact**: 60fps NFR not met, jank discovered in production
- **Mitigation**:
  - Mandatory physical device testing for transition effect scenarios (S21-S27)
  - Frame rate profiling with Choreographer callbacks
  - Manual visual inspection by QA lead

**Risk 6: 24/7 Kiosk Mode Not Testable in CI/CD**
- **Impact**: Stability issues discovered post-deployment
- **Mitigation**:
  - Use physical test tablet running 24/7 in lab
  - Monitor with Firebase Crashlytics + custom logging
  - Weekly manual inspection of test tablet

**Risk 7: Accessibility Testing Limited (No Real TalkBack Users)**
- **Impact**: Usability issues for visually impaired users
- **Mitigation**:
  - Automated semantics testing (Compose Testing)
  - Manual TalkBack testing by QA engineer
  - User acceptance testing with accessibility advocate (if available)

---

## 15. Acceptance Criteria

### 15.1 Test Coverage Criteria

- **User Story Coverage**: 100% of UI/UX aspects of all 12 user stories covered
- **NFR Coverage**: 100% of UI-related NFRs (60fps transitions, error states, 24/7 operation) covered
- **Screen Coverage**: 100% of user-visible screens tested (Slideshow, Settings, Dialogs)
- **Error State Coverage**: 95%+ of error scenarios have corresponding UI tests
- **E2E Flow Coverage**: 100% of critical user flows (setup, daily usage, error recovery) tested

### 15.2 Test Execution Criteria

- **P0 Test Pass Rate**: 100% (blocking for release)
- **P1 Test Pass Rate**: 95%+ (should fix before release)
- **P2 Test Pass Rate**: 80%+ (nice to have)
- **Flaky Test Rate**: <5% (max 2-3 flaky tests in full suite)
- **Screenshot Test Pass Rate**: 95%+ (manual review for false positives)

### 15.3 Performance Criteria (Validated via Tests)

- **Transition Effects**: 60fps (16.67ms per frame) validated on 3+ physical devices
- **Photo Load Time**: <2 seconds (95th percentile) for 1920x1080 photos
- **UI Responsiveness**: Settings screen renders in <500ms
- **Large Collection UX**: First photo displays within 2 seconds, even with 10K photos

### 15.4 Quality Gates for Release

**Phase 1 (MVP Release - Q2 2026)**:
1. All P0 test cases pass (100%)
2. P1 test cases pass (95%+)
3. No P0 bugs open (UI blocking issues)
4. <5 P1 bugs open (minor UI issues, planned fixes in Phase 2)
5. Transition effect performance validated on 3+ physical devices
6. 24/7 stability test (8 hours) passes without crashes

**Phase 2 (Future Enhancements - Q3 2026)**:
1. All P2 test cases pass (100%)
2. Accessibility tests pass (TalkBack, font scaling)
3. Advanced features tested (cloud sync, photo filters, custom transitions)

---

## 16. Debate Summary: QA 1 Collaboration & Coverage Analysis

### 16.1 QA 1's Test Plan: What Worked Well

**Comprehensive Backend Coverage**:
- QA 1's 42 scenarios and 168 test cases provide excellent coverage of business logic, data layer, and external integrations
- Strong focus on NFR validation (security, reliability, scalability) directly addresses PRD P0 requirements
- Well-defined integration test strategy (Docker Samba, Hilt testing) is pragmatic and repeatable

**Proactive Risk Identification**:
- QA 1 identified critical security gaps (Keystore encryption, SMB 2.0+) and reliability concerns (network recovery, memory leaks) before implementation
- Memory leak detection (LeakCanary) and performance profiling (10K photos) are essential for 24/7 operation
- Thorough error handling scenarios (network failures, SMB errors, invalid photos) align with NFR reliability requirements

**Clear Test Strategy**:
- QA 1 defined scope clearly (unit/integration, not UI/E2E)
- Test pyramid followed (55% unit, 30% integration, 15% E2E - though E2E deferred to QA 2)
- Coverage mapping to PRD user stories ensures traceability

### 16.2 QA 1's Test Plan: Coverage Gaps from UI/UX Perspective

**Gap 1: No User-Facing Behavior Validation**
- QA 1 tests that errors are detected (backend logic), but not that error messages are displayed to users (UI)
- QA 1 tests that settings are persisted (data layer), but not that validation errors are shown in the settings form (UI)
- QA 1 tests that transitions are triggered (ViewModel logic), but not that they render smoothly at 60fps (visual validation)

**Gap 2: No End-to-End User Flow Testing**
- QA 1 tests components in isolation (unit tests), but not how they integrate in real user journeys
- Example: QA 1 tests settings save logic + slideshow start logic separately, but not the complete flow: "Save settings → Slideshow restarts with new settings"
- Risk: Individual components work, but integration fails (regression bugs not caught)

**Gap 3: No Visual Regression Testing**
- QA 1 does not use screenshot testing or visual validation
- Risk: Layout bugs (photo stretched, cropped incorrectly), transition glitches (frame drops, jank), error dialog misalignment not detected
- Example: Fade transition may pass logic tests but visually show a "pop" or flicker

**Gap 4: No First-Time Setup Experience Testing**
- QA 1 assumes settings exist, does not test first-launch experience (welcome screen, mandatory setup flow)
- Risk: Users cannot complete initial setup, app unusable out-of-box
- This is a critical UX flow for a product marketed as "set it and forget it"

**Gap 5: No 24/7 Kiosk Mode UX Validation**
- QA 1 tests wake lock logic (screen stays on), but not kiosk mode UX (controls hidden, auto-start, no user interaction required)
- Risk: Slideshow may require user interaction to start (violates "zero-touch operation" NFR)

### 16.3 QA 2's Test Plan: How We Fill the Gaps

**UI Component Testing (30% of Test Pyramid)**:
- **S1-S6**: Slideshow screen rendering, controls, pause/resume, loading states
- **S7-S13**: Settings screen layout, field validation, save/cancel actions
- **S14-S17**: Error dialogs, connection errors, no photos, network disconnect
- **Fills QA 1 Gap 1**: Validates all user-facing UI elements and error messages

**Visual Regression Testing (Screenshot Testing)**:
- **S21-S27**: Transition effects (Fade, Slide, Zoom) with pixel-perfect comparison
- **S1**: Photo display rendering (aspect ratio, centering, color profile)
- **Fills QA 1 Gap 3**: Detects visual bugs, layout issues, and transition glitches

**End-to-End Flow Testing (15% of Test Pyramid)**:
- **S28-S30**: First-time setup flow (connection, validation, save, slideshow start)
- **S31-S33**: Daily usage flow (auto-start, 24/7 operation, wake lock)
- **S34-S36**: Settings change flow (interval, transition, folder path updates)
- **S37-S39**: Error recovery flow (network disconnect, SMB restart, app crash)
- **S40-S42**: Schedule automation flow (enable, activate, deactivate)
- **Fills QA 1 Gaps 2, 4, 5**: Validates complete user journeys and kiosk mode UX

**Performance & Reliability Testing (NFR Validation)**:
- **S22, S24, S26**: Transition effect performance (60fps frame rate profiling)
- **S32**: Continuous slideshow operation (1 hour stability test)
- **S43-S44**: Large collection UX (10K photos, UI responsiveness)
- **Fills QA 1 Gap 3 (performance)**: Validates 60fps NFR with real visual rendering, not just logic tests

### 16.4 Overlap Areas: Coordination to Avoid Duplication

**Settings Validation**:
- **QA 1 Tests**: ViewModel validation logic (empty fields, invalid IP, folder path format)
- **QA 2 Tests**: UI validation error messages (text displayed, field highlighting)
- **Coordination**: QA 2 uses mocked ViewModel states, does not re-test validation logic

**SMB Connection**:
- **QA 1 Tests**: Connection success/failure logic, timeout, retry mechanism
- **QA 2 Tests**: Connection error UI, retry button, user feedback
- **Coordination**: QA 2 uses mocked connection states (success/error), avoids real SMB testing in UI tests

**Photo Buffer/Caching**:
- **QA 1 Tests**: Buffer logic (5-photo cache), preloading, cache eviction
- **QA 2 Tests**: Photo display from cache, loading spinner when buffer empty
- **Coordination**: QA 2 validates UI reflects buffer state, does not test buffer logic

**Error Handling**:
- **QA 1 Tests**: Error detection (network disconnect, SMB error, invalid photo)
- **QA 2 Tests**: Error UI (dialog display, message text, retry button)
- **Coordination**: QA 1 ensures errors detected → QA 2 ensures errors displayed correctly

### 16.5 Integration Points: QA 2 Depends on QA 1

**ViewModel State for UI Tests**:
- QA 2 UI tests assume ViewModels emit correct StateFlow values (validated by QA 1)
- QA 2 uses Hilt test injection to provide mocked repositories, focus on UI rendering only

**Error States for UI Testing**:
- QA 2 triggers error states via ViewModel test methods (assuming error detection works per QA 1)
- QA 2 validates UI response (dialog display, message text, retry button)

**Settings Persistence for E2E Tests**:
- QA 2 E2E tests assume settings correctly persisted and restored on app restart (validated by QA 1)
- QA 2 focuses on user flow only (save → restart → settings applied)

**Photo Buffer for Transition Tests**:
- QA 2 transition tests assume buffer logic works (validated by QA 1)
- QA 2 focuses on animation smoothness and visual correctness only

### 16.6 Final Verdict: QA 1 + QA 2 = Complete Coverage

**QA 1 Test Plan Grade**: A- (Excellent backend coverage, missing UI/UX layer)
**QA 2 Test Plan Grade**: A (Comprehensive UI/E2E coverage, complements QA 1)

**Combined Coverage**:
- **Backend Logic**: 85%+ code coverage (QA 1)
- **UI Components**: 100% screen coverage (QA 2)
- **E2E Flows**: 100% critical user journeys (QA 2)
- **Visual Regression**: 95%+ screenshot test coverage (QA 2)
- **NFR Validation**: 100% (QA 1 backend NFRs + QA 2 UI/visual NFRs)

**Recommendation**: Both test plans required for MVP release. QA 1 and QA 2 are complementary, not redundant.

**Test Execution Order**:
1. QA 1 unit/integration tests run first (validate backend logic)
2. If QA 1 tests pass, run QA 2 UI/E2E tests (validate user-facing behavior)
3. If either fails, block release (critical gap)

**Collaboration Success Metrics**:
- Zero duplicate test cases (QA 1 and QA 2 coordinate on overlap areas)
- Combined test report shows >90% total coverage (unit + integration + UI + E2E)
- All P0 bugs caught before production (no user-facing issues escape testing)

---

## 17. Tools & Dependencies

### 17.1 Testing Frameworks

- **Jetpack Compose Testing**: UI component testing, semantics validation
- **Espresso**: In-app navigation, user interactions
- **UI Automator**: System-level interactions (notifications, screen wake, volume buttons)
- **JUnit 5**: Test runner, assertions
- **Hilt Testing**: Dependency injection for test doubles
- **MockK**: Mocking library (for ViewModels, repositories)
- **Turbine**: Flow testing (StateFlow assertions)

### 17.2 Visual Regression Tools

- **Shot** (https://github.com/pedrovgs/Shot) or **Paparazzi** (https://github.com/cashapp/paparazzi)
- **Pixelmatch** or **Blink-diff**: Image comparison
- **Firebase Test Lab**: Cloud device testing with screenshot capture

### 17.3 Performance Profiling Tools

- **Choreographer**: Frame rate monitoring (detect dropped frames)
- **Android Profiler**: CPU, memory, GPU usage
- **Systrace**: System-level performance tracing
- **LeakCanary**: Memory leak detection (integrated by QA 1)

### 17.4 Test Infrastructure

- **Docker Samba**: Test SMB server (shared with QA 1)
- **Android Test Orchestrator**: Isolated test execution (prevent flaky tests)
- **Firebase Test Lab**: Cloud device testing
- **GitHub Actions**: CI/CD pipeline for automated test execution

---

## 18. Appendix: Test Case Details (Sample)

### Sample Test Case 1: Photo Display Rendering

**Scenario**: S1 - Photo Display Rendering
**Test Case**: TC-S1-01 - Photo displays centered and properly scaled

**Preconditions**:
- App installed and settings configured
- SMB server accessible with test photo set (10 photos)
- Slideshow started

**Test Steps**:
1. Wait for first photo to load
2. Capture screenshot
3. Measure photo position and size
4. Verify photo centered horizontally and vertically
5. Verify photo scaled to fit screen (letterbox or pillarbox)
6. Verify no stretching (aspect ratio preserved)

**Expected Result**:
- Photo centered with equal margins on letterbox/pillarbox sides
- Photo aspect ratio matches source image
- Screenshot comparison passes (within 2% tolerance)

**Priority**: P0
**Automation**: Automated (Compose Testing + Screenshot Test)
**Estimated Duration**: 10 seconds

---

### Sample Test Case 2: Connection Error UI

**Scenario**: S14 - Connection Error UI
**Test Case**: TC-S14-01 - Connection error dialog displays when SMB connection fails

**Preconditions**:
- App installed
- SMB server configured but inaccessible (Docker Samba stopped)

**Test Steps**:
1. Launch app
2. Wait for connection attempt
3. Verify error dialog displays
4. Verify error message text: "Unable to connect to server: [server address]"
5. Verify "Retry" button displayed
6. Verify "Open Settings" button displayed
7. Capture screenshot

**Expected Result**:
- Error dialog displayed within 5 seconds
- Error message includes server address and error reason
- Retry and Open Settings buttons visible and enabled
- Screenshot comparison passes (error dialog layout correct)

**Priority**: P0
**Automation**: Automated (Espresso)
**Estimated Duration**: 15 seconds

---

### Sample Test Case 3: Fade Transition Performance

**Scenario**: S22 - Fade Transition Performance
**Test Case**: TC-S22-01 - Fade transition with 4K photo maintains 60fps

**Preconditions**:
- App installed and settings configured
- SMB server accessible with 4K photo set (10 photos)
- Transition effect set to "Fade"
- Slideshow started on physical device (Samsung Galaxy Tab A)

**Test Steps**:
1. Register Choreographer frame callback
2. Wait for next photo transition
3. Monitor frame rate during fade transition
4. Calculate average frame time (milliseconds per frame)
5. Calculate frame drop count (frames >16.67ms)

**Expected Result**:
- Average frame time: ≤16.67ms (60fps)
- Frame drop count: 0 (no dropped frames)
- Transition completes within 500ms ± 50ms

**Priority**: P0
**Automation**: Automated (Choreographer + custom performance test)
**Estimated Duration**: 30 seconds (includes photo load time)

**Notes**:
- Must run on physical device (emulators do not accurately measure performance)
- Requires custom Choreographer instrumentation in test code

---

## 19. Sign-Off & Approvals

### Test Plan Review

**Prepared By**: QA 2 - UI & E2E Tests focused
**Date**: 2026-03-02
**Phase**: Phase 6 - Test Planning

**Reviewers**:
- [ ] QA 1 - Unit & Integration Tests (review for overlap/integration points)
- [ ] QA 3 - Performance & Accessibility Tests (review for performance testing strategy)
- [ ] QA Lead (approve test scope and effort estimate)
- [ ] Dev Lead (approve integration with QA 1 plan, test infrastructure requirements)

**Approval Criteria**:
1. Test plan covers 100% of UI/UX aspects of PRD user stories
2. Test plan addresses all NFR UI/visual requirements (60fps transitions, error states, 24/7 kiosk mode)
3. Test plan complements (not duplicates) QA 1's unit/integration test plan
4. Test plan defines clear integration points with QA 1
5. Test effort estimate (120-150 hours) aligns with project timeline (4-5 weeks)
6. Test infrastructure requirements (Docker Samba, physical devices, screenshot testing) are feasible

**Next Steps**:
1. **Phase 7**: QA 3 creates Performance & Accessibility test plan (review QA 1 + QA 2 plans first)
2. **Phase 8**: Development team implements features (parallel with test development)
3. **Phase 9**: QA 2 implements UI & E2E tests (4-5 weeks)
4. **Phase 10**: Execute test suite, report results, iterate until acceptance criteria met

---

**END OF UI & E2E TEST PLAN**
