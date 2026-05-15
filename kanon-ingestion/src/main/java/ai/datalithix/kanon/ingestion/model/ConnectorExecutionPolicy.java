package ai.datalithix.kanon.ingestion.model;

import ai.datalithix.kanon.common.runtime.BackpressurePolicy;
import ai.datalithix.kanon.common.runtime.ExecutionControls;
import ai.datalithix.kanon.common.runtime.PayloadTransferPolicy;
import ai.datalithix.kanon.common.runtime.RetryPolicy;

public record ConnectorExecutionPolicy(
        ExecutionControls executionControls,
        RetryPolicy retryPolicy,
        BackpressurePolicy backpressurePolicy,
        PayloadTransferPolicy payloadTransferPolicy,
        int batchSize,
        boolean idempotencyRequired,
        boolean checkpointingRequired,
        boolean asyncRequired
) {
    public ConnectorExecutionPolicy {
        if (executionControls == null) {
            throw new IllegalArgumentException("executionControls is required");
        }
        if (retryPolicy == null) {
            throw new IllegalArgumentException("retryPolicy is required");
        }
        if (backpressurePolicy == null) {
            throw new IllegalArgumentException("backpressurePolicy is required");
        }
        if (payloadTransferPolicy == null) {
            throw new IllegalArgumentException("payloadTransferPolicy is required");
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
        if (!idempotencyRequired) {
            throw new IllegalArgumentException("connectors must require idempotency");
        }
        if (!asyncRequired) {
            throw new IllegalArgumentException("connector execution must have an async boundary");
        }
    }
}
