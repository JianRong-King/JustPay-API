CREATE TABLE fx_rates (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    from_currency VARCHAR(3)     NOT NULL,
    to_currency   VARCHAR(3)     NOT NULL,
    rate          NUMERIC(19, 6) NOT NULL,
    CONSTRAINT uq_fx_rates_pair UNIQUE (from_currency, to_currency),
    CONSTRAINT chk_fx_rates_from_currency CHECK (from_currency IN ('MYR', 'USD', 'SGD')),
    CONSTRAINT chk_fx_rates_to_currency CHECK (to_currency IN ('MYR', 'USD', 'SGD')),
    CONSTRAINT chk_fx_rates_different_currencies CHECK (from_currency <> to_currency),
    CONSTRAINT chk_fx_rates_rate_positive CHECK (rate > 0)
);

INSERT INTO fx_rates (from_currency, to_currency, rate) VALUES
    ('USD', 'MYR', 4.400000),
    ('MYR', 'USD', 0.220000),
    ('SGD', 'MYR', 3.300000),
    ('MYR', 'SGD', 0.300000),
    ('USD', 'SGD', 1.300000),
    ('SGD', 'USD', 0.760000);
