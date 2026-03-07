# Non-Functional Requirements (NFR) Checklist - Frontend Web Applications

This checklist defines comprehensive non-functional requirements for frontend web applications. All features must address applicable NFRs before implementation.

## How to Use This Checklist

- **Review**: Assess each NFR category for relevance to your feature
- **Mark**: ✅ Addressed | ⚠️ Partial | ❌ Not Addressed | 🔍 Needs Investigation | N/A Not Applicable
- **Document**: For each item, document how it's addressed in the architecture
- **Justify**: If marking N/A, explain why it doesn't apply

---

## 1. Security

### 1.1 Authentication & Session Management

| NFR | Description | Priority |
|-----|-------------|----------|
| SEC-001 | Secure authentication implemented | Critical |
| SEC-002 | Session timeout configured appropriately | High |
| SEC-003 | Token refresh handled gracefully | High |
| SEC-004 | Logout functionality clears all session data | Critical |
| SEC-005 | Multi-factor authentication supported | Medium |
| SEC-006 | Remember me functionality secure | Medium |

### 1.2 Client-Side Data Protection

| NFR | Description | Priority |
|-----|-------------|----------|
| SEC-010 | No sensitive data in localStorage | Critical |
| SEC-011 | Sensitive data in sessionStorage encrypted | High |
| SEC-012 | No PII in client-side logs or analytics | Critical |
| SEC-013 | Secure cookie attributes (HttpOnly, Secure, SameSite) | Critical |
| SEC-014 | No sensitive data in URL parameters | Critical |
| SEC-015 | Client-side data sanitized before storage | High |

### 1.3 XSS & Injection Prevention

| NFR | Description | Priority |
|-----|-------------|----------|
| SEC-020 | All user input sanitized | Critical |
| SEC-021 | Output encoding for HTML context | Critical |
| SEC-022 | CSP (Content Security Policy) implemented | High |
| SEC-023 | No eval() or Function() constructor with user input | Critical |
| SEC-024 | DOM-based XSS prevention | Critical |
| SEC-025 | innerHTML avoided or sanitized | Critical |
| SEC-026 | Trusted Types API used (where supported) | Medium |

### 1.4 API & Network Security

