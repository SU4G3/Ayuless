// SPDX-License-Identifier: GPL-3.0-or-later
package org.ayuless.edits;

import android.text.TextUtils;
import org.ayuless.antidelete.AntiDelete;
import org.ayuless.appearance.DeletedStyle;
import org.ayuless.content.RetentionConfig;
import org.ayuless.extras.ExtrasConfig;
import org.ayuless.storage.AyulessStore;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;
import java.util.ArrayList;

public final class DeletedMessageActions {
    private DeletedMessageActions() {}

    public static void prepareReply(int account, SendMessagesHelper.SendMessageParams params) {
        if (params == null || params.retryMessageObject != null || !ExtrasConfig.get(ExtrasConfig.KEY_LOCAL_RESEND)
                || !DeletedStyle.isDeleted(account, params.replyToMsg)) return;
        TLRPC.Message source = params.replyToMsg.messageOwner;
        String text = source.message;
        boolean useText = !TextUtils.isEmpty(params.message);
        boolean caption = params.photo != null || params.document != null;
        if (!TextUtils.isEmpty(text) && (useText || caption)) {
            String excerpt = text.replaceAll("\\R", " ");
            int count = excerpt.codePointCount(0, excerpt.length());
            if (count > 20) excerpt = excerpt.substring(0, excerpt.offsetByCodePoints(0, 20)) + "…";
            String sender = "";
            if (params.peer < 0 && !DialogObject.isEncryptedDialog(params.peer)) {
                long from = source.from_id == null ? params.replyToMsg.getDialogId() : MessageObject.getPeerId(source.from_id);
                MessagesController controller = MessagesController.getInstance(account);
                if (from > 0) {
                    TLRPC.User user = controller.getUser(from);
                    if (user != null) sender = ContactsController.formatName(user.first_name, user.last_name);
                } else if (from < 0) {
                    TLRPC.Chat chat = controller.getChat(-from);
                    if (chat != null) sender = chat.title;
                }
            }
            String prefix = (TextUtils.isEmpty(sender) ? "" : sender + "\n") + "> " + excerpt + "\n\n";
            if (useText) params.message = prefix + params.message;
            else params.caption = prefix + (params.caption == null ? "" : params.caption);
            if (params.entities != null) {
                ArrayList<TLRPC.MessageEntity> shifted = new ArrayList<>();
                for (TLRPC.MessageEntity entity : params.entities) {
                    SerializedData output = new SerializedData();
                    try {
                        entity.serializeToStream(output);
                        SerializedData input = new SerializedData(output.toByteArray());
                        try {
                            TLRPC.MessageEntity copy = TLRPC.MessageEntity.TLdeserialize(input, input.readInt32(true), true);
                            if (copy == null) throw new IllegalStateException("Missing entity copy");
                            copy.offset += prefix.length();
                            shifted.add(copy);
                        } finally {
                            input.cleanup();
                        }
                    } catch (Exception e) {
                        FileLog.e(e);
                    } finally {
                        output.cleanup();
                    }
                }
                params.entities = shifted;
            }
        }
        if (params.replyToTopMsg != null && params.replyToTopMsg.getId() == params.replyToMsg.getId()
                && params.replyToTopMsg.getDialogId() == params.replyToMsg.getDialogId()) params.replyToTopMsg = null;
        params.replyToMsg = null;
        params.replyQuote = null;
    }

    public static boolean canKeep(int account, MessageObject message, boolean scheduled) {
        if (scheduled || message == null || message.messageOwner == null || message.getId() <= 0
                || message.scheduled || !ExtrasConfig.get(ExtrasConfig.KEY_KEEP_OWN_DELETES)
                || DialogObject.isEncryptedDialog(message.getDialogId()) || DeletedStyle.isDeleted(account, message)
                || !RetentionConfig.storable(account, message.messageOwner, RetentionConfig.DELETE)) return false;
        TLRPC.Chat chat = message.getDialogId() < 0 ? MessagesController.getInstance(account).getChat(-message.getDialogId()) : null;
        return message.canDeleteMessage(false, chat);
    }

