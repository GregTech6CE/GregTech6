
# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[1]))
from native_layout import native_path

import json
import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


class JadeTranslations(unittest.TestCase):
    def test_provider_config_names_exist_in_supported_languages(self):
        keys = set()
        for prefix in ('src', 'neoforge/src'):
            providers = native_path(ROOT / prefix / 'main/java/com/gregtech/gregtech/integration/jade')
            platform_keys = set()
            for path in providers.glob('*Provider.java'):
                source = path.read_text(encoding='utf-8')
                identifiers = re.findall(
                    r'ResourceLocation\.fromNamespaceAndPath\("([^"]+)",\s*"([^"]+)"\)', source)
                self.assertTrue(identifiers, f'Cannot determine Jade provider UID: {path.name}')
                platform_keys.update(f'config.jade.plugin_{namespace}.{name}' for namespace, name in identifiers)
            self.assertTrue(platform_keys, f'No Jade providers in {prefix}')
            keys.update(platform_keys)
        for locale in ('en_us', 'zh_cn'):
            translations = json.loads((ROOT / f'core/src/main/resources/assets/gt6ce/lang/{locale}.json')
                                      .read_text(encoding='utf-8'))
            for key in sorted(keys):
                with self.subTest(locale=locale, key=key):
                    self.assertTrue(translations.get(key, '').strip(), f'Missing config translation: {key}')

    def test_all_jade_text_is_chinese_or_format_only(self):
        from localization import read_json, LANG_PATH, format_arguments, is_jade_project_key
        english = read_json(ROOT / LANG_PATH / 'en_us.json')
        chinese = read_json(ROOT / LANG_PATH / 'zh_cn.json')
        for key, value in english.items():
            if not is_jade_project_key(key):
                continue
            with self.subTest(key=key):
                self.assertEqual(format_arguments(value), format_arguments(chinese[key]))
                # Format-only rows contain no prose to translate; units remain intact.
                prose = re.sub(r'%\d*\$?[\d.]*[sdf]|mB|HU|KU|EU|RU|TU|K|L|U|T', '', value)
                if re.search(r'[A-Za-z]{2,}', prose):
                    self.assertRegex(chinese[key], r'[\u3400-\u9fff]')

    def test_project_jade_scope_and_parameters_are_enforced(self):
        import tempfile
        from localization import load_jade_project
        cases = [
            ('item.gt6ce.example', {'english': 'Example', 'value': '示例'}),
            ('jade.gt6ce.example', {'english': 'Heat: %s', 'value': '热量'}),
        ]
        for key, row in cases:
            with self.subTest(key=key), tempfile.TemporaryDirectory() as tmp:
                root = Path(tmp)
                target = root / 'tools/localization/jade_project.json'
                target.parent.mkdir(parents=True)
                target.write_text(json.dumps({'values': {key: row}}), encoding='utf-8')
                with self.assertRaises(ValueError):
                    load_jade_project(root)

    def test_jade_reference_matches_pinned_source_terminology(self):
        from localization import read_json, load_source, CONFIG_PATH
        project = read_json(ROOT / CONFIG_PATH / 'jade_project.json')
        source, metadata = load_source()
        self.assertEqual(project['reference_sha256'], metadata['sha256'])
        for key, value in project['terminology'].items():
            self.assertEqual(value, source[key])

    def test_project_jade_cannot_override_sources_or_drift_english(self):
        import tempfile
        from localization import expected_chinese
        key = 'jade.gt6ce.example'
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            target = root / 'tools/localization/jade_project.json'
            target.parent.mkdir(parents=True)
            target.write_text(json.dumps({'values': {key: {'english': 'Heat: %s', 'value': '热量：%s'}}}), encoding='utf-8')
            with self.assertRaisesRegex(ValueError, 'English drifted'):
                expected_chinese({key: 'Changed: %s'}, {}, {}, repo=root)
            with self.assertRaisesRegex(ValueError, 'overrides source'):
                expected_chinese({key: 'Heat: %s'}, {key: '原文 %s'}, {}, repo=root)

    def test_dynamic_controller_aliases_use_recorded_original_identity(self):
        from localization import read_json, load_source, CONFIG_PATH, LANG_PATH
        aliases = read_json(ROOT / CONFIG_PATH / 'aliases.json')
        source, _ = load_source()
        chinese = read_json(ROOT / LANG_PATH / 'zh_cn.json')
        identities = (ROOT / 'core/src/main/java/com/gregtech/gregtech/content/machine/OriginalMachineMaterialData.java').read_text()
        for family, source_id in [('cokeoven', 17000), ('implosioncompressor', 17110),
                                  ('distillationtower', 17101), ('cryodistillationtower', 17111),
                                  ('fusionreactor', 17198)]:
            key = f'block.gt6ce.{family}_stainless_steel'
            original = f'gt.multitileentity.{source_id}'
            with self.subTest(key=key):
                self.assertRegex(identities, rf'add\("{family}",\s*1,\s*{source_id},')
                self.assertEqual(aliases[key], original)
                self.assertEqual(chinese[key], source[original])
        self.assertEqual(chinese['itemGroup.gt6ce.billet'], source['itemGroup.billet'])
        self.assertEqual(chinese['jade.gt6ce.mold.shape.billet'], '方坯')
