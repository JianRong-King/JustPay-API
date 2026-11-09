---
name: explain
description: Explain a file, class, flow or concept in this project in plain language for learning.
argument-hint: <file, class, flow or concept>
disable-model-invocation: true
---

# /explain — learning-mode explanation

Topic: $ARGUMENTS

1. Read the actual code involved. Quote short snippets with `file:line`. Never explain from memory.
2. Structure:
   - **What it is** — one or two sentences.
   - **Where it is used** — the request path through controller → service → repository → database.
   - **How it works** — step by step, in plain language.
   - **Concepts** — each Spring / JPA / PostgreSQL / Java idea used (e.g. `@Transactional`,
     `@Lock(PESSIMISTIC_WRITE)`, `BigDecimal.compareTo`, Flyway, Testcontainers):
     what it does and what would break without it.
   - **Rule link** — which rule in `.claude/rules/` this code satisfies.
3. Use a small concrete example with real values (e.g. 10.01 USD → 44.04 MYR).
4. End with 2 quick check questions for the user to test their understanding.
5. Do not change any code.
