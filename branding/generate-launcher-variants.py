#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
# Copyright (C) 2026 Ayuless contributors
"""Render independent Ayuless launcher icons. Requires ImageMagick with SVG support."""
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / 'TMessagesProj/src/main/res'
OUT = ROOT / 'branding/launcher-variants'
DENSITIES = {'mdpi': 48, 'hdpi': 72, 'xhdpi': 96, 'xxhdpi': 144, 'xxxhdpi': 192}
VARIANTS = {
    'classic': {'colors': ['#1D93D2', '#37AFE3'], 'angle': 90, 'fold': '#D2E4F1'},
    'ocean': {'colors': ['#137BE5', '#02A3CC', '#00DC6B'], 'angle': 45, 'fold': None},
    'premium': {'colors': ['#5381FE', '#A278FF', '#FC5DDC'], 'angle': 90, 'fold': None,
                'decor': [('M200,209 L206,224 L221,230 L206,236 L200,251 L194,236 L179,230 L194,224 Z', '#FFFFFF'),
                          ('M443,193 L447,203 L457,207 L447,211 L443,221 L439,211 L429,207 L439,203 Z', '#FFFFFF')]},
    'turbo': {'colors': ['#9651FF', '#3A8FFF', '#49F9FA'], 'angle': 45, 'fold': None,
              'decor': [('M134,300 h62 a8,8 0 0,1 0,16 h-62 a8,8 0 0,1 0,-16 Z', '#A6FFFFFF'),
                        ('M116,344 h48 a8,8 0 0,1 0,16 h-48 a8,8 0 0,1 0,-16 Z', '#A6FFFFFF'),
                        ('M134,388 h22 a8,8 0 0,1 0,16 h-22 a8,8 0 0,1 0,-16 Z', '#A6FFFFFF')]},
    'night': {'colors': ['#0D1726', '#1C2D43'], 'angle': 90, 'fold': None,
              'decor': [('M190,195 L196,211 L212,217 L196,223 L190,239 L184,223 L168,217 L184,211 Z', '#FFFFFF'),
                        ('M440,174 L444,185 L455,189 L444,193 L440,204 L436,193 L425,189 L436,185 Z', '#A6FFFFFF'),
                        ('M478,456 L483,469 L496,474 L483,479 L478,492 L473,479 L460,474 L473,469 Z', '#FFFFFF'),
                        ('M176,482 L180,493 L191,497 L180,501 L176,512 L172,501 L161,497 L172,493 Z', '#80FFFFFF')]},
}
HEAD = '<?xml version="1.0" encoding="utf-8"?>\n<!-- SPDX-License-Identifier: GPL-3.0-or-later -->\n'
ANDROID = 'http://schemas.android.com/apk/res/android'


def decorations(spec, android=False):
    result = ''
    for path, color in spec.get('decor', []):
        if android:
            result += f'<path android:fillColor="{color}" android:pathData="{path}"/>\n'
        else:
            opacity = int(color[1:3], 16) / 255 if len(color) == 9 else 1
            rgb = '#' + color[3:] if len(color) == 9 else color
            result += f'<path d="{path}" fill="{rgb}" fill-opacity="{opacity}"/>'
    return result


def foreground(spec):
    fold = spec['fold']
    paths = decorations(spec) + '<path d="M165 398L320 253L475 398" fill="none" stroke="#FFFFFF" stroke-width="72" stroke-linecap="round" stroke-linejoin="round"/>'
    if fold:
        paths += f'<path d="M320 253L475 398" fill="none" stroke="{fold}" stroke-width="72" stroke-linecap="round"/>'
        paths += '<path d="M165 398L320 253" fill="none" stroke="#FFFFFF" stroke-width="72" stroke-linecap="round"/>'
    paths += '<circle cx="320" cy="452" r="37" fill="#FFFFFF"/>'
    return '<g>' + paths + '</g>'


