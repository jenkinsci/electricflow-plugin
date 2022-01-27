package org.jenkinsci.plugins.electricflow;

import org.jenkinsci.plugins.electricflow.ui.HtmlUtils;
import org.junit.Assert;
import org.junit.Test;

public class HtmlSanitizerTest {

    @Test
    public void simpleSanitize() {
        String sanitized = HtmlUtils.getHtmlPolicy().sanitize("<html><head><script>alert('paco');</script></head><body><h1>Hi</h1></body></html>");
        Assert.assertNotNull("sanitized string should not be null", sanitized);
        Assert.assertEquals("The sanitized html is not the one expected", "Hi", sanitized);
    }

    @Test
    public void sanitizeAllowedElements() {
        String sanitized = HtmlUtils.getHtmlPolicy().sanitize("<html><head><script>alert('paco');</script></head><body><h3>hi</h3><b>this is right</b></body></html>");
        Assert.assertNotNull("sanitized string should not be null", sanitized);
        Assert.assertEquals("The sanitized html is not the one expected", "<h3>hi</h3><b>this is right</b>", sanitized);
    }
}
