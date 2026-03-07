# Requirements Refinement Q&A

## Round 1: Core Functionality Clarifications

### Q1: How should the slideshow handle photo playback and transitions?
**Asked**: 2026-03-01
**Answer**: Random order with configurable timer (Recommended)
**Context**: Need to understand basic slideshow behavior - random vs sequential, timing control
**Implication**: Implement random shuffle algorithm with user-configurable interval (e.g., 10s, 30s, 1min)

### Q2: How should the app handle SMB/Samba network share authentication and discovery?
**Asked**: 2026-03-01
**Answer**: Support multiple methods
**Context**: Critical for primary feature - need to support both power users and casual users
**Implication**: Implement both manual configuration (SMB path + credentials) AND network discovery/scanning. More complex but provides best UX.

### Q3: What should happen when the network share or photos are unavailable?
**Asked**: 2026-03-01
**Answer**: Require network connection with read-ahead to ensure smooth transitions
**Context**: Error handling strategy - offline vs cached vs strict network requirement
**Implication**: Implement read-ahead buffering system (preload next 2-3 photos). Don't show slideshow if network unavailable. Focus on smooth transitions rather than offline capability.

### Q4: How should background music work in the slideshow?
**Asked**: 2026-03-01
**Answer**: No background music
**Context**: Scope clarification - music adds complexity with audio APIs and file management
**Implication**: Remove background music from MVP scope. Simplifies development significantly.

## Round 2: UI/UX and Configuration

### Q5: How should the weather and time overlays work?
**Asked**: 2026-03-01
**Answer**: No overlays
**Context**: Scope clarification - overlays add API dependencies and UI complexity
**Implication**: Remove weather/time overlays from MVP scope. Keep slideshow clean and simple.

### Q6: How should scheduled playback work?
**Asked**: 2026-03-01
**Answer**: Time-based on/off with screen control (Recommended)
**Context**: Automation feature - how to schedule slideshow without manual intervention
**Implication**: Implement daily schedule system (e.g., on 7am-11pm, off 11pm-7am). App must control screen wake/sleep using Android power management APIs. Requires WAKE_LOCK permission.

### Q7: How should photo selection and filtering work from SMB shares?
**Asked**: 2026-03-01
**Answer**: Recursive folder scan with file type filter (Recommended)
**Context**: Photo discovery mechanism - complexity vs usability tradeoff
**Implication**: Implement recursive directory traversal on SMB share. Support JPEG, PNG, HEIC formats. Simple, automatic approach - no complex filtering UI needed.

### Q8: What kind of settings/configuration UI do you envision?
**Asked**: 2026-03-01
**Answer**: Simple settings screen (Recommended)
**Context**: Configuration UX approach
**Implication**: Single settings screen with all options: SMB configuration, timing settings, transition effects, schedule settings. No wizard, no multiple profiles. Clean and straightforward.

## Round 3: Technical Details and Edge Cases

### Q9: How should the app handle photos with different aspect ratios (portrait vs landscape)?
**Asked**: 2026-03-01
**Answer**: Scale to fit with black bars (Recommended)
**Context**: Display strategy for mixed orientation photos on landscape tablet
**Implication**: Letterbox/pillarbox approach. Show full photo with black bars as needed. No cropping - preserve entire image. Use Compose's ContentScale.Fit.

### Q10: What transition effects are most important for the slideshow?
**Asked**: 2026-03-01
**Answer**: Fade, Slide (pan), Zoom/Ken Burns (multiple selected)
**Context**: Visual polish - which transitions to implement
**Implication**: Implement three transition types:
1. Fade: Simple cross-fade (default)
2. Slide: Pan from left/right/top/bottom
3. Zoom/Ken Burns: Slow zoom and pan effect (most complex)
User can select preferred transition in settings.

### Q11: How should the app behave when started or brought to foreground?
**Asked**: 2026-03-01
**Answer**: Auto-start slideshow (Recommended)
**Context**: App launch behavior and user interaction model
**Implication**: When app opens, immediately start slideshow (if configured). Settings accessible via gesture/button (e.g., swipe from edge, or tap-hold). Optimized for "set it and forget it" kiosk mode.

### Q12: For cloud services (secondary priority), which should we implement first in a future phase?
**Asked**: 2026-03-01
**Answer**: Google Photos (Recommended)
**Context**: Future roadmap planning for cloud integration
**Implication**: Phase 2 feature. Google Photos has best API and OAuth flow. Defer Dropbox and OneDrive to later phases.

## Identified Corner Cases

### 1. Network Connectivity Issues
**Corner Case**: SMB share becomes unavailable mid-slideshow (network drop, server down, WiFi disconnect)
**Resolution**: Based on Q3 - show error message and pause slideshow. Use read-ahead buffer to continue for 2-3 photos, then gracefully error. Don't fall back to cache - require network.

### 2. Empty Photo Directory
**Corner Case**: SMB share exists but contains no compatible photos
**Resolution**: Show clear error message in UI: "No photos found in configured location. Please check your SMB path and ensure JPEG/PNG/HEIC files exist."

### 3. Slow Network Performance
**Corner Case**: SMB share responds slowly, causing delays between photo transitions
**Resolution**: Based on Q3 - implement aggressive read-ahead buffering. Preload next 2-3 photos in background. If buffer exhausted, pause slideshow until next photo loaded (show loading indicator).

