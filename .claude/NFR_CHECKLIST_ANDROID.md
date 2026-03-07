# Non-Functional Requirements (NFR) Checklist - Android/Native Mobile

This checklist defines comprehensive non-functional requirements for Android and native mobile applications. All features must address applicable NFRs before implementation.

## How to Use This Checklist

- **Review**: Assess each NFR category for relevance to your feature
- **Mark**: ✅ Addressed | ⚠️ Partial | ❌ Not Addressed | 🔍 Needs Investigation | N/A Not Applicable
- **Document**: For each item, document how it's addressed in the architecture
- **Justify**: If marking N/A, explain why it doesn't apply

---

## 1. Security

### 1.1 Authentication & Authorization

| NFR | Description | Priority |
|-----|-------------|----------|
| SEC-001 | User authentication required for sensitive operations | Critical |
| SEC-002 | Session management with appropriate timeout | High |
| SEC-003 | Token refresh handled gracefully | High |
| SEC-004 | Biometric authentication support (where applicable) | Medium |
| SEC-005 | Multi-factor authentication support (where applicable) | Medium |

### 1.2 Data Protection

| NFR | Description | Priority |
|-----|-------------|----------|
| SEC-010 | PII data encrypted at rest | Critical |
| SEC-011 | PII data encrypted in transit (TLS 1.2+) | Critical |
| SEC-012 | No PII in logs | Critical |
| SEC-013 | No sensitive data in crash reports | Critical |
| SEC-014 | Secure storage for credentials and tokens | Critical |
| SEC-015 | Payment data complies with PCI-DSS | Critical |
| SEC-016 | Data minimization - only collect necessary data | High |

### 1.3 Input Validation

| NFR | Description | Priority |
|-----|-------------|----------|
| SEC-020 | All user input validated | Critical |
| SEC-021 | SQL injection prevention | Critical |
| SEC-022 | XSS prevention in WebViews | Critical |
| SEC-023 | Command injection prevention | Critical |
| SEC-024 | Path traversal prevention | High |
| SEC-025 | Buffer overflow prevention | High |

### 1.4 Network Security

| NFR | Description | Priority |
|-----|-------------|----------|
| SEC-030 | HTTPS only for all network calls | Critical |
| SEC-031 | Certificate pinning for critical APIs | High |
| SEC-032 | Mutual TLS (mTLS) for sensitive operations | Medium |
| SEC-033 | No cleartext traffic allowed | Critical |
| SEC-034 | API keys not hardcoded | Critical |

### 1.5 Code Security

| NFR | Description | Priority |
|-----|-------------|----------|
| SEC-040 | No hardcoded secrets or credentials | Critical |
| SEC-041 | ProGuard/R8 obfuscation enabled | High |
| SEC-042 | Root/jailbreak detection (where needed) | Medium |
| SEC-043 | No debug code in production | High |
| SEC-044 | Secure coding practices followed | High |

---

## 2. Performance

### 2.1 Response Time

| NFR | Description | Priority |
|-----|-------------|----------|
| PERF-001 | Screen load time < 2 seconds (on good network) | High |
| PERF-002 | API response handling < 500ms | High |
| PERF-003 | UI transitions smooth (60 FPS) | High |
| PERF-004 | No ANR (Application Not Responding) | Critical |
| PERF-005 | Cold start time < 3 seconds | Medium |
| PERF-006 | Warm start time < 1 second | Medium |

### 2.2 Resource Usage

| NFR | Description | Priority |
|-----|-------------|----------|
| PERF-010 | Memory usage within acceptable limits | High |
| PERF-011 | No memory leaks | Critical |
| PERF-012 | Efficient bitmap handling | High |
| PERF-013 | Lazy loading for large lists | High |
| PERF-014 | Pagination for large data sets | High |
| PERF-015 | Image caching implemented | Medium |

### 2.3 Battery Efficiency

| NFR | Description | Priority |
|-----|-------------|----------|
| PERF-020 | Minimal battery drain | High |
| PERF-021 | Background work optimized | High |
| PERF-022 | Wake locks used judiciously | High |
| PERF-023 | Location updates efficient | High |
| PERF-024 | No unnecessary polling | High |

