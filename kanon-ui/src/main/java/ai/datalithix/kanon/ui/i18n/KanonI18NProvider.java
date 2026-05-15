package ai.datalithix.kanon.ui.i18n;

import com.vaadin.flow.i18n.I18NProvider;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class KanonI18NProvider implements I18NProvider {
    private final LocalizationService localizationService;

    public KanonI18NProvider(LocalizationService localizationService) {
        this.localizationService = localizationService;
    }

    @Override
    public List<Locale> getProvidedLocales() {
        return localizationService.supportedLocales();
    }

    @Override
    public String getTranslation(String key, Locale locale, Object... params) {
        return localizationService.translate(key, locale, params);
    }
}
