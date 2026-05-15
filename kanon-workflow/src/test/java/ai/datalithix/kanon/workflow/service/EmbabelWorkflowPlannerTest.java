package ai.datalithix.kanon.workflow.service;

import ai.datalithix.kanon.common.AiTaskType;
import ai.datalithix.kanon.common.DomainType;
import ai.datalithix.kanon.common.TenantContext;
import ai.datalithix.kanon.domain.model.TaskDescriptor;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbabelWorkflowPlannerTest {
    @Test
    void usesEmbabelClientPlanWhenGoalIsSatisfied() {
        EmbabelWorkflowPlanner planner = new EmbabelWorkflowPlanner(
                provider(problem -> new EmbabelPlanningResult(
                        true,
                        true,
                        List.of("store-payload", "produce-annotations"),
                        "Embabel GOAP plan",
                        null
                )),
                provider(null),
                new DefaultWorkflowPlanner()
        );

        var plan = planner.plan(tenantContext(), taskDescriptor());

        assertEquals("annotation_extraction-embabel-workflow", plan.workflowKey());
        assertEquals(List.of("store-payload", "produce-annotations"), plan.plannedSteps());
        assertEquals("Embabel GOAP plan", plan.rationale());
    }

    @Test
    void fallsBackWhenEmbabelCannotPlan() {
        EmbabelWorkflowPlanner planner = new EmbabelWorkflowPlanner(
                provider(problem -> new EmbabelPlanningResult(false, false, List.of(), null, "no path")),
                provider(null),
                new DefaultWorkflowPlanner()
        );

        var plan = planner.plan(tenantContext(), taskDescriptor());

        assertTrue(plan.rationale().startsWith("Embabel planner fallback"));
        assertEquals("accounting-default-workflow", plan.workflowKey());
    }

    private static TenantContext tenantContext() {
        return new TenantContext("tenant-a", DomainType.ACCOUNTING, "DE", "EU_AI_ACT_2026", true, false, Set.of("AUDIT_REQUIRED"));
    }

    private static TaskDescriptor taskDescriptor() {
        return new TaskDescriptor(AiTaskType.EXTRACTION, "case-1", "memory://input", "v1", false);
    }

    private static <T> ObjectProvider<T> provider(T instance) {
        return new ObjectProvider<>() {
            @Override
            public T getObject(Object... args) {
                return instance;
            }

            @Override
            public T getIfAvailable() {
                return instance;
            }

            @Override
            public T getIfUnique() {
                return instance;
            }

            @Override
            public T getObject() {
                return instance;
            }
        };
    }
}
