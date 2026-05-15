# Skill: Git Commit Overview and Push

## Purpose

Prepare, review, and safely push project changes to a Git remote.

This skill reads the Git history and the project task log from `<project-root>/docs/tasks.md`, creates a clear commit overview, shows the overview to the user, and only pushes changes after the user gives an explicit green signal.

It also supports consolidating recent commits, such as combining the last 5 commits into one commit, but only after showing the impact and receiving explicit approval.

## Inputs

- Project root directory
- Git repository initialized in project root
- Remote repository configured, usually `origin`
- Current branch to push
- Optional task log file:
  - `<project-root>/docs/tasks.md`
- Optional consolidation request, for example:
  - consolidate last 5 commits into one
  - squash last 3 commits
  - combine recent commits and push

## Output

- Git repository status summary
- Recent Git history summary
- Task summary from `docs/tasks.md`
- Commit overview prepared for user review
- Optional consolidated commit plan
- Push confirmation status
- Diagnostics if commit, consolidation, or push fails

## Required Files

The following must exist in project root:

- `.git/`

The following file should be read when available:

- `docs/tasks.md`

If `docs/tasks.md` does not exist:
- continue using Git history only
- clearly report that the task log was not found

## Working Directory

All commands must be executed from project root.

## Git Workflow

### 1. Validate Project Root

Run:

```sh
pwd
test -d .git
```

If `.git/` does not exist:
- report failure
- stop execution

---

### 2. Validate Git Availability

Run:

```sh
git --version
```

If Git is unavailable:
- report failure
- stop execution

---

### 3. Capture Current Repository State

Run:

```sh
git status --short
git status --branch --short
git branch --show-current
git remote -v
```

Collect:
- current branch
- staged files
- unstaged files
- untracked files
- configured remotes
- whether branch is ahead/behind remote

If no remote exists:
- report that pushing is not possible until a remote is configured
- stop before any push step

---

### 4. Read Recent Git History

Run:

```sh
git log --oneline --decorate -n 20
git log --stat -n 10
```

Use this to understand:
- recent commits
- affected files
- commit themes
- whether commits look suitable for pushing as-is
- whether consolidation may be appropriate

---

### 5. Read Task Log

If `docs/tasks.md` exists, read it:

```sh
test -f docs/tasks.md
cat docs/tasks.md
```

Extract:
- completed tasks
- pending tasks
- implementation notes
- bug fixes
- feature additions
- refactoring work
- documentation changes
- known issues

If the file is long, summarize only the parts relevant to the current changes and recent commits.

---

### 6. Compare Working Tree With History

Run:

```sh
git diff --stat
git diff --cached --stat
git diff --name-status
git diff --cached --name-status
```

If needed, inspect detailed diffs safely:

```sh
git diff
git diff --cached
```

Use diffs to identify:
- what changed since the last commit
- whether files are staged
- whether generated files, secrets, logs, or temporary files are included
- whether changes match the completed tasks in `docs/tasks.md`

---

### 7. Check for Sensitive or Risky Files

Before committing or pushing, inspect changed files for obvious risks.

Run:

```sh
git status --short
git diff --name-only
git diff --cached --name-only
```

Flag files such as:

```text
.env
*.key
*.pem
*.p12
*.jks
application-prod.properties
application-prod.yml
secrets.*
credentials.*
*.log
target/
build/
node_modules/
```

If risky files are detected:
- warn the user
- do not commit or push until the user explicitly confirms or removes them

---

## Commit Overview Generation

### 8. Create Commit Overview

Create a concise overview using both:

1. Git history and diffs
2. `<project-root>/docs/tasks.md`, when available

The overview should include:

```text
Commit Overview

Branch:
<current branch>

Remote:
<remote name and URL summary>

Repository State:
- Staged files: ...
- Unstaged files: ...
- Untracked files: ...
- Ahead/behind status: ...

Work Completed:
- ...
- ...
- ...

Files Changed:
- path/to/file.java — reason or summary
- path/to/file.md — reason or summary

Task Log Summary:
- Completed tasks found in docs/tasks.md
- Pending tasks still open
- Notes or risks

Suggested Commit Message:
<type>: <short summary>

Suggested Commit Body:
- ...
- ...

Push Plan:
- Commit current changes: yes/no
- Consolidate commits: yes/no
- Push target: <remote>/<branch>
```

Show this overview to the user before making any commit, rewrite, or push operation.

---

## Standard Commit and Push Workflow

### 9. Ask for Explicit Approval Before Commit

Do not create a commit automatically unless the user has clearly requested it.

Valid approval examples:

```text
yes, commit it
commit and show me before push
create the commit
proceed with commit
```

If the user has only asked for an overview:
- show the overview only
- stop execution

---

### 10. Stage Approved Files

Only stage files that are part of the approved change set.

Preferred:

```sh
git add <approved-file-1> <approved-file-2>
```

Avoid using this automatically:

```sh
git add .
```

Use `git add .` only if:
- the user explicitly approves staging all changes
- risky files have been checked

---

### 11. Create Commit

Run:

```sh
git commit -m "<commit subject>" -m "<commit body>"
```

After commit, verify:

```sh
git status --short
git log --oneline -n 5
```

Report:
- commit hash
- commit message
- remaining uncommitted changes, if any

---

### 12. Ask for Explicit Approval Before Push

Never push automatically after creating or rewriting commits unless the user gives a clear green signal.

