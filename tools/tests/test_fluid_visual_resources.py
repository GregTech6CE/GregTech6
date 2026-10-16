from pathlib import Path
import hashlib
import json
import unittest

ROOT=Path(__file__).resolve().parents[2]
SHARED_ASSETS = ROOT / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"
ASSETS=ROOT/'src/main/resources/assets/gregtech'

class FluidVisualResources(unittest.TestCase):
    def test_all_original_named_fluids_keep_their_png_and_animation_bytes(self):
        ledger=json.loads((ROOT/'tools/gt6_texture_sources.json').read_text(encoding='utf-8'))
        files=list((ASSETS/'textures/block/fluids').glob('*.png'))
        self.assertEqual(len(files),350)
        for file in files:
            for path in [file,Path(str(file)+'.mcmeta')]:
                if not path.exists():continue
                entry=ledger[path.relative_to(ASSETS).as_posix()]
                self.assertEqual(hashlib.sha256(path.read_bytes()).hexdigest(),entry['sha256'],path.name)
                self.assertIn('/textures/blocks/fluids/',entry['source'])

    def test_fluid_atlas_and_tinted_shared_model_are_declared(self):
        atlas=json.loads((ROOT/'src/main/resources/assets/minecraft/atlases/blocks.json').read_text())
        self.assertTrue(any(s.get('source')=='block/fluids' for s in atlas['sources']))
        model=json.loads((SHARED_ASSETS/'models/item/fluid_item.json').read_text())
        self.assertEqual(model['parent'],'minecraft:item/generated')
        self.assertIn('layer0',model['textures'])

if __name__=='__main__':unittest.main()
