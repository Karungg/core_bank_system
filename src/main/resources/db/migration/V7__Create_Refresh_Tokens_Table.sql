CREATE TABLE refresh_tokens (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    token VARCHAR(120) NOT NULL,
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id)
);

ALTER TABLE IF EXISTS refresh_tokens
    ADD CONSTRAINT uk_refresh_tokens_token UNIQUE (token);

ALTER TABLE IF EXISTS refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_user
    FOREIGN KEY (user_id)
    REFERENCES users (id)
    ON DELETE CASCADE;
