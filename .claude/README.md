# Claude Agent-Based Feature Development System

This directory contains a complete autonomous multi-agent orchestration system for structured feature development in the ewe-android-eb Android repository.

## Overview

This system enables **unsupervised, multi-phase feature development** from requirements to implementation-ready artifacts using specialized AI agents.

### Two Workflows: Fast-Track or Comprehensive

**⚡ Simplified Workflow** (XS/S stories):
- For small changes, bug fixes, minor enhancements
- 4 phases: Refinement → Lightweight PRD → Implementation → Testing
- **Duration: 35-85 minutes**
- No architecture debate, assumes best practices

**📋 Full Workflow** (M/L/XL stories):
- For features requiring architecture and comprehensive review
- 10 phases: Full requirements → Architecture debate → NFR review → Test planning → Implementation → Testing
- **Duration: 8-12 hours**
- Collaborative debate between agent teams

**Automatic routing** based on story T-shirt size (XS/S/M/L/XL).

### What It Does

**Full Workflow** (when story size is M/L/XL):
1. **Asks clarifying questions** (Coordinator - Interactive Q&A)
2. **Enriches requirements** (Product Owner agent)
3. **Generates 3 architecture proposals** (3 Architect agents debate → reach consensus)
4. **Synthesizes final architecture** (Synthesis agent)
5. **Validates NFRs** (3 Senior Dev agents debate → reach consensus)
6. **Creates test plans** (3 QA agents debate → reach consensus)
7. **Produces final PRD** (Ralph format)
8. **Implements the code** (Developer agent)
9. **Writes and executes tests** (3 QA implementation agents debate → reach consensus)

**Simplified Workflow** (when story size is XS/S):
1. **Asks clarifying questions** (Coordinator - Interactive Q&A)
2. **Creates lightweight PRD** (Product Owner agent - brief)
3. **Implements the code** (Developer agent - follows existing patterns)
4. **Writes and executes tests** (Single QA agent - focused tests)
5. **Human validation** (Quick spot-check - 5-15 minutes)

All **automatically**, with **collaborative scientific debate** (in full workflow) and minimal user interaction.

## Quick Start

> **Want to start immediately?** See **[QUICKSTART.md](QUICKSTART.md)** for a 3-step guide!

### 0. Setup (First Time Only)

Enable **agent teams** feature - see `.claude/SETUP.md` for detailed instructions.

**Quick setup:**
```json
# Add to ~/.config/claude-code/settings.json
{
  "env": {
    "CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS": "1"
  }
}
```

Then restart your terminal.

### 1. Fill Out the Template

Create a feature request from the template:

```bash
# Create the feature-requests directory (first time only)
mkdir -p docs/feature-requests

# Copy and fill out the template
cp .claude/FEATURE_PROMPT_TEMPLATE.md docs/feature-requests/my-feature.md

# Edit with your feature details
# (Use your editor or IDE to fill it out)
```

**Recommended Location**: `docs/feature-requests/`
- Keeps feature requests organized in one place
- Easy to find and reference
- Can be kept for historical record or deleted after process starts

**🎯 NEW - No Copy-Paste!**: Just reference the file path - the coordinator reads it directly!

**🔗 NEW - Jira Integration**: Just provide your Jira story ID! The system automatically fetches story details (title, description, acceptance criteria) directly from Jira using the Atlassian MCP server.

### 2. Start the Process

Start Claude and reference your feature request file:

```bash
# Start Claude Code
claude

# Then provide this command (no copy-paste needed!):
```

```
@.claude/COORDINATOR_AGENT.md

Execute the feature development process using the requirements in:
docs/feature-requests/my-feature.md
```

The coordinator will read the file directly - no need to copy-paste the entire contents!

**Ultra-Quick Start with Jira**:
If your story is well-defined in Jira, you can even skip the template:
```
@.claude/COORDINATOR_AGENT.md

Execute the feature development process for Jira story ANDROID-456.

Target brands: All brands
Target release: 2025.Q2
```

The system will fetch everything else from Jira automatically!

### 3. Wait for Completion

The coordinator will:
- Execute all phases autonomously
- Spawn specialized agents as needed
- Validate outputs at each gate
- Report progress and completion

### 4. Clean Up (Optional)

After the process starts, you can optionally delete your feature request file:

```bash
rm docs/feature-requests/my-feature.md
```

Or keep it for historical reference. All the enriched requirements are now in the feature directory.

