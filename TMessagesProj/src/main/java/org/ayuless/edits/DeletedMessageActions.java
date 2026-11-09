/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package org.ayuless.edits;

import android.text.TextUtils;
import org.ayuless.antidelete.AntiDelete;
import org.ayuless.antidelete.DeletedCache;
import org.ayuless.extras.ExtrasConfig;
import org.ayuless.storage.AyulessStore;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;
import java.util.ArrayList;
import static org.telegram.messenger.LocaleController.getString;

/** AyuGram's deleted-reply fallback and keep-locally action, without global mutable bypass flags. */
public final class DeletedMessageActions {
    private DeletedMessageActions() {}
    public static void prepareReply(int account, SendMessagesHelper.SendMessageParams params) {
        MessageObject reply = params.replyToMsg;
        if (!ExtrasConfig.get(ExtrasConfig.KEY_LOCAL_RESEND) || reply == null || reply.messageOwner == null
                || !DeletedCache.isDeleted(account, reply.getDialogId(), reply.getId()) || params.retryMessageObject != null) return;
        CharSequence text = reply.messageText;
        if (!TextUtils.isEmpty(text)) {
            String name = "";
            if (!DialogObject.isUserDialog(params.peer)) {
                TLObject sender = reply.getFromPeerObject();
                if (sender instanceof TLRPC.User) name = ContactsController.formatName(((TLRPC.User) sender).first_name, ((TLRPC.User) sender).last_name);
                else if (sender instanceof TLRPC.Chat) name = ((TLRPC.Chat) sender).title;
                if (!TextUtils.isEmpty(name)) name += "\n";
            }
            String snippet = text.toString().replace('\n', ' ');
            if (snippet.codePointCount(0, snippet.length()) > 20) snippet = snippet.substring(0, snippet.offsetByCodePoints(0, Math.min(20, snippet.codePointCount(0, snippet.length())))) + "…";
            String prefix = name + "> " + snippet + "\n\n";
            boolean message = params.message != null;
            if (message) params.message = prefix + params.message;
            else if (params.caption != null || params.photo != null || params.document != null) params.caption = prefix + (params.caption == null ? "" : params.caption);
            ArrayList<TLRPC.MessageEntity> shifted = new ArrayList<>();
            if (params.entities != null) for (TLRPC.MessageEntity entity : params.entities) {
                NativeByteBuffer buffer = null;
                try {
                    buffer = new NativeByteBuffer(entity.getObjectSize());
                    entity.serializeToStream(buffer); buffer.rewind();
                    TLRPC.MessageEntity copy = TLRPC.MessageEntity.TLdeserialize(buffer, buffer.readInt32(true), true);
                    copy.offset += prefix.length();
                    shifted.add(copy);
                } catch (Exception e) { org.telegram.messenger.FileLog.e(e); }
                finally { if (buffer != null) buffer.reuse(); }
            }
            params.entities = shifted;
        }
        params.replyToMsg = null;
        params.replyQuote = null;
    }
    public static void keepAndDelete(BaseFragment fragment, MessageObject object) {
        if (fragment.getParentActivity() == null || object == null || !ExtrasConfig.get(ExtrasConfig.KEY_KEEP_LOCAL)
                || !AyulessStore.isStorable(object.currentAccount, object.messageOwner, org.ayuless.content.RetentionConfig.DELETE)
                || DialogObject.isEncryptedDialog(object.getDialogId())) return;
        fragment.showDialog(new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider())
                .setTitle(getString(R.string.AyulessKeepLocally)).setMessage(getString(R.string.AyulessKeepLocallyConfirm))
                .setNegativeButton(getString(R.string.Cancel), null)
                .setPositiveButton(getString(R.string.Delete), (dialog, which) -> delete(fragment, object)).create());
    }
    private static void delete(BaseFragment fragment, MessageObject object) {
        int account = object.currentAccount;
        MessagesController controller = MessagesController.getInstance(account);
        long channelId = object.messageOwner.peer_id.channel_id;
        TLObject request;
        if (channelId != 0) {
            TLRPC.TL_channels_deleteMessages req = new TLRPC.TL_channels_deleteMessages();
            req.channel = controller.getInputChannel(channelId);
            req.id.add(object.getId()); request = req;
        } else {
            TLRPC.TL_messages_deleteMessages req = new TLRPC.TL_messages_deleteMessages();
            req.revoke = true; req.id.add(object.getId()); request = req;
        }
        ConnectionsManager.getInstance(account).sendRequest(request, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (error != null) {
                if (!fragment.isFinished) BulletinFactory.of(fragment).createErrorBulletin(getString(R.string.ErrorOccurred)).show();
                return;
            }
            AntiDelete.keepLoaded(account, object.messageOwner);
            // Advance pts through the normal update pipeline, which already preserves this message.
            if (response instanceof TLRPC.TL_messages_affectedMessages) {
                TLRPC.TL_messages_affectedMessages affected = (TLRPC.TL_messages_affectedMessages) response;
                TLRPC.TL_updates updates = new TLRPC.TL_updates();
                updates.date = ConnectionsManager.getInstance(account).getCurrentTime();
                if (channelId != 0) {
                    TLRPC.TL_updateDeleteChannelMessages update = new TLRPC.TL_updateDeleteChannelMessages();
                    update.channel_id = channelId; update.messages.add(object.getId());
                    update.pts = affected.pts; update.pts_count = affected.pts_count; updates.updates.add(update);
                } else {
                    TLRPC.TL_updateDeleteMessages update = new TLRPC.TL_updateDeleteMessages();
                    update.messages.add(object.getId()); update.pts = affected.pts; update.pts_count = affected.pts_count; updates.updates.add(update);
                }
                controller.processUpdates(updates, false);
            }
        }));
    }
    public static void showDeleteDate(BaseFragment fragment, MessageObject message) {
        AyulessStore.getInstance(message.currentAccount).getDeleteDate(message.getDialogId(), message.getId(), date -> {
            if (fragment.isFinished || fragment.getParentActivity() == null) return;
            fragment.showDialog(new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider())
                    .setTitle(getString(R.string.AyulessDeleteDate))
                    .setMessage(date == 0 ? getString(R.string.AyulessUnknown) : LocaleController.formatDateAudio(date, true))
                    .setPositiveButton(getString(R.string.OK), null).create());
        });
    }
}
