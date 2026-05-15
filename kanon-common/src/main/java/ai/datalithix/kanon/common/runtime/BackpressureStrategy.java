package ai.datalithix.kanon.common.runtime;

public enum BackpressureStrategy {
    REJECT_NEW_WORK,
    PAUSE_CONNECTOR,
    DEFER_TO_QUEUE,
    DEAD_LETTER
}
