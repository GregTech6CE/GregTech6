from pathlib import Path
SHARED_ASSETS = Path(__file__).resolve().parents[2] / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"
import json
import unittest
from tools.rebuild_signal_wires import ROOT, ASSETS, IDS
from tools.check_render_resources import check


class SignalWireResources(unittest.TestCase):
    def test_six_models_have_textures_and_north_south_item_fallbacks(self):
        paths=[]
        for name in IDS:
            paths += [SHARED_ASSETS/f'blockstates/{name}.json', SHARED_ASSETS/f'models/item/{name}.json']
            model=json.loads((SHARED_ASSETS/f'models/block/signal_wire/{name}.json').read_text())
            self.assertEqual(model['elements'][0]['from'][2],0)
            self.assertEqual(model['elements'][0]['to'][2],16)
            for locale in ['en_us','zh_cn']:
                lang=json.loads((ASSETS/f'lang/{locale}.json').read_text(encoding='utf-8'))
                self.assertTrue(lang['block.gregtech.'+name])
        issues,_=check(paths)
        self.assertEqual(issues,[])

    def test_panels_use_source_wire_ingredients(self):
        for name,key,material in [('energy_display_cover','L','lumium'),('machine_status_display_cover','L','lumium'),
                                  ('redstone_conductor_cover_accept','R','redalloy'),('redstone_conductor_cover_emit','R','redalloy')]:
            recipe=json.loads((ROOT/f'src/main/resources/data/gregtech/recipes/panel_covers/{name}.json').read_text())
            self.assertEqual(recipe['key'][key],{'item':'gregtech:wire_01_'+material})

if __name__=='__main__':unittest.main()
