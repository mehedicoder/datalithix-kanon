ALTER TABLE dataset_definition ADD COLUMN source_annotation_ids TEXT;
ALTER TABLE dataset_definition ADD COLUMN curation_rule_json TEXT;
ALTER TABLE dataset_definition ADD COLUMN export_formats TEXT;

ALTER TABLE dataset_version ADD COLUMN curation_rule_id VARCHAR(120);
ALTER TABLE dataset_version ADD COLUMN splits_json TEXT;
ALTER TABLE dataset_version ADD COLUMN label_distribution_json TEXT;
ALTER TABLE dataset_version ADD COLUMN class_balance_json TEXT;
ALTER TABLE dataset_version ADD COLUMN evidence_event_ids TEXT;

ALTER TABLE training_job ADD COLUMN hyper_parameters_json TEXT NOT NULL DEFAULT '{}';
ALTER TABLE training_job ADD COLUMN metrics_history_json TEXT;
ALTER TABLE training_job ADD COLUMN evidence_event_ids TEXT;

ALTER TABLE compute_backend ADD COLUMN configuration_json TEXT NOT NULL DEFAULT '{}';
ALTER TABLE compute_backend ALTER COLUMN created_at DROP NOT NULL;
ALTER TABLE compute_backend ALTER COLUMN created_by DROP NOT NULL;
ALTER TABLE compute_backend ALTER COLUMN updated_at DROP NOT NULL;
ALTER TABLE compute_backend ALTER COLUMN updated_by DROP NOT NULL;
ALTER TABLE compute_backend ALTER COLUMN audit_version DROP NOT NULL;

ALTER TABLE model_entry ADD COLUMN compliance_tags TEXT;
ALTER TABLE model_entry ADD COLUMN version_ids TEXT;

ALTER TABLE model_version ADD COLUMN artifact_json TEXT NOT NULL DEFAULT '{}';
ALTER TABLE model_version ADD COLUMN hyper_parameters_json TEXT;
ALTER TABLE model_version ADD COLUMN compliance_tags TEXT;
ALTER TABLE model_version ADD COLUMN evaluation_run_ids TEXT;
ALTER TABLE model_version ADD COLUMN deployment_target_ids TEXT;
ALTER TABLE model_version ADD COLUMN evidence_event_ids TEXT;

ALTER TABLE evaluation_run ADD COLUMN metrics_json TEXT;
ALTER TABLE evaluation_run ADD COLUMN per_class_metrics_json TEXT;
ALTER TABLE evaluation_run ADD COLUMN confusion_matrix_uri VARCHAR(1024);
ALTER TABLE evaluation_run ADD COLUMN failure_case_sample_uri VARCHAR(1024);
ALTER TABLE evaluation_run ADD COLUMN evidence_event_ids TEXT;

ALTER TABLE deployment_target ADD COLUMN config_json TEXT;
ALTER TABLE deployment_target ADD COLUMN evidence_event_ids TEXT;

DROP TABLE IF EXISTS active_learning_cycle;

CREATE TABLE active_learning_cycle (
    cycle_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(120) NOT NULL,
    model_entry_id VARCHAR(120) NOT NULL,
    model_version_id VARCHAR(120),
    strategy_type VARCHAR(60) NOT NULL,
    status VARCHAR(60) NOT NULL DEFAULT 'SELECTING',
    selected_record_count INTEGER NOT NULL DEFAULT 0,
    passed_review_count INTEGER NOT NULL DEFAULT 0,
    source_dataset_version_id VARCHAR(120),
    target_dataset_version_id VARCHAR(120),
    retraining_job_id VARCHAR(120),
    evaluation_run_id VARCHAR(120),
    rejection_reason TEXT,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    evidence_event_ids TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    audit_version BIGINT NOT NULL,
    PRIMARY KEY (tenant_id, cycle_id)
);

CREATE INDEX idx_al_cycle_tenant_model ON active_learning_cycle(tenant_id, model_entry_id);
CREATE INDEX idx_al_cycle_tenant_status ON active_learning_cycle(tenant_id, status);
