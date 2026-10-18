"""Dynamite's active/embedded models preserve GT6 art and agree with six-face bounds."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[1]))
from native_layout import native_path

import itertools
import json
import math
from pathlib import Path
import unittest
ROOT = Path(__file__).resolve().parents[2]
ASSETS = native_path(ROOT / 'src/main/resources/assets/gregtech')
ORIGINAL = ROOT.parent / 'gregtech6-master/gregtech6-master/src/main/resources/assets/gregtech/textures/blocks/machines/tools/dynamite'
class DynamiteModelTests(unittest.TestCase):
    def test_all_states_match_the_visible_and_collision_bounds(self):
        directions={'east':(0,1),'west':(0,-1),'up':(1,1),'down':(1,-1),'south':(2,1),'north':(2,-1)}
        for name in ('dynamite','boomstick','strong_dynamite'):
            variants=json.loads((ASSETS/f'blockstates/{name}.json').read_text())['variants']
            self.assertEqual(24,len(variants))
            for key,variant in variants.items():
                state=dict(part.split('=') for part in key.split(','))
                axis,sign=directions[state['facing']]
                model=json.loads((ASSETS/('models/'+variant['model'].split(':')[1]+'.json')).read_text())
                for layer,child in model['children'].items():
                    element=child['elements'][0]
                    points=[]
                    for point in itertools.product(*zip(element['from'],element['to'])):
                        x,y,z=(v-8 for v in point)
                        a=math.radians(-variant.get('x',0)); y,z=y*math.cos(a)-z*math.sin(a),y*math.sin(a)+z*math.cos(a)
                        a=math.radians(-variant.get('y',0)); x,z=x*math.cos(a)+z*math.sin(a),-x*math.sin(a)+z*math.cos(a)
                        points.append((x+8,y+8,z+8))
                    expected=(0,16) if state['sunk']=='false' else (0,2) if sign>0 else (14,16)
                    self.assertAlmostEqual(expected[0],min(p[axis] for p in points),msg=key)
                    self.assertAlmostEqual(expected[1],max(p[axis] for p in points),msg=key)
                    for other in range(3):
                        if other != axis:
                            self.assertAlmostEqual(5,min(p[other] for p in points))
                            self.assertAlmostEqual(11,max(p[other] for p in points))
                    for texture in child['textures'].values():
                        self.assertEqual(state['armed']=='true','_active/' in texture)
                        self.assertTrue((ASSETS/('textures/'+texture.split(':')[1]+'.png')).is_file())
    def test_original_textures_and_animation_metadata(self):
        for folder in ('colored','overlay','colored_active','overlay_active'):
            for source in (ORIGINAL/folder).iterdir():
                if source.suffix in ('.png','.mcmeta'):
                    self.assertEqual(source.read_bytes(),(ASSETS/f'textures/block/machines/tools/dynamite/{folder}'/source.name).read_bytes())
if __name__=='__main__': unittest.main()
