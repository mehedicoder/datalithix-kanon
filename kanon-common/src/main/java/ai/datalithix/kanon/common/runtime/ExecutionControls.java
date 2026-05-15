package ai.datalithix.kanon.common.runtime;

import java.time.Duration;

public record ExecutionControls(
        Duration timeout,
        int maxAttempts,
        int concurrencyLimit,
        int rateLimitPerMinute
) {
    public ExecutionControls {
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be positive");
        }
        if (concurrencyLimit < 1) {
            throw new IllegalArgumentException("concurrencyLimit must be positive");
        }
        if (rateLimitPerMinute < 1) {
            throw new IllegalArgumentException("rateLimitPerMinute must be positive");
        }
    }
}
