/* SPDX-License-Identifier: GPL-3.0-or-later */
import org.ayuless.chat.ExportHtml;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ExportHtmlCheck {
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        String hostile = "<img src=x onerror=alert(1)> & \" '";
        String html = ExportHtml.render(hostile, Collections.emptyList());
        require(!html.contains("<img") && html.contains("&lt;img"), "HTML escaped");
        require(html.contains("&amp;") && html.contains("&quot;") && html.contains("&#39;"), "attributes escaped");
        require(ExportHtml.safeUrl("javascript:alert(1)") == null, "script rejected");
        require(ExportHtml.safeUrl("data:text/html,test") == null, "data rejected");
        require(ExportHtml.safeUrl("file:///etc/passwd") == null, "local URL rejected");
        require(ExportHtml.safeUrl("https://example.com\nmalicious") == null, "newline rejected");
        require("tg://resolve?domain=test".equals(ExportHtml.safeUrl("tg://resolve?domain=test")), "Telegram link retained");
        List<ExportHtml.Entity> entities = Arrays.asList(new ExportHtml.Entity(0, 5, "bold", null),
                new ExportHtml.Entity(2, 5, "italic", null), new ExportHtml.Entity(0, 7, "text_link", "javascript:alert(1)"));
        html = ExportHtml.render("abcdefg", entities);
        require(html.contains("<strong>ab</strong>") && html.contains("<em><strong>cde</strong></em>"), "overlap retains both styles");
        require(!html.contains("href"), "unsafe link rendered as text");
        StringBuilder restored = new StringBuilder();
        for (ExportHtml.Part part : ExportHtml.parts("abcdefg", entities)) restored.append(part.text);
        require(restored.toString().equals("abcdefg"), "all text retained once");
        entities = Arrays.asList(new ExportHtml.Entity(-1, 2, "bold", null),
                new ExportHtml.Entity(Integer.MAX_VALUE, Integer.MAX_VALUE, "italic", null),
                new ExportHtml.Entity(1, 1, "bold", null));
        require(ExportHtml.render("😀 test", entities).equals("😀 test"), "invalid ranges and split surrogate ignored");
        html = ExportHtml.render("click", Collections.singletonList(new ExportHtml.Entity(0, 5, "text_link", "https://example.com/?a=\"x\"&b=2")));
        require(html.contains("&quot;x&quot;&amp;b=2") && html.contains("rel=\"noreferrer noopener\""), "link escaped");
        require(ExportHtml.render("first\nsecond", Collections.emptyList()).equals("first<br>second"), "line breaks");
        System.out.println("ExportHtmlCheck passed: escaping, URL schemes, overlapping entities, UTF-16 ranges and text preservation");
    }
}
