# Clean-room functional contract — phase A

## CR-01 Scope and handoff boundary

This document describes observable requirements, persistent formats and integration locations for an independent implementation. The target is Ayuless at revision 4b25f82c before the phase-A removals, with its current category-based settings and Solar icons. It is not a request to reproduce another client's internal architecture. No implementation, translated implementation, source excerpt or algorithm listing is provided here. English and Russian text below expresses meaning in new words; resource identifiers are compatibility identifiers, not required literal captions.

Phase B must use this document and the remaining independent Ayuless/Telegram tree. It must not consult the reference checkout, the removed implementation, historical patches, or Git objects containing that implementation. The original source remains outside this working tree and Git history has not been rewritten in phase A. A source export without Git history is the appropriate handoff. The removal ledger describes provenance for phase-A review and is not an implementation source.

The pre-existing Ayuless anti-delete store, filters, retention permission switches, encrypted attachment support, read-date and last-seen observations, ghost profiles and locks, delayed delivery, settings cards, branding and Solar assets are independent infrastructure to preserve. Telegram code carrying its upstream GPL version-2-or-later notice remains. A file containing a removed feature can therefore remain largely intact. Requirements below distinguish current visible controls from settings whose controls had already disappeared before phase A.

## CR-02 Settings structure and presentation

The main Ayuless page keeps its existing identity panel, then the category links in this order: privacy/ghost, retained messages/Spy, message filters, appearance/customization, followed by project links. The ghost link displays the enabled-option count out of five while the profile master is enabled, otherwise an off indication. The filters link displays the combined filter and shadow-ban count when enabled. These navigation and filter implementations remain in the tree.

All preference edits persist immediately. Preserve existing explicit false values and existing profile selection rules. Defaults in this document apply only when a key is absent. Rebuilding the list after a change must update dependent enabled states and displayed values.

### CR-02.1 Ghost page: current row order

The page has a profile chip in the action bar. It starts on the effective profile of the account that opened the page. Selecting a profile changes only the settings being edited, never the signed-in account being used elsewhere.

| Order / group | English caption meaning / Russian caption meaning | Preference and default | Interaction and explanatory meaning in EN / RU |
| --- | --- | --- | --- |
| Action bar | Privacy profile / Профиль приватности | Ephemeral selection; shared profile or activated account slot | Open the profile chooser; display avatar and first name, or the shared-profile symbol. |
| 1, account profile only | Separate preferences / Отдельные настройки | override, false for a new account | If disabled, inherit the shared preferences / Если выключено, использовать общие настройки. A stored account master key implies an override for older installations unless an explicit override value exists. |
| 2 | Privacy master / Основной переключатель приватности | enabled, false | Expandable master row, initially expanded; value is raw selected-profile count divided by five. Switch changes master; row tap changes expansion. |
| 2.1 | Conceal message reads / Не сообщать о прочтении | hideRead, false | First nested round checkbox. |
| 2.2 | Conceal story visits / Не сообщать о просмотре историй | hideStoryViews, false | Second nested checkbox. |
| 2.3 | Conceal presence / Не показывать присутствие в сети | hideOnline, false | Third nested checkbox. |
| 2.4 | Conceal activity indicators / Не показывать набор и другие действия | hideTyping, false | Fourth nested checkbox. |
| 2.5 | Return to offline after sending / Возвращаться в офлайн после отправки | offlineAfterSend, false | Fifth nested checkbox. Explanation: peers do not receive enabled activity signals, but the device can still update its own read state / Собственное состояние прочтения на устройстве обновляется, даже когда соответствующие сигналы скрыты от других. |
| Footer after nested rows | Lock explanation / Пояснение закрепления | Each option also has a boolean key with suffix Locked, false | Holding an option fixes its current value independently of the master / Долгое нажатие закрепляет текущее значение независимо от общего переключателя. Add a localized locked annotation and long-press haptic feedback. |
| 3 | Acknowledge reads after an action / Подтверждать чтение после действия | readOnAction, false | Enabled for editing only when the selected profile is in use and its raw hideRead is true. Explanation: a successful outgoing message or reaction may acknowledge the chat; includes secret chats / Успешная отправка сообщения или реакции разрешает подтверждение чтения, в том числе в секретном чате. |
| 4 | Mute outgoing notification sounds / Отправлять без звукового уведомления | sendSilently, false | Independent of master / Не зависит от общего режима. |
| 5 | Offer privacy choice for stories / Предлагать приватный просмотр историй | storyPrompt, true | Existing prompt is unnecessary while story visits are already concealed / Если просмотры уже скрыты, дополнительный вопрос не нужен. |
| 6 | Send after a brief wait / Отправлять с небольшой задержкой | delayedSend, false | Explanation: approximately 12 seconds for text and 20 for media; cloud messages use server scheduling, secret messages wait on this device while the app runs; applies to forwards, story replies and self-chat, excludes quick-reply templates; does not conceal presence by itself and can be unsuitable for slow uploads / Около 12 секунд для текста и 20 для медиа; облачные сообщения планируются на сервере, секретные ожидают на устройстве при работающем приложении; пересылки, ответы на истории и избранное поддерживаются, шаблоны быстрых ответов исключены; статус в сети отдельно не скрывается. |
| 7 | Privacy switch in chat-list menu / Переключатель приватности в меню чатов | ghostInMenu, false, global extras | Add one entry to the existing overflow menu. |
| 8 | Launcher entry with privacy enabled / Ярлык запуска с включённой приватностью | ghostShortcut, false, global extras | Immediately refresh the dynamic launcher shortcut. |
| 9 | Explicit read boundary in message menu / Явное подтверждение чтения до сообщения | readUntil, false, global extras | Explanation: this deliberate command sends a read acknowledgement despite concealment / Явная команда отправляет подтверждение даже при скрытии прочтения. |

