CREATE TABLE transfers (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    payment_request_id VARCHAR(100)   NOT NULL,
    from_account_id    BIGINT         NOT NULL REFERENCES accounts (id),
    from_currency      VARCHAR(3)     NOT NULL,
    from_amount        NUMERIC(19, 2) NOT NULL,
    to_account_id      BIGINT         NOT NULL REFERENCES accounts (id),
    to_currency        VARCHAR(3)     NOT NULL,
    to_amount          NUMERIC(19, 2) NOT NULL,
    rate               NUMERIC(19, 6) NOT NULL,
    created_at         TIMESTAMPTZ    NOT NULL,
    CONSTRAINT uq_transfers_payment_request_id UNIQUE (payment_request_id),
    CONSTRAINT chk_transfers_from_currency CHECK (from_currency IN ('MYR', 'USD', 'SGD')),
    CONSTRAINT chk_transfers_to_currency CHECK (to_currency IN ('MYR', 'USD', 'SGD')),
    CONSTRAINT chk_transfers_amounts_positive CHECK (from_amount > 0 AND to_amount > 0),
    CONSTRAINT chk_transfers_rate_positive CHECK (rate > 0),
    CONSTRAINT chk_transfers_not_same_balance CHECK (from_account_id <> to_account_id OR from_currency <> to_currency)
);

ALTER TABLE ledger_entries
    ADD CONSTRAINT fk_ledger_entries_transfer FOREIGN KEY (transfer_id) REFERENCES transfers (id);
