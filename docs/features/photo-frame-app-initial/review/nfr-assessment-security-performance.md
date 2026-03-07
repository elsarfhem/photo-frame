# NFR Assessment - Security & Performance

**Feature**: Digital Photo Frame - Android Tablet Application (MVP Phase 1)
**Reviewer**: Senior Dev 1 - Security & Performance Focus
**Date**: 2026-03-02
**Phase**: Phase 5A - Initial Assessment (Pre-Debate)
**Status**: READY FOR TEAM REVIEW

---

## 1. Executive Summary

### Focus Area
Security vulnerabilities, performance bottlenecks, resource optimization

### Overall Assessment
**⚠️ PASS WITH CRITICAL CONCERNS**

This architecture has significant security gaps that must be addressed before implementation. While the performance approach is sound (proven libraries, reasonable buffer strategy), there are critical security vulnerabilities around credential storage, network security, and sensitive data handling. These are non-negotiable for a 24/7 network-connected device with stored credentials.

### Top 3 Critical Concerns

1. **🔴 CRITICAL: Unencrypted Credential Storage**
   - SMB username/password stored in DataStore Preferences (plaintext)
   - PRD defers encryption to Phase 2, but credentials are accessible day-1
   - Risk: Any malicious app with root access or backup extraction can read credentials

2. **🔴 CRITICAL: SMB Network Security Undefined**
   - No mention of SMB signing verification, encryption enforcement, or certificate validation
   - jcifs-ng supports both secure (SMB 3.x with encryption) and insecure (SMB 1.x) protocols
   - Risk: Man-in-the-middle attacks, credential interception on local network

3. **🟡 HIGH: Performance Validation Deferred to Week 8**
   - <2s photo load NFR not validated until Week 8
   - Standard jcifs-ng approach may not meet NFR
   - Risk: Late discovery of performance gaps requiring significant rework

### Recommendation
**⚠️ PROCEED WITH MANDATORY MITIGATIONS**

The architecture can proceed to implementation ONLY IF:
1. Android Keystore encryption is implemented for credentials (non-negotiable)
2. SMB security configuration is defined (SMB 3.x with encryption, signing verification)
3. Early performance profiling (Week 4-5, not Week 8) to validate <2s NFR

---

## 2. NFR Coverage Assessment

### 2.1 Security - Data Protection

**Checklist Items Reviewed**: 7 items from NFR checklist (SEC-010 through SEC-016)

**Architecture Coverage**: ❌ **MISSING CRITICAL ITEMS**

**Assessment**:

The architecture defers credential encryption to Phase 2, storing SMB credentials in DataStore Preferences. This is a critical security vulnerability for a device that:
- Runs 24/7 with network credentials stored
- May be left unattended in homes
- Connects to personal network shares with potentially sensitive photos

**DataStore Preferences Security Gaps**:
- DataStore stores data in plaintext XML files in app's private directory
- While private storage prevents other apps from reading (in non-rooted devices), it does NOT prevent:
  - Root access (common on custom ROMs, developer devices)
  - Android backup extraction (`adb backup` or cloud backups)
  - Physical device access (USB debugging enabled)
  - Malware with escalated privileges

**Photo Metadata Privacy**:
- Architecture does not specify if photo paths/filenames are logged
- Photo paths may contain sensitive information (e.g., `/Personal/Medical/`)
- No mention of PII in logs policy

**Architecture References**:
- ADR Decision 8 (Scheduling): "Secure password storage (DataStore for MVP, encrypt in Phase 2)"
- FINAL_ARCHITECTURE Section 4.3.2: "Use EncryptedSharedPreferences or Android Keystore for Phase 2"

**Concerns**:

**⚠️ Concern 1: SMB Credentials Stored Unencrypted**
- **Risk Level**: 🔴 **CRITICAL**
- **Severity**: P0 (Must Fix Before Implementation)
- **Impact**:
  - Credentials accessible via backup extraction or root access
  - Network share compromise (attacker can access all photos, potentially other files)
  - User privacy violation (GDPR concerns if EU users)
  - Reputational damage if disclosed
- **Likelihood**: Medium (root access common on developer/enthusiast devices, backup extraction straightforward)
- **Current Mitigation**: Private app directory (insufficient for sensitive credentials)
- **Required Mitigation**:
  - Use Android Keystore API to encrypt credentials at rest
  - Store encrypted credentials in DataStore, decrypt on use
  - Use AES-256 encryption with Keystore-backed key
- **Effort**: Small (2-3 days)
- **Example Implementation**:
  ```kotlin
  class SecureCredentialStorage(context: Context) {
      private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
      private val cipher = Cipher.getInstance("AES/GCM/NoPadding")

      fun storeCredentials(username: String, password: String) {
          val key = getOrCreateKey()
          cipher.init(Cipher.ENCRYPT_MODE, key)
          val encryptedPassword = cipher.doFinal(password.toByteArray())
          // Store IV and encrypted password in DataStore
      }
  }
  ```

**⚠️ Concern 2: No PII Logging Policy**
- **Risk Level**: 🔴 **CRITICAL**
- **Severity**: P0 (Must Fix Before Implementation)
- **Impact**:
  - SMB credentials logged in plaintext (common mistake in network debugging)
  - Photo paths logged (may contain sensitive folder names)
  - Error messages expose network topology or usernames
  - Crash reports contain sensitive data
- **Likelihood**: High (developers often log request/response for debugging)
- **Current Mitigation**: None mentioned in architecture
- **Required Mitigation**:
  - Define explicit PII logging policy: NEVER log credentials, mask photo paths
  - Use structured logging with PII redaction filters
  - Sanitize error messages before logging
  - Configure Crashlytics to exclude sensitive fields
- **Effort**: Small (1-2 days for policy + code review checklist)
- **Example Policy**:
  ```
  PROHIBITED LOGGING:
  - SMB username/password (always mask)
  - Full photo paths (log only filename, not full path)
  - Network share URLs (mask username if embedded in URL)

  ALLOWED LOGGING:
  - Photo count, display interval, transition type
  - Error types (connection failed, timeout) without credentials
  - Performance metrics (load time, memory usage)
  ```

**⚠️ Concern 3: No Photo Metadata Encryption**
- **Risk Level**: 🟢 **MEDIUM**
- **Severity**: P2 (Address If Time Permits)
- **Impact**:
  - Photo paths cached in Coil disk cache (512MB) are accessible
  - Cached photos readable by backup extraction (not encrypted)
  - Thumbnail cache may persist sensitive images
- **Likelihood**: Low (requires physical access or backup extraction)
- **Current Mitigation**: Coil cache in private app directory
- **Required Mitigation**:
  - For MVP: Acceptable risk (private directory sufficient)
  - For Phase 2: Implement encrypted disk cache using Coil's custom cache
- **Effort**: Medium (5-7 days for encrypted cache)

**Recommendations**:
- [ ] **MANDATORY**: Implement Android Keystore encryption for SMB credentials (2-3 days)
- [ ] **MANDATORY**: Define and enforce PII logging policy (1-2 days)
- [ ] **MANDATORY**: Configure Crashlytics to exclude sensitive fields (1 day)
- [ ] Optional: Document risk acceptance for unencrypted photo cache (MVP only)

**Checklist Coverage**:
- ❌ SEC-010: PII data encrypted at rest (credentials NOT encrypted)
- ⚠️ SEC-011: PII data encrypted in transit (SMB encryption not configured)
- ❌ SEC-012: No PII in logs (policy not defined)
- ❌ SEC-013: No sensitive data in crash reports (not configured)
- ❌ SEC-014: Secure storage for credentials and tokens (using plaintext DataStore)
- ✅ SEC-016: Data minimization (only stores necessary data)
- N/A SEC-015: Payment data (not applicable)

---

### 2.2 Security - Network Security

**Checklist Items Reviewed**: 5 items from NFR checklist (SEC-030 through SEC-034)

**Architecture Coverage**: ❌ **MISSING CRITICAL ITEMS**

**Assessment**:

The architecture specifies jcifs-ng for SMB access but does NOT define security configuration. jcifs-ng supports multiple SMB protocol versions with varying security:

**SMB Protocol Security Levels**:
- **SMB 1.0**: Insecure, deprecated, no encryption (vulnerable to EternalBlue, MITM)
- **SMB 2.0-2.1**: Better than v1, but no encryption by default
- **SMB 3.0+**: Supports encryption, signing verification, modern crypto

