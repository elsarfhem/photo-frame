# Example: Using the Feature Development System

This document shows a complete example of using the autonomous feature development system.

## Example Feature Request

Let's say you want to implement a loyalty points display feature that exists as Jira story ANDROID-1234.

### Step 1: Fill Out the Template

First, create the directory for feature requests (if it doesn't exist):

```bash
mkdir -p docs/feature-requests
```

**Option A: With Jira Integration (Recommended)**

Create `docs/feature-requests/loyalty-points.md` with minimal input:

```markdown
# Feature Development Request

## Jira Story
**ID**: ANDROID-1234

## Additional Functional Requirements
None - Jira story is complete

## References
- Figma: https://www.figma.com/file/abc123/Loyalty-Points-Design

## Context
**Target Brands**: All brands
**Target Release**: 2025.Q2
**Priority**: P1

---
@.claude/COORDINATOR_AGENT.md
then.
```

The system will automatically fetch title, description, acceptance criteria, and links from Jira!

---

**Option B: Without Jira (Manual)**

If you prefer to provide all details manually, create `docs/feature-requests/loyalty-points.md`:

```markdown
# Feature Development Request

## Jira Story

**ID**: ANDROID-1234

**Title**: Display Loyalty Points on Profile Screen

**Description**:
As a user, I want to see my loyalty points balance on my profile screen so that I can track my rewards and understand my loyalty status.

---

## Functional Requirements

1. Display current loyalty points balance on the user profile screen
2. Show points history (earned and redeemed) for the last 90 days
3. Update points in real-time when user earns or redeems points
4. Show pending points that will be credited after stay/trip completion
5. Display loyalty tier (Silver, Gold, Platinum) based on points
6. Provide a link to the loyalty program terms and conditions
7. Support offline mode - show cached points with a "last updated" timestamp

---

## References

### Design Materials
- Figma: https://www.figma.com/file/abc123/Loyalty-Points-Design
- Design specs: See attached PDF in Jira ticket
- Brand guidelines: Use standard Expedia Group Design System (EGDS)

### Technical References
- API documentation: `/api-docs/loyalty-service.md`
- Existing loyalty service: `lib/loyalty/` module
- Similar feature: "Rewards dashboard" in Vrbo app

### Similar Implementations
- Hotels.com already has a similar feature in their profile
- Reference implementation: `project/hotels/src/main/java/com/hotels/profile/loyalty/`

---

## Context

**Target Brands**:
- [x] All brands
- Note: UI may vary slightly per brand based on EGDS tokens

**Target Release**: 2025.Q2 (June release)

**Priority**: P1

**Estimated Complexity**: Medium

---

## Constraints & Assumptions

**Constraints**:
- Must work on Android API 24+ (Android 7.0+)
- Must load within 2 seconds on 4G connection
- Must handle up to 10,000 transaction records efficiently

**Assumptions**:
- Loyalty API v2 is available and stable
- Users must be authenticated to view points
- Backend supports real-time updates via WebSocket or polling

---

## Special Instructions

- Ensure all sensitive data (loyalty number, transaction details) is encrypted
- Support deep linking to loyalty points screen from push notifications
- Include analytics events for tracking engagement
- Must be accessible for users with visual impairments

---

## Start Command

@.claude/COORDINATOR_AGENT.md

Execute the feature development process for the requirements specified above.
```

### Step 2: Start Claude and Reference the File

Start Claude Code:

```bash
cd /path/to/ewe-android-eb
claude
```

Then provide this command:

```
@.claude/COORDINATOR_AGENT.md

Execute the feature development process using the requirements in:
docs/feature-requests/loyalty-points.md
```

**No copy-paste needed!** The coordinator will read the file directly.

### Step 3: Expected Output

The coordinator will respond with something like:

```
Reading feature request from: docs/feature-requests/loyalty-points.md
✅ Feature request file loaded

Starting autonomous feature development process. This will take several minutes.

Feature Development Started: ANDROID-1234

=== Phase 0: Initialization & Jira Integration ===
🔍 Fetching Jira story ANDROID-1234...
   Using MCP tool: mcp__atlassian__getAccessibleAtlassianResources
   Cloud ID: abc123-def456
   Using MCP tool: mcp__atlassian__getJiraIssue
✅ Jira story fetched successfully
   Title: "Display Loyalty Points on Profile Screen"
   Description: [175 characters fetched]
   Acceptance Criteria: 5 criteria found in Jira
   Labels: android, profile, loyalty, p1
   Linked Issues: ANDROID-1200 (blocks this)

Feature slug: android-1234-loyalty-points
Directory created: docs/features/android-1234-loyalty-points/

✅ Feature directory structure created
✅ Progress tracking initialized
✅ Jira data extracted and ready for Product Owner

=== Phase 1: Requirements Enrichment ===
Spawning Product Owner agent...
[Agent executes...]
✅ PRD draft created with 7 user stories
✅ 23 acceptance criteria defined
✅ Success metrics defined
✅ Dependencies identified

=== Phase 2: Architecture Proposals ===
Spawning 3 Architect agents in parallel...
  - Architect 1 (Modularity focus)
  - Architect 2 (Performance focus)
  - Architect 3 (Simplicity focus)
[Agents execute in parallel...]
✅ 3 architecture proposals created
✅ Diverse perspectives captured

=== Phase 3: Architecture Synthesis ===
Spawning Synthesis agent...
[Agent executes...]
✅ Proposal comparison completed
✅ Final architecture synthesized
✅ ADR documented

=== Phase 4: Technical Review & NFR Validation ===
Spawning 2 Senior Dev agents in parallel...
  - Senior Dev 1 (Security & Performance)
  - Senior Dev 2 (Testability & Maintainability)
[Agents execute in parallel...]
✅ NFR assessments completed
✅ 12 security requirements validated
✅ 8 performance requirements validated
✅ Enhanced acceptance criteria added

=== Phase 5: Test Planning ===
Spawning 2 QA agents in parallel...
  - QA 1 (Unit & Integration tests)
  - QA 2 (UI & E2E tests)
[Agents execute in parallel...]
✅ 45 unit/integration test cases defined
✅ 28 UI/E2E test cases defined
✅ Test data and mocks specified

=== Phase 6: Final PRD Generation ===
Compiling all artifacts...
Using /ralph skill to generate prd.json...
✅ Final PRD created
✅ Ralph JSON generated
✅ Summary report created

=== Feature Development Complete: ANDROID-1234 ===

All phases completed successfully. The feature is ready for implementation.

Artifacts Created:
- Requirements: docs/features/android-1234-loyalty-points/requirements/
- Architecture: docs/features/android-1234-loyalty-points/architecture/
- Reviews: docs/features/android-1234-loyalty-points/review/
- Testing: docs/features/android-1234-loyalty-points/testing/
- Final PRD: docs/features/android-1234-loyalty-points/final/PRD.md
- Ralph JSON: docs/features/android-1234-loyalty-points/final/prd.json

Key Decisions:
- Use Repository pattern with Room caching for offline support
- Implement real-time updates via Flow with 30-second polling fallback
- Shared ViewModel in lib/loyalty/ module for use across brands

Implementation Readiness:
- ✅ Architecture approved
- ✅ All critical NFRs addressed
- ✅ Test plans created
- ✅ Ready for development sprint

Next Steps:
1. Review final PRD: docs/features/android-1234-loyalty-points/final/PRD.md
2. Load prd.json into Ralph system
3. Begin implementation sprint
4. Execute test plans during development

Estimated Complexity: Medium (4-6 developer-days)
```

### Step 4: Review the Artifacts

Navigate to the feature directory:

```bash
cd docs/features/android-1234-loyalty-points/
```

Review key documents:

```bash
# Review the requirements
cat requirements/PRD_DRAFT.md

# Review the architecture decision
cat architecture/ADR.md

# Review the final architecture
cat architecture/FINAL_ARCHITECTURE.md

# Review NFR assessments
cat review/nfr-assessment-security-performance.md
cat review/nfr-assessment-testability-maintainability.md

# Review test plans
cat testing/unit-integration-tests.md
cat testing/ui-e2e-tests.md

# Review final PRD
cat final/PRD.md
```

### Step 5: Start Implementation

With the comprehensive documentation:

1. **Architecture**: Follow `architecture/FINAL_ARCHITECTURE.md`
   - Create the specified components
   - Follow the module structure
   - Implement as designed

2. **NFRs**: Address all NFR acceptance criteria
   - Implement security measures from `review/nfr-assessment-security-performance.md`
   - Follow performance guidelines
   - Ensure testability per `review/nfr-assessment-testability-maintainability.md`

3. **Tests**: Implement tests per test plans
   - Unit tests: `testing/unit-integration-tests.md`
   - UI tests: `testing/ui-e2e-tests.md`

4. **Ralph**: Load `final/prd.json` into Ralph system for tracking

## What Each Phase Produces

### Phase 1: Requirements (Product Owner)

**File**: `requirements/PRD_DRAFT.md`

**Content**:
- 7 user stories (US-1 through US-7)
- Each with 3-5 acceptance criteria
- Success metrics
- Dependencies (Loyalty API v2, Auth service)
- Multi-brand considerations

**Example User Story**:
```markdown
### US-1: View Current Points Balance (P0)

**Story**:
As a logged-in user,
I want to see my current loyalty points balance,
So that I can track my rewards status.

**Acceptance Criteria**:
1. Given I am authenticated, When I navigate to profile, Then I see my points balance
2. Given I have 0 points, When I view balance, Then it displays "0 points"
3. Given the API is unavailable, When I view balance, Then I see cached value with warning
4. Given I just earned points, When I refresh, Then new points are shown within 5 seconds

**Business Value**: Increases user engagement with loyalty program by 20%

**Dependencies**: Loyalty API v2, Auth service
```

### Phase 2: Architecture Proposals (3 Architects)

**Files**:
- `architecture/proposals/architect-1-modularity.md`
- `architecture/proposals/architect-2-performance.md`
- `architecture/proposals/architect-3-simplicity.md`

**Architect 1 (Modularity)** proposes:
- Separate `loyalty-domain`, `loyalty-data`, `loyalty-ui` modules
- Clean architecture with UseCases
- Highly reusable components

**Architect 2 (Performance)** proposes:
- Aggressive caching with Room
- Optimized data loading with Paging3
- Background prefetching

**Architect 3 (Simplicity)** proposes:
- Single `loyalty` module
- Direct ViewModel → Repository
- Minimal abstractions

### Phase 3: Synthesis (Synthesis Agent)

**Files**:
- `architecture/PROPOSAL_COMPARISON.md`
- `architecture/FINAL_ARCHITECTURE.md`
- `architecture/ADR.md`

**Synthesis** creates unified approach:
- Takes modular structure from Architect 1
- Adopts caching strategy from Architect 2
- Uses simplified DI from Architect 3
- Documents decision rationale in ADR

### Phase 4: NFR Review (2 Senior Devs)

**Files**:
- `review/nfr-assessment-security-performance.md`
- `review/nfr-assessment-testability-maintainability.md`

**Senior Dev 1** validates:
- ✅ PII encrypted (loyalty numbers)
- ✅ API calls use HTTPS with certificate pinning
- ✅ Load time < 2s target achievable with caching
- ⚠️ Need to add input validation for manual point entry
- ❌ Missing rate limiting for API calls

**Senior Dev 2** validates:
- ✅ ViewModel unit testable with mocked repository
- ✅ Repository integration testable with fake API
- ✅ Cyclomatic complexity acceptable
- ⚠️ Need to add KDoc comments to public APIs

### Phase 5: Test Plans (2 QA Agents)

**Files**:
- `testing/unit-integration-tests.md`
- `testing/ui-e2e-tests.md`

**QA 1** creates:
- 30 unit test cases for ViewModel, UseCase, Repository
- 15 integration test cases for data flow
- Mocking strategy with Mockk
- Test data fixtures

**QA 2** creates:
- 20 UI component tests with Compose Testing
- 8 E2E user journey tests
- Accessibility test cases
- Visual regression tests

### Phase 6: Final PRD

**Files**:
- `final/PRD.md` - Comprehensive PRD
- `final/prd.json` - Ralph format
- `final/SUMMARY.md` - Executive summary

**Final PRD** combines:
- All user stories with NFR acceptance criteria
- Final architecture specification
- NFR requirements validated
- Test strategy
- Implementation plan with phases
- Rollout strategy with feature flags

## Tips for Success

### 1. Provide Complete Information

The more detailed your initial request:
- The better the PRD quality
- The more accurate the architecture
- The fewer gaps in NFR coverage

### 2. Review Intermediate Artifacts

Don't wait until the end:
- Check `PROGRESS.md` periodically
- Review proposals as they're created
- Catch issues early

### 3. Iterate if Needed

If output doesn't meet expectations:
- Provide additional context
- Re-run specific phases
- Refine agent instructions

### 4. Use the Ralph JSON

The `prd.json` is designed for:
- Loading into Ralph system
- Automated task tracking
- Integration with project management tools

### 5. Keep Artifacts Updated

During implementation:
- Update PRD if requirements change
- Document deviations from architecture
- Keep test plans current

## Common Scenarios

### Scenario: Simple Feature (Low Complexity)

For simple features, the process might seem heavy. Consider:
- Using just Phase 1 (Requirements) for very simple features
- Skipping architecture proposals if obvious
- Focusing on NFR validation and testing

### Scenario: Complex Feature (High Complexity)

For complex features:
- Provide extra detail in initial request
- Review architecture proposals carefully
- Consider additional architecture iterations
- Pay special attention to NFR assessments

### Scenario: Cross-Team Dependencies

If feature depends on other teams:
- Document dependencies clearly in initial request
- Product Owner will flag them in PRD
- Architects will design around them
- Senior Devs will validate feasibility

### Scenario: Brand-Specific Feature

If only for specific brands:
- Mark clearly in initial request
- Product Owner will structure accordingly
- Architects will isolate brand-specific code
- Test plans will focus on target brands

## Troubleshooting Examples

### Issue: Missing API Documentation

**Problem**: Phase 1 identifies missing API docs

**Solution**:
1. Pause after Phase 1
2. Obtain API documentation
3. Add to feature directory
4. Resume with Phase 2

### Issue: Architecture Proposals Too Similar

**Problem**: All 3 architects propose same approach

**Solution**:
1. Review proposals
2. If genuinely optimal, proceed
3. If lacking creativity, re-run Phase 2 with more specific perspectives

### Issue: NFR Assessment Blocks Progress

**Problem**: Critical NFR gaps found

**Solution**:
1. Review specific gaps
2. Update architecture to address
3. Re-run NFR validation
4. Proceed when critical items resolved

---

This example demonstrates the complete flow from feature request to implementation-ready artifacts. The system is designed to handle complexity while remaining practical and actionable.
