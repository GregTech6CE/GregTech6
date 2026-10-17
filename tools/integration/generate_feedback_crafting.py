"""Source GT6 BlockBaseSpike:65-78 / MultiItemRandomTools:555-577; LGPL-3.0-or-later."""
from pathlib import Path
import argparse,json
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--check',action='store_true')
args=parser.parse_args()
ROOT=Path(__file__).resolve().parents[2]
files=[]
def save(name, recipe):
    file='catalog_repairs/'+name+'.json'
    path=ROOT/'core/src/main/resources/data/gregtech/recipes'/file
    text=json.dumps(recipe,indent=2)+'\n'
    if args.check: assert path.read_text(encoding='utf-8')==text, 'Recipe drift: '+file
    else:path.write_text(text,encoding='utf-8')
    files.append(file)
for tape,material in [('tape',{'item':'minecraft:paper'}),('duct_tape',{'tag':'gregtech:technology/any_plastic_foil'}),
                      ('braintech_aerospace_advanced_reinforced_duct_tape_fal_84',{'tag':'gregtech:technology/any_tungsten_foil'})]:
    result={'item':'gregtech:'+tape,'count':1}
    save(tape,{'type':'gregtech:tool_shaped','pattern':['PPP',' G '],
               'key':{'P':material,'G':{'tag':'forge:glue'}},'result':result})
    save(tape+'_repack',{'type':'minecraft:crafting_shapeless','ingredients':[{'item':'gregtech:'+tape+'_2'}]*4,'result':result})
for family,materials in [('metal',['copper','lead']),('steel',['blue_steel','red_steel']),('sharp',['steel','titanium']),
                         ('fancy',['gold','silver']),('super',['tungstensteel','adamantium'])]:
    for secondary,mat in enumerate(materials):
        for mode,pattern in [('wall',['BTB','TPT','BTB']),('omni',['TBT','BPB','TBT'])]:
            save('spike_'+family+'_'+mat+'_'+mode,{'type':'gregtech:tool_shaped','pattern':pattern,
                 'key':{key:{'tag':'gregtech:'+prefix+'/'+('tungsten_steel' if mat=='tungstensteel' else mat)} for key,prefix in [('B','tool_head_sword'),('T','screw'),('P','plate')]},
                 'result':{'item':'gregtech:spike_'+family,'count':1,'nbt':{'BlockStateTag':{'mode':mode,'secondary':str(bool(secondary)).lower()}}}})
catalog=ROOT/'core/src/main/java/com/gregtech/gregtech/content/recipe/EquipmentCraftingCatalog.java'
text=catalog.read_text(encoding='utf-8')
anchor='    public static final java.util.List<String> FILES = java.util.List.of(\n'
new=''.join('            '+json.dumps(f)+',\n' for f in files if '"'+f+'"' not in text)
text=text.replace(anchor,anchor+new)
if args.check: assert catalog.read_text(encoding='utf-8')==text,'Feedback recipes missing from NeoForge adapter'
else:catalog.write_text(text,encoding='utf-8')
print('Source feedback crafting recipes:',len(files))
