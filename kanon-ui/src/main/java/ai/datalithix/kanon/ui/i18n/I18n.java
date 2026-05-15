package ai.datalithix.kanon.ui.i18n;

import com.vaadin.flow.component.UI;
import java.util.Locale;

public final class I18n {
    private I18n() {}

    public static String t(String key, Object... params) {
        UI ui = UI.getCurrent();
        if (ui == null) {
            return key;
        }
        return ui.getTranslation(key, params);
    }

    public static Locale currentLocale() {
        UI ui = UI.getCurrent();
        if (ui == null || ui.getLocale() == null) {
            return LocalizationService.DEFAULT_LOCALE;
        }
        return ui.getLocale();
    }
}
