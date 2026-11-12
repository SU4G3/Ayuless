# Clean-room phase-A removal ledger

The functional handoff is [CLEANROOM-SPEC.md](CLEANROOM-SPEC.md). This ledger records removals against input revision 4b25f82c. The phase-A investigation used docs/CREDITS.md, every Radolyn-marked source/resource file, port commit 1eeac1756, its independent parent e965f338, subsequent settings commits 1bcbcc67 and 4b25f82c, and the read-only reference checkout at /home/surge/codex-ayuless/ayugram4a. The reference checkout was not changed. No new feature implementation was written.

Removal means deletion of complete derived files or removal of derived portions in mixed files. Where the port had replaced an independent statement/body, the earlier independent version was retained/restored directly from e965f338. This includes the old history sheet route, ordinary Premium checks, earlier scheduling floor and request-submission timing. These are baseline preservation, not a phase-B implementation. The pre-removal behavior to reconstruct is specified in the contract, and CLEANROOM markers identify the gaps. This intermediate source is not claimed to preserve runtime feature parity.

## Completely deleted files

All Java paths in this table are relative to TMessagesProj/src/main/java/.

| File | Removed responsibility | Specification |
| --- | --- | --- |
| org/ayuless/edits/DeletedMessageActions.java | Deleted-original reply quotation, explicit remote-delete/local-retention action, deletion-time dialog | CR-06.1, CR-06.3, CR-06.5 |
| org/ayuless/edits/EditHistoryActivity.java | Native bubble-based revision screen, revision preparation and copy/open interactions | CR-05.2–3, CR-02.4 |
| org/ayuless/ghost/GhostMenu.java | Optional privacy toggle in chat-list overflow | CR-03.5 |
| org/ayuless/ghost/GhostShortcut.java | Dynamic shortcut creation, disabling and intent handling | CR-03.5 |
| org/ayuless/settings/GhostAccountSelector.java | Avatar/name profile picker, shared row and selected-profile check | CR-02.1, CR-02.4 |
| org/ayuless/settings/SettingsTextDialog.java | Dormant general text preference editor with reset | CR-07, CR-02.4 |
| org/ayuless/spy/MessageSavingConfig.java | Account capture-policy preferences, bot/media category decisions and filtered deletion snapshots | CR-04, CR-10 |
| docs/LICENSE-AYUGRAM.txt | License text accompanying the removed port | CR-01 handoff boundary; no executable behavior |

The account picker contained upstream-derived account selection geometry as well as a later adaptation and local additions. It was deleted as one provenance-mixed port artifact; the independent upstream AccountSelectCell was kept. No account-selection replacement was implemented.

## Removed hunks in retained Ayuless files

Java paths remain relative to TMessagesProj/src/main/java/. All Radolyn port headers in these files were removed together with their derived portions; their independent Ayuless notices remain. The table identifies functional hunks rather than unstable line numbers.

