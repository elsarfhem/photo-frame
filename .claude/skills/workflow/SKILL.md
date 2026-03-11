---
name: workflow
description: Execute the FARO AI feature development workflow. Use when starting feature development, processing Jira stories, or running the structured development process. Triggers on workflow, start workflow, run workflow, feature development.
user-invocable: true
---

# FARO AI Feature Development Workflow

Execute the complete feature development process using the Coordinator Agent.

## The Job

Run the structured, gated feature development workflow for a given feature request or Jira story.

**User Request:** {{input}}

## Steps

1. **Verify Project Context**
   - Check if `.claude/COORDINATOR_AGENT.md` exists in current directory
   - If not found, inform user this skill requires the FARO AI workflow project

2. **Read Workflow Instructions**
   - Read `.claude/COORDINATOR_AGENT.md`
   - Read `.claude/WORKFLOW_ROUTING_GUIDE.md`

3. **Determine Workflow Type**
   - Check story size from input (XS/S vs M/L/XL)
   - If SIZE is specified: follow that routing
   - If JIRA ticket provided: fetch story to determine size
   - If unclear: ask user for story size

4. **Execute Coordinator Process**
   - Follow the complete workflow as defined in COORDINATOR_AGENT.md
   - Use simplified workflow for XS/S stories (6 phases)
   - Use full workflow for M/L/XL stories (11 phases)
   - Validate gate criteria before proceeding between phases
   - Spawn agent teams as specified in the workflow

5. **Output**
   - Feature directory with all artifacts
   - Implementation-ready documentation
   - Summary of completion status

## Important Notes

- This skill operates autonomously through multiple phases
- Jira integration is used automatically if Atlassian MCP is available
- Agent teams use odd numbers (3, 5) for consensus-based decisions
- All outputs are stored in `docs/features/<feature-slug>/`

## Example Invocations

```
/workflow ANDROID-123
/workflow Add tooltip to settings button [SIZE: S]
/workflow New user authentication flow with 2FA [SIZE: L]
```

## Error Handling

- If COORDINATOR_AGENT.md not found → Provide clear error message
- If Jira story not accessible → Prompt user for manual requirements
- If consensus not reached → Document disagreement and prompt for resolution
