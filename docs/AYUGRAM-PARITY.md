# AyuGram parity

Reference: read-only `../ayugram4a` at `7013145676d36d82ee13c02a89f72097b7490dcd`, `TMessagesProj/src/main/java/com/radolyn/ayugram/`,
EN/RU `res/values*/ayu.xml`, and the AyuGram hooks in Telegram sources.
The inventory covers every Radolyn Java file (configuration, preferences, filters,
message/database entities and DAOs, history cells, forwarding/waiters, ghost helpers,
sync controller/models/socket, utilities), not the surrounding exteraGram client.

All new opt-ins default OFF. Existing Ayuless preference names, account profiles,
Spy exceptions, saved databases and retention switches remain compatible. The
pre-existing retention behavior is not enabled or disabled by this port.

| Feature | AyuGram | Ayuless before | Ported now / skipped + reason |
| --- | --- | --- | --- |
| Settings structure | Ghost essentials, Spy essentials, Useful features, Customization, Sync, Debug | Category links to separate screens | Ported: inline sections, expandable ghost controls, retained profile/appearance/storage screens; Telegram cells only |
| Ghost master | Expandable switch, four core options | Profiles, locks, five options; initially empty master | Ported: first explicit activation seeds four core options; existing profiles and locks keep their values |
| Read/online/typing suppression | Request hooks | Already implemented, including secret chats and stories | Retain existing interception; align labels and controls |
| Immediate offline | Offline after online/send | Offline 1.5 s after sends | Ported: online-status requests also trigger the existing offline packet; 1.5-second Ayuless delay retained |
| Read after reply / schedule messages | Mutually exclusive switches | Independent read-on-action and delayed send | Ported: future changes are mutually exclusive; old keys retained; read-after-reply waits for successful acknowledgement; silent sends/encrypted delay retained |
| Read until selected message | Explicit one-request bypass | Missing | Ported: opt-in message context action with a request-identity bypass, including encrypted history |
| Read all confirmation | Explicit ghost read bypass | Missing | Skipped separate confirmation: these strings are unreferenced in supplied Java source; explicit Read until is implemented |
| Anti-delete | Saved messages merged into history | Existing persistent anti-delete and local-delete exclusion | Retain working store and hooks; configurable bot handling added below |
| Edit history | Full screen with message bubbles, media, copy | Text-only sheet with hardcoded English | Ported: native message history screen, EN/RU labels, preserved media open and copy; existing edit-history switch gates it |
| Save media categories | Private chats; public/private channels/groups | All categories under attachment switch | Ported: Message Saving Preferences and chat-type selection sheet; optional overrides preserve old all-category behavior |
| Save formatting / reactions | Boolean controls (default on) | Serialized message metadata always saved | Ported: optional policy for future stored snapshots; does not mutate live Telegram objects or old saved rows |
| Save for bots | Shared delete/edit exclusion | All bots retained if Spy enabled | Ported: optional shared bot exclusion for edit/delete/attachment capture; unknown peers remain eligible |
| Attachment copies | External Downloads/Saved Attachments; proprietary mapper missing | Private files with limit, restore and encrypted copies | Ported: extend private saved attachments to prior edits; restore their paths in history; database export still excludes attachment files |
| Deleted / edited marks | Arbitrary text, reset | Deleted emoji and tint; no edited mark override | Ported: opt-in free-text deleted/edited marks, Reset, source broom default; old emoji/color settings preserved |
| Ghost menu toggle / kill app | Drawer entries | Ghost action bar button; upstream no longer has a drawer | Ported: opt-in ghost/kill actions in the chat-list overflow menu; current upstream has no drawer |
| Ghost launch shortcut | Enter with Ghost | Missing | Ported: opt-in Enter with Ghost dynamic shortcut, preserved across Telegram shortcut rebuilds |
| Ad blocking / local Premium / background service | Useful switches | Already present | Ported UI: Useful features section; local Premium now matches AyuGram client gates, refreshes all active accounts and preserves real server user flags |
| Regex filters | Per-expression editor, global case option, chat scope, album handling | Existing filters, shadowban, scopes, validation, import/export, reload | Retain richer implementation; align section placement |
| Deleted replies / forwarding | Quote fallback and client-side media forwarding | Protected-content forwarding only | Ported: opt-in deleted-message copy fallback using existing uploader and safe quoted replies with cloned entities; no exteraGram utility |
| TTL/media save/copy/screenshots | Hooks in viewers, loaders, message menu | Existing retention/protected-content hooks | Retain working Ayuless implementation |
| Delete date | String exists; no implemented menu action in supplied source | Not exposed | Ported: optional context action using stored deletion timestamp; source only exposes the string via an exteraGram details component, which is not copied |
| Keep locally when deleting | Extra delete-dialog checkbox, history merge | Own deletes intentionally excluded | Ported equivalent: opt-in single-message cloud context action with confirmation; no global delete bypass or copied delete-dialog UI. Multi-select checkbox not ported: reference history merge depends on the absent proprietary module |
| AyuSync | Account registration, authenticated WebSocket, local read batches/force sync; MVP account required | Missing | Ported: opt-in registration, authenticated WebSocket, local read batches, force sync, status/device/profile controls using existing nv-websocket dependency and platform HTTP; explicit server/token required |
| Sync edit history | Commented out in source | Missing | Skipped: not implemented in reference; proprietary entity mapper absent |
| WAL / erase local database | Debug controls; WAL key case mismatch in reference | Upstream journaling; normal cache tools | Adapted: opt-in maintenance section shows existing WAL status and links to saved-data clearing / upstream cache manager. Journal-OFF toggle and destructive raw SQL erase skipped: both databases already use WAL, and reference table schema differs |
| Peer ID / timestamp seconds / sticker, GIF, voice confirmations / recent sticker count | Unused strings in ayu.xml or exteraGram-specific surfaces | Upstream behavior | Skipped: unused strings have no Radolyn implementation to port; exteraGram features/framework are expressly excluded |
| Pill stack / status pills | exteraGram feature | Absent | Skipped: explicitly excluded |
| Easter eggs / device-brand messages | Xiaomi/Ayu custom URI handlers | Absent | Skipped: client branding jokes, not settings or message functionality |
| Proprietary message mapper and bulk history merge | Uninitialized `com.radolyn.ayugram.proprietary` submodule | Native Telegram serialization and AyulessStore | Skipped proprietary code: not present in clone. Existing native-row retention remains; rebuilding deleted history after Telegram cache erasure/import (including proprietary pagination/album merging) is not ported |

