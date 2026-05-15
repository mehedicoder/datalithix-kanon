package ai.datalithix.kanon.ui.view.admin;

import ai.datalithix.kanon.common.security.SecurityAuditEvent;
import ai.datalithix.kanon.tenant.model.CurrentUserContext;
import ai.datalithix.kanon.tenant.service.CurrentUserContextService;
import ai.datalithix.kanon.tenant.service.SecurityAuditQueryService;
import ai.datalithix.kanon.ui.component.HowItWorksSection;
import ai.datalithix.kanon.ui.i18n.I18n;
import ai.datalithix.kanon.ui.layout.MainLayout;
import ai.datalithix.kanon.ui.security.AdminSecuredView;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.List;
import java.util.stream.Collectors;

@PageTitle("Security Audit | Kanon Platform")
@Route(value = "admin/security-audit", layout = MainLayout.class)
public class SecurityAuditView extends VerticalLayout implements AdminSecuredView {
    private final CurrentUserContextService currentUserContextService;

    public SecurityAuditView(SecurityAuditQueryService securityAuditQueryService, CurrentUserContextService currentUserContextService) {
        this.currentUserContextService = currentUserContextService;
        setSizeFull();
        setPadding(true);
        add(new HowItWorksSection(
                I18n.t("admin.security-audit.overview.summary"),
                List.of(
                        I18n.t("admin.security-audit.overview.denials"),
                        I18n.t("admin.security-audit.overview.roles"),
                        I18n.t("admin.security-audit.overview.break-glass")
                )
        ));

        Grid<SecurityAuditEvent> grid = new Grid<>(SecurityAuditEvent.class, false);
        grid.addColumn(SecurityAuditEvent::occurredAt).setHeader(I18n.t("grid.occurred")).setAutoWidth(true);
        grid.addColumn(SecurityAuditEvent::eventType).setHeader(I18n.t("grid.event-type")).setAutoWidth(true);
        grid.addColumn(SecurityAuditEvent::outcome).setHeader(I18n.t("grid.outcome")).setAutoWidth(true);
        grid.addColumn(SecurityAuditEvent::tenantId).setHeader(I18n.t("grid.tenant")).setAutoWidth(true);
        grid.addColumn(SecurityAuditEvent::actorId).setHeader(I18n.t("grid.actor")).setAutoWidth(true);
        grid.addColumn(SecurityAuditEvent::reason).setHeader(I18n.t("grid.reason")).setAutoWidth(true);
        grid.addColumn(this::formatAttributes).setHeader(I18n.t("grid.attributes")).setAutoWidth(true);
        grid.setSizeFull();
        grid.setItems(securityAuditQueryService.recent(200));
        add(grid);
    }

    private String formatAttributes(SecurityAuditEvent event) {
        if (event.attributes().isEmpty()) {
            return "";
        }
        return event.attributes().entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    @Override
    public CurrentUserContext currentUserContext() {
        return currentUserContextService.currentUser();
    }
}
