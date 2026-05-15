ALTER TABLE user_account
    ADD COLUMN preferred_locale VARCHAR(40);

UPDATE user_account
SET preferred_locale = 'en'
WHERE username = 'superadmin'
  AND preferred_locale IS NULL;
