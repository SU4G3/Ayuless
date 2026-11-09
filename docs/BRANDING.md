# Ayuless app identity

Branding is permanent app identity, not an optional feature. No new feature or setting is introduced.
Own logic lives in `org.ayuless.branding.AppBranding`; upstream Java edits carry `// Ayuless:` comments.
No external fork code or artwork was imported; the mark comes from `branding/generate.py` and the existing
`drawable/ayuless_settings_logo.xml`. No credits/license additions are needed.

## Language packs and strings

`LocaleController.getStringV2` wraps the existing local/cloud lookup with `AppBranding.localize`.
The resource ID resolves to its original resource key, including when callers omit the deprecated key
argument. Both ordinary strings and formatted/plural/spannable templates use this path. Replacement
happens **before** formatting, so user names, chat names, parameters and message contents are untouched.
Unknown keys pass through, including server feature names and links. `getServerString` stays unchanged.
This applies again after every downloaded language-pack change; it does not modify downloaded packs.

The explicit allowlist replaces the brand in Latin (case insensitive), Cyrillic (including inflections),
Arabic/Persian, Korean and Japanese spellings. Translations that omit the brand need no replacement.
AppName/AppNameBeta always return Ayuless/Ayuless Beta. Only the first brand mention in
ContactsPermissionAlert is replaced: its second mention names Telegram cloud servers. Already-branded
local templates pass through that special case unchanged.

English app templates were moved into `values/ayuless.xml`. Russian templates are in
`values-ru/ayuless.xml`; equivalent existing translations were updated in the other bundled
`values-*/strings.xml` files. Original resource keys and formatting arguments are retained.
Launcher/recents/share labels already used Ayuless; AppName is now protected from cloud overrides too.
These keys cover onboarding, app titles, permission prompts, cache/database UI, local passcodes,
update/unsupported-content prompts, browser settings, call notifications and hidden-chat notification
labels/channel names. Service notifications using AppName inherit the same behavior.

Changed/protected key inventory:

```text
AgeVerificationNeedCameraPermission, AllowBackgroundActivityInfo, AllowBackgroundActivityInfoOneUIAboveS,
AllowBackgroundActivityInfoOneUIBelowS, AllowFillNumber, AllowReadCall, AllowReadCallAndLog, AllowReadCallLog,
AppLocked, AppName, AppNameBeta, AppUpdate, AppUpdateBeta, BotLocationPermissionRequestDeniedApp,
BrowserExternalRestricted, BrowserSettingsCookiesInfo, BrowserSettingsCustomTabs, BrowserSettingsEnableInfo,
BrowserSettingsNoCustomTabsInfo, CallViaTelegram, ChangePasscodeInfo, ClearTelegramCache,
ClearingCacheDescription, ContactsPermissionAlert, CreateNewThemeAlert, CreatePasscodeInfoPIN,
CreatePasscodeInfoPassword, EnterYourPasscodeInfo, EnterYourTelegramPasscode,
Gift2ExportTONUpdateRequiredText, ImportImportingInfo, IncomingCallsSystemSettingDescription,
LiveLocationPausedText, LocalDatabaseInfo, LocalDatabaseSize, LowDiskSpaceMessage, NoChats,
NotificationHiddenChatName, NotificationHiddenName, NotificationsPermissionAlertSubtitle,
OpenInTelegramBrowser, OptimizingTelegram, Page1Title, Page2Message, Page3Message, Page4Message, Page5Message,
Page6Message, PermissionBackgroundLocation, PermissionDrawAboveOtherApps,
PermissionDrawAboveOtherAppsGroupCall, PermissionFSILockscreen, PermissionNoAudioStorageStory,
PermissionNoAudioVideoWithHint, PermissionNoAudioWithHint, PermissionNoBluetoothWithHint,
PermissionNoCameraMicVideo, PermissionNoCameraWithHint, PermissionNoContactsSaving,
PermissionNoContactsSharing, PermissionNoLocation, PermissionNoLocationFriends,
PermissionNoLocationNavigation, PermissionNoLocationStory, PermissionNoStorageAvatar,
PermissionStorageWithHint, PermissionXiaomiLockscreen, ProfilePopupNotificationInfo,
QRCodePermissionNoCameraWithHint, RoundVideoCompositionInfo, ScreenCaptureAlert, SdCardAlert,
SdCardErrorDescription, SecretChatName, StorageUsageTelegram, StorageUsageTelegramLess, StoryUnsupported,
TelegramCacheSize, TelegramVersion, UnlockToUse, UnsupportedBlockMessage, UnsupportedMedia2,
UnsupportedMessageMessage, UpdateAppAlert, UpdateContactsMessage, UpdateTelegram, UseProxySponsorInfo,
VideoCallViaTelegram, VoiceCallViaTelegram, VoipInCallBranding, VoipInCallBrandingWithName,
VoipInConferenceCallBranding, VoipInVideoCallBranding, VoipInVideoCallBrandingWithName,
VoipNeedCameraPermission, VoipNeedMicCameraPermissionWithHint, VoipNeedMicPermissionWithHint,
VoipOutgoingCall, WalletAuthorizeTransactionPasscode
```

