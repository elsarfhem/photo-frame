# Workflow Routing Guide: When to Use Full vs Simplified

## Quick Decision Tree

```
┌─────────────────────────────────────┐
│ New Story/Feature Request          │
└──────────────┬──────────────────────┘
               │
               ▼
    ┌──────────────────────┐
    │ What's the T-shirt   │
    │ size? (XS/S/M/L/XL)  │
    └──────┬──────┬─────────┘
           │      │
     XS/S  │      │  M/L/XL
           │      │
           ▼      ▼
    ┌──────────┐ ┌────────────┐
    │SIMPLIFIED│ │    FULL    │
    │ WORKFLOW │ │  WORKFLOW  │
    └──────────┘ └────────────┘
    4 phases     10 phases
    35-85 min    8-12 hours
```

## T-Shirt Sizing Guide

### XS (Extra Small) - Trivial
**Time**: < 1 hour
**Examples**:
- Fix typo in error message
- Update text copy
- Change color/styling
- Fix broken link
- Update configuration value

**Characteristics**:
- Single file change
- No logic changes
- No tests needed (or trivial test update)
- No architectural impact

**Workflow**: ⚡ **SIMPLIFIED**

---

### S (Small) - Simple
**Time**: 1-4 hours
**Examples**:
- Add tooltip to button
- Simple validation rule
- Minor UI enhancement
- Small bug fix with edge cases
- Add logging/analytics event

**Characteristics**:
- 1-3 files affected
- Simple logic change
- Requires tests
- Uses existing patterns
- No architectural decisions

**Workflow**: ⚡ **SIMPLIFIED**

---

### M (Medium) - Moderate
**Time**: 1-2 days
**Examples**:
- New screen/page with standard CRUD
- Add new API endpoint
- Integrate with existing service
- Refactor module internals
- Add new business rule

**Characteristics**:
- Multiple files/components
- Some design decisions needed
- Requires architecture review
- NFR considerations
- Multiple test scenarios

**Workflow**: 📋 **FULL**

---

### L (Large) - Complex
**Time**: 3-5 days
**Examples**:
- New feature with multiple screens
- New integration with external service
- Database schema changes
- Cross-module refactoring
- Complex business logic

**Characteristics**:
- Many files across modules
- Architectural decisions required
- Security/performance concerns
- Comprehensive testing needed
- Impacts multiple teams

**Workflow**: 📋 **FULL**

---

### XL (Extra Large) - Major
**Time**: > 1 week
**Examples**:
- New platform feature
- Major refactoring initiative
- New module/service
- Multi-phase rollout required
- Breaking API changes

**Characteristics**:
- System-wide impact
- Multiple sprints
- Requires design docs
- Extensive NFR analysis
- Phased implementation

**Workflow**: 📋 **FULL** (may need to break into smaller stories)

---

## Workflow Comparison

| Aspect | Simplified (XS/S) | Full (M/L/XL) |
|--------|-------------------|---------------|
| **Phases** | 4 (1, 2, 8, 9) | 10 (0-9) |
| **Teams** | No | Yes (3-agent debates) |
| **Architecture** | Use existing patterns | 3 architects debate |
| **NFR Review** | Assume best practices | 3 senior devs review |
| **Test Planning** | Generate tests directly | 3 QA plan comprehensively |
| **Gate Review** | Skip (validation at end) | Human approval required |
| **Time** | 35-85 minutes | 8-12 hours |
| **Human Time** | 5-15 minutes | 4-7 hours |
| **Documentation** | Lightweight (3-5 pages) | Comprehensive (30-50 pages) |

---

## When to Escalate from Simplified to Full

Start with simplified workflow but **escalate to full** if you discover:

### During Phase 1 (Refinement):
- ❌ User mentions "new integration"
- ❌ User mentions "change architecture"
- ❌ Security concerns raised
- ❌ Performance requirements specified
- ❌ Multiple modules affected

### During Phase 2 (PRD):
- ❌ AI identifies >3 user stories
- ❌ Complexity higher than expected
- ❌ Cross-cutting concerns discovered
- ❌ Significant NFR requirements

### During Phase 8 (Implementation):
- ❌ Existing patterns don't fit
- ❌ Architectural decisions needed
- ❌ High implementation complexity

