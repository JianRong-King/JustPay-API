---
name: verify
description: Run the full JustPay test suite (./mvnw verify) and report real results. Use after any code change and before claiming tests pass.
---

# /verify — run all tests

1. Check Docker is running (Testcontainers needs it):
   `docker info --format "{{.ServerVersion}}"`
   If it fails, tell the user to start Docker Desktop and stop.
2. Run `./mvnw -B verify` (timeout 10 minutes). It takes about 1 minute.
3. Read the result:
   - Pass: report the total from the Maven `Tests run:` summary line.
   - Fail: for each failing test, read `target/surefire-reports/<class>.txt` and report:
     test name, expected vs actual, the `file:line` of the failing assertion, likely cause.
   - Build error (no tests ran): report the first compiler / Flyway / Spring error.
4. Never say tests pass without this command's output. Never change a test to make it pass.

Output format:
```
verify: PASS — <n> tests, 0 failures
```
or
```
verify: FAIL — <n> tests, <f> failures, <e> errors
- <TestClass>.<method>: <expected vs actual> (<file:line>) — <likely cause>
```