## Verification

Validation completed:

- `Tools/test_ayugram_settings.py`: 119 assertions against real settings classes
  with Android/Telegram boundary stubs (defaults, upgrades, locks, account
  isolation, mutual exclusion, retention categories/bots, endpoint validation).
- Temporary interception/filter harness: 72 privacy assertions, including no read
  before send acknowledgement, one-request explicit bypass, unrelated receipt
  blocking and encrypted-history receipts. These use dependency stubs.
- JDK parser: all Ayuless and changed Telegram Java files parse successfully
  (syntax only, not Android type checking).
- Android `aapt2 compile`: both EN/RU `ayuless.xml` files compile; resource names
  are unique and all new strings have both translations.
- DeGoogle source audit and `git diff --check` pass.
- Requested offline Gradle command stops before compilation: wrapper lock under
  `~/.gradle` is read-only. Direct cached Gradle with writable `/tmp` user home
  also stops before project configuration: no usable wildcard IP in sandbox.

Android compilation, device/UI and Telegram integration remain unverified.
AyuSync was not connected to a server: no configured endpoint/token or MVP
account. Topic-scoped and encrypted read states are not promoted into cloud
whole-dialog sync watermarks. The reference does not implement encrypted read
sync either; its discussion-read mapping loses topic identity, so it is not
reproduced.

Manual integration checks:

1. Upgrade with pre-existing ghost profiles, locks, Spy exceptions and saved data;
   confirm values and old emoji rendering survive and new opt-ins stay off.
2. Activate a fresh ghost profile; check reads, typing, online/offline, delayed
   text/media and read-after-success. Explicit Read until must authorize one
   request while unrelated receipts remain blocked.
3. Delete/edit messages in each chat type, bots and secret chats; verify selected
   future snapshot policy, old media history, copy/open and attachment quotas.
4. Check custom mark Reset, theme/RTL layouts, menu toggles and shortcut refresh.
5. Test Keep locally success/error handling in cloud private/group/channel chats;
   check normal own deletes still work. Restore/copy deleted text/media and reply
   to deleted messages with formatted captions.
6. Use a compatible test sync server for auth failures, reconnect, account
   isolation, force batches and disabling while a connection is pending; verify
   applying a remote read never sends a Telegram receipt.

No exteraGram code/UI/preferences or pill stack was imported. AyuGram-only
artwork was not needed: existing Ayuless/Solar/upstream drawables are reused.