### During Phase 9 (Testing):
- ❌ Test failures reveal deeper issues
- ❌ Edge cases more complex than expected

### During Human Validation:
- ❌ Reviewer identifies risks
- ❌ Code quality concerns
- ❌ Underestimated complexity

**Escalation Process**:
1. Stop simplified workflow
2. Document reason for escalation
3. Run full workflow phases 3-7 (Architecture, NFR Review, Test Planning, Gate Review)
4. Continue with enhanced implementation/testing

---

## Best Practices

### ✅ Use Simplified For:
- Bug fixes
- UI tweaks
- Config changes
- Small enhancements
- Documentation updates
- Trivial refactoring

### ❌ Don't Use Simplified For:
- New features requiring design
- Architectural changes
- Security-critical changes
- Performance-critical changes
- Cross-module refactoring
- New integrations
- Database migrations

### 💡 When In Doubt:
**Default to FULL workflow**. It's better to over-prepare than under-prepare.

---

## Setting T-Shirt Size

### Option 1: Jira Custom Field
1. Add "T-shirt Size" custom field to your Jira
2. Set values: XS, S, M, L, XL
3. Coordinator auto-detects from Jira

### Option 2: Manual Input
If no Jira field, coordinator will prompt:
```
What is the T-shirt size? (XS/S/M/L/XL)
- XS: Trivial (< 1 hour)
- S: Simple (1-4 hours)
- M: Moderate (1-2 days)
- L: Complex (3-5 days)
- XL: Major (> 1 week)
```

### Option 3: In Feature Prompt
Include size in your initial request:
```
"Add tooltip to settings button [SIZE: S]"
```

---

## Monitoring & Metrics

**Track** to ensure appropriate routing:

### Usage Metrics:
- % stories using simplified workflow (target: 40-60%)
- % escalations from simplified to full (target: <5%)
- Average time for simplified stories (target: <90 min)

### Quality Metrics:
- Bug rate: simplified vs full (should be equal)
- Human validation time (target: <15 min for simplified)
- First-pass success rate (target: >90%)

### Adjust If:
- <20% using simplified → Teams underestimating size
- >80% using simplified → Teams overusing fast-track
- >10% escalation rate → Sizing criteria unclear
- Higher bug rate in simplified → Need better validation

---

## Examples

### ✅ Good Simplified Usage

**Example 1**: "Fix: Error message shows 'null' instead of user-friendly text"
- **Size**: XS
- **Why Simplified**: Single string change, no logic
- **Time**: 20 minutes

**Example 2**: "Add: Show loading spinner during API call"
- **Size**: S
- **Why Simplified**: Standard pattern, uses existing components
- **Time**: 45 minutes

### ✅ Good Full Usage

**Example 3**: "New: User can filter products by category"
- **Size**: M
- **Why Full**: Multiple components, state management, API changes
- **Time**: 8 hours

**Example 4**: "New: Integrate with payment gateway"
- **Size**: L
- **Why Full**: Security critical, new integration, comprehensive testing
- **Time**: 16 hours

### ❌ Inappropriate Simplified Usage

**Example 5**: "Add: New user authentication flow" [marked as S]
- **Should be**: L (Full workflow)
- **Why**: Security critical, architectural impact
- **Result**: Would escalate during implementation

**Example 6**: "Refactor: Move business logic to service layer" [marked as S]
- **Should be**: M (Full workflow)
- **Why**: Architectural change, affects multiple modules
- **Result**: Would escalate during architecture phase

---

## FAQ

**Q: Can I switch from simplified to full mid-workflow?**
A: Yes! Use the escalation process at any phase.

**Q: Can I switch from full to simplified?**
A: No. Once in full workflow, complete all phases. (You overestimated, but that's safer than underestimating.)

**Q: What if I'm unsure about size?**
A: Default to full workflow. Over-preparing is better than under-preparing.

**Q: Can XS stories skip human validation?**
A: No. All changes require at least quick human spot-check (5-15 min).

**Q: Do simplified stories get documented?**
A: Yes, but lightweight (3-5 pages vs 30-50 pages).

---

**Remember**: The goal is to match process overhead to story complexity. Small changes shouldn't require 10-phase workflow, but complex changes need comprehensive planning.
