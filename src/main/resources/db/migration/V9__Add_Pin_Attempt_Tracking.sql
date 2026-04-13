ALTER TABLE accounts ADD COLUMN failed_pin_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE accounts ADD COLUMN pin_locked_until TIMESTAMP;

ALTER TABLE users ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN login_locked_until TIMESTAMP;