| File | Removed hunk / retained boundary | Specification |
| --- | --- | --- |
| org/ayuless/appearance/AppearanceConfig.java | customMarks/deletedMarkText/editedMarkText constants and accessors, text reset/set support, custom-mode clearing added to setMarkEmoji. Original appearance preferences and setters remain. | CR-07 |
| org/ayuless/appearance/DeletedStyle.java | Text-prefix branch in decorateTimeLabel. Original deleted detection, opacity, emoji/tint logic and later independent Solar wastebasket branch remain. | CR-07 |
| org/ayuless/content/ProtectedContent.java | Deleted-cache localResend condition, forwardingRestricted and showForwardHint helpers, and associated needsCopy/sendCopies refactoring. Earlier independent copy-forward implementation remains. | CR-06.2, CR-06.4 |
| org/ayuless/content/RetentionConfig.java | Additional bot-policy predicate in storable. Original exceptions, enablement and content-permission logic remain. | CR-04 |
| org/ayuless/edits/EditHistory.java | Old-revision attachment capture call in onServerEdit and broadened zero/nonzero media-id comparison in contentChanged. Earlier independent capture implementation remains. | CR-05.1 |
| org/ayuless/edits/EditHistorySheet.java | Port-localized current/initial/numbered/empty labels and sheet title. Earlier independent text sheet remains. | CR-05.2, CR-11 |
| org/ayuless/extras/ExtrasConfig.java | Seven port feature constants: readUntil, localResend, keepOwnDeletes, deleteDate, ghostInMenu, ghostShortcut, restrictedForwardHint; hint-specific true default. Original advertising/Premium/background preferences and later requested advertising/background true defaults remain. | CR-02.1, CR-02.3, CR-03.5, CR-06 |
| org/ayuless/extras/LocalPremium.java | Multi-account refresh routine and port-specific explanatory comments. Original independent translation gate remains. | CR-08 |
| org/ayuless/ghost/DelayedSend.java | Port's 11-second minimum-ahead value. Original 5-second constant and original scheduling implementation remain, with a marker for the target behavior. | CR-03.3 |
| org/ayuless/ghost/GhostConfig.java | Initial master-activation seeding and readOnAction/delayedSend mutual-exclusion writes inside setProfile. Original profile setter, keys, inheritance and locks remain. | CR-03.1 |
| org/ayuless/ghost/GhostInterceptor.java | Explicit receipt identity tracking and readUntil request construction; explicit bypass check; online-triggered offline branch; onSuccessfulRequest and the port's movement of automatic read triggering out of shouldBlock. Original interception, one-request automatic-read allowance, secret handling and baseline send-time behavior remain. | CR-03.2–4 |
| org/ayuless/settings/AppearanceSettingsActivity.java | Restricted-forward-hint row id, row and event branch; local Premium refresh call. Original appearance controls, useful-feature group, subsequent original explanatory rows and service action remain. | CR-02.3, CR-06.4, CR-08 |
| org/ayuless/settings/GhostSettingsActivity.java | Three global feature row ids, rows and click handling; ExtrasConfig import; adapted avatar-picker call. The picker body is now only a CLEANROOM marker. Original profile chip, account binding fix, profile controls, locks and independently authored explanatory text remain. | CR-02.1, CR-02.4, CR-03.5 |
| org/ayuless/spy/SavedAttachments.java | Media-category capture gate in onKept and whole prepareHistory routine. Original attachment saver/restorer, encrypted copy support, later true default and shared-budget work remain. | CR-04, CR-05.3 |
| org/ayuless/storage/AyulessStore.java | Filtered snapshot call in saveDeleted, formatting-policy condition in saveEdit, and getDeleteDate method. Original serializers/schema/query/export/import logic and subsequent independent common-budget work remain. | CR-04, CR-06.5, CR-10 |

## Removed hunks in retained Telegram files

All paths remain relative to TMessagesProj/src/main/java/. Telegram's original GPL version-2-or-later notices are retained. Removing a port header did not remove the underlying Telegram class.

