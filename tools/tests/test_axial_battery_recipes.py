import importlib.util
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('axial_batteries', ROOT / 'tools/normalize_axial_battery_recipes.py')
generator = importlib.util.module_from_spec(spec)
spec.loader.exec_module(generator)


class AxialBatteryRecipes(unittest.TestCase):
    def test_normalization_preserves_all_non_group_fields_and_is_idempotent(self):
        recipe = {'pattern': ['PwP', 'BMC', 'PEP'], 'key': {
            'B': {'item': 'gt6ce:battery_lv'}, 'C': {'item': 'gt6ce:circuit_ultimate'},
            'M': {'item': 'gt6ce:original_housing'}}, 'allow_mirror': False,
            '_mirror_flags': 'gt6-census', 'result': {'item': 'gt6ce:original_output'}}
        line = "'B', \"gt:re-battery2\", 'C', OD_CIRCUITS[6]"
        before = json.loads(json.dumps(recipe))
        result = generator.normalize(recipe, line)
        self.assertEqual(recipe, before)
        self.assertEqual(result['key']['B'], {'tag': 'gt6ce:rechargeable_batteries/mv'})
        self.assertEqual(result, generator.normalize(result, line))
        result['key']['B'] = before['key']['B']
        result['key']['C'] = before['key']['C']
        self.assertEqual(result, before)

    def test_eight_written_recipes_use_original_exact_tiers(self):
        for number, name in generator.TARGETS.items():
            recipe = json.loads((generator.RECIPES / (name + '.json')).read_text(encoding='utf-8'))
            tier = 'lv' if number < 17230 else ['lv', 'mv', 'hv', 'ev'][number - 17231]
            self.assertEqual(recipe['key']['B'], {'tag': 'gt6ce:rechargeable_batteries/' + tier})
            self.assertEqual(recipe['key']['C'], {'tag': 'gt6ce:circuits_tier_6_plus'})
            self.assertFalse(recipe['allow_mirror'])


if __name__ == '__main__':
    unittest.main()
