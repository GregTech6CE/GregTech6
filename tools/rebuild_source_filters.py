"""Original filter assets and crafting, preserving existing port registry IDs."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path

import json
from pathlib import Path
from rebuild_container_tool_models import write, ASSETS
from rebuild_source_extenders import model, DIRECTIONS

ROOT = Path(__file__).resolve().parents[1]
FILTERS = [('filter_items','filter_slots',[' hY','ZMZ','Yw ']),
           ('filter_fluids','filter_slots',['Xh ','ZMZ',' wX']),
           ('filter_items_fluids','filter_slots',['XhY','ZMF','YwX']),
           ('filter_oredict','filter_prefix',['XZY','hMw','YZX'])]


def recipe(name, data):
    path=native_path(ROOT/f'src/main/resources/data/gregtech/recipes/logistics/{name}.json')
    path.parent.mkdir(parents=True,exist_ok=True)
    text=json.dumps(data,indent=2)+'\n'
    if not path.exists() or path.read_text(encoding='utf-8')!=text:path.write_text(text,encoding='utf-8')


def main():
    opposite=dict(zip(DIRECTIONS,('up','down','south','north','east','west')))
    for texture in ['filter_slots','filter_prefix']:
        for front in DIRECTIONS:
            for secondary in DIRECTIONS:
                write(f'models/block/logistics/{texture}/{front}_{secondary}.json',model(texture,front,secondary))
    for name,texture,pattern in FILTERS:
        variants={}
        for front in DIRECTIONS:
            for secondary in DIRECTIONS:
                for explicit in [False,True]:
                    actual=secondary if explicit else opposite[front]
                    variants[f'facing={front},secondary={secondary},secondary_set={str(explicit).lower()}']={'model':f'gregtech:block/logistics/{texture}/{front}_{actual}'}
        write(f'blockstates/{name}.json',{'variants':variants})
        write(f'models/item/{name}.json',{'parent':f'gregtech:block/logistics/{texture}/north_south'})
        keys={'M':'casing_machine_double_steelgalvanized' if name in ['filter_items_fluids','filter_oredict'] else 'casing_machine_steelgalvanized',
              'X':'pipe_medium_galvanized_steel','Y':'item_pipe_medium_electrum','Z':'fluid_filter' if name=='filter_fluids' else 'item_filter',
              'F':'fluid_filter','h':'tool_hammer','w':'tool_wrench'}
        used=set(''.join(pattern))-{' '}
        recipe(name,{'type':'gregtech:tool_shaped','pattern':pattern,'key':{k:{'item':'gregtech:'+keys[k]} for k in sorted(used)},'result':{'item':'gregtech:'+name}})
        recipe(name+'_reset',{'type':'minecraft:crafting_shapeless','ingredients':[{'item':'gregtech:'+name}],'result':{'item':'gregtech:'+name}})
    recipe('blank_cover',{'type':'gregtech:tool_shaped','pattern':['Sh','Pd'],'key':{k:{'item':'gregtech:'+v} for k,v in {'S':'screw_aluminium','P':'plate_aluminium','h':'tool_hammer','d':'tool_screwdriver'}.items()},'result':{'item':'gregtech:blank_cover'}})
    for name,pattern in [('item_filter',[' Z ','ZQZ',' Z ']),('fluid_filter',['Z Z',' Q ','Z Z'])]:
        recipe(name,{'type':'minecraft:crafting_shaped','pattern':pattern,'key':{'Q':{'item':'gregtech:blank_cover'},'Z':{'item':'gregtech:foil_zinc'}},'result':{'item':'gregtech:'+name}})


if __name__=='__main__':main()
