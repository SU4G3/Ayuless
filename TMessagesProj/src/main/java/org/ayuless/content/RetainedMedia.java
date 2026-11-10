// SPDX-License-Identifier: GPL-3.0-or-later
package org.ayuless.content;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.ColoredImageSpan;

/** Presentation only: retain TTL metadata and the original viewer/read receipt path. */
public final class RetainedMedia {
    private RetainedMedia() {}

    public static int ttl(MessageObject object) {
        if (object == null || object.messageOwner == null) return 0;
        TLRPC.Message message = object.messageOwner;
        if (!(MessageObject.getMedia(message) instanceof TLRPC.TL_messageMediaPhoto)
                && !object.isVideo() && !object.isGif() && !object.isRoundVideo()) return 0;
        int seconds = message.media == null ? 0 : message.media.ttl_seconds;
        if (seconds == 0 && object.isSecret()) seconds = message.ttl;
        return seconds;
    }

    public static boolean showPreview(MessageObject object) {
        return ttl(object) > 0 && !object.hasExtendedMediaPreview()
                && RetentionConfig.keepMedia(object.currentAccount, object.getDialogId());
    }

    public static boolean blurredPreview(MessageObject object) {
        return object != null && object.needDrawBluredPreview() && !showPreview(object);
    }

    public static CharSequence decorateTime(MessageObject object, CharSequence time) {
        if (!showPreview(object)) return time;
        int seconds = ttl(object);
        boolean viewOnce = seconds == Integer.MAX_VALUE;
        SpannableString badge = new SpannableString("◉ " + (viewOnce
                ? LocaleController.getString(R.string.AyulessMediaViewOnce) : seconds + "s") + " | ");
        ColoredImageSpan icon = new ColoredImageSpan(viewOnce
                ? R.drawable.ayuless_solar_eye : R.drawable.ayuless_solar_clock);
        icon.setRelativeSize(Theme.chat_timePaint.getFontMetricsInt());
        badge.setSpan(icon, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return TextUtils.concat(badge, time);
    }
}
