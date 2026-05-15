CREATE INDEX IF NOT EXISTS idx_al_cycle_tenant_auto_trigger
    ON active_learning_cycle (tenant_id, auto_trigger, status);

COMMENT ON INDEX idx_al_cycle_tenant_auto_trigger IS
    'Supports ActiveLearningScheduler queries for terminal cycles with auto_trigger enabled, per tenant';
