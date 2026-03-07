# Jira Integration Guide

This system uses the **Atlassian MCP server** to directly access Jira stories, eliminating manual copy-paste and ensuring you always work with current data.

## How It Works

1. **You provide**: Just the Jira issue ID (e.g., "ANDROID-123")
2. **System fetches**: Full story details from Jira automatically
3. **Product Owner uses**: Jira data as the primary source for requirements
4. **Optional**: System can update Jira with implementation results

## What Gets Fetched from Jira

When you provide a Jira issue ID, the coordinator automatically fetches:

- ✅ **Story title and description**
- ✅ **Acceptance criteria** (if defined in Jira)
- ✅ **Labels** (priority, components, epic)
- ✅ **Status and workflow state**
- ✅ **Linked issues** (blocks, is blocked by, relates to)
- ✅ **Attachments** (design files, documents)
- ✅ **Comments** (team discussions)
- ✅ **Custom fields** (story points, sprint, etc.)

## Setup Requirements

### 1. Atlassian MCP Server Must Be Available

The Atlassian MCP server should already be configured in your environment. Verify:

```bash
# Check if Atlassian MCP tools are available
# The coordinator will use these MCP tools:
# - mcp__atlassian__getAccessibleAtlassianResources
# - mcp__atlassian__getJiraIssue
# - mcp__atlassian__addCommentToJiraIssue
```

### 2. Authentication

You must be authenticated to your Atlassian/Jira instance. The MCP server handles authentication automatically through your browser or stored credentials.

**First Time Setup**:
- The MCP server will prompt you to authenticate
- Log in via browser when prompted
- Credentials are securely stored for future use

### 3. Permissions

Your Jira account needs:
- **Read access** to the projects you're working on (required)
- **Comment access** to add updates (optional, for Jira updates)

## Usage Example

### Minimal Input Required

Instead of pasting the entire Jira story, just provide the ID:

```markdown
# Feature Development Request

## Jira Story
**ID**: ANDROID-456

## Additional Functional Requirements
- [Only if you need to add something NOT in Jira]

## References
- Figma: https://...

## Context
**Target Brands**: All brands
**Target Release**: 2025.Q2

---
@.claude/COORDINATOR_AGENT.md
Execute the feature development process for the requirements specified above.
```

### What Happens

1. **Phase 0 (Initialization)**:
   ```
   Coordinator: Fetching Jira story ANDROID-456...
   ✅ Fetched: "Add loyalty points display to profile"
   ✅ Description: [Full description from Jira]
   ✅ Acceptance Criteria: 5 criteria found
   ✅ Labels: android, profile, p1
   ✅ Linked Issues: ANDROID-455 (blocks)
   ```

2. **Phase 1 (Requirements)**:
   ```
   Product Owner: Using Jira story as primary source...
   - Title: "Add loyalty points display to profile"
   - Existing AC from Jira: 5 criteria
   - Enriching with user stories format...
   - Adding implementation details...
   ✅ PRD created with Jira data + enhancements
   ```

3. **All subsequent phases** work with the enriched requirements

## Benefits

### 1. No Manual Copy-Paste
- Before: Copy story from Jira → Paste into prompt → Hope it's up to date
- Now: Provide ID → System fetches latest data automatically

### 2. Always Current
- Jira story updated? No problem - system fetches latest version
- Acceptance criteria added? Automatically included
- Story description changed? Always uses current version

### 3. Preserves Metadata
- Links to related stories maintained
- Labels and priority preserved
- Attachments referenced
- Comments visible to Product Owner

### 4. Traceability
- PRD explicitly shows Jira source
- Easy to link back to original story
- Implementation tied to specific Jira issue

## Optional: Update Jira After Implementation

At the end of the process (Phase 9), you can optionally update the Jira story:

```markdown
After completion, add this to your prompt:

"Also, add a comment to the Jira story with:
- Link to the PRD document
- Implementation summary
- Test results
- Link to created artifacts"
```

The coordinator will use `mcp__atlassian__addCommentToJiraIssue` to update the story.

## Example Jira Comment Added

```
🤖 Automated Implementation Update

Feature implementation completed via Claude Code autonomous development system.

📋 Artifacts:
- PRD: docs/features/android-456-loyalty-points/final/PRD.md
- Architecture: docs/features/android-456-loyalty-points/architecture/FINAL_ARCHITECTURE.md
- Implementation: 12 files created, 3 files modified

✅ Test Results:
- Unit Tests: 28/28 passed
- Integration Tests: 12/12 passed
- UI Tests: 15/15 passed
- Coverage: 87%

📊 Code Metrics:
- Files Created: 12
- Files Modified: 3
- Tests Written: 55
- Test Coverage: 87%

Ready for code review and QA testing.
```

## Troubleshooting

### Issue: "Cannot fetch Jira story"

**Cause**: MCP server not authenticated or no access to project

**Solution**:
1. Check if you're logged into Atlassian
2. Verify you have read access to the project
3. Try fetching the story manually in Jira web UI first

### Issue: "Jira story is empty"

**Cause**: Story has minimal content in Jira

**Solution**:
- Add more details in Jira first, OR
- Provide additional requirements in the prompt to supplement

### Issue: "MCP tool not found"

**Cause**: Atlassian MCP server not configured

**Solution**:
- Verify MCP server is set up correctly
- Check Claude Code MCP configuration
- Contact your admin if MCP server needs setup

## Advanced: Search Jira for Stories

You can also search for stories using JQL:

```markdown
Search for related stories first:

Use JQL: "project = ANDROID AND component = Profile AND status = 'To Do'"

Then implement the ones you select.
```

The coordinator can use `mcp__atlassian__searchJiraIssuesUsingJql` to find relevant stories.

## Privacy & Security

- **Authentication**: Handled securely by MCP server
- **Permissions**: Uses your existing Jira permissions
- **No storage**: Jira data is fetched in real-time, not stored
- **Read-only by default**: Write operations (comments) only if explicitly requested

## Best Practices

1. **Keep Jira Updated**: The better your Jira story, the better the PRD
2. **Use Acceptance Criteria**: Define AC in Jira - system will use them
3. **Link Dependencies**: Link related stories - system preserves links
4. **Add Context**: Use Jira description field for detailed context
5. **Update After**: Add implementation results back to Jira for traceability

---

With Jira integration, the feature development process becomes even more seamless - just provide an ID and let the system do the rest!
