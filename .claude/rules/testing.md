---
paths:
  - "src/test/**"
---
# Testing rules — JustPay

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
