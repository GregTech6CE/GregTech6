
# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[1]))
from native_layout import native_path

import importlib.util
import json
import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
ASSETS=native_path(ROOT/'src/main/resources/assets/gregtech')
class LegacyBatteryAssets(unittest.TestCase):
    def test_alias_models_names_and_charge_overrides_match_canonical_chemistry(self):
        langs=[json.loads((ASSETS/f'lang/{lang}.json').read_text(encoding='utf-8')) for lang in ['en_us','zh_cn']]
        for tier,suffix in enumerate(['ulv','lv','mv','hv','ev']):
            old='battery_eu_'+str(8*4**tier);canonical='battery_lithium_cobalt_'+suffix
            for folder in ['blockstates','models/item']:
                self.assertEqual(json.loads((ASSETS/f'{folder}/{old}.json').read_text()),json.loads((ASSETS/f'{folder}/{canonical}.json').read_text()))
            for lang in langs:self.assertEqual(lang['block.gregtech.'+old],lang['block.gregtech.'+canonical])
    def test_old_generators_do_not_restore_active_energy_node_or_waterlogging(self):
        for name in ['generate_energy_node_assets','add_waterlogged_variants']:
            spec=importlib.util.spec_from_file_location(name,ROOT/f'tools/{name}.py');module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
            if name=='generate_energy_node_assets':self.assertFalse(any(row[0].startswith('battery_eu_') for row in module.DEVICES))
            else:self.assertFalse(module.needs_waterlogged('battery_eu_32.json'))
if __name__=='__main__':unittest.main()