### 5. Review Artifacts

When complete, find all artifacts in:
```
docs/features/<your-feature-slug>/
├── requirements/
│   ├── REFINEMENT_QA.md  # NEW: Q&A transcript with corner cases
│   └── PRD_DRAFT.md
├── architecture/
│   ├── proposals/
│   │   ├── architect-1-modularity.md
│   │   ├── architect-2-performance.md
│   │   └── architect-3-simplicity.md
│   ├── PROPOSAL_COMPARISON.md
│   ├── FINAL_ARCHITECTURE.md
│   └── ADR.md
├── review/
│   ├── nfr-assessment-security-performance.md
│   └── nfr-assessment-testability-maintainability.md
├── testing/
│   ├── unit-integration-tests.md
│   ├── ui-e2e-tests.md
│   └── TEST_RESULTS.md
└── final/
    ├── PRD.md
    ├── prd.json  # Ralph format
    └── SUMMARY.md
```

## System Architecture

```
┌──────────────────────────────────────────────────────────────┐
│          COORDINATOR AGENT                                   │
│  (Orchestrates all phases, validates gates)                  │
└──────────────────┬───────────────────────────────────────────┘
                   │
                   ▼
              ┌─────────┐
              │ Phase 1 │
              │ Q&A     │ ← Interactive clarifying questions
              │(Coord.) │
              └────┬────┘
                   │
    ┌──────────────┼──────────────┬─────────────┬──────────┐
    │              │              │             │          │
    ▼              ▼              ▼             ▼          ▼
┌─────────┐  ┌──────────┐  ┌──────────┐  ┌─────────┐  ┌────────┐
│ Phase 2 │  │ Phase 3  │  │ Phase 4  │  │ Phase 5 │  │Phase 6 │
│   PO    │  │3 Archs   │  │Synthesis │  │3 Sr Devs│  │ 3 QAs  │
│ Agent   │  │ (debate) │  │  Agent   │  │ (debate)│  │(debate)│
└─────────┘  └────┬─────┘  └──────────┘  └────┬────┘  └────┬───┘
                  │                            │            │
              Consensus                    Consensus    Consensus
                  │                            │            │
                  └────────────────┬───────────┴────────────┘
                                   │
                     ┌─────────────┼────────────┐
                     ▼             ▼            ▼
                ┌─────────┐  ┌────────┐  ┌─────────┐
                │ Phase 7 │  │Phase 8 │  │ Phase 9 │
                │Final PRD│  │  Dev   │  │ 3 QAs   │
                └─────────┘  └────────┘  │ (debate)│
                                         │ (tests) │
                                         └─────────┘
                                              │
                                          Consensus
```

## Files in This Directory

### Core Orchestration
- **`COORDINATOR_AGENT.md`**: Master orchestrator that runs the entire process using agent teams
- **`FEATURE_PROMPT_TEMPLATE.md`**: Template you fill out to start a feature
- **`QUICKSTART.md`**: 3-step quick start guide
- **`SETUP.md`**: Setup instructions for enabling agent teams
- **`JIRA_INTEGRATION.md`**: Guide for automatic Jira story fetching via MCP
- **`USAGE_COMPARISON.md`**: Old (copy-paste) vs new (file reference) approach

### Agent Definitions
- **`agents/PRODUCT_OWNER.md`**: Requirements enrichment agent
- **`agents/ARCHITECT.md`**: Architecture proposal agent (spawned 3x as teammates)
- **`agents/SYNTHESIS.md`**: Architecture synthesis agent
- **`agents/SENIOR_DEV.md`**: NFR validation agent (spawned 2x as teammates)
- **`agents/QA.md`**: Test planning agent (spawned 2x as teammates)
- **`agents/DEVELOPER.md`**: Implementation agent
- **`agents/QA_IMPLEMENTATION.md`**: Test implementation agent (spawned 2x as teammates)

### Reference Materials
- **`NFR_CHECKLIST_ANDROID.md`**: Comprehensive NFR checklist for Android/native apps
- **`NFR_CHECKLIST_BACKEND.md`**: Comprehensive NFR checklist for backend services
- **`NFR_CHECKLIST_FRONTEND.md`**: Comprehensive NFR checklist for frontend web apps
- **`README.md`**: This file

### Templates

The `templates/` directory contains reusable task templates:

