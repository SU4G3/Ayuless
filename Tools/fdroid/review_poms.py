#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Report cached POM evidence for declarations; does not resolve dependencies."""
from hashlib import sha256
from pathlib import Path
import csv
import os
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
CACHE = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle")) / "caches/modules-2/files-2.1"
NS = {"m": "http://maven.apache.org/POM/4.0.0"}


def inspect(coordinate, seen=()):
    if coordinate in seen:
        return [], "", ["parent cycle"]
    group, artifact, version = coordinate
    files = sorted((CACHE / group / artifact / version).glob("*/*.pom"))
    if not files:
        return [], "", ["missing cached POM: " + ":".join(coordinate)]
    if len({p.read_bytes() for p in files}) != 1:
        return [], "", ["conflicting cached POMs: " + ":".join(coordinate)]
    path = files[0]
    tree = ET.parse(path).getroot()
    evidence = [":".join(coordinate) + " sha256=" + sha256(path.read_bytes()).hexdigest()]
    licences = [(item.findtext("m:name", "", NS), item.findtext("m:url", "", NS))
                for item in tree.findall("m:licenses/m:license", NS)]
    source = tree.findtext("m:scm/m:url", "", NS) or tree.findtext("m:url", "", NS)
    parent = tree.find("m:parent", NS)
    if not licences and parent is not None:
        parent_coord = tuple(parent.findtext("m:" + key, "", NS) for key in ("groupId", "artifactId", "version"))
        inherited, parent_source, proof = inspect(parent_coord, (*seen, coordinate))
        licences = inherited
        source = source or parent_source
        evidence.extend(proof)
    return [(" ".join(n.split()), " ".join(u.split())) for n,u in licences], " ".join(source.split()), evidence


def main():
    coordinates = set()
    for path in [ROOT / "build.gradle", ROOT / "TMessagesProj/build.gradle", ROOT / "buildSrc/build.gradle.kts"]:
        # These files have no commented-out coordinate declarations. This is a
        # declaration inventory, not Gradle's selected runtime/buildscript graph.
        coordinates.update(re.findall(r"['\"]([\w.-]+):([\w.-]+):([\w.+-]+)['\"]", path.read_text()))
    output = ROOT / "docs/fdroid/POM-INVENTORY.tsv"
    with output.open("w", newline="") as stream:
        writer = csv.writer(stream, delimiter="\t", lineterminator="\n")
        writer.writerow(("declared_coordinate", "licences", "licence_urls", "scm_or_project_url", "evidence"))
        for coordinate in sorted(coordinates):
            licences, source, evidence = inspect(coordinate)
            writer.writerow((":".join(coordinate), "; ".join(x[0] for x in licences) or "UNKNOWN",
                             "; ".join(x[1] for x in licences), source, "; ".join(evidence)))
    print(f"{len(coordinates)} declarations inspected using cached POMs; no dependency graph resolved.")


if __name__ == "__main__":
    main()
