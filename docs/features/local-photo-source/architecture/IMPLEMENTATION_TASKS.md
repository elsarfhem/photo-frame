# Implementation Tasks: Local Photo Source

**Generated**: 2026-03-06
**Estimated Effort**: 3-4 days (Medium-sized feature)

## Task Breakdown

### Phase 1: Core Abstraction (Day 1)

**Task 1.1: Create PhotoSource Interface**
- File: `core/src/main/java/com/photoframe/core/source/PhotoSource.kt`
- Effort: 30 minutes
- Dependencies: None
- Testing: Unit tests for interface contract

**Task 1.2: Create PhotoSourceConfig Model**
- File: `core/src/main/java/com/photoframe/core/model/PhotoSourceConfig.kt`
- Effort: 45 minutes
- Dependencies: None
- Add serialization support for DataStore
- Testing: Serialization/deserialization tests

**Task 1.3: Implement SmbPhotoSource**
- File: `core/src/main/java/com/photoframe/core/source/SmbPhotoSource.kt`
- Effort: 2 hours
- Dependencies: Task 1.1
- Extract SMB logic from SlideshowRepositoryImpl
- Testing: Unit tests with mocked SmbClient

**Task 1.4: Create PhotoSourcesManager**
- File: `core/src/main/java/com/photoframe/core/data/PhotoSourcesManager.kt`
- Effort: 2 hours
- Dependencies: Task 1.1, 1.2
- Manages list of PhotoSourceConfig
- Persists to DataStore
- Testing: Unit tests for add/remove/update operations

### Phase 2: Local Photo Source (Day 2)

**Task 2.1: Implement LocalPhotoDataSource**
- File: `core/src/main/java/com/photoframe/core/data/LocalPhotoDataSource.kt`
- Effort: 3 hours
- Dependencies: None
- MediaStore queries
- URI to Photo conversion
- Testing: Instrumented tests with sample media

**Task 2.2: Implement LocalPhotoSource**
- File: `core/src/main/java/com/photoframe/core/source/LocalPhotoSource.kt`
- Effort: 1 hour
- Dependencies: Task 1.1, 2.1
- Wraps LocalPhotoDataSource
- Testing: Unit tests with mocked data source

**Task 2.3: Add MediaStore Observer**
- File: `core/src/main/java/com/photoframe/core/worker/MediaStoreObserver.kt`
- Effort: 1.5 hours
- Dependencies: Task 2.1
- ContentObserver for MediaStore changes
- Integration with WorkManager
- Testing: Instrumented tests for observer lifecycle

**Task 2.4: Create LocalPhotoScanWorker**
- File: `core/src/main/java/com/photoframe/core/worker/LocalPhotoScanWorker.kt`
- Effort: 1.5 hours
- Dependencies: Task 2.1, 2.2
- Background scanning with WorkManager
- Caching scanned photos
- Testing: Worker tests with WorkManager TestDriver

### Phase 3: Multi-Source Repository (Day 3)

**Task 3.1: Extend SlideshowRepository Interface**
- File: `core/src/main/java/com/photoframe/core/repository/SlideshowRepository.kt`
- Effort: 30 minutes
- Dependencies: None
- Add multi-source methods
- Backwards compatible

**Task 3.2: Implement MultiSourcePhotoRepositoryImpl**
- File: `core/src/main/java/com/photoframe/core/repository/MultiSourcePhotoRepositoryImpl.kt`
- Effort: 4 hours
- Dependencies: Task 1.3, 1.4, 2.2, 3.1
- Parallel source scanning
- Photo aggregation
- Random shuffling across sources
- Testing: Unit tests for aggregation logic

**Task 3.3: Update Dependency Injection**
- File: `core/src/main/java/com/photoframe/core/di/CoreModule.kt`
- Effort: 1 hour
- Dependencies: Task 3.2
- Bind new implementations
- Set up WorkManager
- Testing: Integration tests for DI graph

**Task 3.4: Implement Settings Migration**
- File: `core/src/main/java/com/photoframe/core/repository/SettingsRepositoryImpl.kt`
- Effort: 2 hours
- Dependencies: Task 1.4
- Migrate existing SmbConnection to PhotoSourceConfig
- One-time migration flag
- Testing: Migration tests

### Phase 4: UI Updates (Day 4)

**Task 4.1: Update Settings Screen**
- File: `app/src/main/java/com/photoframe/app/ui/settings/SettingsScreen.kt`
- Effort: 3 hours
- Dependencies: Task 3.2
- List of sources
- Add/remove source buttons
- Enable/disable toggles
- Testing: Compose UI tests

