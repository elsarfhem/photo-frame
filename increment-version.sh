#!/bin/bash
# Script to increment version for Google Play release

VERSION_FILE="version.properties"

if [ ! -f "$VERSION_FILE" ]; then
    echo "Error: $VERSION_FILE not found"
    exit 1
fi

# Read current values
CURRENT_VERSION=$(grep "^versionName=" "$VERSION_FILE" | cut -d'=' -f2)
CURRENT_CODE=$(grep "^versionCode=" "$VERSION_FILE" | cut -d'=' -f2)

echo "Current version: $CURRENT_VERSION ($CURRENT_CODE)"
echo ""
echo "Select version bump type:"
echo "1) Patch (bug fixes only) - e.g., 1.0.0 -> 1.0.1"
echo "2) Minor (new features) - e.g., 1.0.0 -> 1.1.0"
echo "3) Major (breaking changes) - e.g., 1.0.0 -> 2.0.0"
echo "4) Custom (enter manually)"
echo ""
read -p "Choice (1-4): " choice

IFS='.' read -r -a version_parts <<< "$CURRENT_VERSION"
MAJOR="${version_parts[0]}"
MINOR="${version_parts[1]}"
PATCH="${version_parts[2]}"

case $choice in
    1)
        # Patch bump
        PATCH=$((PATCH + 1))
        NEW_VERSION="$MAJOR.$MINOR.$PATCH"
        ;;
    2)
        # Minor bump
        MINOR=$((MINOR + 1))
        PATCH=0
        NEW_VERSION="$MAJOR.$MINOR.$PATCH"
        ;;
    3)
        # Major bump
        MAJOR=$((MAJOR + 1))
        MINOR=0
        PATCH=0
        NEW_VERSION="$MAJOR.$MINOR.$PATCH"
        ;;
    4)
        # Custom
        read -p "Enter new version (e.g., 1.2.3): " NEW_VERSION
        ;;
    *)
        echo "Invalid choice"
        exit 1
        ;;
esac

NEW_CODE=$((CURRENT_CODE + 1))

echo ""
echo "New version will be: $NEW_VERSION ($NEW_CODE)"
read -p "Proceed? (y/n): " confirm

if [ "$confirm" = "y" ] || [ "$confirm" = "Y" ]; then
    # Update version.properties
    sed -i.bak "s/^versionName=.*/versionName=$NEW_VERSION/" "$VERSION_FILE"
    sed -i.bak "s/^versionCode=.*/versionCode=$NEW_CODE/" "$VERSION_FILE"
    rm -f "$VERSION_FILE.bak"

    echo ""
    echo "✓ Version updated to $NEW_VERSION ($NEW_CODE)"
    echo ""
    echo "Next steps:"
    echo "1. ./gradlew clean bundleRelease"
    echo "2. Upload app/build/outputs/bundle/release/app-release.aab to Google Play"
    echo "3. git add version.properties"
    echo "4. git commit -m \"Bump version to $NEW_VERSION\""
    echo "5. git tag v$NEW_VERSION"
else
    echo "Cancelled"
    exit 1
fi
