CREATE TABLE active_learning_cycle (
    cycle_id        VARCHAR(64)   PRIMARY KEY,
    dataset_id      VARCHAR(64)   NOT NULL,
    model_version   VARCHAR(64)   NOT NULL,
    strategy        VARCHAR(32)   NOT NULL,
    config_json     TEXT          NOT NULL,
    status          VARCHAR(24)   NOT NULL DEFAULT 'SELECTING',
    selected_records TEXT,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cycle_number    INT           NOT NULL DEFAULT 1,
    metadata_json   TEXT
);

CREATE INDEX idx_al_cycle_dataset ON active_learning_cycle(dataset_id);
CREATE INDEX idx_al_cycle_status ON active_learning_cycle(status);