### 2.4 Network Efficiency

| NFR | Description | Priority |
|-----|-------------|----------|
| PERF-030 | Minimize network calls | High |
| PERF-031 | Request batching where possible | Medium |
| PERF-032 | Efficient JSON parsing | Medium |
| PERF-033 | Image compression | Medium |
| PERF-034 | Adaptive quality based on network | Medium |

### 2.5 Database Performance

| NFR | Description | Priority |
|-----|-------------|----------|
| PERF-040 | Database queries optimized | High |
| PERF-041 | Proper indexing | High |
| PERF-042 | No blocking database operations on main thread | Critical |
| PERF-043 | Database migrations tested | High |

---

## 3. Reliability & Stability

### 3.1 Error Handling

| NFR | Description | Priority |
|-----|-------------|----------|
| REL-001 | All errors handled gracefully | Critical |
| REL-002 | User-friendly error messages | High |
| REL-003 | Retry logic for transient failures | High |
| REL-004 | Circuit breaker for failing services | Medium |
| REL-005 | Fallback mechanisms defined | High |

### 3.2 Crash Prevention

| NFR | Description | Priority |
|-----|-------------|----------|
| REL-010 | Crash rate < 0.1% | Critical |
| REL-011 | No unhandled exceptions | Critical |
| REL-012 | Defensive programming practices | High |
| REL-013 | Null safety (Kotlin) | High |

### 3.3 Offline Support

| NFR | Description | Priority |
|-----|-------------|----------|
| REL-020 | Graceful degradation when offline | High |
| REL-021 | Cached data available offline | Medium |
| REL-022 | Offline queue for user actions | Medium |
| REL-023 | Clear offline indicators | High |

### 3.4 Data Integrity

| NFR | Description | Priority |
|-----|-------------|----------|
| REL-030 | Data consistency maintained | Critical |
| REL-031 | Transaction handling for multi-step operations | High |
| REL-032 | Data validation before persistence | High |
| REL-033 | Sync conflict resolution strategy | Medium |

---

## 4. Usability

### 4.1 User Experience

| NFR | Description | Priority |
|-----|-------------|----------|
| UX-001 | Intuitive UI following Material Design | High |
| UX-002 | Consistent navigation patterns | High |
| UX-003 | Loading indicators for async operations | High |
| UX-004 | Optimistic UI updates | Medium |
| UX-005 | Clear call-to-action buttons | High |

### 4.2 Responsiveness

| NFR | Description | Priority |
|-----|-------------|----------|
| UX-010 | UI responds to user input immediately | High |
| UX-011 | No frozen UI during operations | Critical |
| UX-012 | Progress indicators for long operations | High |
| UX-013 | Cancellable long-running operations | Medium |

### 4.3 Feedback & Messaging

| NFR | Description | Priority |
|-----|-------------|----------|
| UX-020 | Clear success/error messages | High |
| UX-021 | Actionable error messages | High |
| UX-022 | Confirmation for destructive actions | High |
| UX-023 | Toast/Snackbar for transient messages | Medium |

---

## 5. Accessibility

### 5.1 Screen Reader Support

| NFR | Description | Priority |
|-----|-------------|----------|
| A11Y-001 | All UI elements have content descriptions | Critical |
| A11Y-002 | Logical reading order | High |
| A11Y-003 | TalkBack fully supported | Critical |
| A11Y-004 | No "click here" or generic descriptions | High |

### 5.2 Visual Accessibility

| NFR | Description | Priority |
|-----|-------------|----------|
| A11Y-010 | Color contrast ratio ≥ 4.5:1 | High |
| A11Y-011 | Don't rely solely on color | High |
| A11Y-012 | Text scalable (support large text) | High |
| A11Y-013 | Minimum touch target size 48dp | High |

### 5.3 Interaction Accessibility

| NFR | Description | Priority |
|-----|-------------|----------|
| A11Y-020 | Keyboard navigation support | Medium |
| A11Y-021 | Focus indicators visible | High |
| A11Y-022 | Gesture alternatives provided | Medium |
| A11Y-023 | No time-limited interactions (or adjustable) | Medium |

