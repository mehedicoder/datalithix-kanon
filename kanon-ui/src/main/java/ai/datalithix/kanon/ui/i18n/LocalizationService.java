package ai.datalithix.kanon.ui.i18n;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.springframework.stereotype.Service;

@Service
public class LocalizationService {
    public static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
    public static final Locale GERMAN_LOCALE = Locale.GERMAN;

    private static final String BUNDLE_NAME = "i18n.messages";
    private static final ResourceBundle.Control NO_FALLBACK_CONTROL =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);
    private static final List<Locale> SUPPORTED_LOCALES = List.of(DEFAULT_LOCALE, GERMAN_LOCALE);

    public List<Locale> supportedLocales() {
        return SUPPORTED_LOCALES;
    }

    public Locale defaultLocale() {
        return DEFAULT_LOCALE;
    }

    public Locale supportedOrDefault(Locale locale) {
        if (locale == null) {
            return DEFAULT_LOCALE;
        }
        return SUPPORTED_LOCALES.stream()
                .filter(supported -> supported.getLanguage().equals(locale.getLanguage()))
                .findFirst()
                .orElse(DEFAULT_LOCALE);
    }

    public String translate(String key, Locale locale, Object... params) {
        Locale effectiveLocale = supportedOrDefault(locale);
        String pattern = getMessage(key, effectiveLocale);
        if (pattern == null && !DEFAULT_LOCALE.equals(effectiveLocale)) {
            pattern = getMessage(key, DEFAULT_LOCALE);
        }
        if (pattern == null) {
            return "!" + key + "!";
        }
        return MessageFormat.format(pattern, params);
    }

    private static String getMessage(String key, Locale locale) {
        try {
            return ResourceBundle.getBundle(BUNDLE_NAME, locale, NO_FALLBACK_CONTROL).getString(key);
        } catch (MissingResourceException ex) {
            return null;
        }
    }
}
