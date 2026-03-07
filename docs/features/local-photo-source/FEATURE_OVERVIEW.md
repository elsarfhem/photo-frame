# Feature: Local Photo Source

**Status**: Phase 1 - Requirements Refinement
**Created**: 2026-03-06
**Platform**: Android
**Feature Slug**: local-photo-source

## Overview

Add the ability to load photos from local device storage in addition to the existing SMB network share source. Photo sources are not mutually exclusive - all configured sources should contribute photos to the slideshow together.

## Core Requirements

1. Add local device storage as a photo source
2. Support multiple photo sources simultaneously (SMB + Local)
3. Aggregate photos from all configured sources
4. Maintain existing SMB functionality

## Success Criteria

- Users can select local folders/albums for slideshow
- Photos from SMB and local storage appear together in slideshow
- Settings UI supports configuring multiple sources
- No breaking changes to existing SMB functionality

## Phase Status

- [x] Phase 0: Initialization
- [x] Phase 1: Requirements Refinement
- [x] Phase 2: Architecture Design
- [x] Phase 3: Core Implementation (Backend)
- [ ] Phase 4: UI Implementation (Frontend)
- [ ] Phase 5: Testing & Validation
- [ ] Phase 6: Polish & Documentation
