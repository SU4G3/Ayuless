#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Host regression test for #53; requires Python 3 and a JDK, no Gradle/SDK.

Runs production Localization, AppBranding and extracted LocaleController lookup/
formatting methods against binary assets and an empty Android string table.
Also checks that every runtime XML string reference escapes forced discard.
This does not replace inspecting a shrunk APK or testing startup on Android.
"""
import os
from pathlib import Path
import re
import shutil
import struct
import subprocess
import tempfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
SOURCES = ROOT / "TMessagesProj/src/main/java"
TASK = ROOT / "buildSrc/src/main/kotlin/org/telegram/tasks/TelegramStringsTask.kt"


def java_hash(value):
    result = 0
    # Java hashes UTF-16 code units, including possible surrogate pairs.
    encoded = value.encode("utf-16-be")
    for (char,) in struct.iter_unpack(">H", encoded):
        result = (31 * result + char) & 0xffffffff
    return result


def check_resources():
    task = TASK.read_text()
    kept = set(re.findall(r'"(\w+)"', re.search(
        r'GENERATED_EXCLUSIONS = setOf\((.*?)\)', task, re.S)[1]))
    assert 'tools:keep=' in task and '.filterNot { GENERATED_EXCLUSIONS.contains(it) }' in task
    res = ROOT / "TMessagesProj/src/main/res"
    inputs = list(res.glob("values*/strings.xml")) + list(res.glob("values*/ayuless.xml"))
    names = {node.attrib["name"] for path in inputs for node in ET.parse(path).getroot().findall("string")}
    names |= {name.rsplit("_", 1)[0] for name in names
              if name.rsplit("_", 1)[-1] in {"zero", "one", "two", "few", "many", "other"}}
    stable = sorted(names - {"AppName", "AppNameBeta"})
    # IDs shift when the namespace changes; do not freeze incident IDs in the build contract.
    for key in ("InternalNotificationsSystemSetting", "AyulessLauncherName"):
        print(f"{key}: {0x7f0ffffe - stable.index(key):#010x}", flush=True)
    refs = set()
    tools = "{http://schemas.android.com/tools}"
    paths = list((ROOT / "TMessagesProj/config").rglob("*.xml"))
    for module in ROOT.glob("TMessagesProj*"):
        paths += list((module / "src").rglob("*.xml"))
    for path in paths:
        for node in ET.parse(path).getroot().iter():
            values = [value for key, value in node.attrib.items() if not key.startswith(tools)]
            if node.text:
                values.append(node.text)
            for value in values:
                refs.update(re.findall(r"@string/(\w+)", value))
    assert not (refs & names) - kept, f"XML strings forced out of APK: {(refs & names) - kept}"
    for locale in ("values", "values-ru"):
        strings = {n.attrib["name"]: n.text for n in ET.parse(res / locale / "ayuless.xml").getroot()}
        assert strings["AppName"] == "Ayuless" and strings["AppNameBeta"] == "Ayuless Beta"
    assert "AyulessLauncherName" in kept
    print("XML/manifest retention: OK", flush=True)


def method(source, signature):
    start = source.index(signature)
    brace = source.index("{", start)
    depth = 1
    end = brace + 1
    while depth:
        depth += (source[end] == "{") - (source[end] == "}")
        end += 1
    return source[start:end]


STUBS = {
    "androidx/annotation/Nullable.java": "package androidx.annotation; public @interface Nullable {}",
    "androidx/annotation/StringRes.java": "package androidx.annotation; public @interface StringRes {}",
    "android/content/res/AssetManager.java": """
package android.content.res;
public class AssetManager {
 public java.io.InputStream open(String name) throws java.io.IOException {
  return new java.io.FileInputStream(new java.io.File(System.getProperty("assets"), name));
 }
}
""",
    "android/content/res/Resources.java": """
package android.content.res;
public class Resources {
 public AssetManager getAssets() { return new AssetManager(); }
 public String getResourceEntryName(int id) { throw new AssertionError("No string resource table"); }
 public String getString(int id) { throw new AssertionError("No string resource table"); }
}
""",
    "android/content/Context.java": """