def generate(name, spec):
    prefix = 'ic_launcher_ayuless_' + name
    colors = spec['colors']
    stops = ''.join(f'<stop offset="{i / (len(colors) - 1)}" stop-color="{color}"/>' for i, color in enumerate(colors))
    coords = 'x1="0" y1="1" x2="0" y2="0"' if spec['angle'] == 90 else 'x1="0" y1="1" x2="1" y2="0"'
    svg = '<svg xmlns="http://www.w3.org/2000/svg" width="640" height="640" viewBox="0 0 640 640"><defs><linearGradient id="bg" ' + coords + '>' + stops + '</linearGradient></defs><rect width="640" height="640" fill="url(#bg)"/>' + foreground(spec) + '</svg>'
    (OUT / (name + '.svg')).write_text(svg)
    bg = HEAD + f'<shape xmlns:android="{ANDROID}" android:shape="rectangle"><gradient android:type="linear" android:angle="{spec["angle"]}" android:startColor="{colors[0]}" android:endColor="{colors[-1]}"'
    if len(colors) == 3:
        bg += f' android:centerColor="{colors[1]}"'
    (RES / 'drawable' / (prefix + '_background.xml')).write_text(bg + '/></shape>\n')
    fg = HEAD + f'<vector xmlns:android="{ANDROID}" android:width="108dp" android:height="108dp" android:viewportWidth="640" android:viewportHeight="640"><group android:scaleX="0.72" android:scaleY="0.72" android:pivotX="320" android:pivotY="345">\n'
    fg += decorations(spec, android=True)
    fg += '<path android:fillColor="#00000000" android:strokeColor="#FFFFFFFF" android:strokeWidth="72" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="M165,398 L320,253 L475,398"/>\n'
    if spec['fold']:
        fg += f'<path android:fillColor="#00000000" android:strokeColor="{spec["fold"]}" android:strokeWidth="72" android:strokeLineCap="round" android:pathData="M320,253 L475,398"/>\n'
        fg += '<path android:fillColor="#00000000" android:strokeColor="#FFFFFFFF" android:strokeWidth="72" android:strokeLineCap="round" android:pathData="M165,398 L320,253"/>\n'
    fg += '<path android:fillColor="#FFFFFFFF" android:pathData="M283,452 a37,37 0 1,0 74,0 a37,37 0 1,0 -74,0 Z"/>\n</group></vector>\n'
    (RES / 'drawable' / (prefix + '_foreground.xml')).write_text(fg)
    adaptive = HEAD + f'<adaptive-icon xmlns:android="{ANDROID}"><background android:drawable="@drawable/{prefix}_background"/><foreground android:drawable="@drawable/{prefix}_foreground"/><monochrome android:drawable="@drawable/{prefix}_foreground"/></adaptive-icon>\n'
    for suffix in ['', '_round']:
        (RES / 'mipmap-anydpi-v26' / (prefix + suffix + '.xml')).write_text(adaptive)
    # Match the full-size legacy default glyph; adaptive vectors above apply their safe-zone inset.
    # Round fallback uses a circular alpha mask; ordinary fallback retains the square for launcher masking.
    for density, size in DENSITIES.items():
        target = RES / ('mipmap-' + density)
        png = target / (prefix + '.png')
        subprocess.run(['magick', '-background', 'none', '-density', '288', str(OUT / (name + '.svg')), '-resize', f'{size}x{size}', '-depth', '8', '-strip', str(png)], check=True)
        subprocess.run(['magick', str(png), '(', '-size', f'{size}x{size}', 'xc:black', '-fill', 'white', '-draw', f'circle {size/2},{size/2} {size/2},0', ')', '-alpha', 'off', '-compose', 'CopyOpacity', '-composite', '-depth', '8', '-strip', str(target / (prefix + '_round.png'))], check=True)


if __name__ == '__main__':
    OUT.mkdir(exist_ok=True)
    for name, spec in VARIANTS.items():
        generate(name, spec)