---

## 6. Internationalization (i18n) & Localization (l10n)

### 6.1 Language Support

| NFR | Description | Priority |
|-----|-------------|----------|
| I18N-001 | All strings externalized | Critical |
| I18N-002 | Support for multiple languages | High |
| I18N-003 | RTL layout support | High |
| I18N-004 | Proper pluralization | High |
| I18N-005 | Date/time formatting per locale | High |

### 6.2 Regional Support

| NFR | Description | Priority |
|-----|-------------|----------|
| I18N-010 | Currency formatting per locale | High |
| I18N-011 | Number formatting per locale | High |
| I18N-012 | Address format per locale | Medium |
| I18N-013 | Phone number format per locale | Medium |

---

## 7. Testability

### 7.1 Unit Testing

| NFR | Description | Priority |
|-----|-------------|----------|
| TEST-001 | Unit test coverage ≥ 80% | High |
| TEST-002 | All business logic unit tested | High |
| TEST-003 | ViewModels fully unit tested | High |
| TEST-004 | UseCases/Repositories unit tested | High |

### 7.2 Test Design

| NFR | Description | Priority |
|-----|-------------|----------|
| TEST-010 | Components are testable in isolation | High |
| TEST-011 | Dependencies injectable for mocking | High |
| TEST-012 | No direct Android framework dependencies in logic | High |
| TEST-013 | Test doubles provided for complex dependencies | Medium |

### 7.3 Integration & UI Testing

| NFR | Description | Priority |
|-----|-------------|----------|
| TEST-020 | Integration tests for critical paths | High |
| TEST-021 | UI tests for main user journeys | Medium |
| TEST-022 | Compose UI tests for complex components | Medium |

---

## 8. Maintainability

### 8.1 Code Quality

| NFR | Description | Priority |
|-----|-------------|----------|
| MAINT-001 | Code follows Kotlin style guide | High |
| MAINT-002 | Clear naming conventions | High |
| MAINT-003 | Functions < 50 lines | Medium |
| MAINT-004 | Classes < 500 lines | Medium |
| MAINT-005 | Cyclomatic complexity < 10 | Medium |
| MAINT-006 | No code duplication (DRY) | High |

### 8.2 Architecture

| NFR | Description | Priority |
|-----|-------------|----------|
| MAINT-010 | Clear separation of concerns | High |
| MAINT-011 | SOLID principles followed | High |
| MAINT-012 | Consistent architecture patterns | High |
| MAINT-013 | Appropriate abstraction levels | Medium |

### 8.3 Documentation

| NFR | Description | Priority |
|-----|-------------|----------|
| MAINT-020 | Public APIs have KDoc comments | High |
| MAINT-021 | Complex logic documented | High |
| MAINT-022 | Architecture decisions documented (ADR) | High |
| MAINT-023 | README updated | Medium |

### 8.4 Dependencies

| NFR | Description | Priority |
|-----|-------------|----------|
| MAINT-030 | Minimize external dependencies | Medium |
| MAINT-031 | Keep dependencies up to date | Medium |
| MAINT-032 | No deprecated API usage | High |

---

## 9. Scalability

### 9.1 Data Scalability

| NFR | Description | Priority |
|-----|-------------|----------|
| SCALE-001 | Handle large data sets efficiently | High |
| SCALE-002 | Pagination for lists | High |
| SCALE-003 | Infinite scroll where appropriate | Medium |
| SCALE-004 | Data pruning strategies | Medium |

### 9.2 Concurrent Users

| NFR | Description | Priority |
|-----|-------------|----------|
| SCALE-010 | Handle concurrent user sessions | Medium |
| SCALE-011 | No race conditions | High |
| SCALE-012 | Thread-safe components | High |

---

## 10. Privacy & Compliance

### 10.1 Data Privacy

| NFR | Description | Priority |
|-----|-------------|----------|
| PRIV-001 | GDPR compliance | Critical |
| PRIV-002 | CCPA compliance | Critical |
| PRIV-003 | User consent for data collection | Critical |
| PRIV-004 | Data retention policies followed | High |
| PRIV-005 | User data deletion capability | High |

