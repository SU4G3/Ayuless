/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 Ayuless contributors
 */
package org.ayuless.ghost;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import androidx.core.util.Consumer;

/** Local receipts only. Never sends a read acknowledgement or changes the ghost policy. */
public final class GhostSeen {
    public static final int MENTIONS = 0, REACTIONS = 1, POLL_VOTES = 2;
    private static final class LocalCount {
        final int account, generation;
        LocalCount(int account, int generation) {
            this.account = account;
            this.generation = generation;
        }
    }
    private static final GhostSeen[] instances = new GhostSeen[UserConfig.MAX_ACCOUNT_COUNT];
    private final int account;
    private final SharedPreferences prefs;
    // UI-thread request state; message watermarks themselves use thread-safe SharedPreferences.
    private final HashMap<String, Integer> loading = new HashMap<>();
    private final HashMap<String, Long> refreshed = new HashMap<>();
    private final HashMap<String, Integer> outputs = new HashMap<>();
    private final HashMap<String, ArrayList<Consumer<Integer>>> nextCallbacks = new HashMap<>();
    private int generation;
    private boolean publishing;

    private GhostSeen(int account) {
        this.account = account;
        prefs = ApplicationLoader.applicationContext.getSharedPreferences("ayuless_seen_" + account, Context.MODE_PRIVATE);
    }

    private static synchronized GhostSeen get(int account) {
        if (instances[account] == null) instances[account] = new GhostSeen(account);
        return instances[account];
    }

    private static boolean enabled(int account) {
        return account >= 0 && account < instances.length && GhostConfig.hideRead(account)
                && GhostConfig.getProfile(GhostConfig.activeProfile(account), GhostConfig.KEY_LOCAL_SEEN);
    }

    private static String key(long dialog, long topic, int kind) {
        return dialog + ":" + topic + ":" + kind;
    }

    private String messageKey(long dialog, int kind, int id) {
        return "message:" + dialog + ":" + kind + ":" + id;
    }

    private static String reactionToken(TLRPC.MessagePeerReaction reaction) {
        SerializedData data = new SerializedData();
        reaction.reaction.serializeToStream(data);
        String token = DialogObject.getPeerDialogId(reaction.peer_id) + ":" + reaction.date + ":"
                + Base64.encodeToString(data.toByteArray(), Base64.NO_WRAP);
        data.cleanup();
        return token;
    }

    private static String pollToken(TLRPC.PollResults results) {
        StringBuilder token = new StringBuilder().append(results.total_voters);
        for (TLRPC.Peer peer : results.recent_voters) token.append(':').append(DialogObject.getPeerDialogId(peer));
        for (TLRPC.PollAnswerVoters answer : results.results) {
            token.append(':').append(Base64.encodeToString(answer.option, Base64.NO_WRAP)).append('=').append(answer.voters);
        }
        return token.toString();
    }

    public static void applyReactions(int account, long dialog, int id, TLRPC.TL_messageReactions reactions) {
        if (!enabled(account) || reactions == null) return;
        GhostSeen store = get(account);
        Set<String> seen = store.prefs.getStringSet(store.messageKey(dialog, REACTIONS, id), java.util.Collections.emptySet());
        if (seen.isEmpty()) return;
        for (TLRPC.MessagePeerReaction reaction : reactions.recent_reactions) {
            if (reaction.unread && seen.contains(reactionToken(reaction))) reaction.unread = false;
        }
    }

    public static void applyPoll(int account, long dialog, int id, TLRPC.PollResults results) {
        if (!enabled(account) || results == null || !results.has_unread_votes) return;
        GhostSeen store = get(account);
        if (store.prefs.getStringSet(store.messageKey(dialog, POLL_VOTES, id), java.util.Collections.emptySet())
                .contains(pollToken(results))) results.has_unread_votes = false;
    }

    public static void applyMessage(int account, TLRPC.Message message) {
        if (!enabled(account) || message == null || message.id <= 0) return;
        long dialog = MessageObject.getDialogId(message);
        GhostSeen store = get(account);
        if (message.mentioned && store.prefs.contains(store.messageKey(dialog, MENTIONS, message.id))) {
            message.media_unread = false;
        }
        applyReactions(account, dialog, message.id, message.reactions);
        if (message.media instanceof TLRPC.TL_messageMediaPoll) {
            applyPoll(account, dialog, message.id, ((TLRPC.TL_messageMediaPoll) message.media).results);
        }
    }