**Architecture Gaps**:
- No specification of minimum SMB version (should require SMB 2.0+, prefer 3.0+)
- No mention of SMB signing verification (prevents MITM tampering)
- No mention of SMB encryption enforcement (prevents packet sniffing)
- No certificate validation for SMB over TLS (if supported by server)

**Risk Scenario**:
1. User configures app to connect to SMB share
2. Attacker on same network intercepts SMB traffic (MITM)
3. If SMB 2.x without encryption: credentials sent in NTLM hash (crackable)
4. If SMB 1.x: credentials sent nearly plaintext
5. Attacker can capture photos, modify files, or impersonate server

**jcifs-ng Security Configuration**:
jcifs-ng allows configuration via properties:
```properties
jcifs.smb.client.minVersion=SMB210  # Require SMB 2.1+
jcifs.smb.client.maxVersion=SMB311  # Prefer SMB 3.1.1
jcifs.smb.client.signingPreferred=true  # Enable signing
jcifs.smb.client.ipcSigningEnforced=true  # Enforce signing for auth
```

**Concerns**:

**⚠️ Concern 1: SMB Protocol Version Not Constrained**
- **Risk Level**: 🔴 **CRITICAL**
- **Severity**: P0 (Must Fix Before Implementation)
- **Impact**:
  - App may negotiate SMB 1.0 with old servers (insecure protocol)
  - Credentials vulnerable to interception and cracking
  - No encryption, no signing = plaintext on wire