### 10.2 Tracking & Analytics

| NFR | Description | Priority |
|-----|-------------|----------|
| PRIV-010 | Analytics opt-out supported | High |
| PRIV-011 | No tracking without consent | Critical |
| PRIV-012 | Analytics data anonymized | High |

---

## 11. Monitoring & Observability

### 11.1 Logging

| NFR | Description | Priority |
|-----|-------------|----------|
| OBS-001 | Appropriate logging levels | High |
| OBS-002 | No PII in logs | Critical |
| OBS-003 | Structured logging | Medium |
| OBS-004 | Error context captured | High |

### 11.2 Analytics

| NFR | Description | Priority |
|-----|-------------|----------|
| OBS-010 | Key user events tracked | High |
| OBS-011 | Performance metrics tracked | High |
| OBS-012 | Error events tracked | High |
| OBS-013 | Funnel analysis supported | Medium |

### 11.3 Crash Reporting

| NFR | Description | Priority |
|-----|-------------|----------|
| OBS-020 | Crash reporting integrated (Firebase Crashlytics) | Critical |
| OBS-021 | Non-fatal errors tracked | High |
| OBS-022 | Breadcrumbs for debugging | Medium |

---

## 12. Multi-Brand Considerations

### 12.1 Brand Support

| NFR | Description | Priority |
|-----|-------------|----------|
| BRAND-001 | Works across all target brands | Critical |
| BRAND-002 | Brand-specific variations handled cleanly | High |
| BRAND-003 | No brand-specific code in shared modules | High |
| BRAND-004 | Brand configuration externalized | High |

### 12.2 Consistency

| NFR | Description | Priority |
|-----|-------------|----------|
| BRAND-010 | Consistent behavior across brands | High |
| BRAND-011 | Shared components reused | High |
| BRAND-012 | No code duplication across brands | High |

---

## 13. Deployment & Release

### 13.1 Feature Flags

| NFR | Description | Priority |
|-----|-------------|----------|
| DEPLOY-001 | Feature flags for gradual rollout | High |
| DEPLOY-002 | Kill switch available | Medium |
| DEPLOY-003 | A/B testing supported (if needed) | Medium |

### 13.2 Rollback

| NFR | Description | Priority |
|-----|-------------|----------|
| DEPLOY-010 | Rollback strategy defined | High |
| DEPLOY-011 | Data migrations reversible | Medium |
| DEPLOY-012 | No breaking changes without migration path | High |

### 13.3 Versioning

| NFR | Description | Priority |
|-----|-------------|----------|
| DEPLOY-020 | API versioning considered | Medium |
| DEPLOY-021 | Backward compatibility maintained | High |
| DEPLOY-022 | Deprecated APIs have migration path | High |

---

## Usage Instructions for Agents

### For Senior Dev Agents

1. Read this entire checklist
2. For each category, assess if it applies to the feature
3. For applicable NFRs, check if the architecture addresses them
4. Mark status: ✅ Addressed | ⚠️ Partial | ❌ Not Addressed | 🔍 Needs Investigation | N/A
5. For any ⚠️ or ❌, create specific recommendations
6. Add NFR-specific acceptance criteria to user stories
7. Document findings in your NFR assessment

### Priority Levels

- **Critical**: Must be addressed, blocks release if not met
- **High**: Should be addressed, requires justification if not
- **Medium**: Nice to have, address if feasible

### Severity Assessment

When gaps are found:
- **Critical Severity**: Security vulnerabilities, data loss risks, crashes
- **High Severity**: Performance issues, usability problems, compliance gaps
- **Medium Severity**: Code quality, maintainability, minor UX issues
- **Low Severity**: Nice-to-have improvements

---

## References

- [Android Security Best Practices](https://developer.android.com/topic/security/best-practices)
- [Android Performance Best Practices](https://developer.android.com/topic/performance)
- [Android Accessibility Guidelines](https://developer.android.com/guide/topics/ui/accessibility)
- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)
- [GDPR Requirements](https://gdpr.eu/)
- [PCI DSS Standards](https://www.pcisecuritystandards.org/)
