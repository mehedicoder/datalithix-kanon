ALTER TABLE active_learning_cycle ADD COLUMN cron_expression VARCHAR(120);
ALTER TABLE active_learning_cycle ADD COLUMN auto_trigger BOOLEAN NOT NULL DEFAULT FALSE;
