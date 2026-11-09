// SPDX-License-Identifier: GPL-3.0-or-later
package org.ayuless.branding;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/** Installed-app identity only. Never process user text or formatted arguments. */
public final class AppBranding {
    private static final Pattern NAME = Pattern.compile(
            "(?iu)Telegram|Телеграм(?:м)?(?:а|у|ом|е)?|تلگرام|تيليجرام|تيليغرام|تليجرام|텔레그램|テレグラム");
    private static final Set<Integer> BRANDED_KEYS = brandedKeys();

    private AppBranding() {}

    private static Set<Integer> brandedKeys() {
        Set<Integer> hashes = new HashSet<>();
        // Use the same key hashes as localization_*.bin. R.string IDs can be binary-only.
        for (String key : new String[] {
                "AgeVerificationNeedCameraPermission",
                "AllowBackgroundActivityInfo",
                "AllowBackgroundActivityInfoOneUIAboveS",
                "AllowBackgroundActivityInfoOneUIBelowS",
                "AllowFillNumber",
                "AllowReadCall",
                "AllowReadCallAndLog",
                "AllowReadCallLog",
                "AppLocked",
                "AppUpdate",
                "AppUpdateBeta",
                "BotLocationPermissionRequestDeniedApp",
                "BrowserExternalRestricted",
                "BrowserSettingsCookiesInfo",
                "BrowserSettingsCustomTabs",
                "BrowserSettingsEnableInfo",
                "BrowserSettingsNoCustomTabsInfo",
                "CallViaTelegram",
                "ChangePasscodeInfo",
                "ClearTelegramCache",
                "ClearingCacheDescription",
                "CreateNewThemeAlert",
                "CreatePasscodeInfoPIN",
                "CreatePasscodeInfoPassword",
                "EnterYourPasscodeInfo",
                "EnterYourTelegramPasscode",
                "Gift2ExportTONUpdateRequiredText",
                "ImportImportingInfo",
                "IncomingCallsSystemSettingDescription",
                "LiveLocationPausedText",
                "LocalDatabaseInfo",
                "LocalDatabaseSize",
                "LowDiskSpaceMessage",
                "NoChats",
                "NotificationsPermissionAlertSubtitle",
                "OpenInTelegramBrowser",
                "OptimizingTelegram",
                "Page1Title",
                "Page2Message",
                "Page3Message",
                "Page4Message",
                "Page5Message",
                "Page6Message",
                "PermissionBackgroundLocation",
                "PermissionDrawAboveOtherApps",
                "PermissionDrawAboveOtherAppsGroupCall",
                "PermissionFSILockscreen",
                "PermissionNoAudioStorageStory",
                "PermissionNoAudioVideoWithHint",
                "PermissionNoAudioWithHint",
                "PermissionNoBluetoothWithHint",
                "PermissionNoCameraMicVideo",
                "PermissionNoCameraWithHint",
                "PermissionNoContactsSaving",
                "PermissionNoContactsSharing",
                "PermissionNoLocation",
                "PermissionNoLocationFriends",
                "PermissionNoLocationNavigation",
                "PermissionNoLocationStory",
                "PermissionNoStorageAvatar",
                "PermissionStorageWithHint",
                "PermissionXiaomiLockscreen",
                "ProfilePopupNotificationInfo",
                "QRCodePermissionNoCameraWithHint",
                "RoundVideoCompositionInfo",
                "ScreenCaptureAlert",
                "SdCardAlert",
                "SdCardErrorDescription",
                "StorageUsageTelegram",
                "StorageUsageTelegramLess",
                "StoryUnsupported",
                "TelegramCacheSize",
                "TelegramVersion",
                "UnlockToUse",
                "UnsupportedBlockMessage",
                "UnsupportedMedia2",
                "UnsupportedMessageMessage",
                "UpdateAppAlert",
                "UpdateContactsMessage",
                "UpdateTelegram",
                "UseProxySponsorInfo",
                "VideoCallViaTelegram",
                "VoiceCallViaTelegram",
                "VoipInCallBranding",
                "VoipInCallBrandingWithName",
                "VoipInConferenceCallBranding",
                "VoipInVideoCallBranding",
                "VoipInVideoCallBrandingWithName",
                "VoipNeedCameraPermission",
                "VoipNeedMicCameraPermissionWithHint",
                "VoipNeedMicPermissionWithHint",
                "VoipOutgoingCall",
                "WalletAuthorizeTransactionPasscode",
                "NotificationHiddenChatName",
                "NotificationHiddenName",
                "SecretChatName",
        }) {
            hashes.add(key.hashCode());
        }
        return hashes;
    }

    /** Runs after local/cloud lookup, before formatting. Unknown keys pass through. */
    public static String localize(int keyHash, String value) {
        if (value == null) return null;
        if (keyHash == "AppName".hashCode()) return "Ayuless";
        if (keyHash == "AppNameBeta".hashCode()) return "Ayuless Beta";
        // The second mention names Telegram's cloud servers, not this app.
        if (keyHash == "ContactsPermissionAlert".hashCode()) {
            return value.contains("Ayuless") ? value : NAME.matcher(value).replaceFirst("Ayuless");
        }
        return BRANDED_KEYS.contains(keyHash) ? NAME.matcher(value).replaceAll("Ayuless") : value;
    }
}
