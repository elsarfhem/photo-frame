# Architecture Review Summary: Local Photo Source

**Status**: Ready for Review
**Date**: 2026-03-06
**Complexity**: Medium (4-5 days implementation)

## Executive Summary

This architecture transforms the Photo Frame app from single-source (SMB only) to multi-source photo loading, supporting:
- **SMB network shares** (existing)
- **Local device storage** (NEW)
- **Extensible for future sources** (Google Drive, Dropbox, etc.)

All configured sources work together - photos are randomly mixed from all enabled sources in the slideshow.

## Key Design Decisions

### 1. Abstraction via PhotoSource Interface
**Decision**: Create `PhotoSource` interface for all photo sources
**Rationale**:
- Decouples repository from specific source implementations
- Enables easy addition of new source types
- Each source independently responsible for scanning/validation

**Alternative Considered**: Keep SMB hardcoded, add local as special case
**Why Rejected**: Not extensible, would require more refactoring later

### 2. Multi-Source Repository Pattern
**Decision**: Create `MultiSourcePhotoRepositoryImpl` that aggregates multiple sources
**Rationale**:
- Parallel scanning of sources (performance)
- Isolated failure handling (one source failure doesn't block others)
- Centralized aggregation logic

**Alternative Considered**: Modify existing SlideshowRepositoryImpl
**Why Rejected**: Too much complexity, harder to test, breaks single responsibility

### 3. MediaStore for Local Photos
**Decision**: Use Android MediaStore API for local photo scanning
**Rationale**:
- Native Android API, optimized for media queries
- No broad storage permissions needed (scoped storage)
- Built-in thumbnail support
- Automatic indexing by system

**Alternative Considered**: Manual file scanning via File API
**Why Rejected**: Requires storage permissions, slower, no thumbnails

### 4. Background Caching with WorkManager
**Decision**: Use WorkManager for periodic background scanning
**Rationale**:
- Slideshow starts instantly with cached results
- Battery-aware (only runs when not low battery)
- Survives app restarts
- Automatic retry on failure

**Alternative Considered**: Scan on-demand when slideshow starts
**Why Rejected**: Slow startup for large local collections (10,000+ photos)

### 5. MediaStore ContentObserver
**Decision**: Monitor MediaStore for new photos, trigger immediate scan
**Rationale**:
- Near real-time updates when user takes photos
- Minimal battery impact (passive observation)
- Better UX (new photos appear quickly)

**Alternative Considered**: Only scan periodically
**Why Rejected**: Delay between taking photo and seeing it in slideshow

### 6. Random Mix Aggregation
**Decision**: Shuffle photos from all sources together (Fisher-Yates)
**Rationale**:
- User requirement: "random mix"
- Unbiased randomization across sources
- Simple to implement and understand

**Alternative Considered**: Weighted by source (70% SMB, 30% local)
**Why Rejected**: Not requested; adds complexity; can add later if needed

### 7. Backwards Compatible Migration
**Decision**: Auto-migrate existing SMB config to new multi-source format
**Rationale**:
- Zero breaking changes for existing users
- Seamless upgrade experience
- SMB-only users see no difference

**Alternative Considered**: Force users to reconfigure
**Why Rejected**: Bad UX, unnecessary work for users

## Architecture Highlights

### Component Diagram
```
┌─────────────────────────────────────────────┐
│         MultiSourcePhotoRepository          │
│  - loadPhotos()                             │
│  - addPhotoSource()                         │
│  - removePhotoSource()                      │
└──────────────┬──────────────────────────────┘
               │
               ↓
┌──────────────────────────────────────────────┐
│         PhotoSourcesManager                  │
│  - getEnabledSources()                       │
│  - addSource() / removeSource()              │
│  - Persists to DataStore                     │
└──────┬───────────────────────────────────────┘
       │
       ├─→ SmbPhotoSource (wraps SmbPhotoDataSource)
       └─→ LocalPhotoSource (wraps LocalPhotoDataSource)
```

### Data Flow
```
User adds sources in Settings
    ↓
PhotoSourceConfig → DataStore
    ↓
MultiSourcePhotoRepository.loadPhotos()
    ↓
Scan sources in parallel (async/await)
    ↓
Aggregate all photos
    ↓
Shuffle (Fisher-Yates)
    ↓
PhotoBufferManager
    ↓
Display slideshow
```

## New Components

### 1. Core Models
- `PhotoSource` interface - Abstraction for all sources
- `PhotoSourceConfig` data class - Persisted configuration
- `PhotoSourceType` enum - SMB, LOCAL, etc.

### 2. Source Implementations
- `SmbPhotoSource` - Wraps existing SMB logic
- `LocalPhotoSource` - New local storage source

### 3. Data Sources
- `LocalPhotoDataSource` - MediaStore queries
- `PhotoSourcesManager` - Manages source list

### 4. Repository
- `MultiSourcePhotoRepositoryImpl` - Aggregates sources

### 5. Background Workers
- `LocalPhotoScanWorker` - Periodic background scanning
- `MediaStoreObserver` - Real-time photo detection

### 6. UI Components
- Updated `SettingsScreen` - List of sources
- `AddSourceDialog` - Add SMB or local source
- `LocalFolderPicker` - SAF folder selection

## Performance Characteristics

### Scanning Performance
- **Parallel scanning**: Multiple sources scan simultaneously
- **MediaStore optimization**: Uses native Android indexing
- **Background caching**: Results ready before user starts slideshow
- **Incremental loading**: First 100 photos load immediately

### Memory Impact
- **Photo model**: Unchanged (~200 bytes per photo)
- **10,000 photos**: ~2MB metadata
- **Bitmap loading**: Unchanged (uses existing PhotoBufferManager)

### Battery Impact
- **WorkManager**: Only runs when battery not low
- **ContentObserver**: Passive, minimal impact
- **Scanning frequency**: 1 hour default, configurable

## Security & Privacy

### Scoped Storage (Android 10+)
- No broad READ_EXTERNAL_STORAGE permission needed
- User explicitly grants access per folder via SAF
- Permissions persist across app restarts

### Credential Storage
- SMB passwords: Continue using KeystoreCredentialStore
- Local folders: URI permissions via `takePersistableUriPermission()`

### URI Security
- Content URIs are scoped to selected folders only
- Cannot access other user data
- Automatically revoked if user uninstalls app

## Testing Strategy

### Unit Tests (>80% coverage target)
- PhotoSource implementations
- PhotoSourcesManager (add/remove/persist)
- MultiSourcePhotoRepositoryImpl (aggregation logic)
- Fisher-Yates shuffle algorithm

### Integration Tests
- Multi-source loading end-to-end
- Migration from single SMB to multi-source
- Background scanning with WorkManager
- MediaStore observer integration

### E2E Tests
- Add/remove sources via UI
- Select local folders with SAF
- Start slideshow with multiple sources
- Handle empty sources gracefully

### Performance Tests
- 10,000+ photos across multiple sources
- Parallel scanning performance
- Memory profiling
- Background scan duration

### Accessibility Tests
- TalkBack navigation
- Touch target sizes
- Content descriptions
- Keyboard navigation

## Migration Path

### Existing Users (SMB Only)
1. Detect existing `SmbConnection` in DataStore
2. Convert to `PhotoSourceConfig` with id="smb-migrated-1"
3. Save to new `photo_sources` list
4. Set migration flag
5. App works exactly as before

### New Users
- Clean install, no migration needed
- Can add SMB, local, or both from settings

## Risk Assessment

### High Priority Risks
| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| MediaStore queries slow on large libraries | High | Medium | Background caching with WorkManager |
| URI permissions not persisting | High | Low | Extensive testing on Android 10-14 |
| Migration breaks existing SMB users | Critical | Low | Thorough backward compatibility tests |

### Medium Priority Risks
| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Parallel scanning race conditions | Medium | Low | Proper coroutine usage with supervision |
| Photo aggregation bias | Medium | Low | Use Fisher-Yates shuffle algorithm |

### Low Priority Risks
| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| UI complexity | Low | Low | Iterative design with user feedback |
| DataStore serialization issues | Low | Low | Comprehensive serialization tests |

## Success Criteria

- ✅ Users can add multiple local folders
- ✅ Users can add multiple SMB sources
- ✅ Photos from all sources display together in slideshow
- ✅ Shuffle randomizes across all sources
- ✅ Existing SMB-only users upgrade without issues
- ✅ Background scanning completes within 5 seconds for 1000 photos
- ✅ No memory leaks with 10,000+ photos
- ✅ All accessibility tests pass

## Timeline

- **Phase 1**: Core abstraction - 1 day
- **Phase 2**: Local source implementation - 1 day
- **Phase 3**: Multi-source repository - 1 day
- **Phase 4**: UI updates - 1 day
- **Phase 5**: Testing + polish - 1 day

**Total**: 4-5 days

## Open Questions

None - all requirements clarified in Phase 1 Q&A.

## Recommendation

**Approve and proceed to Phase 4: Test Planning**

This architecture is:
- ✅ Well-abstracted and extensible
- ✅ Backwards compatible
- ✅ Performance-optimized
- ✅ Secure and privacy-respecting
- ✅ Testable
- ✅ Aligned with Android best practices

The design addresses all requirements:
1. ✅ Media Picker + File browser support
2. ✅ Modern scoped storage (no permissions)
3. ✅ Random mix aggregation
4. ✅ Multiple local folders
5. ✅ Background caching + automatic monitoring
6. ✅ Fully configurable sources (add/remove SMB, local, both)

## Appendix

### Files to Create (15 new files)
- 4 model files (PhotoSourceConfig, PhotoSourceType, SourceConfig, LoadingState)
- 3 interface/abstract files (PhotoSource, updated repository interfaces)
- 4 implementation files (SmbPhotoSource, LocalPhotoSource, LocalPhotoDataSource, MultiSourcePhotoRepositoryImpl)
- 2 worker files (LocalPhotoScanWorker, MediaStoreObserver)
- 1 manager file (PhotoSourcesManager)
- 1 DI update (CoreModule)

### Files to Modify (5 files)
- SlideshowRepository.kt (extend interface)
- SettingsRepository.kt (add multi-source methods)
- SettingsRepositoryImpl.kt (migration logic)
- SettingsScreen.kt (UI updates)
- SettingsViewModel.kt (source management)

### LOC Estimate
- New code: ~2,000 lines
- Modified code: ~500 lines
- Test code: ~1,500 lines
- **Total**: ~4,000 lines

### Dependencies (No New Dependencies Needed)
All required dependencies already in project:
- ✅ Coroutines (parallel scanning)
- ✅ WorkManager (background scanning)
- ✅ DataStore (settings persistence)
- ✅ Hilt (dependency injection)
- ✅ Coil (image loading - already supports content:// URIs)
