---
name: ai-log
description: Append an entry for the work just done to docs/ai-log.md in the project's existing format. Use at the end of every step or follow-up.
argument-hint: [entry title]
---

# /ai-log — append to docs/ai-log.md

1. Read the last 40 lines of `docs/ai-log.md` to match the style and numbering.
2. Collect facts from this session and `git diff` / `git status` only. Do not invent.
3. Append (never edit old entries) one entry:

```
## <Step N | Follow-up> — <title> (<YYYY-MM-DD>)

Built:
- <file or feature>: <what it does>.

Decisions:
- <choice made> (user) — <why>.

Issues:
- <what went wrong> → <fix>.

Verification:
- `./mvnw verify`: <n> tests pass.
```

Rules:
- Title: $ARGUMENTS if given, otherwise a short summary.
- Use today's date.
- Leave out empty sections.
- Short bullets, file names in backticks, no filler words.
- Only write "tests pass" if `/verify` ran in this session and passed.
