/* SPDX-License-Identifier: GPL-3.0-or-later */
import org.ayuless.chat.ExportHtml;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.TimeZone;

public final class ExportPageCheck {
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static ExportHtml.Message message(int id, int date, long sender, String name, String text) {
        ExportHtml.Message m = new ExportHtml.Message();
        m.id = id; m.date = date; m.sender = sender; m.name = name; m.text = text; m.mediaType = "text";
        return m;
    }
    public static void main(String[] args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+03:00"));
        StringWriter out = new StringWriter();
        try (ExportHtml.Page page = new ExportHtml.Page(out, "Ayuless · Export preview <test>")) {
            ExportHtml.Message m = message(1, 1795352400, 10, "Example Bot", "🔎 Status\n\nService: available\nLast checked: 19:14:27\nLast error: none");
            m.entities = Arrays.asList(new ExportHtml.Entity(0, 9, "bold", null), new ExportHtml.Entity(20, 9, "bold", null));
            ExportHtml.Button status = new ExportHtml.Button(); status.text = "🔎 Status"; status.data = "stats \" <script>alert(1)</script>";
            ExportHtml.Button link = new ExportHtml.Button(); link.text = "🌐 Open website"; link.url = "https://example.org";
            m.buttons.add(Arrays.asList(status, link)); page.message(m);
            m = message(2, 1795352460, 10, "Example Bot", "Second message from the same sender."); page.message(m);
            m = message(3, 1795352520, 11, "Example User", "A reply with formatting and a link.");
            m.replyId = 1;
            m.entities = Collections.singletonList(new ExportHtml.Entity(13, 10, "italic", null)); page.message(m);
            m = message(4, 1795352580, 10, "Example Bot", "<script>alert('text is escaped')</script>");
            m.deleted = true; m.edited = m.date; page.message(m);
            m = message(5, 1795438800, 11, "Example User", "A new day"); page.message(m);
            m = message(6, 1795438860, 0, "", "Example User joined the group"); m.service = true; page.message(m);
        }
        String html = out.toString();
        require(html.contains("css/style.css") && html.contains("js/script.js"), "Desktop-style asset structure");
        require(html.contains("class=\"message default joined\""), "consecutive sender grouped");
        require(html.contains("href=\"#message1\""), "reply links use message anchors");
        require(html.contains("id=\"deleted_message4\"") && html.contains("Deleted message"), "deleted IDs do not collide");
        require(!html.contains("<script>alert") && html.contains("&lt;script&gt;"), "message and button escaping");
        require(html.contains("22 November 2026") && html.contains("23 November 2026"), "day separators");
        require(html.contains("Example User joined the group"), "service text retained");
        require(html.endsWith("</body></html>"), "page closed");
        if (args.length > 0) Files.write(Paths.get(args[0]), html.getBytes(StandardCharsets.UTF_8));
        System.out.println("ExportPageCheck passed: production renderer, grouping, day separators, replies, deleted/service messages, buttons and escaping");
    }
}