Hardcoded installed-client references changed: Android Contacts actions in `ContactsController`
(Profile/Voice Call/Video Call), and this-installation session placeholder in `SessionsActivity`.
Remote session app names are preserved.

## Artwork and resources

- `drawable/ayuless_mark.xml`: transparent white chevron/dot from the existing brand geometry.
- `drawable/notification.xml`: monochrome notification silhouette, replacing all four density-specific
  Telegram notification bitmaps. Existing notification callers use it without per-call changes.
- `drawable/ic_launcher_dr.xml`: Ayuless identity for Android account/contact/phone-account icons and
  fallback notification person icons, replacing five density-specific Telegram launcher bitmaps.
- `drawable/tg_splash_320.xml`: rounded Ayuless vector replaces Telegram's animated Android 12 splash,
  retaining the final upstream 200×200dp bounds centered in the 320×320dp canvas (60dp margins).
- `IntroActivity`: localized Ayuless title replaces Telegram's wordmark; the full rounded vector from
  `ayuless_settings_logo` is rasterized at device density into the centered 150×150dp OpenGL sphere.
  The upstream 200×150dp TextureView, layout and slide transitions are unchanged. The 82×74 plane
  with its (6, −5) anchor and its circular clipping mask use empty textures, so they cannot distort,
  shift or crop the square artwork. Brand colors are fixed in both themes; other slides retain
  their existing generic animations.
- `DialogsActivity`: localized Ayuless text replaces the Telegram wordmark in the main header.
- `BlockingUpdateView`: existing Ayuless settings logo replaces the Telegram update animation.
- `BrandingArtwork`, `RoundVideoOverlayRenderer`, `InstantCameraVideoEncoderOverlayHelper`: replace
  both the plane and baked Telegram wordmark in app-generated round-video overlays with the Ayuless
  vector mark and a Canvas-drawn wordmark. Logo atlas frames are static.
- `AndroidManifest.xml`, `LauncherIconController`: ordinary Vintage/Aqua launcher aliases and their
  previews use the existing Ayuless launcher resources; the default already did so.
- `layout/call_notification.xml`, `layout/call_notification_rtl.xml`: tooling preview label updated.

`chats_widget_preview.webp` and `contacts_widget_preview.webp` were visually inspected: they contain
sample contacts/messages, no Telegram identity. Widget preview layouts likewise contain no brand logo.
The settings/About version footer uses TelegramVersion, now an Ayuless template.

## Telegram intentionally retained

Telegram names the network, accounts, cloud storage, server-side moderation, calls as a network
service, Premium, Stars, Business, Passport, Ads, Gifts, TON/Fragment integration, authentication,
other clients and official support/legal pages. Invite/share strings retain Telegram because their
links invite users to the Telegram service/download, not an Ayuless distribution endpoint.
ContactsPermissionAlert keeps its Telegram-server clause. AyulessDisclaimer keeps its independence
statement. App-owned permission and local passcode prompts are renamed even inside server flows.

Resource inventory of retained Telegram strings (including Standalone SMS program strings):

