#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Record unchanged upstream resource provenance without inferring copyright ownership."""
import csv
import hashlib
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]
REVISION = "7be672ea"


def main():
    revision = subprocess.check_output(
        ["git", "rev-parse", REVISION + "^{commit}"], cwd=ROOT, text=True).strip()
    tree = subprocess.check_output(
        ["git", "ls-tree", "-r", revision, "--", "TMessagesProj/src/main", "TMessagesProj_App/src"],
        cwd=ROOT, text=True)
    upstream = {}
    for line in tree.splitlines():
        metadata, path = line.split("\t", 1)
        upstream[path] = metadata.split()[2]
    target = ROOT / "docs/fdroid/UPSTREAM-ASSET-PROVENANCE.tsv"
    count = 0
    with target.open("w", newline="") as stream:
        writer = csv.writer(stream, delimiter="\t", lineterminator="\n")
        writer.writerow(("path", "sha256", "upstream_commit", "upstream_git_blob", "identical"))
        for row in csv.DictReader((ROOT / "docs/fdroid/ASSET-INVENTORY.tsv").open(), delimiter="\t"):
            path = row["path"]
            data = (ROOT / path).read_bytes()
            sha256 = hashlib.sha256(data).hexdigest()
            if sha256 != row["sha256"]:
                raise ValueError("Regenerate ASSET-INVENTORY.tsv first: " + path)
            blob = hashlib.sha1(f"blob {len(data)}\0".encode() + data).hexdigest()
            identical = upstream.get(path) == blob
            writer.writerow((path, sha256, revision, upstream.get(path, ""), str(identical).lower()))
            count += identical
    print(f"{count} resources byte-identical to Telegram {revision}; ownership is not inferred")


if __name__ == "__main__":
    main()
