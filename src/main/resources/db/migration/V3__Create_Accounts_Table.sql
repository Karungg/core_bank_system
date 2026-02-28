CREATE TABLE accounts (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    balance DECIMAL(12, 0) NOT NULL,
    pin VARCHAR(255) NOT NULL,
    card_number VARCHAR(30) NOT NULL,
    cvv VARCHAR(3) NOT NULL,
    expired_date DATE NOT NULL,
    type VARCHAR(50) NOT NULL,
    created_at DATE NOT NULL,
    updated_at DATE NOT NULL,
    PRIMARY KEY (id)
);

ALTER TABLE IF EXISTS accounts
    ADD CONSTRAINT uk_accounts_account_number UNIQUE (account_number);

ALTER TABLE IF EXISTS accounts
    ADD CONSTRAINT uk_accounts_card_number UNIQUE (card_number);

ALTER TABLE IF EXISTS accounts
    ADD CONSTRAINT fk_accounts_user
    FOREIGN KEY (user_id)
    REFERENCES users (id);