package ai.datalithix.kanon.ui.component;

import ai.datalithix.kanon.ui.i18n.I18n;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import java.util.Collection;

public final class AdminDialogSupport {
    private AdminDialogSupport() {}

    public static Dialog dialog(String title, String width) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(title);
        dialog.setWidth(width);
        dialog.setDraggable(true);
        dialog.setResizable(true);
        return dialog;
    }

    public static VerticalLayout body(Component... components) {
        VerticalLayout body = new VerticalLayout(components);
        body.setPadding(false);
        body.setSpacing(true);
        body.setWidthFull();
        return body;
    }

    public static FormLayout form(Component... components) {
        FormLayout form = new FormLayout(components);
        form.setWidthFull();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("640px", 2)
        );
        return form;
    }

    public static H3 sectionTitle(String text) {
        H3 title = new H3(text);
        title.getStyle().set("margin-bottom", "0");
        return title;
    }

    public static Paragraph help(String text) {
        Paragraph paragraph = new Paragraph(text);
        paragraph.getStyle().set("margin-top", "0").set("color", "var(--lumo-secondary-text-color)");
        return paragraph;
    }

    public static TextField requiredText(String label, String helper) {
        TextField field = new TextField(label);
        field.setRequiredIndicatorVisible(true);
        if (helper != null && !helper.isBlank()) {
            field.setHelperText(helper);
        }
        return field;
    }

    public static void footer(Dialog dialog, String primaryLabel, Runnable primaryAction) {
        Button cancel = new Button(I18n.t("action.cancel"), event -> dialog.close());
        Button primary = new Button(primaryLabel, event -> primaryAction.run());
        primary.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout footer = new HorizontalLayout(cancel, primary);
        footer.setWidthFull();
        footer.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);
        dialog.getFooter().add(footer);
    }

    public static void confirmUpdate(String title, String message, Runnable updateAction) {
        Dialog dialog = dialog(title, "520px");
        dialog.add(body(help(message)));
        Button cancel = new Button(I18n.t("action.cancel"), event -> dialog.close());
        Button confirm = new Button(I18n.t("action.confirm-update"), event -> {
            updateAction.run();
            dialog.close();
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout footer = new HorizontalLayout(cancel, confirm);
        footer.setWidthFull();
        footer.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);
        dialog.getFooter().add(footer);
        dialog.open();
    }

    public static void confirmDeletion(String title, String nameToType, Runnable deleteAction) {
        Dialog dialog = dialog(title, "560px");
        TextField confirmation = new TextField(I18n.t("dialog.delete.type-name"));
        confirmation.setWidthFull();
        confirmation.setHelperText(I18n.t("dialog.delete.required-value", nameToType));
        Button cancel = new Button(I18n.t("action.cancel"), event -> dialog.close());
        Button delete = new Button(I18n.t("action.delete"), event -> {
            deleteAction.run();
            dialog.close();
        });
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        delete.setEnabled(false);
        confirmation.addValueChangeListener(event -> delete.setEnabled(nameToType.equals(event.getValue())));
        HorizontalLayout footer = new HorizontalLayout(cancel, delete);
        footer.setWidthFull();
        footer.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);
        dialog.add(body(
                help(I18n.t("dialog.delete.warning")),
                confirmation
        ));
        dialog.getFooter().add(footer);
        dialog.open();
    }

    public static boolean requireFilled(Collection<TextField> fields) {
        boolean valid = true;
        for (TextField field : fields) {
            boolean empty = field.getValue() == null || field.getValue().isBlank();
            field.setInvalid(empty);
            if (empty) {
                field.setErrorMessage(I18n.t("validation.required", field.getLabel()));
                valid = false;
            }
        }
        return valid;
    }
}
