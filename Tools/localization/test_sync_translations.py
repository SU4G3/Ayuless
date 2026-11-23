# SPDX-License-Identifier: GPL-3.0-or-later
# Copyright (C) 2026 Ayuless contributors
from pathlib import Path
import tempfile
import unittest

import sync_translations as sync


class TranslationTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.resources = Path(self.temp.name) / 'res'
        self.staging = Path(self.temp.name) / 'incoming'
        self.write(self.resources / 'values/ayuless.xml', '<string name="Greeting">Hello %1$s</string><string name="Other">Other</string><string name="Brand" translatable="false">Ayuless</string>')
        self.write(self.resources / 'values-ru/ayuless.xml', '<string name="Greeting">Привет %1$s</string><string name="Other">Другое</string>')

    def write(self, path, entries):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text('<resources>' + entries + '</resources>')

    def test_partial_download_keeps_existing_translation(self):
        self.write(self.staging / 'values-ru/ayuless.xml', '<string name="Greeting">Здравствуйте %1$s</string>')
        self.assertEqual(sync.merge(self.resources, self.staging), 1)
        result = sync.read(self.resources / 'values-ru/ayuless.xml')
        self.assertEqual(sync.content(result['Other']), 'Другое')
        self.assertEqual(sync.content(result['Greeting']), 'Здравствуйте %1$s')
        self.assertEqual(sync.merge(self.resources, self.staging), 0)

    def test_new_partial_locale(self):
        self.write(self.staging / 'values-fr/ayuless.xml', '<string name="Other">Autre</string>')
        sync.merge(self.resources, self.staging)
        self.assertEqual(set(sync.read(self.resources / 'values-fr/ayuless.xml')), {'Other'})

    def test_invalid_batch_does_not_modify_valid_target(self):
        self.write(self.staging / 'values-ru/ayuless.xml', '<string name="Other">Изменено</string>')
        self.write(self.staging / 'values-zz/ayuless.xml', '<string name="Greeting">Wrong %1$d</string>')
        file = self.resources / 'values-ru/ayuless.xml'
        before = file.read_bytes()
        with self.assertRaises(ValueError):
            sync.merge(self.resources, self.staging)
        self.assertEqual(before, file.read_bytes())

    def test_unknown_empty_and_brand_changes_rejected(self):
        source = sync.read(self.resources / 'values/ayuless.xml')
        for xml in ['<string name="Unknown">Text</string>', '<string name="Other"></string>', '<string name="Brand">Something else</string>']:
            file = self.staging / 'values-fr/ayuless.xml'
            self.write(file, xml)
            with self.assertRaises(ValueError):
                sync.validate(source, sync.read(file), file)

    def test_duplicate_and_entity_rejected(self):
        file = self.staging / 'bad.xml'
        self.write(file, '<string name="Other">A</string><string name="Other">B</string>')
        with self.assertRaises(ValueError):
            sync.read(file)
        file.write_text('<!DOCTYPE resources [<!ENTITY value "bad">]><resources><string name="Other">&value;</string></resources>')
        with self.assertRaises(ValueError):
            sync.read(file)

    def test_format_reordering_and_reuse(self):
        self.assertEqual(sync.arguments('%s %d %%'), sync.arguments('%2$d %1$s %%'))
        self.assertEqual(sync.arguments('%1$s %<s'), sync.arguments('%1$s %1$s'))
        self.assertNotEqual(sync.arguments('%1$s'), sync.arguments('%1$d'))

    def test_seed_leaves_repository_unchanged(self):
        file = self.resources / 'values-ru/ayuless.xml'
        before = file.read_bytes()
        sync.seed(self.resources, self.staging)
        self.assertEqual((self.staging / 'values-ru/ayuless.xml').read_bytes(), before)
        self.assertEqual(file.read_bytes(), before)


if __name__ == '__main__':
    unittest.main()
