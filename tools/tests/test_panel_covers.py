
# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[1]))
from native_layout import native_path

import hashlib
import json
import re
import unittest
from tools.rebuild_panel_covers import ROOT, ASSETS, TEXTURES, MODELS, RECIPES
from tools.check_render_resources import check


class PanelCoverResources(unittest.TestCase):
    def test_all_dynamic_sprites_have_original_provenance(self):
        ledger=json.loads((ROOT/'tools/gt6_texture_sources.json').read_text(encoding='utf-8'))
        for texture in TEXTURES:
            path=f'textures/block/machines/covers/{texture}.png'
            self.assertEqual(hashlib.sha256((ASSETS/path).read_bytes()).hexdigest(),ledger[path]['sha256'],path)
            self.assertTrue(ledger[path]['source'].endswith(f'/machines/covers/{texture}.png'))
        issues,_=check([ASSETS/f'models/item/{name}.json' for name in MODELS])
        self.assertEqual(issues,[])

    def test_every_runtime_panel_has_model_recipe_and_bilingual_help(self):
        source=(native_path(ROOT/'src/main/java/com/gregtech/gregtech/content/cover/PanelCover.java')).read_text(encoding='utf-8')
        ids=set(re.findall(r'[A-Z_]+\("([a-z_]+)"\)',source))
        self.assertEqual(ids,set(MODELS));self.assertEqual(ids,set(RECIPES))
        for lang in ['en_us','zh_cn']:
            strings=json.loads((ASSETS/f'lang/{lang}.json').read_text(encoding='utf-8'))
            for name in ids:
                self.assertTrue(strings[f'item.gt6ce.{name}'])
                self.assertTrue(strings[f'item.gt6ce.{name}.tooltip'])
            for key in ['unsupported','style','reset','latch']:
                self.assertTrue(strings[f'message.gt6ce.panel.{key}'])

    def test_shaped_patterns_and_source_component_distinctions(self):
        for name,(pattern,_) in RECIPES.items():
            data=json.loads((native_path(ROOT/f'src/main/resources/data/gt6ce/recipes/panel_covers/{name}.json')).read_text(encoding='utf-8'))
            self.assertEqual(data['pattern'],pattern)
            self.assertEqual(set(data['key']),set(''.join(pattern))-{' '})
            self.assertEqual(data['result']['item'],'gt6ce:'+name)
        source=(native_path(ROOT/'src/main/java/com/gregtech/gregtech/content/cover/PanelCoverRuntime.java')).read_text(encoding='utf-8')
        # Dedicated client snapshots are required: these blocks have no open menu for syncing covers.
        extender=(native_path(ROOT/'src/main/java/com/gregtech/gregtech/block/misc/ExtenderBlockEntity.java')).read_text(encoding='utf-8')
        self.assertIn('getUpdatePacket()',extender)
        self.assertIn('sendBlockUpdated',source)


if __name__=='__main__':unittest.main()
