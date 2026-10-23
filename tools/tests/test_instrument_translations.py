"""Narrow functional instrument localization; no general source-policy escape."""
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from localization import (CONFIG_PATH, INSTRUMENT_PROJECT_KEYS, LANG_PATH, ROOT,
                          expected_chinese, format_arguments, load_instrument_project,
                          load_source, read_json)


class InstrumentTranslations(unittest.TestCase):
    KEY = 'message.gt6ce.meter.input_side'
    ROW = {'english': 'Accepts from %s', 'value': '从%s输入'}

    def fixture(self, root, values):
        target = root / CONFIG_PATH / 'instrument_project.json'
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps({'values': values}), encoding='utf-8')

    def test_no_config_fixture_remains_valid(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.assertEqual({}, load_instrument_project(root))
            self.assertEqual({'example': 'Example'}, expected_chinese({'example': 'Example'}, {}, {}, repo=root))

    def test_exact_allowlist_and_format_enforced(self):
        cases = [
            ('message.gt6ce.unrelated', self.ROW, 'outside exact allowlist'),
            (self.KEY, {'english': 'Accepts from %s', 'value': '输入'}, 'format arguments differ'),
            (self.KEY, {'english': 'Accepts from %s', 'value': '从%2$s输入'}, 'format arguments differ'),
            (self.KEY, {'english': 'Accepts from %s', 'value': ''}, 'Incomplete'),
        ]
        for key, row, error in cases:
            with self.subTest(key=key, row=row), tempfile.TemporaryDirectory() as tmp:
                root = Path(tmp)
                self.fixture(root, {key: row})
                with self.assertRaisesRegex(ValueError, error):
                    load_instrument_project(root)

    def test_english_drift_and_missing_key_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.fixture(root, {self.KEY: self.ROW})
            for english in ({}, {self.KEY: 'Changed %s'}):
                with self.subTest(english=english), self.assertRaisesRegex(ValueError, 'English drifted or key missing'):
                    expected_chinese(english, {}, {}, repo=root)

    def test_original_source_and_alias_binding_cannot_be_overwritten(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.fixture(root, {self.KEY: self.ROW})
            for source, aliases in [({self.KEY: '源%s'}, {}), ({'original': '源%s'}, {self.KEY: 'original'})]:
                with self.subTest(source=source), self.assertRaisesRegex(ValueError, 'overrides source/adopted/Jade binding'):
                    expected_chinese({self.KEY: self.ROW['english']}, source, aliases, repo=root)

    def test_adopted_and_jade_collisions_cannot_be_overwritten(self):
        # Disjoint present-day scopes still have explicit collision protection.
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.fixture(root, {self.KEY: self.ROW})
            for loader in ('load_adopted', 'load_jade_project'):
                with self.subTest(loader=loader), patch('localization.' + loader, return_value={self.KEY: self.ROW}):
                    with self.assertRaisesRegex(ValueError, 'overrides source/adopted/Jade binding'):
                        expected_chinese({self.KEY: self.ROW['english']}, {}, {}, repo=root)

    def test_actual_mapping_is_exact_and_matches_languages(self):
        rows = load_instrument_project(ROOT)
        self.assertEqual(INSTRUMENT_PROJECT_KEYS, rows.keys())
        english = read_json(ROOT / LANG_PATH / 'en_us.json')
        chinese = read_json(ROOT / LANG_PATH / 'zh_cn.json')
        source, metadata = load_source()
        project = read_json(ROOT / CONFIG_PATH / 'instrument_project.json')
        self.assertEqual(metadata['sha256'], project['reference_sha256'])
        for key, row in rows.items():
            with self.subTest(key=key):
                self.assertNotIn(key, source)
                self.assertEqual(row['english'], english[key])
                self.assertEqual(row['value'], chinese[key])
                self.assertEqual({1}, format_arguments(row['english']))
                self.assertEqual({1}, format_arguments(row['value']))
        for direction in ('down', 'up', 'north', 'south', 'west', 'east'):
            self.assertTrue(english['jade.gt6ce.direction.' + direction])
            self.assertTrue(chinese['jade.gt6ce.direction.' + direction])


if __name__ == '__main__':
    unittest.main()
