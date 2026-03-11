# Simplified Workflow for Small Stories (XS/S)

## Overview

This is a **lightweight, fast-track process** for small stories (XS or S T-shirt size) that don't require comprehensive architecture debate or NFR deep-dives.

**Use When**:
- Story size is **XS** or **S** (from Jira "T-shirt size" field or user input)
- Simple changes: bug fixes, small enhancements, config changes
- Single module impact, no architectural decisions needed

**Don't Use When**:
- Story size is M, L, or XL
- Architectural decisions required
- Cross-cutting changes
- Security/performance concerns flagged
- New integrations or modules

## Phases (4 Total)

### Phase 0: Initialization

**Same as full workflow** - Create feature directory structure.

**Actions**:
```bash
docs/features/<feature-slug>/
├── requirements/
└── implementation/
```

**Duration**: < 1 minute

---

### Phase 1: Requirements Refinement (Interactive)

**Same as full workflow** - Interactive Q&A to clarify requirements.

**Goal**: Understand the small change clearly.

**Agent**: Coordinator asks clarifying questions directly (no separate agent).

**Questions Focus**:
- What exactly needs to change?
- What's the expected behavior?
- Are there edge cases?
- What's the acceptance criteria?

**Output**: `requirements/REFINEMENT_QA.md`

**Duration**: 5-10 minutes (user answering questions)

**Validation**:
- [ ] User answered all clarifying questions
- [ ] Acceptance criteria clear
- [ ] No architectural concerns raised

---

### Phase 2: Requirements Enrichment (Lightweight PRD)

**Simplified version** - Create lightweight PRD, not comprehensive.

**Agent**: Product Owner agent (individual, not team)

**Differences from Full Workflow**:
- ❌ Skip: Extensive user stories, NFR analysis, success metrics
- ✅ Keep: Clear description, acceptance criteria, edge cases
- ✅ Lightweight: 1-2 pages vs 5-10 pages

**Agent Instructions**:
```
You are creating a LIGHTWEIGHT PRD for a small story (XS or S size).

DO:
- Write clear description (2-3 paragraphs)
- List acceptance criteria (checklist format)
- Document edge cases from Q&A
- Note affected files/modules

DON'T:
- Write extensive user stories
- Deep NFR analysis (assume best practices)
- Success metrics or business case
- Comprehensive background

Keep it to 1-2 pages maximum.
```

**Output**: `requirements/LIGHTWEIGHT_PRD.md`

**Duration**: 5-10 minutes (AI autonomous)

**Validation**:
- [ ] Description clear
- [ ] Acceptance criteria listed
- [ ] Edge cases documented
- [ ] Affected modules identified

---

### **Phase 3-7: SKIPPED** ⚡

For small stories, we skip:
- ❌ Phase 3-4: Architecture Proposals & Synthesis (assume standard patterns)
- ❌ Phase 5: NFR Review (assume best practices apply)
- ❌ Phase 6: Test Planning (AI will generate appropriate tests)
- ❌ Phase 7: Gate Review (no human approval needed for small changes)

**Assumptions for Small Stories**:
- Standard architecture patterns apply
- NFRs handled by following coding standards
- Tests will be comprehensive enough for small scope
- Risk is low enough to skip gate review

**Safety Net**:
- Human validation still happens in Phase 9
- Can escalate to full workflow if complexity discovered

---

### Phase 8: Implementation

**Same agent as full workflow** - AI Developer writes the code.

**Differences**:
- Uses coding best practices (no explicit architecture doc)
- Follows existing patterns in affected modules
- **Still implements thread safety** (reads CONCURRENCY_GUIDELINES.md)
- **Still follows NFR best practices** (security, performance, etc.)

**Agent Instructions**:
```
Implement this small change following existing codebase patterns.

Input:
- requirements/LIGHTWEIGHT_PRD.md (description + acceptance criteria)
- requirements/REFINEMENT_QA.md (edge cases)

Guidelines:
- Follow patterns in affected modules
- Ensure thread safety (see CONCURRENCY_GUIDELINES.md)
- Follow coding standards
- Handle edge cases from Q&A
- Keep changes minimal and focused

Create:
- implementation/IMPLEMENTATION_SUMMARY.md (brief - 1 page)
```

**Output**:
- Code changes (actual implementation)
- `implementation/IMPLEMENTATION_SUMMARY.md` (brief summary)

**Duration**: 10-30 minutes (AI autonomous)

**Validation**:
- [ ] Code follows existing patterns
- [ ] All acceptance criteria addressed
- [ ] Edge cases handled
- [ ] Thread-safe if needed
- [ ] Implementation summary created

