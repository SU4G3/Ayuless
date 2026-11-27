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

    def test_upstream_override_removed_without_reformatting_or_losing_notices(self):
        upstream = self.resources / 'values-fr/strings.xml'
        upstream.parent.mkdir(parents=True)
        keep = '<string name="Keep">Keep &amp; preserve</string>'
        upstream.write_text('<?xml version="1.0"?>\n<!-- original copyright -->\n<resources>\n'
                            '    <string name="Other">Old <![CDATA[</string>]]> value</string>\n'
                            '    ' + keep + '\n</resources>\n')
        self.write(self.staging / 'values-fr/ayuless.xml', '<string name="Other">Autre</string>')
        self.assertEqual(sync.merge(self.resources, self.staging), 2)
        self.assertEqual(upstream.read_text(), '<?xml version="1.0"?>\n<!-- original copyright -->\n<resources>\n    ' + keep + '\n</resources>\n')
        self.assertEqual(sync.merge(self.resources, self.staging), 0)

    def test_existing_duplicate_is_rejected_and_can_be_repaired(self):
        self.write(self.resources / 'values-ru/strings.xml', '<string name="Other"/><string name="Keep">Keep</string>')
        with self.assertRaises(ValueError):
            sync.check_all(self.resources)
        self.assertEqual(sync.merge(self.resources, self.staging), 1)
        sync.check_all(self.resources)

    def test_unrelated_crlf_upstream_is_not_rewritten(self):
        file = self.resources / 'values-ru/strings.xml'
        original = b'<resources>\r\n    <string name="Keep">Keep</string>\r\n</resources>\r\n'
        file.write_bytes(original)
        self.assertEqual(sync.merge(self.resources, self.staging), 0)
        self.assertEqual(file.read_bytes(), original)


if __name__ == '__main__':
    unittest.main()
