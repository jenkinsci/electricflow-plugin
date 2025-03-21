package org.jenkinsci.plugins.electricflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.jenkinsci.plugins.electricflow.ui.HtmlUtils;
import org.junit.jupiter.api.Test;

class HtmlSanitizerTest {

    @Test
    void simpleSanitize() {
        String sanitized = HtmlUtils.getHtmlPolicy()
                .sanitize("<html><head><script>alert('paco');</script></head><body><h1>Hi</h1></body></html>");
        assertNotNull(sanitized, "Sanitized string should not be null");
        assertEquals("Hi", sanitized, "The sanitized html is not the one expected");
    }

    @Test
    void sanitizeAllowedElements() {
        String sanitized = HtmlUtils.getHtmlPolicy()
                .sanitize(
                        "<html><head><script>alert('paco');</script></head><body><h3>hi</h3><b>this is right</b></body></html>");
        assertNotNull(sanitized, "Sanitized string should not be null");
        assertEquals("<h3>hi</h3><b>this is right</b>", sanitized, "The sanitized html is not the one expected");
    }
}