---

### Phase 9: Testing (Simplified)

**Simplified version** - AI generates and runs tests, but lighter scope.

**Agent**: Single QA agent (not team of 3)

**Differences from Full Workflow**:
- ❌ Skip: Comprehensive test planning phase
- ❌ Skip: Collaborative debate between 3 QA agents
- ✅ Keep: Generate tests, execute tests, report results
- ✅ Focus: Unit tests + critical path testing

**Agent Instructions**:
```
Generate and execute tests for this small change.

Input:
- requirements/LIGHTWEIGHT_PRD.md (acceptance criteria)
- implementation/ (code to test)

Test Scope:
- Unit tests for changed code
- Integration tests if needed
- Basic edge case tests
- Focus on acceptance criteria

Create:
- Test files (unit/integration)
- testing/TEST_RESULTS.md (simplified - 1-2 pages)
```

**Output**:
- Test code (unit/integration tests)
- `testing/TEST_RESULTS.md` (simplified results)

**Duration**: 10-20 minutes (AI autonomous)

**Validation**:
- [ ] Tests written and executed
- [ ] All acceptance criteria tested
- [ ] Edge cases covered
- [ ] Test results documented
- [ ] Critical tests pass

---

### Phase 10: Code Review & Fix (Simplified)

**Collaborative review** - 2 reviewers + developer debate and fix issues.

**Team**: 2 Code Reviewers + 1 Developer

**Differences from Full Workflow**:
- ❌ Skip: Third reviewer (2 instead of 3 for faster consensus)
- ✅ Keep: Scientific debate method, consensus requirement, iterative fixes

**Reviewer Focus Areas**:
1. **Reviewer 1**: Code Quality & Correctness (combines quality + correctness)
2. **Reviewer 2**: Security & Performance (combines security + performance)

**Process**:
1. Both reviewers conduct independent code review
2. Share findings via SendMessage
3. Developer defends implementation and explains choices
4. Team debates: challenge invalid concerns, defend legitimate issues
5. Reach consensus (both reviewers must agree on blocking issues)
6. Developer implements agreed-upon fixes
7. Re-review fixes
8. Repeat until approved (max 2 iterations for small stories)

**What Reviewers Check**:
- Code reuse: Is existing code leveraged?
- Correctness: Does it meet acceptance criteria?
- Edge cases: Are corner cases handled?
- Security: Any vulnerabilities?
- Performance: Any obvious bottlenecks?
- Thread safety: Proper synchronization if concurrent?
- Tests: Are tests adequate?

**Output**:
- `review/code-review-quality-correctness.md`
- `review/code-review-security-performance.md`
- `review/CODE_REVIEW_SUMMARY.md`

**Duration**: 10-20 minutes (AI review + fixes)

**Validation**:
- [ ] Both reviewers completed review
- [ ] Consensus reached on all issues
- [ ] Developer fixed all agreed-upon issues
- [ ] Both reviewers approved
- [ ] CODE_REVIEW_SUMMARY.md written

**On Success**: Proceed to Phase 11 (Human Validation)

---

### Phase 11: Human Validation (Quick Spot-Check)

**Human reviews** - Quick validation before merge.

**Who**: Developer or Tech Lead (5-15 minutes)

**Checklist**:
- [ ] Read LIGHTWEIGHT_PRD.md (understand what changed)
- [ ] Spot-check AI code (focus on logic, not style)
- [ ] Review test results (all critical tests pass?)
- [ ] Run tests locally (optional but recommended)
- [ ] Verify acceptance criteria met

**Actions**:
- ✅ **APPROVE**: Merge to main, deploy
- ⚠️ **REQUEST CHANGES**: AI fixes and re-runs tests
- 🚨 **ESCALATE TO FULL WORKFLOW**: If complexity is higher than expected

**Output**: Approval decision

**Duration**: 5-15 minutes (human)

---

## Workflow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│ SIMPLIFIED WORKFLOW (XS/S Stories)                          │
└─────────────────────────────────────────────────────────────┘

Phase 0: Init (1 min)
    ↓
Phase 1: Requirements Refinement Q&A (5-10 min, interactive)
    ↓
Phase 2: Lightweight PRD (5-10 min, AI)
    ↓
[SKIP Phases 3-7] ⚡ Fast-track!
    ↓
Phase 8: Implementation (10-30 min, AI)
    ↓
Phase 9: Testing (10-20 min, AI)
    ↓
Phase 10: Code Review & Fix (10-20 min, AI - 2 reviewers + developer)
    ↓
Phase 11: Human Validation (5-15 min, human)
    ↓
✅ DONE - Ready to merge!

