import json
import tempfile
import unittest
from pathlib import Path
from generate_advanced_crafting_assets import generate, ORIGINAL, specs

class AdvancedCraftingAssetsTest(unittest.TestCase):
    def test_original_textures_and_all_material_states(self):
        with tempfile.TemporaryDirectory() as temp:
            root=Path(temp); manifest=generate(root)
            self.assertEqual(len(manifest),11)
            for relative,info in manifest.items():
                self.assertEqual((root/relative).read_bytes(),(ORIGINAL/info['source']).read_bytes())
            for suffix,_ in specs():
                name='advanced_crafting_table' if suffix=='steel' else 'advanced_crafting_table_'+suffix
                variants=json.loads((root/f'blockstates/{name}.json').read_text())['variants']
                self.assertEqual(len(variants),4)
                for facing in ('north','south','west','east'):
                    model=variants['facing='+facing]['model'].removeprefix('gt6ce:')
                    self.assertTrue((root/f'models/{model}.json').exists())
                self.assertIn('block.gt6ce.'+name,json.loads((root/'lang/zh_cn.json').read_text(encoding='utf-8')))

    def test_front_back_and_physical_top_bottom_on_horizontal_facings(self):
        with tempfile.TemporaryDirectory() as temp:
            root=Path(temp); generate(root)
            opposite={'up':'down','down':'up','north':'south','south':'north','east':'west','west':'east'}
            for facing in ('north','south','west','east'):
                model=json.loads((root/f'models/block/machine/storage/advanced_crafting_table_{facing}.json').read_text())
                self.assertEqual(model['loader'],'forge:composite')
                for layer in ('colored','overlay'):
                    child=model['children'][layer]; element=child['elements'][0]
                    self.assertEqual((element['from'],element['to']),([0,0,0],[16,16,16]))
                    self.assertEqual(element['faces'][facing]['texture'],'#front')
                    self.assertEqual(element['faces'][opposite[facing]]['texture'],'#back')
                    for face,data in element['faces'].items():
                        self.assertEqual('tintindex' in data,layer=='colored')
                        if face not in (facing,opposite[facing]):
                            self.assertEqual(data['texture'],{'up':'#top','down':'#bottom'}.get(face,'#side'))
                        texture=child['textures'][data['texture'][1:]].removeprefix('gt6ce:')
                        self.assertTrue((root/f'textures/{texture}.png').exists())

if __name__=='__main__': unittest.main()
