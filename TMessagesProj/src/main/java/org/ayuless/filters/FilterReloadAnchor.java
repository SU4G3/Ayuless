/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.filters;

import android.view.View;
import android.view.ViewGroup;

import org.telegram.messenger.MessageObject;
import org.telegram.ui.Cells.ChatActionCell;
import org.telegram.ui.Cells.ChatMessageCell;

import java.util.function.ToIntFunction;

/** Captures a visible history position without touching storage. */
public final class FilterReloadAnchor {
    public final int messageId;
    public final int offset;
    public final long dialogId;

    private FilterReloadAnchor(MessageObject message, int offset) {
        this.messageId = message.getId();
        this.offset = offset;
        this.dialogId = message.getDialogId();
    }

    public static FilterReloadAnchor capture(int account, ViewGroup list, ToIntFunction<View> getOffset) {
        FilterReloadAnchor visible = null;
        FilterReloadAnchor fallback = null;
        for (int i = 0; i < list.getChildCount(); i++) {
            View child = list.getChildAt(i);
            MessageObject message = child instanceof ChatMessageCell ? ((ChatMessageCell) child).getMessageObject()
                    : child instanceof ChatActionCell ? ((ChatActionCell) child).getMessageObject() : null;
            if (message == null || message.getId() <= 0 || message.isSponsored()) {
                continue;
            }
            int offset = getOffset.applyAsInt(child);
            if (fallback == null || Math.abs(offset) < Math.abs(fallback.offset)) {
                fallback = new FilterReloadAnchor(message, offset);
            }
            if (!MessageFilters.isHiddenSafe(account, message)
                    && (visible == null || Math.abs(offset) < Math.abs(visible.offset))) {
                visible = new FilterReloadAnchor(message, offset);
            }
        }
        // Even if every visible message becomes hidden, reload around the same history range.
        return visible != null ? visible : fallback;
    }
}