1. **`FEATURE_PROMPT_TEMPLATE.md`** - Template for starting the feature development workflow
   - Used to create structured feature requests
   - Supports Jira integration (auto-fetches story details)
   - Copy to `docs/feature-requests/<name>.md` and fill out

2. **`PR_DESCRIPTION_GENERATOR.md`** - Automatic PR description generator
   - Analyzes current branch changes vs base branch
   - Reads feature documentation (if using FARO workflow)
   - Generates comprehensive PR description with summary, testing, NFR compliance, and reviewer notes
   - **Usage**: Simply say `"Generate a PR description for the current branch"`
   - Creates `PR_DESCRIPTION.md` ready for GitHub PR

## Process Phases

### Phase 0: Initialization & Platform Detection
- Detects platform type (Android, Backend, or Frontend)
- Selects appropriate NFR checklist based on platform
- Creates feature directory structure
- Generates feature slug
- Sets up progress tracking
- Fetches Jira story details (if Jira ID provided)

### Phase 1: Requirements Refinement (Interactive Q&A)
**Agent**: Coordinator (Interactive)
**Input**: Your feature request + Jira data
**Process**: Asks 1-4 clarifying questions per round to identify:
- Ambiguous requirements
- Corner cases and edge cases
- Technical constraints
- Integration points
- Error handling scenarios
**Output**:
- `requirements/REFINEMENT_QA.md` - Q&A transcript with clarifications and identified corner cases

### Phase 2: Requirements Enrichment
**Agent**: Product Owner
**Input**: Your feature request + Refinement Q&A
**Output**:
- `requirements/PRD_DRAFT.md` - Detailed requirements with user stories

### Phase 3: Architecture Proposals (Collaborative Debate)
**Agents**: 3 Architects (collaborative debate using agent teams)
- Architect 1: Modularity-focused
- Architect 2: Performance-focused
- Architect 3: Simplicity-focused

**Process**: Agents debate and challenge each other's proposals, reaching consensus through scientific discourse.

**Output**:
- `architecture/proposals/architect-1-modularity.md`
- `architecture/proposals/architect-2-performance.md`
- `architecture/proposals/architect-3-simplicity.md`

### Phase 4: Architecture Synthesis
**Agent**: Synthesis
**Input**: 3 architecture proposals
**Output**:
- `architecture/PROPOSAL_COMPARISON.md` - Comparative analysis
- `architecture/FINAL_ARCHITECTURE.md` - Unified architecture
- `architecture/ADR.md` - Architecture Decision Record

### Phase 5: Technical Review & NFR Validation (Collaborative Debate)
**Agents**: 3 Senior Devs (collaborative debate using agent teams)
- Senior Dev 1: Security & Performance focus
- Senior Dev 2: Testability & Maintainability focus
- Senior Dev 3: Scalability & Reliability focus

**Process**: Agents debate NFR priorities and risks, challenging each other's assessments.

**Output**:
- `review/nfr-assessment-security-performance.md`
- `review/nfr-assessment-testability-maintainability.md`
- `review/nfr-assessment-scalability-reliability.md`

### Phase 6: Test Planning (Collaborative Debate)
**Agents**: 3 QA Engineers (collaborative debate using agent teams)
- QA 1: Unit & Integration tests
- QA 2: UI & E2E tests
- QA 3: Performance & Accessibility tests

**Process**: Agents debate test coverage and priorities, identifying gaps in each other's plans.

**Output**:
- `testing/unit-integration-tests.md`
- `testing/ui-e2e-tests.md`
- `testing/performance-accessibility-tests.md`

### Phase 7: Final PRD Generation
**Agent**: Coordinator
**Output**:
- `final/PRD.md` - Comprehensive PRD with all phases synthesized
- `final/prd.json` - Ralph-formatted PRD
- `final/SUMMARY.md` - Executive summary

### Phase 8: Implementation
**Agent**: Developer
**Input**: All artifacts from previous phases
**Output**:
- `implementation/IMPLEMENTATION_SUMMARY.md` - Summary of files created/modified
- Actual Kotlin code files in appropriate modules
- Hilt dependency injection modules

### Phase 9: Test Implementation & Execution (Collaborative Debate)
**Agents**: 3 QA Implementation Engineers (collaborative debate using agent teams)
- QA Implementation 1: Unit & Integration tests
- QA Implementation 2: UI & E2E tests
- QA Implementation 3: Performance & Accessibility tests

**Process**: Agents review each other's test code, debating quality and coverage.