Total Time: 55-125 minutes (vs 8-12 hours for full workflow)
```

## Comparison: Simplified vs Full Workflow

| Aspect | Full Workflow (M/L/XL) | Simplified (XS/S) |
|--------|------------------------|-------------------|
| **Phases** | 10 phases | 4 phases (+ human validation) |
| **Teams** | Yes (3-agent debates) | No (individual agents) |
| **Architecture** | 3 architects debate | Use existing patterns |
| **NFR Review** | 3 senior devs review | Assume best practices |
| **Test Planning** | 3 QA plan tests | AI generates tests directly |
| **Gate Review** | Human approval required | Skipped (validation at end) |
| **Total Time** | 8-12 hours (2-3 days) | 35-85 minutes (same day) |
| **Human Time** | 4-7 hours | 5-15 minutes |
| **Documentation** | Comprehensive | Lightweight |

## When to Escalate to Full Workflow

During simplified workflow, escalate if:

### **During Phase 1 (Refinement)**:
- User mentions architectural changes
- Security concerns raised
- Performance requirements mentioned
- Multiple modules affected
- Integration with new systems

### **During Phase 2 (PRD)**:
- Agent identifies complexity higher than expected
- Multiple user stories emerge
- Cross-cutting concerns discovered

### **During Phase 8 (Implementation)**:
- AI discovers architectural decisions needed
- Existing patterns don't fit
- High complexity in implementation

### **During Phase 9 (Testing)**:
- Test failures reveal deeper issues
- Edge cases more complex than expected

### **During Phase 10 (Human Validation)**:
- Human reviewer identifies risks
- Code quality concerns
- Complexity underestimated

**Escalation Process**:
1. Stop simplified workflow
2. Create architecture artifacts (Phases 3-4)
3. Run NFR review (Phase 5)
4. Run test planning (Phase 6)
5. Gate review (Phase 7)
6. Continue with enhanced implementation/testing

## Success Criteria

**Speed**:
- ✅ < 90 minutes total time
- ✅ Same-day completion

**Quality**:
- ✅ All acceptance criteria met
- ✅ Tests pass
- ✅ Code follows standards
- ✅ No security/performance issues

**Appropriate Scope**:
- ✅ Used for truly small changes
- ✅ Escalated when complexity higher than expected
- ✅ Human validation catches issues

## Configuration

### Jira Integration

**T-shirt Size Field**:
- Field name: `customfield_XXXXX` (your Jira custom field ID)
- Values: XS, S, M, L, XL

**Workflow Routing**:
```
IF story.tshirt_size IN ['XS', 'S']:
    use SIMPLIFIED_WORKFLOW
ELSE:
    use FULL_WORKFLOW
```

### Manual Input

If no Jira field, prompt user:
```
What is the story size? (XS/S/M/L/XL)
- XS: Trivial change (< 1 hour)
- S: Small change (1-4 hours)
- M: Medium feature (1-2 days)
- L: Large feature (3-5 days)
- XL: Major feature (> 1 week)
```

## Best Practices

### **Do Use Simplified Workflow For**:
- ✅ Bug fixes
- ✅ Small UI tweaks
- ✅ Config changes
- ✅ Minor enhancements to existing features
- ✅ Documentation updates
- ✅ Simple refactoring

### **Don't Use Simplified Workflow For**:
- ❌ New features requiring design
- ❌ Architectural changes
- ❌ Security-sensitive changes
- ❌ Performance-critical changes
- ❌ Cross-module refactoring
- ❌ New integrations
- ❌ Database schema changes

## Monitoring

**Track**:
- Simplified workflow usage rate
- Time savings vs full workflow
- Escalation rate (simplified → full)
- Quality metrics (bug rate in small stories)
- Human validation time

**Target Metrics**:
- 50-60% of stories use simplified workflow
- < 5% escalation rate
- Same or lower bug rate vs full workflow
- < 15 min human validation time

## Examples

### **XS Story Example**:
**Title**: Fix typo in error message
**Size**: XS
**Phases**: Refinement (skip Q&A) → Lightweight PRD → Implementation → Testing → Validation
**Time**: ~30 minutes

### **S Story Example**:
**Title**: Add tooltip to settings button
**Size**: S
**Phases**: Refinement → Lightweight PRD → Implementation → Testing → Validation
**Time**: ~60 minutes

### **M Story (Uses Full Workflow)**:
**Title**: Add new loyalty tier calculation
**Size**: M
**Phases**: All 10 phases (not simplified)
**Time**: 8-12 hours

---

**Remember**: When in doubt, use full workflow. It's better to over-prepare than under-prepare for complex changes.
