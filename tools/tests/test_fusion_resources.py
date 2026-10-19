"""Read maintained shared data and resource overlays without generating native views.

These checks cover source assets and numeric recipe rows, not baked Minecraft models
or native recipe resolution.
"""
import json
import re
import unittest
from pathlib import Path
from collections import Counter

ROOT = Path(__file__).resolve().parents[2]
SHARED_JAVA = ROOT / 'multiversion/src/main/java/com/gregtech/gregtech'
CORE_JAVA = ROOT / 'core/src/main/java/com/gregtech/gregtech'
PLATFORMS = ('1.20.1-forge', '1.21.1-neoforge')


def asset_roots(platform):
    return [
        ROOT / 'multiversion/versions' / platform / 'src/main/resources/assets/gt6ce',
        ROOT / 'multiversion/src/main/resources/assets/gt6ce',
        ROOT / 'core/src/main/resources/assets/gt6ce',
    ]


def asset(platform, relative):
    for base in asset_roots(platform):
        path = base / relative
        if path.is_file():
            return path
    raise FileNotFoundError(f'{platform}: gt6ce/{relative}')


class FusionResources(unittest.TestCase):
    def test_exact_original_part_counts_and_energy_ports(self):
        source = (CORE_JAVA / 'content/multiblock/SharedFusionStructure.java').read_text(encoding='utf8')
        cells = re.findall(r'Cell\(\s*(-?\d+)\s*,\s*(-?\d+)\s*,\s*(-?\d+)\s*,\s*(\d+)\s*,\s*Role\.(\w+)\s*\)', source)
        self.assertEqual(len(cells), 886)
        self.assertEqual(len({(x,y,z) for x,y,z,_,_ in cells}),886)
        self.assertEqual(Counter(int(part) for _,_,_,part,_ in cells), {18003:576,18045:144,18299:50,18002:36,18008:53,18200:3,18201:12,18202:12})
        self.assertEqual({(int(x),int(y),int(z)) for x,y,z,_,r in cells if r=='ENERGY_OUTPUT'}, {(-9,0,2),(9,0,2),(0,0,-7),(0,0,11)})

    def test_part_models_use_existing_original_textures_and_separate_tint(self):
        for platform in PLATFORMS:
            with self.subTest(platform=platform):
                relative_paths = {
                    path.relative_to(base)
                    for base in asset_roots(platform)
                    for path in (base / 'models/block/machine/fusion').glob('*.json')
                }
                self.assertTrue(relative_paths, 'No fusion models found')
                for relative in sorted(relative_paths):
                    model = json.loads(asset(platform, relative).read_text(encoding='utf8'))
                    self.assertTrue(model['children'], relative)
                    for name, child in model['children'].items():
                        for texture in child['textures'].values():
                            namespace, path = texture.split(':', 1)
                            self.assertEqual(namespace, 'gt6ce')
                            asset(platform, 'textures/' + path + '.png')
                        for element in child['elements']:
                            for face in element['faces'].values():
                                self.assertEqual('tintindex' in face, name == 'base')

    def test_platform_controller_states_reference_existing_models(self):
        controllers = {
            '1.20.1-forge': 'fusion_reactor_main',
            '1.21.1-neoforge': 'fusionreactor_stainless_steel',
        }
        expected = {
            f'facing={face},lit={lit},running={running}'
            for face in ('north', 'east', 'south', 'west')
            for lit in ('false', 'true')
            for running in ('false', 'true')
        }
        for platform, controller in controllers.items():
            with self.subTest(platform=platform):
                states = json.loads(asset(platform, f'blockstates/{controller}.json').read_text(encoding='utf8'))
                self.assertEqual(set(states['variants']), expected)
                for variant in states['variants'].values():
                    namespace, path = variant['model'].split(':', 1)
                    self.assertEqual(namespace, 'gt6ce')
                    asset(platform, 'models/' + path + '.json')

    def test_source_reaction_values_are_preserved(self):
        original=ROOT.parent/'gregtech6-master/gregtech6-master/src/main/java/gregtech/loaders/c/Loader_Recipes_Other.java'
        if not original.exists():self.skipTest('Original GT6 checkout unavailable')
        source=original.read_text(encoding='utf8')
        expected=[]
        for line in source.splitlines():
            if 'RM.Fusion.addRecipe1' not in line:continue
            eut,ticks,circuit=re.search(r'addRecipe1\(F,\s*(-?\d+),\s*(\d+), ST.tag\((\d+)\)',line).groups()
            value=1
            for number in re.search(r'setSpecialNumber\(([^)]+)',line).group(1).replace('L','').split('*'):value*=int(number)
            expected.append((int(circuit),int(ticks),int(eut),value))
        self.assertEqual(len(expected), 18, 'Original numeric fusion rows were not fully read')
        port=(SHARED_JAVA / 'content/recipe/FusionRecipeLoader.java').read_text(encoding='utf8')
        actual=[tuple(map(int,match)) for match in re.findall(r'add\((\d+),(\d+),(-?\d+),(\d+)L,',port)]
        self.assertEqual(actual,expected)
