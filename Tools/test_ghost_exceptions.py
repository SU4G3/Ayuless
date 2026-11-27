#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
# Copyright (C) 2026 Ayuless contributors
"""Host privacy regression checks with real ghost policies and Android/network boundaries."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile

from test_ayugram_settings import STUBS as SETTINGS_STUBS, method

ROOT = Path(__file__).resolve().parents[1]
SOURCES = ROOT / 'TMessagesProj/src/main/java'
STUBS = {k: SETTINGS_STUBS[k] for k in (
    'android/content/Context.java', 'android/content/SharedPreferences.java',
    'android/content/Intent.java', 'org/telegram/messenger/ApplicationLoader.java')}
STUBS['android/content/Context.java'] = STUBS['android/content/Context.java'].replace(
    'public boolean contains(String k)',
    'public java.util.Set<String> getStringSet(String k,java.util.Set<String> d){return (java.util.Set<String>)map.getOrDefault(k,d);}\n public boolean contains(String k)')
STUBS['android/content/Context.java'] = STUBS['android/content/Context.java'].replace(
    'public Editor putInt(String k,int v)',
    'public Editor putStringSet(String k,java.util.Set<String> v){pending.put(k,new java.util.HashSet<>(v));return this;}\n public Editor putInt(String k,int v)')
STUBS['android/content/SharedPreferences.java'] = STUBS['android/content/SharedPreferences.java'].replace(
    'Editor edit();', 'java.util.Set<String> getStringSet(String k,java.util.Set<String> d); Editor edit();').replace(
    'Editor clear();', 'Editor putStringSet(String k,java.util.Set<String> v); Editor clear();')
STUBS.update({
    'org/telegram/messenger/AndroidUtilities.java': '''package org.telegram.messenger;
public class AndroidUtilities {public static void runOnUIThread(Runnable r){r.run();}}''',
    'org/telegram/messenger/UserConfig.java': '''package org.telegram.messenger;
public class UserConfig {public static final int MAX_ACCOUNT_COUNT=3;
 public static UserConfig getInstance(int a){return new UserConfig();} public long getClientUserId(){return 1000;}}''',
    'org/telegram/messenger/DialogObject.java': '''package org.telegram.messenger;
import org.telegram.tgnet.TLRPC;
public class DialogObject {
 public static boolean isEncryptedDialog(long id){return id>0&&(id&0x4000000000000000L)!=0;}
 public static boolean isUserDialog(long id){return id>0&&!isEncryptedDialog(id);}
 public static int getEncryptedChatId(long id){return (int)id;}
 public static long makeEncryptedDialogId(long id){return 0x4000000000000000L|id;}
 public static long getPeerDialogId(TLRPC.Peer p){return p.user_id!=0?p.user_id:-(p.chat_id!=0?p.chat_id:p.channel_id);}
}''',
    'org/telegram/messenger/MessagesController.java': '''package org.telegram.messenger;
import org.telegram.tgnet.TLRPC;
public class MessagesController {
 private static final MessagesController[] accounts={new MessagesController(),new MessagesController(),new MessagesController()};
 public final java.util.Map<Integer,TLRPC.EncryptedChat> chats=new java.util.HashMap<>();
 public TLRPC.InputPeer peer;
 public static MessagesController getInstance(int a){return accounts[a];}
 public TLRPC.EncryptedChat getEncryptedChat(int id){return chats.get(id);}
 public TLRPC.InputPeer getInputPeer(long id){return peer;}
}''',
    'org/telegram/messenger/MessageObject.java': '''package org.telegram.messenger;
public class MessageObject {public boolean out,scheduled;public long dialog;public int id;
 public boolean isOutOwner(){return out;} public long getDialogId(){return dialog;} public int getId(){return id;}}''',
    'org/telegram/tgnet/TLObject.java': 'package org.telegram.tgnet; public class TLObject {}',
    'org/telegram/tgnet/ConnectionsManager.java': '''package org.telegram.tgnet;
public class ConnectionsManager {
 public interface Delegate {void run(TLObject r,TLRPC.TL_error e);}
 public static final ConnectionsManager INSTANCE=new ConnectionsManager();
 public int calls;public TLObject request;public Delegate completion;
 public static ConnectionsManager getInstance(int a){return INSTANCE;}
 public int sendRequest(TLObject r,Delegate c){calls++;request=r;completion=c;return calls;}
}''',
    'org/telegram/tgnet/tl/TL_stories.java': '''package org.telegram.tgnet.tl;
import org.telegram.tgnet.*;
public class TL_stories {
 public static class TL_stories_readStories extends TLObject {public TLRPC.InputPeer peer;}
 public static class TL_stories_incrementStoryViews extends TLObject {public TLRPC.InputPeer peer;}
}''',
    'org/telegram/tgnet/tl/TL_account.java': '''package org.telegram.tgnet.tl;
public class TL_account {public static class updateStatus extends org.telegram.tgnet.TLObject {public boolean offline;}}''',
    'org/ayuless/ghost/StoryGhostPrompt.java': '''package org.ayuless.ghost;
public class StoryGhostPrompt {public static boolean sessionActive(int a){return false;}}''',
    'org/ayuless/ghost/DelayedSend.java': '''package org.ayuless.ghost;
public class DelayedSend {public static void fixOnSend(int a,org.telegram.tgnet.TLObject r){}}''',
    'org/ayuless/extras/SponsoredBlocker.java': '''package org.ayuless.extras;
public class SponsoredBlocker {public static boolean shouldBlock(org.telegram.tgnet.TLObject r){return false;}}''',
})
rpc = '''package org.telegram.tgnet;
public class TLRPC {
 public static class Peer {public long user_id,chat_id,channel_id;}
 public static class InputPeer extends Peer {}
 public static class TL_inputPeerEmpty extends InputPeer {}
 public static class TL_inputEncryptedChat {public int chat_id;}
 public static class EncryptedChat {public long user_id;}
 public static class Dialog {public Peer peer;public int read_inbox_max_id;}
 public static class TL_error {}
 public static class TL_inputDialogPeer {public InputPeer peer;}
 public static class TL_messages_getPeerDialogs extends TLObject {public java.util.List<TL_inputDialogPeer> peers=new java.util.ArrayList<>();}
 public static class TL_messages_peerDialogs extends TLObject {public java.util.List<Dialog> dialogs=new java.util.ArrayList<>();}
'''
for name in ('readHistory', 'readMentions', 'readReactions', 'readPollVotes', 'readDiscussion', 'setTyping'):
    rpc += f' public static class TL_messages_{name} extends TLObject {{public InputPeer peer;}}\n'
rpc += '''public static class TL_messages_readSavedHistory extends TLObject {public InputPeer parent_peer;}
public static class TL_messages_readEncryptedHistory extends TLObject {public TL_inputEncryptedChat peer;}
public static class TL_messages_setEncryptedTyping extends TLObject {public TL_inputEncryptedChat peer;}
public static class TL_messages_readMessageContents extends TLObject {}
public static class TL_channels_readHistory extends TLObject {}
public static class TL_channels_readMessageContents extends TLObject {}
}'''
STUBS['org/telegram/tgnet/TLRPC.java'] = rpc

# Exercise the production request decision itself; Android storage/send helpers are outside this test.
interceptor = (SOURCES / 'org/ayuless/ghost/GhostInterceptor.java').read_text()
policy = '''package org.ayuless.ghost;
import org.telegram.tgnet.*;import org.telegram.tgnet.tl.*;import org.telegram.messenger.*;import java.util.*;
public class GhostInterceptor {
private static final Set<TLObject> explicitOnce=new HashSet<>(),allowedOnce=new HashSet<>();
private static final Map<TLObject,Long> readDialogs=Collections.synchronizedMap(new WeakHashMap<>());
private static boolean isSendRequest(TLObject r){return false;}
private static void makeSilent(TLObject r){} private static void scheduleOffline(int a){}
'''
for signature in (
    'public static void tagReadRequest(', 'public static boolean shouldBlock(',
    'public static boolean blocksRead(', 'private static long peerDialog(',
    'private static long encryptedDialog(', 'private static long requestDialog(',
    'private static boolean isReadRequest(',
):
    policy += method(interceptor, signature) + '\n'
STUBS['org/ayuless/ghost/GhostInterceptor.java'] = policy + '}'

TEST = '''import java.util.*;
import org.ayuless.ghost.*;import org.ayuless.extras.*;
import org.telegram.messenger.*;import org.telegram.tgnet.*;import org.telegram.tgnet.tl.*;
public class GhostPrivacyCheck {
 static int assertions;
 static void check(boolean b){assertions++;if(!b)throw new AssertionError("assertion "+assertions);}
 static TLRPC.InputPeer user(long id){TLRPC.InputPeer p=new TLRPC.InputPeer();p.user_id=id;return p;}
 static TLRPC.TL_messages_readHistory read(long id){TLRPC.TL_messages_readHistory r=new TLRPC.TL_messages_readHistory();r.peer=user(id);return r;}
 public static void main(String[] args){
  GhostConfig.setProfile(-1,GhostConfig.KEY_ENABLED,true);
  GhostConfig.setProfile(-1,GhostConfig.KEY_HIDE_STORY_VIEWS,true);
  check(GhostInterceptor.shouldBlock(0,read(42)));
  GhostExceptions.set(-1,new ArrayList<>(Arrays.asList(42L,42L,-7L,0L,null)));
  check(GhostExceptions.get(-1).equals(Arrays.asList(42L)));
  check(!GhostInterceptor.shouldBlock(0,read(42)));check(!GhostInterceptor.shouldBlock(1,read(42)));
  check(GhostInterceptor.shouldBlock(0,read(43)));check(GhostInterceptor.blocksRead(0,-42));
  check(GhostInterceptor.shouldBlock(0,new TLRPC.TL_messages_readHistory()));
  TLRPC.TL_messages_setTyping typing=new TLRPC.TL_messages_setTyping();typing.peer=user(42);
  check(!GhostInterceptor.shouldBlock(0,typing));typing.peer=user(43);check(GhostInterceptor.shouldBlock(0,typing));
  TL_stories.TL_stories_readStories story=new TL_stories.TL_stories_readStories();story.peer=user(42);
  check(!GhostInterceptor.shouldBlock(0,story));story.peer=user(43);check(GhostInterceptor.shouldBlock(0,story));
  TL_account.updateStatus online=new TL_account.updateStatus();check(!GhostInterceptor.shouldBlock(0,online));check(online.offline);
  TLRPC.TL_messages_readMessageContents content=new TLRPC.TL_messages_readMessageContents();
  check(GhostInterceptor.shouldBlock(0,content));GhostInterceptor.tagReadRequest(content,42);
  check(!GhostInterceptor.shouldBlock(0,content));
  GhostExceptions.set(-1,new ArrayList<>());check(GhostInterceptor.shouldBlock(0,content));
  GhostExceptions.set(-1,new ArrayList<>(Arrays.asList(42L)));
  GhostConfig.setOverride(1,true);GhostConfig.setProfile(1,GhostConfig.KEY_ENABLED,true);
  check(!GhostExceptions.contains(1,42));check(GhostInterceptor.shouldBlock(1,read(42)));
  GhostExceptions.set(1,new ArrayList<>(Arrays.asList(43L)));check(GhostExceptions.contains(1,43));check(!GhostExceptions.contains(0,43));
  long secret=DialogObject.makeEncryptedDialogId(7);
  check(!GhostExceptions.contains(0,secret));TLRPC.EncryptedChat chat=new TLRPC.EncryptedChat();chat.user_id=42;
  MessagesController.getInstance(0).chats.put(7,chat);check(GhostExceptions.contains(0,secret));
  check(!GhostExceptions.contains(1,secret));check(!GhostExceptions.contains(-1,42));check(!GhostExceptions.contains(3,42));
  TLRPC.TL_messages_readEncryptedHistory encrypted=new TLRPC.TL_messages_readEncryptedHistory();
  encrypted.peer=new TLRPC.TL_inputEncryptedChat();encrypted.peer.chat_id=7;
  check(!GhostInterceptor.shouldBlock(0,encrypted));encrypted.peer.chat_id=8;check(GhostInterceptor.shouldBlock(0,encrypted));

  MessageObject message=new MessageObject();message.dialog=42;message.id=5;
  check(!GhostReadReceipt.shouldShow(0,message));ExtrasConfig.set(ExtrasConfig.KEY_GHOST_READ_RECEIPT,true);
  check(GhostReadReceipt.shouldShow(0,message));message.out=true;check(!GhostReadReceipt.shouldShow(0,message));message.out=false;
  message.scheduled=true;check(!GhostReadReceipt.shouldShow(0,message));message.scheduled=false;
  message.dialog=secret;check(!GhostReadReceipt.shouldShow(0,message));message.dialog=-42;check(!GhostReadReceipt.shouldShow(0,message));
  message.dialog=1000;check(!GhostReadReceipt.shouldShow(0,message));message.dialog=42;
  int[] status={99};check(GhostReadReceipt.query(0,message,x->status[0]=x)==0);check(status[0]==GhostReadReceipt.UNKNOWN);
  MessagesController.getInstance(0).peer=user(42);
  ConnectionsManager cm=ConnectionsManager.INSTANCE;
  check(GhostReadReceipt.query(0,message,x->status[0]=x)>0);
  check(cm.request instanceof TLRPC.TL_messages_getPeerDialogs);check(cm.calls==1);
  TLRPC.TL_messages_peerDialogs response=new TLRPC.TL_messages_peerDialogs();TLRPC.Dialog dialog=new TLRPC.Dialog();
  dialog.peer=user(42);dialog.read_inbox_max_id=4;response.dialogs.add(dialog);
  cm.completion.run(response,null);check(status[0]==GhostReadReceipt.NOT_SENT);
  dialog.read_inbox_max_id=5;cm.completion.run(response,null);check(status[0]==GhostReadReceipt.SENT);
  dialog.read_inbox_max_id=50;cm.completion.run(response,null);check(status[0]==GhostReadReceipt.SENT);
  cm.completion.run(response,new TLRPC.TL_error());check(status[0]==GhostReadReceipt.UNKNOWN);
  dialog.peer=user(43);cm.completion.run(response,null);check(status[0]==GhostReadReceipt.UNKNOWN);
  cm.completion.run(null,null);check(status[0]==GhostReadReceipt.UNKNOWN);
  GhostConfig.setProfile(-1,GhostConfig.KEY_ENABLED,false);check(!GhostReadReceipt.shouldShow(0,message));
  check(!GhostInterceptor.shouldBlock(0,read(43)));
  System.out.println("Ghost exceptions/receipt privacy: "+assertions+" assertions passed");
 }
}'''


def main():
    java_home = Path(os.environ['JAVA_HOME']) if 'JAVA_HOME' in os.environ else None
    with tempfile.TemporaryDirectory(prefix='ayuless-ghost-check-') as directory:
        work = Path(directory)
        for name, text in STUBS.items():
            file = work / name
            file.parent.mkdir(parents=True, exist_ok=True)
            file.write_text(text)
        for name in ('ghost/GhostConfig', 'ghost/GhostExceptions', 'ghost/GhostReadReceipt', 'extras/ExtrasConfig'):
            file = work / ('org/ayuless/' + name + '.java')
            file.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(SOURCES / ('org/ayuless/' + name + '.java'), file)
        (work / 'GhostPrivacyCheck.java').write_text(TEST)
        javac = str(java_home / 'bin/javac') if java_home else 'javac'
        java = str(java_home / 'bin/java') if java_home else 'java'
        subprocess.run([javac, '-encoding', 'UTF-8', '-d', str(work / 'out')]
                       + [str(p) for p in work.rglob('*.java')], check=True)
        subprocess.run([java, '-cp', str(work / 'out'), 'GhostPrivacyCheck'], check=True)


if __name__ == '__main__':
    main()
