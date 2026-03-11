# Versioning Strategy

## Overview

The app uses **automated semantic versioning** via the **axion-release-plugin**.

Versions are determined by:
- Git tags (e.g., `v1.0.0`)
- Conventional commit messages

## How It Works

### versionName (User-Facing)
- Source: Git tags
- Format: Semantic versioning `Major.Minor.Patch`
- Example: `1.0.0`, `1.2.3`, `2.0.0`

### versionCode (Google Play)
- Auto-calculated from versionName
- Formula: `Major × 10000 + Minor × 100 + Patch`
- Example: `1.2.3` → `10203`
- Always increases monotonically
- Maximum: `214.74.83` (under Google's 2.1B limit)

## Quick Start

### Check Current Version
```bash
./gradlew currentVersion
# Output: Project version: 1.0.0
```

### Create New Release

**Option 1: Auto-increment (recommended)**
```bash
# Creates next version based on commits
./gradlew release
```

**Option 2: Specific version**
```bash
./gradlew release -Prelease.forceVersion=1.1.0
```

**Option 3: Manual tag**
```bash
git tag v1.1.0
git push origin v1.1.0
```

### Build Release
```bash
./gradlew clean bundleRelease
```

## Conventional Commits

Use conventional commit format to control version bumps:

```bash
# Patch (1.0.0 → 1.0.1)
git commit -m "fix: resolve memory leak"

# Minor (1.0.0 → 1.1.0)
git commit -m "feat: add dark mode"

# Major (1.0.0 → 2.0.0)
git commit -m "feat: redesign UI

BREAKING CHANGE: requires data migration"
```

See **CONVENTIONAL_COMMITS.md** for full details.

## Version Calculation Examples

| Version | versionCode | Calculation |
|---------|-------------|-------------|
| 1.0.0   | 10000       | 1×10000 + 0×100 + 0 |
| 1.0.1   | 10001       | 1×10000 + 0×100 + 1 |
| 1.1.0   | 10100       | 1×10000 + 1×100 + 0 |
| 1.2.3   | 10203       | 1×10000 + 2×100 + 3 |
| 2.0.0   | 20000       | 2×10000 + 0×100 + 0 |

## Version History

| Version | Code  | Date       | Changes |
|---------|-------|------------|---------|
| 1.0.0   | 10000 | 2026-03-11 | Initial release |

## Benefits

✅ **Automated**: No manual file editing
✅ **Semantic**: Clear version meaning
✅ **Git-based**: Single source of truth
✅ **Monotonic**: versionCode always increases
✅ **Google Play compatible**: Formula ensures uniqueness

## Migration Notes

Switched from manual `version.properties` to axion-release-plugin for automated semantic versioning based on git tags and conventional commits.
