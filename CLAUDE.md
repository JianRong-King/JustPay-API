# CLAUDE.md — JustPay

## Project overview

JustPay is a small backend API for multi-currency wallets, built as a learning project.
It is modelled on the idea of a WorldFirst World Account: one account holds balances in
several currencies, and money can be transferred and converted between them.

Users can:
- Create an account, which automatically gets an MYR, USD and SGD balance
- Deposit money into any of their balances
- Transfer money to another account, in the same or a different currency
- Convert money between their own balances (e.g. USD → MYR in the same account)

Transfers are idempotent: retrying the same request never moves money twice.

This is a LEARNING project. I must understand every line of code. Keep the design
simple and explain your work in plain language.

Supported currencies: MYR, USD, SGD.
Out of scope: live exchange rates, FX quotes with expiry, authentication, frontend,
refunds, withdrawals, real payment providers.

## Tech stack

- Java 21, Spring Boot 3.x, Maven (`./mvnw`)
- PostgreSQL 16 (via Docker Compose), Spring Data JPA, Flyway migrations
- JUnit 5, Testcontainers (PostgreSQL), MockMvc
- springdoc-openapi (Swagger UI), GitHub Actions

Do not add new dependencies without asking me first.

## Commands

- Start database: `docker compose up -d`
- Run app: `./mvnw spring-boot:run`
- Run all tests: `./mvnw verify`
- API docs: http://localhost:8080/swagger-ui.html

## Project structure

```
src/main/java/com/justpay/
  account/    Account, Balance entities, AccountController, AccountService, deposits
  transfer/   Transfer entity, TransferController, TransferService
  ledger/     LedgerEntry entity, LedgerRepository
  fx/         FxRate entity, FxRateRepository, FxService (conversion)
  common/     error handling (ErrorResponse, GlobalExceptionHandler)
src/main/resources/db/migration/   Flyway SQL migrations
```

## Data model

- `accounts`: id, holder name, created time.
- `balances`: id, account id, currency, amount `NUMERIC(19,2)`.
  - UNIQUE (account id, currency). `CHECK (amount >= 0)`.
  - When an account is created, create all three balances (MYR, USD, SGD) at 0 in the
    same transaction. Balances are never created anywhere else.
- `transfers`: id, paymentRequestId (UNIQUE), fromAccountId, fromCurrency, fromAmount,
  toAccountId, toCurrency, toAmount, rate, created time.
- `ledger_entries`: id, account id, currency, signed amount, type (`DEPOSIT`,
  `TRANSFER_OUT`, `TRANSFER_IN`), transfer id (null for deposits), created time.
