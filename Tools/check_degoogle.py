#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
# Copyright (C) 2026 Ayuless contributors
"""Check the configured Java source sets and manifests for proprietary SDK references.

This is a source audit, not a replacement for Gradle dependency resolution or javac.
"""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
FORBIDDEN = re.compile(
    r"com\.google\.(?:android\.(?:gms|play|recaptcha|search\.verification)|firebase|mlkit)"
    # OS launcher badge permissions/IPC are not Huawei SDK dependencies.
    r"|com\.huawei\.(?:hms|hianalytics|updatesdk|agconnect)"
    r"|com\.android\.billingclient|credentials-play-services-auth|libgsaverification"
)
ANDROID_NAME = "{http://schemas.android.com/apk/res/android}name"
errors = []


def code_without_comments(text):
    # Preserve strings (including URLs); comments can contain historical SDK names.
    return re.sub(
        r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*[\s\S]*?\*/',
        lambda match: "" if match[0].startswith(("//", "/*")) else match[0],
        text,
    )


settings = code_without_comments((ROOT / "settings.gradle").read_text())
modules = ["TMessagesProj", *re.findall(r"include\s+['\"]:(TMessagesProj_App\w*)['\"]", settings)]
modules = list(dict.fromkeys(modules))
for module in ["", *modules]:
    path = ROOT / module / "build.gradle"
    code = code_without_comments(path.read_text())
    for line in code.splitlines():
        if re.search(r"\b(?:implementation|api|classpath|id)\b|apply plugin:", line) and (
            FORBIDDEN.search(line) or "com.google.gms" in line or "com.google.firebase" in line
        ):
            errors.append(f"{path.relative_to(ROOT)}: proprietary declaration: {line.strip()}")
    if not module:
        continue
    excluded = set(re.findall(r"exclude\s+['\"]([^'\"]+\.java)['\"]", code))
    source = ROOT / module / "src/main/java"
    for java in source.rglob("*.java"):
        if java.relative_to(source).as_posix() in excluded:
            continue
        code = code_without_comments(java.read_text())
        # Protocol identifier strings are allowed; executable references and imports are not.
        code = re.sub(r'"(?:\\.|[^"\\])*"', '""', code)
        if FORBIDDEN.search(code):
            errors.append(f"{java.relative_to(ROOT)}: active proprietary SDK reference")
        for facade, name in re.findall(r"import org\.ayuless\.nogms\.(\w+)\.(\w+);", code):
            adapter = ROOT / f"TMessagesProj/src/main/java/org/ayuless/nogms/{facade}.java"
            if not adapter.exists() or not re.search(r"\b(?:class|interface)\s+" + name + r"\b", adapter.read_text()):
                errors.append(f"{java.relative_to(ROOT)}: missing local adapter {facade}.{name}")

manifests = [ROOT / module / "src/main/AndroidManifest.xml" for module in modules]
manifests += list((ROOT / "TMessagesProj/config").rglob("*.xml"))
for manifest in manifests:
    if not manifest.exists():
        continue
    for element in ET.parse(manifest).iter():
        name = element.get(ANDROID_NAME, "")
        if FORBIDDEN.search(name) or any(part in name for part in (
            "GcmPushListenerService", "WearAuthListenerService", "GoogleVoiceClient",
            "com.android.vending.BILLING", "permission.MAPS_RECEIVE",
            "com.microsoft.appcenter.",
        )):
            errors.append(f"{manifest.relative_to(ROOT)}: SDK manifest entry {name}")

if list(ROOT.glob("TMessagesProj*/google-services.json")):
    errors.append("Google Services configuration still exists")
if (ROOT / "TMessagesProj/libs/libgsaverification-client.aar").exists():
    errors.append("Proprietary search verification AAR still exists")
if (ROOT / "TMessagesProj_AppHuawei").exists():
    errors.append("Unused proprietary Huawei distribution still exists")
if errors:
    raise SystemExit("\n".join(errors))
print(f"DeGoogle source audit passed ({len(modules)} configured modules; SDK-only sources excluded).")
