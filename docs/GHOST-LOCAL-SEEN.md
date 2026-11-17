# Local ghost markers and explicit reads

Local seen markers are always remembered while the account's effective hide-read option is active. There is no separate setting or profile preference for this behavior.

`org.ayuless.ghost.GhostSeen` records markers when the upstream chat actually
displays/clears them. Its preferences are isolated by account slot and dialog;
topic counters are tracked separately. Logout clears the account's records.
Mentions use message IDs. Reactions use peer, reaction and event date, so a new
reaction to an old message is still unread. Poll votes use a result snapshot.
The overlay is applied to loaded message objects, live reaction/poll updates,
dialog counters and chat jump buttons. Cached counters are provisionally
adjusted using the stored intersection; read-only `getUnreadMentions`,
`getUnreadReactions` and `getUnreadPollVotes` pages reconcile the exact counters
and jump targets. These queries never acknowledge reading. Failed queries leave
the local watermarks intact. Disabling the hide-read policy bypasses the overlay.

**Mark as read** / **Прочитать**, with the existing Solar eye drawable, is available
in a message's long-press context menu. It reads history up to and including that
message; it is absent from chat-list preview/selection menus and the chat header.
Only the bounded history request bypasses ghost mode. It does not send dialog-wide
mention, reaction or poll-vote acknowledgements. Chunked affected-history replies
are completed before reporting success. Local unread counts and notifications
preserve later unread messages. Secret history receipts use the selected message's
date (Telegram's secret-chat protocol uses second-resolution dates) and start the
normal local TTL task after success. The separate mark-as-unread action and the
configured chat swipe gesture remain available; the swipe uses the normal ghost
policy.

Automatic delayed sends retain their existing scheduling calculations. A local
message parameter identifies their origin, including forwards, retries and
protected-content copies prepared asynchronously. The two automatic scheduled
navigation paths ignore these messages; explicit scheduling keeps its normal
navigation. The delayed-send hint toast was removed.

## Verification

Before the message-action cleanup, an isolated Java 21 harness compiled the production `GhostSeen`,
`GhostInterceptor` and `DelayedSend` against dependency stubs and passed 53
checks. These cover restarts, account/dialog isolation, new reactions on old
messages, mentions and poll snapshots, counter reconciliation and pagination,
jump filtering, option changes during requests, explicit one-time receipts,
chunked receipts, failure handling, upstream delegation, secret TTL and delayed
send origin. This is not an Android integration test.

After the cleanup (2026-11-11), a separate JDK 21 harness compiled the extracted production message-read methods against stubs. Private/channel ID boundaries, secret-chat dates and TTL, zero/nonzero local unread counts, invalid messages/peers and failed requests passed. SQLite cases checked normal/reverse ID bounds, read-state bits, outgoing messages and dialog isolation. This does not verify Android integration or server behavior.

The requested offline release Java compilation stopped before compilation:
the sandbox cannot create the Gradle wrapper lock under `~/.gradle` (read-only
filesystem). Java syntax parsing and EN/RU XML/resource checks were also run.
Device checks remain: badge/jump-button restoration after process death with
server and cache loads, offline reloads, topics, reaction animations, manual
read acknowledgements observed from another account, and delayed text/media/
album/forward/copy sends versus explicit scheduling. No reference-client source
was used for this implementation.
