CREATE TABLE ledger_entries (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id  BIGINT         NOT NULL REFERENCES accounts (id),
    currency    VARCHAR(3)     NOT NULL,
    amount      NUMERIC(19, 2) NOT NULL,
    type        VARCHAR(20)    NOT NULL,
    transfer_id BIGINT,
    created_at  TIMESTAMPTZ    NOT NULL,
    CONSTRAINT chk_ledger_entries_currency CHECK (currency IN ('MYR', 'USD', 'SGD')),
    CONSTRAINT chk_ledger_entries_type CHECK (type IN ('DEPOSIT', 'TRANSFER_OUT', 'TRANSFER_IN')),
    CONSTRAINT chk_ledger_entries_amount_not_zero CHECK (amount <> 0),
    CONSTRAINT chk_ledger_entries_transfer_link CHECK ((type = 'DEPOSIT') = (transfer_id IS NULL))
);

CREATE INDEX idx_ledger_entries_account_created ON ledger_entries (account_id, created_at DESC);
