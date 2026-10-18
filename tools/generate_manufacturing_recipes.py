"""Generate wire recipes and GT6's orientation-sensitive extruder shape tree.
Source checkout is read-only; no GTM code is used. Run from any directory.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()

from pathlib import Path
import json,re
ROOT=Path(__file__).resolve().parents[1]
ORIGINAL=ROOT.parent/'gregtech6-master/gregtech6-master/src/main/java'
def write(folder,name,data):
    path=native_path(ROOT/'src/main/resources/data/gt6ce/recipes'/folder/(name+'.json'))
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,indent=2)+'\n',encoding='utf8')
def item(name):return {'item':'gt6ce:'+name}
def shapeless(name,inputs,output,count=1):
    write('wire_working',name,{'type':'minecraft:crafting_shapeless','ingredients':[item(x) for x in inputs],'result':item(output)|{'count':count}})
def main():
    defs=(native_path(ROOT/'src/main/java/com/gregtech/gregtech/content/energy/WireDefinitions.java')).read_text(encoding='utf8')
    materials=re.findall(r'new WireDef\("([a-z_]+)"',defs)
    for material in materials:
        wire=lambda size:f'wire_{size:02d}_{material}'
        for big in range(2,17):
            shapeless(f'unpack_{big:02d}_{material}',[wire(big)],wire(1),big)
            for small in range(1,big):
                if big%small==0 and big//small<10:
                    shapeless(f'weave_{small:02d}_{big:02d}_{material}',[wire(small)]*(big//small),wire(big))
        for size in ([] if material in {"graphene","superconductor"} else [1,2]):shapeless(f'insulate_{size:02d}_{material}',[wire(size),'plate_rubber'],f'cable_{size:02d}_{material}')
    source=(ORIGINAL/'gregtech/items/MultiItemTechnological.java').read_text(encoding='utf8')
    names={'Empty':'empty','Plate':'plate','Rod_Long':'longrod','Bolt':'bolt','Ring':'ring','Cell':'cell','Ingot':'ingot','Wire':'wire','Casing':'casing',
           'Pipe_Tiny':'tinypipe','Pipe_Small':'smallpipe','Pipe_Medium':'mediumpipe','Pipe_Large':'largepipe','Pipe_Huge':'hugepipe','Block':'block',
           'Sword':'swordblade','Pickaxe':'pickaxehead','Shovel':'shovelhead','Axe':'axehead','Hoe':'hoehead','Hammer':'hammerhead','File':'filehead',
           'Saw':'sawblade','Gear':'gear','Bottle':'bottle','Plate_Curved':'curvedplate','Gear_Small':'smallgear','Rod':'rod','CCC':'capsulecellcontainer',
           'Foil':'foil','Plate_Tiny':'tinyplate','Wire_Fine':'finewire'}
    count=0
    for family,prefix in [('Extruder','extruder_shape_'),('SimpleEx','low_heat_extruder_shape_')]:
        pattern=r'CR.shaped\(IL.Shape_'+family+r'_(\w+)\s*\.get\(1\), CR.DEF_REV\s*,(.*)'
        for result,body in re.findall(pattern,source):
            rows=re.findall(r'"([ hfxP]+)"',body)
            if result=='Empty':keys={'h':item('tool_hammer'),'f':item('tool_file'),'x':item('tool_wire_cutter'),'P':item('plate_double_'+('steel' if family=='SimpleEx' else 'tungstencarbide'))}
            else:
                parent=re.search(r"'P', IL.Shape_"+family+r'_(\w+)',body).group(1)
                keys={'P':item(prefix+names[parent]),'x':item('tool_wire_cutter')}
            write('extruder_shapes',prefix+names[result],{'type':'gt6ce:tool_shaped','allow_mirror':False,'pattern':rows,'key':keys,'result':item(prefix+names[result])|{'count':1}})
            count+=1
    print(f'{len(materials)} conductor materials; {count} extruder shapes')
if __name__=='__main__':main()
