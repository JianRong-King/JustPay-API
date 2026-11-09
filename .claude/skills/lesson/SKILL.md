---
name: lesson
description: Turn a mistake the user just corrected into a "Lessons learned" rule for CLAUDE.md.
argument-hint: [what went wrong]
disable-model-invocation: true
---

# /lesson — record a lesson learned

Mistake: $ARGUMENTS (if empty, use the most recent correction in this conversation).

1. State in one sentence what went wrong and what the user corrected.
2. Draft one rule:
   ```
   - [YYYY-MM-DD] Rule: <imperative, specific, checkable>. (Why: <the real mistake>)
   ```
   Good rule: specific and testable ("Look up balance ids with a scalar query before locking").
   Bad rule: vague ("Be careful with locks").
3. Check for duplicates in CLAUDE.md "Lessons learned" and `.claude/rules/`.
4. Suggest where it belongs:
   - Applies to every session → CLAUDE.md "Lessons learned".
   - Applies to one area → the matching `.claude/rules/*.md` file.
   - Can be checked by a script → also suggest a hook in `.claude/hooks/check-java-rules.sh`.
5. Show the draft and WAIT for approval. Only then edit the file.
6. Run `/ai-log` with the title "Lesson — <short>".
