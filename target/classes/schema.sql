CREATE TABLE IF NOT EXISTS accounts (
    id VARCHAR(255) PRIMARY KEY,
    account_number VARCHAR(20) UNIQUE NOT NULL,
    holder_name VARCHAR(255) NOT NULL,
    balance DECIMAL(19,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_accounts_account_number ON accounts (account_number);
CREATE INDEX IF NOT EXISTS idx_accounts_status ON accounts (status);

CREATE TABLE IF NOT EXISTS transactions (
    id VARCHAR(255) PRIMARY KEY,
    sender_account_id VARCHAR(255) NOT NULL,
    recipient_account_id VARCHAR(255) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    CONSTRAINT fk_transactions_sender FOREIGN KEY (sender_account_id) REFERENCES accounts(id),
    CONSTRAINT fk_transactions_recipient FOREIGN KEY (recipient_account_id) REFERENCES accounts(id),
    CONSTRAINT chk_transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_transactions_different_accounts CHECK (sender_account_id != recipient_account_id)
);

CREATE INDEX IF NOT EXISTS idx_transactions_sender_account_id ON transactions (sender_account_id);
CREATE INDEX IF NOT EXISTS idx_transactions_recipient_account_id ON transactions (recipient_account_id);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions (status);
CREATE INDEX IF NOT EXISTS idx_transactions_created_at ON transactions (created_at);

CREATE TABLE IF NOT EXISTS payments (
    id VARCHAR(255) PRIMARY KEY,
    transaction_id VARCHAR(255),
    sender_account_id VARCHAR(255) NOT NULL,
    recipient_account_id VARCHAR(255) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    current_step VARCHAR(50) NOT NULL DEFAULT 'INITIATED',
    failure_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    CONSTRAINT fk_payments_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id),
    CONSTRAINT fk_payments_sender FOREIGN KEY (sender_account_id) REFERENCES accounts(id),
    CONSTRAINT fk_payments_recipient FOREIGN KEY (recipient_account_id) REFERENCES accounts(id),
    CONSTRAINT chk_payments_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_payments_different_accounts CHECK (sender_account_id != recipient_account_id)
);

CREATE INDEX IF NOT EXISTS idx_payments_transaction_id ON payments (transaction_id);
CREATE INDEX IF NOT EXISTS idx_payments_sender_account_id ON payments (sender_account_id);
CREATE INDEX IF NOT EXISTS idx_payments_recipient_account_id ON payments (recipient_account_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments (status);
CREATE INDEX IF NOT EXISTS idx_payments_current_step ON payments (current_step);
CREATE INDEX IF NOT EXISTS idx_payments_created_at ON payments (created_at);

CREATE TABLE IF NOT EXISTS outbox_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) UNIQUE NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_data JSONB NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_outbox_events_processed_at ON outbox_events (processed_at);
CREATE INDEX IF NOT EXISTS idx_outbox_events_event_type ON outbox_events (event_type);
CREATE INDEX IF NOT EXISTS idx_outbox_events_occurred_at ON outbox_events (occurred_at);