```text
AIEditorLimitText, AIEditorStyleLimitTextPremium, AboutPremiumDescription, AboutPremiumDescription2,
AboutPremiumTitle, AboutTelegramPremium, AccountFrozen1Text, ActionGiftPremiumText, ActionGiftStarsSubtitle,
ActionGiftStarsSubtitleYou, ActionUniqueGiftTransferOutboundAssigned, AdHidden, AddAccountInfo2_one,
AddAccountInfo2_other, AddAnotherAccountInfo, AddGroupEmojiPackHint, AdditionalReactionsDescription,
AffiliateProgramStartedText, AgeVerificationTextGB, AnimatedEffectPremium, AreYouSureLogout,
AreYouSureLogoutGrams, AreYouSureWebSessions, ArticleConversionText, AskAQuestionInfo, AuthAnotherClientInfo1,
AuthAnotherClientInfo2, AuthAnotherClientInfo4, AuthAnotherClientUrl, BlockUserAlert, BlockUserMessage,
BlockUsersMessage, BoostingChooseRandomGroup_one, BoostingChooseRandomGroup_other, BoostingChooseRandom_one,
BoostingChooseRandom_other, BoostingGetMoreBoostByGifting, BoostingGetMoreBoostByGiftingCount_one,
BoostingGetMoreBoostByGiftingCount_other, BoostingGiveawayAdditionPrizeCountHint_one,
BoostingGiveawayAdditionPrizeCountHint_other, BoostingGiveawayAdditionPrizeCountNameHint_one,
BoostingGiveawayAdditionPrizeCountNameHint_other, BoostingGiveawayHowItWorksSubTextDateEnd_one,
BoostingGiveawayHowItWorksSubTextDateEnd_other, BoostingGiveawayHowItWorksSubTextDateSeveral1_one,
BoostingGiveawayHowItWorksSubTextDateSeveral1_other, BoostingGiveawayHowItWorksSubTextDateSeveralEnd1_one,
BoostingGiveawayHowItWorksSubTextDateSeveralEnd1_other, BoostingGiveawayHowItWorksSubTextDate_one,
BoostingGiveawayHowItWorksSubTextDate_other, BoostingGiveawayHowItWorksSubTextEnd_one,
BoostingGiveawayHowItWorksSubTextEnd_other, BoostingGiveawayHowItWorksSubTextSeveral1_one,
BoostingGiveawayHowItWorksSubTextSeveral1_other, BoostingGiveawayHowItWorksSubTextSeveralEnd1_one,
BoostingGiveawayHowItWorksSubTextSeveralEnd1_other, BoostingGiveawayHowItWorksSubText_one,
BoostingGiveawayHowItWorksSubText_other, BoostingGiveawayHowItWorksTextEndGroup_one,
BoostingGiveawayHowItWorksTextEndGroup_other, BoostingGiveawayHowItWorksTextEnd_one,
BoostingGiveawayHowItWorksTextEnd_other, BoostingGiveawayHowItWorksTextGroup_one,
BoostingGiveawayHowItWorksTextGroup_other, BoostingGiveawayHowItWorksText_one,
BoostingGiveawayHowItWorksText_other, BoostingGiveawayJustStarted, BoostingGiveawayJustStartedGroup,
BoostingGiveawayMsgInfoPlural1_one, BoostingGiveawayMsgInfoPlural1_other,
BoostingGiveawayResultsMsgWinnersTitle_one, BoostingGiveawayResultsMsgWinnersTitle_other,
BoostingGiveawayServiceWinnersSelected_one, BoostingGiveawayServiceWinnersSelected_other,
BoostingGroupBoostWhatAreBoostsDescription_one, BoostingGroupBoostWhatAreBoostsDescription_other,
BoostingLinkAllows, BoostingLinkAllowsAnyone, BoostingLinkAllowsToUser, BoostingLinkUsed, BoostingPremium,
BoostingPremiumChristmasSubTitle, BoostingPremiumChristmasToast, BoostingReassignBoostTextLink,
BoostingReceivedGiftDuration, BoostingReceivedPrizeDuration, BoostingStars,
BoostingStarsGiveawayServiceWinnersSelected_one, BoostingStarsGiveawayServiceWinnersSelected_other,
BoostingStoriesFeaturesAndTerms, BoostingTelegramPremiumCountPlural_one,
BoostingTelegramPremiumCountPlural_other, BoostingTelegramPremiumFor, BoostingUnclaimedPrizeDuration,
BoostingWaitWarningPlural_one, BoostingWaitWarningPlural_other, BotAffiliateProgramFeature2,
BotMonetizationInfo, BotMonetizationInfoFeature1Name, BotMonetizationInfoFeature1Text,
BotMonetizationInfoTONText, BotPermissionGameAlert, BotRestoreStorageText, BotWebAppDisclaimerSubtitle,
BusinessBotNotSupportedMessage, CallAvailableIn2, CallReportSent, CallText, CancelAccountResetInfo2,
CaptionPremiumSubtitle_other, ChangePhoneNumberOccupied, ChannelAntiSpamFalsePositiveReported,
ChannelAntiSpamInfo, ChannelAntiSpamUser, ChannelCaptionLimitPremiumPromo_one,
ChannelCaptionLimitPremiumPromo_other, ChannelEnablePaidReactionsInfo, ChannelLinkInfo, ChannelUserLeftError,
ChatMultipleReactionsPromo, CheckPhoneNumberInfo, ClearOtherWebSessionsHelp, CocoonFeature2Text,
ContactJoined, ContactNotRegistered, ContactNotRegisteredTitle, CreateManagedBotUsernamePremiumRequired,
CreateManagedBotUsernamePremiumRequiredGeneric, CreateManagedBotUsernameSuffixesInfo, DidNotGetTheCodeInfo,
DownloadSpeedLimitedMessage, DownloadedFilesMessage, EmailLoginChangeMessage, EmojiSuggestionsInfo,
EnableAllStreamingInfo, EnabledPasswordText, ExpiredViewsStubPremiumDescription, ExplainStarsFeature2Text,
ExplainStarsFeature3Text, ExplainStarsTitle2, FeaturePreviewGiftsDescription, FolderShowTagsInfoPremium,
ForwardHideSenderNamePremium, FpnvConsentText, Gift2ActionWearNeededPremium, Gift2AuctionLearnMore2,
Gift2CantDoText, Gift2ChannelNotifyChecked, Gift2ChannelNotifyNotChecked, Gift2ExportTONFragmentText,
Gift2ExportTONUnlocksAlertText_one, Gift2ExportTONUnlocksAlertText_other, Gift2Host, Gift2MoveToMyGiftsText,
Gift2PremiumInfo, Gift2PremiumSubtitle, Gift2PremiumSubtitleMany_one, Gift2PremiumSubtitleMany_other,
Gift2PrepayUpgradeFeature2Text, Gift2TransferToMyProfileText, Gift2UpgradeFeature2Text,
Gift2UpgradeFeature2TextLearn, GiftPremiumAboutThisLinkEnd, GiftPremiumActivateErrorTitle,
GiftPremiumListFeaturesAndTerms, GiftPremiumPrivacyPolicyAndTerms, GiftStarsSubtitle,
GiftTelegramPremiumDescription, GiftTelegramPremiumTitle, GiftValueAverage, GiftValueLastTelegram,
GiftValueMinPriceInfo, GiftValueOnSaleTelegram, GiftsFeature2Text, GotAQuestion, GraceSuggestionTitle,
GroupCallCreatedLinkText, GroupCallLinkText, GroupUserLeftError, ImportDoneInfo, ImportStickersDoneInfo,
InviteFriendsHelp, InvitePremiumBlockedSubscribe, InviteText2, InviteTextNum_one, InviteTextNum_other,
InviteToTelegram, InviteToTelegramShort, InviteUser, LanguageUnknownCustomAlert, LimitReachedAccounts,
LimitReachedChatInFolders, LimitReachedCommunities, LimitReachedFileSize, LimitReachedFolderLinks,
LimitReachedFolders, LimitReachedPinDialogs, LimitReachedPinSavedDialogs, LimitReachedPublicLinks,
LimitReachedReorderFolder, LimitReachedSharedFolders, LimitReachedStoriesCount2Second_one,
LimitReachedStoriesCount2Second_other, LimitReachedStoriesMonthly, LimitReachedStoriesWeekly, LinkInfo,
LinkInfoChannel, LoginEmailResetMessage, LoginEmailResetPremiumRequiredMessage,
LoginEmailResetPremiumRequiredTitle, MapPreviewProviderTelegram, MessageScheduledRepeatPremium,
MigrateOldFolderDescription, MonetizationInfo, MonetizationInfoFeature1Name, MonetizationInfoFeature1Text,
MonetizationInfoTONText, MoreSimilarBotsText, MoreSimilarText, NewContactPhoneNotOnTelegram,
NewContactPhoneOnTelegram, NoContactsYet3Sub, NoPhoneFound, NoRtmpStreamFromAppOwner,
NoRtmpStreamFromAppViewer, NoUsernameFound, NoUsernameFound2, NotificationContactJoined,
NotificationMessageGiftCode, NotificationUnrecognizedDevice, NotificationsServiceConnectionInfo,
NumberNotOnTelegram, OptionPremiumRequiredMessage, OtherLoginCode, PassportInfo2, PassportInfoTitle,
PassportPhoneUseSameEmailInfo, PassportPhoneUseSameInfo, PassportRequestPasswordInfo, PaymentWarningText,
PhoneNumberHelp, PleaseLoginPassport, PremiumAvatarToast, PremiumLastSeenButton2, PremiumMessageButton,
PremiumMessageText, PremiumNeededForBoosting, PremiumNeededForBoostingGroup,
PremiumPreviewAdvancedChatManagementDescription2, PremiumPreviewAppIconDescription,
PremiumPreviewAppIconDescription2, PremiumPreviewDownloadSpeedDescription2, PremiumPreviewEmojiPack,
PremiumPreviewNoAdsDescription, PremiumPreviewNoAdsDescription2, PremiumPreviewProfileBadgeDescription,
PremiumPreviewVoiceToTextDescription2, PremiumReadButton2, PrimaryLinkHelp, PrimaryLinkHelpChannel,
PrivacyBotsInfo, PrivacyCallsP2PHelp, PrivacyLastSeenPremium, PrivacyLastSeenPremiumForPremium,
PrivacyLastSeenPremiumInfoForPremium, PrivacyMessagesInfo, PrivacyPaymentsClearAlert, PrivacyPhoneInfo,
PrivacyPhoneInfo3, PrivacyPremiumText, PrivacyVoiceMessagesPremiumOnly, PrivateMessagesChargePremiumLocked,
ProfileBotOpenAppInfoOwner, ProfileUnofficialSecurityRisk, QRCodeLinkGroupCall, QRCodeLinkHelpChannel,
QRCodeLinkHelpChannelDirect, QRCodeLinkHelpFolder, QRCodeLinkHelpGroup, RecoveryEmailSubtitle, ReportSentInfo,
Reported2, RestorePremiumHintMessage, RevenueSharingAdsAlertSubtitle, RevenueSharingAdsInfo1Subtitle,
RevenueSharingAdsInfo1SubtitleBot, RevenueSharingAdsInfo2Subtitle, RevenueSharingAdsInfo2SubtitleBot,
RevenueSharingAdsInfo3Subtitle, RevenueSharingAdsInfo3SubtitleBot, RevenueSharingAdsInfo4Subtitle2,
RevenueSharingAdsInfo4Subtitle2Bot, SMSFee1Text, SMSFee1TextCountry, SMSFee2Text, SMSFee3Text,
SMSFee3TextDays_one, SMSFee3TextDays_other, SMSFee3Title, SMSFeePurchaseText, SMSFeePurchaseTextDays_one,
SMSFeePurchaseTextDays_other, SaveOnAnnualPremiumMessage, SearchAdsAbout2Subtitle,
SearchAdsAbout2SubtitlePremium, SearchAdsAboutLaunchSubtitle, SearchAdsAboutSubtitle, SearchPostsHeaderNews,
SecretLinkPreviewAlert, SecretWebPageInfo, SelectSendAsPeerPremiumHint, SentAppCode, SentAppCodeTitle,
SentAppCodeWithPhone, SentCallCode, SessionsListInfo, SessionsSettingsInfo, ShareTelegram2, ShowAdsInfo,
SmsJobsPremiumHintMessage, SmsNotificationTitle, SmsPermissionText, SmsPermissionTextPhoneSettings,
SmsPermissionTextSMSPhoneSettings, SmsPermissionTextSMSSettings, SmsPremiumActivatedText,
SmsPremiumActivatedTextUnknown, SmsStatusDoneSubtitle, SmsStatusFirstSubtitle, SmsStatusNoPermissionSubtitle,
SmsStatusSendingSubtitle_other, SmsSubscribeFeature1Message_other, SmsSubscribeFeature2Title,
SmsSubscribeFeature3Message, SmsSubscribeMessage, SponsoredMessageInfo2Description1,
SponsoredMessageInfo2Description2, SponsoredMessageInfo2Description3, StarRatingDescription,
StarRatingDescription1, StarRatingSelfDescription, StarRatingTitle1, StarsTransactionPremiumGift,
StealthModePremiumHint, StorageCleared2, StorageUsageInfo, StoriesPremiumHint2, StoryLinkPremium,
StoryPeriodPremium_one, StoryPeriodPremium_other, StoryPremiumFormatting, StoryPremiumWidgets2_other,
StoryViewsPremiumHint, SubscribeToPremiumOfficialAppNeededDescription, SummaryLimit, SyncContactsDeleteText,
TelegramBusiness, TelegramBusinessSubscribedSubtitleTemp, TelegramBusinessSubtitle,
TelegramBusinessSubtitleTemp, TelegramContacts_one, TelegramContacts_other, TelegramFAQ, TelegramFaq,
TelegramFeatures, TelegramFeaturesUrl, TelegramPassport, TelegramPassportCreatePasswordInfo,
TelegramPassportDelete, TelegramPassportDeleteAlert, TelegramPassportDeleteTitle, TelegramPremium,
TelegramPremiumSubscribedSubtitle, TelegramPremiumSubtitle, TelegramPremiumUserDialogSubtitle,
TelegramPremiumUserDialogTitle, TelegramPremiumUserGiftedPremiumDialogTitleWithPlural,
TelegramPremiumUserGiftedPremiumDialogTitleWithPluralSomeone,
TelegramPremiumUserGiftedPremiumOutboundDialogTitleWithPlural, TelegramPremiumUserStatusDialogSubtitle,
TelegramStars, TelegramStarsInfo2, TelegramTones, TerminateWebSessionInfo, TodoPremiumRequired, TosDecline,
TosDeclineDeleteAccount, TosUpdateDecline, TranscriptionTrialEndBuy, TranscriptionTrialEndWaitOrBuy,
TurnPasswordOffPassport, UnconfirmedAuthDeniedWarning, UnlockPremium, UnlockPremiumEmojiHint,
UnlockPremiumEmojiReaction, UnlockPremiumReactionsDescription, UnlockPremiumStickersDescription,
UnlockPremiumTranscriptionHint, UnlockSimilarChannelsPremium_other, UpgradePremiumMessage,
UploadSpeedLimitedMessage, UseProxyTelegramInfo2, UserBirthdayHint, UsernameHelp,
VoipChannelVideoNotAvailableAdmin, VoipErrorUnknown, VoipPeerVideoOutdated, VoipQuickRepliesExplain,
VoipRateCallAlert, VoipRingtoneInfo, VoipVideoNotAvailableAdmin, WalletAddressLinkedToUser,
WalletAddressNoLinkedAccount, WalletBackupDisabledDescription, WalletBackupEnabledDescription,
WalletBackupEnabledInfo, WalletEnterSecretPhraseInfo, WalletFeePaidByTelegram, WalletFirstGramsStoreInfo,
WalletInvalidRecoveryPhraseInfo, WalletNetworkFeeInfo, WalletRecoveryPhraseSupportWarning,
WalletTransactionTelegram, WearAuthTitle, WebSessionsTitle, YourContacts, YourEmailSkipWarningText
```

