# Requirements Q&A: Local Photo Source

**Phase 1: Requirements Refinement**
**Date**: 2026-03-06

## Questions for User

### 1. Local Storage Access

**Q1.1**: How should users select local folders/photos?
- Option A: Android Media Picker (modern, scoped storage)
- Option B: File browser to select specific folders
- Option C: Access to Media Store collections (DCIM, Pictures, Downloads)
- Option D: All of the above

**Q1.2**: Should the app:
- Request READ_MEDIA_IMAGES permission (Android 13+)?
- Request READ_EXTERNAL_STORAGE permission (Android 12 and below)?
- Use scoped storage only (no permissions needed)?

### 2. Photo Aggregation Strategy

**Q2.1**: How should photos from multiple sources be combined in the slideshow?
- Option A: Interleaved (alternating between sources)
- Option B: Sequential (all SMB, then all local)
- Option C: Random mix from all sources
- Option D: User-configurable per source (e.g., 70% SMB, 30% local)

**Q2.2**: Should photo order within each source be:
- Chronological (by date taken)?
- Alphabetical (by filename)?
- Random?
- User preference?

### 3. Settings UI

**Q3.1**: Settings screen layout for multiple sources:
- Option A: List of sources with enable/disable toggles
- Option B: Separate screens for each source type
- Option C: Single screen with expandable sections per source

**Q3.2**: Should users be able to:
- Add multiple local folders (e.g., DCIM + Screenshots)?
- Configure priority/weight per source?
- Preview photos from each source before enabling?

### 4. Performance & Caching

**Q4.1**: For local photo scanning:
- Scan on-demand when slideshow starts?
- Background scan and cache results?
- Use WorkManager for periodic updates?

**Q4.2**: Should the app:
- Cache local photo metadata (paths, dates)?
- Watch for new photos (MediaStore observer)?
- Rescan periodically or manually only?

### 5. User Experience

**Q5.1**: First-time setup flow:
- Guide users to configure both SMB and local sources?
- Allow skipping local source setup?
- Show sample/demo photos from local storage?

**Q5.2**: Error handling:
- What if local folder is empty?
- What if permissions are denied?
- What if no photos found in any source?

### 6. Existing Features

**Q6.1**: Should the new feature work with:
- Shuffle mode (shuffle within source or across all sources)?
- Auto-advance timing (same for all sources)?
- Photo transitions (same animation for all sources)?

**Q6.2**: Should the existing SMB settings:
- Remain unchanged (backwards compatible)?
- Be migrated to new multi-source architecture?
- Support disabling SMB and using local only?

## Answers

### 1. Local Storage Access
**A1.1**: Media Picker + File browser (both options available to user)
**A1.2**: Modern scoped storage (no broad storage permissions needed)

### 2. Photo Aggregation Strategy
**A2.1**: Random mix from all sources
**A2.2**: Random (consistent with overall strategy)

### 3. Settings UI
**A3.1**: List of sources with add/remove capability
**A3.2**: Multiple local folders support (DCIM, Screenshots, Downloads, custom folders)

### 4. Performance & Caching
**A4.1**: Background caching with WorkManager
**A4.2**: Automatic monitoring for new photos (MediaStore observer)

### 5. User Experience
**A5.1**: Allow skipping setup (users can configure later)
**A5.2**: Handle gracefully - show message, allow retry or skip

### 6. Existing Features
**A6.1**: Shuffle across all photos from all sources
**A6.2**: Fully configurable - users can add/remove any combination (SMB only, local only, both, or multiple of each type)

## Requirements Summary

### Functional Requirements
1. Support Media Picker for photo/folder selection
2. Support file browser for folder selection
3. Allow multiple local folders (unlimited)
4. Random mix of photos from all enabled sources
5. Background caching with WorkManager
6. Automatic monitoring for new photos
7. Add/remove sources dynamically
8. Support any combination: SMB only, local only, both, or multiple instances
9. Allow skipping initial setup

### Technical Requirements
1. Use modern scoped storage APIs (Android 10+)
2. No READ_EXTERNAL_STORAGE permission needed
3. MediaStore ContentObserver for new photo detection
4. WorkManager for background scanning
5. Maintain backwards compatibility with existing SMB functionality

### Non-Functional Requirements
1. Performance: Background caching to avoid UI blocking
2. UX: Graceful error handling for empty/unavailable sources
3. Privacy: Scoped storage respects user privacy
4. Maintainability: Clean separation between source types

## Next Steps

1. ✅ Requirements refinement complete
2. → Proceed to Phase 2: Architecture Design
