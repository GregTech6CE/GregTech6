import sys
import tempfile
import unittest
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools'))
import generate_safe_assets as generator

class SafeAssets(unittest.TestCase):
    def test_original_layers_names_and_regenerated_models(self):
        with tempfile.TemporaryDirectory() as temporary:
            target=Path(temporary)
            sources=generator.generate(target)
            for relative,entry in sources.items():
                self.assertEqual((generator.ORIGINAL/entry['source']).read_bytes(),(ROOT/'core/src/main/resources/assets/gregtech'/relative).read_bytes())
            shipped = ROOT/'platform-shared/src/main/resources/assets/gregtech'
            for path in target.rglob('*.json'):
                relative = path.relative_to(target)
                if relative.parts[0] == 'lang':
                    continue
                self.assertEqual(json.loads(path.read_text(encoding='utf-8')),json.loads((shipped/relative).read_text(encoding='utf-8')))
            self.assertEqual(18,len(sources))
    def test_closed_open_layers_and_material_tint_are_separate(self):
        assets=ROOT/'platform-shared/src/main/resources/assets/gregtech'
        for kind,phase in [('mechanical',''),('keylocked',''),('keylocked','_open')]:
            data=json.loads((assets/f'models/block/machine/storage/safe_{kind}{phase}.json').read_text(encoding='utf-8'))
            for name,child in data['children'].items():
                for face in child['elements'][0]['faces'].values():
                    self.assertEqual(name=='colored','tintindex' in face)
                for texture in child['textures'].values():
                    self.assertTrue((ROOT/'core/src/main/resources/assets/gregtech'/('textures/'+texture.split(':',1)[1]+'.png')).is_file())
        state=json.loads((ROOT/'core/src/main/resources/assets/gregtech/blockstates/key_safe_steel.json').read_text(encoding='utf-8'))
        for direction in ['north','east','south','west','up','down']:
            self.assertNotEqual(state['variants'][f'facing={direction},open=true']['model'],state['variants'][f'facing={direction},open=false']['model'])
        for opened in ['true','false']:
            self.assertEqual(270,state['variants'][f'facing=up,open={opened}']['x'])
            self.assertEqual(90,state['variants'][f'facing=down,open={opened}']['x'])

if __name__=='__main__': unittest.main()
