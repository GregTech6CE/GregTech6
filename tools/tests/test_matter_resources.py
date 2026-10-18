
# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[1]))
from native_layout import native_path

import json
import unittest
from pathlib import Path
import sys

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools'))
from check_render_resources import check


class MatterResources(unittest.TestCase):
    def test_matter_parts_and_all_compact_tiers_have_complete_model_graphs(self):
        assets=native_path(ROOT/'src/main/resources/assets/gregtech')
        names=['dense_lead_wall','large_osmium_coil','conversion_processor_unit','largemassfab_lead','massfab_osmium']
        names += ['massfab_osmiridium_t'+str(t) for t in range(2,6)]
        paths=[assets/f'blockstates/{name}.json' for name in names]+[assets/f'models/item/{name}.json' for name in names]
        for path in paths:self.assertTrue(path.is_file(),path)
        errors,_=check(paths);self.assertEqual([],errors)
        for name in names[:3]:
            model=json.loads((assets/f'models/block/machine/matter/{name}.json').read_text(encoding='utf8'))
            self.assertTrue(all(face.get('tintindex')==0 for face in model['children']['colored']['elements'][0]['faces'].values()))
            self.assertTrue(all('tintindex' not in face for face in model['children']['overlay']['elements'][0]['faces'].values()))

    def test_machine_crafting_uses_original_field_generator_tiers(self):
        folder=native_path(ROOT/'src/main/resources/data/gregtech/recipes/matter')
        for tier,emitter in enumerate(['lv','mv','hv','ev','iv'],1):
            name='massfab_osmium' if tier==1 else 'massfab_osmiridium_t'+str(tier)
            recipe=json.loads((folder/(name+'.json')).read_text(encoding='utf8'))
            self.assertEqual('gregtech:casing_machine_osmiridium',recipe['key']['M']['item'])
            self.assertEqual('gregtech:compact_force_field_emitter_'+emitter,recipe['key']['F']['item'])
            self.assertEqual(['RFS','FMF','RFS'],recipe['pattern'])
