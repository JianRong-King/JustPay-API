---
paths:
  - "src/main/java/**/*Controller.java"
  - "src/main/java/**/*Request.java"
  - "src/main/java/**/*Response.java"
  - "src/main/java/com/justpay/common/**"
  - "src/test/java/**/*Test.java"
---
# API rules — JustPay

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

## API conventions

- JSON fields in camelCase. Use Java `record`s for request and response DTOs.
- Never return JPA entities from controllers.
- Controllers stay thin: validate input, call a service, return a response.
- All errors go through one `@RestControllerAdvice` and use this shape:
  ```json
  { "code": "INSUFFICIENT_BALANCE", "message": "Human-readable message" }
  ```
- Status codes: `400` invalid input, `404` not found, `409` conflict, `422` business rule broken.
