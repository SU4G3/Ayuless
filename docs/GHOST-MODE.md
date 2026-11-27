# Ghost exceptions and read receipt hints

These additions are for the next release; they are not included in the published
Ayuless 1.0.0 APK.

## User exceptions

Open **Settings → Ayuless → Ghost mode → Exceptions** and select users. The list
belongs to the profile selected at the top of the ghost settings screen: either
the shared profile or an account's own profile.

Selected users can receive normal read receipts, typing indicators and views of
their stories. This also covers a secret chat with the selected user. A user
exception does not exempt a group containing that user. Unknown peers remain
protected by the existing ghost settings.

Online/offline status is account-wide, so its ghost settings still apply to the
whole account. The independent silent-send and delayed-send preferences also
retain their existing behavior. Selecting an exception does not immediately send
receipts for old messages; normal chat activity can send them afterwards.

## Read receipt hint

Enable **Settings → Ayuless → Customization → Read receipt status in message
menu**. While ghost mode is active, open an incoming message's menu in a
non-secret private chat. A hint appears in the same panel as the original
forwarding restriction, and both hints can appear together.

- **Read receipt sent:** the account has acknowledged this message as read.
- **Read receipt not sent:** the account has not acknowledged it as read.
- **Read receipt status unavailable:** the query failed or did not return this
  dialog. This is not treated as an unread result.

The hint uses a read-only dialog query. Opening it does not send a read receipt
or replace local unread counters with server data. It does not use the local
message's read flag: ghost mode can clear that flag without telling the sender.
It shows the account's receipt state, including reads from another logged-in
device, rather than whether the message was viewed only on this phone.

The hint is disabled by default and does not apply to outgoing messages, Saved
Messages, groups, channels, scheduled messages or secret chats.

## Device checks

1. With hidden reads enabled, compare an excepted private user and a user who is
   not excepted. Check read receipts, typing, story views and voice-message
   listening receipts. Repeat after removing the exception.
2. Check the same user in a secret chat, and confirm a group containing that user
   still follows the ordinary ghost policy.
3. Switch between shared and per-account profiles. Confirm their exception lists
   and receipt behavior remain separate.
4. Enable the menu hint and inspect an incoming private message read only under
   ghost mode. Then use **Mark as read** and reopen the menu. Compare with the
   sender's client. Also test with network access unavailable.
5. Inspect a protected message: the forwarding restriction and receipt hint must
   both fit the popup in light and dark themes.
