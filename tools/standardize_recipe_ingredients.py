"""Use concrete Forge material-form tags in recipe ingredients, never in results."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path

from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
FORMS={'ingot':'ingots','nugget':'nuggets','dust':'dusts','dust_small':'small_dusts','dust_tiny':'tiny_dusts','gem':'gems','plate':'plates','stick':'rods','stick_long':'long_rods','gear_gt':'gears','ring':'rings','bolt':'bolts','screw':'screws','foil':'foils','ore_raw':'raw_materials'}
MATERIAL_NAMES={'stainlesssteel':'stainless_steel','neodymiummagnetic':'neodymium_magnetic','tungstensteel':'tungsten_steel','steelmagnetic':'steel_magnetic','steelgalvanized':'steel_galvanized','ironmagnetic':'iron_magnetic','tinalloy':'tin_alloy','tantalumhafniumcarbide':'tantalum_hafnium_carbide','enderiumbase':'enderium_base'}
VANILLA={'iron_ingot':'ingots/iron','gold_ingot':'ingots/gold','copper_ingot':'ingots/copper','iron_nugget':'nuggets/iron','gold_nugget':'nuggets/gold','diamond':'gems/diamond','emerald':'gems/emerald','quartz':'gems/quartz','redstone':'dusts/redstone'}
def tag(item):
 ns,_,path=item.partition(':')
 if ns=='minecraft':return 'forge:'+VANILLA[path] if path in VANILLA else None
 if ns!='gregtech':return None
 for form in sorted(FORMS,key=len,reverse=True):
  if path.startswith(form+'_'):
   material=path[len(form)+1:]
   # Do not infer subclasses (double ingots, small gears, gem grades) from a shorter prefix.
   if any(material.startswith(x+'_') for x in ['double','triple','quadruple','quintuple','dense','tiny','small','long','curved','fine','flawless','flawed','chipped','exquisite']):return None
   if material=='nether_quartz':material='quartz'
   return 'forge:'+FORMS[form]+'/'+MATERIAL_NAMES.get(material,material)
 return None
def main():
 manifest={};changed=0
 def visit(value):
  nonlocal changed
  if isinstance(value,list):
   for v in value:visit(v)
  elif isinstance(value,dict):
   if set(value)=={'item'} and (target:=tag(value['item'])):
    manifest[value['item']]=target;value.clear();value['tag']=target;changed+=1
   else:
    for v in value.values():visit(v)
 for p in (native_path(ROOT/'src/main/resources/data/gregtech/recipes')).rglob('*.json'):
  data=json.loads(p.read_text(encoding='utf-8'));before=json.dumps(data)
  for key in ['key','ingredients','ingredient','base','addition']: 
   if key in data:visit(data[key])
  if before!=json.dumps(data):p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 out=ROOT/'docs/standardized-ingredients.json'
 if out.exists():manifest={**json.loads(out.read_text(encoding='utf-8')),**manifest}
 out.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 print('Replaced ingredient references:',changed,'distinct mappings:',len(manifest))
if __name__=='__main__':main()
