"""Bake the existing idle boiler dial into inventory-only models on both loaders."""
from pathlib import Path
import argparse,copy,json
ROOT=Path(__file__).resolve().parents[2]
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--check',action='store_true');a=p.parse_args()
def put(path,obj):
 text=json.dumps(obj,indent=2)+'\n'
 if a.check:assert path.read_text(encoding='utf-8')==text,path
 else:path.parent.mkdir(parents=True,exist_ok=True);path.write_text(text,encoding='utf-8')
count=0
for platform in ['src','neoforge/src']:
 models=ROOT/platform/'main/resources/assets/gregtech/models/block/machine'
 boiler=json.loads((models/'boiler/steam_boiler_inventory.json').read_text(encoding='utf-8'))
 children=boiler.get('children',boiler.get('parts'))
 for path in (models/'multiblock').glob('*_boiler_main_barometer.json'):
  model=json.loads(path.read_text(encoding='utf-8'))
  model.get('children',model.get('parts')).update({k:copy.deepcopy(children[k]) for k in ['gauge','needle']})
  put(path.with_name(path.stem+'_inventory.json'),model)
  put(ROOT/'core/src/main/resources/assets/gregtech/models/item'/path.name,
      {'parent':'gregtech:block/machine/multiblock/'+path.stem+'_inventory'})
  count+=1
print(f'{count} native inventory gauge models '+('checked' if a.check else 'generated'))