For an account profile with override disabled, the master and profile-specific options are disabled. Global rows 7–9 remain editable. The shared profile always remains editable. Expansion and selected profile are not persisted by this screen. The five-item count uses stored option values, not the effective values after master/lock gating.

### CR-02.2 Spy page: current row order

The current screen does not expose the detailed capture-policy switches in CR-04. Do not add the removed separate saving-settings page. Its stored preferences must nevertheless continue to affect future captures.

| Order / group | English meaning / Russian meaning | Key, scope, default | Required result / explanatory meaning |
| --- | --- | --- | --- |
| 1, retained-message group | Retain removed messages / Сохранять удалённые сообщения | enabled in ayuless_antidelete_N, true | Existing anti-delete switch. Other people's deletions remain represented locally; ordinary self-deletions still follow existing exclusion rules. |
| 2, same group | Chats excluded from capture / Чаты без сохранения | Existing SpyExceptions selection; initially empty | Existing peer chooser, count or no-selection value; icon ayuless_solar_msg_block. Exclude these peers from deleted-message and revision capture. |
| 3, same group | Keep earlier revisions / Сохранять предыдущие редакции | enabled in ayuless_edits_N, false | Existing history switch; shared footer explains exclusions. |
| 4, attachment group | Keep downloaded attachments / Сохранять загруженные вложения | enabled_N in ayuless_attachments, true | Existing attachment switch, extended to earlier edited versions as described in CR-05. |
| 5, same group | Private attachment location / Расположение сохранённых вложений | No preference | Show the current account's absolute private directory in an informational bulletin; ayuless_solar_msg_download. This is not a directory picker. |
| 6, same group | Shared storage budget / Общий лимит хранения | limit_bytes in ayuless_attachments; 1 GiB | Inline label, then slider: 300 MiB, 1 GiB, 2 GiB, 5 GiB, 16 GiB, unlimited. Zero means unlimited. Footer shows total database plus attachment usage across account slots and the selected limit. Use a pending indication until sizes arrive. |
| 7, observation group | Remember observed reading time / Запоминать замеченное время прочтения | saveReadDate in ayuless_spy_extras_N, false | Existing feature; explain device-observed incoming acknowledgements for personal and secret conversations, including cases without server-displayed time. |
| 8, same group | Remember observed presence / Запоминать замеченное присутствие | saveLastSeen in ayuless_spy_extras_N, false | Existing feature; estimates from visible activity may be approximate, including secret-chat typing. |
| 9, data group | Write database to a file / Выгрузить базу в файл | No preference | Existing account database export, icon ayuless_solar_msg_filled_shareout. |
| 10, same group | Add records from a file / Добавить записи из файла | No preference | Existing account database import, icon ayuless_solar_msg_download. |
| 11, same group | Erase this account's saved records / Очистить сохранённые записи аккаунта | No preference | Existing destructive confirmation and clear operation, icon ayuless_solar_msg_delete, red text/icon. |

Import/export stay per account. Attachment bytes are not included in a database export. Existing import merges records without replacing the user's preferences and omits secret-message records except supported read observations. The shared-budget and slider behavior is original Ayuless code retained during phase A. It must survive the independent feature work.

### CR-02.3 Customization page: current row order

| Order / group | English meaning / Russian meaning | Key and default | Result |
| --- | --- | --- | --- |
| Top | Preview of a retained deletion / Пример сохранённого удалённого сообщения | No preference | Existing live preview, 150 dp high; refresh on appearance changes. |
| 1, deleted-message group | Dim retained deletions / Приглушать удалённые сообщения | translucentDeleted, true | Existing bubble/content fading. |
| 2, same group | Deletion symbol / Символ удаления | deletedMarkEmoji, wastebasket emoji | Existing chooser, ayuless_solar_msg_delete icon, current symbol or empty indication. Selecting it also exits the stored custom-text mode. |
| 3, same group | Symbol tint / Цвет символа | deletedMarkColor, 0 | Existing palette circles, zero follows message-time color. Footer says changes appear as rows are rendered again / Изменения видны при повторном отображении сообщений. |
| 4, useful-feature group | Show privacy status in chat list / Показывать приватность в списке чатов | ghostInDialogs, true | Existing action-bar ghost button; footer describes tapping it to toggle. |
| 5, same group | Suppress advertising content / Не загружать рекламные сообщения | hideSponsored, true | Existing sponsored-request blocker. |
| 6, same group | Device-local Premium features / Локальные возможности Premium | localPremium, false | Preserve real server premium flags; refresh all active account gates when toggled, CR-08. |
| 7, same group | Explain source forwarding limits / Показывать ограничение пересылки у источника | restrictedForwardHint, true | New context-menu footer described in CR-06.4. |
| 8, same group | Maintain background connection / Поддерживать фоновое соединение | workInBackground, true | Existing keep-alive service apply action. |

Appearance keys use ayuless_appearance. Useful-feature keys other than ghostInDialogs use ayuless_extras. The final footer explains advertising suppression, on-device feature access and a quiet background-service notification. It must not promise a server subscription or larger server limits. The old explanatory text described translation alone, but the actual removed behavior also affected general client Premium gates; CR-08 defines the behavior to preserve.

### CR-02.4 Geometry and visual vocabulary

Preserve the independently implemented settings layout and upstream switch drawing. Values below describe current surfaces and do not require copying any removed view classes.

