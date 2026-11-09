#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
# Copyright (C) 2026 Ayuless contributors
"""Exercise real settings policies on a JVM, with Android/Telegram boundary stubs.

Covers upgrades, account/profile isolation, opt-in defaults, locks, mutually
exclusive ghost actions, media categories, bot retention and endpoint validation.
Does not replace an Android build or device/network integration testing.
"""
from pathlib import Path
import os
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
SOURCES = ROOT / "TMessagesProj/src/main/java"
STUBS = {
"android/content/Context.java": """
package android.content;
public class Context {
 public static final int MODE_PRIVATE=0;
 private final java.util.Map<String,SharedPreferences> prefs=new java.util.HashMap<>();
 public SharedPreferences getSharedPreferences(String name,int mode) { return prefs.computeIfAbsent(name,k->new Memory()); }
 static class Memory implements SharedPreferences {
  final java.util.Map<String,Object> map=new java.util.HashMap<>();
  public boolean getBoolean(String k,boolean d){return (Boolean)map.getOrDefault(k,d);}
  public String getString(String k,String d){return (String)map.getOrDefault(k,d);}
  public int getInt(String k,int d){return (Integer)map.getOrDefault(k,d);}
  public boolean contains(String k){return map.containsKey(k);}
  public Editor edit(){return new Editor(){
   final java.util.Map<String,Object> pending=new java.util.HashMap<>();
   boolean clear;
   public Editor clear(){clear=true;return this;}
   public Editor putInt(String k,int v){pending.put(k,v);return this;}
   public Editor putBoolean(String k,boolean v){pending.put(k,v);return this;}
   public Editor putString(String k,String v){pending.put(k,v);return this;}
   public Editor remove(String k){pending.put(k,null);return this;}
   public void apply(){if(clear)map.clear();pending.forEach((k,v)->{if(v==null)map.remove(k);else map.put(k,v);});}
  };}
 }
}
""",
"android/content/SharedPreferences.java": """
package android.content;
public interface SharedPreferences {
 boolean getBoolean(String k,boolean d); String getString(String k,String d); boolean contains(String k); int getInt(String k,int d); Editor edit();
 interface Editor {Editor clear();Editor putInt(String k,int v);Editor putBoolean(String k,boolean v);Editor putString(String k,String v);Editor remove(String k);void apply();}
}
""",
"org/telegram/messenger/ApplicationLoader.java": """
package org.telegram.messenger;
public class ApplicationLoader {public static android.content.Context applicationContext=new android.content.Context();}
""",
"org/telegram/messenger/DialogObject.java": """
package org.telegram.messenger;
public class DialogObject {public static boolean isEncryptedDialog(long did){return did>=0 && (did & 0x4000000000000000L)!=0;}}
""",
"org/telegram/messenger/ChatObject.java": """
package org.telegram.messenger;
public class ChatObject {
 public static boolean isPublic(org.telegram.tgnet.TLRPC.Chat chat){return chat.publicChat;}
 public static boolean isChannel(org.telegram.tgnet.TLRPC.Chat chat){return chat.channel;}
}
""",
"org/telegram/messenger/MessagesController.java": """
package org.telegram.messenger;
public class MessagesController {
 static final MessagesController[] accounts={new MessagesController(),new MessagesController(),new MessagesController()};
 public final java.util.Map<Long,org.telegram.tgnet.TLRPC.User> users=new java.util.HashMap<>();
 public final java.util.Map<Long,org.telegram.tgnet.TLRPC.Chat> chats=new java.util.HashMap<>();
 public static MessagesController getInstance(int a){return accounts[a];}
 public org.telegram.tgnet.TLRPC.User getUser(long did){return users.get(did);}
 public org.telegram.tgnet.TLRPC.Chat getChat(long did){return chats.get(did);}
}
""",
"org/telegram/messenger/MessageObject.java": """
package org.telegram.messenger;
public class MessageObject {public static long getDialogId(org.telegram.tgnet.TLRPC.Message m){return m.dialog_id;}}
""",
"org/telegram/messenger/FileLog.java": """
package org.telegram.messenger; public class FileLog {public static void e(Exception e){throw new AssertionError(e);}}
""",
"org/telegram/messenger/R.java": """
package org.telegram.messenger; public class R {public static class string {public static final int EditedMessage=1;}}
""",
"org/telegram/messenger/LocaleController.java": """
package org.telegram.messenger; public class LocaleController {public static String getString(int id){return "edited";}}
""",
"org/telegram/tgnet/NativeByteBuffer.java": """
package org.telegram.tgnet; public class NativeByteBuffer {
 public NativeByteBuffer(int size){} public void rewind(){} public int readInt32(boolean e){return 0;} public void reuse(){}
}
""",
"org/telegram/tgnet/TLRPC.java": """
package org.telegram.tgnet;
public class TLRPC {
 public static final int MESSAGE_FLAG_HAS_ENTITIES=128;
 public static class User {public boolean bot;}
 public static class Chat {public boolean publicChat,channel,megagroup;}
 public static class Media {public int ttl_seconds;}
 public static class Message {
  public long dialog_id; public int id,ttl,ttl_period,destroyTime,flags; public long destroyTimeMillis;
  public Media media;public String attachPath;public Object reactions;public java.util.List<Object> entities=new java.util.ArrayList<>();
  public int getObjectSize(){return 0;}public void serializeToStream(NativeByteBuffer b){}
  public static Message TLdeserialize(NativeByteBuffer b,int c,boolean e){return new Message();}
 }
}
""",
"org/ayuless/ghost/StoryGhostPrompt.java": """
package org.ayuless.ghost; public class StoryGhostPrompt {public static boolean sessionActive(int a){return false;}}
""",
"org/ayuless/spy/SpyExceptions.java": """
package org.ayuless.spy; public class SpyExceptions {public static boolean isExcepted(int a,long did){return did==99;}}
""",
"org/ayuless/spy/SavedAttachments.java": """
package org.ayuless.spy; public class SavedAttachments {public static boolean isEnabled(int a){return false;}}
""",
}
ACTUAL = [
 "org/ayuless/ghost/GhostConfig.java", "org/ayuless/spy/MessageSavingConfig.java",
 "org/ayuless/sync/SyncConfig.java", "org/ayuless/extras/ExtrasConfig.java",
 "org/ayuless/appearance/AppearanceConfig.java", "org/ayuless/content/RetentionConfig.java",
 "org/ayuless/antidelete/AntiDeleteConfig.java", "org/ayuless/edits/EditsConfig.java",
]
TEST = r"""
import org.ayuless.ghost.GhostConfig;
import org.ayuless.spy.MessageSavingConfig;
import org.ayuless.sync.SyncConfig;
import org.ayuless.extras.ExtrasConfig;
import org.ayuless.appearance.AppearanceConfig;
import org.ayuless.content.RetentionConfig;
import org.ayuless.antidelete.AntiDeleteConfig;
import org.telegram.messenger.*;
import org.telegram.tgnet.TLRPC;
public class PolicyTest {
 static int assertions;
 static void check(boolean value){assertions++;if(!value)throw new AssertionError("Check "+assertions);}
 public static void main(String[] args) {
  for(int a=0;a<3;a++){
   check(!GhostConfig.isEnabled(a));check(!GhostConfig.anyActive(a));check(!AntiDeleteConfig.isEnabled(a));
   check(!GhostConfig.hideRead(a));check(!GhostConfig.hideOnline(a));check(!GhostConfig.delayedSend(a));
   check(MessageSavingConfig.saveFormatting(a));check(MessageSavingConfig.saveReactions(a));
   check(MessageSavingConfig.saveDialog(a,42));check(!MessageSavingConfig.get(a,MessageSavingConfig.CUSTOM));
  }
  check(!AppearanceConfig.customMarks());check(!SyncConfig.enabled());check(SyncConfig.secure());
  String[] extras={ExtrasConfig.KEY_READ_UNTIL,ExtrasConfig.KEY_KEEP_LOCAL,ExtrasConfig.KEY_DELETE_DATE,
   ExtrasConfig.KEY_GHOST_MENU,ExtrasConfig.KEY_KILL_MENU,ExtrasConfig.KEY_LOCAL_RESEND,
   ExtrasConfig.KEY_GHOST_SHORTCUT,ExtrasConfig.KEY_MAINTENANCE};
  for(String key:extras)check(!ExtrasConfig.get(key));
  GhostConfig.setProfile(-1,GhostConfig.KEY_ENABLED,true);
  check(GhostConfig.countEnabled(-1)==4);check(GhostConfig.hideRead(0));check(GhostConfig.hideOnline(2));
  check(!GhostConfig.hideStoryViews(0));
  GhostConfig.setProfile(-1,GhostConfig.KEY_ENABLED,false);
  check(!GhostConfig.hideRead(0));check(GhostConfig.countEnabled(-1)==4);
  GhostConfig.setLocked(-1,GhostConfig.KEY_HIDE_READ,true);check(GhostConfig.hideRead(0));check(GhostConfig.anyActive(0));
  GhostConfig.setLocked(-1,GhostConfig.KEY_HIDE_READ,false);
  // Upgrade: old per-account master flag implicitly retains the account override.
  GhostConfig.profilePrefs(1).edit().putBoolean(GhostConfig.KEY_ENABLED,false).putBoolean(GhostConfig.KEY_HIDE_READ,false).apply();
  check(GhostConfig.hasOverride(1));check(GhostConfig.activeProfile(1)==1);
  GhostConfig.setProfile(1,GhostConfig.KEY_ENABLED,true);check(!GhostConfig.hideRead(1));check(GhostConfig.countEnabled(1)==0);
  GhostConfig.setProfile(-1,GhostConfig.KEY_ENABLED,true);check(GhostConfig.hideRead(0));check(!GhostConfig.hideRead(1));
  GhostConfig.setOverride(1,false);check(GhostConfig.activeProfile(1)==-1);check(GhostConfig.hideRead(1));
  GhostConfig.setProfile(-1,GhostConfig.KEY_READ_ON_ACTION,true);check(GhostConfig.readOnAction(0));
  GhostConfig.setProfile(-1,GhostConfig.KEY_DELAYED_SEND,true);check(GhostConfig.delayedSend(0));check(!GhostConfig.readOnAction(0));
  GhostConfig.setProfile(-1,GhostConfig.KEY_READ_ON_ACTION,true);check(!GhostConfig.delayedSend(0));check(GhostConfig.readOnAction(0));
  TLRPC.User bot=new TLRPC.User();bot.bot=true;MessagesController.getInstance(0).users.put(42L,bot);
  TLRPC.Message message=new TLRPC.Message();message.dialog_id=42;
  AntiDeleteConfig.setEnabled(0,true);check(RetentionConfig.storable(0,message,RetentionConfig.DELETE));
  MessageSavingConfig.set(0,MessageSavingConfig.CUSTOM,true);MessageSavingConfig.set(0,MessageSavingConfig.BOTS,false);
  check(!MessageSavingConfig.saveDialog(0,42));check(!RetentionConfig.storable(0,message,RetentionConfig.DELETE));
  check(MessageSavingConfig.saveDialog(1,42));check(MessageSavingConfig.saveDialog(0,43));
  message.dialog_id=99;check(!RetentionConfig.storable(0,message,RetentionConfig.DELETE));
  message.dialog_id=0x400000000000002aL;check(MessageSavingConfig.saveDialog(0,message.dialog_id));
  for(int type=0;type<5;type++){
   TLRPC.Chat chat=new TLRPC.Chat();chat.channel=type!=0;chat.megagroup=type>=3;chat.publicChat=type==1||type==3;
   message.dialog_id=type==0?44:-(100+type);MessagesController.getInstance(0).chats.put(-message.dialog_id,chat);
   MessageSavingConfig.set(0,MessageSavingConfig.MEDIA_CATEGORIES,false);check(MessageSavingConfig.saveMedia(0,message));
   MessageSavingConfig.set(0,MessageSavingConfig.MEDIA_CATEGORIES,true);
   check(MessageSavingConfig.saveMedia(0,message)==(type==0||type==2||type==4));
   MessageSavingConfig.set(0,MessageSavingConfig.CATEGORIES[type],true);check(MessageSavingConfig.saveMedia(0,message));
   MessageSavingConfig.set(0,MessageSavingConfig.CATEGORIES[type],false);check(!MessageSavingConfig.saveMedia(0,message));
   check(MessageSavingConfig.saveMedia(1,message));
  }
  message.dialog_id=-999;check(MessageSavingConfig.saveMedia(0,message)); // unknown metadata retains
  MessageSavingConfig.set(0,MessageSavingConfig.FORMATTING,false);MessageSavingConfig.set(0,MessageSavingConfig.REACTIONS,false);
  check(!MessageSavingConfig.saveFormatting(0));check(!MessageSavingConfig.saveReactions(0));check(MessageSavingConfig.saveFormatting(1));
  MessageSavingConfig.set(0,MessageSavingConfig.CUSTOM,false);
  check(MessageSavingConfig.saveFormatting(0));check(MessageSavingConfig.saveReactions(0));check(MessageSavingConfig.saveDialog(0,42));
  for(String server:new String[]{"example.org","https://example.org/","example.org:8443","[::1]:8443"})check(SyncConfig.setServer(server));
  for(String server:new String[]{"https://example.org/path","user@example.org","example.org?token=x","example.org#fragment","example.org:0","example.org:99999","example.org:bad","example.org\nInjected: x"})check(!SyncConfig.setServer(server));
  check(SyncConfig.setServer(""));check(SyncConfig.server().isEmpty());
  check(!SyncConfig.enabled());SyncConfig.setEnabled(true);check(SyncConfig.enabled());
  SyncConfig.setSecure(false);check(!SyncConfig.secure());SyncConfig.setSecure(true);check(SyncConfig.secure());
  System.out.println("Settings policy passed: "+assertions+" assertions (JVM boundary stubs)");
 }
}
"""

def main():
    java_home = Path(os.environ["JAVA_HOME"]) if "JAVA_HOME" in os.environ else None
    javac = str(java_home / "bin/javac") if java_home else "javac"
    java = str(java_home / "bin/java") if java_home else "java"
    with tempfile.TemporaryDirectory(prefix="ayuless-parity-test-") as directory:
        work = Path(directory)
        for name, text in STUBS.items():
            path = work / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text)
        for name in ACTUAL:
            path = work / name
            path.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(SOURCES / name, path)
        (work / "PolicyTest.java").write_text(TEST)
        subprocess.run([javac, "-encoding", "UTF-8", "-d", str(work / "out")]
                       + [str(p) for p in work.rglob("*.java")], check=True)
        subprocess.run([java, "-cp", str(work / "out"), "PolicyTest"], check=True)

if __name__ == "__main__":
    main()
