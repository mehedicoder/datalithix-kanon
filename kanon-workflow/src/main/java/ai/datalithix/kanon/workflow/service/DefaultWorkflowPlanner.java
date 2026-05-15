package ai.datalithix.kanon.workflow.service;

import ai.datalithix.kanon.common.TenantContext;
import ai.datalithix.kanon.common.runtime.BackpressurePolicy;
import ai.datalithix.kanon.common.runtime.BackpressureStrategy;
import ai.datalithix.kanon.common.runtime.RetryPolicy;
import ai.datalithix.kanon.domain.model.TaskDescriptor;
import ai.datalithix.kanon.workflow.model.WorkflowExecutionPolicy;
import ai.datalithix.kanon.workflow.model.WorkflowPlan;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DefaultWorkflowPlanner implements WorkflowPlanner {
    private static final List<String> DEFAULT_STEPS = List.of("INGEST", "PLAN", "ROUTE_MODEL", "EXECUTE_AGENT", "REVIEW", "APPROVE", "EXPORT");

    @Override
    public WorkflowPlan plan(TenantContext tenantContext, TaskDescriptor taskDescriptor) {
        return new WorkflowPlan(
                tenantContext.domainType().name().toLowerCase() + "-default-workflow",
                taskDescriptor.caseId(),
                taskDescriptor.caseId(),
                DEFAULT_STEPS,
                new WorkflowExecutionPolicy(
                        true,
                        true,
                        new RetryPolicy(3, Duration.ofSeconds(1), Duration.ofSeconds(30)),
                        new BackpressurePolicy(1_000, BackpressureStrategy.DEFER_TO_QUEUE),
                        DEFAULT_STEPS
                ),
                "Baseline workflow plan generated from tenant domain and task type"
        );
    }
}
