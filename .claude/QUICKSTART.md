# Quick Start Guide

Get started with feature development in 3 steps.

## Prerequisites

1. **Enable Agent Teams** (one-time):
   ```bash
   # Add to ~/.config/claude-code/settings.json
   {
     "env": {
       "CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS": "1"
     }
   }
   ```
   Then restart your terminal.

2. **Create Feature Requests Directory** (one-time):
   ```bash
   cd /path/to/ewe-android-eb
   mkdir -p docs/feature-requests
   ```

## Start a Feature

### Step 1: Create Your Feature Request

Copy the template:
```bash
cp .claude/FEATURE_PROMPT_TEMPLATE.md docs/feature-requests/my-feature.md
```

Edit `docs/feature-requests/my-feature.md` and fill in:
- Jira story ID (system fetches details automatically)
- Any additional requirements not in Jira
- References (Figma, API docs, etc.)
- Target brands and release

**Minimal example with Jira** (`docs/feature-requests/loyalty-points.md`):
```markdown
# Feature Development Request

## Jira Story
**ID**: ANDROID-456

## Context
**Target Brands**: All brands
**Target Release**: 2025.Q2
```

### Step 2: Start Claude Code

```bash
claude
```

### Step 3: Reference and Run

In Claude, provide this command:

```
@.claude/COORDINATOR_AGENT.md

Execute the feature development process using the requirements in:
docs/feature-requests/my-feature.md
```

**No copy-paste needed!** The coordinator reads the file directly.

That's it! The system runs autonomously through all 8 phases.

## What Gets Created

### Your Input
```
docs/feature-requests/
└── my-feature.md          ← You create this
```

### System Output
```
docs/features/
└── <jira-id-feature-name>/    ← System creates this
    ├── requirements/
    ├── architecture/
    ├── review/
    ├── testing/
    ├── implementation/
    └── final/
        ├── PRD.md
        └── prd.json

lib/<module>/src/
├── main/java/             ← Implementation code
├── test/java/             ← Unit tests
└── androidTest/java/      ← UI tests
```

## After Completion

Review artifacts:
```bash
# View the final PRD
cat docs/features/<jira-id-feature-name>/final/PRD.md

# View implementation summary
cat docs/features/<jira-id-feature-name>/implementation/IMPLEMENTATION_SUMMARY.md

# View test results
cat docs/features/<jira-id-feature-name>/testing/TEST_RESULTS.md
```

Optional cleanup:
```bash
# Delete your request file (optional)
rm docs/feature-requests/my-feature.md
```

## What Happens Autonomously

1. ✅ Fetches Jira story automatically
2. ✅ Creates enriched PRD with user stories
3. ✅ Generates 3 architecture proposals (agent team)
4. ✅ Synthesizes final architecture
5. ✅ Validates all NFRs (agent team)
6. ✅ Creates test plans (agent team)
7. ✅ Writes implementation code
8. ✅ Writes and executes tests (agent team)
9. ✅ Generates complete documentation

**Result**: Fully tested, production-ready feature with comprehensive documentation!

## Troubleshooting

**Issue**: "Cannot fetch Jira story"
- **Fix**: Authenticate with Atlassian MCP server

**Issue**: "Agent teams not working"
- **Fix**: Verify `CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1` is set and terminal restarted

**Issue**: "Permission prompts"
- **Fix**: Configure permissions in `settings.json` (see `.claude/SETUP.md`)

## Learn More

- **Complete Guide**: `.claude/README.md`
- **Jira Integration**: `.claude/JIRA_INTEGRATION.md`
- **Setup Details**: `.claude/SETUP.md`
- **Example Walkthrough**: `.claude/EXAMPLE_USAGE.md`

---

**Pro Tip**: Keep `docs/feature-requests/` in version control to track feature request history, or add to `.gitignore` if you prefer to delete after starting.
