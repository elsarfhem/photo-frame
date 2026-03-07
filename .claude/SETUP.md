# Setup Guide for Agent-Based Feature Development

This guide walks you through enabling and configuring the feature development system.

## Prerequisites

- Claude Code CLI installed
- Access to this repository (`ewe-android-eb`)
- Basic familiarity with Android development

## Step 1: Enable Agent Teams

The system uses Claude Code's **agent teams** feature for parallel execution. This feature is experimental and must be enabled.

### Option A: Via Settings File (Recommended)

Add to your `~/.config/claude-code/settings.json`:

```json
{
  "env": {
    "CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS": "1"
  }
}
```

### Option B: Via Environment Variable

Add to your shell profile (`~/.bashrc`, `~/.zshrc`, etc.):

```bash
export CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1
```

Then restart your terminal or run `source ~/.bashrc` (or equivalent).

### Verify Setup

```bash
# Check if environment variable is set
echo $CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS

# Should output: 1
```

## Step 2: Choose Display Mode (Optional)

Agent teams support two display modes:

### In-Process Mode (Default)
- All teammates run in your main terminal
- Use Shift+Up/Down to switch between teammates
- Works in any terminal

### Split-Pane Mode
- Each teammate gets its own pane
- See all outputs simultaneously
- Requires tmux or iTerm2

**To enable split-pane mode:**

Install tmux:
```bash
# macOS
brew install tmux

# Ubuntu/Debian
sudo apt-get install tmux

# Or use iTerm2 with it2 CLI
```

Configure in `settings.json`:
```json
{
  "teammateMode": "tmux"
}
```

## Step 3: Configure Permissions (Recommended)

To reduce permission prompts during agent execution, pre-approve common operations in your `settings.json`:

```json
{
  "prompts": {
    "bash": {
      "npm install": "allow",
      "gradle": "allow",
      "./gradlew": "allow",
      "git": "allow"
    },
    "write": {
      "docs/features/**": "allow",
      "lib/**/*.kt": "ask",
      "project/**/*.kt": "ask"
    },
    "edit": {
      "lib/**/*.kt": "ask",
      "project/**/*.kt": "ask"
    }
  }
}
```

Alternatively, run Claude with `--dangerously-skip-permissions` (NOT recommended for production code):

```bash
claude --dangerously-skip-permissions
```

## Step 4: Test the Setup

Test that agent teams work:

```bash
cd /path/to/ewe-android-eb
claude
```

Then in Claude:

```
Create a simple agent team with 2 teammates to explore the codebase structure.
One teammate should focus on the lib/ directory, one on project/ directory.
```

If teammates spawn successfully, you're ready to use the feature development system!

## Step 5: Run Your First Feature Development

Copy the template:

```bash
cp .claude/FEATURE_PROMPT_TEMPLATE.md my-first-feature.md
```

Edit `my-first-feature.md` with your feature details, then:

```bash
claude
```

Paste your filled template and the coordinator will handle the rest!

## Troubleshooting

### Agent teams not spawning

**Problem**: Claude doesn't create teammates

**Solutions**:
1. Verify `CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1` is set
2. Restart your terminal after setting the env var
3. Make sure you're using a recent version of Claude Code

### Too many permission prompts

**Problem**: Constant interruptions for file operations

**Solutions**:
1. Configure prompts in `settings.json` (see Step 3)
2. Use `--dangerously-skip-permissions` for non-production work

### Split panes not working

**Problem**: Teammates not appearing in split panes

**Solutions**:
1. Verify tmux is installed: `which tmux`
2. Try in-process mode first: remove `"teammateMode"` from settings
3. Check iTerm2 Python API is enabled (if using iTerm2)

### Orphaned tmux sessions

**Problem**: tmux sessions persist after team ends

**Solution**:
```bash
tmux ls
tmux kill-session -t <session-name>
```

### Lead shuts down too early

**Problem**: Coordinator stops before all work is done

**Solution**: Tell it to continue:
```
Wait for all teammates to finish their work before proceeding.
```

### Task list issues

**Problem**: Tasks appear stuck even though work is done

**Solution**:
```
Check task status and mark completed tasks as done.
```

## Configuration Options

### Full Settings Example

Here's a complete `settings.json` example optimized for Android development:

```json
{
  "env": {
    "CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS": "1"
  },
  "teammateMode": "in-process",
  "prompts": {
    "bash": {
      "npm install": "allow",
      "gradle": "allow",
      "./gradlew": "allow",
      "git": "allow",
      "mkdir": "allow",
      "cp": "allow"
    },
    "write": {
      "docs/features/**": "allow",
      "lib/**/*.kt": "ask",
      "project/**/*.kt": "ask",
      "**/*Test.kt": "ask"
    },
    "edit": {
      "lib/**/*.kt": "ask",
      "project/**/*.kt": "ask",
      "gradle/libs.versions.toml": "ask"
    }
  },
  "model": "claude-opus-4-6"
}
```

## Next Steps

Once setup is complete:

1. Review `.claude/README.md` for system overview
2. Read `.claude/EXAMPLE_USAGE.md` for a complete walkthrough
3. Try the system with a small feature first
4. Gradually increase feature complexity as you get comfortable

## Support

For issues with:
- **Agent teams feature**: Check [Claude Code docs](https://code.claude.com/docs/en/agent-teams)
- **This system**: Review agent instructions in `.claude/agents/`
- **Android-specific issues**: Check `CLAUDE.md` for codebase context

---

**Remember**: Agent teams are experimental. If you encounter issues, you can always fall back to running the process manually by spawning agents one-at-a-time using the Task tool.
