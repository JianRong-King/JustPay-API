---
paths:
  - "src/main/resources/db/migration/**"
  - "src/main/java/**/*Repository.java"
  - "src/main/resources/application.yml"
---
# Database rules — JustPay

## Database

- All schema changes go through Flyway: `V{number}__{description}.sql`.
- Never edit a migration that has already been applied. Create a new one.
- `spring.jpa.hibernate.ddl-auto=validate`.