**Output**:
- `testing/TEST_RESULTS.md` - Test execution results
- Actual test code files in module test directories
- Coverage reports
- Verification of all acceptance criteria

## Key Features

### 1. File Reference (No Copy-Paste!)
Simply point to your feature request file:
```
Execute using: docs/feature-requests/my-feature.md
```
- ✅ No copy-paste of large file contents
- ✅ Clean, concise prompts
- ✅ Coordinator reads files directly
- ✅ Easy to update and re-reference

### 2. Direct Jira Integration
Automatically fetches story details from Jira using the Atlassian MCP server:
- No manual copy-paste needed
- Always uses current Jira data
- Preserves links, labels, and metadata
- Can optionally update Jira with results

### 3. Autonomous Execution
The coordinator runs all phases without requiring user intervention between phases.

### 4. Collaborative Debate & Consensus
Uses Claude Code's **agent teams** feature for collaborative scientific debate:
- **Odd Number Rule**: Always 3 teammates (never 2) to enable majority consensus and avoid deadlock
- **3 architects** debate and challenge each other's proposals
- **3 senior devs** debate NFR priorities and risk assessments
- **3 QA engineers** debate test coverage and identify gaps
- **Scientific Process**: Agents try to disprove each other's theories, like peer review
- **Consensus Required**: Teams proceed only when at least 2 out of 3 agree
- Teammates communicate via SendMessage during work
- Shared task list coordinates work

### 5. Validation Gates
Each phase has strict validation criteria:
- Required artifacts must be created
- Checklists must be completed
- Quality gates must pass

### 6. Conflict Avoidance
Agents are assigned non-overlapping file scopes to prevent merge conflicts:
- Each architect writes to a separate proposal file (3 files)
- Senior devs focus on different NFR areas (3 assessment files)
- QA agents handle different test types (3 test plan files)
- QA implementation agents write tests for different scopes (3 test suite files)

### 7. Platform-Specific NFR Integration
Non-functional requirements are automatically validated using platform-specific checklists:
- **Android NFR Checklist** (480+ checks): Security, performance, battery, offline support, Material Design, accessibility
- **Backend NFR Checklist** (350+ checks): API security, scalability, reliability, database performance, monitoring
- **Frontend NFR Checklist** (400+ checks): Web performance, accessibility (WCAG), browser compatibility, SEO, PWA
- Platform automatically detected from repository structure
- NFR-specific acceptance criteria added to stories

### 8. Complete Implementation
Includes developer and QA agents that write production-ready code:
- Kotlin code following project patterns
- Unit, integration, UI, performance, and accessibility tests
- Hilt dependency injection setup
- Follows architecture exactly as specified
- Tests executed with coverage reports
- Collaborative test code review between 3 QA implementation agents

### 9. Automatic PR Description Generator
Generate comprehensive pull request descriptions with a single command:
- **Usage**: Just say `"Generate a PR description for the current branch"`
- Analyzes git diff and commit history
- Extracts content from feature documentation (PRD, architecture, test plans)
- Generates structured PR description with:
  - Summary and changes
  - Architecture decisions
  - Testing coverage and NFR compliance
  - Breaking changes and migration guide
  - Reviewer notes and focus areas
- Creates `PR_DESCRIPTION.md` ready to copy to GitHub PR
- Optional: Can create PR directly with `gh pr create`

## Customization

### Adding New Agent Types

1. Create agent definition in `agents/<AGENT_NAME>.md`
2. Update `COORDINATOR_AGENT.md` to spawn the new agent
3. Define input/output contracts
4. Add validation criteria

### Modifying NFR Checklists

Edit the appropriate platform-specific NFR checklist:
- **`.claude/NFR_CHECKLIST_ANDROID.md`**: For Android/native mobile app requirements
- **`.claude/NFR_CHECKLIST_BACKEND.md`**: For backend service requirements
- **`.claude/NFR_CHECKLIST_FRONTEND.md`**: For frontend web app requirements

Modifications can include:
- Add new NFR categories
- Modify priority levels
- Add platform-specific requirements
- Update compliance standards (GDPR, WCAG, PCI-DSS, etc.)

### Adjusting Process Flow

Edit `COORDINATOR_AGENT.md` to:
- Add new phases
- Change agent spawning logic
- Modify validation gates
- Adjust parallel execution

## Best Practices

### For Feature Requests

