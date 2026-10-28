# AI Log

One entry per step: what was built, decisions, mistakes and corrections.

## Step 0 — Project setup (2026-10-27)

Built:
- Maven wrapper and layout from Spring Initializr; `pom.xml` hand-written for Spring Boot 3.5.16, Java release 21.
- `docker-compose.yml` (postgres:16), `application.yml` (`ddl-auto=validate`, `open-in-view=false`, UTC).
- `IntegrationTest` base: one shared Testcontainers Postgres for all test classes, MockMvc.
- Smoke test `should_loadContext_when_applicationStarts`.

Decisions:
- Spring Initializr only offers Boot 4.x now. Kept Boot 3.5.16 to match CLAUDE.md; used Initializr only for wrapper and layout.
- JDK 25 installed locally; compile target is 21.
- Container started in a static block (singleton), not with `@Container`, so it is not stopped between test classes while Spring reuses its cached context.
- `@Autowired` field in the test base class only (MockMvc); production code stays constructor injection.

Issues:
- First `./mvnw verify` failed: Docker Desktop not running. Started it, re-ran, passed.

## Step 1 — Common building blocks (2026-10-27)

Built (`common/`):
- `Currency` enum (MYR, USD, SGD).
- `ErrorResponse(code, message)` record.
- `ApiException(status, code, message)`: one class for all business errors.
- `GlobalExceptionHandler`: ApiException → its status; validation, bad JSON, unknown enum, bad query/path param → 400 `INVALID_REQUEST`; Spring web errors (404 path, 405) keep their status as `REQUEST_ERROR`; anything else → 500 `INTERNAL_ERROR` (logged).

Decisions:
- 400 error codes not specified in CLAUDE.md; chose `INVALID_REQUEST`.
- Amount annotations (`@NotNull @Positive @Digits(integer = 17, fraction = 2)`) go on request DTOs in Step 3, the first DTO with an amount.
- No tests yet: handler needs an endpoint; covered in Steps 2–3.

## Step 2 — Accounts (2026-10-27)

Built:
- `V1__create_accounts_and_balances.sql`: identity ids, UNIQUE (account_id, currency), CHECK currency in MYR/USD/SGD, CHECK amount >= 0.
- `Account`, `Balance` entities; `Balance` stores plain `accountId` (no JPA relation).
- `AccountService.create` saves account + 3 zero balances in one transaction; `get` → 404 `ACCOUNT_NOT_FOUND`.
- `POST /api/v1/accounts` (201), `GET /api/v1/accounts/{id}`.
- `IntegrationTest`: truncates tables before each test; `createAccount` helper.
- Tests: three zero balances (and GET returns the same), 404 unknown id, 400 blank name.

Decisions:
- Kept `INVALID_REQUEST` for all 400s (no change requested).
- Zero balance created as `0.00` (scale 2) and `createdAt` truncated to microseconds, so the create response matches what the database returns later.

## Step 3 — Ledger and deposits (2026-10-27)

Built:
- `V2__create_ledger_entries.sql`: CHECKs on currency, type, amount <> 0, and "DEPOSIT has no transfer_id, transfers have one". Index on (account_id, created_at DESC).
- `ledger/`: `LedgerEntry` (`@Immutable`), `LedgerEntryType`, `LedgerRepository`, `LedgerEntryResponse`.
- `POST /api/v1/accounts/{id}/deposits`: locks the balance row (`PESSIMISTIC_WRITE`), adds amount, writes one `DEPOSIT` entry; returns the account.
- `GET /api/v1/accounts/{id}/ledger?currency=`: newest first, id as tie-breaker.
- `DepositRequest` amount rules: `@NotNull @Positive @Digits(integer = 17, fraction = 2)`.
- `IntegrationTest`: `@AfterEach` SQL check that every balance equals the sum of its ledger entries; `deposit` and `getLedger` helpers.
- Tests: USD-only deposit + 1 entry, EUR → 400, 0 / negative / 3 decimals → 400, unknown account → 404, ledger filter + order, ledger `?currency=EUR` → 400, ledger unknown account → 404.

Decisions:
- Ledger endpoint lives in `AccountController` / `AccountService` (path is under `/accounts`); `ledger/` holds the entity and repository, per CLAUDE.md structure.

## Step 4 — FX rates (2026-10-27)

Built:
- `V3__create_fx_rates.sql`: plain id + UNIQUE (from, to), CHECKs (known currencies, from <> to, rate > 0); 6 seeded rates.
- `fx/`: `FxRate` (`@Immutable`), `FxRateRepository`, `Conversion(toAmount, rate)`, `FxService.convert`, `FxRateResponse`, `GET /api/v1/fx/rates`.
- Conversion: same currency → amount unchanged, rate 1.000000; else `amount × rate`, rounded once to 2dp, HALF_EVEN.
- Tests: 10.01 USD → 44.04 MYR; HALF_EVEN (0.15 MYR → 0.04 SGD, 0.25 MYR → 0.08 SGD); same currency; no round trip gains money (all pairs, several amounts); 100 USD → 440.00 MYR → 96.80 USD; GET returns 6 rates.

Decisions:
- Plain id instead of composite primary key to avoid `@EmbeddedId`.
- Test cleanup does not truncate `fx_rates` (seed data must stay).

