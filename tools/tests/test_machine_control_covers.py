import hashlib
import json
import re
import unittest
from pathlib import Path
SHARED_ASSETS = Path(__file__).resolve().parents[2] / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"
from tools.rebuild_machine_control_covers import SPECS, ASSETS, ROOT
from tools.check_render_resources import check


class MachineControlCoverResources(unittest.TestCase):
    def test_original_textures_and_layered_models(self):
        ledger = json.loads((ROOT / 'tools/gt6_texture_sources.json').read_text(encoding='utf-8'))
        paths = []
        for name, texture, *_ in SPECS:
            path = SHARED_ASSETS / f'models/item/{name}.json'
            paths.append(path)
            model = json.loads(path.read_text(encoding='utf-8'))
            self.assertEqual(model['textures']['layer0'], 'gregtech:block/machines/covers/base')
            self.assertEqual(model['textures']['layer1'], f'gregtech:block/machines/covers/{texture}/circuit')
            for image in ['base', texture + '/circuit']:
                relative = f'textures/block/machines/covers/{image}.png'
                self.assertEqual(hashlib.sha256((ASSETS / relative).read_bytes()).hexdigest(), ledger[relative]['sha256'])
        issues, count = check(paths)
        self.assertFalse(issues)
        self.assertGreaterEqual(count, 12)

    def test_java_catalog_and_crafting_patterns_agree(self):
        source = (ROOT / 'src/main/java/com/gregtech/gregtech/content/cover/MachineCoverSpec.java').read_text(encoding='utf-8')
        declared = set(re.findall(r'\("([a-z0-9_]+)","([a-z0-9_/]+)",-?\d+\)', source))
        self.assertEqual(declared, {(name, texture) for name, texture, *_ in SPECS})
        for name, _, pattern, *_ in SPECS:
            recipe = json.loads((ROOT / f'src/main/resources/data/gregtech/recipes/control_covers/{name}.json').read_text(encoding='utf-8'))
            self.assertEqual(recipe['pattern'], pattern)
            self.assertEqual(set(recipe['key']), set(''.join(pattern)) - {' '})
            self.assertEqual(recipe['type'], 'gregtech:tool_shaped')

    def test_cover_messages_and_names_have_both_languages(self):
        for locale in ['en_us', 'zh_cn']:
            data = json.loads((ASSETS / f'lang/{locale}.json').read_text(encoding='utf-8'))
            for mode in ['strong', 'weak', 'normal', 'inverted']:
                self.assertTrue(data['message.gregtech.cover.' + mode])
            for name, *_ in SPECS:
                self.assertTrue(data['item.gregtech.' + name])
