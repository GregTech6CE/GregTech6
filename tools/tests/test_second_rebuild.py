import hashlib
import json
from pathlib import Path
import unittest
import sys
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SHARED_ASSETS = ROOT / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"
ASSETS = ROOT / 'src/main/resources/assets/gregtech'
sys.path.insert(0, str(ROOT / 'tools'))

def read(path):
    return json.loads(path.read_text(encoding='utf-8'))

class SecondRebuildTests(unittest.TestCase):
    def test_fluid_categories_use_distinct_valid_standard_sprites(self):
        paths = ('fluid/molten', 'fluid/gas', 'metallic/molten', 'plasma/gas')
        hashes = []
        for part in paths:
            texture = ASSETS / f'textures/block/material_icons/{part}.png'
            hashes.append(hashlib.sha256(texture.read_bytes()).digest())
            with Image.open(texture) as image: width, height = image.size
            meta = texture.with_suffix('.png.mcmeta')
            if height > width: self.assertTrue(meta.exists(), part)
            if meta.exists():
                for frame in read(meta)['animation'].get('frames', []):
                    self.assertLess(frame if isinstance(frame,int) else frame['index'], height//width, part)
        self.assertNotEqual(hashes[0],hashes[2], 'ordinary liquid must not reuse molten-metal pixels')
        self.assertNotEqual(hashes[0],hashes[1], 'gas and liquid must differ')

    def test_sensor_buttons_match_face_coordinates_after_model_rotation(self):
        import math
        for path in (SHARED_ASSETS / 'blockstates').glob('sensor_*.json'):
            for face, state in read(path)['variants'].items():
                # Default north texture pixel (13,7), transformed using Minecraft's negative model angles.
                x,y,z = 1-13/16-.5, 1-7/16-.5, 14/16-.5
                ax, ay = math.radians(-state.get('x',0)), math.radians(-state.get('y',0))
                y,z = y*math.cos(ax)-z*math.sin(ax), y*math.sin(ax)+z*math.cos(ax)
                x,z = x*math.cos(ay)+z*math.sin(ay), -x*math.sin(ay)+z*math.cos(ay)
                x,y,z = x+.5,y+.5,z+.5
                uv = {'facing=north':(1-x,1-y),'facing=south':(x,1-y),
                      'facing=west':(z,1-y),'facing=east':(1-z,1-y),
                      'facing=up':(x,z),'facing=down':(x,1-z)}[face]
                self.assertAlmostEqual(uv[0]*16,13, msg=f'{path.name} {face} button x')
                self.assertAlmostEqual(uv[1]*16,7, msg=f'{path.name} {face} button y')

    def test_burning_box_top_matches_solid_in_both_states(self):
        from generate_burning_box_jsons import make_direction_model
        for family in ('liquid', 'gas', 'fluidbed'):
            for facing in ('north', 'east', 'south', 'west'):
                for state in ('off', 'on'):
                    model = read(SHARED_ASSETS / f'models/block/machine/burning_{family}/{facing}_{state}.json')
                    generated = make_direction_model(f'burning_{family}', facing, state == 'on')
                    self.assertEqual(generated, model, 'regeneration must preserve the repaired model')
                    layer = 'overlay_active' if state == 'on' else 'overlay'
                    for face, name in [('up', 'top'), ('down', 'bottom')]:
                        self.assertEqual(model['children']['layer1']['textures'][face],
                                         f'gregtech:block/machines/generators/burning_{family}/{layer}/{name}')
                    for part in ('colored', layer):
                        texture = ASSETS / f'textures/block/machines/generators/burning_{family}/{part}/top.png'
                        solid = ASSETS / f'textures/block/machines/generators/burning_solid/{part}/top.png'
                        self.assertEqual(hashlib.sha256(texture.read_bytes()).digest(), hashlib.sha256(solid.read_bytes()).digest())

    def test_standard_fluid_has_valid_animation_frames(self):
        texture = ASSETS / 'textures/block/material_icons/metallic/fluid.png'
        with Image.open(texture) as image:
            width, height = image.size
        animation = read(texture.with_suffix('.png.mcmeta'))['animation']
        self.assertEqual((width, height), (16, 320))
        self.assertGreater(animation['frametime'], 0)
        frames = [frame if isinstance(frame, int) else frame['index'] for frame in animation['frames']]
        self.assertEqual(set(frames), set(range(height // width)))

    def test_five_vessels_have_placeable_models(self):
        for name in ('jug', 'cup', 'measuring_pot', 'thermos', 'barometer_gas_cylinder'):
            model = read(SHARED_ASSETS / f'blockstates/fluid_{name}.json')['variants']['']['model']
            self.assertEqual(model, read(SHARED_ASSETS / f'models/item/fluid_{name}.json')['parent'])
            self.assertTrue((SHARED_ASSETS / f'models/{model.split(":")[1]}.json').exists())

    def test_anvil_material_coverage_and_resources(self):
        specs = read(ROOT / 'tools/gt6_anvil_specs.json')
        self.assertEqual(len(specs), 35)
        self.assertEqual(len({s['material'] for s in specs}), 35)
        self.assertEqual(next(s['durability'] for s in specs if s['material'] == 'Steel'), 10000000)
        catalog = {s['name'] for s in read(ROOT / 'build/registration-material-catalog.json')}
        for spec in specs:
            self.assertIn(spec['material'], catalog)
            name = 'anvil' if spec['material'] == 'Steel' else 'anvil_' + spec['material'].lower()
            variants = read(SHARED_ASSETS / f'blockstates/{name}.json')['variants']
            self.assertEqual(len(variants), 4)
            self.assertEqual(read(SHARED_ASSETS / f'models/item/{name}.json')['parent'], 'gregtech:block/tool/anvil')

    def test_sensor_geometry_is_not_full_cube(self):
        for path in (SHARED_ASSETS / 'models/block/machine/sensor').glob('*.json'):
            def visit(node):
                if isinstance(node, dict):
                    for element in node.get('elements', []):
                        self.assertEqual(element['from'], [0, 0, 14], path.name)
                        self.assertEqual(element['to'], [16, 16, 16], path.name)
                        self.assertTrue(all('cullface' not in face for face in element['faces'].values()), path.name)
                    for value in node.values():
                        visit(value)
                elif isinstance(node, list):
                    for value in node: visit(value)
            visit(read(path))

if __name__ == '__main__':
    unittest.main()
