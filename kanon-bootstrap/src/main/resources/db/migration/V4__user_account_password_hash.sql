ALTER TABLE user_account
    ADD COLUMN password_hash VARCHAR(255);

UPDATE user_account
SET password_hash = '{noop}admin'
WHERE username = 'superadmin' AND password_hash IS NULL;

ALTER TABLE user_account
    ALTER COLUMN password_hash SET NOT NULL;
