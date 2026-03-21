DEVICE ?= emulator-5554
APK = app/build/outputs/apk/debug/app-debug.apk
AAB = app/build/outputs/bundle/release/app-release.aab
PKG = com.photoframe.app
ACTIVITY = $(PKG)/.MainActivity

.PHONY: build install run bir logs clean release uninstall

# Build debug APK
build:
	./gradlew assembleDebug

# Install on device/emulator
install: build
	adb -s $(DEVICE) install -r $(APK)

# Launch the app
run:
	adb -s $(DEVICE) shell am start -n $(ACTIVITY)

# Build + Install + Run
bir: install run

# Tail logs filtered to app tags
logs:
	adb -s $(DEVICE) logcat | grep -E "SlideshowScreen|PanTransition|PhotoFrame"

# Build release bundle
release:
	./gradlew clean bundleRelease
	@echo "AAB: $(AAB)"

# Clean build artifacts
clean:
	./gradlew clean

# Uninstall from device (needed after signature changes)
uninstall:
	adb -s $(DEVICE) uninstall $(PKG)
