# PR Description Generator

**PURPOSE**: Automatically generate a comprehensive Pull Request description based on changes in the current branch.

---

## Usage

**Simple invocation** (just say this to Claude):
```
Generate a PR description for the current branch
```

Or with the template reference:
```
@.claude/templates/PR_DESCRIPTION_GENERATOR.md

Generate PR description for current branch
```

---

## What This Does

This task will:

1. **Analyze Current Branch**
   - Run `git diff main...HEAD` (or your base branch)
   - Identify changed files and change scope
   - Extract commit messages for context

2. **Check for Feature Documentation** (if using FARO workflow)
   - Look for feature docs in `docs/features/*/`
   - Read PRD, architecture, and implementation summary
   - Extract acceptance criteria and NFR compliance

3. **Generate PR Description** with:
   - **Title**: Clear, concise (< 70 characters)
   - **Summary**: What was changed and why
   - **Changes**: Bulleted list of key changes
   - **Testing**: Test coverage and validation
   - **Reviewer Notes**: What to focus on
   - **Checklist**: Pre-merge checklist

4. **Output Location**
   - Creates: `PR_DESCRIPTION.md` in repo root
   - Or directly creates PR with `gh pr create` (optional)

---

## Instructions for Claude

When this template is invoked, follow these steps:

### Step 1: Gather Context

```bash
# Get current branch and compare to main
git branch --show-current
git log main..HEAD --oneline
git diff main...HEAD --stat

# Check for merge conflicts or issues
git status
```

### Step 2: Search for Feature Documentation

```bash
# Check if this is part of FARO workflow
Glob: "docs/features/*/IMPLEMENTATION_SUMMARY.md"
Glob: "docs/features/*/requirements/PRD_DRAFT.md"
Glob: "docs/features/*/architecture/FINAL_ARCHITECTURE.md"
```

If found, read:
- PRD for acceptance criteria and user stories
- Architecture for design decisions
- Implementation summary for what was built
- Test plans for coverage

### Step 3: Analyze Code Changes

Focus on:
- **Scope**: Which modules/packages changed?
- **Type**: Feature, bug fix, refactor, tech debt?
- **Complexity**: Lines changed, files touched
- **Risk areas**: Database migrations, API changes, breaking changes
- **Dependencies**: New libraries, version updates

### Step 4: Generate PR Description

Use this structure:

```markdown
# [Type]: [Brief Description]

## Summary

[2-3 sentences: What was changed and why]

**Jira**: [ANDROID-XXX] (if applicable)
**Feature Docs**: [link to docs/features/*/] (if applicable)

## Changes

### [Module/Area 1]
- Change 1
- Change 2

### [Module/Area 2]
- Change 1
- Change 2

## Architecture Decisions

[If ADR exists, summarize key decisions]

## Testing

- [ ] Unit tests: [X files, Y% coverage]
- [ ] Integration tests: [scenarios covered]
- [ ] UI tests: [screens/flows covered]
- [ ] Manual testing: [what was tested]

### Test Evidence
[Link to test results or describe testing done]

## NFR Compliance

[If from FARO workflow, summarize NFR validation]
- **Security**: [status]
- **Performance**: [status]
- **Accessibility**: [status]
- **Scalability**: [status]

## Breaking Changes

[List any breaking changes or state "None"]

## Migration Guide

[If applicable, provide migration steps]

## Reviewer Notes

**Focus areas for review**:
1. [Specific area to review carefully]
2. [Another area]

**Known limitations**:
- [Any known issues or future work]

## Screenshots / Demo

[Add screenshots for UI changes or link to demo]

## Pre-merge Checklist

- [ ] All tests passing
- [ ] No merge conflicts
- [ ] Documentation updated
- [ ] Feature flags configured (if applicable)
- [ ] Monitoring/logging added
- [ ] Security review completed (if flagged)
- [ ] Performance validated (if flagged)

---

🤖 Generated with [Claude Code](https://claude.com/claude-code)
```

### Step 5: Create Output

**Option A**: Write to file
```bash
Write: PR_DESCRIPTION.md
```

**Option B**: Create PR directly (if requested)
```bash
gh pr create --title "[Title]" --body "$(cat PR_DESCRIPTION.md)"
```

---

## Configuration

### Base Branch
By default, compares against `main`. To use a different base:
```
Generate PR description comparing against develop
```

### Include/Exclude Files
To focus on specific files:
```
Generate PR description for changes in src/mobile/android/
```

### Create PR Immediately
To create the PR (not just generate description):
```
Generate PR description and create the PR
```

---

## Example Invocations

**Basic**:
```
Generate a PR description for the current branch
```

**With custom base branch**:
```
Generate PR description comparing against develop branch
```

**For specific module**:
```
Generate PR description for loyalty module changes only
```

**Create PR immediately**:
```
Generate PR description and create PR against main
```

**With custom title**:
```
Generate PR description titled "feat: Add loyalty points display to home screen"
```

---

## Notes

- **Feature Documentation**: If this branch is from the FARO workflow, the generator will automatically pull content from feature docs
- **Commit Messages**: Uses commit messages as hints for what changed
- **Smart Defaults**: Automatically detects change type (feature/bugfix/refactor)
- **Reviewer Context**: Focuses on providing reviewers with actionable information

---

## Output Location

**Generated file**: `PR_DESCRIPTION.md` (repo root)

You can then:
1. Review and edit the generated description
2. Copy/paste to GitHub PR
3. Or use `gh pr create --body-file PR_DESCRIPTION.md`

---

**No setup needed!** Just invoke this template when you're ready to create a PR.
