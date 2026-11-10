# Credits

## Icons

Ayuless uses icons from the **Solar Icons Set**
(https://www.figma.com/community/file/1166831539721848736/solar-icons-set)
by **480 Design** and **R4IN80W**, licensed under
[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/).
The icons were converted to Android vector drawables; colors may be adjusted.

Files: `TMessagesProj/src/main/res/drawable/ayuless_solar_*.xml`.

`ayuless_solar_eye.xml` is an eye adaptation drawn in the Solar Linear style
for the retained TTL media badge, with the same CC BY 4.0 attribution above.
`ayuless_solar_clock.xml` is a clock adaptation in the same Solar Linear style
for timed media, credited to 480 Design / R4IN80W under CC BY 4.0.

CC BY 4.0 is one-way compatible with GPLv3: the icons can be distributed as part of
Ayuless (GPL-3.0-or-later) as long as this attribution is kept.

## Inspiration

Thanks to the [AyuGram](https://github.com/AyuGram) team (Radolyn Labs). Ayuless follows AyuGram's feature set. Ayuless is not affiliated with AyuGram or Radolyn Labs. Name idea from exteraless.

The original settings restyle was independently implemented from the visual
measurements in [SETTINGS-STYLE.md](SETTINGS-STYLE.md). The parity port below adapts
Radolyn behavior and EN/RU wording using Telegram UI classes. The settings
category structure is restored to PR #51.
No exteraGram code, UI components or preferences framework is included; all
Ayuless branding remains its own.

## Code derived from AyuGram

Parts of Ayuless are based on code from [AyuGram4A](https://github.com/AyuGram/AyuGram4A), Copyright @Radolyn (Radolyn Labs). Files containing such code keep the original copyright header.

License note: the AyuGram4A repository ships the GNU GPL version 2 text and its file headers say "We do not and cannot prevent the use of our code, but be respectful and credit the original author", without "or later". Ayuless as a whole is GPL-3.0-or-later; permission to use this code under GPLv3 is being confirmed with the authors.

Reference revision: `7013145676d36d82ee13c02a89f72097b7490dcd`. The original GPLv2 text is preserved in
[LICENSE-AYUGRAM.txt](LICENSE-AYUGRAM.txt). See [AYUGRAM-PARITY.md](AYUGRAM-PARITY.md)
for exact behavior changes, retained implementations and exclusions.

The forwarding restriction hint follows the context-menu footer placement in
AyuGram4A `ChatActivity.java` (the `showNoForwards` block). Ayuless reuses its
existing upstream footer with independent original-restriction detection in
`ProtectedContent.java`; no additional AyuGram code was copied.

Source mapping: settings/text dialogs and marks derive from `AyuConfig`,
`AyuGramPreferencesActivity`, `MessageSavingPreferencesActivity`, `AyuUi` and
`AyuConstants`; capture policies and history derive from `AyuMessagesController`,
`AyuMessageHistory` and `AyuMessageCell`; ghost/request/context/launcher hooks
adapt `AyuGhostUtils`, `AyuState`, `AyuForwarder`, `AyuUtils` and the AyuGram-marked
Telegram hooks. The ghost profile picker adapts the avatar/name account dialog
from `org/telegram/ui/Components/AlertsCreator.createAccountSelectDialog` in the
reference checkout, reusing Ayuless's upstream `AccountSelectCell` without any
exteraGram customizations. Its shared row and selected-profile check are local
additions; it never changes the active Telegram account. There is no separate
Radolyn ghost account selector in that checkout. Resource wording derives from `values/ayu.xml` and
`values-ru/ayu.xml`. The test harness and parity inventory are original Ayuless
verification/documentation, not copied reference code.

Derived files (each keeps the original Radolyn header):

- `TMessagesProj/src/main/java/org/ayuless/appearance/AppearanceConfig.java`
- `TMessagesProj/src/main/java/org/ayuless/appearance/DeletedStyle.java`
- `TMessagesProj/src/main/java/org/ayuless/content/ProtectedContent.java`
- `TMessagesProj/src/main/java/org/ayuless/content/RetentionConfig.java`
- `TMessagesProj/src/main/java/org/ayuless/edits/DeletedMessageActions.java`
- `TMessagesProj/src/main/java/org/ayuless/edits/EditHistory.java`
- `TMessagesProj/src/main/java/org/ayuless/edits/EditHistoryActivity.java`
- `TMessagesProj/src/main/java/org/ayuless/edits/EditHistorySheet.java`
- `TMessagesProj/src/main/java/org/ayuless/extras/ExtrasConfig.java`
- `TMessagesProj/src/main/java/org/ayuless/extras/LocalPremium.java`
- `TMessagesProj/src/main/java/org/ayuless/ghost/DelayedSend.java`
- `TMessagesProj/src/main/java/org/ayuless/ghost/GhostConfig.java`
- `TMessagesProj/src/main/java/org/ayuless/ghost/GhostInterceptor.java`
- `TMessagesProj/src/main/java/org/ayuless/ghost/GhostMenu.java`
- `TMessagesProj/src/main/java/org/ayuless/ghost/GhostShortcut.java`
- `TMessagesProj/src/main/java/org/ayuless/settings/AppearanceSettingsActivity.java`
- `TMessagesProj/src/main/java/org/ayuless/settings/GhostAccountSelector.java`
- `TMessagesProj/src/main/java/org/ayuless/settings/GhostSettingsActivity.java`
- `TMessagesProj/src/main/java/org/ayuless/settings/SettingsTextDialog.java`
- `TMessagesProj/src/main/java/org/ayuless/settings/SpySettingsActivity.java`
- `TMessagesProj/src/main/java/org/ayuless/spy/MessageSavingConfig.java`
- `TMessagesProj/src/main/java/org/ayuless/spy/SavedAttachments.java`
- `TMessagesProj/src/main/java/org/ayuless/storage/AyulessStore.java`
- `TMessagesProj/src/main/java/org/telegram/messenger/ApplicationLoader.java`
- `TMessagesProj/src/main/java/org/telegram/messenger/MediaDataController.java`
- `TMessagesProj/src/main/java/org/telegram/messenger/MessagesController.java`
- `TMessagesProj/src/main/java/org/telegram/messenger/SendMessagesHelper.java`
- `TMessagesProj/src/main/java/org/telegram/messenger/UserConfig.java`
- `TMessagesProj/src/main/java/org/telegram/tgnet/ConnectionsManager.java`
- `TMessagesProj/src/main/java/org/telegram/ui/Cells/ChatMessageCell.java`
- `TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java`
- `TMessagesProj/src/main/java/org/telegram/ui/DialogsActivity.java`
- `TMessagesProj/src/main/java/org/telegram/ui/LaunchActivity.java`
- `TMessagesProj/src/main/res/values/ayuless.xml` (ported EN/RU wording)
- `TMessagesProj/src/main/res/values-ru/ayuless.xml` (ported EN/RU wording)

## Upstream

Ayuless is built on the official Telegram for Android sources
([DrKLO/Telegram](https://github.com/DrKLO/Telegram)), GPL v2 or later.
See [LICENSE-AUDIT.md](LICENSE-AUDIT.md) for third-party code inside the upstream tree.
