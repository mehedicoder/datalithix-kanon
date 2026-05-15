package ai.datalithix.kanon.bootstrap.persistence;

import ai.datalithix.kanon.activelearning.model.ActiveLearningCycle;
import ai.datalithix.kanon.activelearning.model.CycleStatus;
import ai.datalithix.kanon.activelearning.model.SelectionStrategyType;
import ai.datalithix.kanon.activelearning.service.ActiveLearningCycleRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.audit;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.fromJson;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.instant;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.timestamp;
import static ai.datalithix.kanon.bootstrap.persistence.PersistenceMappingSupport.toJson;

@Repository
@Profile("!test")
public class PostgresActiveLearningCycleRepository implements ActiveLearningCycleRepository {
    private final JdbcTemplate jdbcTemplate;

    public PostgresActiveLearningCycleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ActiveLearningCycle save(ActiveLearningCycle cycle) {
        jdbcTemplate.update("""
                INSERT INTO active_learning_cycle (
                    cycle_id, tenant_id, model_entry_id, model_version_id,
                    strategy_type, status, selected_record_count, passed_review_count,
                    source_dataset_version_id, target_dataset_version_id,
                    retraining_job_id, evaluation_run_id, rejection_reason,
                    started_at, completed_at, cron_expression, auto_trigger,
                    evidence_event_ids,
                    created_at, created_by, updated_at, updated_by, audit_version
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, cycle_id)
                DO UPDATE SET
                    model_version_id = EXCLUDED.model_version_id,
                    status = EXCLUDED.status,
                    selected_record_count = EXCLUDED.selected_record_count,
                    passed_review_count = EXCLUDED.passed_review_count,
                    source_dataset_version_id = EXCLUDED.source_dataset_version_id,
                    target_dataset_version_id = EXCLUDED.target_dataset_version_id,
                    retraining_job_id = EXCLUDED.retraining_job_id,
                    evaluation_run_id = EXCLUDED.evaluation_run_id,
                    rejection_reason = EXCLUDED.rejection_reason,
                    completed_at = EXCLUDED.completed_at,
                    cron_expression = EXCLUDED.cron_expression,
                    auto_trigger = EXCLUDED.auto_trigger,
                    evidence_event_ids = EXCLUDED.evidence_event_ids,
                    updated_at = EXCLUDED.updated_at,
                    updated_by = EXCLUDED.updated_by,
                    audit_version = active_learning_cycle.audit_version + 1
                """,
                cycle.cycleId(), cycle.tenantId(), cycle.modelEntryId(), cycle.modelVersionId(),
                cycle.strategyType().name(), cycle.status().name(), cycle.selectedRecordCount(),
                cycle.passedReviewCount(), cycle.sourceDatasetVersionId(),
                cycle.targetDatasetVersionId(), cycle.retrainingJobId(), cycle.evaluationRunId(),
                cycle.rejectionReason(), timestamp(cycle.startedAt()), timestamp(cycle.completedAt()),
                cycle.cronExpression(), cycle.autoTrigger(),
                toJson(cycle.evidenceEventIds()),
                timestamp(cycle.audit().createdAt()), cycle.audit().createdBy(),
                timestamp(cycle.audit().updatedAt()), cycle.audit().updatedBy(),
                cycle.audit().version()
        );
        return findById(cycle.tenantId(), cycle.cycleId()).orElse(cycle);
    }

    @Override
    public Optional<ActiveLearningCycle> findById(String tenantId, String cycleId) {
        return jdbcTemplate.query("SELECT * FROM active_learning_cycle WHERE tenant_id = ? AND cycle_id = ?",
                this::map, tenantId, cycleId).stream().findFirst();
    }

    @Override
    public List<ActiveLearningCycle> findByTenant(String tenantId) {
        return jdbcTemplate.query("SELECT * FROM active_learning_cycle WHERE tenant_id = ? ORDER BY started_at DESC",
                this::map, tenantId);
    }

    @Override
    public List<ActiveLearningCycle> findByModel(String tenantId, String modelEntryId) {
        return jdbcTemplate.query("SELECT * FROM active_learning_cycle WHERE tenant_id = ? AND model_entry_id = ? ORDER BY started_at DESC",
                this::map, tenantId, modelEntryId);
    }

    @Override
    public List<ActiveLearningCycle> findByStatus(String tenantId, CycleStatus status) {
        return jdbcTemplate.query("SELECT * FROM active_learning_cycle WHERE tenant_id = ? AND status = ? ORDER BY started_at DESC",
                this::map, tenantId, status.name());
    }

    @Override
    public List<ActiveLearningCycle> findAllByStatus(CycleStatus status) {
        return jdbcTemplate.query("SELECT * FROM active_learning_cycle WHERE status = ? ORDER BY started_at DESC",
                this::map, status.name());
    }

    @Override
    public Optional<ActiveLearningCycle> findLatestByModel(String tenantId, String modelEntryId) {
        return jdbcTemplate.query("""
                SELECT * FROM active_learning_cycle
                WHERE tenant_id = ? AND model_entry_id = ?
                ORDER BY started_at DESC LIMIT 1
                """, this::map, tenantId, modelEntryId).stream().findFirst();
    }

    private ActiveLearningCycle map(ResultSet rs, int rowNum) throws SQLException {
        return new ActiveLearningCycle(
                rs.getString("cycle_id"),
                rs.getString("tenant_id"),
                rs.getString("model_entry_id"),
                rs.getString("model_version_id"),
                SelectionStrategyType.valueOf(rs.getString("strategy_type")),
                CycleStatus.valueOf(rs.getString("status")),
                rs.getInt("selected_record_count"),
                rs.getInt("passed_review_count"),
                rs.getString("source_dataset_version_id"),
                rs.getString("target_dataset_version_id"),
                rs.getString("retraining_job_id"),
                rs.getString("evaluation_run_id"),
                rs.getString("rejection_reason"),
                instant(rs, "started_at"),
                instant(rs, "completed_at"),
                rs.getString("cron_expression"),
                rs.getBoolean("auto_trigger"),
                fromJson(rs.getString("evidence_event_ids"), new TypeReference<List<String>>() {}),
                audit(rs)
        );
    }
}
