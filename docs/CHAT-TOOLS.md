# Chat tools and export

This is an original implementation in `org.ayuless.chat`; no reference-client sources or new sticker assets were used.

Open a chat's three-dot menu and choose **Ayuless**. The menu and export are always available. The tools operate on the whole dialog, including when opened from a topic.

- Deleted messages use date/id keyset pages of 100, newest first. Only the current page is retained. A tap checks Telegram's local cache before opening the message.
- The deleted list uses only a trash emoji before each message.
- Saving exceptions remain in Spy settings; receipt/typing exceptions and their old preference reads have been removed.
- Clear deleted removes this dialog's saved deleted messages, revisions and saved attachment copies. It preserves observed read dates and Telegram's own cache. Completion waits for attachment deletion and reports failures; the operation cannot be undone.

## Export behavior

Choose message categories, optionally choose inclusive local calendar dates, and choose HTML (default) or JSON. The system folder picker selects a writable SAF directory. The export creates `export_<dialog-id>_<timestamp>/` there; it does not need broad storage permission or a Downloads fallback.

The export worker pages server history in batches of 100. Secret chats have local history only. Saved deleted messages are appended in their own newest-first sequence and carry an explicit deleted marker. Category and date filters apply to both sequences. JSON includes IDs, timestamps, sender, direction, text, entities, reply ID, media path, and missing-media status; it is an Ayuless export schema, not an import format. HTML has escaped message bubbles, sender names when returned by the history API, timestamps, downloaded image/video/audio previews and attachment links. Deleted/local-only senders may be shown by numeric ID.

Media are copied from already downloaded files or plaintext Ayuless saved copies. Missing media, including encrypted-only cached files, are marked unavailable. This version does not fetch missing media, decrypt encrypted cache files, or export the history of a migrated predecessor group. Message categories outside the listed media types (for example polls and service messages) use the text category and are not full structured backups.

All network waits, database reads, media copies and output formatting run off the UI thread. A cancellable progress dialog reports the number of exported messages. Keep the export screen open. Leaving it cancels the task; there is no persistent foreground service or recovery after process termination. Cancellation/error attempts to remove the incomplete export directory; a provider that becomes unavailable may leave partial files. A network operation times out after 60 seconds. The exporter stops if the account changes.

## Verification

The Java/SDK and aapt2 checks below describe the original export implementation, before the menu cleanup.

- Requested offline Gradle command: blocked before compilation by the sandbox's read-only Gradle wrapper lock under `$HOME/.gradle`.
- All changed/new Java files: compiled successfully with JDK 21 `javac`, the Android 36 SDK, existing repository build classes and cached dependency jars. Resource constants were generated in `/tmp` solely for this check. This does not replace a full Gradle build.
- English and Russian `ayuless.xml`: compiled with SDK `aapt2`.

Menu cleanup verification (2026-11-11): the requested offline Gradle command still stops at the read-only wrapper lock before compilation. All 15 remaining changed Java files passed JDK 21 syntax parsing. EN/RU XML keys and numbered placeholders match, and `Tools/test_strings_build.py` passed. These checks do not replace a full Android build.

Device checks still needed: cloud/secret/group/channel chats, multi-page saved history, per-account shared/overridden Ghost profiles, date boundaries, SAF providers that rename files, permission revocation, full storage, cancellation during a media copy, deletion failures, and process termination. Open exported HTML and parse JSON from a real large chat, including attachment names with punctuation and hostile HTML text.