1. **Be specific**: Provide detailed functional requirements
2. **Include references**: Link to designs, APIs, similar features
3. **Define scope**: Clarify which brands are affected
4. **Note constraints**: Document any known limitations

### For Reviewing Outputs

1. **Check PROGRESS.md first**: See current status
2. **Review in order**: Start with PRD, then architecture, then reviews
3. **Look for concerns**: Check for "⚠️" or "❌" markers
4. **Validate completeness**: Ensure all user stories are covered

### For Implementation

1. **Follow the architecture**: Implement as specified in FINAL_ARCHITECTURE.md
2. **Address NFRs**: Implement all NFR acceptance criteria
3. **Write tests**: Follow the test plans
4. **Update docs**: Keep documentation current

## Troubleshooting

### Process Stuck at a Phase

**Symptoms**: Coordinator reports "Phase X failed validation"

**Solutions**:
1. Check PROGRESS.md for blocker details
2. Review the phase's output artifacts
3. Manually complete missing artifacts
4. Re-run the coordinator from that phase

### Agent Produced Incomplete Output

**Symptoms**: Validation fails due to missing sections

**Solutions**:
1. Review the agent's instructions in `agents/`
2. Check if all inputs were provided
3. Manually complete the missing sections
4. Consider refining agent instructions

### Conflicting Recommendations

**Symptoms**: Different agents provide contradictory guidance

**Solutions**:
1. Review PROPOSAL_COMPARISON.md for synthesis logic
2. Check ADR.md for decision rationale
3. Escalate to human review if critical
4. Update agent instructions to align better

## Workflow Routing: Choosing the Right Process

The system automatically routes stories to the appropriate workflow based on **T-shirt size**.

### Automatic Routing

```
Story Size → Workflow Type → Duration
═══════════════════════════════════════
XS (< 1 hour)     → ⚡ Simplified → 35-85 min
S  (1-4 hours)    → ⚡ Simplified → 35-85 min
M  (1-2 days)     → 📋 Full      → 8-12 hours
L  (3-5 days)     → 📋 Full      → 8-12 hours
XL (> 1 week)     → 📋 Full      → 8-12 hours
```

### How Size is Determined

**Option 1: Jira Custom Field** (Recommended)
- Add "T-shirt Size" field to Jira
- Values: XS, S, M, L, XL
- Coordinator auto-detects from Jira

**Option 2: Manual Input**
- Coordinator prompts: "What is the T-shirt size? (XS/S/M/L/XL)"
- You provide size based on estimated effort

**Option 3: In Feature Prompt**
- Include size in request: `"Add tooltip to settings [SIZE: S]"`

### Simplified Workflow (XS/S)

**Use For**:
- Bug fixes
- Small UI tweaks
- Config changes
- Minor enhancements
- Simple refactoring

**Phases**:
1. Requirements Refinement (interactive Q&A)
2. Lightweight PRD (brief, 1-2 pages)
3. Implementation (follows existing patterns)
4. Testing (focused, single QA agent)
5. Human validation (5-15 min spot-check)

**Benefits**:
- ⚡ 10x faster (< 90 min vs 8-12 hours)
- No architecture debate overhead
- Same quality through validation

### Full Workflow (M/L/XL)

**Use For**:
- New features
- Architectural changes
- Security/performance critical
- Cross-module refactoring
- New integrations

**Phases**:
All 10 phases including:
- Architecture debate (3 agents)
- NFR review (3 agents)
- Test planning (3 agents)
- Gate review (human approval)

**Benefits**:
- Comprehensive review
- Multiple perspectives
- Documented decisions
- Lower risk for complex changes

### When to Escalate

Start with simplified but **escalate to full** if you discover:
- Architectural decisions needed
- Security concerns
- Multiple modules affected
- Complexity higher than expected

**See**: `.claude/WORKFLOW_ROUTING_GUIDE.md` for complete routing guide and sizing examples.

## Directory Structure

Understanding where files go in the feature development process:

