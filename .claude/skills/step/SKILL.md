---
name: step
description: Run one learning-mode development step end to end (plan, approval, build, verify, review, explain, log, commit).
argument-hint: <what to build>
disable-model-invocation: true
---

# /step — one learning-mode step

Goal: $ARGUMENTS

Follow these phases in order. Do not skip or merge phases.

## 1. Plan (then STOP)
- Read the rule files in `.claude/rules/` that this change touches.
- Read the code you will change. Reuse what exists.
- Tell the user in plain language:
  - What you will build and why.
  - Files you will create or change.
  - Tests you will add (names in `should_<expected>_when_<condition>` form).
  - Anything unclear → ask. Do not guess.
- Wait for approval. Do not write code before the user says yes.

## 2. Build
- Smallest correct change. No new dependencies without asking.
- Schema change → use `/new-migration`. Never edit an applied migration.
- Write the tests together with the code.

## 3. Verify
- Run `/verify`. If tests fail, fix the code, not the test. If a test seems wrong, stop and ask.

## 4. Review
- Run the `money-rules-reviewer` and `test-guardian` agents on the diff in parallel.
- Fix real findings, re-run `/verify`. Tell the user which findings you rejected and why.

## 5. Explain
- Each new or changed class: what it does, in one or two sentences.
- Each Spring / JPA / PostgreSQL concept the user may not know (e.g. `@Transactional`,
  pessimistic lock, Flyway): what it does and why it is needed here.

## 6. Log and commit
- Run `/ai-log` for this step.
- Propose a commit message in the repo style: `type(scope): lowercase summary`
  (e.g. `feat(transfer): ...`, `fix(ledger): ...`, `test(account): ...`).
- No `Co-Authored-By` line. Commit only after the user agrees.
