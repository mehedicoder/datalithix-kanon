package ai.datalithix.kanon.ui.component;

import ai.datalithix.kanon.ui.i18n.I18n;
import com.vaadin.flow.component.html.Span;

public class RedactedText extends Span {
    private static final String REDACTED_VALUE = "••••••••";

    public RedactedText(String fieldName, String value, RedactionContext context) {
        boolean redacted = SensitiveTextClassifier.shouldRedact(context, fieldName, value);
        setText(redacted ? REDACTED_VALUE : valueOrEmpty(value));
        addClassName(redacted ? "kanon-redacted-text" : "kanon-visible-text");
        if (redacted) {
            getElement().setAttribute("title", I18n.t("redaction.hidden.tooltip"));
            getElement().setAttribute("aria-label", I18n.t("redaction.hidden.aria"));
        }
    }

    public static RedactedText sensitiveField(String fieldName, String value) {
        return new RedactedText(fieldName, value, RedactionContext.SENSITIVE_FIELD);
    }

    public static RedactedText payloadPreview(String fieldName, String value) {
        return new RedactedText(fieldName, value, RedactionContext.PAYLOAD_PREVIEW);
    }

    public static RedactedText secret(String fieldName, String value) {
        return new RedactedText(fieldName, value, RedactionContext.SECRET);
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
