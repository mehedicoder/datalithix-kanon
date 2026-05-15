package ai.datalithix.kanon.common.runtime;

public record BackpressurePolicy(
        int maxQueueDepth,
        BackpressureStrategy strategy
) {
    public BackpressurePolicy {
        if (maxQueueDepth < 1) {
            throw new IllegalArgumentException("maxQueueDepth must be positive");
        }
        if (strategy == null) {
            strategy = BackpressureStrategy.REJECT_NEW_WORK;
        }
    }
}
