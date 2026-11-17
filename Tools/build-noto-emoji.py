#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Build Telegram's EPK3 assets from the pinned, freely licensed Noto SVGs."""
import argparse
from concurrent.futures import ProcessPoolExecutor
from hashlib import sha256
import io
import json
import os
from pathlib import Path
import re
import struct
import xml.etree.ElementTree as ET

import cairosvg
from PIL import Image, features

ROOT = Path(__file__).resolve().parents[1]
SOURCES = ROOT / "branding/noto-emoji"
SIDE = 64


def emoji_pages():
    text = (ROOT / "TMessagesProj/src/main/java/org/telegram/messenger/EmojiData.java").read_text()
    section = text.split("public static final String[][] data = {", 1)[1].split("\n    };", 1)[0]
    pages = [[json.loads(s) for s in re.findall(r'"(?:[^"\\]|\\.)*"', row)]
             for row in section.split("new String[]{")[1:]]
    if len(pages) != 8 or not all(pages) or any(len(page) >= 4096 for page in pages):
        raise ValueError("EmojiData layout changed; update the Noto source mapping")
    return pages


def sequence_name(text):
    return "_".join(f"{ord(char):04x}" for char in text if ord(char) != 0xFE0F)


def render(svg):
    # Oversample vector edges before reducing to the reader's fixed 64px size.
    # Some Noto SVGs declare internal namespace entities. Resolve those with
    # the standard parser and serialize plain XML; never enable CairoSVG's
    # unsafe mode or external entity resolution.
    plain_xml = ET.tostring(ET.fromstring(svg))
    png = cairosvg.svg2png(bytestring=plain_xml, output_width=128, output_height=128)
    with Image.open(io.BytesIO(png)) as image:
        return image.convert("RGBA").resize((SIDE, SIDE), Image.Resampling.LANCZOS)


def encode_record(item):
    key, path = item
    try:
        output = io.BytesIO()
        render(path.read_bytes()).save(output, format="WEBP", lossless=True, method=4, exact=True)
        return key, output.getvalue()
    except Exception as error:
        raise RuntimeError(f"Could not render {path.name}: {error}") from error


def render_hand(svg, side):
    document = ET.fromstring(svg)
    for child in list(document):
        # The upstream handshake exposes complete Lhand*/Rhand* vector layers,
        # including each layer's own clipping definitions. Keep their geometry.
        if not child.get("id", "").startswith(side + "hand"):
            document.remove(child)
    if not len(document):
        raise ValueError("Noto handshake layer structure changed")
    return render(ET.tostring(document))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=Path, help="generated Android assets directory")
    parser.add_argument("--jobs", type=int, default=2)
    args = parser.parse_args()
    if args.jobs < 1:
        parser.error("--jobs must be positive")
    if not features.check("webp"):
        raise SystemExit("Pillow needs WebP support to build Noto emoji")
    manifest = json.loads((SOURCES / "SOURCES.json").read_text())
    for relative, info in manifest["files"].items():
        data = (SOURCES / relative).read_bytes()
        if sha256(data).hexdigest() != info["sha256"]:
            raise ValueError("Noto source hash mismatch: " + relative)
        # Pinned inputs are self-contained; the build must never fetch images,
        # stylesheets, or fonts from the network.
        document = ET.fromstring(data)
        for reference in re.findall(r"url\(([^)]+)\)", data.decode()):
            if not reference.strip(" \t\r\n\"'").startswith("#"):
                raise ValueError("External SVG paint reference: " + relative)
        if "@import" in data.decode():
            raise ValueError("External SVG stylesheet: " + relative)
        for node in document.iter():
            if node.tag.rsplit("}", 1)[-1] in ("text", "script", "foreignObject"):
                raise ValueError("Non-vector Noto input: " + relative)
            for key, value in node.attrib.items():
                if key.rsplit("}", 1)[-1] == "href" and not value.startswith(("#", "data:image/")):
                    raise ValueError("External SVG reference: " + relative)
    inputs = []
    for page, emojis in enumerate(emoji_pages()):
        for index, emoji in enumerate(emojis):
            source = manifest["emoji_files"].get(sequence_name(emoji))
            if source is None:
                raise ValueError("Missing Noto source for " + emoji)
            inputs.append((page * 4096 + index, SOURCES / source))
    args.output.mkdir(parents=True, exist_ok=True)
    count = len(inputs)
    offset = 32 + count * 20
    entries = bytearray()
    images = bytearray()
    with ProcessPoolExecutor(max_workers=args.jobs) as pool:
        for key, image in pool.map(encode_record, inputs, chunksize=16):
            if not 0 < len(image) < 65536:
                raise ValueError("Emoji image does not fit an EPK3 record")
            # FULL_IMAGE=3: the existing reader decodes the complete WebP with
            # BitmapFactory. No palette, mask, predictor, or native change.
            entries += struct.pack("<HHIHHHHBBBB", key, 65535, offset, len(image),
                                   65535, 65535, 0, 3, 0, 0, 0)
            images += image
            offset += len(image)
            if len(entries) % (500 * 20) == 0:
                print(f"Rendered {len(entries) // 20}/{count} emoji", flush=True)
    header = struct.pack("<4sHH6I", b"EPK3", 3, 20, SIDE, SIDE, count, count, offset, 0)
    (args.output / "emoji.pack").write_bytes(header + entries + images)
    compound = args.output / "emoji/compound"
    compound.mkdir(parents=True, exist_ok=True)
    for skin in range(-1, 5):
        suffix = "" if skin == -1 else f"_{0x1F3FB + skin:x}"
        svg = (SOURCES / f"svg/emoji_u1f91d{suffix}.svg").read_bytes()
        for place, side in enumerate(("L", "R")):
            render_hand(svg, side).save(compound / f"0_{skin}_{place}.png", compress_level=9)
    for path in [args.output / "emoji.pack", *compound.glob("*.png")]:
        os.utime(path, (1, 1))
    print(f"Built {count} Noto emoji and 12 handshake layers ({offset:,} pack bytes)")


if __name__ == "__main__":
    main()
