package ai.datalithix.kanon.ui.view;

import ai.datalithix.kanon.tenant.model.CurrentUserContext;
import ai.datalithix.kanon.tenant.model.Tenant;
import ai.datalithix.kanon.tenant.model.UserAccount;
import ai.datalithix.kanon.tenant.service.CurrentUserContextService;
import ai.datalithix.kanon.tenant.service.TenantRepository;
import ai.datalithix.kanon.tenant.service.UserProfileService;
import ai.datalithix.kanon.ui.component.HowItWorksSection;
import ai.datalithix.kanon.ui.i18n.I18n;
import ai.datalithix.kanon.ui.i18n.LocalizationService;
import ai.datalithix.kanon.ui.layout.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import java.util.List;
import java.util.Locale;

@PageTitle("Profile | Kanon Platform")
@Route(value = "profile", layout = MainLayout.class)
public class UserProfileView extends VerticalLayout {
    private static final String LOCALE_SESSION_ATTRIBUTE = "kanon.locale";

    private final CurrentUserContextService currentUserContextService;
    private final UserProfileService userProfileService;
    private final TenantRepository tenantRepository;
    private final LocalizationService localizationService;
    private final ComboBox<Locale> language = new ComboBox<>(I18n.t("profile.language"));

    public UserProfileView(
            CurrentUserContextService currentUserContextService,
            UserProfileService userProfileService,
            TenantRepository tenantRepository,
            LocalizationService localizationService
    ) {
        this.currentUserContextService = currentUserContextService;
        this.userProfileService = userProfileService;
        this.tenantRepository = tenantRepository;
        this.localizationService = localizationService;
        setSizeFull();
        setPadding(true);
        add(new HowItWorksSection(
                I18n.t("profile.summary"),
                List.of(
                        I18n.t("profile.detail.user-preference"),
                        I18n.t("profile.detail.tenant-fallback"),
                        I18n.t("profile.detail.reload")
                )
        ));
        addContent();
    }

    private void addContent() {
        UserAccount user = userProfileService.currentUser();
        CurrentUserContext context = currentUserContextService.currentUser();
        String tenantDefaultLocale = tenantRepository.findById(context.activeTenantId())
                .map(Tenant::defaultLocale)
                .filter(value -> value != null && !value.isBlank())
                .orElse(localizationService.defaultLocale().toLanguageTag());

        H2 title = new H2(I18n.t("profile.title"));
        Paragraph help = new Paragraph(I18n.t("profile.language.helper"));

        TextField username = readOnlyField(I18n.t("profile.username"), user.username());
        TextField displayName = readOnlyField(I18n.t("profile.display-name"), user.displayName());
        TextField email = readOnlyField(I18n.t("profile.email"), user.email());
        TextField tenantDefault = readOnlyField(I18n.t("profile.tenant-default-language"), labelFor(languageFromTag(tenantDefaultLocale)));

        language.setItems(localizationService.supportedLocales());
        language.setItemLabelGenerator(this::labelFor);
        language.setClearButtonVisible(true);
        language.setHelperText(I18n.t("profile.language.clear-helper"));
        language.setValue(user.preferredLocale() == null || user.preferredLocale().isBlank()
                ? null
                : languageFromTag(user.preferredLocale()));

        FormLayout form = new FormLayout(username, displayName, email, tenantDefault, language);
        form.setWidthFull();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("640px", 2)
        );

        Button save = new Button(I18n.t("profile.save"), event -> saveLanguagePreference());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout actions = new HorizontalLayout(save);
        actions.setWidthFull();
        actions.setJustifyContentMode(JustifyContentMode.END);

        add(title, help, form, actions);
    }

    private void saveLanguagePreference() {
        Locale selected = language.getValue();
        userProfileService.updatePreferredLocale(selected == null ? null : selected.toLanguageTag());
        Locale effectiveLocale = languageFromTag(userProfileService.effectiveLocale());
        VaadinSession.getCurrent().setAttribute(LOCALE_SESSION_ATTRIBUTE, effectiveLocale);
        UI.getCurrent().setLocale(effectiveLocale);
        Notification.show(I18n.t("profile.saved"));
        UI.getCurrent().getPage().reload();
    }

    private TextField readOnlyField(String label, String value) {
        TextField field = new TextField(label);
        field.setValue(value == null ? "" : value);
        field.setReadOnly(true);
        field.setWidthFull();
        return field;
    }

    private Locale languageFromTag(String languageTag) {
        return localizationService.supportedOrDefault(Locale.forLanguageTag(languageTag));
    }

    private String labelFor(Locale locale) {
        return Locale.GERMAN.getLanguage().equals(locale.getLanguage())
                ? I18n.t("language.german")
                : I18n.t("language.english");
    }
}
