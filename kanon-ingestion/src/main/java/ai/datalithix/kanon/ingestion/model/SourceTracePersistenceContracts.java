package ai.datalithix.kanon.ingestion.model;

import ai.datalithix.kanon.common.persistence.IndexColumn;
import ai.datalithix.kanon.common.persistence.IndexDefinition;
import ai.datalithix.kanon.common.persistence.TableContract;
import ai.datalithix.kanon.common.model.SortDirection;
import java.util.List;

public final class SourceTracePersistenceContracts {
    public static final TableContract SOURCE_TRACES = new TableContract(
            "source_traces",
            true,
            false,
            true,
            List.of(
                    "tenant_id",
                    "case_id",
                    "source_category",
                    "source_type",
                    "source_system",
                    "source_identifier",
                    "compliance_classification",
                    "data_residency"
            ),
            List.of(
                    new IndexDefinition(
                            "idx_source_traces_tenant_case_ingested",
                            List.of(
                                    new IndexColumn("tenant_id", SortDirection.ASC),
                                    new IndexColumn("case_id", SortDirection.ASC),
                                    new IndexColumn("ingestion_timestamp", SortDirection.DESC)
                            ),
                            false,
                            "Case timeline and source trace views"
                    ),
                    new IndexDefinition(
                            "idx_source_traces_tenant_source_identity",
                            List.of(
                                    new IndexColumn("tenant_id", SortDirection.ASC),
                                    new IndexColumn("source_system", SortDirection.ASC),
                                    new IndexColumn("source_identifier", SortDirection.ASC)
                            ),
                            false,
                            "Idempotent lookup by external source identity"
                    ),
                    new IndexDefinition(
                            "idx_source_traces_tenant_correlation",
                            List.of(
                                    new IndexColumn("tenant_id", SortDirection.ASC),
                                    new IndexColumn("correlation_id", SortDirection.ASC)
                            ),
                            false,
                            "Operational lookup by correlation id"
                    )
            )
    );

    public static final TableContract INGESTION_BATCHES = new TableContract(
            "ingestion_batches",
            true,
            false,
            true,
            List.of("tenant_id", "connector_id", "source_system", "status"),
            List.of(
                    new IndexDefinition(
                            "idx_ingestion_batches_tenant_connector_status",
                            List.of(
                                    new IndexColumn("tenant_id", SortDirection.ASC),
                                    new IndexColumn("connector_id", SortDirection.ASC),
                                    new IndexColumn("status", SortDirection.ASC),
                                    new IndexColumn("started_at", SortDirection.DESC)
                            ),
                            false,
                            "Connector operations and retry dashboards"
                    )
            )
    );

    public static final TableContract CONNECTOR_CONFIGURATIONS = new TableContract(
            "connector_configurations",
            true,
            false,
            true,
            List.of("tenant_id", "connector_id", "source_category", "source_type"),
            List.of(
                    new IndexDefinition(
                            "idx_connector_configurations_tenant_type",
                            List.of(
                                    new IndexColumn("tenant_id", SortDirection.ASC),
                                    new IndexColumn("source_category", SortDirection.ASC),
                                    new IndexColumn("source_type", SortDirection.ASC)
                            ),
                            false,
                            "Connector configuration grids and source-type filters"
                    )
            )
    );

    public static final TableContract CONNECTOR_HEALTH = new TableContract(
            "connector_health",
            true,
            false,
            true,
            List.of("tenant_id", "connector_id", "status"),
            List.of(
                    new IndexDefinition(
                            "idx_connector_health_tenant_status",
                            List.of(
                                    new IndexColumn("tenant_id", SortDirection.ASC),
                                    new IndexColumn("status", SortDirection.ASC),
                                    new IndexColumn("last_ingestion_at", SortDirection.DESC)
                            ),
                            false,
                            "Connector health views and lag monitoring"
                    )
            )
    );

    private SourceTracePersistenceContracts() {
    }
}
