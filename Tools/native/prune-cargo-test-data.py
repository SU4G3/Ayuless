#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Remove reviewed non-Android fixtures from Cargo vendor trees before scanning."""
import argparse
import hashlib
import json
from pathlib import Path

# Only these pinned packages were reviewed. New scanner findings need review,
# not a blanket exception for vendor directories or archive extensions.
UNUSED = {
    "askama_parser-0.13.0": ("tests/fuzzed_span_is_not_substring_of_source.bin",),
    "askama_parser-0.16.0": ("tests/fuzzed_span_is_not_substring_of_source.bin",),
    "bitstream-io-4.10.0": ("tests/random-3be.bin", "tests/random-3le.bin", "tests/random.bin"),
    "poly1305-0.8.0": ("src/fuzz/id=*",),
    # The archive is copied only for target_family=wasm in build.rs; the WASM
    # component is referenced by documentation examples. Neither runs on Android.
    "wit-bindgen-0.57.1": ("wasi-cli@0.2.0.wasm", "src/rt/libwit_bindgen_cabi.a"),
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("vendor", type=Path)
    args = parser.parse_args()
    total = 0
    for name, patterns in UNUSED.items():
        package = args.vendor / name
        if not package.is_dir():
            continue
        manifest = package / ".cargo-checksum.json"
        checksums = json.loads(manifest.read_text())
        files = sorted({p for pattern in patterns for p in package.glob(pattern)})
        # Validate the original archive's per-file hashes before deleting anything.
        # The registry package checksum and every remaining file hash stay intact.
        for path in files:
            if not path.is_file() or path.is_symlink():
                raise ValueError("Expected a regular vendored fixture: " + str(path))
            relative = path.relative_to(package).as_posix()
            if checksums["files"].get(relative) != hashlib.sha256(path.read_bytes()).hexdigest():
                raise ValueError("Vendored fixture checksum mismatch: " + str(path))
        for path in files:
            relative = path.relative_to(package).as_posix()
            path.unlink()
            del checksums["files"][relative]
            print("Removed unused fixture:", name + "/" + relative)
        if files:
            manifest.write_text(json.dumps(checksums, sort_keys=True, separators=(",", ":")) + "\n")
        total += len(files)
    print(f"Removed {total} reviewed Cargo fixtures; remaining file checksums unchanged")


if __name__ == "__main__":
    main()
