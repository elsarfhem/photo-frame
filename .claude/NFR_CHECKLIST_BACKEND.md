# Non-Functional Requirements (NFR) Checklist - Backend Services

This checklist defines comprehensive non-functional requirements for backend services and APIs. All features must address applicable NFRs before implementation.

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
| SEC-001 | API authentication required (OAuth2, JWT, API keys) | Critical |
| SEC-002 | Role-based access control (RBAC) implemented | Critical |
| SEC-003 | Token expiration and refresh handled | High |
| SEC-004 | Service-to-service authentication (mTLS, service mesh) | High |
| SEC-005 | Multi-factor authentication support | Medium |
| SEC-006 | Session management with appropriate timeout | High |

### 1.2 Data Protection

| NFR | Description | Priority |
|-----|-------------|----------|
| SEC-010 | PII data encrypted at rest | Critical |
| SEC-011 | Data encrypted in transit (TLS 1.2+) | Critical |
| SEC-012 | No PII in logs or traces | Critical |
| SEC-013 | Secure key management (KMS, Vault) | Critical |
| SEC-014 | Database credentials rotated regularly | High |
| SEC-015 | Payment data complies with PCI-DSS | Critical |
| SEC-016 | Data minimization - only process necessary data | High |
| SEC-017 | Data masking for sensitive fields in logs | Critical |

### 1.3 Input Validation & Injection Prevention

| NFR | Description | Priority |
|-----|-------------|----------|
| SEC-020 | All API input validated | Critical |
| SEC-021 | SQL injection prevention (parameterized queries) | Critical |
| SEC-022 | NoSQL injection prevention | Critical |
| SEC-023 | Command injection prevention | Critical |
| SEC-024 | XML/JSON injection prevention | High |
| SEC-025 | Path traversal prevention | High |
| SEC-026 | LDAP injection prevention | High |
| SEC-027 | Request size limits enforced | High |

### 1.4 API Security

| NFR | Description | Priority |
|-----|-------------|----------|
| SEC-030 | HTTPS only for all endpoints | Critical |
| SEC-031 | CORS configured properly | High |
| SEC-032 | Rate limiting implemented | Critical |
| SEC-033 | API versioning strategy | High |
| SEC-034 | No sensitive data in URLs/query params | Critical |
| SEC-035 | Content Security Policy headers | High |
| SEC-036 | OWASP API Security Top 10 addressed | Critical |

### 1.5 Infrastructure Security

| NFR | Description | Priority |
|-----|-------------|----------|
| SEC-040 | Secrets not in source code or config files | Critical |
| SEC-041 | Infrastructure as Code security scanned | High |
| SEC-042 | Container images scanned for vulnerabilities | High |
| SEC-043 | Least privilege principle for service accounts | Critical |
| SEC-044 | Network segmentation enforced | High |
| SEC-045 | Security groups/firewall rules minimal | High |

---

## 2. Performance

### 2.1 Response Time

| NFR | Description | Priority |
|-----|-------------|----------|
| PERF-001 | API response time < 200ms (p50) | High |
| PERF-002 | API response time < 500ms (p95) | High |
| PERF-003 | API response time < 1s (p99) | Medium |
| PERF-004 | Database query time < 100ms (p95) | High |
| PERF-005 | Cache hit ratio > 80% for frequently accessed data | Medium |

### 2.2 Throughput

| NFR | Description | Priority |
|-----|-------------|----------|
| PERF-010 | Handle target requests per second (define baseline) | Critical |
| PERF-011 | Support concurrent connections (define baseline) | High |
| PERF-012 | Message queue throughput meets demand | High |
| PERF-013 | Batch processing efficiency defined | Medium |

### 2.3 Resource Utilization

| NFR | Description | Priority |
|-----|-------------|----------|
| PERF-020 | CPU utilization < 70% under normal load | High |
| PERF-021 | Memory utilization < 80% under normal load | High |
| PERF-022 | No memory leaks | Critical |
| PERF-023 | Connection pooling implemented | High |
| PERF-024 | Thread pool sizing optimized | Medium |
| PERF-025 | Efficient serialization/deserialization | Medium |

### 2.4 Database Performance

| NFR | Description | Priority |
|-----|-------------|----------|
| PERF-030 | Database queries optimized with proper indexes | High |
| PERF-031 | N+1 query problems avoided | High |
| PERF-032 | Pagination for large result sets | High |
| PERF-033 | Database connection pooling configured | High |
| PERF-034 | Read replicas used for read-heavy operations | Medium |
| PERF-035 | Query plans reviewed and optimized | Medium |

### 2.5 Caching

