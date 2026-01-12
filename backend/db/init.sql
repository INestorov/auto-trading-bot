-- backend/db/init.sql

CREATE TABLE IF NOT EXISTS accounts (
    id              BIGSERIAL PRIMARY KEY,
    symbol          VARCHAR(10) NOT NULL DEFAULT 'USDT',
    cash_balance    NUMERIC(18,8) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

CREATE TABLE IF NOT EXISTS positions (
        id              BIGSERIAL PRIMARY KEY,
        account_id      BIGINT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
        symbol          VARCHAR(20) NOT NULL,
        quantity        NUMERIC(18, 8) NOT NULL,
        avg_entry_price NUMERIC(18, 8) NOT NULL,
        updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
        UNIQUE (account_id, symbol)
        );

CREATE TABLE IF NOT EXISTS trades (
    id                  BIGSERIAL PRIMARY KEY,
    account_id          BIGINT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    mode                VARCHAR(10) NOT NULL,
    symbol              VARCHAR(20) NOT NULL,
    transaction_type    VARCHAR(4) NOT NULL,
    quantity            NUMERIC(18,8) NOT NULL,
    price               NUMERIC(18,8) NOT NULL,
    fee                 NUMERIC(18,8) NOT NULL DEFAULT 0,
    realized_pnl        NUMERIC(18,8) NOT NULL DEFAULT 0,
    purchased_at        TIMESTAMPTZ NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_trades_mode_symbol_ts
    ON trades(mode, symbol, purchased_at);

CREATE TABLE IF NOT EXISTS portfolio_snapshots (
    id              BIGSERIAL PRIMARY KEY,
    account_id      BIGINT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    mode            VARCHAR(10) NOT NULL,
    symbol          VARCHAR(20) NOT NULL,
    purchased_at    TIMESTAMPTZ NOT NULL,
    cash_balance    NUMERIC(18,8) NOT NULL,
    position_qty    NUMERIC(18,8) NOT NULL,
    position_value  NUMERIC(18,8) NOT NULL,
    total_value     NUMERIC(18,8) NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_snapshots_mode_symbol_purchased_at
    ON portfolio_snapshots(mode, symbol, purchased_at);

CREATE TABLE IF NOT EXISTS candles (
    symbol          VARCHAR(20) NOT NULL,
    time_interval   VARCHAR(10) NOT NULL,
    open_time       TIMESTAMPTZ NOT NULL,
    open_pice       NUMERIC(18,8) NOT NULL,
    high            NUMERIC(18,8) NOT NULL,
    low             NUMERIC(18,8) NOT NULL,
    close_price     NUMERIC(18,8) NOT NULL,
    volume          NUMERIC(18,8) NOT NULL,
    PRIMARY KEY(symbol, time_interval, open_time)
    );
    INSERT INTO accounts (symbol, cash_balance, created_at)
    SELECT 'USDT', 10000, TIMESTAMPTZ '2026-01-01 00:00:00+02'
        WHERE NOT EXISTS (SELECT 1 FROM accounts);