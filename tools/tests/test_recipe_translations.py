"""One user-confirmed recipe presence tooltip, without source-policy escape."""
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from localization import (CONFIG_PATH, LANG_PATH, RECIPE_PROJECT_KEYS, ROOT,
                          expected_chinese, load_recipe_project, read_json)


class RecipeTranslations(unittest.TestCase):
    KEY = 'gt6ce.jei.fluid_presence'
    ROW = {'english': 'Presence only (0 mB consumed; no amount reserved)',
           'value': '仅需存在该流体（消耗 0 mB，不额外预留）'}

    def fixture(self, root, values):
        target = root / CONFIG_PATH / 'recipe_project.json'
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps({'values': values}), encoding='utf-8')

    def test_no_config_fixture_remains_valid(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.assertEqual({}, load_recipe_project(root))
            self.assertEqual({'example': 'Example'}, expected_chinese({'example': 'Example'}, {}, {}, repo=root))

    def test_exact_allowlist_and_format_enforced(self):
        cases = [
            ('gt6ce.jei.unrelated', self.ROW, 'outside exact allowlist'),
            (self.KEY, {'english': 'Presence %s', 'value': '存在'}, 'format arguments differ'),
            (self.KEY, {'english': 'Presence %s', 'value': '存在%2$s'}, 'format arguments differ'),
            (self.KEY, {'english': 'Presence', 'value': ''}, 'Incomplete'),
        ]
        for key, row, error in cases:
            with self.subTest(key=key, row=row), tempfile.TemporaryDirectory() as tmp:
                root = Path(tmp)
                self.fixture(root, {key: row})
                with self.assertRaisesRegex(ValueError, error):
                    load_recipe_project(root)

    def test_english_drift_and_missing_key_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.fixture(root, {self.KEY: self.ROW})
            for english in ({}, {self.KEY: 'Consumes 1 mB'}):
                with self.subTest(english=english), self.assertRaisesRegex(ValueError, 'English drifted or key missing'):
                    expected_chinese(english, {}, {}, repo=root)

    def test_original_source_and_alias_binding_cannot_be_overwritten(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.fixture(root, {self.KEY: self.ROW})
            for source, aliases in [({self.KEY: '源'}, {}), ({'original': '源'}, {self.KEY: 'original'})]:
                with self.subTest(source=source), self.assertRaisesRegex(ValueError, 'overrides source/adopted/Jade/instrument binding'):
                    expected_chinese({self.KEY: self.ROW['english']}, source, aliases, repo=root)

    def test_other_project_and_adopted_collisions_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.fixture(root, {self.KEY: self.ROW})
            for loader in ('load_adopted', 'load_jade_project', 'load_instrument_project'):
                with self.subTest(loader=loader), patch('localization.' + loader, return_value={self.KEY: self.ROW}):
                    with self.assertRaisesRegex(ValueError, 'overrides source/adopted/Jade/instrument binding'):
                        expected_chinese({self.KEY: self.ROW['english']}, {}, {}, repo=root)

    def test_actual_exact_mapping_matches_both_languages(self):
        rows = load_recipe_project(ROOT)
        self.assertEqual(RECIPE_PROJECT_KEYS, rows.keys())
        self.assertEqual(self.ROW, rows[self.KEY])
        for locale, field in [('en_us', 'english'), ('zh_cn', 'value')]:
            self.assertEqual(self.ROW[field], read_json(ROOT / LANG_PATH / (locale + '.json'))[self.KEY])


if __name__ == '__main__':
    unittest.main()