| Surface | Measurements and assets | Theme behavior |
| --- | --- | --- |
| Settings cards | Existing section drawing uses 12 dp and 20 dp section parameters. Section heading insets use HeaderCell parameters 24, 6, 3. Standard button inset 21 dp; value edge correction 6 dp. | Headers windowBackgroundWhiteBlueHeader; regular text windowBackgroundWhiteBlackText; icons windowBackgroundWhiteGrayIcon; divider key_divider. Accent windowBackgroundWhiteBlueText4; destructive text text_RedRegular and icon text_RedBold. |
| Preference dividers | 1 physical pixel; start inset 68 dp with icon, 20 dp without; mirror in RTL. | key_divider. |
| Master suboptions | Existing round checkbox rows, one nesting pad level. | Upstream checkbox and switch theme resolution. |
| Profile action-bar chip | 120 dp width; horizontal padding 6/8 dp; avatar 28 by 28 dp with 14 dp radius; label 13 sp, one line, ellipsis, 7 dp gap. | actionBarDefaultTitle; shared avatar uses AVATAR_TYPE_FILTER_GROUPS. |
| Profile dialog | Vertical list; shared row first, then activated account slots in ascending slot order; each row 56 dp high, horizontal padding 14 dp. Shared avatar 36 by 36 dp, 18 dp radius, offsets 10 dp left/top. Shared name 15 sp bold, left offset 61 dp, right reserve 52 dp. | shared label chats_menuItemText; account rows reuse upstream AccountSelectCell; selector background from Theme. |
| Profile check | account_check drawable, centered in 40 dp-wide trailing region, trailing inset 6 dp; invisible for unselected rows. | chats_menuItemCheck with multiply tint. Checked identity is selected profile, independent of active Telegram account. |
| Free-text editor, dormant helper | One line, 16 sp; padding 24 dp horizontal and 12 dp vertical; cursor starts at end; focus and keyboard on show. Maximum supplied by caller; historic mark use was 32 UTF-16 units. | dialogTextBlack; native themed AlertDialog; save, cancel and reset controls. |
| Forward-restriction footer | Text 14 dp, maximum width popup width minus 38 dp; 11 dp padding on each side; top offset -8 dp; popup_fixed_alert4 background. When reactions panel is present, side margins 16/36 dp as in the existing upstream footer. | actionBarDefaultSubmenuItem text, actionBarDefaultSubmenuBackground background. |
| History screen | Standard UniversalFragment heading and native message bubbles; a label preceding each version, wallpaper background on every bubble cell. | chat_wallpaper and normal incoming/outgoing message theme keys. No separate screenshot-derived bubble renderer. |
| Menu and launcher privacy affordances | ayuless_solar_ghost. | Existing menu/launcher tint and layout rules. |

The existing emoji chooser and color palette are retained original code. Empty symbol hides the deletion prefix. Its preset symbols and custom-single-emoji validation remain as supplied in the tree. The existing Solar wastebasket rendering and eye/clock TTL badges are also independent changes to retain.

## CR-03 Ghost behavior

### CR-03.1 Profiles and initial activation

Shared preferences are stored in ayuless_ghost_all; account-specific files are ayuless_ghost_N, where N is the zero-based Telegram account slot. The shared profile identifier is -1. An account with override enabled uses its own settings; otherwise it uses shared settings. For an older account with an enabled key but no override key, retain the account profile. Do not change or erase other profile values when choosing a profile in the settings dialog.

On enabling a profile master, seed message-read concealment, presence concealment, activity concealment and return-to-offline to true only if none of the five core option keys has ever been stored in that profile. Do not seed story concealment. A stored false on even one core option makes this an already configured profile. Locks are separate keys and do not count as core option values for this check. Later activations preserve choices.

Core options apply when both their value and the profile master are true. A locked option uses its own value regardless of master state. A lock on false therefore also matters. The story prompt can conceal story views for its existing session even without enabling the master. Silent sending, delayed sending and the story-prompt preference are independent of the master.

When readOnAction is turned on, write delayedSend as false in the same profile. When delayedSend is turned on, write readOnAction as false. Merely reading older preferences must not migrate or normalize a profile that already has both true. Disabling either option leaves the other alone. Effective read-on-action additionally requires effective hideRead.

### CR-03.2 Acknowledgement after successful sends

Retain the existing request suppression for read receipts, content reads, secret-chat receipts, typing, story views and status updates. The removed change moves automatic read-after-action triggering to a successful response, not request submission. An error or absent response must not trigger it. Supported cloud actions are plain messages, a single media send, albums, forwarding and reactions. Supported secret actions are encrypted text and encrypted file sends. Encrypted service traffic must not trigger an acknowledgement.

For cloud actions, acknowledge to the dialog's latest known positive top-message id. Choose a channel acknowledgement for channels and an ordinary history acknowledgement otherwise. For encrypted actions, use the latest known dialog message date and encrypted peer identity. Missing peers/dialogs/boundaries produce no read request. Recheck effective read-on-action after dispatching to the UI thread. Allow the resulting particular request through suppression once. If concealment is still enabled but the automatic action setting was disabled while queued, it must again be suppressed. Do not temporarily disable privacy globally or for unrelated requests.

### CR-03.3 Offline status and delayed delivery

The existing post-send offline timer is 1500 ms. Also start that timer when an online status request is issued while return-to-offline is effective. A status update already declaring offline must not schedule another timer. If presence concealment is effective, the status request is changed to offline and still completes normally; that branch needs no extra timer. Preserve existing send-triggered timers.

Delayed delivery retains its original 12-second text / 20-second media scheduling and account/dialog grouping window of 3 seconds. The removed parity adjustment requires a scheduled request leaving the network layer to be at least 11 seconds ahead of current server time, rather than the pre-port 5-second floor. Preserve explicit scheduled dates, existing supported/excluded request categories, silent sends and the independent secret-chat holding mechanism. The adjustment applies to cloud messages, single-media, albums and forwards handled by the existing scheduling code. Do not convert failed ordinary message retries into scheduled messages.

