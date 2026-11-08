# Ghost read receipts and secret-chat filters

## Read path audit

`ConnectionsManager.sendRequestInternal` invokes `GhostInterceptor` for both
asynchronous requests and `sendRequestSync`, before serialization. With the
effective `GhostConfig.hideRead(account)` enabled, it blocks history reads for
users/groups, channels, saved dialogs, discussions and secret chats, as well as
individual content reads. It also blocks mention, reaction and poll-vote reads
that the chat can issue while scrolling after a send.

`ChatActivity` marks visible history read and flushes pending reads on pause.
`MessagesController.markDialogAsRead` updates local state and queues a `ReadTask`;
`completeReadTask` builds the appropriate history RPC. Those RPCs pass through
the interceptor even when a send causes scrolling or the task is flushed later.
The `markDialogAsRead` call in `SendMessagesHelper` belongs to bot callback
handling, not ordinary text/media sending; it reaches the same interceptor.
Local unread state is intentionally independent of remote receipts.

Secret content receipts are different: `MessagesController.markMessageAsRead`
calls `SecretChatHelper.sendMessagesReadMessage`, which embeds
`decryptedMessageActionReadMessages` in `messages.sendEncryptedService`.
The network interceptor cannot inspect its encrypted payload. The helper now
checks `hideRead` before constructing or resending that receipt. Other encrypted
service messages (key exchange, TTL settings, transport acknowledgments, etc.)
remain available.

`Read on actions` is an existing, default-off exception, effective only with
hidden reads. Ordinary sends and reactions explicitly allow one history read;
secret text/media sends now do the same using `readEncryptedHistory` and a date.
Encrypted service packets never trigger that exception. The option is checked
again on the UI queue and when the one-time permission is consumed, so turning
it off while a read is queued revokes that permission. Ordinary queued reads
never inherit permission merely because the option is on.

The accessible AyuGram smali was inspected for its request classifier and
`isMarkReadAfterAction` handling. No AyuGram implementation was copied.

Telegram documents the separate history and encrypted content receipt methods
in [Views and read metrics](https://core.telegram.org/api/views). Neither this
source audit nor the isolated checks establish whether a live Telegram server
implicitly advances read state upon sending. If it does so without a client read
request, filtering client requests cannot prevent it. Verify remote checkmarks
with a second account; local unread counters are not evidence of remote reads.

## Filter behavior

The handoff commit removed the secret-dialog exclusion. Secret chats share the
existing private-chat regex switch; shadow bans and hide-blocked rules use the
sender's user ID independently of that switch. When a secret message lacks
`from_id`, the cached encrypted-chat participants supply the incoming sender.
No database lookup is performed in drawing or notification code.

The existing shared `MessageFilters` hooks cover loaded/new chat messages in
`ChatActivity`, last-message previews in `DialogCell`, and incoming notifications
in `NotificationsController`. The filters master switch remains off by default.

## Validation and remaining device checks

An isolated Java 21 harness compiled the production `GhostInterceptor`,
`MessageFilters` and `MessageFilter` against dependency stubs and passed 65
checks: ten read RPC types with hidden reads on/off and the action option on/off;
ordinary and secret text/media sends; one-time permissions; option revocation;
service packets; secret regex/private-switch behavior; shadow bans; blocked
users; missing senders for both participant roles; outgoing exclusions; disabled
filters; ordinary private/group regression cases. This does not replace Android
compilation or network tests.

The requested offline Gradle command stopped before compilation because the
sandbox cannot write the wrapper lock under `~/.gradle`. Running the installed
Gradle with a writable `/tmp` user home also failed before compilation because
Gradle could not determine a usable wildcard IP.

On devices, check both secret-chat participant roles, history reloads, new
messages, previews and notifications. With hidden reads enabled, compare remote
checkmarks after text, media, forwards, replies and reactions with `Read on
actions` off/on in private chats, groups, channels, topics and secret chats.
Also check pending reads on backgrounding, notification replies, account/profile
switches and locked ghost toggles. Secret media playback and TTL behavior need
particular attention because their encrypted content receipts are suppressed.