    public static void keepLocally(BaseFragment fragment, int account, MessageObject message, boolean scheduled) {
        if (fragment == null || fragment.isFinished || fragment.getParentActivity() == null || !canKeep(account, message, scheduled)) return;
        fragment.showDialog(new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider())
                .setTitle(LocaleController.getString(R.string.AyulessKeepLocally))
                .setMessage(LocaleController.getString(R.string.AyulessKeepLocallyConfirm))
                .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                .setPositiveButton(LocaleController.getString(R.string.Delete), (dialog, which) -> {
                    if (!canKeep(account, message, scheduled)) return;
                    MessagesController controller = MessagesController.getInstance(account);
                    long channelId = message.messageOwner.peer_id == null ? 0 : message.messageOwner.peer_id.channel_id;
                    TLObject request;
                    if (channelId != 0) {
                        TLRPC.TL_channels_deleteMessages deletion = new TLRPC.TL_channels_deleteMessages();
                        deletion.channel = controller.getInputChannel(channelId);
                        deletion.id.add(message.getId());
                        request = deletion;
                    } else {
                        TLRPC.TL_messages_deleteMessages deletion = new TLRPC.TL_messages_deleteMessages();
                        deletion.revoke = true;
                        deletion.id.add(message.getId());
                        request = deletion;
                    }
                    ConnectionsManager.getInstance(account).sendRequest(request, (response, error) -> {
                        if (error != null || response == null) {
                            AndroidUtilities.runOnUIThread(() -> showFailure(fragment));
                            return;
                        }
                        MessagesStorage.getInstance(account).getStorageQueue().postRunnable(() -> {
                            AntiDelete.keepLoaded(account, message.messageOwner);
                            Utilities.stageQueue.postRunnable(() -> {
                                if (response instanceof TLRPC.TL_messages_affectedMessages) {
                                    TLRPC.TL_messages_affectedMessages affected = (TLRPC.TL_messages_affectedMessages) response;
                                    TLRPC.TL_updates updates = new TLRPC.TL_updates();
                                    if (channelId != 0) {
                                        org.telegram.tgnet.tl.TL_update.TL_updateDeleteChannelMessages update = new org.telegram.tgnet.tl.TL_update.TL_updateDeleteChannelMessages();
                                        update.channel_id = channelId;
                                        update.messages.add(message.getId());
                                        update.pts = affected.pts;
                                        update.pts_count = affected.pts_count;
                                        updates.updates.add(update);
                                    } else {
                                        org.telegram.tgnet.tl.TL_update.TL_updateDeleteMessages update = new org.telegram.tgnet.tl.TL_update.TL_updateDeleteMessages();
                                        update.messages.add(message.getId());
                                        update.pts = affected.pts;
                                        update.pts_count = affected.pts_count;
                                        updates.updates.add(update);
                                    }
                                    controller.processUpdates(updates, false);
                                } else if (response instanceof TLRPC.Updates) {
                                    controller.processUpdates((TLRPC.Updates) response, false);
                                }
                            });
                        });
                    });
                }).create());
    }

    public static void showDeleteDate(BaseFragment fragment, int account, MessageObject message) {
        if (fragment == null || message == null || !ExtrasConfig.get(ExtrasConfig.KEY_DELETE_DATE)
                || !DeletedStyle.isDeleted(account, message)) return;
        AyulessStore.getInstance(account).getDeleteDate(message.getDialogId(), message.getId(), timestamp -> {
            if (fragment.isFinished || fragment.getParentActivity() == null) return;
            fragment.showDialog(new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider())
                    .setTitle(LocaleController.getString(R.string.AyulessDeleteDate))
                    .setMessage(timestamp == 0 ? LocaleController.getString(R.string.AyulessUnknown)
                            : LocaleController.formatDateAudio(timestamp, true))
                    .setPositiveButton(LocaleController.getString(R.string.OK), null).create());
        });
    }

    public static void showFailure(BaseFragment fragment) {
        if (!fragment.isFinished && fragment.getParentActivity() != null) {
            BulletinFactory.of(fragment).createErrorBulletin(LocaleController.getString(R.string.UnknownError)).show();
        }
    }
}
