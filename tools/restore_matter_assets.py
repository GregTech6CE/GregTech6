"""Original matter-fabricator parts and obtainable controller/processor recipes."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path

import json,shutil,hashlib
from tools.restore_laser_models import ROOT,ORIGINAL,ASSETS,SOURCE,write,cube,layer

PARTS={'dense_lead_wall':('metalwalldense','Dense Lead Wall','致密铅墙'),
       'large_osmium_coil':('coil','Large Osmium Coil','大型锇线圈'),
       'conversion_processor_unit':('processorconversion','Conversion Quadcore Processor Unit','转换四核处理器组件')}

def main():
    manifest=ROOT/'tools/gt6_texture_sources.json';entries=json.loads(manifest.read_text(encoding='utf8'))
    for name,(folder,en,zh) in PARTS.items():
        children={}
        for part in ['colored','overlay']:
            textures={}
            for face in ['up','down','north','south','east','west']:
                basename='top' if face=='up' else 'bottom' if face=='down' else 'side'
                source=SOURCE/f'machines/multiblockparts/{folder}/0/{part}/{basename}.png'
                relative='textures/block/'+source.relative_to(SOURCE).as_posix()
                dest=ASSETS/relative;dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,dest)
                entries[relative]={'source':source.relative_to(ORIGINAL).as_posix(),'sha256':hashlib.sha256(source.read_bytes()).hexdigest()}
                textures[face]='gregtech:'+relative.removeprefix('textures/').removesuffix('.png')
            children[part]=layer(textures,[cube([0,0,0],[16,16,16],part=='colored')])
        path='block/machine/matter/'+name
        write('models/'+path+'.json',{'parent':'minecraft:block/block','loader':'forge:composite','textures':{'particle':children['colored']['textures']['north']},'children':children})
        write('models/item/'+name+'.json',{'parent':'gregtech:'+path})
        write('blockstates/'+name+'.json',{'variants':{'':{'model':'gregtech:'+path}}})
    manifest.write_text(json.dumps(entries,indent=2,sort_keys=True)+'\n',encoding='utf8')
    for i,locale in enumerate(['en_us','zh_cn']):
        path=ASSETS/f'lang/{locale}.json';data=json.loads(path.read_text(encoding='utf8'))
        data.update({'block.gregtech.'+name:values[1+i] for name,values in PARTS.items()})
        data['gregtech.jei.assembly_matter']=[
            '5 x 5 x 6: 97 dense lead walls, 26 osmium coils and one empty center. Top: 16 vents, central versatile processor, 4 control + 4 conversion processors in any order. QU input and item/fluid IO through walls/coils; auto output below controller.',
            '5×5×6：97 致密铅墙、26 锇线圈，中心留空。顶部 16 通风组件，中央 1 通用处理器，周围 4 控制与 4 转换处理器可交换位置。墙体及线圈输入 QU 和物品/流体；主控底面自动输出。'][i]
        path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    path=native_path(ROOT/'src/main/resources/data/minecraft/tags/blocks/mineable/pickaxe.json');data=json.loads(path.read_text(encoding='utf8'))
    for name in PARTS:
        if 'gregtech:'+name not in data['values']:data['values'].append('gregtech:'+name)
    path.write_text(json.dumps(data,indent=2)+'\n',encoding='utf8')
    folder=native_path(ROOT/'src/main/resources/data/gregtech/recipes/matter');folder.mkdir(parents=True,exist_ok=True)
    def craft(name,pattern,key):
        data={'type':'minecraft:crafting_shaped','pattern':pattern,'key':{k:{'item':'gregtech:'+v} for k,v in key.items()},'result':{'item':'gregtech:'+name}}
        (folder/(name+'.json')).write_text(json.dumps(data,indent=2)+'\n',encoding='utf8')
    craft('largemassfab_lead',['FFF','FMF','FFF'],{'F':'compact_force_field_emitter_iv','M':'dense_lead_wall'})
    craft('conversion_processor_unit',['PCP','CMC','PCP'],{'P':'crystal_processor_sapphire','C':'circuit_ultimate','M':'casing_machine_steelgalvanized'})
    # As with the fusion coil recipe, hand-tool wear is a separate outstanding crafting concern.
    craft('large_osmium_coil',['WWW','W W','WWW'],{'W':'wire_04_osmium'})
    states=json.loads((ASSETS/'blockstates/massfab_osmium.json').read_text(encoding='utf8'))
    item=json.loads((ASSETS/'models/item/massfab_osmium.json').read_text(encoding='utf8'))
    for tier,emitter in enumerate(['lv','mv','hv','ev','iv'],1):
        name='massfab_osmium' if tier==1 else 'massfab_osmiridium_t'+str(tier)
        if tier>1:
            write('blockstates/'+name+'.json',states)
            write('models/item/'+name+'.json',item)
        craft(name,['RFS','FMF','RFS'],{'R':'crystal_processor_ruby','S':'crystal_processor_sapphire','F':'compact_force_field_emitter_'+emitter,'M':'casing_machine_osmiridium'})
        for locale in ['en_us','zh_cn']:
            path=ASSETS/f'lang/{locale}.json';data=json.loads(path.read_text(encoding='utf8'))
            data['block.gregtech.'+name]=f'Matter Fabricator (T{tier}, Osmiridium)' if locale=='en_us' else f'物质制造机（T{tier}，锇铱合金）'
            path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    path=native_path(ROOT/'src/main/resources/data/minecraft/tags/blocks/mineable/pickaxe.json');data=json.loads(path.read_text(encoding='utf8'))
    for tier in range(2,6):
        name='gregtech:massfab_osmiridium_t'+str(tier)
        if name not in data['values']:data['values'].append(name)
    path.write_text(json.dumps(data,indent=2)+'\n',encoding='utf8')

if __name__=='__main__':main()
