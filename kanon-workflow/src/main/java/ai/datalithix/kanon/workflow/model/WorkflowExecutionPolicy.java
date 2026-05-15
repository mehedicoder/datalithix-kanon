package ai.datalithix.kanon.workflow.model;

import ai.datalithix.kanon.common.runtime.BackpressurePolicy;
import ai.datalithix.kanon.common.runtime.RetryPolicy;
import java.util.List;

public record WorkflowExecutionPolicy(
        boolean resumable,
        boolean orchestrationStateOnly,
        RetryPolicy retryPolicy,
        BackpressurePolicy backpressurePolicy,
        List<String> idempotentSteps
) {
    public WorkflowExecutionPolicy {
        if (!resumable) {
            throw new IllegalArgumentException("workflows must be resumable");
        }
        if (!orchestrationStateOnly) {
            throw new IllegalArgumentException("workflow state must not carry large payloads");
        }
        if (retryPolicy == null) {
            throw new IllegalArgumentException("retryPolicy is required");
        }
        if (backpressurePolicy == null) {
            throw new IllegalArgumentException("backpressurePolicy is required");
        }
        idempotentSteps = idempotentSteps == null ? List.of() : List.copyOf(idempotentSteps);
    }
}