    public static void rememberContent(int account, MessageObject message) {
        if (!enabled(account) || message == null || !message.messageOwner.mentioned) return;
        TLRPC.Dialog dialog = MessagesController.getInstance(account).dialogs_dict.get(message.getDialogId());
        remember(account, message, message.getTopicId(), MENTIONS, dialog == null ? 0 : dialog.unread_mentions_count - 1);
    }

    /** Called only when the corresponding marker was actually displayed, before upstream clears it. */
    public static void remember(int account, MessageObject message, long topic, int kind, int remaining) {
        if (!enabled(account) || message == null || message.getId() <= 0) return;
        if (kind == MENTIONS && !message.messageOwner.media_unread) return;
        GhostSeen store = get(account);
        String mk = store.messageKey(message.getDialogId(), kind, message.getId());
        Set<String> tokens = new HashSet<>(store.prefs.getStringSet(mk, java.util.Collections.emptySet()));
        if (kind == REACTIONS && message.messageOwner.reactions != null) {
            for (TLRPC.MessagePeerReaction reaction : message.messageOwner.reactions.recent_reactions) {
                if (reaction.unread) tokens.add(reactionToken(reaction));
            }
        } else if (kind == POLL_VOTES && message.messageOwner.media instanceof TLRPC.TL_messageMediaPoll) {
            tokens.add(pollToken(((TLRPC.TL_messageMediaPoll) message.messageOwner.media).results));
        } else if (kind == MENTIONS) {
            tokens.add("seen");
        }
        if (tokens.isEmpty() || tokens.equals(store.prefs.getStringSet(mk, java.util.Collections.emptySet()))) return;
        String scope = key(message.getDialogId(), topic, kind);
        SharedPreferences.Editor edit = store.prefs.edit().putStringSet(mk, tokens)
                .putBoolean("dialog:" + message.getDialogId(), true)
                .putBoolean("kind:" + message.getDialogId() + ":" + kind, true)
                .putBoolean("scope:" + scope, true).putInt("left:" + scope, Math.max(0, remaining));
        store.addHidden(edit, scope, message.getId());
        edit.apply();
        store.outputs.put(scope, Math.max(0, remaining));
        store.generation++;
        store.refreshed.remove(scope);
        if (topic != 0) {
            // The parent dialog also includes markers viewed inside a topic.
            String parent = key(message.getDialogId(), 0, kind);
            TLRPC.Dialog dialog = MessagesController.getInstance(account).dialogs_dict.get(message.getDialogId());
            int raw = dialog == null ? 0 : kind == MENTIONS ? dialog.unread_mentions_count
                    : kind == REACTIONS ? dialog.unread_reactions_count : dialog.unread_poll_votes_count;
            SharedPreferences.Editor parentEdit = store.prefs.edit().putBoolean("scope:" + parent, true)
                    .putInt("left:" + parent, Math.max(0, raw - 1));
            store.addHidden(parentEdit, parent, message.getId());
            parentEdit.apply();
            store.outputs.put(parent, Math.max(0, raw - 1));
            store.refreshed.remove(parent);
        }
    }

    private void addHidden(SharedPreferences.Editor edit, String scope, int messageId) {
        Set<String> ids = new HashSet<>(prefs.getStringSet("hidden_ids:" + scope, java.util.Collections.emptySet()));
        ids.add(Integer.toString(messageId));
        // A later reaction to an already server-unread message is still only one badge entry.
        edit.putStringSet("hidden_ids:" + scope, ids).putInt("hidden:" + scope, ids.size());
    }

    /** Counters persisted by upstream may already be reduced; do not subtract twice. */
    private int count(long dialog, long topic, int kind, int raw) {
        String scope = key(dialog, topic, kind);
        if (!prefs.getBoolean("scope:" + scope, false)) return raw;
        int left = prefs.getInt("left:" + scope, raw);
        Integer previous = outputs.get(scope);
        if (previous != null && previous == raw) return raw;
        refreshed.remove(scope); // A changed server/cache counter needs a fresh intersection.
        int result = raw == left ? raw : Math.max(0, raw - prefs.getInt("hidden:" + scope, 0));
        outputs.put(scope, result);
        return result;
    }

    public static int countForChat(int account, long dialog, long topic, int kind, int raw) {
        if (!enabled(account)) return raw;
        GhostSeen store = get(account);
        int result = store.count(dialog, topic, kind, raw);
        store.refresh(dialog, topic, kind);
        return result;
    }

