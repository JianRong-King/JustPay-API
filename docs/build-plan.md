# JustPay — Build Plan

## Context
Empty folder (only `CLAUDE.md`). Goal: build JustPay multi-currency wallet API exactly as specified in `CLAUDE.md`, in small approved steps, learning mode.

Decisions:
- JDK 25 installed locally; compile with `maven.compiler.release=21`. CI uses JDK 21.
- Scaffold via Spring Initializr (start.spring.io zip), Spring Boot 3.5.x.
- `git init`; one commit per step after `./mvnw verify` passes.
- Append an entry to `docs/ai-log.md` every step.

Per-step loop: explain what/why → wait for approval → build → `./mvnw verify` → explain code + concepts → log → commit.

## Steps

### Step 0 — Project setup
- `git init`, `.gitignore`.
- Initializr deps: Web, Data JPA, Validation, PostgreSQL driver, Flyway, Testcontainers. Add `springdoc-openapi-starter-webmvc-ui` to `pom.xml` (in approved stack).
- `docker-compose.yml`: postgres:16, port 5432.
- `application.yml`: datasource, `ddl-auto=validate`, Flyway on, UTC timezone.
- Test base `IntegrationTest`: `@SpringBootTest`, `@AutoConfigureMockMvc`, static `PostgreSQLContainer` with `@ServiceConnection`, table cleanup between tests.
- Smoke test: context loads.
- Create `docs/ai-log.md`.

### Step 1 — Common building blocks (`common/`)
- `Currency` enum (MYR, USD, SGD).
- `ErrorResponse(code, message)` record.
- `ApiException` (code + HTTP status) with subclasses or factory methods.
- `GlobalExceptionHandler` (`@RestControllerAdvice`): ApiException, bean validation → 400, bad JSON / bad enum → 400.
- Amount validation: `> 0`, max 2 decimals (`@Positive` + `@Digits(integer=17, fraction=2)`).

### Step 2 — Accounts (`account/`)
- `V1__create_accounts_and_balances.sql` (UNIQUE, CHECK amount >= 0).
- `Account`, `Balance` entities + repositories.
- `AccountService.create` → account + 3 balances at 0 in one `@Transactional`.
- `POST /api/v1/accounts`, `GET /api/v1/accounts/{id}` (404 `ACCOUNT_NOT_FOUND`).
- Test: create → MYR, USD, SGD at 0.

### Step 3 — Ledger + deposits (`ledger/`, `account/`)
- `V2__create_ledger_entries.sql` (`transfer_id` column, FK added in Step 5).
- `LedgerEntry` entity (insert-only), `LedgerRepository`.
- `POST /api/v1/accounts/{id}/deposits` → balance += amount, one `DEPOSIT` entry.
- `GET /api/v1/accounts/{id}/ledger?currency=` newest first.
- Ledger invariant check in `IntegrationTest` `@AfterEach`: every balance == sum of its ledger entries.
- Tests: deposit USD → only USD changes + 1 entry; unsupported currency → 400.

### Step 4 — FX rates (`fx/`)
- `V3__create_fx_rates.sql` + 6 seeded rates.
- `FxRate` entity, `FxRateRepository`, `FxService.convert(amount, from, to)` → `(toAmount, rate)`; same currency → rate 1; else `amount × rate` rounded once, HALF_EVEN, 2dp.
- `GET /api/v1/fx/rates`.
- Unit tests for rounding cases.

### Step 5 — Transfers (`transfer/`)
- `V4__create_transfers.sql` + `ALTER TABLE ledger_entries ADD FOREIGN KEY transfer_id`.
- `Transfer` entity, repository.
- `BalanceRepository` lock query: `@Lock(PESSIMISTIC_WRITE)`; lock lower balance id first.
- `TransferService.transfer` (one `@Transactional`): checks (404, `SAME_BALANCE` 422, `INSUFFICIENT_BALANCE` 422) → convert → debit → credit → save transfer → 2 ledger entries.
- `POST /api/v1/transfers`, `GET /api/v1/transfers/{id}`.
- Tests: same-currency transfer, cross-currency (rounding + rate stored), USD→MYR conversion in one account, `SAME_BALANCE`, insufficient balance (no balance change).

### Step 6 — Idempotency
- Before moving money: look up by `paymentRequestId`.
  - Same details → return original transfer (200, same body).
  - Different details → 409 `PAYMENT_REQUEST_ID_CONFLICT`.
- Race (two identical requests at same time): unique constraint catches it; I will explain options and ask before adding handling.
- Tests: same id twice → one transfer, same response; same id different details → 409.

### Step 7 — CI + docs check
- `.github/workflows/ci.yml`: JDK 21, `./mvnw verify` (Testcontainers runs on Ubuntu runner).
- Confirm Swagger UI at `http://localhost:8080/swagger-ui.html`.

## Critical files
- `pom.xml`, `docker-compose.yml`, `src/main/resources/application.yml`
- `src/main/resources/db/migration/V1..V4__*.sql`
- `src/main/java/com/justpay/{common,account,ledger,fx,transfer}/`
- `src/test/java/com/justpay/IntegrationTest.java` + per-feature test classes
- `docs/ai-log.md`, `.github/workflows/ci.yml`

## Verification
- Docker Desktop running (needed by Testcontainers).
- `./mvnw verify` after every step; all required tests from `CLAUDE.md` exist and pass by Step 6.
- Manual: `docker compose up -d`, `./mvnw spring-boot:run`, exercise endpoints via Swagger UI.
