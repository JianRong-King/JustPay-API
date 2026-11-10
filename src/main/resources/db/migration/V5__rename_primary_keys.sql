ALTER TABLE accounts       RENAME COLUMN id TO account_id;
ALTER TABLE balances       RENAME COLUMN id TO balance_id;
ALTER TABLE transfers      RENAME COLUMN id TO transfer_id;
ALTER TABLE ledger_entries RENAME COLUMN id TO ledger_entry_id;
ALTER TABLE fx_rates       RENAME COLUMN id TO fx_rate_id;