    public static boolean isLocalCount(Object[] args) {
        // NotificationCenter can postpone delivery until after an animation finishes.
        return args.length > 4 && args[4] instanceof LocalCount;
    }

    /** Jump over server-unread markers already seen here, including reactions on old messages. */
    public static boolean findNext(int account, long dialog, long topic, int kind, Consumer<Integer> callback) {
        if (!enabled(account)) return false;
        GhostSeen store = get(account);
        if (!store.prefs.getBoolean("kind:" + dialog + ":" + kind, false)) return false;
        String scope = key(dialog, topic, kind);
        store.nextCallbacks.computeIfAbsent(scope, k -> new ArrayList<>()).add(callback);
        store.refreshed.remove(scope);
        store.refresh(dialog, topic, kind);
        return true;
    }

    private void applyDialog(TLRPC.Dialog dialog) {
        if (!prefs.getBoolean("dialog:" + dialog.id, false)) return;
        dialog.unread_mentions_count = count(dialog.id, 0, MENTIONS, dialog.unread_mentions_count);
        dialog.unread_reactions_count = count(dialog.id, 0, REACTIONS, dialog.unread_reactions_count);
        dialog.unread_poll_votes_count = count(dialog.id, 0, POLL_VOTES, dialog.unread_poll_votes_count);
        for (int kind = 0; kind < 3; kind++) refresh(dialog.id, 0, kind);
    }

    public static void applyDialogs(int account, MessagesController controller) {
        if (!enabled(account)) return;
        GhostSeen store = get(account);
        if (store.publishing) return;
        for (int i = 0; i < controller.dialogs_dict.size(); i++) store.applyDialog(controller.dialogs_dict.valueAt(i));
    }

    /** Runs before UI observers so cache/server counters and chat jump buttons use the same overlay. */
    public static boolean beforeNotification(int account, int id, Object[] args) {
        if (!enabled(account)) return !isLocalCount(args);
        if (id != NotificationCenter.dialogsNeedReload && id != NotificationCenter.messagesDidLoad
                && id != NotificationCenter.updateMentionsCount && id != NotificationCenter.dialogsUnreadReactionsCounterChanged
                && id != NotificationCenter.dialogsUnreadPollVotesCounterChanged) return true;
        GhostSeen store = get(account);
        if (isLocalCount(args)) {
            LocalCount snapshot = (LocalCount) args[4];
            return snapshot.account == account && snapshot.generation == store.generation;
        }
        if (store.publishing) return true;
        MessagesController controller = MessagesController.getInstance(account);
        if (id == NotificationCenter.dialogsNeedReload) {
            applyDialogs(account, controller);
        } else if (id == NotificationCenter.messagesDidLoad) {
            long dialog = (long) args[0];
            TLRPC.Dialog d = controller.dialogs_dict.get(dialog);
            if (d != null) store.applyDialog(d);
        } else {
            long dialog = (long) args[0], topic = (long) args[1];
            int kind = id == NotificationCenter.updateMentionsCount ? MENTIONS
                    : id == NotificationCenter.dialogsUnreadReactionsCounterChanged ? REACTIONS : POLL_VOTES;
            args[2] = store.count(dialog, topic, kind, (int) args[2]);
            store.refresh(dialog, topic, kind);
        }
        return true;
    }

    private void refresh(long dialog, long topic, int kind) {
        String scope = key(dialog, topic, kind);
        if (!prefs.getBoolean("kind:" + dialog + ":" + kind, false) || loading.containsKey(scope)
                || DialogObject.isEncryptedDialog(dialog)) return;
        long now = android.os.SystemClock.elapsedRealtime();
        Long last = refreshed.get(scope);
        if (last != null && now - last < 30_000) return;
        loading.put(scope, generation);
        refreshed.put(scope, now);
        loadPage(dialog, topic, kind, 0, 0, 0, new HashSet<>(), generation);
    }