## Step 5 — Transfers (2026-10-27)

Built:
- `V4__create_transfers.sql`: UNIQUE payment_request_id, CHECKs (currencies, amounts > 0, rate > 0, not same balance); adds FK `ledger_entries.transfer_id → transfers.id` (new migration, V2 untouched).
- `transfer/`: `Transfer` (`@Immutable`), `TransferRepository`, `TransferRequest`, `TransferResponse`, `TransferService`, `TransferController`.
- `TransferService.transfer` (one transaction): accounts exist (404) → not same balance (422 `SAME_BALANCE`) → convert, reject 0.00 (422 `AMOUNT_TOO_SMALL`) → lock both balances lower id first → enough money (422 `INSUFFICIENT_BALANCE`) → subtract, add, save transfer, 2 ledger entries.
- `GET /api/v1/transfers/{id}` → 404 `TRANSFER_NOT_FOUND`.
- `common/Timestamps.now()` shared by account and transfer services.
- Tests: same-currency, cross-currency (10.01 USD → 44.04 MYR, rate stored, GET equals POST), USD→MYR in one account, SAME_BALANCE, insufficient (nothing changes), whole balance allowed, AMOUNT_TOO_SMALL, unknown account 404, EUR 400, blank paymentRequestId 400, unknown transfer 404.

Decisions (user):
- Converted amount rounding to 0.00 → reject 422 `AMOUNT_TOO_SMALL`.
- Idempotent replay (Step 6) → 201 first, 200 on replay, same body.

Pitfall avoided:
- Balance ids are looked up with a scalar query (`select b.id ...`), not by loading the entity. If the entity were loaded before locking, Hibernate would return that stale in-memory copy from the locked query and the lock would not protect the amount.

## Step 6 — Idempotency (2026-10-27)

Built:
- `TransferRepository.findByPaymentRequestId`; `TransferResult(transfer, created)`.
- `TransferService.transfer`: checks `paymentRequestId` first (same details → replay, different → 409 `PAYMENT_REQUEST_ID_CONFLICT`), and again right after locking balances (catches a concurrent identical request that committed while we waited for the lock).
- Unique violation on `uq_transfers_payment_request_id` at insert → 409 (concurrent request with same id but different balances).
- Controller: 201 new, 200 replay, same body.
- Tests: replay returns same body + money moves once; `30` vs `30.00` is a replay; 4 kinds of different details → 409; replay after balance spent → 200 not 422; 5 identical concurrent requests → one 201, four 200; 20 opposite concurrent transfers → no deadlock, totals unchanged.

Verification:
- Full `./mvnw verify`: 36 tests pass. Concurrency test class re-run 3 more times: all pass.

## Step 7 — CI and Swagger check (2026-10-27)

Built:
- `.github/workflows/ci.yml`: on push / PR, Temurin JDK 21 with Maven cache, `./mvnw -B verify`.
- `mvnw` marked executable in git (`100644` → `100755`) so Linux CI can run it.

Issue and fix:
- `spring-boot:run` failed: "password authentication failed for user justpay". A native PostgreSQL 17 Windows service already listens on `0.0.0.0:5432`; Docker only got `:::5432`, so `localhost` reached the native server.
- User chose: move Docker Postgres to host port 5433 (`docker-compose.yml`, `application.yml`). Tests unaffected (Testcontainers uses random ports).

Verified:
- App starts against Docker Postgres, Flyway applies V1–V4.
- `/swagger-ui.html` → 302 → `/swagger-ui/index.html` 200; `/v3/api-docs` lists all 7 endpoints.
- Smoke `POST /api/v1/accounts` → 3 balances at 0.00.
- `./mvnw verify`: 36 tests pass.
- CI workflow not run yet: no GitHub remote.

## Follow-up — README and UNSUPPORTED_CURRENCY (2026-10-27)

- Bad currency in a body (`"currency": "EUR"`) or query (`?currency=EUR`) now returns 400 `UNSUPPORTED_CURRENCY` (was `INVALID_REQUEST`). Other 400s unchanged. Three tests updated to expect the new code.
- `README.md`: run/test commands (DB on port 5433), endpoints, curl example flow, business rules, error code table.
- `./mvnw verify`: 36 tests pass.

## Follow-up — GitHub and diagrams (2026-10-27)

- Installed GitHub CLI 2.103.3 (winget); user logged in as JianRong-King.
- Created private repo https://github.com/JianRong-King/JustPay and pushed `master`. First CI run passed (36 tests, 55s). CI warns that `actions/checkout@v4` and `actions/setup-java@v4` are deprecated.
- README "How it works": three Mermaid diagrams (architecture, transfer sequence, data model). GitHub renders them; no new dependency.

## Follow-up — FX rates in data model diagram (2026-10-28)

- User noticed `FX_RATES` was not connected in the README data model diagram.
- Checked: correct for the schema. `transfers.rate` is a copied value, not a foreign key; same-currency transfers use rate 1, which has no `fx_rates` row.
- Added a dotted line `FX_RATES ||..o{ TRANSFERS : "rate copied into"` (dotted = no foreign key) and one explaining sentence.
