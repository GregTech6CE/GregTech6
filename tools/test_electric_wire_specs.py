"""Compare every port electric family with the authoritative GT6 registration calls."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path

import re,json,unittest
from check_render_resources import check
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ORIGINAL=ROOT.parent/'gregtech6-master/gregtech6-master/src/main/java'
class WireSpecs(unittest.TestCase):
 def test_all_original_family_parameters(self):
  original=(ORIGINAL/'gregtech/loaders/b/Loader_MultiTileEntities.java').read_text(encoding='utf-8')
  rows=re.findall(r'addElectricWires\(\d+,\s*\d+,\s*V\[(\d+)\]\s*(?:\*\s*(\d+))?,\s*(\d+),\s*(\d+),\s*(\d+),\s*([TF]),\s*([TF]),\s*([TF]),.*?MT\.(\w+)\);',original)
  expected={mat:(8*4**int(tier)*int(mul or 1),int(amp),int(wire),int(cable),contact=='T',has=='T') for tier,mul,amp,wire,cable,contact,contact_cable,has,mat in rows}
  self.assertEqual(30,len(expected))
  port=(native_path(ROOT/'src/main/java/com/gregtech/gregtech/content/energy/WireDefinitions.java')).read_text(encoding='utf-8')
  found=re.findall(r'new WireDef\("([^"\n]+)", Materials\.(\w+), (\d+)L, (\d+), (\d+), (\d+), (true|false), (true|false)\), // MT\.(\w+)',port)
  actual={mat:(int(v),int(a),int(w),int(c),d=='true',has=='true') for suffix,material,v,a,w,c,d,has,mat in found}
  self.assertEqual(expected,actual)
  self.assertEqual(30,len({row[0] for row in found}))
 def test_geometry_assets_match_original_diameters_and_north_south_preview(self):
  original=(ORIGINAL/'gregapi/tileentity/connectors/MultiTileEntityWireElectric.java').read_text(encoding='utf-8')
  for kind in ('wire','cable'):
   rows=re.findall(r'OP.'+kind+r'Gt(\d+).*?NBT_DIAMETER, PX_P\[\s*(\d+)\]',original)
   self.assertEqual(16 if kind=='wire' else 5,len(rows))
   for size,width in rows:
    width=int(width)
    model=json.loads((native_path(ROOT/f'src/main/resources/assets/gregtech/models/block/blocks/{kind}_item_{size}.json')).read_text())
    elements=model['elements']
    self.assertEqual(width,max(e['to'][0] for e in elements)-min(e['from'][0] for e in elements))
    self.assertEqual(width,max(e['to'][1] for e in elements)-min(e['from'][1] for e in elements))
    self.assertEqual(0,min(e['from'][2] for e in elements));self.assertEqual(16,max(e['to'][2] for e in elements))
 def test_generated_model_dependencies(self):
  base=native_path(ROOT/'src/main/resources/assets/gregtech/models/block')
  paths=[]
  for kind,sizes in [('wire',range(1,17)),('cable',[1,2,4,8,12])]:
   for size in sizes:
    paths += [base/'blocks'/f'{kind}_item_{size:02d}.json',base/'machine'/f'{kind}_{size:02d}_core.json',base/'machine'/f'{kind}_{size:02d}_side.json']
  issues,_=check(paths)
  self.assertEqual([],issues)
 def test_no_recipe_references_removed_cables(self):
  invalid=re.compile(r'gregtech:cable_(?:16_[a-z_]+|(?:01|02|04|08|12)_(?:graphene|superconductor))(?![a-z_])')
  for path in (native_path(ROOT/'src/main/resources/data')).rglob('*.json'):
   self.assertIsNone(invalid.search(path.read_text(encoding='utf-8')),str(path))
  recipe=json.loads((native_path(ROOT/'src/main/resources/data/gregtech/recipes/hand_components/usb4_cable.json')).read_text())
  self.assertEqual({'item':'gregtech:wire_01_graphene'},recipe['key']['C'])
if __name__=='__main__':unittest.main()
