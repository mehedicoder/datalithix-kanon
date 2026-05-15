package ai.datalithix.kanon.common.runtime;

@FunctionalInterface
public interface HealthIndicator {
    ComponentHealth health();
}
