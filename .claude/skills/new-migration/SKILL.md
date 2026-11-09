---
name: new-migration
description: Create the next Flyway migration file (V{n+1}__description.sql). Use for every schema change instead of editing an existing migration.
argument-hint: <short description>
---

# /new-migration — next Flyway migration

Description: $ARGUMENTS

1. List `src/main/resources/db/migration/` and find the highest `V{n}`.
2. Create `V{n+1}__<snake_case_description>.sql`. Never edit an existing `V*` file.
3. Follow the style of the existing migrations:
   - Money columns `NUMERIC(19,2)`, rates `NUMERIC(19,6)`, timestamps `TIMESTAMPTZ`.
   - Currency columns get `CHECK (currency IN ('MYR', 'USD', 'SGD'))`.
   - Name constraints explicitly (`uq_<table>_<cols>`, `chk_<table>_<rule>`, `fk_<table>_<col>`).
   - Add business rules as CHECK constraints where the database can enforce them.
4. If an entity changes, update it so `ddl-auto=validate` still passes.
5. Tell the user: the file name, each statement in plain language, and why a new
   migration is needed instead of editing the old one (Flyway checksums: a changed
   applied file makes the app refuse to start).
6. Run `/verify`.