```
ewe-android-eb/
├── .claude/                                    # System files (DO NOT MODIFY)
│   ├── COORDINATOR_AGENT.md                    # Master orchestrator
│   ├── FEATURE_PROMPT_TEMPLATE.md             # Template to copy
│   ├── SETUP.md                               # Setup guide
│   ├── JIRA_INTEGRATION.md                    # Jira integration guide
│   ├── README.md                              # This file
│   ├── EXAMPLE_USAGE.md                       # Usage example
│   ├── NFR_CHECKLIST.md                       # NFR checklist
│   └── agents/                                # Agent instructions
│       ├── PRODUCT_OWNER.md
│       ├── ARCHITECT.md
│       ├── SYNTHESIS.md
│       ├── SENIOR_DEV.md
│       ├── QA.md
│       ├── DEVELOPER.md
│       └── QA_IMPLEMENTATION.md
│
├── docs/
│   ├── feature-requests/                      # YOUR FILLED TEMPLATES GO HERE
│   │   ├── loyalty-points.md                  # ← You create this from template
│   │   ├── dark-mode.md                       # ← You create this from template
│   │   └── ...                                # (Optional: delete after starting)
│   │
│   └── features/                              # GENERATED ARTIFACTS GO HERE
│       ├── android-123-loyalty-points/        # ← Coordinator creates this
│       │   ├── requirements/
│       │   │   └── PRD_DRAFT.md
│       │   ├── architecture/
│       │   │   ├── proposals/
│       │   │   ├── FINAL_ARCHITECTURE.md
│       │   │   └── ADR.md
│       │   ├── review/
│       │   │   ├── nfr-assessment-security-performance.md
│       │   │   └── nfr-assessment-testability-maintainability.md
│       │   ├── testing/
│       │   │   ├── unit-integration-tests.md
│       │   │   ├── ui-e2e-tests.md
│       │   │   └── TEST_RESULTS.md
│       │   ├── implementation/
│       │   │   └── IMPLEMENTATION_SUMMARY.md
│       │   └── final/
│       │       ├── PRD.md
│       │       ├── prd.json
│       │       └── SUMMARY.md
│       └── ...                                # More features
│
├── lib/                                        # Implementation code
│   └── <module>/
│       ├── src/main/java/                     # ← Developer agent writes here
│       │   └── com/expedia/bookings/...
│       ├── src/test/java/                     # ← QA agent writes unit tests here
│       │   └── com/expedia/bookings/...
│       └── src/androidTest/java/              # ← QA agent writes UI tests here
│           └── com/expedia/bookings/...
│
└── project/                                    # Brand-specific code
    └── <brand>/src/...
```

### File Flow Summary

1. **You create**: `docs/feature-requests/my-feature.md` (from template)
2. **You start**: Copy contents into Claude + coordinator command
3. **System creates**: `docs/features/<jira-id-feature-name>/` (all planning docs)
4. **System writes**: Code in `lib/` and `project/` modules
5. **System writes**: Tests in `src/test/` and `src/androidTest/`
6. **You review**: All artifacts in `docs/features/<jira-id-feature-name>/`
7. **You decide**: Keep or delete `docs/feature-requests/my-feature.md`

### Important Notes

- ✅ **`.claude/`** - Never modify. These are system files.
- ✅ **`docs/feature-requests/`** - Your workspace for creating requests
- ✅ **`docs/features/`** - Generated by system. Review but don't manually create.
- ✅ **`lib/` and `project/`** - System writes actual code here

## Integration with Existing Workflow

### With Jira

- Start with Jira story ID
- Link artifacts back to Jira
- Use Jira for implementation tracking

### With Ralph System

- Output includes `prd.json` in Ralph format
- Load into Ralph for implementation tracking
- Use Ralph for progress monitoring

### With PR Process

- Include ADR in PR description
- Reference NFR assessments in code review
- Link test plans in PR comments

## Future Enhancements

Potential additions to the system:

- [ ] API documentation generation
- [ ] Database schema design agent
- [ ] UI mockup generation
- [ ] Implementation task breakdown
- [ ] Code generation for boilerplate
- [ ] Automated PR creation
- [ ] Integration with CI/CD

## Contributing

To improve this system:

1. Test with real features
2. Document issues and edge cases
3. Propose enhancements to agent instructions
4. Add new agent types as needed
5. Refine validation criteria

## Support

For issues or questions:
- Check this README first
- Review agent instructions in `agents/`
- Examine example outputs
- Consult with team leads

## Version History

- **v1.0** (2025-02-17): Initial system creation
  - 6 agent types (PO, Architect x3, Synthesis, Senior Dev x2, QA x2)
  - 6-phase process
  - Comprehensive NFR checklist
  - Autonomous orchestration

---

**Remember**: This system is designed to run autonomously. Trust the process, but always review outputs for quality and appropriateness.