| NFR | Description | Priority |
|-----|-------------|----------|
| SEC-030 | HTTPS only for all requests | Critical |
| SEC-031 | API keys not exposed in client code | Critical |
| SEC-032 | CORS configured properly | High |
| SEC-033 | CSRF protection implemented | Critical |
| SEC-034 | Rate limiting on client side (prevent abuse) | Medium |
| SEC-035 | Secure WebSocket connections (wss://) | High |

### 1.5 Third-Party Dependencies

| NFR | Description | Priority |
|-----|-------------|----------|
| SEC-040 | Third-party libraries vetted for security | High |
| SEC-041 | Subresource Integrity (SRI) for CDN resources | High |
| SEC-042 | Dependency vulnerabilities monitored | High |
| SEC-043 | No deprecated or unmaintained dependencies | High |

---

## 2. Performance

### 2.1 Load Time

| NFR | Description | Priority |
|-----|-------------|----------|
| PERF-001 | First Contentful Paint (FCP) < 1.8s | High |
| PERF-002 | Largest Contentful Paint (LCP) < 2.5s | High |
| PERF-003 | Time to Interactive (TTI) < 3.8s | High |
| PERF-004 | First Input Delay (FID) < 100ms | High |
| PERF-005 | Cumulative Layout Shift (CLS) < 0.1 | High |

### 2.2 Resource Optimization

| NFR | Description | Priority |
|-----|-------------|----------|
| PERF-010 | Code splitting implemented | High |
| PERF-011 | Lazy loading for routes and components | High |
| PERF-012 | Images optimized and compressed | High |
| PERF-013 | Responsive images with srcset | High |
| PERF-014 | Critical CSS inlined | Medium |
| PERF-015 | Non-critical CSS loaded asynchronously | Medium |
| PERF-016 | Unused CSS removed | High |
| PERF-017 | JavaScript bundle size optimized | High |
| PERF-018 | Tree shaking applied | High |

### 2.3 Caching & CDN

| NFR | Description | Priority |
|-----|-------------|----------|
| PERF-020 | Static assets cached with cache-control headers | High |
| PERF-021 | Service Worker for offline caching (if PWA) | Medium |
| PERF-022 | CDN used for static assets | High |
| PERF-023 | Cache busting strategy implemented | High |
| PERF-024 | API responses cached appropriately | Medium |

### 2.4 Runtime Performance

| NFR | Description | Priority |
|-----|-------------|----------|
| PERF-030 | No layout thrashing | High |
| PERF-031 | Animation frame rate ≥ 60 FPS | High |
| PERF-032 | Long tasks broken up (< 50ms) | High |
| PERF-033 | Debouncing/throttling for frequent events | High |
| PERF-034 | Virtual scrolling for long lists | Medium |
| PERF-035 | Memoization for expensive computations | Medium |
| PERF-036 | Web Workers for CPU-intensive tasks | Medium |

### 2.5 Network Performance

| NFR | Description | Priority |
|-----|-------------|----------|
| PERF-040 | HTTP/2 or HTTP/3 used | Medium |
| PERF-041 | Resource hints (preload, prefetch, preconnect) | Medium |
| PERF-042 | API request batching where possible | Medium |
| PERF-043 | GraphQL query optimization (if applicable) | High |
| PERF-044 | Compression enabled (gzip/brotli) | High |

---

## 3. Accessibility (A11Y)

### 3.1 Screen Reader Support

| NFR | Description | Priority |
|-----|-------------|----------|
| A11Y-001 | Semantic HTML elements used | Critical |
| A11Y-002 | All images have alt text | Critical |
| A11Y-003 | ARIA labels for interactive elements | Critical |
| A11Y-004 | Logical reading order | High |
| A11Y-005 | Skip navigation links provided | High |
| A11Y-006 | Landmark regions defined | High |
| A11Y-007 | Live regions for dynamic content | High |

### 3.2 Keyboard Navigation

| NFR | Description | Priority |
|-----|-------------|----------|
| A11Y-010 | All interactive elements keyboard accessible | Critical |
| A11Y-011 | Focus indicators visible | Critical |
| A11Y-012 | Logical tab order | Critical |
| A11Y-013 | Focus management for modals and dialogs | High |
| A11Y-014 | No keyboard traps | Critical |
| A11Y-015 | Keyboard shortcuts documented | Medium |

### 3.3 Visual Accessibility

| NFR | Description | Priority |
|-----|-------------|----------|
| A11Y-020 | Color contrast ratio ≥ 4.5:1 (normal text) | High |
| A11Y-021 | Color contrast ratio ≥ 3:1 (large text) | High |
| A11Y-022 | Information not conveyed by color alone | High |
| A11Y-023 | Text resizable up to 200% | High |
| A11Y-024 | No horizontal scrolling at 320px width | High |
| A11Y-025 | Focus visible on all interactive elements | Critical |

### 3.4 Forms & Inputs

| NFR | Description | Priority |
|-----|-------------|----------|
| A11Y-030 | Form labels associated with inputs | Critical |
| A11Y-031 | Error messages descriptive and linked | Critical |
| A11Y-032 | Required fields indicated | High |
| A11Y-033 | Input purpose identified (autocomplete) | High |
| A11Y-034 | Validation messages accessible | Critical |

### 3.5 WCAG Compliance

| NFR | Description | Priority |
|-----|-------------|----------|
| A11Y-040 | WCAG 2.1 Level AA compliance | Critical |
| A11Y-041 | Automated accessibility tests passing | High |
| A11Y-042 | Manual accessibility testing performed | High |

---

## 4. Browser Compatibility

### 4.1 Browser Support

| NFR | Description | Priority |
|-----|-------------|----------|
| COMPAT-001 | Support for defined browser list (e.g., last 2 versions) | Critical |
| COMPAT-002 | Chrome support | Critical |
| COMPAT-003 | Firefox support | Critical |
| COMPAT-004 | Safari support | Critical |
| COMPAT-005 | Edge support | Critical |
| COMPAT-006 | Graceful degradation for older browsers | Medium |

### 4.2 Feature Detection

| NFR | Description | Priority |
|-----|-------------|----------|
| COMPAT-010 | Feature detection used (not browser sniffing) | High |
| COMPAT-011 | Polyfills for unsupported features | High |
| COMPAT-012 | Progressive enhancement approach | High |
| COMPAT-013 | Fallbacks for modern APIs | High |

### 4.3 Cross-Platform

| NFR | Description | Priority |
|-----|-------------|----------|
| COMPAT-020 | Works on desktop and mobile browsers | Critical |
| COMPAT-021 | Touch and mouse input supported | High |
| COMPAT-022 | Hover states have touch alternatives | High |

---

## 5. Responsive Design

### 5.1 Viewport Adaptability

| NFR | Description | Priority |
|-----|-------------|----------|
| RESP-001 | Mobile-first design | High |
| RESP-002 | Responsive breakpoints defined | Critical |
| RESP-003 | Works on phones (320px+) | Critical |
| RESP-004 | Works on tablets (768px+) | Critical |
| RESP-005 | Works on desktops (1024px+) | Critical |
| RESP-006 | Works on large screens (1920px+) | Medium |

### 5.2 Touch Optimization

| NFR | Description | Priority |
|-----|-------------|----------|
| RESP-010 | Touch targets ≥ 44x44px | High |
| RESP-011 | Touch gestures supported (swipe, pinch) | Medium |
| RESP-012 | No hover-only interactions | High |

### 5.3 Layout & Typography

| NFR | Description | Priority |
|-----|-------------|----------|
| RESP-020 | Flexible grid layout | High |
| RESP-021 | Fluid typography (responsive font sizes) | Medium |
| RESP-022 | No horizontal scrolling (unless intentional) | High |
| RESP-023 | Content reflows appropriately | High |

---

## 6. User Experience (UX)

### 6.1 Usability

| NFR | Description | Priority |
|-----|-------------|----------|
| UX-001 | Intuitive navigation | High |
| UX-002 | Consistent UI patterns | High |
| UX-003 | Clear visual hierarchy | High |
| UX-004 | Descriptive page titles | High |
| UX-005 | Breadcrumbs for deep navigation | Medium |
| UX-006 | Search functionality (if applicable) | Medium |

### 6.2 Feedback & States

| NFR | Description | Priority |
|-----|-------------|----------|
| UX-010 | Loading indicators for async operations | Critical |
| UX-011 | Skeleton screens for content loading | High |
| UX-012 | Disabled states clearly indicated | High |
| UX-013 | Success/error messages clear | High |
| UX-014 | Progress indicators for multi-step processes | High |
| UX-015 | Confirmation for destructive actions | Critical |

### 6.3 Error Handling

| NFR | Description | Priority |
|-----|-------------|----------|
| UX-020 | User-friendly error messages | High |
| UX-021 | Actionable error recovery steps | High |
| UX-022 | Inline validation for forms | High |
| UX-023 | Error boundaries to prevent white screen | Critical |
| UX-024 | Offline mode indicators | High |

### 6.4 Performance Perception

| NFR | Description | Priority |
|-----|-------------|----------|
| UX-030 | Optimistic UI updates | Medium |
| UX-031 | Immediate visual feedback | High |
| UX-032 | Perceived performance optimized | High |

---

## 7. Internationalization (i18n) & Localization (l10n)

### 7.1 Language Support

| NFR | Description | Priority |
|-----|-------------|----------|
| I18N-001 | All text externalized to translation files | Critical |
| I18N-002 | Multiple languages supported | High |
| I18N-003 | Language switcher available | High |
| I18N-004 | RTL (Right-to-Left) layout support | Medium |
| I18N-005 | Locale detection (browser preference) | High |

### 7.2 Formatting

| NFR | Description | Priority |
|-----|-------------|----------|
| I18N-010 | Date/time formatting per locale | High |
| I18N-011 | Number formatting per locale | High |
| I18N-012 | Currency formatting per locale | High |
| I18N-013 | Pluralization rules per language | High |
| I18N-014 | No hardcoded strings | Critical |

---

## 8. SEO (Search Engine Optimization)

### 8.1 Technical SEO

| NFR | Description | Priority |
|-----|-------------|----------|
| SEO-001 | Semantic HTML structure | High |
| SEO-002 | Meta tags (title, description) on all pages | High |
| SEO-003 | Open Graph tags for social sharing | Medium |
| SEO-004 | Twitter Card tags | Medium |
| SEO-005 | Canonical URLs defined | High |
| SEO-006 | robots.txt configured | Medium |
| SEO-007 | Sitemap.xml generated | Medium |

### 8.2 Content Accessibility

| NFR | Description | Priority |
|-----|-------------|----------|
| SEO-010 | Server-side rendering or static generation | High |
| SEO-011 | Content available without JavaScript | Medium |
| SEO-012 | Image alt text descriptive | High |
| SEO-013 | Heading hierarchy logical (H1, H2, H3) | High |

### 8.3 Performance for SEO

| NFR | Description | Priority |
|-----|-------------|----------|
| SEO-020 | Core Web Vitals passing | High |
| SEO-021 | Mobile-friendly (Google Mobile-Friendly Test) | High |
| SEO-022 | HTTPS enabled | Critical |
| SEO-023 | Structured data (Schema.org) where applicable | Medium |

---

## 9. Progressive Web App (PWA)

### 9.1 PWA Core Features

| NFR | Description | Priority |
|-----|-------------|----------|
| PWA-001 | Web App Manifest configured | Medium |
| PWA-002 | Service Worker implemented | Medium |
| PWA-003 | Works offline (or shows offline page) | Medium |
| PWA-004 | Installable on home screen | Medium |
| PWA-005 | App icons provided (multiple sizes) | Medium |

### 9.2 PWA Enhancements

| NFR | Description | Priority |
|-----|-------------|----------|
| PWA-010 | Push notifications (if applicable) | Low |
| PWA-011 | Background sync (if applicable) | Low |
| PWA-012 | Add to home screen prompt | Low |
| PWA-013 | Splash screen configured | Low |

---

## 10. Reliability & Error Handling

### 10.1 Error Boundaries

| NFR | Description | Priority |
|-----|-------------|----------|
| REL-001 | Error boundaries catch React errors | Critical |
| REL-002 | Fallback UI for errors | Critical |
| REL-003 | Error logging to monitoring service | Critical |
| REL-004 | User-friendly error pages (404, 500) | High |

### 10.2 Network Resilience

| NFR | Description | Priority |
|-----|-------------|----------|
| REL-010 | Retry logic for failed API calls | High |
| REL-011 | Timeout configured for API requests | High |
| REL-012 | Offline detection and handling | High |
| REL-013 | Request queuing for offline actions | Medium |
| REL-014 | Network error messages user-friendly | High |

### 10.3 State Management

| NFR | Description | Priority |
|-----|-------------|----------|
| REL-020 | State persistence across page reloads | Medium |
| REL-021 | Form data saved on error | High |
| REL-022 | Browser back/forward handled correctly | High |

---

## 11. Testing

### 11.1 Unit Testing

| NFR | Description | Priority |
|-----|-------------|----------|
| TEST-001 | Unit test coverage ≥ 80% | High |
| TEST-002 | All utility functions tested | High |
| TEST-003 | Component logic tested | High |
| TEST-004 | Redux/state management tested | High |

### 11.2 Integration Testing

| NFR | Description | Priority |
|-----|-------------|----------|
| TEST-010 | Component integration tests | High |
| TEST-011 | API integration tests (mocked) | High |
| TEST-012 | User flow tests | High |

### 11.3 E2E Testing

| NFR | Description | Priority |
|-----|-------------|----------|
| TEST-020 | Critical user journeys tested (Cypress, Playwright) | High |
| TEST-021 | Cross-browser E2E tests | Medium |
| TEST-022 | Accessibility tests automated | High |

### 11.4 Visual Regression Testing

| NFR | Description | Priority |
|-----|-------------|----------|
| TEST-030 | Visual regression tests for UI changes | Medium |
| TEST-031 | Screenshot comparison automated | Medium |

---

## 12. Monitoring & Analytics

### 12.1 Error Monitoring

| NFR | Description | Priority |
|-----|-------------|----------|
| MON-001 | Error tracking configured (Sentry, Bugsnag) | Critical |
| MON-002 | Unhandled promise rejections caught | Critical |
| MON-003 | Source maps configured for production debugging | High |
| MON-004 | Error context captured (user, browser, etc.) | High |

### 12.2 Performance Monitoring

| NFR | Description | Priority |
|-----|-------------|----------|
| MON-010 | Real User Monitoring (RUM) implemented | High |
| MON-011 | Core Web Vitals tracked | High |
| MON-012 | Bundle size monitored | Medium |
| MON-013 | API performance tracked | High |

### 12.3 User Analytics

| NFR | Description | Priority |
|-----|-------------|----------|
| MON-020 | User behavior analytics configured | High |
| MON-021 | Custom events tracked | High |
| MON-022 | Conversion funnels defined | Medium |
| MON-023 | A/B testing framework (if needed) | Low |
| MON-024 | Privacy-compliant analytics (GDPR, CCPA) | Critical |

---

## 13. Build & Deployment

### 13.1 Build Optimization

| NFR | Description | Priority |
|-----|-------------|----------|
| BUILD-001 | Production builds minified | Critical |
| BUILD-002 | Source maps generated | High |
| BUILD-003 | Environment variables managed securely | Critical |
| BUILD-004 | Build reproducibility ensured | Medium |

### 13.2 Deployment

| NFR | Description | Priority |
|-----|-------------|----------|
| DEPLOY-001 | CI/CD pipeline configured | High |
| DEPLOY-002 | Automated testing in pipeline | High |
| DEPLOY-003 | Staging environment available | High |
| DEPLOY-004 | Feature flags for gradual rollout | Medium |
| DEPLOY-005 | Rollback strategy defined | High |

### 13.3 Versioning

| NFR | Description | Priority |
|-----|-------------|----------|
| DEPLOY-010 | Semantic versioning followed | Medium |
| DEPLOY-011 | Changelog maintained | Medium |
| DEPLOY-012 | Version displayed in UI (footer/about) | Low |

---

## 14. Code Quality & Maintainability

### 14.1 Code Standards

| NFR | Description | Priority |
|-----|-------------|----------|
| MAINT-001 | Linting configured (ESLint) | High |
| MAINT-002 | Code formatting enforced (Prettier) | High |
| MAINT-003 | TypeScript (or PropTypes) for type safety | High |
| MAINT-004 | Consistent naming conventions | High |
| MAINT-005 | Code review process enforced | High |

### 14.2 Architecture

| NFR | Description | Priority |
|-----|-------------|----------|
| MAINT-010 | Component architecture documented | High |
| MAINT-011 | State management pattern consistent | High |
| MAINT-012 | Folder structure logical | High |
| MAINT-013 | Separation of concerns maintained | High |

### 14.3 Documentation

| NFR | Description | Priority |
|-----|-------------|----------|
| MAINT-020 | Component library/Storybook maintained | Medium |
| MAINT-021 | README with setup instructions | High |
| MAINT-022 | Architecture decisions documented | High |
| MAINT-023 | API integration documented | High |

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
- **Low**: Future enhancement

### Severity Assessment

When gaps are found:
- **Critical Severity**: Security vulnerabilities, accessibility failures, broken UX
- **High Severity**: Performance issues, browser compatibility, SEO problems
- **Medium Severity**: Code quality, maintainability, minor UX issues
- **Low Severity**: Nice-to-have improvements

---

## References

- [Web Vitals](https://web.dev/vitals/)
- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)
- [MDN Web Docs](https://developer.mozilla.org/)
- [React Best Practices](https://react.dev/learn)
- [Vue.js Best Practices](https://vuejs.org/guide/best-practices/)
- [PWA Guidelines](https://web.dev/progressive-web-apps/)
- [OWASP Frontend Security](https://cheatsheetseries.owasp.org/cheatsheets/HTML5_Security_Cheat_Sheet.html)
- [Google Lighthouse](https://developers.google.com/web/tools/lighthouse)
