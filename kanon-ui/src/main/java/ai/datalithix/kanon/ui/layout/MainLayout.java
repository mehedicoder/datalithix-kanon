package ai.datalithix.kanon.ui.layout;

import ai.datalithix.kanon.tenant.model.CurrentUserContext;
import ai.datalithix.kanon.tenant.model.Workspace;
import ai.datalithix.kanon.tenant.service.CurrentUserContextService;
import ai.datalithix.kanon.tenant.service.UserProfileService;
import ai.datalithix.kanon.tenant.service.WorkspaceRepository;
import ai.datalithix.kanon.ui.view.admin.ActiveLearningAdminView;
import ai.datalithix.kanon.ui.view.UserProfileView;
import ai.datalithix.kanon.ui.i18n.I18n;
import ai.datalithix.kanon.ui.i18n.LocalizationService;
import ai.datalithix.kanon.ui.security.AdminAccess;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainLayout extends AppLayout implements AfterNavigationObserver {
    private static final String LOCALE_SESSION_ATTRIBUTE = "kanon.locale";

    private final H1 viewTitle = new H1();
    private final CurrentUserContextService currentUserContextService;
    private final WorkspaceRepository workspaceRepository;
    private final LocalizationService localizationService;
    private final UserProfileService userProfileService;

    public MainLayout(
            CurrentUserContextService currentUserContextService,
            WorkspaceRepository workspaceRepository,
            LocalizationService localizationService,
            UserProfileService userProfileService
    ) {
        this.currentUserContextService = currentUserContextService;
        this.workspaceRepository = workspaceRepository;
        this.localizationService = localizationService;
        this.userProfileService = userProfileService;
        applySettings();
        addClassName("kanon-shell");
        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
    }

    private void applySettings() {
        ThemeList themeList = UI.getCurrent().getElement().getThemeList();
        themeList.remove(Lumo.DARK);
        UI.getCurrent().setLocale(resolveSessionLocale());
    }

    private void addHeaderContent() {
        addToNavbar(createHeader());
    }

    HorizontalLayout createHeader() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel(I18n.t("menu.toggle"));

        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.SEMIBOLD);
        viewTitle.getStyle().set("margin", "0");

        HorizontalLayout header = new HorizontalLayout(toggle, viewTitle);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(viewTitle);
        header.setWidthFull();
        header.addClassName("kanon-topbar");
        header.addClassNames(LumoUtility.Padding.Vertical.NONE, LumoUtility.Padding.Horizontal.MEDIUM);
        header.add(createWorkspaceSelector());
        header.add(createUserMenu());

        return header;
    }

    private Locale resolveSessionLocale() {
        VaadinSession session = VaadinSession.getCurrent();
        Object value = sessionAttribute(session);
        if (value instanceof Locale locale) {
            return locale;
        }
        Locale effectiveLocale = localizationService.supportedOrDefault(Locale.forLanguageTag(userProfileService.effectiveLocale()));
        setSessionAttribute(session, effectiveLocale);
        return effectiveLocale;
    }

    private Object sessionAttribute(VaadinSession session) {
        if (session == null) {
            return null;
        }
        try {
            return session.getAttribute(LOCALE_SESSION_ATTRIBUTE);
        } catch (NullPointerException ignored) {
            return null;
        }
    }

    private void setSessionAttribute(VaadinSession session, Locale locale) {
        if (session == null) {
            return;
        }
        try {
            session.setAttribute(LOCALE_SESSION_ATTRIBUTE, locale);
        } catch (NullPointerException ignored) {
            // Component tests can run without a fully initialized servlet-backed VaadinSession.
        }
    }

    private Div createUserMenu() {
        String username = currentUserContextService.currentUser().username();
        Avatar avatar = new Avatar(username);
        avatar.addClassNames(LumoUtility.Margin.Right.SMALL);

        MenuBar userMenu = new MenuBar();
        userMenu.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);

        Div identity = new Div(avatar, new Span(username));
        identity.addClassName("kanon-user-chip");
        identity.getStyle().set("display", "flex").set("align-items", "center");

        MenuItem menuItem = userMenu.addItem(identity);
        menuItem.getSubMenu().addItem(I18n.t("user.profile"), event -> UI.getCurrent().navigate(UserProfileView.class));
        menuItem.getSubMenu().addItem(I18n.t("about.menu"), event -> openAboutDialog());
        menuItem.getSubMenu().addItem(I18n.t("user.sign-out"), event -> logoutCurrentUser());

        return new Div(userMenu);
    }

    void logoutCurrentUser() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(LOCALE_SESSION_ATTRIBUTE, null);
        }
        UI.getCurrent().getPage().setLocation(logoutLocation());
    }

    String logoutLocation() {
        return "logout";
    }

    private void openAboutDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(I18n.t("about.title"));

        Paragraph summary = new Paragraph(I18n.t("about.summary"));
        Paragraph attribution = new Paragraph(I18n.t("about.attribution"));

        Button close = new Button(I18n.t("action.close"), event -> dialog.close());
        close.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Div content = new Div(summary, attribution);
        content.setWidth("560px");
        dialog.add(content);
        dialog.getFooter().add(close);
        dialog.open();
    }

    private ComboBox<Workspace> createWorkspaceSelector() {
        CurrentUserContext context = currentUserContextService.currentUser();
        ComboBox<Workspace> selector = new ComboBox<>();
        selector.setPlaceholder(I18n.t("workspace.selector"));
        selector.setItemLabelGenerator(workspace -> workspace.name() + " / " + workspace.organizationId());
        selector.setItems(workspaceRepository.findByOrganizationId(context.activeTenantId(), context.activeOrganizationId()));
        workspaceRepository.findById(context.activeTenantId(), context.activeOrganizationId(), context.activeWorkspaceId())
                .ifPresent(selector::setValue);
        selector.setWidth("300px");
        selector.addClassNames(LumoUtility.Margin.Right.MEDIUM);
        selector.setReadOnly(true);
        return selector;
    }

    private void addDrawerContent() {
        H1 appName = new H1(I18n.t("app.name"));
        appName.addClassName("kanon-brand-title");
        appName.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.MEDIUM);

        Div brand = new Div(appName);
        brand.addClassName("kanon-brand");
        brand.getStyle().set("padding-bottom", "var(--lumo-space-m)");

        Scroller scroller = new Scroller(createNavigation());
        addToDrawer(brand, scroller);
    }

    SideNav createNavigation() {
        SideNav nav = new SideNav();
        nav.addClassName("kanon-nav");
        CurrentUserContext context = currentUserContextService.currentUser();

        SideNavItem controlPanel = new SideNavItem(I18n.t("nav.control-panel"));
        controlPanel.setPrefixComponent(VaadinIcon.SLIDERS.create());
        controlPanel.addItem(new SideNavItem(I18n.t("nav.command-center"), "", VaadinIcon.DASHBOARD.create()));
        nav.addItem(controlPanel);

        SideNavItem administration = new SideNavItem(I18n.t("nav.administration"));
        administration.setPrefixComponent(VaadinIcon.COG.create());
        List<SideNavItem> administrationItems = administrationItems(context);
        administrationItems.forEach(administration::addItem);
        if (!administrationItems.isEmpty()) {
            nav.addItem(administration);
        }
        return nav;
    }

    List<SideNavItem> administrationItems(CurrentUserContext context) {
        List<SideNavItem> items = new ArrayList<>();
        if (AdminAccess.canViewTenants(context)) {
            items.add(new SideNavItem(I18n.t("nav.tenants"), "admin/tenants", VaadinIcon.BUILDING.create()));
        }
        if (AdminAccess.canViewOrganizations(context)) {
            items.add(new SideNavItem(I18n.t("nav.organizations"), "admin/organizations", VaadinIcon.SITEMAP.create()));
        }
        if (AdminAccess.canViewWorkspaces(context)) {
            items.add(new SideNavItem(I18n.t("nav.workspaces"), "admin/workspaces", VaadinIcon.GRID_BIG.create()));
        }
        if (AdminAccess.canViewUsers(context)) {
            items.add(new SideNavItem(I18n.t("nav.users"), "admin/users", VaadinIcon.USERS.create()));
        }
        if (AdminAccess.canAssignMembership(context)) {
            items.add(new SideNavItem(I18n.t("nav.memberships"), "admin/memberships", VaadinIcon.USER_CHECK.create()));
        }
        if (AdminAccess.canViewRoles(context)) {
            items.add(new SideNavItem(I18n.t("nav.roles"), "admin/roles", VaadinIcon.KEY.create()));
        }
        if (AdminAccess.canReadSecurityAudit(context)) {
            items.add(new SideNavItem(I18n.t("nav.security-audit"), "admin/security-audit", VaadinIcon.SHIELD.create()));
        }
        if (AdminAccess.canViewAgents(context)) {
            items.add(new SideNavItem(I18n.t("nav.agents"), "admin/agents", VaadinIcon.CUBES.create()));
        }
        if (AdminAccess.canViewWorkflows(context)) {
            items.add(new SideNavItem(I18n.t("nav.workflows"), "admin/workflows", VaadinIcon.LIST.create()));
        }
        if (AdminAccess.canViewModels(context)) {
            items.add(new SideNavItem(I18n.t("nav.models"), "admin/models", VaadinIcon.CLOUD.create()));
        }
        if (AdminAccess.canViewAnnotationNodes(context)) {
            items.add(new SideNavItem(I18n.t("nav.annotation-nodes"), "admin/annotation-nodes", VaadinIcon.CONNECT.create()));
        }
        if (AdminAccess.canViewDatasets(context)) {
            items.add(new SideNavItem(I18n.t("nav.datasets"), "admin/datasets", VaadinIcon.DATABASE.create()));
        }
        if (AdminAccess.canViewTrainingJobs(context)) {
            items.add(new SideNavItem(I18n.t("nav.training"), "admin/training", VaadinIcon.COG.create()));
        }
        if (AdminAccess.canViewModelRegistry(context)) {
            items.add(new SideNavItem(I18n.t("nav.model-registry"), "admin/model-registry", VaadinIcon.CLUSTER.create()));
        }
        if (AdminAccess.canViewActiveLearning(context)) {
            items.add(new SideNavItem(I18n.t("nav.active-learning"), "admin/active-learning", VaadinIcon.RETWEET.create()));
        }
        return items;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        String path = event.getLocation().getPath();
        viewTitle.setText(I18n.t(getTitleKeyForPath(path)));
    }

    private String getTitleKeyForPath(String path) {
        return switch (path) {
            case "" -> "nav.command-center";
            case "admin/tenants" -> "nav.tenants";
            case "admin/organizations" -> "nav.organizations";
            case "admin/workspaces" -> "nav.workspaces";
            case "admin/users" -> "nav.users";
            case "admin/memberships" -> "nav.memberships";
            case "admin/roles" -> "nav.roles";
            case "admin/security-audit" -> "nav.security-audit";
            case "admin/agents" -> "nav.agents";
            case "admin/workflows" -> "nav.workflows";
            case "admin/models" -> "nav.models";
            case "admin/annotation-nodes" -> "nav.annotation-nodes";
            case "admin/datasets" -> "nav.datasets";
            case "admin/training" -> "nav.training";
            case "admin/model-registry" -> "nav.model-registry";
            case "admin/active-learning" -> "nav.active-learning";
            case "profile" -> "user.profile";
            default -> "app.name";
        };
    }
}
