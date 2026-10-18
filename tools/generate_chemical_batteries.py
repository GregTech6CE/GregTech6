"""GT6 chemical batteries: original recipe rows, textures, bounds and standard Chinese names."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()

import json
import re
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
ORIGINAL=ROOT.parent/'gregtech6-master/gregtech6-master/src/main'
ASSETS=native_path(ROOT/'src/main/resources/assets/gt6ce')
DATA=native_path(ROOT/'src/main/resources/data/gt6ce')
CHEM=[('Lead_Acid','lead_acid','Lead-Acid','lead_acid_cell_filled'),('Alkaline','alkaline','Alkaline','alkaline_button_cell_filled'),('NiCd','nickel_cadmium','Nickel-Cadmium','nickel_cadmium_cell_filled'),('LiCoO2','lithium_cobalt','Lithium-Cobalt','lithium_cobalt_cell_filled'),('LiMn','lithium_manganese','Lithium-Manganese','lithium_manganese_cell_filled')]
TIERS=['ulv','lv','mv','hv','ev']
CIRCUITS=['basic','good','advanced','elite','master','ultimate']

def write(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    text=json.dumps(data,ensure_ascii=False,indent=2)+'\n'
    if not path.exists() or path.read_text(encoding='utf-8')!=text:path.write_text(text,encoding='utf-8')

def run():
    source=(ORIGINAL/'java/gregtech/loaders/b/Loader_MultiTileEntities.java').read_text(encoding='utf-8')
    cn=(ROOT.parent/'GregTech.lang').read_text(encoding='utf-8')
    translations={lang:json.loads((ASSETS/f'lang/{lang}.json').read_text(encoding='utf-8')) for lang in ['en_us','zh_cn']}
    for kind in ['standard','advanced']:
        for tier,voltage in enumerate([8,32,128,512,2048]):
            inset=[5,5,4,3,2][tier];height=[8,11,11,11,13][tier];scale=[4,7,7,7,9][tier]
            folder=f'block/machines/batteries/eu/{kind}/{voltage}'
            for face in ['bottom','top','sides','bar']:
                origin=ORIGINAL/f'resources/assets/gregtech/textures/blocks/machines/batteries/eu/{kind}/{voltage}/{face}.png'
                target=ASSETS/f'textures/{folder}/{face}.png'
                target.parent.mkdir(parents=True,exist_ok=True)
                if not target.exists() or target.read_bytes()!=origin.read_bytes():target.write_bytes(origin.read_bytes())
            body={'from':[inset,0,inset],'to':[16-inset,height,16-inset],'faces':{face:{'texture':'#'+('top' if face=='up' else 'bottom' if face=='down' else 'sides')} for face in ['north','south','east','west','up','down']}}
            for energy in range(scale+1):
                elements=[body]
                if energy:
                    elements.append({'from':[inset-.032,1,inset-.032],'to':[16-inset+.032,energy+1,16-inset+.032],'faces':{face:{'texture':'#bar','tintindex':0} for face in ['north','south','east','west']}})
                model={'parent':'minecraft:block/block','render_type':'minecraft:cutout','textures':{face:f'gt6ce:{folder}/{face}' for face in ['bottom','top','sides','bar']},'elements':elements}
                model['textures']['particle']=model['textures']['sides']
                write(ASSETS/f'models/block/chemical_battery/{kind}_{voltage}_{energy}.json',model)
    for chemistry,(old,name,en,cell) in enumerate(CHEM):
        for tier,suffix in enumerate(TIERS):
            number=14000+chemistry*10+tier;voltage=8*4**tier;ident=f'battery_{name}_{suffix}'
            line=next(line for line in source.splitlines() if re.search(r'IL\.Battery_'+old+'_'+suffix.upper()+r'\s*\.set\(aRegistry\.add',line))
            assert re.search(r',\s*'+str(number)+r'\s*,',line)
            rows=[s for s in re.findall(r'"([^"]*)"',line) if re.fullmatch(r'[ WxPBC]+',s)]
            assert len(rows)==3,(ident,rows)
            keys={'x':{'item':'gt6ce:tool_wire_cutter'},'P':{'item':'gt6ce:plate_batteryalloy'},'B':{'item':'gt6ce:'+cell},'W':{'item':'gt6ce:cable_01_'+['lead','tin','copper','gold','aluminium'][tier]}}
            if 'C' in ''.join(rows):
                ct=int(re.search(r"'C', OD_CIRCUITS\[(\d+)\]",line).group(1))
                keys['C']={'tag':f'gt6ce:circuits_tier_{ct}_plus'}
            keys={symbol:value for symbol,value in keys.items() if symbol in ''.join(rows)}
            write(DATA/f'recipes/chemical_batteries/{ident}.json',{'type':'gt6ce:tool_shaped','allow_mirror':False,'pattern':rows,'key':keys,'result':{'item':'gt6ce:'+ident}})
            kind='advanced' if chemistry>=3 else 'standard';stem=f'gt6ce:block/chemical_battery/{kind}_{voltage}_'
            write(ASSETS/f'blockstates/{ident}.json',{'variants':{'':{'model':stem+'0'}}})
            scale=[4,7,7,7,9][tier]
            write(ASSETS/f'models/item/{ident}.json',{'parent':stem+'0','overrides':[{'predicate':{'gt6ce:battery_charge':energy/scale},'model':stem+str(energy)} for energy in range(1,scale+1)]})
            translations['en_us']['block.gt6ce.'+ident]=f'{en} Battery ({suffix.upper()})'
            translations['zh_cn']['block.gt6ce.'+ident]=re.search(r'S:gt\.multitileentity\.'+str(number)+r'=([^\r\n]+)',cn).group(1)
    # Retained prototype IDs use the very same chemistry assets, never the old generic-node models.
    for tier,suffix in enumerate(TIERS):
        old='battery_eu_'+str(8*4**tier);canonical='battery_lithium_cobalt_'+suffix
        for folder in ['blockstates','models/item']:
            write(ASSETS/f'{folder}/{old}.json',json.loads((ASSETS/f'{folder}/{canonical}.json').read_text(encoding='utf-8')))
        for lang in translations:translations[lang]['block.gt6ce.'+old]=translations[lang]['block.gt6ce.'+canonical]
    # Circuit tags are maintained by generate_battery_box_recipes.circuit_tags.
    for tier,suffix in enumerate(TIERS+['iv']):
        values=['gt6ce:battery_'+name+'_'+suffix for _,name,_,_ in CHEM] if tier<5 else []
        if tier<5:values.append('gt6ce:battery_eu_'+str(8*4**tier))
        if tier>0:values.append('gt6ce:battery_'+suffix)
        write(DATA/f'tags/items/rechargeable_batteries/{suffix}.json',{'replace':False,'values':values})
    for lang in translations:
        translations[lang]['tooltip.gt6ce.chemical_battery.packet']='EU packet size: %s–%s' if lang=='en_us' else 'EU 能量包大小：%s–%s'
        translations[lang]['tooltip.gt6ce.chemical_battery.place']='Sneak to place; stored charge is retained when picked up' if lang=='en_us' else '潜行放置；挖回时保留电量'
        write(ASSETS/f'lang/{lang}.json',translations[lang])
    print('Generated 25 GT6 battery recipes/blocks/items, shared charged models, 40 original textures and battery/circuit tags.')

if __name__=='__main__':run()
