
# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[1]))
from native_layout import native_path

import importlib.util
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("vanilla_compositions", ROOT / "tools/generate_vanilla_compositions.py")
generator = importlib.util.module_from_spec(spec)
spec.loader.exec_module(generator)

class VanillaCompositionDataTests(unittest.TestCase):
    def test_source_import_preserves_composites_and_fractional_units(self):
        generator.rows.clear()
        generator.imported()
        rows=generator.rows;u=generator.U
        self.assertEqual(rows['iron_pickaxe']['components'], {'Iron':u*26//9,'Wood':u})
        self.assertEqual(rows['hopper']['components'], {'Iron':u*5,'Wood':u*4})
        self.assertEqual(rows['clock']['components'], {'Gold':u*4,'Redstone':u})
        self.assertEqual(rows['clay']['components'], {'Clay':u*4})
        self.assertEqual(rows['stone']['components'], {'Stone':u*9})

    def test_modern_inferences_do_not_enable_automatic_recovery(self):
        rows=json.loads((native_path(ROOT/'src/main/resources/data/gregtech/materials/vanilla_compositions.json')).read_text(encoding='utf-8'))
        for name,row in rows.items():
            self.assertTrue(row['source'])
            self.assertTrue(all(isinstance(v,int) and v>0 for v in row['components'].values()),name)
            if row['source'].startswith('Minecraft 1.20.1 recipe:'):
                self.assertFalse(row['recoverable'],name)
        self.assertEqual(rows['raw_iron_block']['components']['Iron'],18*generator.U)
        self.assertFalse(rows['cod_bucket']['recoverable'])
        self.assertEqual(rows['command_block']['components'],{})

    def test_source_quantities_do_not_depend_on_stack_size(self):
        rows=json.loads((native_path(ROOT/'src/main/resources/data/gregtech/materials/vanilla_compositions.json')).read_text(encoding='utf-8'))
        self.assertEqual(rows['iron_ingot']['components']['Iron'],generator.U)
        self.assertEqual(rows['iron_block']['components']['Iron'],9*generator.U)
        self.assertEqual(rows['glass_bottle']['components']['Glass'],generator.U)

    def test_wood_dictionary_and_modern_stonecutting_override_legacy_defaults(self):
        rows=json.loads((native_path(ROOT/'src/main/resources/data/gregtech/materials/vanilla_compositions.json')).read_text(encoding='utf-8'))
        u=generator.U
        for wood in ['oak','spruce','birch','jungle','acacia','dark_oak','mangrove','cherry']:
            self.assertEqual(rows[wood+'_log']['components'],{'Wood':8*u,'Bark':u})
            self.assertEqual(rows['stripped_'+wood+'_log']['components'],{'Wood':8*u})
        self.assertEqual(rows['quartz_stairs']['components'],{'NetherQuartz':4*u})
        self.assertEqual(rows['oak_sign']['components'],{'Wood':13*u//6})
        self.assertEqual(rows['ladder']['components'],{'Wood':7*u//6})

if __name__=='__main__':unittest.main()
