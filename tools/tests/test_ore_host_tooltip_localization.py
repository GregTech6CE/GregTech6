"""Reviewed sediment drop-policy display is bilingual and keeps its optional-tool argument."""
from pathlib import Path
import json,unittest
class OreHostTooltipLocalizationTests(unittest.TestCase):
 def test_bilingual_optional_tool_label(self):
  root=Path(__file__).resolve().parents[2]
  values={lang:json.loads((root/f'core/src/main/resources/assets/gt6ce/lang/{lang}.json').read_text())['tooltip.gt6ce.ore.hand_harvest_preferred'] for lang in ['en_us','zh_cn']}
  self.assertEqual(values['en_us'],'Hand-harvestable; %s is faster')
  self.assertEqual(values['zh_cn'],'可徒手采集；使用%s更快')
  for value in values.values():self.assertEqual(value.count('%s'),1)
 def test_policy_key_is_not_a_mandatory_harvest_level_label(self):
  root=Path(__file__).resolve().parents[2]
  for lang in ['en_us','zh_cn']:
   value=json.loads((root/f'core/src/main/resources/assets/gt6ce/lang/{lang}.json').read_text())['tooltip.gt6ce.ore.hand_harvest_preferred']
   self.assertNotIn('%d',value)
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from localization import (CONFIG_PATH, LANG_PATH, ORE_HOST_PROJECT_KEYS, ROOT,
                          expected_chinese, load_ore_host_project, read_json)


class OreHostProjectProvenance(unittest.TestCase):
    KEY = 'tooltip.gt6ce.ore.hand_harvest_preferred'
    ROW = {'english': 'Hand-harvestable; %s is faster',
           'value': '可徒手采集；使用%s更快'}

    def fixture(self, root, values):
        target = root / CONFIG_PATH / 'ore_host_project.json'
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps({'values': values}), encoding='utf-8')

    def test_no_config_fixture_remains_valid(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.assertEqual({}, load_ore_host_project(root))
            self.assertEqual({'example': 'Example'}, expected_chinese({'example': 'Example'}, {}, {}, repo=root))

    def test_exact_allowlist_and_format_enforced(self):
        cases = [
            ('tooltip.gt6ce.ore.unrelated', self.ROW, 'outside exact allowlist'),
            (self.KEY, {'english': 'Presence %s', 'value': '存在'}, 'format arguments differ'),
            (self.KEY, {'english': 'Presence %s', 'value': '存在%2$s'}, 'format arguments differ'),
            (self.KEY, {'english': 'Presence', 'value': ''}, 'Incomplete'),
        ]
        for key, row, error in cases:
            with self.subTest(key=key, row=row), tempfile.TemporaryDirectory() as tmp:
                root = Path(tmp)
                self.fixture(root, {key: row})
                with self.assertRaisesRegex(ValueError, error):
                    load_ore_host_project(root)

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
                with self.subTest(source=source), self.assertRaisesRegex(ValueError, 'overrides source/adopted/Jade/instrument/recipe binding'):
                    expected_chinese({self.KEY: self.ROW['english']}, source, aliases, repo=root)

    def test_other_project_and_adopted_collisions_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.fixture(root, {self.KEY: self.ROW})
            for loader in ('load_adopted', 'load_jade_project', 'load_instrument_project', 'load_recipe_project'):
                with self.subTest(loader=loader), patch('localization.' + loader, return_value={self.KEY: self.ROW}):
                    with self.assertRaisesRegex(ValueError, 'overrides source/adopted/Jade/instrument/recipe binding'):
                        expected_chinese({self.KEY: self.ROW['english']}, {}, {}, repo=root)

    def test_actual_exact_mapping_matches_both_languages(self):
        rows = load_ore_host_project(ROOT)
        self.assertEqual(ORE_HOST_PROJECT_KEYS, rows.keys())
        self.assertEqual(self.ROW, rows[self.KEY])
        for locale, field in [('en_us', 'english'), ('zh_cn', 'value')]:
            self.assertEqual(self.ROW[field], read_json(ROOT / LANG_PATH / (locale + '.json'))[self.KEY])



if __name__=='__main__':unittest.main()