| File | Removed hunk | Specification |
| --- | --- | --- |
| org/telegram/messenger/ApplicationLoader.java | Radolyn header only. The temporary sync initialization had already been removed before this phase; original startup code remains. | CR-01, CR-12 |
| org/telegram/messenger/MediaDataController.java | Extra shortcut slot reservation, recreation after removeAllDynamicShortcuts and retained ghost identifier during pruning, all in buildShortcuts; port header. | CR-03.5 |
| org/telegram/messenger/MessagesController.java | Local-self Premium extension in isPremiumUser; port header. Original edit-capture hook and other independent integrations remain. | CR-08 |
| org/telegram/messenger/SendMessagesHelper.java | Deleted-reply preparation at sendMessage(SendMessageParams) entry; port header. Existing independent copy-forward and delayed-send hooks remain. | CR-06.1–2 |
| org/telegram/messenger/UserConfig.java | Local-Premium extension in both checkPremiumSelf update paths and isPremium; port header. | CR-08 |
| org/telegram/tgnet/ConnectionsManager.java | Successful-response ghost callback in sendRequest stageQueue dispatch; port header. Existing privacy interception and independent login/identity handling remain. | CR-03.2 |
| org/telegram/ui/Cells/ChatMessageCell.java | Custom edited text in ordinary timeString branch and associated port comment/header. Original deleted-style, retained-media and attachment hooks remain. | CR-07 |
| org/telegram/ui/ChatActivity.java | Action constants 9003–9005; explicit read, deletion-time and keep-local dispatch and menu eligibility; full-screen history route and localized history label; extended original-forward-restriction footer in createMenu; port header. Original history integration 9001 and retained-media action 9002 remain. | CR-03.4, CR-05.2, CR-06, CR-07 |
| org/telegram/ui/DialogsActivity.java | GhostMenu call/comment in showItemOptions; port header. Original action-bar ghost badge remains. | CR-03.5 |
| org/telegram/ui/LaunchActivity.java | Ghost shortcut handler in handleIntent; port header. Later independent KeepAliveService.apply call in onResume remains. | CR-03.5, CR-12 |

## Resources removed

Forty string entries were removed from each of TMessagesProj/src/main/res/values/ayuless.xml and TMessagesProj/src/main/res/values-ru/ayuless.xml, together with their Radolyn/adapted-wording headers. This includes the surviving port-added resource set and the later explicit-read, deleted-action and restricted-forward resource additions. No replacement translations were written. XML CLEANROOM comments point to the rephrased meanings in the specification.

| Removed resource identifiers, in both locales | Specification |
| --- | --- |
| AyulessCustomMarks, AyulessDeletedText, AyulessEditedText | CR-07, CR-11 |
| AyulessGhostMenu, AyulessEnableGhost, AyulessDisableGhost, AyulessGhostShortcut | CR-02.1, CR-03.5, CR-11 |
| AyulessCustomSaving, AyulessCustomSavingInfo, AyulessMediaCategories, AyulessChooseMediaCategories | CR-04, CR-11 |
| AyulessMediaPrivateChats, AyulessMediaPublicChannels, AyulessMediaPrivateChannels, AyulessMediaPublicGroups, AyulessMediaPrivateGroups | CR-04 |
| AyulessSaveFormatting, AyulessSaveReactions, AyulessSaveBots | CR-04 |
| AyulessReadUntilSetting, AyulessReadUntil, AyulessReadSent, AyulessReadUntilInfo | CR-02.1, CR-03.4, CR-11 |
| AyulessLocalResend, AyulessKeepLocalSetting, AyulessKeepLocally, AyulessKeepLocallyConfirm, AyulessDeletedActionsInfo | CR-06.1–3, CR-11 |
| AyulessDeleteDateSetting, AyulessDeleteDate, AyulessUnknown | CR-06.5, CR-11 |
| AyulessEditsHistoryTitle, AyulessEditsHistoryMenu, AyulessHistoryCurrent, AyulessHistoryOriginal, AyulessHistoryVersion, AyulessHistoryEmpty, AyulessHistoryHint | CR-05.2, CR-11 |
| AyulessRestrictedForwardHint, AyulessOriginalForwardRestricted | CR-02.3, CR-06.4, CR-11 |

The three older captions temporarily changed to reference wording in the port were examined separately: AyulessGhostOfflineAfterSend, AyulessGhostReadOnAction and AyulessGhostDelayedSend. Subsequent commits had already replaced those port values with independent short wording. Their current independent values remain. Pre-port generic feature names, independent later explanatory text, branding overrides, locale tooling, current two-placeholder attachment usage text and the original content-retention strings were not removed solely for resembling another client's feature vocabulary.

## Documentation and contribution boundary

