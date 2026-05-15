package ai.datalithix.kanon.ui.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.flow.component.UI;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RedactedTextTest {
    @BeforeEach
    void setUp() {
        UI ui = new UI();
        ui.setLocale(Locale.ENGLISH);
        UI.setCurrent(ui);
    }

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
    }

    @Test
    void masksSensitiveValues() {
        RedactedText text = RedactedText.sensitiveField("password", "secret-value");

        assertEquals("••••••••", text.getText());
        assertTrue(text.getClassNames().contains("kanon-redacted-text"));
    }

    @Test
    void leavesOrdinaryValuesVisible() {
        RedactedText text = RedactedText.sensitiveField("status", "APPROVED");

        assertEquals("APPROVED", text.getText());
        assertTrue(text.getClassNames().contains("kanon-visible-text"));
    }
}
