---
paths:
  - "src/main/java/**"
---
# Java style rules — JustPay

## Code conventions

- Constructor injection only. No `@Autowired` on fields. No Lombok.
- Small methods with clear names. Prefer simple, readable code over clever code.
- Timestamps stored as `Instant` / `TIMESTAMPTZ` in UTC.
