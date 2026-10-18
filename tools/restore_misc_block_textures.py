"""Replace generic wall placeholders with byte-for-byte original GT6 textures."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path

from pathlib import Path
import json, shutil, hashlib
ROOT=Path(__file__).resolve().parents[1]
ORIGINAL=ROOT.parent/'gregtech6-master/gregtech6-master'
ASSETS=native_path(ROOT/'src/main/resources/assets/gregtech')
MAPPING={
 'advanced_crafting_table':'craftingtables/advanced', 'charging_crafting_table':'craftingtables/charging',
 'filter_items':'extenders/filter_slots', 'filter_fluids':'extenders/filter_slots',
 'filter_items_fluids':'extenders/filter_slots', 'filter_oredict':'extenders/filter_prefix',
 'extender_basic':'extenders/inv', 'extender_advanced':'extenders/inv_tank',
 'extender_elite':'extenders/universal', 'extender_wireless':'extenders/bridge_universal',
 'long_dist_endpoint_item':'pipelines/item', 'long_dist_endpoint_fluid':'pipelines/fluid',
}
for metal in ('steel','aluminium','stainless','titanium','tungsten','ultimet'):
 MAPPING['auto_igniter_'+metal]='autotools/igniter'
for metal in ('steel','aluminium','titanium','tungsten'):
 MAPPING['auto_hammer_'+metal]='autotools/hammer'

def write(path,data): path.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
def main():
 manifest_path=ROOT/'tools/gt6_texture_sources.json'
 manifest=json.loads(manifest_path.read_text(encoding='utf-8'))
 def copy(source,target):
  target.parent.mkdir(parents=True,exist_ok=True); shutil.copyfile(source,target)
  manifest[target.relative_to(ASSETS).as_posix()]={'source':source.relative_to(ORIGINAL).as_posix(),'sha256':hashlib.sha256(source.read_bytes()).hexdigest()}
 for name,folder in MAPPING.items():
  source=native_path(ORIGINAL/'src/main/resources/assets/gregtech/textures/blocks/machines'/folder)
  for p in source.rglob('*.png*'): copy(p,ASSETS/'textures/block/machines'/folder/p.relative_to(source))
  children={}
  for layer in ('colored','overlay'):
   available={p.stem for p in (source/layer).glob('*.png')}
   faces={}
   textures={}
   for face,wanted in {'north':'front','south':'back','up':'top','down':'bottom','east':'side','west':'side'}.items():
    if wanted not in available: wanted=({'front':'in','back':'out'}.get(wanted,'side'))
    if wanted not in available: wanted='side'
    assert wanted in available,(name,layer,face)
    textures[face]=f'gregtech:block/machines/{folder}/{layer}/{wanted}'
    faces[face]={'texture':'#'+face,'cullface':face}
    if layer=='colored': faces[face]['tintindex']=0
   children[layer]={'textures':textures,'elements':[{'from':[0,0,0],'to':[16,16,16],'faces':faces}],'render_type':'minecraft:cutout'}
  model={'loader':'forge:composite','parent':'minecraft:block/block','textures':{'particle':children['colored']['textures']['down']},'children':children}
  write(ASSETS/f'models/block/misc/{name}.json',model)
 for name,icon in {'asphalt':'ASPHALT','concrete':'CONCRETE','concrete_reinforced':'CONCRETE_REINFORCED','cfoam':'CFOAM_HARDENED','cfoam_fresh':'CFOAM_FRESH'}.items():
  source=native_path(ORIGINAL/f'src/main/resources/assets/gregtech/textures/blocks/iconsets/{icon}.png')
  copy(source,ASSETS/f'textures/block/iconsets/{icon.lower()}.png')
  texture=f'gregtech:block/iconsets/{icon.lower()}'
  write(ASSETS/f'models/block/decoration/{name}.json',{'parent':'minecraft:block/cube_all','textures':{'all':texture}})
  panel=ASSETS/f'models/block/misc/panel_{name}.json'
  if panel.exists():
   data=json.loads(panel.read_text(encoding='utf-8'))
   # Preserve the panel mesh and its existing orientation/UV structure.
   def replace(node):
    if isinstance(node,dict):
     if 'textures' in node:
      node['textures']={key:texture for key in node['textures']}
     for value in node.values(): replace(value)
    elif isinstance(node,list):
     for value in node: replace(value)
   replace(data); write(panel,data)
 write(manifest_path,manifest)
 print(f'Restored {len(MAPPING)} machine models and 5 decoration texture families')
if __name__=='__main__': main()
