#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Inventory local packaged resources, without inferring rights from filenames.

Run from any directory. Unavailable submodules and generated/dependency assets
must also be audited in a prepared release checkout and the final APK.
"""
from collections import Counter
from hashlib import sha256
from pathlib import Path
import csv
import re

ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "docs/fdroid/ASSET-INVENTORY.tsv"
UPSTREAM = "https://github.com/DrKLO/Telegram/tree/7be672ea/"
AYULESS = "https://github.com/SU4G3/Ayuless"


def classify(path):
    name = path.name
    relative = path.relative_to(ROOT).as_posix()
    if name.startswith("ayuless") or "_ayuless" in name:
        return ("solar" if name.startswith("ayuless_solar_") else "ayuless",
                "CC-BY-4.0" if name.startswith("ayuless_solar_") else "GPL-3.0-or-later", AYULESS)
    if "/fonts/" in relative:
        if path.suffix not in (".ttf", ".otf"):
            return "font-map", "UNKNOWN", UPSTREAM + relative
        if name == "rmono_var.ttf":
            return "roboto-mono", "OFL-1.1 (embedded grant)", "https://github.com/googlefonts/RobotoMono"
        families = {
            "mw_": ("merriweather", "OFL-1.1 (family; provenance pending)", "https://github.com/EbenSorkin/Merriweather"),
            "rmono": ("roboto-mono", "OFL-1.1 (family; provenance pending)", "https://github.com/googlefonts/RobotoMono"),
            "r": ("roboto", "Apache-2.0/OFL-1.1 (version pending)", "https://github.com/googlefonts/roboto-classic"),
        }
        for prefix, result in families.items():
            if name.startswith(prefix):
                return result
        return "custom-font", "UNKNOWN", UPSTREAM + relative
    if "/models/" in relative:
        return "model", "UNKNOWN", UPSTREAM + relative
    if name in ("secureid_ocr_nn.dat", "shapes.dat", "grammars.dat", "PhoneFormats.dat"):
        return "binary-data", "UNKNOWN", UPSTREAM + relative
    if name == "emoji.pack" or "/emoji/" in relative:
        return "emoji (Apple generator risk)", "UNKNOWN", UPSTREAM + relative
    if "/res/raw/" in relative and path.suffix == ".json":
        return "animation", "UNKNOWN", UPSTREAM + relative
    if path.suffix in (".mp3", ".wav", ".ogg"):
        return "sound", "UNKNOWN", UPSTREAM + relative
    if "/res/drawable" in relative or "/res/mipmap" in relative or path.suffix in (".png", ".webp", ".svg", ".jpg"):
        return "artwork", "UNKNOWN", UPSTREAM + relative
    if path.suffix in (".glsl", ".agsl"):
        return "shader", "GPL-2.0-or-later (repository grant)", UPSTREAM + relative
    if "/WidgetSandbox/" in relative:
        return "widget-manifest", "GPL-2.0-or-later (repository grant)", UPSTREAM + relative
    return "upstream-resource", "UNKNOWN", UPSTREAM + relative


def main():
    settings = (ROOT / "settings.gradle").read_text()
    modules = ["TMessagesProj", *re.findall(r"include\s+['\"]:(TMessagesProj_App\w*)['\"]", settings)]
    paths = set()
    for module in modules:
        source = ROOT / module / "src"
        for directory in source.glob("*/assets"):
            paths.update(p for p in directory.rglob("*") if p.is_file())
        for directory in source.glob("*/res"):
            paths.update(p for p in directory.rglob("*") if p.is_file())
    # Notices are generated/maintained separately; do not hash their own inventory.
    paths = sorted(p for p in paths if not p.name.startswith("ayuless-notices")
                   and "/assets/ayuless-notices/" not in p.as_posix())
    counts = Counter()
    with OUTPUT.open("w", newline="") as stream:
        writer = csv.writer(stream, delimiter="\t", lineterminator="\n")
        writer.writerow(("path", "sha256", "component", "licence_status", "source"))
        for path in paths:
            component, licence, source = classify(path)
            writer.writerow((path.relative_to(ROOT), sha256(path.read_bytes()).hexdigest(), component, licence, source))
            counts[component] += 1
    print(f"{len(paths)} resources inventoried; UNKNOWN/pending entries are release blockers.")
    for component, count in sorted(counts.items()):
        print(f"{component}: {count}")


if __name__ == "__main__":
    main()
