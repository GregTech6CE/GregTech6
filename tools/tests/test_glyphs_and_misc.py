import json, sys, unittest
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[2]
SHARED_ASSETS = ROOT / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"
CORE_ASSETS = ROOT / 'core/src/main/resources/assets/gregtech'
sys.path.insert(0,str(ROOT/'tools'))
from restore_misc_block_textures import MAPPING
from check_render_resources import check

class GlyphAndMiscTests(unittest.TestCase):
    def test_original_character_tile_is_not_the_whole_sheet(self):
        p=CORE_ASSETS/'textures/block/overlays/characters/8.png'
        with Image.open(p) as source:
            image=source.convert('RGBA')
            self.assertEqual(image.size,(64,64))
            # Original tiles have slight gray noise, but repeat the same glyph silhouette.
            def silhouette(box): return bytes(255 if p[3]>128 and sum(p[:3])>300 else 0 for p in image.crop(box).getdata())
            tile=silhouette((0,0,8,8))
            self.assertEqual(tile,silhouette((8,8,16,16)))
            self.assertEqual(tile,silhouette((56,56,64,64)))
            pixels=[''.join('#' if sum(image.getpixel((x,y))[:3])>300 and image.getpixel((x,y))[3]>128 else '.' for x in range(8)) for y in range(8)]
            self.assertEqual(pixels,['........','..####..','..#..#..','..####..','..#..#..','..#..#..','..####..','........'])

    def test_display_renderers_no_longer_use_minecraft_fonts(self):
        for name in ('SensorRenderer','MassStorageRenderer'):
            source=(ROOT/f'src/main/java/com/gregtech/gregtech/client/{name}.java').read_text(encoding='utf-8')
            self.assertNotIn('drawInBatch',source)
            self.assertIn('SixCellDisplayRenderer.draw',source)
            self.assertIn('DisplayFaceOrientation.rotation(facing)',source)
        for name in ['hex','percent','greater','equal','smaller','scale','liter','eu','kelvin','ru','ton','cubicmeter','cubicdecameter','gibbl','lumin','greg','clock','neutron','lu']+[f'0x{x:x}' for x in range(16)]:
            self.assertTrue((CORE_ASSETS/f'textures/block/overlays/characters/{name}.png').exists(),name)

    def test_misc_models_have_specific_original_layers(self):
        for name,folder in MAPPING.items():
            path=SHARED_ASSETS/f'models/block/misc/{name}.json'
            model=json.loads(path.read_text(encoding='utf-8'))
            self.assertEqual(model['loader'],'forge:composite')
            self.assertEqual(model['parent'],'minecraft:block/block')
            for layer,child in model['children'].items():
                self.assertTrue(all(f'machines/{folder}/{layer}/' in texture for texture in child['textures'].values()))
                for face in child['elements'][0]['faces'].values():
                    self.assertEqual(face.get('tintindex'),0 if layer=='colored' else None)
            self.assertEqual(check([path])[0],[])

    def test_tank_models_cover_all_six_directions(self):
        for name in ['tank_3x3','tank_5x5']:
            path=CORE_ASSETS/f'blockstates/{name}.json'
            variants=json.loads(path.read_text(encoding='utf-8'))['variants']
            self.assertEqual(set(variants),{f'facing={face}' for face in ['north','south','east','west','up','down']})
            self.assertEqual(variants['facing=up']['x'],270)
            self.assertEqual(variants['facing=down']['x'],90)
            self.assertEqual(check([path])[0],[])

    def test_new_jei_labels_have_both_translations(self):
        keys=['inputs','outputs','duration','energy_used','energy_produced','energy_total',
              'assembly','assembly_boiler','assembly_tank','part.controller','part.air',
              'part.heat_input','part.fluid_input','part.fluid_output','part.fluid_io']
        for lang in ['en_us','zh_cn']:
            values=json.loads((CORE_ASSETS/f'lang/{lang}.json').read_text(encoding='utf-8'))
            for key in keys: self.assertTrue(values.get('gregtech.jei.'+key),key)

if __name__=='__main__': unittest.main()
