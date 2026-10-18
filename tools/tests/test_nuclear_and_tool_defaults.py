
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
ASSETS = native_path(ROOT / 'src/main/resources/assets/gregtech')

class NuclearAndToolDefaults(unittest.TestCase):
    def test_each_tool_has_its_own_uncolored_gregtech_model(self):
        source = (native_path(ROOT / 'src/main/java/com/gregtech/gregtech/api/tool/GTToolType.java')).read_text(encoding='utf-8')
        ids = re.findall(r'^    \w+\(\d+, "([^"]+)"', source, re.M)
        self.assertGreaterEqual(len(ids), 37)
        for name in ids:
            model = json.loads((ASSETS / f'models/item/tool_{name}.json').read_text(encoding='utf-8'))
            self.assertEqual('minecraft:item/handheld', model['parent'])
            self.assertTrue(0 < len(model['textures']) <= 5)
            for texture in model['textures'].values():
                self.assertTrue(texture.startswith('gregtech:'), name)
                self.assertTrue((ASSETS / 'textures' / (texture.split(':')[1]+'.png')).exists(), texture)
        hammer = (ASSETS / 'models/item/tool_hammer.json').read_text()
        self.assertIn('toolheadhammer', hammer)
        self.assertIn('iconsets/wrench', (ASSETS / 'models/item/tool_wrench.json').read_text())

    def test_original_rod_catalog_products_and_resources(self):
        rods = json.loads((ROOT / 'docs/reactor-rod-source.json').read_text())
        ids = {r['original'] for r in rods}
        self.assertEqual(46, len(ids))
        for rod in rods:
            if rod['product']:
                self.assertIn(rod['product'], ids)
            model = json.loads((ASSETS / f"models/item/{rod['id']}.json").read_text())
            self.assertEqual('gregtech:block/machine/container/reactor_rod', model['parent'])
            for lang in ['en_us', 'zh_cn']:
                names = json.loads((ASSETS / f'lang/{lang}.json').read_text(encoding='utf-8'))
                self.assertIn('item.gregtech.'+rod['id'], names)
        thorium = next(r for r in rods if r['original'] == 9210)
        self.assertEqual(12000000000, thorium['life'])
        self.assertEqual(32, next(r for r in rods if r['original'] == 9221)['self'])
