"""Import GT6 Loader_Tools suits, original names, recipes and attributed textures.

Requires explicit read-only original and resource-reference checkouts. No invented translations.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[1]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()

from pathlib import Path
import argparse, hashlib, json, re
ROOT=Path(__file__).resolve().parents[2]
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--source',type=Path,required=True)
p.add_argument('--reference',type=Path,required=True)
a=p.parse_args()
def load(path): return json.loads(path.read_text(encoding='utf-8'))
def save(path,value):
 path.parent.mkdir(parents=True,exist_ok=True)
 path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
assets=ROOT/'core/src/main/resources/assets/gregtech'
data=ROOT/'core/src/main/resources/data/gregtech'
source=native_path(a.source/'src/main/java/gregtech/loaders/a/Loader_Tools.java')
rows=re.findall(r'"(gt\.armor\.hazmat\.(\w+)\.(\w+))"\s*,\s*"([^"]+)"\s*,\s*"([^"]+)"\s*,\s*"([^"]+)"',source.read_text(encoding='utf-8'))
assert len(rows)==24
en=load(assets/'lang/en_us.json');aliases=load(ROOT/'tools/localization/aliases.json');english=load(ROOT/'tools/localization/english_source.json')
parts={'head':('helmet',0),'chest':('chestplate',1),'legs':('leggings',2),'boots':('boots',3)}
provenance=[];recipes=[]
for key,family,part,name,tip,texture in rows:
 slot,index=parts[part];id=f'{family}_suit_{slot}'
 for dest,original,value in [(f'item.gregtech.{id}',key,name),(key+'.tooltip_main',key+'.tooltip_main',tip)]:
  en[dest]=value;aliases[dest]=original;english['values'][dest]={'source_key':original,'value':value}
 for prefix,checkout in [('armor',a.source),('items/armor',a.reference)]:
  rel=f'textures/{prefix}/{texture}/{index}.png';src=native_path(checkout/'src/main/resources/assets/gregtech'/rel)
  dst=assets/rel;dst.parent.mkdir(parents=True,exist_ok=True);dst.write_bytes(src.read_bytes())
  provenance.append({'source':str(src),'destination':str(dst.relative_to(ROOT)),'sha256':sha(src)})
 save(assets/f'models/item/{id}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'gregtech:items/armor/{texture}/{index}'}})
 if family=='radiation':continue # Existing source recipe, stable ID.
 tools={'l':{'tag':'forge:tools/file'},'q':{'tag':'forge:tools/scissors'},'x':{'tag':'forge:tools/wire_cutter'}}
 if family=='universal':
  pattern=['AlB','CqD','ExF'];keys={k:{'item':f'gregtech:{f}_suit_{slot}'} for k,f in zip('ABCDE',['biochemgas','insect','frost','heat','radiation'])}
  keys['F']={'item':'minecraft:chainmail_'+slot}
 else:
  tool='x' if family=='heat' else 'q'
  pattern=[['MMM','MGM',f'{tool} l'],[f'M{tool}M','MMM','MMM'],['MMM',f'M{tool}M','MlM'],['MlM',f'M{tool}M']][index]
  ingredient={'insect':'gregtech:technology/any_rubber_foil','frost':'gregtech:plate/asbestos','heat':'gregtech:foil/aluminium','biochemgas':'gregtech:technology/any_rubber_plate'}[family]
  keys={'M':{'tag':ingredient}}
  if index==0:keys['G']={'item':'minecraft:glass_pane' if family=='biochemgas' else 'minecraft:black_stained_glass_pane'}
 keys.update({k:v for k,v in tools.items() if k in ''.join(pattern)})
 file=f'hazmat/{id}.json';recipes.append(file)
 save(data/'recipes'/file,{'type':'gregtech:tool_shaped','pattern':pattern,'key':keys,'result':{'item':'gregtech:'+id},'allow_mirror':False,'gregtech_autocraftable':False})
tags={'hazmat_chem':['biochemgas','universal'],'hazmat_gas':['biochemgas','universal'],'hazmat_bio':['biochemgas','universal'],'hazmat_electric':['universal'],'hazmat_heat':['heat','universal'],'hazmat_frost':['frost','universal'],'hazmat_insect':['insect','universal'],'radiation_protection':['radiation','universal']}
for tag,families in tags.items():
 save(data/f'tags/items/{tag}.json',{'replace':False,'values':[f'gregtech:{family}_suit_{slot}' for family in families for slot,index in parts.values()]})
catalog=ROOT/'core/src/main/java/com/gregtech/gregtech/content/recipe/EquipmentCraftingCatalog.java';text=catalog.read_text(encoding='utf-8')
anchor='    public static final java.util.List<String> FILES = java.util.List.of(\n'
text=text.replace(anchor,anchor+''.join('            '+json.dumps(f)+',\n' for f in recipes if '"'+f+'"' not in text));catalog.write_text(text,encoding='utf-8')
save(assets/'lang/en_us.json',en);save(ROOT/'tools/localization/aliases.json',dict(sorted(aliases.items())))
if not any(x['path']==str(source) for x in english['source_files']):english['source_files'].append({'path':str(source),'sha256':sha(source)})
save(ROOT/'tools/localization/english_source.json',english)
save(ROOT/'docs/integration/verification/hazmat-source-20261018.json',{'author':'Gregorius Techneticies / GregTech-6 Team; item icon reference distributed by wolfram0108','code_source':str(source),'code_sha256':sha(source),'code_license':'LGPL-3.0-or-later','asset_license':'CC0-1.0; original and reference LICENSE.assets','assets':provenance})
print('24 suit identities, 20 added recipes, 8 protection tags; original English and Chinese aliases pinned')
