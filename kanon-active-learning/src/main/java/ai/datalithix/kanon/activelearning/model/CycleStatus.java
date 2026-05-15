package ai.datalithix.kanon.activelearning.model;

public enum CycleStatus {
    SELECTING,
    AWAITING_REVIEW,
    DATASET_UPDATED,
    RETRAINING,
    EVALUATING,
    PROMOTED,
    REJECTED,
    CANCELLED,
    FAILED
}
