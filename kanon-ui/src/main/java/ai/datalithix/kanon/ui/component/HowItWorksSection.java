package ai.datalithix.kanon.ui.component;

import ai.datalithix.kanon.ui.i18n.I18n;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.List;

public class HowItWorksSection extends VerticalLayout {

    public HowItWorksSection(String previewText, List<String> details) {
        setPadding(false);
        setSpacing(false);
        setWidthFull();

        Span preview = new Span(previewText + "... ");
        Button more = new Button(I18n.t("action.more"), event -> openDetailsDialog(details));
        more.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

        HorizontalLayout summary = new HorizontalLayout(preview, more);
        summary.setSpacing(false);
        summary.setAlignItems(Alignment.BASELINE);
        summary.setWidthFull();

        add(summary);
    }

    private static void openDetailsDialog(List<String> details) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(I18n.t("dialog.more.title"));

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.setMaxWidth("760px");
        content.add(new Paragraph(I18n.t("dialog.more.intro")));

        UnorderedList list = new UnorderedList();
        details.stream()
                .map(ListItem::new)
                .forEach(list::add);
        content.add(list);

        Button close = new Button(I18n.t("action.close"), event -> dialog.close());
        close.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(content);
        dialog.getFooter().add(close);
        dialog.open();
    }
}
