#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
# Copyright (C) 2026 Ayuless contributors
"""Check Android translations and merge approved Crowdin downloads without losing existing entries."""
import argparse
from collections import Counter
from copy import deepcopy
from pathlib import Path
import re
import shutil
import xml.etree.ElementTree as ET

RESOURCES = Path('TMessagesProj/src/main/res')
STAGING = Path('.crowdin/translations')
FORMAT = re.compile(r'%(?:(\d+)\$)?([-#+ 0,(<]*)(?:\d+)?(?:\.\d+)?([tT][a-zA-Z]|[a-zA-Z%])')
LOCALE = re.compile(r'values-(?:[a-z]{2,3}(?:-r(?:[A-Z]{2}|\d{3}))?|b\+[A-Za-z0-9]+(?:\+[A-Za-z0-9]+)*)\Z')


def read(path):
    text = path.read_text(encoding='utf-8-sig')
    if len(text) > 1_000_000 or re.search(r'<!\s*(?:DOCTYPE|ENTITY)', text, re.I):
        raise ValueError(f'{path}: XML declarations/entities or oversized translation file')
    root = ET.fromstring(text)
    if root.tag != 'resources':
        raise ValueError(f'{path}: expected <resources>')
    result = {}
    for entry in root:
        name = entry.get('name')
        if entry.tag != 'string' or not name or name in result:
            raise ValueError(f'{path}: unsupported or duplicate resource {name!r}')
        result[name] = entry
    return result


def arguments(text):
    result = Counter()
    automatic, previous = 1, None
    for match in FORMAT.finditer(text):
        explicit, flags, conversion = match.groups()
        if conversion in ('%', 'n'):
            continue
        if '<' in flags:
            if previous is None:
                raise ValueError('format reuses an argument before it exists')
            index = previous
        elif explicit:
            index = int(explicit)
        else:
            index = automatic
            automatic += 1
        previous = index
        result[index, conversion] += 1
    return result


def content(entry):
    return ''.join(entry.itertext())


def validate(source, target, path):
    for key, translated in target.items():
        if key not in source:
            raise ValueError(f'{path}: unknown source key {key}')
        original = source[key]
        if original.get('translatable') == 'false' and content(translated) != content(original):
            raise ValueError(f'{path}: non-translatable branding changed: {key}')
        if content(original).strip() and not content(translated).strip():
            raise ValueError(f'{path}: empty translation for {key}')
        if original.get('formatted') != 'false' and arguments(content(original)) != arguments(content(translated)):
            raise ValueError(f'{path}: format argument mismatch for {key}')


def canonical(entry):
    return (entry.tag, tuple(sorted(entry.attrib.items())), entry.text or '',
            tuple((canonical(child), child.tail or '') for child in entry))


def check_all(resources):
    source = read(resources / 'values/ayuless.xml')
    for file in sorted(resources.glob('values-*/ayuless.xml')):
        validate(source, read(file), file)
    return source


def seed(resources=RESOURCES, staging=STAGING):
    check_all(resources)
    for file in resources.glob('values-*/ayuless.xml'):
        destination = staging / file.parent.name / file.name
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(file, destination)


def merge(resources=RESOURCES, staging=STAGING):
    source = check_all(resources)
    planned = []
    for file in sorted(staging.glob('values-*/ayuless.xml')):
        if not LOCALE.fullmatch(file.parent.name):
            raise ValueError(f'{file}: invalid Android locale directory')
        incoming = read(file)
        validate(source, incoming, file)
        destination = resources / file.parent.name / file.name
        existing = read(destination) if destination.exists() else {}
        combined = dict(existing)
        combined.update(incoming)
        if combined.keys() == existing.keys() and all(canonical(value) == canonical(existing[key]) for key, value in combined.items()):
            continue
        lines = ['<?xml version="1.0" encoding="utf-8"?>',
                 '<!-- SPDX-License-Identifier: GPL-3.0-or-later -->', '<resources>']
        for key in source:
            if key in combined:
                element = deepcopy(combined[key])
                element.tail = None
                lines.append('    ' + ET.tostring(element, encoding='unicode'))
        lines.append('</resources>')
        planned.append((destination, '\n'.join(lines) + '\n'))
    # Validate the complete batch before changing any repository translation.
    for destination, text in planned:
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_text(text, encoding='utf-8')
    check_all(resources)
    return len(planned)


def main():
    parser = argparse.ArgumentParser()
    actions = parser.add_mutually_exclusive_group()
    actions.add_argument('--seed', action='store_true')
    actions.add_argument('--merge', action='store_true')
    args = parser.parse_args()
    if args.seed:
        seed()
        print('Existing translations staged for first Crowdin upload')
    elif args.merge:
        print(f'Merged {merge()} translation files')
    else:
        source = check_all(RESOURCES)
        print(f'Translations checked against {len(source)} English strings')


if __name__ == '__main__':
    main()
