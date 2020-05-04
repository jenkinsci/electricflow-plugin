package org.jenkinsci.plugins.electricflow.ui;

import org.owasp.encoder.Encode;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

public class HtmlUtils {
  public static PolicyFactory getHtmlPolicy() {
    return new HtmlPolicyBuilder()
        .allowElements("h3", "table", "tr", "td", "a", "b", "pre")
        .allowAttributes("href")
        .onElements("a")
        .allowAttributes("cellspacing", "cellpadding")
        .onElements("table")
        .allowStyling()
        .allowStandardUrlProtocols()
        .toFactory();
  }

  public static String encodeForHtml(String input) {
    return Encode.forHtml(input);
  }
}
