package ai.datalithix.kanon.training.model;

public enum TrainingJobStatus {
    REQUESTED, QUEUED, STARTING, RUNNING, CHECKPOINTING, COMPLETED, FAILED, CANCELLED
}