package android.content;
public class Context {
 public android.content.res.Resources getResources() { return new android.content.res.Resources(); }
 public android.content.res.AssetManager getAssets() { return getResources().getAssets(); }
 public String getString(int id) { return getResources().getString(id); }
}
""",
    "android/util/SparseArray.java": """
package android.util;
public class SparseArray<T> {
 private final java.util.TreeMap<Integer,T> data = new java.util.TreeMap<>();
 public SparseArray() {} public SparseArray(int capacity) {}
 public void put(int key,T value) { data.put(key,value); }
 public void append(int key,T value) { put(key,value); }
 public T get(int key) { return data.get(key); }
 public int size() { return data.size(); }
 public int keyAt(int i) { return new java.util.ArrayList<>(data.keySet()).get(i); }
 public T valueAt(int i) { return data.get(keyAt(i)); }
 public SparseArray<T> clone() { SparseArray<T> copy=new SparseArray<>(); copy.data.putAll(data); return copy; }
}
""",
    "android/util/SparseIntArray.java": """
package android.util;
public class SparseIntArray extends SparseArray<Integer> {
 public SparseIntArray(int capacity) { super(capacity); }
 public Integer get(int key) { Integer value=super.get(key); return value==null ? 0 : value; }
}
""",
    "org/telegram/tgnet/SerializedData.java": """
package org.telegram.tgnet;
public class SerializedData {
 private final java.io.DataInputStream input;
 public SerializedData(java.io.InputStream input) { this.input=new java.io.DataInputStream(input); }
 public int readInt32(boolean exception) throws java.io.IOException { return Integer.reverseBytes(input.readInt()); }
 public String readString(boolean exception) throws java.io.IOException {
  int length=input.readUnsignedByte(), header=1;
  if(length==254) { length=input.readUnsignedByte() | input.readUnsignedByte()<<8 | input.readUnsignedByte()<<16; header=4; }
  byte[] bytes=new byte[length]; input.readFully(bytes);
  for(int i=(length+header)%4; i!=0 && i<4; i++) input.readByte();
  return new String(bytes,java.nio.charset.StandardCharsets.UTF_8);
 }
}
""",
    "org/telegram/messenger/ApplicationLoader.java": """
