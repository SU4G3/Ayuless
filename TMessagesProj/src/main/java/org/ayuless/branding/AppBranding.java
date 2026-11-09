// SPDX-License-Identifier: GPL-3.0-or-later
package org.ayuless.branding;

import java.util.regex.Pattern;

/** Installed-app identity only. Never process user text or formatted arguments. */
public final class AppBranding {
    private static final Pattern NAME = Pattern.compile(
            "(?iu)Telegram|Телеграм(?:м)?(?:а|у|ом|е)?|تلگرام|تيليجرام|تيليغرام|تليجرام|텔레그램|テレグラム");

    private AppBranding() {}

    /** Runs after local/cloud lookup, before formatting. Unknown keys pass through. */
    public static String localize(String key, String value) {
        if (key == null || value == null) return value;
        switch (key) {
            case "AppName": return "Ayuless";
            case "AppNameBeta": return "Ayuless Beta";
            // The second mention names Telegram's cloud servers, not this app.
            case "ContactsPermissionAlert":
                return value.contains("Ayuless") ? value : NAME.matcher(value).replaceFirst("Ayuless");
            case "AgeVerificationNeedCameraPermission":
            case "AllowBackgroundActivityInfo":
            case "AllowBackgroundActivityInfoOneUIAboveS":
            case "AllowBackgroundActivityInfoOneUIBelowS":
            case "AllowFillNumber":
            case "AllowReadCall":
            case "AllowReadCallAndLog":
            case "AllowReadCallLog":
            case "AppLocked":
            case "AppUpdate":
            case "AppUpdateBeta":
            case "BotLocationPermissionRequestDeniedApp":
            case "BrowserExternalRestricted":
            case "BrowserSettingsCookiesInfo":
            case "BrowserSettingsCustomTabs":
            case "BrowserSettingsEnableInfo":
            case "BrowserSettingsNoCustomTabsInfo":
            case "CallViaTelegram":
            case "ChangePasscodeInfo":
            case "ClearTelegramCache":
            case "ClearingCacheDescription":
            case "CreateNewThemeAlert":
            case "CreatePasscodeInfoPIN":
            case "CreatePasscodeInfoPassword":
            case "EnterYourPasscodeInfo":
            case "EnterYourTelegramPasscode":
            case "Gift2ExportTONUpdateRequiredText":
            case "ImportImportingInfo":
            case "IncomingCallsSystemSettingDescription":
            case "LiveLocationPausedText":
            case "LocalDatabaseInfo":
            case "LocalDatabaseSize":
            case "LowDiskSpaceMessage":
            case "NoChats":
            case "NotificationsPermissionAlertSubtitle":
            case "OpenInTelegramBrowser":
            case "OptimizingTelegram":
            case "Page1Title":
            case "Page2Message":
            case "Page3Message":
            case "Page4Message":
            case "Page5Message":
            case "Page6Message":
            case "PermissionBackgroundLocation":
            case "PermissionDrawAboveOtherApps":
            case "PermissionDrawAboveOtherAppsGroupCall":
            case "PermissionFSILockscreen":
            case "PermissionNoAudioStorageStory":
            case "PermissionNoAudioVideoWithHint":
            case "PermissionNoAudioWithHint":
            case "PermissionNoBluetoothWithHint":
            case "PermissionNoCameraMicVideo":
            case "PermissionNoCameraWithHint":
            case "PermissionNoContactsSaving":
            case "PermissionNoContactsSharing":
            case "PermissionNoLocation":
            case "PermissionNoLocationFriends":
            case "PermissionNoLocationNavigation":
            case "PermissionNoLocationStory":
            case "PermissionNoStorageAvatar":
            case "PermissionStorageWithHint":
            case "PermissionXiaomiLockscreen":
            case "ProfilePopupNotificationInfo":
            case "QRCodePermissionNoCameraWithHint":
            case "RoundVideoCompositionInfo":
            case "ScreenCaptureAlert":
            case "SdCardAlert":
            case "SdCardErrorDescription":
            case "StorageUsageTelegram":
            case "StorageUsageTelegramLess":
            case "StoryUnsupported":
            case "TelegramCacheSize":
            case "TelegramVersion":
            case "UnlockToUse":
            case "UnsupportedBlockMessage":
            case "UnsupportedMedia2":
            case "UnsupportedMessageMessage":
            case "UpdateAppAlert":
            case "UpdateContactsMessage":
            case "UpdateTelegram":
            case "UseProxySponsorInfo":
            case "VideoCallViaTelegram":
            case "VoiceCallViaTelegram":
            case "VoipInCallBranding":
            case "VoipInCallBrandingWithName":
            case "VoipInConferenceCallBranding":
            case "VoipInVideoCallBranding":
            case "VoipInVideoCallBrandingWithName":
            case "VoipNeedCameraPermission":
            case "VoipNeedMicCameraPermissionWithHint":
            case "VoipNeedMicPermissionWithHint":
            case "VoipOutgoingCall":
            case "WalletAuthorizeTransactionPasscode":
                return NAME.matcher(value).replaceAll("Ayuless");
            case "NotificationHiddenChatName":
            case "NotificationHiddenName":
            case "SecretChatName":
                return NAME.matcher(value).replaceAll("Ayuless");
            default: return value;
        }
    }
}
