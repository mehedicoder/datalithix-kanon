package ai.datalithix.kanon.ui.i18n;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminLocalizationKeysTest {
    private static final List<String> REQUIRED_KEYS = List.of(
            "nav.agents",
            "nav.workflows",
            "action.search",
            "action.enable",
            "action.disable",
            "action.archive",
            "field.mode",
            "field.workflow-type",
            "field.planner-type",
            "field.task-type",
            "field.model-route-policy",
            "field.goal",
            "grid.mode",
            "grid.last-run",
            "grid.workflow-type",
            "grid.planner-type",
            "grid.task-type",
            "state.never",
            "state.not-available",
            "admin.agent.overview.summary",
            "admin.agent.overview.list",
            "admin.agent.overview.execution",
            "admin.agent.overview.routing",
            "admin.agent.dialog.create",
            "admin.agent.dialog.update",
            "admin.agent.dialog.delete",
            "admin.agent.dialog.archive",
            "admin.agent.name.helper",
            "admin.agent.create.help",
            "admin.agent.update.help",
            "admin.agent.validation.type-mode-required",
            "admin.agent.validation.mode-required",
            "admin.agent.created",
            "admin.agent.updated",
            "admin.agent.enabled",
            "admin.agent.disabled",
            "admin.agent.deleted",
            "admin.agent.archived",
            "admin.agent.restored",
            "admin.agent.confirm.restore.title",
            "admin.agent.confirm.restore.message",
            "admin.workflow.create",
            "admin.workflow.overview.summary",
            "admin.workflow.overview.boundary",
            "admin.workflow.overview.planning",
            "admin.workflow.overview.agent-routing",
            "admin.workflow.dialog.create",
            "admin.workflow.dialog.update",
            "admin.workflow.dialog.delete",
            "admin.workflow.dialog.archive",
            "admin.workflow.name.helper",
            "admin.workflow.model-route-policy.placeholder",
            "admin.workflow.goal.placeholder",
            "admin.workflow.create.help",
            "admin.workflow.update.help",
            "admin.workflow.validation.type-planner-required",
            "admin.workflow.validation.planner-required",
            "admin.workflow.created",
            "admin.workflow.updated",
            "admin.workflow.enabled",
            "admin.workflow.disabled",
            "admin.workflow.archived",
            "admin.workflow.restored",
            "admin.workflow.deleted",
            "admin.workflow.confirm.restore.title",
            "admin.workflow.confirm.restore.message"
    );

    @Test
    void englishAdminAgentAndWorkflowKeysArePresent() {
        assertKeysPresent(Locale.ENGLISH);
    }

    @Test
    void germanAdminAgentAndWorkflowKeysArePresent() {
        assertKeysPresent(Locale.GERMAN);
    }

    private static void assertKeysPresent(Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle("i18n.messages", locale);
        for (String key : REQUIRED_KEYS) {
            assertTrue(bundle.containsKey(key), () -> "Missing i18n key " + key + " for " + locale);
        }
    }
}
