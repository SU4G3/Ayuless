#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
# Copyright (C) 2026 Ayuless contributors
"""Generate Ayuless icon sources (SVG + Android vector drawables).

Run: python3 branding/generate.py
PNG export: magick -background none -density 384 branding/ayuless-icon.svg -resize 512x512 branding/ayuless-icon-512.png
"""
import math
import os

OUT = os.path.dirname(os.path.abspath(__file__))
SIZE = 640
BG = "#251D41"
BLOB = "#3A2C66"
CHEVRON = "#F5F3FF"
DOT = "#B79CFF"


def wavy_blob(cx, cy, r, waves, amp, phase=0.0, steps=240):
    pts = []
    for i in range(steps):
        t = 2 * math.pi * i / steps
        rr = r + amp * math.sin(waves * t + phase) + amp * 0.4 * math.sin(2 * waves * t + phase * 1.7)
        pts.append((cx + rr * math.cos(t), cy + rr * math.sin(t)))
    d = "M%.1f,%.1f " % pts[0] + " ".join("L%.1f,%.1f" % p for p in pts[1:]) + " Z"
    return d


BLOBS = [
    wavy_blob(615, 75, 215, 11, 9, 0.3),
    wavy_blob(25, 610, 190, 10, 9, 1.1),
]
CHEVRON_D = "M165,398 L320,253 L475,398"
CHEVRON_W = 72
DOT_C = (320, 452, 37)


def svg(background=True, foreground=True, rounded=False):
    parts = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{SIZE}" height="{SIZE}" viewBox="0 0 {SIZE} {SIZE}">']
    if rounded:
        parts.append(f'<clipPath id="c"><rect width="{SIZE}" height="{SIZE}" rx="140"/></clipPath><g clip-path="url(#c)">')
    if background:
        parts.append(f'<rect width="{SIZE}" height="{SIZE}" fill="{BG}"/>')
        parts += [f'<path d="{d}" fill="{BLOB}"/>' for d in BLOBS]
    if foreground:
        parts.append(f'<path d="{CHEVRON_D}" fill="none" stroke="{CHEVRON}" stroke-width="{CHEVRON_W}" stroke-linecap="round" stroke-linejoin="round"/>')
        parts.append(f'<circle cx="{DOT_C[0]}" cy="{DOT_C[1]}" r="{DOT_C[2]}" fill="{DOT}"/>')
    if rounded:
        parts.append("</g>")
    parts.append("</svg>")
    return "\n".join(parts) + "\n"


def circle_path(cx, cy, r):
    return f"M{cx - r},{cy} a{r},{r} 0 1,0 {2 * r},0 a{r},{r} 0 1,0 {-2 * r},0 Z"


VD_HEAD = ('<?xml version="1.0" encoding="utf-8"?>\n<!-- SPDX-License-Identifier: GPL-3.0-or-later -->\n'
           '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
           '    android:width="108dp" android:height="108dp"\n'
           f'    android:viewportWidth="{SIZE}" android:viewportHeight="{SIZE}">\n')


def vd_background():
    s = VD_HEAD + f'    <path android:fillColor="{BG}" android:pathData="M0,0h{SIZE}v{SIZE}h-{SIZE}z"/>\n'
    s += "".join(f'    <path android:fillColor="{BLOB}" android:pathData="{d}"/>\n' for d in BLOBS)
    return s + "</vector>\n"


def vd_foreground():
    # Adaptive icons mask to a 66dp-diameter safe zone out of 108dp: scale glyph to fit.
    s = VD_HEAD + f'    <group android:scaleX="0.72" android:scaleY="0.72" android:pivotX="{SIZE // 2}" android:pivotY="{SIZE // 2 + 25}">\n'
    s += (f'        <path android:strokeColor="{CHEVRON}" android:strokeWidth="{CHEVRON_W}" android:strokeLineCap="round"'
          f' android:strokeLineJoin="round" android:pathData="{CHEVRON_D}"/>\n')
    s += f'        <path android:fillColor="{DOT}" android:pathData="{circle_path(*DOT_C)}"/>\n'
    return s + "    </group>\n</vector>\n"


def write(name, text):
    with open(os.path.join(OUT, name), "w") as f:
        f.write(text)


write("ayuless-icon.svg", svg())
write("ayuless-icon-rounded.svg", svg(rounded=True))
os.makedirs(os.path.join(OUT, "android"), exist_ok=True)
write("android/ic_launcher_ayuless_background.xml", vd_background())
write("android/ic_launcher_ayuless_foreground.xml", vd_foreground())
write("android/ic_launcher_ayuless.xml",
      '<?xml version="1.0" encoding="utf-8"?>\n<!-- SPDX-License-Identifier: GPL-3.0-or-later -->\n'
      '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
      '    <background android:drawable="@drawable/ic_launcher_ayuless_background"/>\n'
      '    <foreground android:drawable="@drawable/ic_launcher_ayuless_foreground"/>\n'
      '    <monochrome android:drawable="@drawable/ic_launcher_ayuless_foreground"/>\n'
      '</adaptive-icon>\n')
