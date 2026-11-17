# Content retention

All new options are per account and default to off, under Ayuless → Spy.
The existing group/channel saving behavior is preserved.

Anti-delete, edit history and saved attachments each have separate opt-ins for
secret chats, timed messages and encrypted service actions. Enable the base Spy
feature and every applicable category. For example, retaining a timed secret
message needs both secret-chat and timed-message options for that feature.
Spy exceptions and existing storage quotas still apply.

“Keep self-destruct and view-once media” suppresses local expiry independently of
those Spy features. It preserves the cached file and media descriptor; it cannot
recover media that has already disappeared or was never downloaded. Ordinary
cache cleanup and explicit user deletion remain available. The official open
receipt path remains in place. Open receipts for retained media are remembered
per account and message to prevent another receipt when reopening after restart.

The restricted-content section controls private-chat sharing restrictions,
auto-delete chats, timed media, secret chats and protected stories independently.
Reopen the chat after changing these options. Protected messages and stories are
shared as downloaded media copies; a missing download produces the existing
“download first” message. Saving an encrypted cache file explicitly decrypts it
into the private application cache before exporting it.

Database export/import includes secret-chat and timed-message rows, regardless
of the receiving account’s capture settings. As before, the database export does
not bundle attachment files or Telegram secret-chat session keys.

## Validation

- JavaParser accepted all changed Java sources (Java 17 syntax).
- An isolated JVM harness exercised the actual `RetentionConfig` eligibility code
  against lightweight Telegram/settings stubs: 181 assertions passed, covering
  defaults, feature/account isolation, combined categories, each timer field,
  view-once media, null messages and Spy exceptions.
- EN/RU resource XML parsed successfully with unique resource names.
- `git diff --check` passed.
- The requested offline Gradle build stopped before compilation because its
  wrapper lock is outside the writable sandbox. Direct Gradle with a writable
  temporary user home also stopped before compilation: no usable wildcard IP.

Device/integration verification remains required:

1. With all options off, confirm upstream secret/TTL deletion and saving restrictions.
2. Toggle each Spy category separately and together; delete/edit cloud and secret
   messages, including negative secret-message IDs and encrypted service actions.
3. Download timed photos/videos and view-once voice/round video, open them, wait
   for expiry, close/reopen, restart the app and reopen again. Inspect the peer’s
   received open notifications/network requests for duplicates.
4. Exercise pending expiry tasks created before enabling retention, multi-message
   timer batches, exceptions, local deletion, account switching and logout/reuse.
5. Save/copy/forward protected private, auto-delete, TTL and secret content. Check
   screenshots in chat, photo, secret-media and voice viewers with each option.
6. Save protected public/private stories and send them as media copies, with and
   without downloaded media and with a topic/paid destination.
7. Export/import the retained rows with capture options off. Clear normal media
   cache and restore saved plain and encrypted attachments; exercise quota eviction.