| File | Change | Specification |
| --- | --- | --- |
| docs/CREDITS.md | Removed the complete Code derived from AyuGram section, source mappings, obsolete license-permission paragraph and preceding port-adaptation explanation. Kept the thanks/inspiration line, Solar attribution and upstream attribution. Added links to the clean-room documents. | CR-01 |
| docs/AYUGRAM-PARITY.md | Kept original Ayuless audit/documentation but marked it historical and directed phase B to the new contract. | CR-01, CR-12 |
| README.md | Removed claims that the current source retains the attributed port; kept inspiration/thanks. Stated that independent reimplementation is pending. | CR-01 |
| CONTRIBUTING.md | Replaced the old permission to import the reference code with the requested clean-room boundary, including exclusion of historical implementation from phase-B inputs. Other rules remain. | CR-01 |
| docs/CLEANROOM-SPEC.md | Added prose/table contract with fresh EN/RU meanings, keys/defaults, current/dormant UI distinctions, behavior, hook locations, storage formats and measurements. | CR-01–12 |

## Reviewed files deliberately retained

| Area | Provenance decision and remaining work |
| --- | --- |
| org/ayuless/settings/SpySettingsActivity.java | Its port-only navigation and header had already been removed. Current grouping, slider, usage accounting and explanations are original later Ayuless work; no executable hunk needed removal now. CR-02.2 records the layout. |
| org/ayuless/settings/AyulessSettingsActivity.java | Current source already matches the independent category page from the pre-port parent. The temporary monolithic settings implementation was absent at phase-A input. |
| AntiDeleteConfig, KeepAliveService, RetainedMedia, DeletedMessagePreview, DeletedMarkColors, AyulessSettingsFragment, Solar drawables and upstream Switch | Original behavior, user-requested later defaults, geometry, badges and switch restoration are retained. They are not replacements written in phase A. |
| AyulessDbHelper, StoredEdit and store infrastructure | Independent schema, serialization, account directories, quota and transfer infrastructure remain; no database/schema deletion or migration was performed. |
| MessageFilters and its UI; read-date/last-seen logic; secret/TTL/content-retention hooks | Independent richer implementations already existed; preserved. |
| LoginSpoof, BuildVars and pre-existing network/login hooks | Independent protocol/client-identity code predates the port. References to inspiration are not copied implementations; no matching substantive three-line blocks were found against the reference feature Java tree. Preserved. |
| Tools/test_ayugram_settings.py and Tools/test_strings_build.py | Original Ayuless verification harnesses, not imported implementations. Kept. The policy harness depends on removed production classes and remains intentionally unusable until phase B. It was not run or rewritten. |
| Sync and removed temporary settings classes | Already absent in the input tree: org/ayuless/sync/SyncConfig, SyncController; settings/SyncSettingsActivity, ChatFeaturesSettingsActivity and MessageSavingSettingsActivity. No historical implementation was restored. CR-12. |

## Static verification and handoff limitation

Phase A checked the complete port file inventory against the current tree, verified that all 32 existing Radolyn-marked Java paths were either stripped of their derived hunks/header or completely deleted, and removed both marked resource headers. Seven complete derived Java files were deleted. Each removed resource identifier is absent from both locale files; the surviving files parse as XML and have no duplicate string names. No surviving production Java reference to the deleted helper classes or removed preference accessors remains. Remaining Radolyn mentions in resources are independent affiliation disclaimers, and the credit keeps the requested inspiration thanks.

The mixed-file edits were compared with the independent parent to confirm that restored bodies come from that parent and later independent changes remain. A normalized three-substantive-line overlap scan between surviving org/ayuless Java and the reference feature Java tree found no matches; this is a supplementary mechanical check, not a proof of provenance. The ledger's history-based hunk classification is the main removal record. Whitespace/error checking uses git diff --check. No Gradle, Java compiler, Android build, production runtime tests, commit or push was invoked.

Git history and the external reference checkout still contain the removed material. Phase B must receive a clean source export and this specification without those inputs. No repository-history rewrite was attempted, and no claim is made that deleting working-tree files removes old Git objects.
