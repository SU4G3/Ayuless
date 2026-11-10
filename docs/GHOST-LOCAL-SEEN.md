# Local ghost markers and explicit reads

The default-off **Remember seen markers locally** option is in Ayuless → Ghost
mode and follows the selected shared/account profile. It only applies while
that account's effective hide-read option is active.

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
the local watermarks intact. Disabling the option or hiding-read policy bypasses
the overlay.

**Mark as read** / **Прочитать**, with the existing Solar eye drawable, is always
available in chat-list preview/selection menus and the normal chat header menu.
It has no setting. With hidden reads enabled, only the request instances created
for that action bypass the interceptor: history, mentions, reactions and poll
votes for the selected dialog. Chunked affected-history replies are completed
before reporting success. Local cleanup follows successful receipts. Secret
history reads use dates and start the normal local TTL task after success.
With hidden reads off, the action uses upstream controller methods. The separate
mark-as-unread action remains available.

Automatic delayed sends retain their existing scheduling calculations. A local
message parameter identifies their origin, including forwards, retries and
protected-content copies prepared asynchronously. The two automatic scheduled
navigation paths ignore these messages; explicit scheduling keeps its normal
navigation. The delayed-send hint toast was removed.

## Verification

An isolated Java 21 harness compiled the production `GhostSeen`,
`GhostInterceptor` and `DelayedSend` against dependency stubs and passed 53
checks. These cover restarts, account/dialog isolation, new reactions on old
messages, mentions and poll snapshots, counter reconciliation and pagination,
jump filtering, option changes during requests, explicit one-time receipts,
chunked receipts, failure handling, upstream delegation, secret TTL and delayed
send origin. This is not an Android integration test.

The requested offline release Java compilation stopped before compilation:
the sandbox cannot create the Gradle wrapper lock under `~/.gradle` (read-only
filesystem). Java syntax parsing and EN/RU XML/resource checks were also run.
Device checks remain: badge/jump-button restoration after process death with
server and cache loads, offline reloads, topics, reaction animations, manual
read acknowledgements observed from another account, and delayed text/media/
album/forward/copy sends versus explicit scheduling. No reference-client source
was used for this implementation.
