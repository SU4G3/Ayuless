#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Check every generated image against the existing EmojiPack reader contract."""
import argparse
import hashlib
import io
from pathlib import Path
import struct
import zipfile

from PIL import Image


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("assets", type=Path, help="generated assets directory or built APK")
    args = parser.parse_args()
    archive = zipfile.ZipFile(args.assets) if args.assets.is_file() else None

    def read_asset(name):
        return archive.read("assets/" + name) if archive else (args.assets / name).read_bytes()

    if archive:
        assert archive.getinfo("assets/emoji.pack").compress_type == zipfile.ZIP_STORED, "EmojiPack requires openFd on an uncompressed asset"
    data = read_asset("emoji.pack")
    header = struct.unpack_from("<4sHH6I", data)
    pages = [1999, 218, 130, 338, 131, 232, 298, 260]
    count = sum(pages)
    assert header == (b"EPK3", 3, 20, 64, 64, count, count, len(data), 0)
    expected_keys = [page * 4096 + index for page, size in enumerate(pages) for index in range(size)]
    next_offset = 32 + count * 20
    for record, key in enumerate(expected_keys):
        entry = struct.unpack_from("<HHIHHHHBBBB", data, 32 + record * 20)
        actual_key, mask, offset, length, a, b, colors, codec, prediction, channels, flags = entry
        assert (actual_key, mask, a, b, colors, codec, prediction, channels, flags) == (
            key, 65535, 65535, 65535, 0, 3, 0, 0, 0), (record, entry)
        assert offset == next_offset and 0 < length < 65536 and offset + length <= len(data)
        with Image.open(io.BytesIO(data[offset:offset + length])) as image:
            assert image.format == "WEBP" and image.size == (64, 64), key
            assert image.convert("RGBA").getchannel("A").getbbox() is not None, key
            image.load()
        next_offset += length
    assert next_offset == len(data)
    combinations = set()
    for left in range(-1, 5):
        for right in range(-1, 5):
            with Image.open(io.BytesIO(read_asset(f"emoji/compound/0_{left}_0.png"))) as a:
                with Image.open(io.BytesIO(read_asset(f"emoji/compound/0_{right}_1.png"))) as b:
                    assert a.size == b.size == (64, 64)
                    composed = Image.alpha_composite(a.convert("RGBA"), b.convert("RGBA"))
                    combinations.add(hashlib.sha256(composed.tobytes()).digest())
    assert len(combinations) == 36, "Hand skin tones must remain independently selectable"
    if archive:
        archive.close()
    print(f"Validated {count} EPK3 records and all 36 handshake skin-tone combinations")


if __name__ == "__main__":
    main()
