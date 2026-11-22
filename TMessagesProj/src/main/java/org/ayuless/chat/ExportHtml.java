/* SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors */
package org.ayuless.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/** Escaping and UTF-16 entity rendering shared by the exporter and regression checks. */
public final class ExportHtml {
    private ExportHtml() { }
    public static final class Entity {
        public final int offset, length;
        public final String type, url;
        public Entity(int offset, int length, String type, String url) {
            this.offset = offset; this.length = length; this.type = type; this.url = url;
        }
    }
    public static final class Part {
        public final String text;
        public final List<Entity> entities;
        Part(String text, List<Entity> entities) { this.text = text; this.entities = entities; }
    }
    public static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
    public static String safeUrl(String value) {
        if (value == null || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) return null;
        String lower = value.toLowerCase(Locale.ROOT);
        for (String scheme : new String[]{"https://", "http://", "tg://", "mailto:", "tel:"})
            if (lower.startsWith(scheme)) return value;
        return null;
    }
    private static boolean boundary(String text, int at) {
        return at == 0 || at == text.length() || !Character.isHighSurrogate(text.charAt(at - 1))
                || !Character.isLowSurrogate(text.charAt(at));
    }
    public static List<Part> parts(String text, List<Entity> entities) {
        TreeSet<Integer> cuts = new TreeSet<>(); cuts.add(0); cuts.add(text.length());
        List<Entity> valid = new ArrayList<>();
        for (Entity e : entities) {
            if (e.offset < 0 || e.length <= 0 || e.offset > text.length() - e.length
                    || !boundary(text, e.offset) || !boundary(text, e.offset + e.length)) continue;
            valid.add(e); cuts.add(e.offset); cuts.add(e.offset + e.length);
        }
        List<Part> result = new ArrayList<>(); int start = 0;
        for (int end : cuts) {
            if (end <= start) continue;
            List<Entity> active = new ArrayList<>();
            for (Entity e : valid) if (e.offset <= start && e.offset + e.length >= end) active.add(e);
            result.add(new Part(text.substring(start, end), active)); start = end;
        }
        return result;
    }
    public static String render(String text, List<Entity> entities) {
        StringBuilder result = new StringBuilder();
        for (Part part : parts(text, entities)) {
            String value = escape(part.text).replace("\n", "<br>");
            boolean linked = false;
            for (Entity e : part.entities) {
                switch (e.type) {
                    case "bold": value = "<strong>" + value + "</strong>"; break;
                    case "italic": value = "<em>" + value + "</em>"; break;
                    case "underline": value = "<u>" + value + "</u>"; break;
                    case "strikethrough": value = "<s>" + value + "</s>"; break;
                    case "code": value = "<code>" + value + "</code>"; break;
                    case "pre": value = "<pre>" + value + "</pre>"; break;
                    case "blockquote": value = "<blockquote>" + value + "</blockquote>"; break;
                    case "spoiler": value = "<span class=\"spoiler\">" + value + "</span>"; break;
                    default:
                        String url = safeUrl(e.url);
                        if (url != null && !linked) {
                            value = "<a href=\"" + escape(url) + "\" rel=\"noreferrer noopener\">" + value + "</a>";
                            linked = true;
                        }
                }
            }
            result.append(value);
        }
        return result.toString();
    }
    public static final class Button {
        public String text, url, data;
    }
    public static final class Message {
        public int id, date, edited, replyId, kind;
        public long sender;
        public boolean deleted, service, music;
        public String name, text, file, fileName, mediaType, forwarded;
        public List<Entity> entities = new ArrayList<>();
        public List<List<Button>> buttons = new ArrayList<>();
    }
    public static final class Page implements java.io.Closeable {
        private final java.io.Writer out;
        private String lastDay;
        private long lastSender = Long.MIN_VALUE, lastDate;
        private boolean lastDeleted;
        public Page(java.io.Writer out, String title) throws java.io.IOException {
            this.out = out;
            out.write("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>" + escape(title)
                    + "</title><link rel=\"stylesheet\" href=\"css/style.css\"><script defer src=\"js/script.js\"></script></head><body><div class=\"page_wrap\"><header class=\"page_header\"><div class=\"content\">"
                    + escape(title) + "</div></header><main class=\"page_body chat_page\"><div class=\"history\">");
        }
        private String timestamp(long seconds) {
            return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new java.util.Date(seconds * 1000));
        }
        public void message(Message m) throws java.io.IOException {
            boolean deleted = m.deleted; String file = m.file;
            java.util.Date date = new java.util.Date(m.date * 1000L);
            String day = new java.text.SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH).format(date);
            boolean newDay = !day.equals(lastDay);
            if (newDay) {
                out.write("<div class=\"message service\"><div class=\"body details\">" + day + "</div></div>");
                lastDay = day;
            }
            String id = (deleted ? "deleted_message" : "message") + m.id;
            boolean service = m.service;
            if (service) {
                out.write("<div class=\"message service" + (deleted ? " deleted" : "") + "\" id=\"" + id + "\"><div class=\"body details\">" + escape(m.text) + "</div></div>");
                lastSender = Long.MIN_VALUE;
                return;
            }
            boolean joined = !newDay && m.sender == lastSender && deleted == lastDeleted && m.date - lastDate <= 900;
            String name = m.name;
            StringBuilder initials = new StringBuilder();
            for (String word : name.trim().split("\\s+")) {
                if (!word.isEmpty()) initials.appendCodePoint(word.codePointAt(0));
                if (initials.codePointCount(0, initials.length()) == 2) break;
            }
            out.write("<div class=\"message default" + (joined ? " joined" : "") + (deleted ? " deleted" : "") + "\" id=\"" + id + "\"><div class=\"userpic_wrap\">");
            if (!joined) out.write("<div class=\"userpic userpic" + ((m.sender & 7L) + 1) + "\">" + escape(initials.toString()) + "</div>");
            out.write("</div><div class=\"body\"><a class=\"date details\" href=\"#" + id + "\" title=\""
                    + new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss XXX", Locale.US).format(date) + "\">"
                    + new java.text.SimpleDateFormat("HH:mm", Locale.US).format(date) + "</a>");
            if (!joined) out.write("<div class=\"from_name\">" + escape(name) + "</div>");
            if (deleted) out.write("<div class=\"details\">Deleted message</div>");
            if (m.edited != 0) out.write("<div class=\"details\" title=\"" + timestamp(m.edited) + "\">Edited</div>");
            if (m.forwarded != null) out.write("<div class=\"forwarded details\">Forwarded message" + (m.forwarded.isEmpty() ? "" : " from " + escape(m.forwarded)) + "</div>");
            if (m.replyId != 0) out.write("<div class=\"reply_to details\">In reply to <a href=\"#message" + m.replyId + "\">this message</a></div>");
            if (file != null) {
                String path = escape(file), type = m.mediaType;
                out.write("<div class=\"media_wrap\">");
                if (m.kind == 1 || m.kind == 5 && !file.endsWith(".tgs") && !file.endsWith(".webm"))
                    out.write("<a href=\"" + path + "\"><img loading=\"lazy\"" + (m.kind == 5 ? " class=\"sticker\"" : "") + " src=\"" + path + "\" alt=\"" + type + "\"></a>");
                else if (m.kind == 2 || m.kind == 7 || m.kind == 8 || file.endsWith(".webm"))
                    out.write("<video controls preload=\"none\"" + (m.kind == 7 ? " class=\"round\"" : "") + " src=\"" + path + "\"></video>");
                else if (m.kind == 3 || m.music) out.write("<audio controls preload=\"none\" src=\"" + path + "\"></audio>");
                out.write("<div><a href=\"" + path + "\">" + escape(m.fileName) + "</a></div></div>");
            } else if (m.kind != 0) out.write("<div class=\"media_wrap details\">" + m.mediaType + " · File not included (not selected, not downloaded or over size limit)</div>");
            out.write("<div class=\"text\">" + render(m.text, m.entities) + "</div>");
            for (List<Button> row : m.buttons) {
                out.write("<div class=\"bot_buttons\">");
                for (Button button : row) {
                    String url = safeUrl(button.url);
                    if (url != null) out.write("<a class=\"bot_button\" rel=\"noreferrer noopener\" href=\"" + escape(url) + "\">" + escape(button.text) + "</a>");
                    else if (button.data != null) out.write("<button type=\"button\" class=\"bot_button\" data-copy=\"" + escape(button.data) + "\">" + escape(button.text) + "</button>");
                    else out.write("<span class=\"bot_button\">" + escape(button.text) + "</span>");
                }
                out.write("</div>");
            }
            out.write("</div></div>"); lastSender = m.sender; lastDate = m.date; lastDeleted = deleted;
        }
        @Override public void close() throws java.io.IOException {
            out.write("</div><footer class=\"export_note details\">Exported with Ayuless · Downloaded media only</footer></main></div></body></html>");
        }
    }
}
