CREATE TABLE accounts (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    holder_name VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL
);

CREATE TABLE balances (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id BIGINT         NOT NULL REFERENCES accounts (id),
    currency   VARCHAR(3)     NOT NULL,
    amount     NUMERIC(19, 2) NOT NULL,
    CONSTRAINT uq_balances_account_currency UNIQUE (account_id, currency),
    CONSTRAINT chk_balances_currency CHECK (currency IN ('MYR', 'USD', 'SGD')),
    CONSTRAINT chk_balances_amount_non_negative CHECK (amount >= 0)
);