### CR-03.4 Explicit read-to-message action

The optional message-menu command acknowledges history through the selected incoming message. It is shown outside scheduling mode, for a nonzero message id, when the message is neither outgoing nor locally marked deleted, hideRead is effective and readUntil is enabled. The command is explicit authorization for only that request, even if privacy preferences change while queued. Unrelated acknowledgements stay suppressed.

For a channel, the boundary is selected message id with channel identity. For an ordinary cloud peer, use selected message id and peer identity. For a secret conversation, use selected message date and the encrypted chat id/access hash; missing encrypted chat information prevents sending. Apply affected-message pts/pts_count to the controller when returned. Deliver completion to the UI thread and, if the fragment is still alive, show localized success or generic failure feedback. The action does not impose an extra all-messages confirmation dialog or globally mark subsequent reads as permitted.

### CR-03.5 Menu and launcher

With ghostInMenu enabled, append a ghost-icon row to the existing chat-list overflow. Its meaning changes between turning privacy on and turning it off according to the effective profile master. Use the existing GhostBadge toggle behavior, including its account/profile handling. There is no navigation drawer to restore and no process-kill menu item.

The optional dynamic shortcut identifier is ayuless_ghost. Its explicit intent action is org.ayuless.ENTER_WITH_GHOST and destination is LaunchActivity. Enabling the setting adds/updates the shortcut with the ghost icon and a localized label meaning launch with privacy. Disabling removes the dynamic entry and disables previously pinned instances with a localized off indication. Shortcut-manager failures must be logged without crashing.

On this action, only while ghostShortcut is still enabled, enable the effective profile of UserConfig.selectedAccount. Initial-option seeding follows CR-03.1. Consume the special action by changing it to the ordinary main action so repeated handling is harmless. Ignore null, unrelated or disabled shortcut actions. Do not change selectedAccount.

Telegram shortcut rebuilds must preserve the privacy shortcut: reserve an extra slot when enabled, restore it after removing all dynamic shortcuts, and include its identifier among entries that should survive incremental pruning. Preserve Telegram's existing shortcut-count fallback and API guards.

## CR-04 Capture-policy compatibility

These settings are dormant controls in the current UI: values can exist from older releases, but the current Spy page has no rows for changing them. Independent implementation must honor existing values without reintroducing removed settings screens. No database migration is required for them.

| File / key | Type and absent default | English meaning / Russian meaning |
| --- | --- | --- |
| ayuless_saving_N / customSaving | boolean, false | Apply additional capture choices / Учитывать дополнительные правила сохранения |
| same / saveFormatting | boolean, true | Preserve formatting entities / Сохранять разметку текста |
| same / saveReactions | boolean, true | Preserve reaction metadata / Сохранять сведения о реакциях |
| same / saveForBots | boolean, true | Allow capture in bot conversations / Разрешать сохранение переписки с ботами |
| same / mediaCategories | boolean, false | Restrict attachment capture by peer category / Ограничивать сохранение вложений по виду чата |
| same / privateChats | boolean, true | Personal and encrypted conversations / Личные и секретные диалоги |
| same / publicChannels | boolean, false | Broadcast channels with public addressing / Публичные вещательные каналы |
| same / privateChannels | boolean, true | Broadcast channels without public addressing / Закрытые вещательные каналы |
| same / publicGroups | boolean, false | Public groups / Публичные группы |
| same / privateGroups | boolean, true | Non-public groups / Непубличные группы |

When customSaving is false, keep formatting, reactions, bot conversations and every media category regardless of the subsidiary stored values. When customSaving is true, apply formatting/reaction/bot choices; media category choices additionally require mediaCategories. Do not rewrite older snapshots or live Telegram messages when changing these preferences.

The bot exclusion applies to delete, revision and attachment eligibility via the common retention predicate. It excludes a known bot only for a positive ordinary user dialog. Unknown users remain eligible, as do chats and encrypted dialogs subject to their other existing permission rules. Continue to apply Spy exceptions and all existing retention permissions.

For attachment category checks, ordinary personal and secret dialogs use privateChats. A broadcast channel is a channel that is not a megagroup; other negative-id peers use group categories. Public/private status comes from Telegram chat metadata. Missing chat metadata must retain existing eligibility rather than silently discard an already opted-in attachment.

Deletion capture stores a separately serialized snapshot. In custom policy mode, omit entities and their presence flag when formatting is disabled, and omit reactions and their presence flag when reactions are disabled. Preserve the dialog identity and attachment path needed by the existing storage format. The live message must remain untouched. Snapshot creation failures are logged and retain the unfiltered capture as the current fallback. Revision capture separately omits stored entities when formatting is disabled. Reactions are not a column of revision history and are not reconstructed for old revisions.

## CR-05 Revision capture, history screen and attachments

### CR-05.1 Capture conditions

Keep the existing capture timing: inspect the cached previous message on MessagesStorage's storage queue before an incoming server edit overwrites it. Existing edit-history enablement, exceptions and retention eligibility still apply. Preserve capture for text changes and entity changes. Treat any change in the photo/document media identifier as a revision, including a change between no media (zero identifier) and actual media. Non-photo/document media uses the existing zero identifier behavior. A missing old cached message cannot produce a previous revision.

After retaining an old revision, also ask the existing attachment saver to retain its already downloaded media. Its own switch, category policy, exceptions, retention permissions and budget still apply. Do not download unavailable old media solely to populate history. Mark the existing history cache only as the existing successful capture path does.

### CR-05.2 Full-screen history

