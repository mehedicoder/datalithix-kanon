# Localization Architecture

Kanon Platform supports localized UI text through Vaadin's `I18NProvider` and resource bundles in `kanon-ui`.

## Supported Languages

V1 supported locales:

- English: default locale, `Locale.ENGLISH`
- German: `Locale.GERMAN`

Unsupported or missing locale values fall back to English.

## Runtime Model

The localization layer lives in `kanon-ui`:

- `LocalizationService` is the Spring service that owns supported locales, fallback behavior, resource bundle lookup, and message formatting.
- `KanonI18NProvider` is the Vaadin adapter. It delegates translation and locale discovery to `LocalizationService`.
- `I18n` is the lightweight UI helper for shared components that need translated labels.
- `messages.properties` contains the base fallback text.
- `messages_en.properties` contains explicit English text.
- `messages_de.properties` contains German text.

`LocalizationService` disables JVM default-locale fallback when loading bundles. This prevents a German JVM or operating-system locale from being used accidentally when the requested UI locale is English. Missing German keys fall back manually to English through `LocalizationService`.

The selected language is stored on `user_account.preferred_locale` when the user chooses a language in the Profile view. The application shell resolves the locale in this order:

1. User preferred locale
2. Active tenant `default_locale`
3. English fallback

The resolved locale is also stored in the Vaadin session as `kanon.locale` for the active UI session. The Profile view reloads the page after changing the locale so navigation labels, dialogs, and shared controls are recreated with the selected language.

## UI Rules

All user-facing strings in Vaadin views and shared UI components should use translation keys unless the value is domain data loaded from the database.

Use stable keys instead of copying text into components directly:

```java
new Button(I18n.t("action.close"));
```

Spring-managed classes that are not Vaadin presentation helpers should inject `LocalizationService` directly:

```java
localizationService.translate("action.close", locale);
```

Key naming conventions:

- `app.*` for global application labels
- `nav.*` for navigation items
- `action.*` for common actions
- `dialog.*` for shared dialog text
- `validation.*` for shared validation errors
- `about.*` for the product about dialog

View-specific text should use a view prefix, for example `admin.tenant.create.title`, when those views are localized.

## Tenant and User Preferences

The implementation stores the tenant default locale on `tenant.default_locale` and the user override on `user_account.preferred_locale`:

1. User preference overrides all other defaults.
2. Tenant default locale applies when no user preference exists.
3. English remains the platform fallback.

Tenant-scoped administration should not allow one tenant to change another tenant's default locale.

## Backend and API Text

Backend APIs should return stable codes for validation and failure states. The UI translates those codes at the presentation boundary. Server logs, audit event types, and evidence event types should remain stable machine-readable values; localized explanatory text belongs in the UI.

## Adding a Language

To add a new language:

1. Add the locale to `LocalizationService`.
2. Add a matching `messages_<locale>.properties` file.
3. Keep `messages.properties` and `messages_en.properties` aligned for base and explicit English fallback.
4. Add a label in the language selector.
5. Compile and run the UI smoke path.
