# AyuGram parity — historical phase-A audit input

The implementation described below has been removed by clean-room phase A. This
is a historical inventory, not the phase-B implementation contract or evidence
that the current tree retains the port. Phase B must use
[CLEANROOM-SPEC.md](CLEANROOM-SPEC.md) and must not consult the reference checkout
or historical implementation patches. See [CLEANROOM-REMOVED.md](CLEANROOM-REMOVED.md).

Reference: read-only `../ayugram4a` at `7013145676d36d82ee13c02a89f72097b7490dcd`, `TMessagesProj/src/main/java/com/radolyn/ayugram/`,
EN/RU `res/values*/ayu.xml`, and the AyuGram hooks in Telegram sources.
The inventory covers every Radolyn Java file (configuration, preferences, filters,
message/database entities and DAOs, history cells, forwarding/waiters, ghost helpers,
utilities), not the surrounding exteraGram client.

Optional additions default OFF. Anti-delete, saved attachments, sponsored-message
hiding and background work default ON when their preference is absent; stored
true/false choices take precedence. Existing Ayuless preference names, account profiles,
Spy exceptions, saved databases and retention switches remain compatible.

| Feature | AyuGram | Ayuless before | Ported now / skipped + reason |
| --- | --- | --- | --- |
| Settings structure | Inline feature groups | Category links to separate screens | Restored PR #51 categories and cards. New settings live in Ghost, Spy and Customization, with no additional settings screens or developer controls. The account picker uses avatar/name rows from the reference account dialog, with a shared profile and per-account selection. |
| Ghost master | Expandable switch, four core options | Profiles, locks, five options; initially empty master | Ported: first explicit activation seeds four core options; existing profiles and locks keep their values |
| Read/online/typing suppression | Request hooks | Already implemented, including secret chats and stories | Retain existing interception; align labels and controls |
| Immediate offline | Offline after online/send | Offline 1.5 s after sends | Ported: online-status requests also trigger the existing offline packet; 1.5-second Ayuless delay retained |
| Read after reply / schedule messages | Mutually exclusive switches | Independent read-on-action and delayed send | Ported: future changes are mutually exclusive; old keys retained; read-after-reply waits for successful acknowledgement; silent sends/encrypted delay retained |
| Read until selected message | Explicit one-request bypass | Missing | Ported: opt-in message context action with a request-identity bypass, including encrypted history |
| Read all confirmation | Explicit ghost read bypass | Missing | Skipped separate confirmation: these strings are unreferenced in supplied Java source; explicit Read until is implemented |
| Anti-delete | Saved messages merged into history | Existing persistent anti-delete and local-delete exclusion | Retain working store and hooks; configurable bot handling added below |
| Edit history | Full screen with message bubbles, media, copy | Text-only sheet with hardcoded English | Ported: native message history screen, EN/RU labels, preserved media open and copy; existing edit-history switch gates it |
| Save media categories | Private chats; public/private channels/groups | All categories under attachment switch | Ported: inline Spy saving options and chat-type selection sheet; optional overrides preserve old all-category behavior |
| Save formatting / reactions | Boolean controls (default on) | Serialized message metadata always saved | Ported: optional policy for future stored snapshots; does not mutate live Telegram objects or old saved rows |
| Save for bots | Shared delete/edit exclusion | All bots retained if Spy enabled | Ported: optional shared bot exclusion for edit/delete/attachment capture; unknown peers remain eligible |
| Attachment copies | External Downloads/Saved Attachments; proprietary mapper missing | Private files with limit, restore and encrypted copies | Ported: extend private saved attachments to prior edits; restore their paths in history; database export still excludes attachment files |
| Deleted / edited marks | Arbitrary text, reset | Deleted emoji and tint; no edited mark override | Ported: opt-in free-text deleted/edited marks, Reset, source broom default; old emoji/color settings preserved |
| Ghost menu toggle | Drawer entry | Ghost action bar button; upstream no longer has a drawer | Ported: opt-in ghost action in the chat-list overflow menu; current upstream has no drawer |
| Ghost launch shortcut | Enter with Ghost | Missing | Ported: opt-in Enter with Ghost dynamic shortcut, preserved across Telegram shortcut rebuilds |
| Ad blocking / local Premium / background service | Useful switches | Already present | Useful features card in Customization; local Premium now matches AyuGram client gates, refreshes all active accounts and preserves real server user flags |
| Regex filters | Per-expression editor, global case option, chat scope, album handling | Existing filters, shadowban, scopes, validation, import/export, reload | Retain richer implementation; align section placement |
| Deleted replies / forwarding | Quote fallback and client-side media forwarding | Protected-content forwarding only | Ported: opt-in deleted-message copy fallback using existing uploader and safe quoted replies with cloned entities; no exteraGram utility |
| TTL/media save/copy/screenshots | Hooks in viewers, loaders, message menu | Existing retention/protected-content hooks | Retain working Ayuless implementation |
| Delete date | String exists; no implemented menu action in supplied source | Not exposed | Ported: optional context action using stored deletion timestamp; source only exposes the string via an exteraGram details component, which is not copied |
| Keep locally when deleting | Extra delete-dialog checkbox, history merge | Own deletes intentionally excluded | Ported equivalent: opt-in single-message cloud context action with confirmation; no global delete bypass or copied delete-dialog UI. Multi-select checkbox not ported: reference history merge depends on the absent proprietary module |
| Peer ID / timestamp seconds / sticker, GIF, voice confirmations / recent sticker count | Unused strings in ayu.xml or exteraGram-specific surfaces | Upstream behavior | Skipped: unused strings have no Radolyn implementation to port; exteraGram features/framework are expressly excluded |
| Pill stack / status pills | exteraGram feature | Absent | Skipped: explicitly excluded |
| Easter eggs / device-brand messages | Xiaomi/Ayu custom URI handlers | Absent | Skipped: client branding jokes, not settings or message functionality |
| Proprietary message mapper and bulk history merge | Uninitialized `com.radolyn.ayugram.proprietary` submodule | Native Telegram serialization and AyulessStore | Skipped proprietary code: not present in clone. Existing native-row retention remains; rebuilding deleted history after Telegram cache erasure/import (including proprietary pagination/album merging) is not ported |

## Verification

Settings feedback validation (this revision):

- Restored the category screen directly from `e965f338`; remaining settings
  changes add the retained feature controls inside its existing groups.
- `Tools/test_ayugram_settings.py`: 119 JVM assertions cover new defaults and
  explicit false/true values for anti-delete, attachments, sponsored messages and
  the keep-alive startup path (Android service calls use boundary stubs).
- `Tools/test_strings_build.py`, DeGoogle source audit, parsing 70 Java files,
  EN/RU resource/placeholder/reference checks, `aapt2 compile` and
  `git diff --check` pass.
- Full Android build remains blocked before project compilation by the sandbox
  restrictions described below. Device rendering and service lifecycle need a build.

Earlier feature-port validation:

- `Tools/test_ayugram_settings.py`: policy assertions against real settings classes
  with Android/Telegram boundary stubs (defaults, upgrades, locks, account
  isolation, mutual exclusion, retention categories/bots).
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

No exteraGram code/UI/preferences or pill stack was imported. AyuGram-only
artwork was not needed: existing Ayuless/Solar/upstream drawables are reused.
