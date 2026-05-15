# Skill: Changelog Maintenance

## Purpose

Maintain `CHANGELOG.md` in project root from git history using commit date and commit message.

## Inputs

- Git commit history
- Existing `CHANGELOG.md` (if present)

## Output

- Updated root `CHANGELOG.md` with newest commits at top.

## Required Format

Each entry must follow this exact line format:

`- YYYY-MM-DD | <short-hash> | <commit message>`

Section layout:

```markdown
# Changelog

## Unreleased

- ...

## History

- ...
```

## Update Workflow

1. Read existing changelog entries and collect known short hashes.
2. Get commits from git:
   - `git log --date=short --pretty=format:"%ad|%h|%s"`
3. For each commit not already present in changelog:
   - Add one line under `## History` in reverse chronological order.
4. Preserve existing manual notes in `## Unreleased`.
5. Do not rewrite or reword commit messages.
6. Do not drop historical entries unless duplicated by hash.

## Quality Checks

- No duplicate commit hashes.
- Dates are ISO format (`YYYY-MM-DD`).
- Root file path is exactly `CHANGELOG.md`.
- New commits appear before older commits.