The existing history menu entry remains conditional on cached knowledge that older versions exist. It now opens a full-screen native Telegram message-history view for the selected account/dialog/message instead of the earlier text-only sheet. Its title means message revision history / история редакций сообщения; menu caption means earlier versions / предыдущие редакции.

Show a loading indication while the asynchronous store query is pending. Show the current version first, followed by stored revisions from newest to oldest. Each entry has a label and a normal ChatMessageCell bubble. Label meanings are latest state / актуальная редакция; initial state / исходная редакция for a revision with zero edit date; otherwise revision number / номер редакции using stored version plus one. Retain an empty-content indication where the text-only compatibility surface needs it.

Create local previews without changing the live original. Current preview preserves the original attachment path. Older versions use their stored text, entities and media. Their displayed timestamp is stored edit date, or original send date for the initial version. Do not display an old revision as edited, scheduled, a fresh incoming message, a reply or a reaction-bearing message. Historical revisions have no reconstructed reactions or reply header. Retain original sender/outgoing identity and remaining message metadata needed for native bubble rendering. A serialization failure omits that preview and logs the error. Stop processing UI updates after the fragment is finished.

Tap a text-only entry to copy its nonempty underlying text. Tap a photo/document entry to open its readable downloaded file using the existing platform viewer; materialize encrypted data on a background queue. Report unavailable media with the existing protected-media download-needed feedback. Long press copies nonempty text/caption even for media. Show the standard message-copy bulletin. Reading or copying these previews must not emit a read acknowledgement. The final instruction line explains tap-to-copy/open and hold-to-copy in both languages, using fresh wording.

### CR-05.3 Reconnecting saved attachments

Before constructing the history bubbles, resolve saved media on the attachment queue. Use Telegram's media filename. Skip empty filenames or filenames containing a slash. If a plain saved file exists, associate its absolute path with that preview. Otherwise, if both encrypted data and its key exist, restore them to the existing Telegram encrypted-cache data and key locations. Do not decrypt on the UI thread. Finish on the UI thread and ignore a finished fragment. Paths and naming are defined in CR-10.

The earlier text-only EditHistorySheet remains original Ayuless infrastructure; only its localized labels were part of the removed port. If the retained compatibility sheet is used elsewhere, its title and version labels should eventually use the independently worded meanings above. Do not introduce an additional navigation surface merely to expose that old helper.

## CR-06 Deleted-message actions and forwarding

| Global preference in ayuless_extras | Type / absent default | English setting meaning / Russian setting meaning | Current settings visibility |
| --- | --- | --- | --- |
| localResend | boolean / false | Copy retained deletions and avoid replies to missing originals / Отправлять сохранённые удаления копиями и заменять ссылки на отсутствующие оригиналы | No row; honor an existing value. |
| keepOwnDeletes | boolean / false | Offer remote deletion with local retention / Предлагать удаление у всех с сохранением на устройстве | No row; honor an existing value. |
| deleteDate | boolean / false | Offer the locally observed deletion timestamp / Предлагать время удаления, замеченное на устройстве | No row; honor an existing value. |

### CR-06.1 Quoted replies to unavailable originals

With localResend enabled, a new outgoing reply to a locally marked deleted message must avoid referring to that missing server message. Do not alter retries. For a nonempty source message text, prepend a short quotation to outgoing text, or to a photo/document caption when text is absent. Replace line breaks in the quotation excerpt with spaces; keep at most 20 Unicode code points, adding an ellipsis when truncated. For a non-personal destination, include the original sender's formatted user name or chat title on a preceding line if known. The quote line starts with the conventional greater-than quotation marker followed by a space; leave one blank line before the new content.

Keep the sender/name prefix out for personal destinations. Offset existing outgoing text/caption entities by the UTF-16 length of the inserted prefix, using independent entities so the original formatting objects are untouched. Log and omit an entity that cannot be copied. Always clear the server reply target and explicit reply quote for this deleted-original fallback, including when the deleted source has no text. Existing unsupported payload shapes keep their content but lose the invalid reply reference. With the setting off, an ordinary reply, an unknown/nondeleted target, a null target or a retry, preserve the existing path.

### CR-06.2 Sending retained deletions as copies

With localResend enabled, a locally retained deleted message becomes another reason for the existing local-copy forwarding path. Preserve the original permission check: the retention/unlock policy must still allow that message. Do not broaden that policy. Use existing text/media upload behavior and existing unavailable-download reporting. Restriction detection for the source must remain separate from the decision to send a local copy; timed, secret and deleted messages can need copying without a server forwarding prohibition.

### CR-06.3 Keep a local copy while deleting remotely

The optional keepOwnDeletes setting is false by default and has no current settings row. Its context action appears only outside schedule mode, on a positive-id cloud message which is not locally deleted, which Telegram allows this user to delete, and which the existing anti-delete retention predicate permits. Secret conversations are excluded. Do not expose a multi-selection checkbox or alter the ordinary self-delete exclusion.

Ask for confirmation whose meaning is remove the selected message for everyone while keeping a marked local version / удалить выбранное сообщение у всех, сохранив помеченную локальную копию. Use native cancel and delete buttons. On confirm, send the channel-specific deletion request when peer channel id is nonzero; otherwise use the ordinary delete request with revoke enabled. The local retention action happens only after a response without an error. On error, show standard error feedback if the fragment is alive and leave the local deletion state unchanged.

After successful remote deletion, retain the loaded original through existing anti-delete machinery. Feed an affected-messages response through the normal update-processing path with its pts and pts_count, as a channel or ordinary deletion update as appropriate, so sequence state advances consistently while the local retention path protects the retained message. Do not globally bypass outgoing-delete protection. Null activity/message, disabled setting, disallowed retention or encrypted peer must also be rejected by the action handler itself.

