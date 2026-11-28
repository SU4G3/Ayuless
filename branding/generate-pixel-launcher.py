#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Package the approved Pixel artwork for adaptive and legacy launchers."""
from pathlib import Path
import shutil
import subprocess

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / 'TMessagesProj/src/main/res'
SOURCE = ROOT / 'branding/launcher-variants/pixel.png'
PREFIX = 'ic_launcher_ayuless_pixel'
HEADER = '<?xml version="1.0" encoding="utf-8"?>\n'
ANDROID = 'http://schemas.android.com/apk/res/android'


def generate():
    (RES / 'drawable-nodpi').mkdir(exist_ok=True)
    shutil.copyfile(SOURCE, RES / 'drawable-nodpi' / (PREFIX + '_art.png'))
    # Artwork already includes safe-zone padding. Keep it on the masked background
    # so the existing picker does not draw opaque corners over its circular clip.
    (RES / 'drawable' / (PREFIX + '_background.xml')).write_text(
        HEADER + f'<bitmap xmlns:android="{ANDROID}" android:src="@drawable/{PREFIX}_art" android:gravity="fill" android:filter="true"/>\n')
    (RES / 'drawable' / (PREFIX + '_foreground.xml')).write_text(
        HEADER + f'<shape xmlns:android="{ANDROID}" android:shape="rectangle"><solid android:color="#00000000"/></shape>\n')
    adaptive = HEADER + f'<adaptive-icon xmlns:android="{ANDROID}"><background android:drawable="@drawable/{PREFIX}_background"/><foreground android:drawable="@drawable/{PREFIX}_foreground"/><monochrome android:drawable="@drawable/ic_launcher_ayuless_foreground"/></adaptive-icon>\n'
    for suffix in ('', '_round'):
        (RES / 'mipmap-anydpi-v26' / (PREFIX + suffix + '.xml')).write_text(adaptive)
    # Adaptive icon layers are 108dp; the static visible viewport is 72dp.
    for density, size in {'mdpi': 48, 'hdpi': 72, 'xhdpi': 96, 'xxhdpi': 144, 'xxxhdpi': 192}.items():
        target = RES / ('mipmap-' + density)
        png = target / (PREFIX + '.png')
        subprocess.run(['magick', str(SOURCE), '-gravity', 'center', '-crop', '66.6667%x66.6667%+0+0', '+repage', '-resize', f'{size}x{size}', '-depth', '8', '-strip', str(png)], check=True)
        subprocess.run(['magick', str(png), '(', '-size', f'{size}x{size}', 'xc:black', '-fill', 'white', '-draw', f'circle {size/2},{size/2} {size/2},0', ')', '-alpha', 'off', '-compose', 'CopyOpacity', '-composite', '-depth', '8', '-strip', str(target / (PREFIX + '_round.png'))], check=True)


if __name__ == '__main__':
    generate()
