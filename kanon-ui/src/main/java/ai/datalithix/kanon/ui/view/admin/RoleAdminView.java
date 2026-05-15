package ai.datalithix.kanon.ui.view.admin;

import ai.datalithix.kanon.tenant.model.CurrentUserContext;
import ai.datalithix.kanon.tenant.model.Role;
import ai.datalithix.kanon.tenant.service.CurrentUserContextService;
import ai.datalithix.kanon.tenant.service.GovernanceAdministrationService;
import ai.datalithix.kanon.ui.component.HowItWorksSection;
import ai.datalithix.kanon.ui.i18n.I18n;
import ai.datalithix.kanon.ui.layout.MainLayout;
import ai.datalithix.kanon.ui.security.AdminSecuredView;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.List;

@PageTitle("Roles | Kanon Platform")
@Route(value = "admin/roles", layout = MainLayout.class)
public class RoleAdminView extends VerticalLayout implements AdminSecuredView {
    private final CurrentUserContextService currentUserContextService;
    private final Grid<Role> grid = new Grid<>(Role.class, false);

    public RoleAdminView(GovernanceAdministrationService service, CurrentUserContextService currentUserContextService) {
        this.currentUserContextService = currentUserContextService;
        setSizeFull();
        setPadding(true);
        add(new HowItWorksSection(
                I18n.t("admin.role.overview.summary"),
                List.of(
                        I18n.t("admin.role.overview.scope-aware"),
                        I18n.t("admin.role.overview.membership"),
                        I18n.t("admin.role.overview.deferred")
                )
        ));
        grid.addColumn(Role::roleKey).setHeader(I18n.t("grid.role")).setAutoWidth(true);
        grid.addColumn(Role::name).setHeader(I18n.t("grid.name")).setAutoWidth(true);
        grid.addColumn(Role::allowedScope).setHeader(I18n.t("grid.scope")).setAutoWidth(true);
        grid.addColumn(role -> String.join(", ", role.permissions())).setHeader(I18n.t("grid.permissions")).setAutoWidth(true);
        grid.setSizeFull();
        grid.setItems(service.roles());
        add(grid);
    }

    @Override
    public CurrentUserContext currentUserContext() {
        return currentUserContextService.currentUser();
    }
}