### CR-06.4 Source forwarding restriction footer

With restrictedForwardHint enabled, add an explanation below the context menu when the menu actually offers forward and the original message had a forwarding restriction. Meaning in EN: the source forbids direct forwarding / RU: исходное сообщение запрещает прямую пересылку. Forwarding as an independently uploaded copy remains available under the existing unlock policy.

Detect the original message-level noforwards flag even after the existing unlock path has cleared it; the existing weak identity tracking of unlocked originals provides that information. For personal peers, also consider either user-full noforwards flag. For groups/channels consider the chat noforwards flag. A secret conversation without a recorded message restriction is not automatically a restricted-forward source. Being deleted or timed alone does not qualify. Null message is unrestricted. Keep all ordinary upstream restricted-forward footer branches when this optional extra reason is absent.

### CR-06.5 Local deletion time

The optional deleteDate setting is false and has no current settings row. Show its context command for locally marked deleted messages when enabled. It uses msg_recent. Query this account's retained record by dialog id and message id and read deleted_at. Show a native dialog with a title meaning observed deletion time / замеченное время удаления. A nonzero value uses Telegram's localized audio-date formatter including time; zero, missing record or query failure displays a localized unavailable-time meaning. This is the local capture time, not an asserted server deletion timestamp. Deliver on the UI thread; skip a finished fragment or absent activity.

## CR-07 Text markers and compatibility

These runtime preferences survive from an earlier UI, but the current Customization screen offers only the independent emoji/color controls. Do not restore removed text-marker rows just because the preferences exist.

| File / key | Type / absent default | Meaning |
| --- | --- | --- |
| ayuless_appearance / customMarks | boolean / false | Use stored text prefixes instead of the deletion emoji, and override the normal edited label / Использовать текстовые пометки удаления и изменения. |
| same / deletedMarkText | string / broom emoji U+1F9F9 | Prefix before the deleted-message time / Пометка перед временем удалённого сообщения. |
| same / editedMarkText | string / current localized upstream EditedMessage | Label for an edited timestamp / Подпись изменённого сообщения. |

In custom mode, a nonempty deletion text is followed by one space and the original time label; an empty text leaves the time alone. Outside custom mode retain the original emoji rendering, tint and Solar wastebasket adaptation. Selecting any emoji style also sets customMarks to false, without discarding the stored text choices. Reset removes the relevant text key, so subsequent use resolves the current default, including current UI language for the edited label. Save preserves text as entered; there is no automatic trim in the old general text editor.

For edited labels, the custom text participates only in the ordinary edited-time branch of ChatMessageCell.timeString. Do not override Telegram's special private edited-date formatting or unrelated saved/forwarded timestamps. Preserve the existing timestamp and one-space separation, including with an empty custom edited label. Do not change the independent deleted-message opacity logic.

The dormant general text dialog supports caller-supplied maximum length, save, cancel without mutation and reset. It is not itself reachable from current settings. A future implementation can omit an unused helper while honoring the stored markers and ensuring no removed rows return.

## CR-08 Device-local Premium gates

The localPremium setting broadens client-side self-account Premium checks in addition to the existing translation feature. UserConfig.isPremium returns false when there is no current user; otherwise the real user Premium flag or the local preference can enable client gates. MessagesController.isPremiumUser recognizes the preference only for the current self user, still excluding support users and rejecting null. It must not turn unrelated users into Premium users or mutate any server-provided user object's Premium flag.

When server self-user data changes, update client Premium availability using real subscription state together with the local preference. Preserve the existing notifications and refresh work in UserConfig.checkPremiumSelf. Changing the local setting refreshes every activated account: update controller Premium state, notify that account's current-user Premium status changed, reload Premium promotion data and reactions without forcing a server subscription, invalidate the story-limit cache, and emit the global Premium-status notification. Skip inactive account slots. Actual purchases, server entitlements, upload/usage limits and request validation remain Telegram's responsibility; no server user flags are forged.

## CR-09 Hook locations in retained Telegram sources

Only behavior deltas are listed here. All surrounding upstream and independent Ayuless logic must remain. Numeric action values are listed for compatibility with the existing menu integration, not as a required internal organization.

| Class | Method / location | Required integration | Contract |
| --- | --- | --- | --- |
| org.telegram.tgnet.ConnectionsManager | sendRequest, response dispatch on stageQueue | After a nonnull successful response, notify ghost read-after-action logic before client completion callbacks. Existing outgoing suppression stays. | CR-03.2 |
| org.telegram.messenger.MediaDataController | buildShortcuts | Reserve/preserve/recreate the optional launcher entry through both shortcut rebuild paths. | CR-03.5 |
| org.telegram.ui.LaunchActivity | handleIntent | Consume the explicit privacy-launch action before ordinary intent routing. | CR-03.5 |
| org.telegram.ui.DialogsActivity | showItemOptions | Add the conditional privacy toggle to the existing overflow options. | CR-03.5 |
| org.telegram.ui.ChatActivity | fillMessageMenu | After ordinary options, add eligible explicit read, deletion-time and keep-local commands, then the conditional history entry. Icons: msg_markread, msg_recent, msg_delete, msg_recent respectively. Former action ids: 9003, 9004, 9005; original history id 9001. | CR-03.4, CR-05.2, CR-06 |
| org.telegram.ui.ChatActivity | processSelectedOption | Dispatch those commands; history opens the full-screen preview. Keep the independent retained-media save action 9002. | CR-03.4, CR-05.2, CR-06.3, CR-06.5 |
| org.telegram.ui.ChatActivity | createMenu overload with searchGroup/longpress/suggestEdit | Extend the existing restriction footer only when forward is present and the source was restricted. Keep upstream branches and geometry. | CR-06.4 |
| org.telegram.messenger.SendMessagesHelper | sendMessage(SendMessageParams) entry | Prepare the deleted-original reply fallback before fields are consumed by ordinary sending. Existing copy-forward integration stays. | CR-06.1–2 |
| org.telegram.messenger.UserConfig | isPremium; checkPremiumSelf | Device-local client self gates and server-self-update refresh behavior. | CR-08 |
| org.telegram.messenger.MessagesController | isPremiumUser | Apply the local preference to the self user's gate only. Existing edit update interception calls EditHistory.onServerEdit before storage. | CR-08, CR-05.1 |
| org.telegram.ui.Cells.ChatMessageCell | timeString | Substitute optional edited text only in its ordinary edited timestamp branch. Existing deleted-style and attachment restoration integration stays. | CR-07 |
| org.telegram.messenger.ApplicationLoader | No surviving derived executable hook at phase-A input | Its former sync startup had already been removed. Keep original startup and keep-alive behavior. | CR-01, CR-12 |