| NFR | Description | Priority |
|-----|-------------|----------|
| PERF-040 | Caching strategy defined and implemented | High |
| PERF-041 | Cache invalidation strategy defined | High |
| PERF-042 | CDN for static assets | Medium |
| PERF-043 | HTTP caching headers configured | Medium |
| PERF-044 | Cache stampede prevention | Medium |

---

## 3. Reliability & Availability

### 3.1 High Availability

| NFR | Description | Priority |
|-----|-------------|----------|
| REL-001 | Service uptime SLA defined (e.g., 99.9%) | Critical |
| REL-002 | Multi-AZ/region deployment | High |
| REL-003 | No single point of failure | Critical |
| REL-004 | Automatic failover configured | High |
| REL-005 | Load balancing implemented | Critical |
| REL-006 | Health checks configured | Critical |

### 3.2 Error Handling & Resilience

| NFR | Description | Priority |
|-----|-------------|----------|
| REL-010 | All errors handled gracefully | Critical |
| REL-011 | Circuit breaker pattern for external dependencies | High |
| REL-012 | Retry logic with exponential backoff | High |
| REL-013 | Timeout configured for all external calls | Critical |
| REL-014 | Bulkhead pattern for resource isolation | Medium |
| REL-015 | Graceful degradation strategy | High |
| REL-016 | Fallback mechanisms defined | High |

### 3.3 Data Integrity & Consistency

| NFR | Description | Priority |
|-----|-------------|----------|
| REL-020 | ACID properties for transactions | Critical |
| REL-021 | Eventual consistency strategy (if applicable) | High |
| REL-022 | Data validation before persistence | High |
| REL-023 | Idempotency for state-changing operations | Critical |
| REL-024 | Duplicate request handling | High |
| REL-025 | Distributed transaction handling (if needed) | Medium |

### 3.4 Disaster Recovery

| NFR | Description | Priority |
|-----|-------------|----------|
| REL-030 | Backup strategy defined and tested | Critical |
| REL-031 | Recovery Time Objective (RTO) defined | High |
| REL-032 | Recovery Point Objective (RPO) defined | High |
| REL-033 | Disaster recovery plan documented | High |
| REL-034 | Regular DR testing performed | Medium |

---

## 4. Scalability

### 4.1 Horizontal Scalability

| NFR | Description | Priority |
|-----|-------------|----------|
| SCALE-001 | Stateless service design | Critical |
| SCALE-002 | Auto-scaling configured | High |
| SCALE-003 | Handles traffic spikes gracefully | High |
| SCALE-004 | No hardcoded instance dependencies | High |
| SCALE-005 | Session state externalized | High |

### 4.2 Vertical Scalability

| NFR | Description | Priority |
|-----|-------------|----------|
| SCALE-010 | Resource limits defined and tested | High |
| SCALE-011 | Handles increased data volume | High |
| SCALE-012 | Database scaling strategy defined | High |

### 4.3 Data Scalability

| NFR | Description | Priority |
|-----|-------------|----------|
| SCALE-020 | Database sharding strategy (if needed) | Medium |
| SCALE-021 | Data archiving strategy defined | Medium |
| SCALE-022 | Efficient handling of large payloads | High |
| SCALE-023 | Pagination for large result sets | High |
| SCALE-024 | Streaming for large data transfers | Medium |

---

## 5. Observability & Monitoring

### 5.1 Logging

| NFR | Description | Priority |
|-----|-------------|----------|
| OBS-001 | Structured logging implemented | High |
| OBS-002 | Appropriate log levels used | High |
| OBS-003 | No PII in logs | Critical |
| OBS-004 | Correlation IDs for request tracing | High |
| OBS-005 | Log aggregation configured | High |
| OBS-006 | Log retention policy defined | Medium |

### 5.2 Metrics

| NFR | Description | Priority |
|-----|-------------|----------|
| OBS-010 | Application metrics exposed (Prometheus, CloudWatch) | High |
| OBS-011 | Business metrics tracked | High |
| OBS-012 | Error rate metrics tracked | Critical |
| OBS-013 | Latency metrics tracked (p50, p95, p99) | High |
| OBS-014 | Throughput metrics tracked | High |
| OBS-015 | Resource utilization metrics tracked | High |

### 5.3 Tracing

| NFR | Description | Priority |
|-----|-------------|----------|
| OBS-020 | Distributed tracing implemented | High |
| OBS-021 | Request flow traceable across services | High |
| OBS-022 | Database queries traced | Medium |
| OBS-023 | External API calls traced | High |

### 5.4 Alerting

