package ai.datalithix.kanon.bootstrap;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

@StyleSheet(Lumo.UTILITY_STYLESHEET)
@Theme("kanon-platform")
public class KanonAppShell implements AppShellConfigurator {
}
