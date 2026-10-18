
# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[1]))
from native_layout import native_path

import importlib.util
import json
import re
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('chest_assets', ROOT / 'tools/generate_reinforced_chest_assets.py')
generator = importlib.util.module_from_spec(spec)
spec.loader.exec_module(generator)


class ReinforcedChestAssets(unittest.TestCase):
    def test_original_textures_and_standard_chinese_survive_regeneration(self):
        originals = dict(re.findall(r'^\s*S:gt.multitileentity\.(\d+)=(.*)$',
                                   (ROOT.parent / 'GregTech.lang').read_text(encoding='utf-8'), re.M))
        with tempfile.TemporaryDirectory() as temporary:
            assets = Path(temporary)
            family = generator.generate(assets)
            zh = json.loads((assets / 'lang/zh_cn.json').read_text(encoding='utf-8'))
            for suffix, source_id in family:
                name = 'reinforced_wood_chest_' + suffix
                self.assertEqual(originals[str(source_id)].strip(), zh['block.gregtech.' + name])
                for relative in ('blockstates/' + name + '.json', 'models/item/' + name + '.json'):
                    self.assertEqual((assets / relative).read_bytes(), (generator.ASSETS / relative).read_bytes())
            for shell in ('woodchest', 'lootchest'):
                for original_layer, target_layer in (('colored', 'colored'), ('plain', 'overlay')):
                    source = native_path(generator.ORIGINAL / f'src/main/resources/assets/gregtech/textures/model/gt.multitileentity/{shell}.{original_layer}.png')
                    target = generator.ASSETS / f'textures/block/machines/{shell}/{target_layer}.png'
                    self.assertEqual(source.read_bytes(), target.read_bytes())
            before = json.loads((generator.ASSETS / 'lang/zh_cn.json').read_text(encoding='utf-8'))
            self.assertEqual(before, zh, 'regeneration must preserve unrelated translations')

    def test_all_chest_items_resolve_to_the_shared_entity_renderer(self):
        for suffix, _ in generator.specs():
            name = 'reinforced_wood_chest_' + suffix
            path = generator.ASSETS / ('models/item/' + name + '.json')
            visited = set()
            while True:
                self.assertNotIn(path, visited, 'model parent cycle')
                visited.add(path)
                model = json.loads(path.read_text(encoding='utf-8'))
                parent = model['parent']
                if parent == 'builtin/entity':
                    break
                self.assertTrue(parent.startswith('gregtech:'))
                path = generator.ASSETS / ('models/' + parent.split(':', 1)[1] + '.json')


if __name__ == '__main__':
    unittest.main()
