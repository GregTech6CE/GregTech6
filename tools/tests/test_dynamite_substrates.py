
# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[1]))
from native_layout import native_path

import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


class DynamiteSubstrateAssets(unittest.TestCase):
    def test_original_soils_and_all_stained_clays_are_drillable(self):
        data = json.loads((native_path(ROOT / 'src/main/resources/data/gregtech/tags/blocks/drillable_dynamite.json')).read_text())
        self.assertFalse(data['replace'])
        values = data['values']
        self.assertEqual(len(values), len(set(values)))
        self.assertEqual(17, sum(v.endswith('terracotta') for v in values))
        for name in ('dirt', 'grass_block', 'mycelium', 'clay', 'snow_block', 'gravel',
                     'sandstone', 'cobblestone', 'mossy_cobblestone', 'end_stone', 'infested_stone',
                     'coarse_dirt', 'podzol', 'chiseled_sandstone', 'cut_sandstone', 'infested_cobblestone',
                     'infested_stone_bricks', 'infested_mossy_stone_bricks', 'infested_cracked_stone_bricks',
                     'infested_chiseled_stone_bricks'):
            self.assertIn('minecraft:' + name, values)
        for name in ('bedrock', 'oak_planks', 'stone_bricks'):
            self.assertNotIn('minecraft:' + name, values)

    def test_tool_hint_uses_supplied_gt6_chinese(self):
        path = native_path(ROOT / 'src/main/resources/assets/gregtech/lang')
        cn = json.loads((path / 'zh_cn.json').read_text(encoding='utf-8'))
        en = json.loads((path / 'en_us.json').read_text(encoding='utf-8'))
        self.assertEqual('放置炸药并绑定到快捷栏中的远程激活器', cn['gt.behaviour.placedynamite'])
        self.assertEqual('Places Dynamite and links it to Remote Activators in Hotbar', en['gt.behaviour.placedynamite'])


if __name__ == '__main__':
    unittest.main()