    /** Enumerate unread markers with read-only getUnread* RPCs: old reactions can be on any message ID. */
    private void loadPage(long dialog, long topic, int kind, int offset, int unseen, int firstUnseen, Set<String> hidden, int version) {
        MessagesController controller = MessagesController.getInstance(account);
        TLObject request;
        if (kind == MENTIONS) {
            TLRPC.TL_messages_getUnreadMentions req = new TLRPC.TL_messages_getUnreadMentions();
            req.peer = controller.getInputPeer(dialog); req.offset_id = offset; req.limit = 100;
            if (topic != 0) { req.flags |= 1; req.top_msg_id = (int) topic; }
            request = req;
        } else if (kind == REACTIONS) {
            TLRPC.TL_messages_getUnreadReactions req = new TLRPC.TL_messages_getUnreadReactions();
            req.peer = controller.getInputPeer(dialog); req.offset_id = offset; req.limit = 100;
            if (topic != 0) {
                if (controller.isMonoForum(dialog)) { req.flags |= 2; req.saved_peer_id = controller.getInputPeer(topic); }
                else { req.flags |= 1; req.top_msg_id = (int) topic; }
            }
            request = req;
        } else {
            TLRPC.TL_messages_getUnreadPollVotes req = new TLRPC.TL_messages_getUnreadPollVotes();
            req.peer = controller.getInputPeer(dialog); req.offset_id = offset; req.limit = 100;
            if (topic != 0) { req.flags |= 1; req.top_msg_id = (int) topic; }
            request = req;
        }
        ConnectionsManager.getInstance(account).sendRequest(request, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            String scope = key(dialog, topic, kind);
            Integer pendingVersion = loading.get(scope);
            if (pendingVersion == null || pendingVersion != version) return; // A previous login must not retire a new query.
            if (!enabled(account) || version != generation || !(response instanceof TLRPC.messages_Messages)) {
                loading.remove(scope);
                refreshed.remove(scope);
                if (enabled(account) && version != generation) refresh(dialog, topic, kind);
                else nextCallbacks.remove(scope); // A failed query must not clear a marker or issue a receipt.
                return;
            }
            TLRPC.messages_Messages messages = (TLRPC.messages_Messages) response;
            int next = 0, left = unseen, first = firstUnseen;
            for (TLRPC.Message message : messages.messages) {
                if (message.id <= 0) continue;
                next = next == 0 ? message.id : Math.min(next, message.id);
                message.dialog_id = dialog;
                applyMessage(account, message);
                boolean unread = kind == MENTIONS ? message.mentioned && message.media_unread
                        : kind == REACTIONS ? MessageObject.hasUnreadReactions(message.reactions)
                        : message.media instanceof TLRPC.TL_messageMediaPoll && ((TLRPC.TL_messageMediaPoll) message.media).results.has_unread_votes;
                if (unread) {
                    left++;
                    first = first == 0 ? message.id : Math.min(first, message.id);
                } else {
                    hidden.add(Integer.toString(message.id));
                }
            }
            if (messages.messages.size() >= 100 && next > 0 && (offset == 0 || next < offset)) {
                loadPage(dialog, topic, kind, next, left, first, hidden, version);
                return;
            }
            loading.remove(scope);
            prefs.edit().putBoolean("scope:" + scope, true).putInt("left:" + scope, left)
                    .putStringSet("hidden_ids:" + scope, hidden).putInt("hidden:" + scope, hidden.size()).apply();
            outputs.put(scope, left);
            publish(dialog, topic, kind, left);
            ArrayList<Consumer<Integer>> callbacks = nextCallbacks.remove(scope);
            if (callbacks != null) for (Consumer<Integer> callback : callbacks) callback.accept(first);
        }));
    }

    private void publish(long dialog, long topic, int kind, int count) {
        publishing = true;
        try {
            MessagesController controller = MessagesController.getInstance(account);
            TLRPC.Dialog d = controller.dialogs_dict.get(dialog);
            if (d != null && topic == 0) {
                if (kind == MENTIONS) d.unread_mentions_count = count;
                else if (kind == REACTIONS) d.unread_reactions_count = count;
                else d.unread_poll_votes_count = count;
            }
            controller.sortDialogs(null);
            NotificationCenter center = NotificationCenter.getInstance(account);
            center.postNotificationName(kind == MENTIONS ? NotificationCenter.updateMentionsCount
                    : kind == REACTIONS ? NotificationCenter.dialogsUnreadReactionsCounterChanged
                    : NotificationCenter.dialogsUnreadPollVotesCounterChanged, dialog, topic, count, null, new LocalCount(account, generation));
            center.postNotificationName(NotificationCenter.dialogsNeedReload);
        } finally {
            publishing = false;
        }
    }

    public static void clear(int account) {
        GhostSeen store = get(account);
        store.generation++;
        store.prefs.edit().clear().apply();
        store.loading.clear();
        store.refreshed.clear();
        store.outputs.clear();
        store.nextCallbacks.clear();
    }
}
