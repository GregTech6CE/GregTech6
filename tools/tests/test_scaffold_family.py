"""All GT6 metalset scaffolds have reachable models, source names and survival recipes."""
import importlib.util
import json
from pathlib import Path
import re
import unittest
ROOT = Path(__file__).resolve().parents[2]
SHARED_ASSETS = ROOT / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"
ASSETS = ROOT / 'src/main/resources/assets/gregtech'
class ScaffoldFamilyTests(unittest.TestCase):
    def test_all_sixty_families_have_models_and_recipes(self):
        source = (ROOT / 'src/main/java/com/gregtech/gregtech/registry/GTStorageMetals.java').read_text(encoding='utf-8')
        suffixes = re.findall(r'new Spec\("([^"]+)"', source)
        self.assertEqual(60, len(suffixes))
        for suffix in suffixes:
            name = 'scaffold' if suffix == 'steel' else 'scaffold_' + suffix
            states = json.loads((SHARED_ASSETS / f'blockstates/{name}.json').read_text())['variants']
            self.assertEqual(16, len(states))
            for variant in states.values():
                model = json.loads((ASSETS / ('models/' + variant['model'].split(':')[1] + '.json')).read_text())
                base = model.get('children', {}).get('base', model)
                for ref in base['textures'].values():
                    self.assertTrue((ASSETS / ('textures/' + ref.split(':')[1] + '.png')).is_file(), ref)
            recipe = json.loads((ROOT / f'src/main/resources/data/gregtech/recipes/scaffolds/{name}.json').read_text())
            self.assertEqual('gregtech:tool_shaped', recipe['type'])
            self.assertEqual(['TPT', 'SdS'], recipe['pattern'])
            self.assertEqual('gregtech:' + name, recipe['result']['item'])
            self.assertFalse(recipe['allow_mirror'])
    def test_chinese_names_match_original_ids(self):
        names = dict(re.findall(r'^\s*S:gt.multitileentity\.(\d+)=(.*)$', (ROOT.parent / 'GregTech.lang').read_text(encoding='utf-8'), re.M))
        lang = json.loads((ASSETS / 'lang/zh_cn.json').read_text(encoding='utf-8'))
        recipes = ROOT / 'src/main/resources/data/gregtech/recipes/scaffolds'
        for path in recipes.glob('*.json'):
            data = json.loads(path.read_text())
            old_id = re.search(r'\((\d+)\)', data['_comment'])[1]
            self.assertEqual(names[old_id].strip(), lang['block.gregtech.' + path.stem])
if __name__ == '__main__':
    unittest.main()
