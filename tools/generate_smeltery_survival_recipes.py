
# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()

from pathlib import Path
import re,json
r=native_path(Path('src/main/resources')); models=r/'assets/gt6ce/models/item'
source=Path('../gregtech6-master/gregtech6-master/src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java').read_text(encoding='utf-8')
# Exact symbol-to-port item suffixes; do not translate localized display strings.
names={'Pb':'lead','Bi':'bismuth','Cu':'copper','Cr':'chromium','Ti':'titanium','W':'tungsten','Ta':'tantalum','Mo':'molybdenum','Nb':'niobium','Os':'osmium','Ir':'iridium','V':'vanadium','Ta4HfC5':'ta4hfc5','SiC':'silicon_carbide','C':'carbon','Quartz':'quartz'}
def snake(s):return re.sub(r'(?<=[a-z0-9])(?=[A-Z])','_',s).lower()
def suffix(s):return names.get(s,snake(s))
registry=native_path(Path('src/main/java/com/gregtech/gregtech/registry/GTMachines.java')).read_text(encoding='utf-8')
crucibles={int(n):name for name,n in re.findall(r'crucible\("([^"]+)",\s*[^,]+,\s*(\d+)',registry)}
out=r/'data/gt6ce/recipes/smeltery_survival';out.mkdir(exist_ok=True)
manifest={};skipped=[]
for number,line in enumerate(source.splitlines(),1):
 if line.lstrip().startswith('//'):continue
 m=re.search(r'aMat = (?:MT\.(?:STONES\.)?|ANY\.)(\w+);\s*aRegistry.add\("([^"]+)".*?,\s*(\d+),\s*\d+, aClass',line)
 if not m:continue
 mat,title,meta=m.groups(); mat=suffix(mat); output=None
 if 'Smelting Crucible (' in title:output=crucibles.get(int(meta))
 elif 'Burning Box (' in title:
  kind='solid' if 'Solid' in title else 'liquid' if 'Liquid' in title else 'fluidbed' if 'Fluid' in title else None
  if kind:output='burning_box_'+kind+('_dense_' if 'Dense' in title else '_')+mat
 elif 'Steam Boiler Tank (' in title:output=('strong_' if 'Strong' in title else '')+'steam_boiler_'+mat
 if not output or not (models/(output+'.json')).exists():continue
 pat=re.search(r'\), "([^"]{1,3})", "([^"]{1,3})", "([^"]{1,3})", (.*)\);',line)
 if not pat:skipped.append([output,'no supported literal pattern']);continue
 a,b,c,tail=pat.groups();key={};bad=False
 for marker,form,target in re.findall(r"'([A-Z])', OP\.(\w+)\.dat\((aMat|ANY\.\w+|MT\.\w+)\)",tail):
  material=mat if target=='aMat' else suffix(target.split('.')[-1]); item=snake(form)+'_'+material
  # Item suffixes and machine suffixes differ for several compound materials.
  material=material.replace('ta4hfc5','tantalum_hafnium_carbide')
  if form=='stone':
   special={'nether_brick':'minecraft:nether_bricks','quartz':'minecraft:quartz_block'}
   candidate='stone_'+material+'_stone'
   if material in special:key[marker]={'item':special[material]}
   elif (models/(candidate+'.json')).exists():key[marker]={'item':'gt6ce:'+candidate}
  else:key[marker]={'tag':('forge:plates/' if form=='plate' else 'gt6ce:'+snake(form)+'/')+material}
 for marker,block in re.findall(r"'([A-Z])', Blocks\.(\w+)",tail):
  vanilla={'stone':'stone','brick_block':'bricks'}.get(block)
  if vanilla:key[marker]={'item':'minecraft:'+vanilla}
 for marker,tool in {'h':'hammer','w':'wrench','y':'chisel'}.items():
  if marker in a+b+c:key[marker]={'item':'gt6ce:tool_'+tool}
 if "'P', MD.HBM.mLoaded" in tail:
  form=re.search(r'OP\.(\w+)\.dat',tail).group(1)
  key['P']={'tag':'gt6ce:'+snake(form)+'/'+mat}
 if "OD.craftingFirestarter" in tail:key['F']={'item':'minecraft:flint_and_steel'}
 missing=set(a+b+c)-set(key)-{' '}
 if missing:skipped.append([output,'unresolved ingredients '+str(sorted(missing))]);continue
 data={'type':'gt6ce:tool_shaped','pattern':[a,b,c],'key':key,'result':{'item':'gt6ce:'+output}}
 (out/(output+'.json')).write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
 manifest['gt6ce:smeltery_survival/'+output]={'source_line':number,'output':'gt6ce:'+output}
extras={
 'clay_crucible':{'type':'gt6ce:tool_shaped','pattern':['CkC','CRC','CCC'],'key':{'C':{'item':'minecraft:clay_ball'},'k':{'item':'gt6ce:tool_knife'},'R':{'item':'gt6ce:tool_rolling_pin'}},'result':{'item':'gt6ce:clay_crucible'}},
 'ceramic_crucible_firing':{'type':'minecraft:smelting','ingredient':{'item':'gt6ce:clay_crucible'},'result':'gt6ce:smelting_crucible_ceramic','experience':0.0,'cookingtime':200},
 'clay_crucible_reclaim':{'type':'minecraft:crafting_shapeless','ingredients':[{'item':'gt6ce:clay_crucible'}],'result':{'item':'minecraft:clay_ball','count':7}}
}
for name,data in extras.items():
 (out/(name+'.json')).write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
 manifest['gt6ce:smeltery_survival/'+name]={'source':'MultiItemRandomTools.java:113,125; Loader_MultiTileEntities.java:256'}
Path('docs/smeltery-survival-recipes.json').write_text(json.dumps({'recipes':manifest,'skipped':skipped},indent=2)+'\n',encoding='utf-8')
print('Crafting recipes:',len(manifest),'unsupported:',len(skipped))