Artwork retained: Premium/Stars/Fragment and Premium launcher choices (Premium/Turbo/Nox),
`TermsOfServiceView`/`logo_middle` (Telegram terms), `DialogStoriesCell`/`telegram_logo_2` (service
identity), QR logo/plane assets in network invite/authentication/payment QR codes, wallet service art. Unreferenced legacy artwork is not bulk-deleted.

Protocol and compatibility identifiers remain unchanged: org.telegram packages, bot JavaScript bridge,
Telegram user-agent compatibility token, tg:// and t.me URLs, TelegramConnectionService identifiers,
existing Telegram media-directory names/migration paths, service account 777000 and other service
users in MessagesController. Login help describing server errors and mentions of other devices/clients
remain Telegram. Source copyright/license headers are unchanged.

## Verification

Standalone checks (no Android SDK needed):

```sh
python3 tests/branding/check_resources.py
javac -d /tmp/ayuless-branding-test TMessagesProj/src/main/java/org/ayuless/branding/AppBranding.java tests/branding/AppBrandingTest.java
java -cp /tmp/ayuless-branding-test AppBrandingTest
```

These passed, along with `git diff --check`. Checks cover cloud/local identity templates, translated
names, mixed app/server wording, formatting arguments, untouched server keys, nulls, XML parsing,
unique string names and EN/RU placeholder parity.

The requested offline Gradle compile could not reach compilation: the wrapper lock in $HOME/.gradle
is read-only in this sandbox. Running the installed Gradle directly with GRADLE_USER_HOME under /tmp
also failed before configuration: "Could not determine a usable wildcard IP for this machine."
An APK build, device UI checks, notification rendering, splash/intro animation and a live downloaded
language-pack switch still need verification outside this sandbox.