- **Likelihood**: Medium (depends on user's SMB server, some NAS devices default to SMB 1.x)
- **Current Mitigation**: jcifs-ng defaults (unclear, may allow SMB 1.x)
- **Required Mitigation**:
  - Configure jcifs-ng to require minimum SMB 2.0 (`minVersion=SMB210`)
  - Prefer SMB 3.0+ for encryption (`maxVersion=SMB311`)
  - Reject connections to SMB 1.x servers with clear error message
- **Effort**: Small (1 day for configuration)
- **Example Code**:
  ```kotlin
  class SmbClientConfig {
      init {
          Properties().apply {
              setProperty("jcifs.smb.client.minVersion", "SMB210")
              setProperty("jcifs.smb.client.maxVersion", "SMB311")
              setProperty("jcifs.smb.client.signingPreferred", "true")
              setProperty("jcifs.smb.client.ipcSigningEnforced", "true")
              Config.setProperties(this)
          }
      }
  }
  ```

**⚠️ Concern 2: No SMB Signing Verification**
- **Risk Level**: 🟡 **HIGH**
- **Severity**: P1 (Should Fix Before Launch)
- **Impact**:
  - Man-in-the-middle can tamper with SMB packets (modify photos, inject errors)
  - No integrity protection for photo data
  - Attacker can corrupt slideshow or inject malicious images
- **Likelihood**: Low (requires active MITM attack on local network)
- **Current Mitigation**: None (signing not mentioned)
- **Required Mitigation**:
  - Enable SMB signing (`signingPreferred=true`)
  - For sensitive environments, enforce signing (`signingEnforced=true`)
  - Document that unsigned SMB connections are less secure
- **Effort**: Small (included in configuration above)

**⚠️ Concern 3: No SMB Encryption Enforcement**
- **Risk Level**: 🟡 **HIGH**
- **Severity**: P1 (Should Fix Before Launch)
- **Impact**:
  - Photo content visible to network sniffers (privacy violation)
  - Credentials (NTLM hash) capturable by packet analysis
  - Local network eavesdropping possible
- **Likelihood**: Low (requires packet capture on local network, attacker must be on same LAN)
- **Current Mitigation**: None (encryption not mentioned)
- **Required Mitigation**:
  - For MVP: Document that SMB 3.0+ provides encryption, recommend users enable it on server
  - For Phase 2: Enforce encryption by checking negotiated protocol version
- **Effort**: Small (documentation for MVP, 2 days for enforcement in Phase 2)

**⚠️ Concern 4: No Certificate Validation for SMB over TLS**
- **Risk Level**: 🟢 **MEDIUM**
- **Severity**: P2 (Address If Time Permits)
- **Impact**: If SMB server uses TLS, lack of cert validation allows MITM
- **Likelihood**: Low (SMB over TLS rare for home networks)
- **Current Mitigation**: Not applicable (most SMB doesn't use TLS)
- **Required Mitigation**: For Phase 2, implement cert pinning if SMB over TLS supported
- **Effort**: Medium (3-5 days)

**Recommendations**:
- [ ] **MANDATORY**: Configure jcifs-ng to require SMB 2.0+ (1 day)
- [ ] **MANDATORY**: Enable SMB signing verification (included in above)
- [ ] **HIGHLY RECOMMENDED**: Document SMB 3.0+ encryption benefits in user guide
- [ ] Optional: Add UI indicator for connection security level (SMB version, encrypted Y/N)

**Checklist Coverage**:
- N/A SEC-030: HTTPS only (not applicable, SMB protocol)
- ❌ SEC-031: Certificate pinning (not addressed for SMB)
- N/A SEC-032: mTLS (not applicable to SMB)
- ⚠️ SEC-033: No cleartext traffic (SMB 2.x without encryption is cleartext)
- ❌ SEC-034: API keys not hardcoded (SMB credentials stored insecurely)

---

### 2.3 Security - Input Validation

**Checklist Items Reviewed**: 6 items from NFR checklist (SEC-020 through SEC-025)

**Architecture Coverage**: ⚠️ **PARTIAL**

**Assessment**:

The architecture does not explicitly address input validation for SMB paths and user inputs. This creates risks for path traversal, command injection, and malformed input handling.

**Input Attack Surfaces**:
1. **SMB URL/Path Input**: `smb://server/share/path`
   - Path traversal: `smb://server/share/../../etc/passwd`
   - Command injection: `smb://server/share/path$(malicious_command)`
   - URL parsing vulnerabilities
2. **Username Input**: May contain special characters
3. **Password Input**: May contain special characters requiring escaping
4. **Display Interval**: Numeric input (3-60 seconds)
5. **Schedule Times**: Time input validation

**jcifs-ng Path Handling**:
- jcifs-ng uses `SmbFile` class to parse SMB URLs
- Library handles most path normalization, but app should validate BEFORE passing to library
- Malformed URLs may cause exceptions or unexpected behavior

**Concerns**:

**⚠️ Concern 1: SMB Path Validation Not Specified**
- **Risk Level**: 🟡 **HIGH**
- **Severity**: P1 (Should Fix Before Launch)
- **Impact**:
  - Path traversal may access files outside intended share
  - Malformed paths may crash app or leak error info
  - Special characters in paths may cause parsing errors
- **Likelihood**: Medium (users may mistype paths, or malicious actors may craft URLs)
- **Current Mitigation**: jcifs-ng internal validation (unknown coverage)
- **Required Mitigation**:
  - Validate SMB URL format before passing to jcifs-ng
  - Reject paths with `..` (parent directory traversal)
  - Whitelist allowed characters in paths (alphanumeric, `-`, `_`, `/`, `.`)
  - Sanitize user input and display validation errors
- **Effort**: Small (2-3 days)
- **Example Validation**:
  ```kotlin
  fun validateSmbPath(path: String): Result<Unit> {
      return when {
          path.contains("..") -> Result.failure(InvalidPathException("Path traversal not allowed"))
          !path.matches(Regex("^smb://[a-zA-Z0-9.-]+/.*$")) -> Result.failure(InvalidPathException("Invalid SMB URL format"))
          path.length > 512 -> Result.failure(InvalidPathException("Path too long"))
          else -> Result.success(Unit)
      }
  }
  ```

**⚠️ Concern 2: Username/Password Special Character Handling**
- **Risk Level**: 🟢 **MEDIUM**
- **Severity**: P2 (Address If Time Permits)
- **Impact**:
  - Special characters in credentials may break authentication (e.g., `@`, `:`, `/`)
  - URL encoding issues if credentials embedded in SMB URL
  - Authentication failures with unclear error messages
- **Likelihood**: Medium (users with complex passwords may hit this)
- **Current Mitigation**: jcifs-ng handles encoding (assumed)
- **Required Mitigation**:
  - Document supported characters in username/password
  - Use NtlmPasswordAuthenticator (not URL-embedded credentials) to avoid encoding issues
  - Test with special characters (`@`, `#`, `$`, `%`, etc.)
- **Effort**: Small (1-2 days for testing + documentation)

**⚠️ Concern 3: Numeric Input Validation (Display Interval)**
- **Risk Level**: 🟢 **MEDIUM**
- **Severity**: P2 (Address If Time Permits)
- **Impact**:
  - Invalid intervals (0, negative, >60) may cause unexpected behavior
  - Extreme values may cause performance issues
- **Likelihood**: Low (UI likely constrains input, but validate server-side too)
- **Current Mitigation**: UI constraints (assumed)
- **Required Mitigation**:
  - Validate interval range: 3 ≤ interval ≤ 60
  - Reject non-numeric input
  - Clamp out-of-range values with warning
- **Effort**: Trivial (1 day)

**Recommendations**:
- [ ] **MANDATORY**: Implement SMB path validation (2-3 days)
- [ ] **RECOMMENDED**: Test username/password with special characters (1-2 days)
- [ ] **RECOMMENDED**: Validate all numeric inputs (1 day)
- [ ] Document input validation rules in architecture

**Checklist Coverage**:
- ⚠️ SEC-020: All user input validated (partial, SMB path validation missing)
- ✅ SEC-021: SQL injection prevention (N/A, no SQL in MVP)
- N/A SEC-022: XSS prevention (no WebViews)
- ⚠️ SEC-023: Command injection prevention (SMB path injection possible)
- ⚠️ SEC-024: Path traversal prevention (not validated)
- ✅ SEC-025: Buffer overflow prevention (Kotlin memory-safe)

---

### 2.4 Security - Code Security

**Checklist Items Reviewed**: 5 items from NFR checklist (SEC-040 through SEC-044)

**Architecture Coverage**: ⚠️ **PARTIAL**

**Assessment**:

The architecture does not specify code security practices like ProGuard/R8 obfuscation, root detection, or debug code removal.

**Concerns**:

**⚠️ Concern 1: No ProGuard/R8 Obfuscation Specified**
- **Risk Level**: 🟢 **MEDIUM**
- **Severity**: P2 (Address Before Launch)
- **Impact**:
  - Reverse engineering easier (attacker can decompile APK and read credential handling code)
  - Security logic visible to attackers
- **Likelihood**: Low (requires motivated attacker)
- **Current Mitigation**: None mentioned
- **Required Mitigation**:
  - Enable R8 obfuscation in release builds
  - Configure ProGuard rules to keep public APIs, obfuscate internals
- **Effort**: Trivial (1 day for configuration)

**⚠️ Concern 2: Root Detection Not Specified**
- **Risk Level**: 🟢 **MEDIUM**
- **Severity**: P3 (Nice to Have)
- **Impact**:
  - Rooted devices can bypass Android security (Keystore may be compromised)
  - App credentials more vulnerable on rooted devices
- **Likelihood**: Medium (enthusiast users may root devices)
- **Current Mitigation**: None mentioned
- **Required Mitigation**:
  - For MVP: Accept risk (document that root devices less secure)
  - For Phase 2: Implement root detection, warn user (don't block, just warn)
- **Effort**: Small (2-3 days for root detection library integration)

**Recommendations**:
- [ ] **RECOMMENDED**: Enable R8 obfuscation for release builds (1 day)
- [ ] Optional: Implement root detection with user warning (2-3 days)

**Checklist Coverage**:
- ❌ SEC-040: No hardcoded secrets (SMB credentials stored insecurely, effectively hardcoded in DataStore)
- ❌ SEC-041: ProGuard/R8 obfuscation (not mentioned)
- ⚠️ SEC-042: Root detection (not specified, may not be needed for MVP)
- ⚠️ SEC-043: No debug code in production (assume standard build process, but not documented)
- ⚠️ SEC-044: Secure coding practices (not explicitly documented)

---

### 2.5 Security - Permissions

**Checklist Items Reviewed**: Android permissions (implicit in architecture)

**Architecture Coverage**: ⚠️ **PARTIAL**

**Assessment**:

The architecture does not list required Android permissions. Based on functionality, the app will need:

**Required Permissions**:
- `INTERNET` (SMB network access)
- `ACCESS_NETWORK_STATE` (check connectivity)
- `WAKE_LOCK` (keep screen on during slideshow)
- `FOREGROUND_SERVICE` (if using service for 24/7 operation)

**Permissions Analysis**:
- ✅ Minimal permissions (only what's necessary)
- ✅ No dangerous permissions beyond network (no CAMERA, LOCATION, etc.)
- ⚠️ `WAKE_LOCK` may cause battery drain (acceptable for photo frame use case)

**Concerns**:

**⚠️ Concern 1: WAKE_LOCK Battery Impact**
- **Risk Level**: 🟢 **MEDIUM**
- **Severity**: P2 (Monitor)
- **Impact**: Screen always on = high battery drain (acceptable for plugged-in tablet, but should warn users)
- **Likelihood**: High (core use case requires screen on)
- **Current Mitigation**: None (PRD specifies <5% battery/hour, but that's for sleep mode)
- **Required Mitigation**:
  - Document that device should be plugged in for 24/7 use
  - Add UI warning if battery not charging
- **Effort**: Small (1 day for battery check + UI warning)

**Recommendations**:
- [ ] Document required permissions in architecture (1 hour)
- [ ] Implement battery charging check with user warning (1 day)

---

## 3. Performance Assessment

### 3.1 Performance - Photo Load Time (<2s NFR)

**Target NFR**: Photo load time < 2 seconds (P95)

**Architecture Coverage**: ⚠️ **PARTIAL - VALIDATION DEFERRED**

**Assessment**:

The architecture aims for <2s photo load using:
1. **Coil image loading** with downsampling to screen resolution (2560x1600)
2. **4-photo buffer** [Current-1, Current, Current+1, Current+2]
3. **Standard jcifs-ng** SMB client (no connection pooling)
4. **Pre-loading** (buffer fills ahead of current photo)

**Performance Calculation** (from architecture):
- Downsampled image: 2560 × 1600 × 4 bytes (ARGB_8888) = ~16MB per photo
- 4-photo buffer: 4 × 16MB = ~64MB

**Performance Risk Analysis**:

**SMB Photo Load Breakdown** (estimated):
1. SMB connection handshake: ~100-300ms (if not reused)
2. File open/read: ~50-150ms (depends on network latency)
3. File transfer: ~500-1500ms (for 4-8MB JPEG over 100Mbps LAN)
4. Coil decode + downsample: ~200-500ms (CPU-bound)
5. **Total estimated**: ~850-2450ms (P50-P95)

**Risk**: P95 load time may exceed 2s target, especially on slower networks (WiFi interference, congestion) or older tablets.

**Architecture Mitigation**:
- ADR Decision 5: "Profile early (Week 8), add connection pooling if needed"
- FINAL_ARCHITECTURE Section 7.1: "If <2s NFR not met: Add connection pooling first (lowest complexity)"

**Concerns**:

**⚠️ Concern 1: Performance Validation Deferred to Week 8**
- **Risk Level**: 🟡 **HIGH**
- **Severity**: P1 (Should Address Earlier)
- **Impact**:
  - If <2s NFR not met, requires rework (connection pooling, increased buffer, or memory-mapped I/O)
  - Week 8 is late in 3-4 month timeline (only 4-5 weeks remaining for fixes)
  - May delay MVP launch or require NFR relaxation
- **Likelihood**: Medium (standard jcifs-ng likely sufficient, but not guaranteed)
- **Current Mitigation**: Optimization plan defined (add connection pooling if needed)
- **Required Mitigation**:
  - Move performance profiling to **Week 4-5** (earlier validation)
  - Test on target hardware (tablet + real SMB share) in Week 5
  - If <2s not met, implement connection pooling in Week 6-7
- **Effort**: No additional effort (just shift timeline)

**⚠️ Concern 2: Network Variability Not Accounted For**
- **Risk Level**: 🟡 **HIGH**
- **Severity**: P1 (Should Fix Before Launch)
- **Impact**:
  - WiFi interference, congestion, or weak signal may cause >2s loads
  - User experience degrades with network issues
  - No retry logic or timeout handling specified
- **Likelihood**: High (WiFi networks are variable)
- **Current Mitigation**: 4-photo buffer provides some resilience
- **Required Mitigation**:
  - Implement timeout for SMB reads (e.g., 5s timeout)
  - Show loading indicator if photo takes >1s
  - Fall back to cached photos if network fails
  - Monitor network quality and warn user if poor
- **Effort**: Small (2-3 days for timeout + fallback logic)

**⚠️ Concern 3: jcifs-ng Connection Reuse Not Verified**
- **Risk Level**: 🟢 **MEDIUM**
- **Severity**: P2 (Validate Early)
- **Impact**:
  - If jcifs-ng doesn't reuse connections, handshake overhead (~200ms) on every photo
  - Connection pooling becomes necessary (adds complexity)
- **Likelihood**: Low (jcifs-ng likely reuses connections for sequential reads)
- **Current Mitigation**: Assumption that jcifs-ng handles connection reuse
- **Required Mitigation**:
  - Verify jcifs-ng connection reuse behavior with profiling
  - If not reused, implement connection pooling
- **Effort**: Small (1-2 days for profiling + verification)

**Recommendations**:
- [ ] **MANDATORY**: Move performance profiling to Week 4-5 (timeline adjustment)
- [ ] **MANDATORY**: Implement SMB read timeout and fallback logic (2-3 days)
- [ ] **RECOMMENDED**: Verify jcifs-ng connection reuse in Week 5 (1-2 days)
- [ ] **RECOMMENDED**: Add network quality monitoring (2 days)

---

### 3.2 Performance - 60fps Transitions

**Target NFR**: 60fps transitions (smooth animations)

**Architecture Coverage**: ✅ **GOOD**

**Assessment**:

The architecture uses **Jetpack Compose** with hardware-accelerated animations. Compose is optimized for 60fps UI updates.

**Compose Animation Performance**:
- Crossfade, slide, zoom transitions built into Compose (hardware-accelerated)
- GPU handles rendering (not CPU-bound)
- Bitmap already loaded in memory (no I/O during transition)

**Risk Analysis**:
- ✅ Compose animations are battle-tested (60fps standard)
- ✅ 4-photo buffer ensures next photo ready (no load stutter)
- ⚠️ High-resolution photos (>2560x1600) may cause frame drops if not downsampled

**Concerns**:

**⚠️ Concern 1: Downsampling Not Enforced**
- **Risk Level**: 🟢 **MEDIUM**
- **Severity**: P2 (Validate)
- **Impact**:
  - If Coil doesn't downsample correctly, full-resolution bitmaps (48MB) may cause OOM or slow rendering
  - Frame drops during transitions
- **Likelihood**: Low (Coil downsampling is automatic, but should verify config)
- **Current Mitigation**: Coil configured with screen resolution target
- **Required Mitigation**:
  - Verify Coil downsampling config: `size(2560, 1600)` in ImageRequest
  - Test with 12MP+ photos to ensure downsampling works
- **Effort**: Trivial (1 day for verification)

**Recommendations**:
- [ ] **RECOMMENDED**: Verify Coil downsampling configuration (1 day)
- [ ] Test transition performance with high-res photos (included in Week 4-5 profiling)

---

### 3.3 Performance - Memory Usage (<300MB NFR)

**Target NFR**: Total memory usage < 300MB

**Architecture Coverage**: ✅ **GOOD**

**Assessment**:

The architecture estimates memory usage:
- 4-photo buffer: ~64MB (downsampled to 2560x1600)
- Coil in-memory cache: 100MB
- Coil disk cache: 512MB (disk, not memory)
- App overhead: ~50-70MB (Compose, ViewModel, etc.)
- **Total estimated**: ~214-234MB (well under 300MB limit)

**Memory Safety**:
- ✅ Coil handles LRU eviction (automatic memory management)
- ✅ Downsampling prevents OOM from full-resolution photos
- ✅ Immutable data classes prevent accidental memory leaks

**Concerns**:

**⚠️ Concern 1: Memory Leak Risk During 24/7 Operation**
- **Risk Level**: 🟡 **HIGH**
- **Severity**: P1 (Must Validate)
- **Impact**:
  - Long-running coroutines or leaked references may cause memory leaks
  - App may crash after hours/days of operation
  - 24/7 use case is most vulnerable to memory leaks
- **Likelihood**: Medium (memory leaks common in long-running Android apps)
- **Current Mitigation**:
  - Architecture uses lifecycle-aware components (`viewModelScope`)
  - Coroutines structured concurrency
- **Required Mitigation**:
  - Use LeakCanary during development
  - Stress test: Run slideshow for 24 hours, monitor memory with Android Profiler
  - Verify no lifecycle leaks (Activity/Fragment references in coroutines)
- **Effort**: Small (2-3 days for stress testing)

**⚠️ Concern 2: Coil Cache Size Not Validated**
- **Risk Level**: 🟢 **MEDIUM**
- **Severity**: P2 (Monitor)
- **Impact**:
  - 100MB in-memory cache may be too small (photos evicted before slideshow loops)
  - Or too large (leaves less headroom for app overhead)
- **Likelihood**: Low (100MB is reasonable for 6-7 cached photos)
- **Current Mitigation**: 100MB in-memory cache (design decision)
- **Required Mitigation**:
  - Monitor cache hit rate during testing
  - Adjust cache size based on profiling (may increase to 150MB or decrease to 75MB)
- **Effort**: Trivial (configuration change)

**Recommendations**:
- [ ] **MANDATORY**: Use LeakCanary during development (1 day setup)
- [ ] **MANDATORY**: 24-hour stress test with memory profiling (2-3 days)
- [ ] **RECOMMENDED**: Monitor Coil cache hit rate, adjust size if needed (ongoing)

---

### 3.4 Performance - Battery Life (<5% drain/hour NFR)

**Target NFR**: Battery drain < 5% per hour during active display

**Architecture Coverage**: ⚠️ **PARTIAL**

**Assessment**:

The PRD specifies <5% battery drain per hour, but this is for a 24/7 photo frame with screen on. This NFR is **nearly impossible** for a backlit screen.

**Battery Drain Analysis**:
- **Screen on (100% brightness)**: ~15-25% per hour (varies by tablet)
- **Screen on (auto brightness, ~50%)**: ~8-15% per hour
- **App CPU usage**: <1% per hour (mostly idle, occasional photo load)
- **Network usage**: <1% per hour (periodic SMB reads)

**NFR Reality Check**:
- ✅ <5% drain/hour is achievable if screen is OFF (sleep mode between scheduled hours)
- ❌ <5% drain/hour is NOT achievable if screen is ON (screen dominates battery drain)

**PRD Context** (from requirements):
- Automated scheduling (8am-10pm), implying screen off outside scheduled hours
- "Set it and forget it" operation, implying plugged in

**Concerns**:

**⚠️ Concern 1: NFR Ambiguity - Screen On vs. Sleep Mode**
- **Risk Level**: 🟡 **HIGH**
- **Severity**: P1 (Clarify NFR)
- **Impact**:
  - If NFR means "screen on", it's unachievable (physics limitation)
  - If NFR means "sleep mode", it's achievable
  - Misinterpretation may cause wasted optimization effort
- **Likelihood**: High (NFR is ambiguous)
- **Current Mitigation**: None (NFR not clarified)
- **Required Mitigation**:
  - **Clarify NFR with product team**:
    - Option A: <5% drain/hour during sleep mode (screen off) ✅ Achievable
    - Option B: <5% drain/hour during active display (screen on) ❌ Impossible
    - Option C: Assume device plugged in, remove battery NFR ✅ Realistic
  - Document assumption: Device should be plugged in for 24/7 use
- **Effort**: No implementation effort (clarification only)

**⚠️ Concern 2: WAKE_LOCK Battery Impact**
- **Risk Level**: 🟢 **MEDIUM**
- **Severity**: P2 (Document)
- **Impact**:
  - WAKE_LOCK prevents device from sleeping (necessary for photo frame)
  - Battery drain is expected behavior, but users should be warned
- **Likelihood**: High (core use case)
- **Current Mitigation**: None
- **Required Mitigation**:
  - Add UI warning: "For best experience, keep device plugged in"
  - Detect if device is unplugged during slideshow, show low battery warning
- **Effort**: Small (1 day for battery check + UI warning)

**⚠️ Concern 3: WorkManager Battery Efficiency Not Validated**
- **Risk Level**: 🟢 **MEDIUM**
- **Severity**: P2 (Validate)
- **Impact**:
  - WorkManager is battery-efficient, but periodic checks (every 15min) may add ~1% drain/hour
  - Acceptable overhead, but should be validated
- **Likelihood**: Low (WorkManager is optimized)
- **Current Mitigation**: WorkManager battery optimization (architecture decision)
- **Required Mitigation**:
  - Measure battery drain during sleep mode (screen off, WorkManager active)
  - Validate <5% drain/hour during sleep mode
- **Effort**: Small (included in battery profiling)

**Recommendations**:
- [ ] **MANDATORY**: Clarify battery NFR with product team (screen on vs. sleep mode)
- [ ] **MANDATORY**: Add UI warning for unplugged device (1 day)
- [ ] **RECOMMENDED**: Validate battery drain during sleep mode (included in profiling)
- [ ] Document assumption: Device should be plugged in for 24/7 use

---

### 3.5 Performance - Startup Time

**Target NFR**: Cold start < 3s, Warm start < 1s (from NFR checklist)

**Architecture Coverage**: ⚠️ **PARTIAL**

**Assessment**:

The architecture does not explicitly address startup time, but it's achievable with standard Android optimizations.

**Startup Bottlenecks**:
1. **Hilt initialization**: ~200-300ms (standard overhead)
2. **Compose layout**: ~100-200ms (first frame)
3. **SMB library initialization**: ~50-100ms (jcifs-ng setup)
4. **DataStore read (credentials)**: ~50-100ms (first read)
5. **First photo load**: ~500-2000ms (network I/O)
6. **Total estimated**: ~900-2700ms (within 3s target)

**Concerns**:

**⚠️ Concern 1: SMB Directory Scan May Delay Startup**
- **Risk Level**: 🟡 **HIGH**
- **Severity**: P1 (Address)
- **Impact**:
  - Scanning 10,000 photos may take 10-30 seconds (blocks startup)
  - Poor first-launch experience
- **Likelihood**: High (power users may have large libraries)
- **Current Mitigation**: ADR Risk 3 mentions this issue
- **Required Mitigation**:
  - Show splash screen with progress indicator during scan
  - Cache photo list in DataStore (avoid rescanning on every start)
  - Implement incremental scan (start slideshow with first batch, scan rest in background)
- **Effort**: Small (2-3 days)

**Recommendations**:
- [ ] **MANDATORY**: Implement incremental SMB scan with progress indicator (2-3 days)
- [ ] **RECOMMENDED**: Cache photo list to avoid rescanning on every start (1-2 days)

---

## 4. Concurrency & Thread Safety Review

**Reviewed Against**: `.claude/CONCURRENCY_GUIDELINES.md`

### 4.1 Concurrency Design

**Architecture Coverage**: ✅ **GOOD**

**Assessment**:

The architecture specifies:
- **Coroutines** for structured concurrency (Dispatchers.IO for I/O, Dispatchers.Main for UI)
- **Single Mutex** for photo buffer management
- **@Immutable data classes** to prevent accidental mutations
- **StateFlow** for reactive UI updates (thread-safe)

**Design Analysis**:
- ✅ Clear dispatcher usage (I/O for SMB, Main for UI)
- ✅ Single Mutex simplifies synchronization (low contention expected)
- ✅ Immutable data classes prevent shared mutable state issues
- ✅ StateFlow is thread-safe (no manual synchronization needed)

**Concerns**:

**⚠️ Concern 1: Photo Buffer Mutex Contention**
- **Risk Level**: 🟢 **MEDIUM**
- **Severity**: P2 (Monitor)
- **Impact**:
  - Single Mutex for buffer updates may become bottleneck if multiple operations conflict
  - User swipe + auto-advance may race for buffer access
- **Likelihood**: Low (slideshow use case has low concurrent access)
- **Current Mitigation**: Single Mutex with short critical sections
- **Required Mitigation**:
  - Keep critical sections short (only buffer updates, no I/O inside mutex)
  - Monitor mutex wait times during profiling
  - If contention found, consider read-write lock or atomic operations
- **Effort**: Small (monitor during profiling)

**⚠️ Concern 2: Coil Thread Safety**
- **Risk Level**: 🟢 **MEDIUM**
- **Severity**: P2 (Validate)
- **Impact**:
  - Coil image loading from multiple coroutines (buffer pre-loading + current display)
  - Coil is thread-safe, but should verify no race conditions
- **Likelihood**: Low (Coil is battle-tested)
- **Current Mitigation**: Coil internal thread safety
- **Required Mitigation**:
  - Test concurrent Coil loads (buffer pre-loading + user navigation)
  - Verify no duplicate requests for same photo
- **Effort**: Small (included in testing)

**⚠️ Concern 3: SMB Connection Thread Safety**
- **Risk Level**: 🟡 **HIGH**
- **Severity**: P1 (Validate)
- **Impact**:
  - jcifs-ng `SmbFile` objects may not be thread-safe
  - Concurrent reads from same SmbFile instance may cause corruption or exceptions
- **Likelihood**: Medium (architecture doesn't specify connection management)
- **Current Mitigation**: Unknown (jcifs-ng thread safety not documented)
- **Required Mitigation**:
  - **Verify jcifs-ng thread safety**: Review documentation or source code
  - If not thread-safe: Protect SmbFile access with Mutex or create separate instances per coroutine
  - Test concurrent SMB reads (buffer pre-loading + navigation)
- **Effort**: Small (2-3 days for investigation + mitigation)

**Recommendations**:
- [ ] **MANDATORY**: Verify jcifs-ng thread safety and protect if needed (2-3 days)
- [ ] **RECOMMENDED**: Monitor Mutex contention during profiling (ongoing)
- [ ] **RECOMMENDED**: Test concurrent Coil loads (included in testing)

### 4.2 Common Anti-Patterns Check

**Reviewed Against**: CONCURRENCY_GUIDELINES.md anti-patterns

**Check 1: Mutable State Without Synchronization**
- ✅ **PASS**: Architecture specifies Mutex for buffer, StateFlow for UI state

**Check 2: Blocking Main Thread**
- ✅ **PASS**: Architecture specifies Dispatchers.IO for SMB reads, Dispatchers.Main for UI

**Check 3: Check-Then-Act Race Condition**
- ⚠️ **POTENTIAL ISSUE**: Photo buffer logic may have check-then-act patterns
  - Example: `if (buffer.size < 4) { buffer.add(loadPhoto()) }`
  - **Mitigation**: Use atomic operations or ensure Mutex protects entire operation

**Check 4: Shared ViewModel State Without Flow**
- ✅ **PASS**: Architecture specifies StateFlow for ViewModel state

**Recommendations**:
- [ ] **RECOMMENDED**: Review photo buffer logic for check-then-act patterns (2 days)
- [ ] Add code review checklist for concurrency anti-patterns (from CONCURRENCY_GUIDELINES.md)

---

## 5. Risk Assessment Summary

### Critical Risks (🔴 Must Fix Before Implementation)

#### Risk 1: Unencrypted SMB Credentials
- **Category**: Security - Data Protection
- **Description**: SMB username/password stored in DataStore Preferences without encryption. Any app with root access or backup extraction can read credentials.
- **Likelihood**: Medium
- **Impact**: High (credential compromise, network share access, privacy violation)
- **Current Mitigation**: Private app directory (insufficient)
- **Gaps**: No encryption, no Keystore usage
- **Required Action**: Implement Android Keystore encryption for credentials
- **Owner**: Developer (Security focus)
- **Estimated Effort**: 2-3 days

#### Risk 2: SMB Protocol Security Not Configured
- **Category**: Security - Network Security
- **Description**: jcifs-ng may negotiate insecure SMB 1.x protocol. No signing or encryption enforcement. Credentials vulnerable to MITM interception.
- **Likelihood**: Medium
- **Impact**: High (credential interception, MITM attacks, plaintext photo transfer)
- **Current Mitigation**: None
- **Gaps**: No minimum SMB version constraint, no signing config
- **Required Action**: Configure jcifs-ng to require SMB 2.0+, enable signing
- **Owner**: Developer (Network stack)
- **Estimated Effort**: 1 day

#### Risk 3: No PII Logging Policy
- **Category**: Security - Data Protection
- **Description**: No policy to prevent logging SMB credentials, photo paths, or other sensitive data. High risk of credential leakage in logs or crash reports.
- **Likelihood**: High
- **Impact**: High (credential exposure, privacy violation)
- **Current Mitigation**: None
- **Gaps**: No logging guidelines, no Crashlytics configuration
- **Required Action**: Define PII logging policy, configure Crashlytics exclusions
- **Owner**: Developer (All)
- **Estimated Effort**: 1-2 days

---

### High Risks (🟡 Should Fix Before Launch)

#### Risk 4: Performance Validation Deferred to Week 8
- **Category**: Performance - Response Time
- **Description**: <2s photo load NFR not validated until Week 8. If not met, requires rework with limited time remaining.
- **Likelihood**: Medium
- **Impact**: High (may delay MVP launch or require NFR relaxation)
- **Current Mitigation**: Optimization plan defined
- **Gaps**: Late validation, no early risk mitigation
- **Required Action**: Move performance profiling to Week 4-5
- **Owner**: Developer (Performance focus)
- **Estimated Effort**: 0 days (timeline adjustment)

#### Risk 5: SMB Path Validation Missing
- **Category**: Security - Input Validation
- **Description**: No validation for SMB paths (path traversal, malformed URLs). May allow access outside intended share or cause crashes.
- **Likelihood**: Medium
- **Impact**: Medium (unauthorized file access, crashes)
- **Current Mitigation**: jcifs-ng internal validation (unknown coverage)
- **Gaps**: No app-level validation before passing to library
- **Required Action**: Implement SMB path validation (reject `..`, validate format)
- **Owner**: Developer (Input handling)
- **Estimated Effort**: 2-3 days

#### Risk 6: Memory Leaks During 24/7 Operation
- **Category**: Performance - Memory Usage
- **Description**: Long-running coroutines or leaked references may cause memory leaks. App may crash after hours/days of 24/7 operation.
- **Likelihood**: Medium
- **Impact**: High (app unusable for primary use case)
- **Current Mitigation**: Lifecycle-aware components, structured concurrency
- **Gaps**: No leak detection during development, no 24-hour stress test
- **Required Action**: Use LeakCanary, run 24-hour stress test
- **Owner**: Developer (All)
- **Estimated Effort**: 2-3 days

#### Risk 7: Network Variability Not Handled
- **Category**: Performance - Reliability
- **Description**: WiFi interference, congestion, or weak signal may cause >2s loads or timeouts. No retry logic or fallback specified.
- **Likelihood**: High
- **Impact**: Medium (degraded user experience, slideshow stutter)
- **Current Mitigation**: 4-photo buffer provides some resilience
- **Gaps**: No timeout handling, no fallback to cache, no network quality monitoring
- **Required Action**: Implement timeout, retry, and fallback logic
- **Owner**: Developer (Network stack)
- **Estimated Effort**: 2-3 days

#### Risk 8: jcifs-ng Thread Safety Unknown
- **Category**: Concurrency - Thread Safety
- **Description**: jcifs-ng `SmbFile` objects may not be thread-safe. Concurrent reads (buffer pre-loading + navigation) may cause corruption.
- **Likelihood**: Medium
- **Impact**: High (data corruption, crashes)
- **Current Mitigation**: Unknown
- **Gaps**: jcifs-ng thread safety not verified
- **Required Action**: Verify jcifs-ng thread safety, add Mutex if needed
- **Owner**: Developer (Concurrency focus)
- **Estimated Effort**: 2-3 days

#### Risk 9: Battery NFR Ambiguity
- **Category**: Performance - Battery Life
- **Description**: <5% battery drain/hour NFR is ambiguous (screen on vs. sleep mode). Screen on is unachievable (~10-15% drain).
- **Likelihood**: High
- **Impact**: Medium (wasted effort if misinterpreted)
- **Current Mitigation**: None
- **Gaps**: NFR not clarified with product team
- **Required Action**: Clarify NFR (assume device plugged in or sleep mode only)
- **Owner**: Product team
- **Estimated Effort**: 0 days (clarification only)

---

### Medium Risks (🟢 Address If Time Permits)

#### Risk 10: SMB Signing Not Enforced
- **Category**: Security - Network Security
- **Description**: No SMB signing verification. MITM can tamper with packets (modify photos, inject errors).
- **Likelihood**: Low
- **Impact**: Medium (data integrity compromise)
- **Current Mitigation**: None
- **Gaps**: Signing not enabled
- **Required Action**: Enable SMB signing in jcifs-ng config
- **Owner**: Developer (Network stack)
- **Estimated Effort**: Small (included in SMB config)

#### Risk 11: No Root Detection
- **Category**: Security - Code Security
- **Description**: Rooted devices can bypass Android Keystore. Credentials more vulnerable.
- **Likelihood**: Medium
- **Impact**: Low (acceptable for MVP)
- **Current Mitigation**: None
- **Gaps**: No root detection
- **Required Action**: For Phase 2, implement root detection with user warning
- **Owner**: Developer (Security focus)
- **Estimated Effort**: 2-3 days

#### Risk 12: Coil Downsampling Not Verified
- **Category**: Performance - Memory Usage
- **Description**: If Coil doesn't downsample correctly, full-resolution bitmaps may cause OOM or slow rendering.
- **Likelihood**: Low
- **Impact**: High (OOM crash)
- **Current Mitigation**: Coil automatic downsampling
- **Gaps**: Configuration not verified
- **Required Action**: Verify Coil downsampling config with high-res photos
- **Owner**: Developer (Image loading)
- **Estimated Effort**: 1 day

---

### Low Risks (⚪ Monitor)

#### Risk 13: Photo Cache Not Encrypted
- **Category**: Security - Data Protection
- **Description**: Coil disk cache (512MB) stores photos unencrypted. Accessible via backup extraction.
- **Likelihood**: Low
- **Impact**: Medium (privacy violation)
- **Current Mitigation**: Private app directory
- **Gaps**: No encryption
- **Required Action**: For Phase 2, implement encrypted disk cache
- **Owner**: Developer (Phase 2)
- **Estimated Effort**: 5-7 days

---

## 6. Implementation Recommendations

### Priority Order

**Phase 1 - Address Critical Risks (Week 1-2)**:
1. Implement Android Keystore encryption for SMB credentials (2-3 days)
2. Configure jcifs-ng SMB security (min version SMB 2.0, signing) (1 day)
3. Define and enforce PII logging policy (1-2 days)
4. Configure Crashlytics to exclude sensitive fields (1 day)

**Phase 2 - Implement Core NFRs (Week 3-4)**:
5. Implement SMB path validation (2-3 days)
6. Implement SMB read timeout and fallback logic (2-3 days)
7. Verify jcifs-ng thread safety and protect if needed (2-3 days)
8. Setup LeakCanary for memory leak detection (1 day)

**Phase 3 - Early Performance Validation (Week 4-5)**:
9. Performance profiling on target hardware (2-3 days)
10. Validate <2s photo load NFR (included in profiling)
11. Verify Coil downsampling configuration (1 day)
12. Test concurrent operations (buffer pre-loading + navigation) (2 days)

**Phase 4 - Address High Risks (Week 5-7)**:
13. Implement incremental SMB scan with progress indicator (2-3 days)
14. Cache photo list in DataStore (1-2 days)
15. Add battery charging check and UI warning (1 day)
16. Run 24-hour stress test with memory profiling (2-3 days)

**Phase 5 - Optimization & Polish (Week 8+)**:
17. Enable R8 obfuscation for release builds (1 day)
18. Monitor network quality and warn user (2 days)
19. Adjust Coil cache size based on profiling (ongoing)

---

### Key Implementation Patterns

#### Pattern 1: Secure Credential Storage (Android Keystore)

**When to Use**: Storing sensitive credentials (SMB username/password)

**Example**:
```kotlin
class SecureCredentialStorage(private val context: Context) {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val keyAlias = "smb_credentials_key"

    private fun getOrCreateKey(): SecretKey {
        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .build()
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
        return keyStore.getKey(keyAlias, null) as SecretKey
    }

    suspend fun storeCredentials(username: String, password: String) {
        withContext(Dispatchers.IO) {
            val key = getOrCreateKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)

            val iv = cipher.iv
            val encryptedPassword = cipher.doFinal(password.toByteArray())

            // Store in DataStore
            context.dataStore.edit { prefs ->
                prefs[KEY_USERNAME] = username
                prefs[KEY_ENCRYPTED_PASSWORD] = Base64.encodeToString(encryptedPassword, Base64.DEFAULT)
                prefs[KEY_IV] = Base64.encodeToString(iv, Base64.DEFAULT)
            }
        }
    }

    suspend fun retrieveCredentials(): SmbCredentials? {
        return withContext(Dispatchers.IO) {
            val prefs = context.dataStore.data.first()
            val username = prefs[KEY_USERNAME] ?: return@withContext null
            val encryptedPassword = prefs[KEY_ENCRYPTED_PASSWORD] ?: return@withContext null
            val iv = prefs[KEY_IV] ?: return@withContext null

            val key = getOrCreateKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, Base64.decode(iv, Base64.DEFAULT)))

            val decryptedPassword = String(cipher.doFinal(Base64.decode(encryptedPassword, Base64.DEFAULT)))

            SmbCredentials(username, decryptedPassword)
        }
    }
}
```

**NFRs Addressed**: SEC-010 (data encrypted at rest), SEC-014 (secure credential storage)

---

#### Pattern 2: SMB Security Configuration

**When to Use**: Initializing jcifs-ng SMB client

**Example**:
```kotlin
object SmbClientConfig {
    fun initialize() {
        Properties().apply {
            // Require SMB 2.1 minimum (reject SMB 1.x)
            setProperty("jcifs.smb.client.minVersion", "SMB210")

            // Prefer SMB 3.1.1 for encryption
            setProperty("jcifs.smb.client.maxVersion", "SMB311")

            // Enable signing to prevent MITM tampering
            setProperty("jcifs.smb.client.signingPreferred", "true")

            // Enforce signing for authentication
            setProperty("jcifs.smb.client.ipcSigningEnforced", "true")

            // Set connection timeout
            setProperty("jcifs.smb.client.connTimeout", "30000")  // 30 seconds

            // Set response timeout
            setProperty("jcifs.smb.client.responseTimeout", "30000")  // 30 seconds

            jcifs.Config.setProperties(this)
        }
    }

    fun validateSmbVersion(smbFile: SmbFile): Result<Unit> {
        return try {
            val negotiatedVersion = smbFile.getNegotiatedProtocol()
            when {
                negotiatedVersion < SMB_PROTOCOL_VERSION_210 -> {
                    Result.failure(SecurityException(
                        "SMB 1.x detected. Please upgrade your server to SMB 2.0+ for security."
                    ))
                }
                negotiatedVersion < SMB_PROTOCOL_VERSION_300 -> {
                    // SMB 2.x acceptable but warn about lack of encryption
                    Log.w(TAG, "SMB 2.x connection (no encryption). SMB 3.0+ recommended.")
                    Result.success(Unit)
                }
                else -> {
                    // SMB 3.x with encryption
                    Log.i(TAG, "Secure SMB 3.x connection established.")
                    Result.success(Unit)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

**NFRs Addressed**: SEC-030 (network security), SEC-033 (no cleartext traffic)

---

#### Pattern 3: Input Validation (SMB Paths)

**When to Use**: Validating user-provided SMB URLs

**Example**:
```kotlin
object SmbPathValidator {
    private val SMB_URL_REGEX = Regex("^smb://[a-zA-Z0-9.-]+(/[a-zA-Z0-9_.-]+)*/?$")
    private const val MAX_PATH_LENGTH = 512

    fun validate(path: String): Result<String> {
        return when {
            path.isBlank() -> {
                Result.failure(InvalidPathException("SMB path cannot be empty"))
            }
            path.contains("..") -> {
                Result.failure(InvalidPathException("Path traversal not allowed (.. detected)"))
            }
            path.length > MAX_PATH_LENGTH -> {
                Result.failure(InvalidPathException("Path too long (max $MAX_PATH_LENGTH characters)"))
            }
            !path.startsWith("smb://", ignoreCase = true) -> {
                Result.failure(InvalidPathException("Path must start with smb://"))
            }
            !SMB_URL_REGEX.matches(path) -> {
                Result.failure(InvalidPathException("Invalid SMB URL format"))
            }
            else -> {
                Result.success(path)
            }
        }
    }

    fun sanitizeForLogging(path: String): String {
        // Remove username from URL if embedded: smb://user:pass@server/share -> smb://***@server/share
        return path.replace(Regex("smb://[^@]+@"), "smb://***@")
            .replace(Regex("/[^/]+$"), "/***")  // Mask last path component
    }
}
```

**NFRs Addressed**: SEC-020 (input validation), SEC-023 (command injection), SEC-024 (path traversal)

---

#### Pattern 4: PII-Safe Logging

**When to Use**: All logging that may involve sensitive data

**Example**:
```kotlin
object SafeLogger {
    private const val TAG = "PhotoFrame"

    // NEVER log credentials
    fun logConnectionAttempt(serverUrl: String) {
        val sanitized = SmbPathValidator.sanitizeForLogging(serverUrl)
        Log.d(TAG, "Attempting SMB connection to: $sanitized")
    }

    // NEVER log full photo paths (may contain sensitive folder names)
    fun logPhotoLoaded(photoPath: String, loadTimeMs: Long) {
        val filename = photoPath.substringAfterLast('/')
        Log.d(TAG, "Photo loaded: $filename (${loadTimeMs}ms)")
    }

    // Mask sensitive error details
    fun logConnectionError(error: Throwable, serverUrl: String) {
        val sanitized = SmbPathValidator.sanitizeForLogging(serverUrl)
        when (error) {
            is AuthenticationException -> {
                // Don't log credentials in error
                Log.e(TAG, "Authentication failed for $sanitized (check username/password)")
            }
            is IOException -> {
                Log.e(TAG, "Connection failed for $sanitized: ${error.message}")
            }
            else -> {
                Log.e(TAG, "Error connecting to $sanitized", error)
            }
        }
    }
}

// Configure Crashlytics to exclude sensitive data
class CrashlyticsConfig {
    fun initialize() {
        FirebaseCrashlytics.getInstance().apply {
            // Don't send user identifiable information
            setCustomKey("smb_server_masked", true)

            // Set up custom keys that are safe to log
            setCustomKey("photo_count", 0)
            setCustomKey("display_interval_sec", 0)

            // Add log filter to redact PII
            setCrashlyticsCollectionEnabled(true)
        }
    }
}
```

**NFRs Addressed**: SEC-012 (no PII in logs), SEC-013 (no sensitive data in crash reports)

---

#### Pattern 5: Thread-Safe SMB Connection Management

**When to Use**: Concurrent SMB reads (buffer pre-loading + user navigation)

**Example**:
```kotlin
class SmbConnectionPool(
    private val credentials: SmbCredentials,
    private val maxConnections: Int = 3
) {
    private val mutex = Mutex()
    private val activeConnections = mutableMapOf<String, SmbFile>()

    suspend fun <T> withConnection(url: String, block: suspend (SmbFile) -> T): T {
        return mutex.withLock {
            val connection = activeConnections.getOrPut(url) {
                createConnection(url)
            }

            try {
                block(connection)
            } catch (e: Exception) {
                // Connection may be stale, remove and retry
                activeConnections.remove(url)
                throw e
            }
        }
    }

    private fun createConnection(url: String): SmbFile {
        val ntlmAuth = NtlmPasswordAuthenticator(
            null,  // domain (null for most cases)
            credentials.username,
            credentials.password
        )
        return SmbFile(url, ntlmAuth)
    }

    suspend fun close() {
        mutex.withLock {
            activeConnections.values.forEach { it.close() }
            activeConnections.clear()
        }
    }
}

// Usage in repository
class SmbPhotoRepository(
    private val connectionPool: SmbConnectionPool,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun loadPhoto(photoPath: String): Result<ByteArray> = withContext(ioDispatcher) {
        try {
            connectionPool.withConnection(photoPath) { smbFile ->
                smbFile.inputStream.use { it.readBytes() }
            }
            Result.success(photoData)
        } catch (e: Exception) {
            SafeLogger.logConnectionError(e, photoPath)
            Result.failure(e)
        }
    }
}
```

**NFRs Addressed**: REL-001 (error handling), PERF-001 (response time), thread safety

---

### Code Review Checklist

**CRITICAL: Concurrency & Thread Safety** (ALL reviews must include):
- [ ] Are SMB credentials encrypted with Android Keystore?
- [ ] Is jcifs-ng configured to require SMB 2.0+ with signing?
- [ ] Are photo paths sanitized before logging?
- [ ] Is Crashlytics configured to exclude sensitive fields?
- [ ] Are mutable variables properly synchronized (Mutex, Atomic types)?
- [ ] Are collections thread-safe (ConcurrentHashMap) or protected?
- [ ] Are state updates atomic (no read-modify-write races)?
- [ ] Are appropriate dispatchers used (IO, Main, Default)?
- [ ] Is `delay()` used instead of `Thread.sleep()`?
- [ ] Are blocking calls wrapped in `withContext(Dispatchers.IO)`?
- [ ] Is cancellation handled correctly (don't catch `CancellationException`)?
- [ ] Are there any check-then-act patterns without synchronization?
- [ ] Are StateFlow/SharedFlow used correctly for reactive state?
- [ ] Is shared mutable state minimized?

**Security-Specific Checks**:
- [ ] No credentials logged in plaintext
- [ ] SMB paths validated before use (reject `..`, validate format)
- [ ] No hardcoded credentials or API keys
- [ ] Input validation for all user inputs (SMB URL, username, password, intervals)
- [ ] Error messages don't expose sensitive information

**Performance-Specific Checks**:
- [ ] SMB reads have timeout configured (e.g., 5s)
- [ ] Photos downsampled to screen resolution (2560x1600)
- [ ] No blocking operations on main thread
- [ ] Coroutines use appropriate dispatchers
- [ ] Memory leaks checked with LeakCanary

---

### Testing Strategy

**Unit Tests Must Cover**:
- Credential encryption/decryption (mock Keystore)
- SMB path validation (valid/invalid inputs)
- Input validation (intervals, schedule times)
- Photo buffer management (add/remove/concurrent access)
- Error handling (network failures, authentication errors)

**Integration Tests Must Cover**:
- SMB connection with real server (SMB 2.x, SMB 3.x)
- Photo loading with timeout and retry
- Concurrent operations (buffer pre-loading + user navigation)
- 24-hour stress test (memory leaks, performance degradation)

**Performance Tests Must Cover**:
- Photo load time <2s (P95) on target hardware
- Transition frame rate 60fps
- Memory usage <300MB over 24 hours
- Battery drain during sleep mode <5% per hour

**Security Tests Must Cover**:
- Credential storage encrypted (verify DataStore contents)
- SMB 1.x connections rejected
- Path traversal attempts blocked
- No credentials in logs or crash reports

---

## 7. Consensus Summary

**Status**: Awaiting teammate feedback

This section will be updated after reviewing assessments from:
- Senior Dev 2 (Testability & Maintainability)
- Senior Dev 3 (Scalability & Reliability)

### Feedback Received
(To be filled after team review)

### Consensus Reached
(To be filled after team discussion)

### Updated Risk Priorities (After Debate)
(To be filled after team discussion)

### Final Recommendation
(To be filled after consensus)

---

## 8. NFR Acceptance Criteria

These acceptance criteria should be added to the PRD and validated during implementation:

### Security

#### Credential Protection
- [ ] SMB username/password encrypted using Android Keystore (AES-256-GCM)
- [ ] Encrypted credentials stored in DataStore (not plaintext)
- [ ] Credentials never logged in plaintext (masked in all logs)
- [ ] Crashlytics configured to exclude credential fields

#### Network Security
- [ ] jcifs-ng configured to require SMB 2.0+ (reject SMB 1.x)
- [ ] SMB signing enabled and verified
- [ ] Connection rejected if SMB 1.x detected (with user-friendly error)
- [ ] Clear error message if authentication fails

#### Input Validation
- [ ] SMB path validated (reject `..`, validate format, max length 512)
- [ ] All user inputs validated (display interval 3-60, schedule times valid)
- [ ] Invalid inputs show clear error messages
- [ ] Username/password special characters tested and supported

#### Code Security
- [ ] R8 obfuscation enabled for release builds
- [ ] No hardcoded credentials in code
- [ ] ProGuard rules configured correctly

---

### Performance

#### Photo Load Time
- [ ] Photo load time <2s (P95) on target hardware (validated in Week 4-5)
- [ ] SMB read timeout configured (5s max)
- [ ] Fallback to cached photos if network fails
- [ ] Loading indicator shown if photo takes >1s

#### Transitions
- [ ] All transitions render at 60fps (measured with GPU profiler)
- [ ] No frame drops during transitions (validated with frame timing graph)
- [ ] Coil downsampling configured for screen resolution (2560x1600)

#### Memory Usage
- [ ] Total memory usage <300MB (measured over 24-hour stress test)
- [ ] No memory leaks detected (validated with LeakCanary)
- [ ] Coil cache hit rate >70% (monitored during testing)

#### Startup Time
- [ ] Cold start <3s (first photo displayed)
- [ ] Warm start <1s
- [ ] SMB directory scan incremental (start slideshow with first batch)
- [ ] Progress indicator shown during initial scan

#### Battery Life
- [ ] Battery drain <5% per hour during sleep mode (screen off)
- [ ] UI warning shown if device unplugged during slideshow
- [ ] Documentation states device should be plugged in for 24/7 use

---

### Concurrency & Thread Safety

- [ ] Photo buffer protected by Mutex (all access synchronized)
- [ ] StateFlow used for UI state (thread-safe)
- [ ] All SMB reads run on Dispatchers.IO (not main thread)
- [ ] jcifs-ng thread safety verified (or Mutex added if not thread-safe)
- [ ] No check-then-act patterns without synchronization
- [ ] 24-hour stress test passes without crashes

---

## 9. Validation Criteria

How to verify NFRs are met:

### Security Validation

**Credential Encryption**:
1. Install app, configure SMB credentials
2. Use Android Studio Device File Explorer to inspect DataStore file
3. Verify credentials are encrypted (not plaintext)
4. Use `adb backup` to extract app data, verify credentials not readable

**Network Security**:
1. Configure app to connect to SMB 1.x server
2. Verify app rejects connection with error message
3. Use Wireshark to capture SMB packets, verify signing enabled (SMB 2.x)
4. Verify no plaintext credentials in packet capture

**Logging**:
1. Review all log statements in codebase
2. Verify no log contains credentials (use grep for username/password)
3. Trigger authentication error, verify error message doesn't expose credentials
4. Review Crashlytics dashboard, verify no sensitive data in crash reports

---

### Performance Validation

**Photo Load Time**:
1. Setup test environment (tablet + SMB share on same LAN)
2. Use Android Profiler to measure time from `nextPhoto()` to bitmap rendered
3. Repeat 100 times, calculate P50, P95, P99
4. Verify P95 <2s

**Memory Usage**:
1. Start slideshow, run for 24 hours
2. Monitor memory usage with Android Profiler (take heap dumps every hour)
3. Verify no upward trend (memory leak)
4. Verify peak memory <300MB

**Battery Life**:
1. Configure schedule (e.g., 8am-10pm), let app run overnight
2. Monitor battery level at 8pm (before sleep) and 8am (after sleep)
3. Calculate drain rate: (battery_8pm - battery_8am) / 12 hours
4. Verify <5% per hour during sleep mode

---

### Concurrency Validation

**Thread Safety**:
1. Enable StrictMode in debug builds (detect main thread I/O)
2. Use ThreadSanitizer or similar tool (if available)
3. Run concurrent operations (rapid user swipes + auto-advance)
4. Verify no crashes, no data corruption

**Memory Leaks**:
1. Install LeakCanary in debug builds
2. Run slideshow for 1 hour with frequent navigation
3. Check LeakCanary for detected leaks
4. Fix all leaks before release

---

## 10. Open Questions

**For Product Team**:
1. **Battery NFR Clarification**: Does <5% drain/hour apply to screen-on (impossible) or sleep mode (achievable)? Recommend changing to "Device should be plugged in for 24/7 use" or "Battery drain <5%/hour during sleep mode only".

**For Architecture Team**:
2. **jcifs-ng Thread Safety**: Need to verify if jcifs-ng `SmbFile` objects are thread-safe. If not, must add Mutex or create separate instances per coroutine.

**For Development Team**:
3. **SMB Server Testing**: Need access to multiple SMB server types for testing (Windows SMB, Samba 4.x, Synology NAS, QNAP NAS). Can we get test hardware?

---

## 11. Summary

This architecture has **solid performance foundations** (Coil, coroutines, 4-photo buffer) but **critical security gaps** that must be addressed before implementation.

**Strengths**:
- ✅ Proven libraries (Coil, jcifs-ng, WorkManager)
- ✅ Reasonable performance strategy (profile first, optimize later)
- ✅ Clear concurrency design (coroutines, Mutex, StateFlow)
- ✅ Memory-efficient buffer strategy (4 photos, downsampling)

**Critical Gaps**:
- ❌ Credentials stored unencrypted (Android Keystore required)
- ❌ SMB security not configured (SMB 1.x rejection, signing, encryption)
- ❌ No PII logging policy (credential leakage risk)
- ❌ Late performance validation (Week 8 too late)

**Go/No-Go Decision**:
**⚠️ CONDITIONAL GO** - Can proceed to implementation IF:
1. Android Keystore encryption implemented (non-negotiable)
2. SMB security configured (non-negotiable)
3. PII logging policy defined and enforced (non-negotiable)
4. Performance profiling moved to Week 4-5 (recommended)

Without addressing these critical security concerns, this app should NOT be released. Users' network credentials and photos are at risk.

---

**End of Assessment**

**Next Steps**:
1. Share assessment with Senior Dev 2 and Senior Dev 3
2. Review their assessments and provide feedback
3. Engage in debate on risk priorities
4. Reach consensus on critical issues
5. Update assessment with team feedback
6. Finalize recommendation