| Ayuless integration surface | Methods / behavior boundary | Contract |
| --- | --- | --- |
| GhostConfig | setProfile; existing profile getters and effective-value handling | First-enable seeding and mutual exclusion, retain original profile/lock infrastructure. CR-03.1 |
| GhostInterceptor | shouldBlock; successful-response handling; existing offline timer | Request-specific explicit allowance, post-success automatic reads and online-triggered offline timer. CR-03.2–4 |
| DelayedSend | fixOnSend | 11-second floor using existing scheduling integration. CR-03.3 |
| RetentionConfig | storable | Add the optional bot policy without bypassing existing permission checks. CR-04 |
| AyulessStore | saveDeleted; saveEdit; deletion timestamp lookup | Future snapshot policy and async timestamp access. CR-04, CR-06.5 |
| EditHistory | onServerEdit; contentChanged | Capture attachment changes including zero/nonzero transitions; save downloaded previous media. CR-05.1 |
| SavedAttachments | onKept; history preparation | Apply category eligibility to new copies; resolve older media off the UI thread. CR-04, CR-05.3 |
| ProtectedContent | existing needsCopy / sendCopies boundary; original-restriction query | Deleted-message copy eligibility and independent source restriction explanation. CR-06.2, CR-06.4 |
| AppearanceConfig / DeletedStyle | mark setters; decorateTimeLabel | Text-mark preferences, reset/default semantics and existing emoji choice exit. CR-07 |
| GhostSettingsActivity | fillSettingsItems; onClick; showProfilePicker | Current rows, footer meanings and profile-dialog selection. CR-02.1 |
| AppearanceSettingsActivity | fillSettingsItems; onClick | Restriction hint setting and refresh on local Premium toggle. CR-02.3, CR-08 |
| SpySettingsActivity | fillSettingsItems | Preserve present original groups and budget slider; remove no original storage functionality. CR-02.2 |

## CR-10 Data contracts

### CR-10.1 Preference namespaces

N means account slot, not server user id. Existing files must be reused without wiping them. Boolean absent defaults and all derived feature keys appear in CR-02–04 and CR-07. New runtime state for one-request allowances and UI profile/expansion is process-local, not persisted. Do not leave an authorization flag in preferences.

| Namespace | Scope and purpose |
| --- | --- |
| ayuless_ghost_all, ayuless_ghost_N | Shared/account privacy profiles including locks and override rules. |
| ayuless_extras | Global advertising, local Premium, background service and optional menu/launcher/message actions. |
| ayuless_appearance | Global retained-deletion appearance and legacy text-marker compatibility. |
| ayuless_saving_N | Optional future-capture policy. |
| ayuless_antidelete_N, ayuless_edits_N, ayuless_spy_extras_N | Existing per-account retention and observation switches. |
| ayuless_attachments | Per-account enabled_N booleans and shared limit_bytes. |

### CR-10.2 Existing SQLite format: preserve, do not replace

The original independent database remains schema version 2, WAL enabled, separate from Telegram cache4.db. Account zero uses files/ayuless_0.db; slot N greater than zero uses files/accountN/ayuless_N.db. Timestamps described here are Unix seconds. No new schema was added by the removed features.

| Table | Columns, types and keys | Meaning relevant to reimplementation |
| --- | --- | --- |
| deleted_messages | dialog_id INTEGER required; msg_id INTEGER required; topic_id INTEGER nullable; date INTEGER required; deleted_at INTEGER required; from_id INTEGER nullable; data BLOB required. Composite primary key dialog_id + msg_id. Indexes deleted_messages_dialog and deleted_messages_deleted_at. | Existing native TL-serialized message snapshot; local deletion/capture time in deleted_at; original date and optional sender/topic metadata separate. Replace conflict retains the existing single-row semantics. |
| edit_history | dialog_id INTEGER required; msg_id INTEGER required; edit_date INTEGER required; version INTEGER required; text TEXT; entities BLOB; media BLOB. Composite primary key dialog_id + msg_id + version. Indexes edit_history_dialog and edit_history_edit_date. | Store previous content, starting version zero then increasing within the message. entities is the existing count followed by serialized TL entities; media is serialized native MessageMedia when nonempty. |
| read_outbox | dialog_id INTEGER required; min_id INTEGER required; max_id INTEGER required; read_at INTEGER required. Composite primary key dialog_id + max_id. | Original observation feature; retain unchanged. Interval is above min_id through max_id inclusive. |
| last_seen | user_id INTEGER primary key; seen_at INTEGER required. | Original observation feature; retain unchanged. |

Do not introduce the reference client's database schema or a proprietary mapper. Use existing serializers and store APIs. The formatting/reaction policy affects only newly captured BLOBs/entities. A history preview combines the current original metadata with stored previous text/media/entities because the revision table does not contain an entire old message.

