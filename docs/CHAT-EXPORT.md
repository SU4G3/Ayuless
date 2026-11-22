# Chat export

The chat menu opens the export settings. Text is always included. Photos, videos,
voice messages, round videos, stickers, GIFs and files have separate switches.
Saved deleted messages are optional. The per-file limit defaults to 8 MiB and can
be adjusted between 1 and 4000 MiB. Choose a destination with Android's folder
picker, then press **Export chat**. The start/end dates can be reset independently.
The end date includes the whole selected local calendar day.

Formats: human-readable HTML, machine-readable JSON, or both. Both formats share
one copy of each attachment. Messages, including saved deleted messages, are
merged oldest first using a temporary database in the application's private cache.
The database is removed on completion, cancellation or a handled error. This avoids
keeping an entire chat in memory. An interrupted process may leave cache files
until Android or the user clears the application's cache.

Output:

```text
ChatExport_yyyy-MM-dd_HHmmss_SSS/
  messages.html            # HTML or both
  css/style.css
  js/script.js
  result.json              # JSON or both
  photos/                  # created only when files of this kind are copied
  video_files/
  voice_messages/
  round_video_messages/
  stickers/
  animations/
  files/
```

The HTML independently recreates Desktop export geometry: a narrow history column,
chat title, day separators, sender initials, grouped consecutive messages, timestamps,
reply anchors, formatted text, media controls and inline bot button rows. It follows
the browser's light/dark preference. Assets are bundled locally; no external fonts,
images, libraries or analytics are loaded. Text and attributes are escaped, entity
ranges are checked in UTF-16, and message links allow only HTTP(S), Telegram,
email and telephone schemes. Callback buttons show/copy their exported payload;
they do not invoke Telegram bots. TGS stickers remain downloadable files.

JSON uses Desktop-style `name`, `type`, `id`, `messages`, ISO dates, string Unix
seconds, sender IDs, `text`, `text_entities`, media fields and `inline_bot_buttons`.
Ayuless adds `deleted`, `media_missing`, `media_policy` and original `entities`
with UTF-16 offsets. Flattened `text_entities` represent one formatting type per
segment; the original ranges retain overlapping entities. This is not a promise
of complete Telegram Desktop schema or import compatibility. Service descriptions
are exported as text; advanced media such as polls and contact cards are not fully
represented as structured data. There is no HTML pagination.

Only already downloaded media and Ayuless saved attachments are copied. Unselected,
missing and oversized attachments do not remove the message or its caption. They
are marked as unavailable in the output. The size limit applies to each attachment,
not to the total export. Secret chats export their local history only. Keep the
export settings open until completion. A handled error/cancellation removes the
partial output directory where the selected document provider permits deletion.

Completion displays message count, actual output file count and bytes written,
with **Show my data** opening the output folder. If the file manager cannot view
that directory URI, Android's folder picker is offered instead.

## Verification

Run the production HTML renderer checks without Android:

```sh
javac -d /tmp/ayuless-export-check \
  TMessagesProj/src/main/java/org/ayuless/chat/ExportHtml.java \
  Tools/chat-export/ExportHtmlCheck.java Tools/chat-export/ExportPageCheck.java
java -cp /tmp/ayuless-export-check ExportHtmlCheck
java -cp /tmp/ayuless-export-check ExportPageCheck /tmp/export-preview.html
```

The checks cover HTML/attribute escaping, link schemes, invalid and overlapping
entity ranges, UTF-16 surrogate boundaries, grouping, day separators, replies,
service/deleted messages and bot rows. The fixture contains synthetic data only.

Before release, test on Android:

- HTML, JSON and both; validate JSON with a parser and compare message IDs/counts.
- Text-only export retains captions and messages with unchecked media types.
- Every media switch and per-file limit, including a missing file and TGS sticker.
- First/last selected dates, resets, empty history and an end-of-day message.
- A chat spanning several history pages; a secret chat and saved deleted messages.
- Cancel during history loading and during media copying; account switch; unwritable
  storage or insufficient space. Verify partial output and cache cleanup.
- Output links and **Show my data** with the device's document provider/file manager.

Desktop JVM tests and Java compilation do not verify Android SAF behavior or device
rendering. Full APK packaging and phone checks are separate release steps.
