package ai.datalithix.kanon.airouting.model;

import ai.datalithix.kanon.common.runtime.ExecutionControls;
import ai.datalithix.kanon.common.runtime.RetryPolicy;

public record ModelExecutionPolicy(
        ExecutionControls executionControls,
        RetryPolicy retryPolicy,
        String fallbackProfileKey,
        boolean asyncRequired,
        boolean healthCheckRequired,
        boolean evidenceRequired
) {
    public ModelExecutionPolicy {
        if (executionControls == null) {
            throw new IllegalArgumentException("executionControls is required");
        }
        if (retryPolicy == null) {
            throw new IllegalArgumentException("retryPolicy is required");
        }
        if (!asyncRequired) {
            throw new IllegalArgumentException("model invocation must have an async boundary");
        }
        if (!healthCheckRequired) {
            throw new IllegalArgumentException("model profiles must expose health checks");
        }
        if (!evidenceRequired) {
            throw new IllegalArgumentException("model routing and fallback decisions must emit evidence");
        }
    }
}