- `fx_rates`: from currency, to currency, rate `NUMERIC(19,6)`.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/accounts` | Create an account (gets MYR, USD, SGD balances at 0) |
| GET | `/api/v1/accounts/{id}` | Get account with all three balances |
| POST | `/api/v1/accounts/{id}/deposits` | Deposit `{currency, amount}` into one balance |
| POST | `/api/v1/transfers` | Transfer or convert money (see Transfer rules) |
| GET | `/api/v1/transfers/{id}` | Get a transfer |
| GET | `/api/v1/accounts/{id}/ledger` | List ledger entries, newest first; optional `?currency=` filter |
| GET | `/api/v1/fx/rates` | List the fixed exchange rates |

## Money rules

- NEVER use `double` or `float` for money. Use `BigDecimal` in Java and `NUMERIC(19,2)` in the database.
- Amounts must be greater than 0 and have at most 2 decimal places. Otherwise → 400.
- Currency must be `MYR`, `USD` or `SGD`. Anything else → 400.
- Compare BigDecimal values with `compareTo`, never `equals`.

## Transfer rules

- A transfer request contains: `paymentRequestId`, `fromAccountId`, `fromCurrency`,
  `toAccountId`, `toCurrency`, `amount`.
- `amount` is in `fromCurrency`.
- The same account is allowed ONLY if the currencies differ (this is a conversion).
  Same account AND same currency → 422 `SAME_BALANCE`.
- Unknown account → 404 `ACCOUNT_NOT_FOUND`.
- Sender's `fromCurrency` balance must be enough → otherwise 422 `INSUFFICIENT_BALANCE`.
  No money moves.
- A successful transfer does all of this in ONE `@Transactional` method:
  1. Calculate `toAmount` (see Currency conversion rules)
  2. Subtract `amount` from the sender's `fromCurrency` balance
  3. Add `toAmount` to the receiver's `toCurrency` balance
  4. Save the transfer
  5. Write two ledger entries (see Ledger rules)
- Locking: load both balance rows with `@Lock(PESSIMISTIC_WRITE)` so two transfers
  cannot spend the same money at the same time. Always lock the balance with the
  lower id first, so two opposite transfers never wait on each other forever.

## Currency conversion rules

- Rates are seeded by a Flyway migration and never change at runtime.
- Seed rates so converting there and back never gains money:

  | From | To | Rate |
  |---|---|---|
  | USD | MYR | 4.400000 |
  | MYR | USD | 0.220000 |
  | SGD | MYR | 3.300000 |
  | MYR | SGD | 0.300000 |
  | USD | SGD | 1.300000 |
  | SGD | USD | 0.760000 |

- Same currency → `toAmount = amount`, rate = 1.
- Different currency → `toAmount = amount × rate`, rounded to 2 decimal places with
  `RoundingMode.HALF_EVEN`. Round once, at the end.
- Every transfer stores the rate it used.

## Idempotency rules

- `paymentRequestId` is required and has a UNIQUE constraint in the database.
- Same `paymentRequestId` + same fromAccountId, fromCurrency, toAccountId, toCurrency
  and amount → return the original transfer. Do not move money again.
- Same `paymentRequestId` + any different detail → 409 `PAYMENT_REQUEST_ID_CONFLICT`.
- Compare the stored fields directly. No hashing needed.

## Ledger rules

- Every balance change writes a ledger entry with the balance's currency.
- A deposit writes one entry (+amount, type `DEPOSIT`).
- A transfer writes two entries: sender (−fromAmount, `TRANSFER_OUT`) and
  receiver (+toAmount, `TRANSFER_IN`), both linked to the transfer id.
- Ledger entries are never updated or deleted.
- Each balance must always equal the sum of ledger entries for that account and currency.

## API conventions

- JSON fields in camelCase. Use Java `record`s for request and response DTOs.
- Never return JPA entities from controllers.
- Controllers stay thin: validate input, call a service, return a response.
- All errors go through one `@RestControllerAdvice` and use this shape:
  ```json
  { "code": "INSUFFICIENT_BALANCE", "message": "Human-readable message" }
  ```
- Status codes: `400` invalid input, `404` not found, `409` conflict, `422` business rule broken.

## Code conventions

- Constructor injection only. No `@Autowired` on fields. No Lombok.
- Small methods with clear names. Prefer simple, readable code over clever code.
- Timestamps stored as `Instant` / `TIMESTAMPTZ` in UTC.

## Database

- All schema changes go through Flyway: `V{number}__{description}.sql`.
- Never edit a migration that has already been applied. Create a new one.
- `spring.jpa.hibernate.ddl-auto=validate`.

## Testing rules

- Integration tests use Testcontainers PostgreSQL and MockMvc. No H2.
- Test names follow `should_<expected>_when_<condition>`.
- These tests must exist and pass:
  - Create account → three balances (MYR, USD, SGD) at 0
  - Deposit USD → only the USD balance increases, one ledger entry written
  - Same-currency transfer between accounts → both balances update, two ledger entries
  - Cross-currency transfer between accounts → receiver gets amount × rate, rounded correctly, rate stored
  - Conversion within one account (USD → MYR) → both balances in that account update
  - Same account and same currency → 422 `SAME_BALANCE`
  - Insufficient balance → 422, no balances change
  - Same `paymentRequestId` sent twice → only one transfer, same response
  - Same `paymentRequestId` with different details → 409
  - Unsupported currency → 400
  - Every balance equals the sum of its ledger entries after every test scenario
- NEVER delete, skip, or weaken a test to make it pass. If a test seems wrong, stop and ask me.

## How to work with me (learning mode)

1. Work in small steps. Before each step, tell me in plain language what you will
   build and why, then wait for my approval.
2. After each step, explain the code you wrote: what each class does and any Spring
   or database concept I might not know (e.g. what `@Transactional` or a pessimistic
   lock actually does).
3. Run `./mvnw verify` after each step. Never claim tests pass without running them.
4. Prefer the simplest correct solution. If something more advanced is needed, explain
   why and ask first.
5. If anything is unclear, ask me. Do not guess.
6. Do not edit this file unless I ask.
7. When I correct a mistake, suggest a rule to add to "Lessons learned" below.

## Lessons learned

Rules added after real mistakes during development (see `docs/ai-log.md`).

<!-- Add entries as they happen, e.g.:
- [date] Rule: ... (Why: ...)
-->