### CR-10.3 Files and quota

| Artifact | Location / representation |
| --- | --- |
| Plain saved attachment | files/ayuless/attachments/N/, filename formed by decimal dialog id, underscore, decimal message id, underscore, Telegram attachment filename. |
| Encrypted saved attachment | Same base path with .enc data suffix and .enc.key key suffix. Preserve both; this is the existing Ayuless encrypted-copy format. |
| History encrypted restoration | Telegram FileLoader message cache path plus .enc; corresponding key at FileLoader internal cache directory under cache basename plus .enc.key. |
| Database export | Existing document picker, application/octet-stream, suggested filename ayuless- followed by local yyyyMMdd-HHmm and .db. Attachments are excluded. |

Existing shared-budget accounting includes live database pages across accounts plus saved attachment bytes. Database work stays on its queue and attachment work on its own queue. Original cleanup runs periodic checks after 50 writes, removes oldest deleted/revision rows in batches of 200 under the existing guard, and uses the remaining budget for oldest attachment files. The original slider schedules a size refresh after 300 ms. These mechanics remain original code; the new policy/history integration must use them rather than substitute another storage system.

## CR-11 Rephrased resource meanings and verification cases

Resource identifiers can be recreated by phase B using independently written English and Russian captions. Preserve placeholders and category meaning. The following table supplements visible-row meanings with feedback and dormant surfaces. It deliberately does not reproduce removed resource values.

| Resource family / identifiers | English meaning / Russian meaning |
| --- | --- |
| AyulessEnableGhost, AyulessDisableGhost | Turn the privacy profile on or off / Включить или выключить профиль приватности. |
| AyulessReadSent, AyulessReadUntilInfo | A deliberate read acknowledgement was submitted; this command overrides concealment for that receipt / Явное подтверждение отправлено; команда разрешает только это подтверждение. |
| AyulessHistoryCurrent, AyulessHistoryOriginal, AyulessHistoryVersion | Latest content; initial content; numbered revision with one integer placeholder / Последняя редакция; начальный текст; редакция с одним целочисленным номером. |
| AyulessHistoryEmpty, AyulessHistoryHint | No textual content; tap for text copy or downloaded-file opening, hold for text copy / Текста нет; касание копирует текст или открывает загруженный файл, удержание копирует текст. |
| AyulessKeepLocallyConfirm, AyulessDeletedActionsInfo | Remote deletion plus local retention needs confirmation and anti-delete eligibility; media resending needs an available file / Удаление у всех с локальным сохранением требует подтверждения и разрешённого сохранения; для медиа нужен доступный файл. |
| AyulessUnknown, AyulessDeleteDate | Local removal time may be unavailable / Время локально замеченного удаления может быть неизвестно. |
| AyulessCustomSavingInfo | Extra capture choices affect later snapshots; disabling them restores ordinary capture / Дополнительные правила влияют на будущие копии; отключение возвращает обычное сохранение. |
| AyulessCustomMarks, AyulessDeletedText, AyulessEditedText | Legacy arbitrary text marker mode and separate removal/edit labels / Прежний режим произвольных текстовых пометок удаления и изменения. |
| AyulessOriginalForwardRestricted, AyulessRestrictedForwardHint | Explain the source's direct-forward prohibition while a copy action can remain available / Объяснить запрет прямой пересылки у источника при доступной отправке копией. |
| AyulessAttachmentsUsage | Two ordered string placeholders: formatted used size, then formatted limit / Два строковых значения: использованный объём и затем предел. This original current resource remains. |

Independent verification should cover fresh and upgraded profiles, stored false values, locks, shared versus account isolation, account-dialog selection without account switching, and first activation with any core key already stored. Verify successful versus failed sends, encrypted versus cloud receipts, a queued automatic read after its preference is disabled, and unrelated reads during an explicit one-message authorization. Check online/offline timer behavior and the 11-second scheduling floor after slow uploads.

For storage, exercise both customSaving states, all five categories, unknown peers, bots, exclusions and allowed secret/timed content. Compare live entities/reactions before and after snapshotting, and confirm existing rows are unchanged. Check media additions/removals as revisions, history order/labels, text/caption copy, available/missing/encrypted attachment opening and closing the fragment during asynchronous work. For actions, exercise missing deleted-reply text, supplementary Unicode characters at the excerpt boundary, entity offsets, retries, successful/failed remote deletion and channel sequence updates. Verify the source-restriction hint for a genuine restriction and its absence for merely timed/deleted media. Check dynamic shortcut recreation, disabling pinned entries, local Premium refresh across all active accounts, theme/RTL and current settings row order.

No compilation or executable verification is part of phase A. The remaining tree deliberately has feature gaps marked for phase B; pre-existing policy tests reference removed features and are not expected to pass until an independent implementation exists.

## CR-12 Already absent features and retained originals

Sync controllers, sync models/settings, maintenance/database-reset screens, a process-kill menu action, and the standalone chat-feature and detailed-saving screens had already been deleted before this phase. Do not resurrect them or their resources. The old port's monolithic settings layout had already been replaced by the original category layout. No read-all confirmation, peer-id feature, seconds display, sticker/GIF/voice confirmation, proprietary bulk-history merger, proprietary code, pill UI, device-brand jokes or custom URL handlers are required.

The current independent login compatibility/protocol handling, official client identity and certificate choices, localization tooling, branding/splash changes, background-service retry on app resume, shared storage-budget work, Solar deletion icon, retained eye/clock badges and settings switch restoration remain. Their conceptual resemblance to another client is not evidence that their independent implementation should be deleted. They require no new implementation in phase B.
