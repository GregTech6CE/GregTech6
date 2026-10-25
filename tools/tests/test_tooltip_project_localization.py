"""Source-first translations must survive regeneration without hiding English drift."""
import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import localization as lang


class TooltipProjectLocalizationTests(unittest.TestCase):
    KEY = 'gt.tooltip.usb.material_data'
    ROW = {'english': 'Material Data: %s', 'value': '材料数据：%s'}

    def fixture(self, root, filename, values):
        path = root / lang.CONFIG_PATH / filename
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps({'values': values}), encoding='utf-8')

    def test_project_cannot_override_source_or_alias(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.fixture(root, 'tooltip_project.json', {self.KEY: self.ROW})
            for source, aliases in [({self.KEY: '原文%s'}, {}), ({'original': '原文%s'}, {self.KEY: 'original'})]:
                with self.subTest(source=source), self.assertRaisesRegex(ValueError, 'overrides existing'):
                    lang.expected_chinese({self.KEY: self.ROW['english']}, source, aliases, repo=root)

    def test_drift_missing_key_and_lost_arguments_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.fixture(root, 'tooltip_project.json', {self.KEY: self.ROW})
            for english in ({}, {self.KEY: 'Different data: %s'}):
                with self.subTest(english=english), self.assertRaisesRegex(ValueError, 'English drifted'):
                    lang.expected_chinese(english, {}, {}, repo=root)
            self.fixture(root, 'tooltip_project.json', {self.KEY: dict(self.ROW, value='材料数据')})
            with self.assertRaisesRegex(ValueError, 'format arguments differ'):
                lang.load_tooltip_project(root)

    def test_actual_catalog_preserves_all_original_entries(self):
        source, _ = lang.load_source()
        english = lang.read_json(lang.ROOT / lang.LANG_PATH / 'en_us.json')
        chinese = lang.read_json(lang.ROOT / lang.LANG_PATH / 'zh_cn.json')
        aliases = lang.read_json(lang.ROOT / lang.CONFIG_PATH / 'aliases.json')
        generated = lang.expected_chinese(english, source, aliases)
        self.assertEqual(chinese, generated)
        self.assertFalse({key for key, value in source.items() if generated[key] != value})
        for key in english:
            if key.startswith('gt.tooltip.usb.'):
                self.assertNotEqual(english[key], generated[key], key)
        self.assertEqual('硅岩金属', generated['tooltip.gt6ce.ui.rod.fuel_type.naquadah'])
        self.assertEqual('浸洗器', generated['gt.recipe.bath'])

    def test_form_samples_must_agree_and_original_identity_wins(self):
        english = {'item.gt6ce.casingMachineDense': 'Dense %s Machine Casing'}
        key = next(iter(english))
        source = {'gt.material.Iron': '铁', 'gt.material.Steel': '钢',
                  'oredict.casingMachineDenseIron': '致密铁机械外壳',
                  'oredict.casingMachineDenseSteel': '致密钢机械外壳'}
        aliases = {'itemGroup.gt6ce.casingMachineDense': 'itemGroup.casingMachineDense'}
        row = {'english': english[key], 'source_key': 'oredict.casingMachineDenseIron',
               'material_key': 'gt.material.Iron', 'cross_check_source_key': 'oredict.casingMachineDenseSteel',
               'cross_check_material_key': 'gt.material.Steel'}
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.fixture(root, 'form_templates.json', {key: row})
            self.assertEqual('致密蓝晶石机械外壳', lang.material_form_templates(english, source, aliases, root)[key] % '蓝晶石')
            with self.assertRaisesRegex(ValueError, 'overrides original identity'):
                lang.material_form_templates(english, source, dict(aliases, **{key: row['source_key']}), root)
            source[row['cross_check_source_key']] = '钢机械外壳'
            with self.assertRaisesRegex(ValueError, 'samples disagree'):
                lang.material_form_templates(english, source, aliases, root)


if __name__ == '__main__':
    unittest.main()
