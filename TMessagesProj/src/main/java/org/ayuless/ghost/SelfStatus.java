// SPDX-License-Identifier: GPL-3.0-or-later
package org.ayuless.ghost;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.Vector;

/** Server-only self status, scoped to a visible profile and never merged into the user cache. */
public final class SelfStatus {
    private final int account;
    private final Runnable changed;
    private TLRPC.UserStatus status;
    private int generation;
    private int requestId;
    private boolean active;
    private final Runnable poll = this::load;

    public SelfStatus(int account, Runnable changed) {
        this.account = account;
        this.changed = changed;
    }

    public void start() {
        stop();
        active = true;
        load();
    }

    public void stop() {
        active = false;
        generation++;
        AndroidUtilities.cancelRunOnUIThread(poll);
        if (requestId != 0) ConnectionsManager.getInstance(account).cancelRequest(requestId, true);
        requestId = 0;
        status = null;
    }

    private void load() {
        if (!active) return;
        if (!GhostConfig.hideOnline(account)) {
            status = null;
            changed.run();
            AndroidUtilities.runOnUIThread(poll, 30000);
            return;
        }
        final int token = generation;
        TLRPC.TL_users_getUsers request = new TLRPC.TL_users_getUsers();
        request.id.add(new TLRPC.TL_inputUserSelf());
        requestId = ConnectionsManager.getInstance(account).sendRequest(request, (response, error) -> {
            // Read the response itself: the cached self user is overwritten by local online state.
            TLRPC.UserStatus received = null;
            if (error == null && response instanceof Vector) {
                for (Object entry : ((Vector<?>) response).objects) {
                    if (entry instanceof TLRPC.User && ((TLRPC.User) entry).id == UserConfig.getInstance(account).getClientUserId()) {
                        received = ((TLRPC.User) entry).status;
                        break;
                    }
                }
            }
            final TLRPC.UserStatus result = received;
            AndroidUtilities.runOnUIThread(() -> {
                if (!active || token != generation) return;
                requestId = 0;
                status = result;
                changed.run();
                AndroidUtilities.runOnUIThread(poll, 30000);
            });
        });
    }

    public String text() {
        String value;
        if (status == null || status instanceof TLRPC.TL_userStatusEmpty) {
            return LocaleController.getString(R.string.AyulessGhostSelfOfflineUnknown);
        } else if (status instanceof TLRPC.TL_userStatusRecently) {
            value = LocaleController.getString(R.string.Lately);
        } else if (status instanceof TLRPC.TL_userStatusLastWeek) {
            value = LocaleController.getString(R.string.WithinAWeek);
        } else if (status instanceof TLRPC.TL_userStatusLastMonth) {
            value = LocaleController.getString(R.string.WithinAMonth);
        } else if (status.expires > ConnectionsManager.getInstance(account).getCurrentTime()) {
            value = LocaleController.getString(R.string.Online);
        } else if (status.expires > 0) {
            value = LocaleController.formatDateOnline(status.expires, null);
        } else {
            return LocaleController.getString(R.string.AyulessGhostSelfOfflineUnknown);
        }
        return LocaleController.formatString(R.string.AyulessGhostSelfStatus, value);
    }
}
