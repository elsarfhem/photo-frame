# Feature Development Request

**INSTRUCTIONS**:
1. Copy this template to `docs/feature-requests/<your-feature>.md`
2. Fill out the template with your feature details
3. Start Claude and reference the file path - no copy-paste needed!

**Example usage**:
```
@.claude/COORDINATOR_AGENT.md

Execute the feature development process using the requirements in:
docs/feature-requests/my-feature.md
```

**NEW**: The system will automatically fetch your Jira story details using the Atlassian MCP server. You only need to provide the Jira ID - title, description, and acceptance criteria will be fetched automatically!

---

## Jira Story

**ID**: [e.g., ANDROID-123]

**IMPORTANT**: The coordinator will automatically fetch this story from Jira using the MCP server. You don't need to paste the description manually - just provide the ID!

**Manual Override** (only if you want to add details not in Jira):

**Additional Title Context**: [Optional - only if you want to clarify]

**Additional Description**:
```
[Optional - only if Jira is incomplete or you want to add context]
```

---

## Additional Functional Requirements

**NOTE**: The system will use Jira story as the primary source. Only add requirements here that are NOT already in Jira or need clarification:

1. [Additional requirement 1]
2. [Additional requirement 2]
3. [Additional requirement 3]
...

---

## References

### Design Materials
- Figma: [URL or path]
- Design specs: [URL or path]
- Brand guidelines: [relevant brands]

### Technical References
- API documentation: [URL or path]
- Related features: [links or file paths]
- Dependencies: [external services, libraries]

### Similar Implementations
- [Reference to similar feature in codebase]
- [Reference to similar feature in other apps]

---

## Context

**Target Brands**:
- [ ] All brands
- [ ] Expedia only
- [ ] Hotels.com only
- [ ] Vrbo only
- [ ] Other: [specify]

**Target Release**: [version or date]

**Priority**: [P0/P1/P2/P3]

**Estimated Complexity**: [Low/Medium/High/Unknown]

---

## Constraints & Assumptions

List any known constraints or assumptions:

- [Constraint 1]
- [Assumption 1]
...

---

## Special Instructions

Any special instructions for the development team:

[Additional context, preferences, or requirements]

---

## Start Command

**After saving this file, start Claude and use this command:**

```
@.claude/COORDINATOR_AGENT.md

Execute the feature development process using the requirements in:
docs/feature-requests/<your-feature-file-name>.md
```

**No copy-paste needed!** Just point to the file path and the coordinator will read it.
