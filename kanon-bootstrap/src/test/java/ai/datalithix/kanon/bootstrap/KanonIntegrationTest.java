package ai.datalithix.kanon.bootstrap;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Tag("docker")
class KanonIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("datalithix_kanon")
            .withUsername("test")
            .withPassword("test");

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    private Connection pgConnection() throws Exception {
        return DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    @Test
    void flywayMigrationsCreateAllExpectedTables() throws Exception {
        try (Connection conn = pgConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = meta.getTables(null, "public", "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
            assertTrue(tables.contains("dataset_definition"), "Missing: dataset_definition");
            assertTrue(tables.contains("dataset_version"), "Missing: dataset_version");
            assertTrue(tables.contains("compute_backend"), "Missing: compute_backend");
            assertTrue(tables.contains("training_job"), "Missing: training_job");
            assertTrue(tables.contains("model_entry"), "Missing: model_entry");
            assertTrue(tables.contains("model_version"), "Missing: model_version");
            assertTrue(tables.contains("evaluation_run"), "Missing: evaluation_run");
            assertTrue(tables.contains("deployment_target"), "Missing: deployment_target");
            assertTrue(tables.contains("active_learning_cycle"), "Missing: active_learning_cycle");
            assertTrue(tables.contains("active_configuration_version"), "Missing: active_configuration_version");
            assertTrue(tables.contains("agent_profile"), "Missing: agent_profile");
            assertTrue(tables.contains("workflow_definition"), "Missing: workflow_definition");
            assertTrue(tables.contains("workflow_instance"), "Missing: workflow_instance");
            assertTrue(tables.contains("tenant"), "Missing: tenant");
            assertTrue(tables.contains("organization"), "Missing: organization");
            assertTrue(tables.contains("workspace"), "Missing: workspace");
            assertTrue(tables.contains("user_account"), "Missing: user_account");
            assertTrue(tables.contains("kanon_role"), "Missing: kanon_role");
            assertTrue(tables.contains("membership"), "Missing: membership");
            assertTrue(tables.contains("model_profile"), "Missing: model_profile");
            assertTrue(tables.contains("external_annotation_node"), "Missing: external_annotation_node");
        }
    }

    @Test
    void flywayMigrationVersionApplied() throws Exception {
        try (Connection conn = pgConnection()) {
            var stmt = conn.createStatement();
            var rs = stmt.executeQuery("SELECT MAX(version) FROM flyway_schema_history");
            assertTrue(rs.next());
            assertTrue(rs.getInt(1) >= 14, "Expected at least Flyway version 14, got " + rs.getInt(1));
        }
    }

    @Test
    void activeConfigurationVersionCrud() throws Exception {
        try (Connection conn = pgConnection()) {
            var now = Timestamp.from(Instant.now());
            conn.createStatement().execute("""
                    INSERT INTO active_configuration_version
                        (tenant_id, configuration_type, configuration_id, template_id, version,
                         activation_state, activated_by, activated_at, change_reason,
                         created_at, created_by, updated_at, updated_by, audit_version)
                    VALUES ('test-tenant', 'DOMAIN', 'test-domain', 'test-pack', 1,
                            'ACTIVE', 'admin', '%s', 'seed',
                            '%s', 'admin', '%s', 'admin', 1)
                    """.formatted(now, now, now));

            var rs = conn.createStatement().executeQuery(
                    "SELECT configuration_id, activation_state FROM active_configuration_version WHERE tenant_id = 'test-tenant'");
            assertTrue(rs.next());
            assertEquals("test-domain", rs.getString("configuration_id"));
            assertEquals("ACTIVE", rs.getString("activation_state"));

            conn.createStatement().execute("""
                    UPDATE active_configuration_version
                    SET activation_state = 'INACTIVE', audit_version = audit_version + 1
                    WHERE tenant_id = 'test-tenant' AND configuration_type = 'DOMAIN' AND configuration_id = 'test-domain'
                    """);

            rs = conn.createStatement().executeQuery(
                    "SELECT activation_state, audit_version FROM active_configuration_version WHERE tenant_id = 'test-tenant'");
            assertTrue(rs.next());
            assertEquals("INACTIVE", rs.getString("activation_state"));
            assertEquals(2, rs.getInt("audit_version"));

            conn.createStatement().execute("DELETE FROM active_configuration_version WHERE tenant_id = 'test-tenant'");
        }
    }

    @Test
    void activeLearningCycleCrud() throws Exception {
        try (Connection conn = pgConnection()) {
            var now = Timestamp.from(Instant.now());
            conn.createStatement().execute("""
                    INSERT INTO active_learning_cycle
                        (cycle_id, tenant_id, model_entry_id, model_version_id,
                         strategy_type, status, selected_record_count, passed_review_count,
                         source_dataset_version_id, started_at, auto_trigger,
                         evidence_event_ids,
                         created_at, created_by, updated_at, updated_by, audit_version)
                    VALUES ('test-cycle-1', 'tenant-1', 'model-1', 'version-1',
                            'UNCERTAINTY_SAMPLING', 'AWAITING_REVIEW', 50, 0,
                            'dsv-1', '%s', true,
                            '["evt-1"]',
                            '%s', 'admin', '%s', 'admin', 1)
                    """.formatted(now, now, now));

            var rs = conn.createStatement().executeQuery(
                    "SELECT strategy_type, status, auto_trigger FROM active_learning_cycle WHERE cycle_id = 'test-cycle-1'");
            assertTrue(rs.next());
            assertEquals("UNCERTAINTY_SAMPLING", rs.getString("strategy_type"));
            assertEquals("AWAITING_REVIEW", rs.getString("status"));
            assertTrue(rs.getBoolean("auto_trigger"));

            conn.createStatement().execute("""
                    UPDATE active_learning_cycle
                    SET status = 'PROMOTED', completed_at = '%s', audit_version = audit_version + 1
                    WHERE cycle_id = 'test-cycle-1'
                    """.formatted(now));

            rs = conn.createStatement().executeQuery(
                    "SELECT status FROM active_learning_cycle WHERE cycle_id = 'test-cycle-1'");
            assertTrue(rs.next());
            assertEquals("PROMOTED", rs.getString("status"));

            conn.createStatement().execute("DELETE FROM active_learning_cycle WHERE cycle_id = 'test-cycle-1'");
        }
    }

    @Test
    void datasetCrud() throws Exception {
        try (Connection conn = pgConnection()) {
            var now = Timestamp.from(Instant.now());
            conn.createStatement().execute("""
                    INSERT INTO dataset_definition
                        (dataset_definition_id, tenant_id, name, domain_type,
                         train_ratio, val_ratio, test_ratio,
                         created_at, created_by, updated_at, updated_by, audit_version)
                    VALUES ('dd-1', 'tenant-1', 'Test Dataset', 'ACCOUNTING',
                            0.7, 0.15, 0.15,
                            '%s', 'admin', '%s', 'admin', 1)
                    """.formatted(now, now));

            var rs = conn.createStatement().executeQuery(
                    "SELECT name, domain_type FROM dataset_definition WHERE dataset_definition_id = 'dd-1'");
            assertTrue(rs.next());
            assertEquals("Test Dataset", rs.getString("name"));
            assertEquals("ACCOUNTING", rs.getString("domain_type"));

            conn.createStatement().execute("""
                    INSERT INTO dataset_version
                        (dataset_version_id, dataset_definition_id, tenant_id, version_number, record_count,
                         created_at, created_by, updated_at, updated_by, audit_version)
                    VALUES ('dv-1', 'dd-1', 'tenant-1', 1, 100,
                            '%s', 'admin', '%s', 'admin', 1)
                    """.formatted(now, now));

            rs = conn.createStatement().executeQuery(
                    "SELECT version_number, record_count FROM dataset_version WHERE dataset_version_id = 'dv-1'");
            assertTrue(rs.next());
            assertEquals(1, rs.getInt("version_number"));
            assertEquals(100, rs.getInt("record_count"));

            conn.createStatement().execute("DELETE FROM dataset_version WHERE dataset_version_id = 'dv-1'");
            conn.createStatement().execute("DELETE FROM dataset_definition WHERE dataset_definition_id = 'dd-1'");
        }
    }

    @Test
    void trainingJobCrud() throws Exception {
        try (Connection conn = pgConnection()) {
            var now = Timestamp.from(Instant.now());
            conn.createStatement().execute("""
                    INSERT INTO training_job
                        (job_id, tenant_id, model_name, status, dataset_version_id,
                         requested_at,
                         created_at, created_by, updated_at, updated_by, audit_version)
                    VALUES ('tj-1', 'tenant-1', 'test-model', 'QUEUED', 'dv-1',
                            '%s',
                            '%s', 'admin', '%s', 'admin', 1)
                    """.formatted(now, now, now));

            var rs = conn.createStatement().executeQuery(
                    "SELECT status, model_name FROM training_job WHERE job_id = 'tj-1'");
            assertTrue(rs.next());
            assertEquals("QUEUED", rs.getString("status"));
            assertEquals("test-model", rs.getString("model_name"));

            conn.createStatement().execute("""
                    UPDATE training_job
                    SET status = 'RUNNING', audit_version = audit_version + 1
                    WHERE job_id = 'tj-1'
                    """);

            rs = conn.createStatement().executeQuery(
                    "SELECT status, audit_version FROM training_job WHERE job_id = 'tj-1'");
            assertTrue(rs.next());
            assertEquals("RUNNING", rs.getString("status"));
            assertEquals(2, rs.getInt("audit_version"));

            conn.createStatement().execute("DELETE FROM training_job WHERE job_id = 'tj-1'");
        }
    }

    @Test
    void modelRegistryCrud() throws Exception {
        try (Connection conn = pgConnection()) {
            var now = Timestamp.from(Instant.now());
            conn.createStatement().execute("""
                    INSERT INTO model_entry
                        (model_entry_id, tenant_id, model_name, task_type, domain_type,
                         latest_lifecycle_stage,
                         created_at, created_by, updated_at, updated_by, audit_version)
                    VALUES ('me-1', 'tenant-1', 'test-model', 'EXTRACTION', 'ACCOUNTING',
                            'DEVELOPMENT',
                            '%s', 'admin', '%s', 'admin', 1)
                    """.formatted(now, now));

            var rs = conn.createStatement().executeQuery(
                    "SELECT model_name, latest_lifecycle_stage FROM model_entry WHERE model_entry_id = 'me-1'");
            assertTrue(rs.next());
            assertEquals("test-model", rs.getString("model_name"));
            assertEquals("DEVELOPMENT", rs.getString("latest_lifecycle_stage"));

            conn.createStatement().execute("""
                    INSERT INTO model_version
                        (model_version_id, model_entry_id, tenant_id, version_number, lifecycle_stage,
                         created_at, created_by, updated_at, updated_by, audit_version)
                    VALUES ('mv-1', 'me-1', 'tenant-1', 1, 'DEVELOPMENT',
                            '%s', 'admin', '%s', 'admin', 1)
                    """.formatted(now, now));

            rs = conn.createStatement().executeQuery(
                    "SELECT version_number FROM model_version WHERE model_version_id = 'mv-1'");
            assertTrue(rs.next());
            assertEquals(1, rs.getInt("version_number"));

            conn.createStatement().execute("DELETE FROM model_version WHERE model_version_id = 'mv-1'");
            conn.createStatement().execute("DELETE FROM model_entry WHERE model_entry_id = 'me-1'");
        }
    }

    @Test
    void governanceTablesCrud() throws Exception {
        try (Connection conn = pgConnection()) {
            var now = Timestamp.from(Instant.now());
            conn.createStatement().execute("""
                    INSERT INTO tenant (tenant_id, name, status, created_at, created_by, updated_at, updated_by, audit_version)
                    VALUES ('tc-tenant', 'Test Corp', 'ACTIVE', '%s', 'admin', '%s', 'admin', 1)
                    """.formatted(now, now));

            var rs = conn.createStatement().executeQuery(
                    "SELECT name, status FROM tenant WHERE tenant_id = 'tc-tenant'");
            assertTrue(rs.next());
            assertEquals("Test Corp", rs.getString("name"));
            assertEquals("ACTIVE", rs.getString("status"));

            conn.createStatement().execute("DELETE FROM tenant WHERE tenant_id = 'tc-tenant'");
        }
    }

    @Test
    void tenantIsolationAcrossTables() throws Exception {
        try (Connection conn = pgConnection()) {
            var now = Timestamp.from(Instant.now());

            conn.createStatement().execute("""
                    INSERT INTO active_learning_cycle
                        (cycle_id, tenant_id, model_entry_id, model_version_id,
                         strategy_type, status, selected_record_count, passed_review_count,
                         source_dataset_version_id, started_at, auto_trigger,
                         evidence_event_ids,
                         created_at, created_by, updated_at, updated_by, audit_version)
                    VALUES ('al-t1', 'tenant-a', 'm1', 'v1',
                            'UNCERTAINTY_SAMPLING', 'PROMOTED', 10, 5,
                            'dsv-1', '%s', false,
                            '[]', '%s', 'admin', '%s', 'admin', 1)
                    """.formatted(now, now, now));
            conn.createStatement().execute("""
                    INSERT INTO active_learning_cycle
                        (cycle_id, tenant_id, model_entry_id, model_version_id,
                         strategy_type, status, selected_record_count, passed_review_count,
                         source_dataset_version_id, started_at, auto_trigger,
                         evidence_event_ids,
                         created_at, created_by, updated_at, updated_by, audit_version)
                    VALUES ('al-t2', 'tenant-b', 'm2', 'v2',
                            'DIVERSITY_SAMPLING', 'AWAITING_REVIEW', 20, 0,
                            'dsv-2', '%s', true,
                            '[]', '%s', 'admin', '%s', 'admin', 1)
                    """.formatted(now, now, now));

            var rsA = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) AS cnt FROM active_learning_cycle WHERE tenant_id = 'tenant-a'");
            assertTrue(rsA.next());
            assertEquals(1, rsA.getInt("cnt"));

            var rsB = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) AS cnt FROM active_learning_cycle WHERE tenant_id = 'tenant-b'");
            assertTrue(rsB.next());
            assertEquals(1, rsB.getInt("cnt"));

            var rsAll = conn.createStatement().executeQuery(
                    "SELECT COUNT(*) AS cnt FROM active_learning_cycle");
            assertTrue(rsAll.next());
            assertEquals(2, rsAll.getInt("cnt"));

            conn.createStatement().execute("DELETE FROM active_learning_cycle WHERE tenant_id IN ('tenant-a', 'tenant-b')");
        }
    }

    @Test
    void auditColumnsArePopulatedOnInsert() throws Exception {
        try (Connection conn = pgConnection()) {
            var now = Timestamp.from(Instant.now());
            conn.createStatement().execute("""
                    INSERT INTO dataset_definition
                        (dataset_definition_id, tenant_id, name, domain_type,
                         train_ratio, val_ratio, test_ratio,
                         created_at, created_by, updated_at, updated_by, audit_version)
                    VALUES ('audit-dd', 'audit-tenant', 'Audit Test', 'HR',
                            0.8, 0.1, 0.1,
                            '%s', 'test-creator', '%s', 'test-updater', 1)
                    """.formatted(now, now));

            var rs = conn.createStatement().executeQuery(
                    "SELECT created_by, updated_by, audit_version FROM dataset_definition WHERE dataset_definition_id = 'audit-dd'");
            assertTrue(rs.next());
            assertEquals("test-creator", rs.getString("created_by"));
            assertEquals("test-updater", rs.getString("updated_by"));
            assertEquals(1, rs.getInt("audit_version"));

            conn.createStatement().execute("DELETE FROM dataset_definition WHERE dataset_definition_id = 'audit-dd'");
        }
    }
}
