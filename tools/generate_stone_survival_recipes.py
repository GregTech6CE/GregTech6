
# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()

from pathlib import Path
import json
base=native_path(Path('src/main/resources'));out=base/'data/gregtech/recipes/stone_survival';out.mkdir(exist_ok=True)
count=0
for file in (base/'assets/gregtech/models/item').glob('stone_*_stone.json'):
 family=file.stem[:-6]
 for name,source,target in [('cobble_firing','cobble','stone'),('stone_polishing','stone','smooth')]:
  data={'type':'minecraft:smelting','ingredient':{'item':'gregtech:'+family+'_'+source},'result':'gregtech:'+family+'_'+target,'experience':0.0,'cookingtime':200}
  (out/(family+'_'+name+'.json')).write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8');count+=1
 data={'type':'minecraft:crafting_shaped','pattern':['SS','SS'],'key':{'S':{'item':'gregtech:'+family+'_stone'}},'result':{'item':'gregtech:'+family+'_bricks','count':4}}
 (out/(family+'_bricks.json')).write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8');count+=1
print('Stone crafting/furnace recipes:',count)