| NFR | Description | Priority |
|-----|-------------|----------|
| OBS-030 | Critical error alerts configured | Critical |
| OBS-031 | Performance degradation alerts configured | High |
| OBS-032 | SLA breach alerts configured | High |
| OBS-033 | Anomaly detection configured | Medium |
| OBS-034 | Alert fatigue minimized (meaningful alerts only) | High |

---

## 6. API Design

### 6.1 RESTful Design

| NFR | Description | Priority |
|-----|-------------|----------|
| API-001 | RESTful principles followed | High |
| API-002 | Proper HTTP methods used (GET, POST, PUT, DELETE) | High |
| API-003 | HTTP status codes used correctly | High |
| API-004 | Resource-oriented URLs | High |
| API-005 | Consistent naming conventions | High |

### 6.2 API Documentation

| NFR | Description | Priority |
|-----|-------------|----------|
| API-010 | OpenAPI/Swagger documentation | Critical |
| API-011 | Request/response examples provided | High |
| API-012 | Error response format documented | High |
| API-013 | Authentication requirements documented | Critical |
| API-014 | Rate limits documented | High |

### 6.3 Versioning & Compatibility

| NFR | Description | Priority |
|-----|-------------|----------|
| API-020 | API versioning strategy implemented | High |
| API-021 | Backward compatibility maintained | High |
| API-022 | Deprecation policy defined | High |
| API-023 | Breaking changes communicated in advance | High |

### 6.4 Request/Response Design

| NFR | Description | Priority |
|-----|-------------|----------|
| API-030 | Consistent response format | High |
| API-031 | Pagination for list endpoints | High |
| API-032 | Filtering and sorting supported | Medium |
| API-033 | Field selection/sparse fieldsets supported | Medium |
| API-034 | HATEOAS for discoverability (if applicable) | Low |

---

## 7. Data Management

### 7.1 Database Design

| NFR | Description | Priority |
|-----|-------------|----------|
| DATA-001 | Proper normalization (or denormalization justification) | High |
| DATA-002 | Referential integrity enforced | High |
| DATA-003 | Appropriate indexes defined | High |
| DATA-004 | Database migrations versioned and tested | Critical |
| DATA-005 | Schema changes backward compatible | High |

### 7.2 Data Privacy & Compliance

| NFR | Description | Priority |
|-----|-------------|----------|
| DATA-010 | GDPR compliance | Critical |
| DATA-011 | Data retention policies enforced | High |
| DATA-012 | Right to deletion implemented | Critical |
| DATA-013 | Data export capability | High |
| DATA-014 | Audit trail for sensitive operations | High |
| DATA-015 | PII data handling compliant | Critical |

### 7.3 Data Quality

| NFR | Description | Priority |
|-----|-------------|----------|
| DATA-020 | Data validation rules enforced | High |
| DATA-021 | Data consistency checks | High |
| DATA-022 | Duplicate detection strategy | Medium |
| DATA-023 | Data cleansing processes defined | Medium |

---

## 8. Testability

### 8.1 Unit Testing

| NFR | Description | Priority |
|-----|-------------|----------|
| TEST-001 | Unit test coverage ≥ 80% | High |
| TEST-002 | Business logic fully unit tested | High |
| TEST-003 | All service methods unit tested | High |
| TEST-004 | Edge cases covered in tests | High |

### 8.2 Integration Testing

| NFR | Description | Priority |
|-----|-------------|----------|
| TEST-010 | Integration tests for external dependencies | High |
| TEST-011 | Database integration tests | High |
| TEST-012 | API integration tests (contract testing) | High |
| TEST-013 | Message queue integration tests | Medium |

### 8.3 Test Infrastructure

| NFR | Description | Priority |
|-----|-------------|----------|
| TEST-020 | Test doubles/mocks for external dependencies | High |
| TEST-021 | Test data management strategy | High |
| TEST-022 | CI/CD pipeline includes automated tests | Critical |
| TEST-023 | Performance tests automated | Medium |
| TEST-024 | Load testing performed | High |

---

## 9. Maintainability

### 9.1 Code Quality

| NFR | Description | Priority |
|-----|-------------|----------|
| MAINT-001 | Code follows language style guide | High |
| MAINT-002 | Clear naming conventions | High |
| MAINT-003 | Functions < 50 lines | Medium |
| MAINT-004 | Classes < 500 lines | Medium |
| MAINT-005 | Cyclomatic complexity < 10 | Medium |
| MAINT-006 | No code duplication (DRY) | High |
| MAINT-007 | Code review process enforced | High |

### 9.2 Architecture