### 4. Mixed Portrait/Landscape Photos
**Corner Case**: Photo collection has mixed orientations
**Resolution**: Based on Q9 - scale to fit with black bars. Preserves full image, no cropping.

### 5. Very Large Photos (4K+, RAW files)
**Corner Case**: High-resolution photos may cause memory issues or slow loading
**Resolution**: Implement image downsampling/resizing on load. Load at screen resolution (tablet max ~2560x1600). Reject RAW files in scan (only JPEG/PNG/HEIC).

### 6. SMB Authentication Failure
**Corner Case**: Invalid credentials, permission denied, or authentication timeout
**Resolution**: Show clear error in settings UI with retry option. Don't start slideshow until SMB connection verified. Test connection on settings save.

### 7. Schedule Conflicts with System Sleep
**Corner Case**: Android system sleep/power saving may interfere with scheduled wake
**Resolution**: Use AlarmManager with setExactAndAllowWhileIdle() for schedule. Require WAKE_LOCK and SCHEDULE_EXACT_ALARM permissions. Document that aggressive battery optimization must be disabled for this app.

### 8. Screen Rotation / Configuration Changes
**Corner Case**: Tablet orientation changes (though typically fixed in landscape for photo frame)
**Resolution**: Lock orientation to landscape in manifest. Photo frames are typically landscape tablets in fixed position.

### 9. App Backgrounded During Slideshow
**Corner Case**: User switches away from app or home button pressed
**Resolution**: Based on Q11 - when app returns to foreground, auto-resume slideshow. Maintain current position in photo queue.

### 10. Invalid/Corrupted Photo Files
**Corner Case**: Photo file exists but can't be decoded (corrupted, unsupported format variant)
**Resolution**: Catch image load exceptions, log error, skip to next photo. Don't crash or stop slideshow. Show brief toast: "Skipped 1 corrupted photo."

## Clarified Requirements

### Core Features (MVP)
1. ✅ SMB/Samba network share support with authentication
2. ✅ Network discovery AND manual configuration for SMB
3. ✅ Random slideshow with configurable timer
4. ✅ Read-ahead buffering for smooth transitions (2-3 photos)
5. ✅ Three transition effects: Fade, Slide, Zoom/Ken Burns
6. ✅ Scheduled on/off with screen wake/sleep control
7. ✅ Recursive photo scanning (JPEG, PNG, HEIC)
8. ✅ Scale-to-fit display with letterboxing
9. ✅ Simple settings UI
10. ✅ Auto-start slideshow on launch

### Removed from MVP
1. ❌ Background music playback
2. ❌ Weather overlays
3. ❌ Time overlays
4. ❌ Cloud services (Google Photos, Dropbox, OneDrive) - deferred to Phase 2

### Technical Requirements
1. **Tech Stack**: Jetpack Compose, Kotlin Coroutines & Flow, Hilt/Dagger, Room
2. **Permissions**:
   - INTERNET
   - ACCESS_NETWORK_STATE
   - WAKE_LOCK
   - SCHEDULE_EXACT_ALARM
   - FOREGROUND_SERVICE (for background scheduling)
3. **Android Version**: Target Android 13+ (API 33+), minimum Android 8.0 (API 26)
4. **Orientation**: Landscape only (locked)
5. **Target Device**: Android tablets (7" and larger)

### Non-Functional Requirements
1. **Performance**: Smooth 60fps transitions, <2s photo load time
2. **Memory**: Efficient image loading (downsample to screen resolution)
3. **Battery**: Screen-on during active hours, controlled sleep during off hours
4. **Network**: Resilient to temporary network issues, clear error messaging
5. **Security**: Secure credential storage (Android Keystore), no plaintext passwords

## Assumptions Validated

### ✅ Assumption 1: Primary use case is kiosk/dedicated device mode
**Validation**: Confirmed by Q11 - auto-start slideshow, minimal interaction
**Impact**: Design for "set it and forget it" - optimize for hands-off operation

### ✅ Assumption 2: Local network (SMB) is more important than cloud
**Validation**: Confirmed by initial priority discussion - local storage first
**Impact**: Invest in robust SMB implementation before cloud features

### ✅ Assumption 3: Simplicity preferred over feature richness for MVP
**Validation**: Confirmed by Q4, Q5 (removed music and overlays)
**Impact**: Focus on core slideshow quality rather than many features

### ✅ Assumption 4: Network reliability assumed (not offline-first)
**Validation**: Confirmed by Q3 - require network with read-ahead
**Impact**: Don't build complex offline cache system, focus on buffering

### ✅ Assumption 5: Tablet will be in fixed landscape orientation
**Validation**: Inferred from digital photo frame use case
**Impact**: Lock orientation, optimize UI for landscape only

### ❌ Assumption 6: Weather/time overlays are essential for photo frames
**Validation**: Rejected by Q5 - user doesn't want overlays
**Impact**: Removed from scope - keep display clean

### ✅ Assumption 7: Users want variety in transitions, not just fade
**Validation**: Confirmed by Q10 - multiple transitions selected
**Impact**: Implement three distinct transition types with user selection

## Summary

**Total Questions**: 12 across 3 rounds
**Features Removed**: 3 (background music, weather overlay, time overlay)
**Corner Cases Identified**: 10
**Technical Clarifications**: 7
**Assumptions Validated**: 7 confirmed, 1 rejected

The requirements are now well-defined with clear scope boundaries. Ready to proceed to Phase 2 (Requirements Enrichment).
