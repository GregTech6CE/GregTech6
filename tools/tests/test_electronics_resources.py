import json
from pathlib import Path
import sys
import unittest
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools'))
from check_render_resources import check

class ElectronicsResources(unittest.TestCase):
    def test_new_material_forms_have_complete_shared_textures(self):
        assets=ROOT/'platform-shared/src/main/resources/assets/gregtech/models/item/material'
        paths=[assets/texture/(form+'.json') for texture in ['copper','metallic'] for form in ['boulegt','plategem','plategemtiny']]
        paths += [assets/texture/(form+'.json') for texture in ['rubber','dull'] for form in ['ingot','plate','ring']]
        issues,_=check(paths)
        self.assertEqual([],issues)
    def test_component_results_have_existing_item_models(self):
        root=ROOT/'src/main/resources'
        recipes=list((root/'data/gregtech/recipes/components').glob('*.json'))
        self.assertEqual(55,len(recipes))
        paths=[]
        for path in recipes:
            recipe=json.loads(path.read_text(encoding='utf8'))
            result=recipe['result']['item'].split(':')[1]
            paths.append(root/'assets/gregtech/models/item'/f'{result}.json')
            for ingredient in recipe['key'].values():
                identifier=ingredient.get('item','').split(':')[-1]
                if identifier.startswith('tool_'):
                    self.assertTrue((root/'assets/gregtech/models/item'/f'{identifier}.json').is_file(),identifier)
        issues,_=check(paths)
        self.assertEqual([],issues)
