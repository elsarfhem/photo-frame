# Easy Photo Frame - Release Notes

## Version 1.0.0 - Initial Release

### Overview
Easy Photo Frame transforms your Android tablet into a beautiful, always-on digital photo frame. Display photos from network shares with smooth transitions, intelligent slideshow features, and automated scheduling.

### Key Features

#### 📷 Photo Sources
- **SMB/Network Share Support**: Connect to Windows shares, NAS devices, or network storage
- **Local Storage**: Display photos from device folders and media library
- **Multi-Source**: Combine multiple photo sources in a single slideshow
- **RAW Image Support**: Display professional RAW formats (NEF, CR2, ARW, DNG)

#### 🎬 Slideshow Transitions
- **Fade**: Classic smooth crossfade between photos
- **Slide**: Dynamic sliding transitions with directional awareness
- **Ken Burns**: Cinematic zoom and pan effects
- **Pan Animation**: Intelligent panning that fills the screen (no black bars)
  - Automatically pans horizontally for landscape photos
  - Pans vertically for portrait photos
  - Smooth continuous motion

#### ⚙️ Customization
- **Display Interval**: Set photo duration from 3 to 300 seconds (default: 10s)
- **Shuffle Mode**: Randomize photo order for variety
- **Transition Selection**: Choose your preferred animation style
- **Smart Pan**: Photos intelligently fill the screen with smooth scrolling

#### ⏰ Scheduling
- **Auto Start/Stop**: Schedule when slideshow runs (e.g., 8 AM - 10 PM)
- **Device Wake**: Automatically turns on screen at scheduled time
- **Auto Launch**: App opens automatically when schedule starts
- **Auto Close**: Gracefully closes when schedule ends
- **Boot Recovery**: Resumes schedule after device reboot

#### 🛡️ Reliability
- **24/7 Operation**: Designed for continuous, unattended operation
- **Auto-Recovery**: Automatically restarts after crashes
- **Memory Management**: Proactive cache clearing prevents memory leaks
- **Watchdog Monitor**: Detects and recovers from hung states
- **Smart Buffering**: Preloads photos for smooth, uninterrupted playback

#### 🎨 User Interface
- **Full Screen Immersive**: No navigation bars or status bars
- **Landscape Optimized**: Perfect for wall-mounted tablets
- **Settings Screen**: Easy configuration without leaving full-screen mode
- **Photo Sources Manager**: Add, edit, and manage multiple photo sources
- **Setup Wizard**: Guided first-time setup

### Technical Highlights
- **Efficient Image Loading**: Smart caching and buffer management
- **Timeout Handling**: Robust network error recovery
- **Read-Ahead Buffer**: Maintains 2 photos ahead, 1 behind
- **Dynamic Timeouts**: Adjusts based on your display interval
- **Modern Architecture**: MVVM pattern with Jetpack Compose

### Requirements
- Android 6.0 (API 23) or higher
- Network access for SMB shares (optional)
- Storage permissions for local photos

### Getting Started
1. **First Launch**: Follow the setup wizard to configure your first photo source
2. **Add SMB Share**: Enter server URL, share path, and credentials
3. **Configure Settings**: Set display interval, transitions, and schedule
4. **Start Slideshow**: Enjoy your photos in full-screen glory

### Ideal For
- Home photo displays
- Digital art frames
- Office lobby displays
- Store front displays
- Restaurant menu boards
- Hotel lobby displays
- Smart home dashboards

### Privacy & Security
- All data stored locally on device
- Network credentials stored securely
- No cloud services or data collection
- No ads, no tracking, no external analytics

### Support
For issues, feature requests, or questions, please contact hkisling software co.

---

**Version**: 1.0.0
**Release Date**: March 11, 2026
**Package**: com.photoframe.app
**Min SDK**: Android 6.0 (API 23)
**Target SDK**: Android 14 (API 34)