**Task 4.2: Create Add Source Dialog**
- File: `app/src/main/java/com/photoframe/app/ui/settings/AddSourceDialog.kt`
- Effort: 2 hours
- Dependencies: Task 4.1
- Choose source type (SMB/Local)
- Configure source based on type
- Testing: Compose UI tests

**Task 4.3: Create Local Folder Picker**
- File: `app/src/main/java/com/photoframe/app/ui/settings/LocalFolderPicker.kt`
- Effort: 2 hours
- Dependencies: Task 4.2
- Launch SAF/MediaPicker
- Display selected folders
- Persist URI permissions
- Testing: Instrumented tests with ActivityScenario

**Task 4.4: Update ViewModel**
- File: `app/src/main/java/com/photoframe/app/ui/settings/SettingsViewModel.kt`
- Effort: 1.5 hours
- Dependencies: Task 3.2, 4.1
- Expose sources StateFlow
- Add/remove/update source operations
- Testing: ViewModel tests

### Phase 5: Integration & Testing (Day 4-5)

**Task 5.1: Integration Testing**
- Effort: 2 hours
- Test end-to-end flow: add sources → scan → slideshow
- Test source combinations (SMB only, local only, both)
- Test empty sources
- Test error handling

**Task 5.2: Performance Testing**
- Effort: 1.5 hours
- Test with 10,000+ photos across multiple sources
- Measure parallel scanning performance
- Test background caching effectiveness
- Memory profiling

**Task 5.3: Accessibility Testing**
- Effort: 1 hour
- TalkBack navigation for new UI
- Touch target sizes
- Content descriptions

**Task 5.4: Migration Testing**
- Effort: 1 hour
- Test upgrade from single SMB to multi-source
- Verify existing SMB config still works
- Test backward compatibility

### Phase 6: Documentation & Polish (Day 5)

**Task 6.1: Update User Documentation**
- Effort: 1 hour
- Update README with local source instructions
- Add screenshots of new settings UI
- Document folder selection process

**Task 6.2: Code Documentation**
- Effort: 1 hour
- KDoc for all new public APIs
- Architecture decision records
- Update existing docs

**Task 6.3: UI Polish**
- Effort: 1.5 hours
- Loading states for source scanning
- Error messages
- Empty states
- Source icons/badges

## Estimated Timeline

- **Day 1**: Core abstraction layer (6.5 hours)
- **Day 2**: Local photo source implementation (7 hours)
- **Day 3**: Multi-source repository (7.5 hours)
- **Day 4**: UI updates + integration (8.5 hours)
- **Day 5**: Testing + documentation + polish (5.5 hours)

**Total**: ~35 hours (4-5 working days)

## Critical Path

```
Task 1.1 (PhotoSource interface)
    ↓
Task 1.3 (SmbPhotoSource) + Task 2.1 (LocalPhotoDataSource)
    ↓
Task 1.4 (PhotoSourcesManager) + Task 2.2 (LocalPhotoSource)
    ↓
Task 3.2 (MultiSourcePhotoRepositoryImpl)
    ↓
Task 4.1 (Settings Screen)
    ↓
Task 5.1 (Integration Testing)
```

## Risk Assessment

### High Risk
- **MediaStore queries performance**: Mitigate with background caching
- **URI permission persistence**: Test thoroughly on different Android versions
- **Migration from single to multi-source**: Extensive backward compatibility testing

### Medium Risk
- **Parallel scanning complexity**: Use coroutines carefully, handle failures
- **Photo aggregation shuffling**: Ensure unbiased randomization

### Low Risk
- **UI updates**: Straightforward Compose changes
- **Settings persistence**: Already using DataStore

## Testing Coverage Targets

- Unit tests: >80% coverage for new code
- Integration tests: All critical paths
- E2E tests: Main user flows
- Performance tests: Large collections (10K+ photos)
- Accessibility tests: TalkBack navigation

## Success Metrics

- ✅ Users can add multiple local folders
- ✅ Users can add multiple SMB sources
- ✅ Photos from all sources appear in slideshow
- ✅ Shuffle works across all sources
- ✅ Existing SMB-only users upgrade seamlessly
- ✅ Background scanning completes within 5 seconds for 1000 photos
- ✅ No memory leaks with large collections
- ✅ Passes all accessibility tests

## Next Steps

1. Review architecture and tasks
2. Get approval to proceed
3. Create implementation plan
4. Begin Phase 5: Implementation