Valid approval examples:

```text
green signal, push
push the changes
yes, push now
looks good, push
proceed with push
```

If approval is missing:
- stop after showing the commit overview or commit result
- ask for confirmation

---

### 13. Push Changes

Run:

```sh
git push origin <current-branch>
```

If the branch has no upstream, ask the user before setting upstream, or proceed only if the user requested a normal first push.

```sh
git push -u origin <current-branch>
```

After push, verify:

```sh
git status --branch --short
git log --oneline --decorate -n 5
```

Success condition:
- local branch is no longer ahead of remote
- push command completes successfully

---

## Commit Consolidation Workflow

Use this workflow when the user asks to consolidate commits, for example:

```text
consolidate last 5 commits into one then push
squash last 5 commits and push
combine my last commits into one clean commit
```

### 14. Validate Consolidation Request

Identify the number of commits requested.

Example:

```text
last 5 commits
```

Run:

```sh
git log --oneline -n 5
git status --branch --short
```

Check whether the commits may already be pushed:

```sh
git status --branch --short
git branch -vv
```

If commits were already pushed/shared:
- warn the user that rewriting history may affect collaborators
- require explicit approval before force pushing

---

### 15. Show Consolidation Overview Before Rewriting History

Before squashing, show:

```text
Consolidation Overview

Branch:
<current branch>

Commits to Consolidate:
- <hash> <message>
- <hash> <message>
- <hash> <message>
- <hash> <message>
- <hash> <message>

Combined Work Summary:
- ...
- ...

Files Affected Across These Commits:
- ...

Suggested New Commit Message:
<type>: <short summary>

Suggested Commit Body:
- ...
- ...

History Rewrite Warning:
This will replace the last <N> commits with one new commit.
If these commits were already pushed, a force-with-lease push will be required.
```

Stop and wait for explicit approval.

Valid approval examples:

```text
yes, squash them
approved, consolidate
rewrite history and continue
green signal to consolidate
```

---

### 16. Consolidate Commits Safely

Preferred non-interactive method:

```sh
git reset --soft HEAD~<N>
git commit -m "<new commit subject>" -m "<new commit body>"
```

Then verify:

```sh
git log --oneline -n 10
git status --branch --short
```

Report:
- old commits that were consolidated
- new commit hash
- new commit message
- branch ahead/behind status

---

### 17. Push After Consolidation

If history was not previously pushed, run:

```sh
git push origin <current-branch>
```

If history was already pushed and the user explicitly approved rewriting remote history, run:

```sh
git push --force-with-lease origin <current-branch>
```

Never use plain force push automatically:

```sh
git push --force
```

After push, verify:

```sh
git status --branch --short
git log --oneline --decorate -n 5
```

---

## Safety Rules

Allowed automatic read-only operations:

```sh
pwd
test -d .git
git --version
git status --short
git status --branch --short
git branch --show-current
git branch -vv
git remote -v
git log --oneline --decorate -n 20
git log --stat -n 10
git diff --stat
git diff --cached --stat
git diff --name-status
git diff --cached --name-status
test -f docs/tasks.md
cat docs/tasks.md
```

Allowed after user approval:

```sh
git add <files>
git commit -m "..."
git push origin <branch>
git push -u origin <branch>
```

Operations requiring explicit history rewrite approval:

```sh
git reset --soft HEAD~<N>
git rebase -i HEAD~<N>
git push --force-with-lease origin <branch>
```

Never perform automatically:

```sh
git reset --hard
git clean -fd
git clean -fdx
git push --force
git branch -D <branch>
git tag -d <tag>
git push origin --delete <branch>
```

Never push if:
- the commit overview has not been shown
- the user has not given a clear green signal
- risky files are detected and unresolved
- the remote or branch target is unclear
- the repository has merge conflicts
- the branch is behind remote and push would be rejected or unsafe

---

## Quality Checks

Before commit:

- `.git/` exists
- current branch is known
- changed files are reviewed
- risky files are checked
- `docs/tasks.md` is read when present
- overview is shown to the user
- approval is received

Before push:

- latest commit hash is shown
- push target is clear
- user gives explicit green signal
- history rewrite risk is disclosed when relevant
- force-with-lease is used instead of force push when rewriting remote history

After push:

- push command succeeds
- branch status is verified
- final commit hash is reported

---

## Failure Reporting Format

If commit preparation fails, report:

```text
Git commit preparation failed.

Repository status:
...

Detected issue:
...

Suggested next action:
...
```

If commit fails, report:

```text
Git commit failed.

Repository status:
...

Git error:
...

Suggested next action:
...
```

If consolidation fails, report:

```text
Git consolidation failed.

Requested consolidation:
...

Repository status:
...

Git error:
...

Recovery suggestion:
...
```

If push fails, report:

```text
Git push failed.

Branch:
...

Remote:
...

Git error:
...

Suggested next action:
...
```

---

## User-Facing Confirmation Format

Before pushing, always show:

```text
Ready to push.

Branch:
<branch>

Remote target:
origin/<branch>

Commit(s) to push:
- <hash> <message>

Overview:
- ...
- ...

Please confirm with: "green signal, push".
```

After successful push, show:

```text
Push completed successfully.

Branch:
<branch>

Remote target:
origin/<branch>

Pushed commit:
<hash> <message>

Repository status:
<clean / remaining changes / ahead-behind status>
```