| NFR | Description | Priority |
|-----|-------------|----------|
| MAINT-010 | Clear separation of concerns | High |
| MAINT-011 | SOLID principles followed | High |
| MAINT-012 | Consistent architecture patterns | High |
| MAINT-013 | Dependency injection used | High |
| MAINT-014 | Appropriate abstraction levels | Medium |

### 9.3 Documentation

| NFR | Description | Priority |
|-----|-------------|----------|
| MAINT-020 | API documentation complete and up-to-date | Critical |
| MAINT-021 | Architecture decisions documented (ADR) | High |
| MAINT-022 | Runbooks for operational procedures | High |
| MAINT-023 | README with setup instructions | High |
| MAINT-024 | Code comments for complex logic | High |

### 9.4 Dependencies

| NFR | Description | Priority |
|-----|-------------|----------|
| MAINT-030 | Dependency versions managed | High |
| MAINT-031 | Security vulnerabilities monitored | Critical |
| MAINT-032 | No deprecated dependencies | High |
| MAINT-033 | Minimal dependency footprint | Medium |

---

## 10. Deployment & Operations

### 10.1 Deployment

| NFR | Description | Priority |
|-----|-------------|----------|
| DEPLOY-001 | Zero-downtime deployment | High |
| DEPLOY-002 | Blue-green or canary deployment supported | High |
| DEPLOY-003 | Feature flags for gradual rollout | High |
| DEPLOY-004 | Automated deployment pipeline | High |
| DEPLOY-005 | Environment parity (dev/staging/prod) | High |
| DEPLOY-006 | Configuration externalized | Critical |

### 10.2 Rollback

| NFR | Description | Priority |
|-----|-------------|----------|
| DEPLOY-010 | Rollback strategy defined and tested | Critical |
| DEPLOY-011 | Database migrations reversible | High |
| DEPLOY-012 | Rollback time < 15 minutes | High |

### 10.3 Configuration Management

| NFR | Description | Priority |
|-----|-------------|----------|
| DEPLOY-020 | Configuration externalized from code | Critical |
| DEPLOY-021 | Environment-specific configuration | High |
| DEPLOY-022 | Dynamic configuration updates (where applicable) | Medium |
| DEPLOY-023 | Configuration validation on startup | High |

---

## 11. Asynchronous Processing

### 11.1 Message Queue

| NFR | Description | Priority |
|-----|-------------|----------|
| ASYNC-001 | Message durability guaranteed | High |
| ASYNC-002 | At-least-once delivery semantics | High |
| ASYNC-003 | Idempotent message processing | Critical |
| ASYNC-004 | Dead letter queue configured | High |
| ASYNC-005 | Message retry strategy defined | High |
| ASYNC-006 | Message ordering preserved (if required) | Medium |

### 11.2 Background Jobs

| NFR | Description | Priority |
|-----|-------------|----------|
| ASYNC-010 | Job scheduling implemented | High |
| ASYNC-011 | Job failure handling defined | High |
| ASYNC-012 | Job monitoring and alerting | High |
| ASYNC-013 | Job idempotency ensured | High |

---

## 12. Third-Party Integrations

### 12.1 External APIs

| NFR | Description | Priority |
|-----|-------------|----------|
| INT-001 | Circuit breaker for external services | Critical |
| INT-002 | Timeout configured for all external calls | Critical |
| INT-003 | Retry logic with exponential backoff | High |
| INT-004 | Fallback strategy for failures | High |
| INT-005 | API rate limits respected | High |
| INT-006 | Webhook validation implemented | High |

### 12.2 Service Dependencies

| NFR | Description | Priority |
|-----|-------------|----------|
| INT-010 | Service dependencies documented | High |
| INT-011 | Dependency health monitored | High |
| INT-012 | Graceful degradation when dependencies fail | High |

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
- **Critical Severity**: Security vulnerabilities, data loss risks, service downtime
- **High Severity**: Performance issues, reliability problems, compliance gaps
- **Medium Severity**: Code quality, maintainability, monitoring gaps
- **Low Severity**: Nice-to-have improvements

---

## References

- [OWASP API Security Top 10](https://owasp.org/www-project-api-security/)
- [12-Factor App Methodology](https://12factor.net/)
- [REST API Design Best Practices](https://restfulapi.net/)
- [OpenAPI Specification](https://swagger.io/specification/)
- [AWS Well-Architected Framework](https://aws.amazon.com/architecture/well-architected/)
- [Google SRE Books](https://sre.google/books/)
- [GDPR Requirements](https://gdpr.eu/)
- [PCI DSS Standards](https://www.pcisecuritystandards.org/)