package org.telegram.messenger;
public class ApplicationLoader { public static android.content.Context applicationContext=new android.content.Context(); }
""",
    "org/telegram/messenger/BuildVars.java": "package org.telegram.messenger; public class BuildVars { public static boolean USE_CLOUD_STRINGS=true; }",
    "org/telegram/messenger/FileLog.java": "package org.telegram.messenger; public class FileLog { public static void e(Exception e) {} }",
}

HARNESS = """
package org.telegram.messenger;
import android.content.Context;
import androidx.annotation.StringRes;
import org.telegram.utils.localization.Localization;
public class LocaleController {
 private static final LocaleController INSTANCE=new LocaleController();
 private Localization localizationInternalDefault;
 private Localization localizationInternal;
 private Localization localizationExternal=Localization.EMPTY;
 private java.util.Locale currentLocale=java.util.Locale.US;
 private static LocaleController getInstance() { return INSTANCE; }
 private void checkLocalizationInternal() {
  if (localizationInternalDefault==null) {
   localizationInternalDefault=new Localization.Builder().addResLocalization(ApplicationLoader.applicationContext,"localization_en.bin").build();
   localizationInternal=localizationInternalDefault;
  }
 }
 METHODS
 private static void equal(String expected,String actual) {
  if(!java.util.Objects.equals(expected,actual)) throw new AssertionError(expected+" != "+actual);
 }
 private static class StartupChannel {
  static final String NAME=getInstance().getStringV2(null,0x7f0fe86d,null);
 }
 public static void main(String[] args) {
  equal("Internal notifications",StartupChannel.NAME);
  equal("Ayuless",INSTANCE.getStringV2(null,1,null));
  equal("Ayuless Beta",INSTANCE.getStringV2("AppNameBeta",0,null));
  equal("Allow Ayuless calls",INSTANCE.getStringV2(null,2,null));
  java.util.HashMap<String,String> local=new java.util.HashMap<>();
  local.put("AllowReadCall","Разрешить звонки Telegram");
  INSTANCE.localizationInternal=new Localization.Builder().addLocalization(INSTANCE.localizationInternalDefault).addLocalization(local).build();
  equal("Разрешить звонки Ayuless",INSTANCE.getStringV2("AllowReadCall",0,null));
  INSTANCE.localizationInternal=INSTANCE.localizationInternalDefault;
  equal(null,INSTANCE.getStringV2(null,999,null));
  equal("Allow Telegram calls",INSTANCE.getStringV2("UnknownKey",0,"AllowReadCall"));
  java.util.HashMap<String,String> cloud=new java.util.HashMap<>();
  cloud.put("AllowReadCall","Allow Telegram calls for %1$s");
  cloud.put("ContactsPermissionAlert","Telegram contacts sync to Telegram servers");
  cloud.put("UnknownKey","Telegram Premium");
  cloud.put("AppName","Telegram");
  INSTANCE.localizationExternal=new Localization.Builder().addLocalization(cloud).build();
  equal("Ayuless",INSTANCE.getStringV2(null,1,null));
  equal("Allow Ayuless calls for Telegram user",formatString(null,null,2,"Telegram user"));
  equal("Ayuless contacts sync to Telegram servers",INSTANCE.getStringV2("ContactsPermissionAlert",0,null));
  equal("Telegram Premium",INSTANCE.getStringV2("UnknownKey",0,null));
  cloud.put("AllowReadCall","Broken %d");
  INSTANCE.localizationExternal=new Localization.Builder().addLocalization(cloud).build();
  equal("Allow Ayuless calls",formatString(null,null,2,"user"));
  equal("Allow Ayuless calls",formatString("AllowReadCall",null,2,"user"));
  System.out.println("Binary-only startup, branding, cloud strings and formatting fallback: OK");
 }
}
"""


def write_assets(directory):
    entries = {"AppName": "Ayuless", "AppNameBeta": "Ayuless Beta",
               "AllowReadCall": "Allow Telegram calls",
               "InternalNotificationsSystemSetting": "Internal notifications"}
    blob = bytearray(struct.pack("<I", len(entries)))
    for key in sorted(entries, key=java_hash):
        value = entries[key].encode()
        assert len(value) < 254
        blob += struct.pack("<I", java_hash(key))
        record = bytes([len(value)]) + value
        blob += record + bytes((-len(record)) % 4)
    (directory / "localization_en.bin").write_bytes(blob)
    ids = {1: "AppName", 2: "AllowReadCall", 0x7f0fe86d: "InternalNotificationsSystemSetting"}
    (directory / "string_resource_ids.bin").write_bytes(struct.pack("<I", len(ids)) + b"".join(
        struct.pack("<II", res_id, java_hash(key)) for res_id, key in sorted(ids.items())))


def main():
    check_resources()
    # Use the real locale lookup and format fallback, with only lifecycle/platform boundaries stubbed.
    source = (SOURCES / "org/telegram/messenger/LocaleController.java").read_text()
    signatures = ["private String getStringV2(", "private String getUnbrandedStringV2(",
                  "private static String formatString(String key, String fallback,"]
    harness = HARNESS.replace("METHODS", "\n".join(method(source, signature) for signature in signatures))
    jdk = Path(os.environ["JAVA_HOME"]) / "bin" if os.environ.get("JAVA_HOME") else None
    javac = str(jdk / "javac") if jdk else shutil.which("javac")
    java = str(jdk / "java") if jdk else shutil.which("java")
    with tempfile.TemporaryDirectory(prefix="ayuless-strings-") as temp:
        work = Path(temp)
        stubs = dict(STUBS, **{"org/telegram/messenger/LocaleController.java": harness})
        for name, content in stubs.items():
            path = work / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content)
        write_assets(work)
        sources = list(work.rglob("*.java")) + [SOURCES / "org/ayuless/branding/AppBranding.java",
                                               SOURCES / "org/telegram/utils/localization/Localization.java"]
        subprocess.run([javac, "--release", "8", "-encoding", "UTF-8", "-d", str(work)] + list(map(str, sources)), check=True)
        subprocess.run([java, "-Dassets=" + str(work), "-cp", str(work), "org.telegram.messenger.LocaleController"], check=True)


if __name__ == "__main__":
    main()
