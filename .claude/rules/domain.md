---
paths:
  - "src/**/*.java"
  - "src/main/resources/db/migration/**"
---
# Domain rules — JustPay

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
