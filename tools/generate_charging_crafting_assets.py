"""GT6 charging crafting table assets. Original textures: GregTech-6 Team, LGPL-3.0-or-later."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()

import json, re, hashlib
from generate_reinforced_chest_assets import specs, ROOT, ASSETS, ORIGINAL


def generate(assets=ASSETS):
    def write(relative,data):
        p=assets/relative; p.parent.mkdir(parents=True,exist_ok=True)
        p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    textures={}
    for layer in ('colored','overlay'):
        for part in ('bottom','top','front','back','side'):
            relative=f'textures/block/machines/craftingtables/charging/{layer}/{part}.png'
            source=str(native_path(f'src/main/resources/assets/gregtech/textures/blocks/machines/craftingtables/charging/{layer}/{part}.png'))
            p=assets/relative; p.parent.mkdir(parents=True,exist_ok=True); p.write_bytes((ORIGINAL/source).read_bytes())
            textures[relative]={'source':source,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()}
    opposite={'down':'up','up':'down','north':'south','south':'north','west':'east','east':'west'}
    # GT6 top/bottom follow world sides; front/back follow the horizontal facing.
    for facing in ('north','south','west','east'):
        children={}
        for layer in ('colored','overlay'):
            faces={}
            for face in opposite:
                part='front' if face==facing else 'back' if face==opposite[facing] else {'up':'top','down':'bottom'}.get(face,'side')
                faces[face]={'texture':f'#'+part,'cullface':face}
                if layer=='colored': faces[face]['tintindex']=0
            children[layer]={'render_type':'minecraft:cutout','textures':{p:f'gregtech:block/machines/craftingtables/charging/{layer}/{p}' for p in ('bottom','top','front','back','side')},
                'elements':[{'from':[0,0,0],'to':[16,16,16],'faces':faces}]}
        write(f'models/block/machine/storage/charging_crafting_table_{facing}.json',{'loader':'forge:composite','parent':'minecraft:block/block','children':children,
            'textures':{'particle':'gregtech:block/machines/craftingtables/charging/colored/side'}})
    lang={locale:json.loads((ASSETS/f'lang/{locale}.json').read_text(encoding='utf-8')) for locale in ('en_us','zh_cn')}
    chinese=dict(re.findall(r'^\s*S:gt.multitileentity\.(\d+)=(.*)$',(ROOT.parent/'GregTech.lang').read_text(encoding='utf-8'),re.M))
    for suffix,wood_id in specs():
        name='charging_crafting_table' if suffix=='steel' else 'charging_crafting_table_'+suffix
        write(f'blockstates/{name}.json',{'variants':{'facing='+face:{'model':'gregtech:block/machine/storage/charging_crafting_table_'+face} for face in ('north','south','west','east')}})
        write(f'models/item/{name}.json',{'parent':'gregtech:block/machine/storage/charging_crafting_table_north'})
        lang['zh_cn']['block.gregtech.'+name]=chinese[str(wood_id-500+5500)].strip()
        lang['en_us']['block.gregtech.'+name]=lang['en_us']['block.gregtech.chest_'+suffix].removesuffix(' Chest')+' Charging Crafting Table'
    tooltips={
        'gt.tooltip.charging_crafting.packets':('Charges five tool slots directly, at most one packet per slot per input call','直接为五个工具槽充能，每次输入每槽最多接收一个能量包'),
        'gt.tooltip.advanced_crafting.access':('Top: crafting; front/back: 36-slot storage','顶部：合成界面；正面/背面：36 格存储'),
        'gt.tooltip.advanced_crafting.tools':('Monkey wrench: automation; screwdriver: diversity filter','活动扳手：自动化开关；螺丝刀：物品分散过滤'),
        'gt.tooltip.advanced_crafting.blueprint':('Shift-left-click an empty blueprint to record the grid','Shift 左击空蓝图，记录当前合成格'),
        'gt.tooltip.advanced_crafting.flush':('Allow automation to extract the crafting grid','允许自动化取出合成格中的物品'),
        'gt.tooltip.advanced_crafting.store':('Return crafting materials to storage','将合成格中的物品存回存储区'),
        'gt.tooltip.advanced_crafting.craft':('Left: once; right: stack; Shift: inventory; Shift-right: fill inventory','左击合成一次；右击合成一组；Shift 放入背包；Shift 右击填满背包'),
        'message.gregtech.crafting.filter':('%s diversity filter: %s','%s 分散过滤：%s'),
        'message.gregtech.crafting.access':('%s automation access: %s','%s 自动化访问：%s'),
    }
    for locale,data in lang.items():
        for key,values in tooltips.items():data[key]=values[0 if locale=='en_us' else 1]
        write(f'lang/{locale}.json',data)
    relative='textures/gui/machines/advanced_crafting_table_charging.png'
    source=str(native_path('src/main/resources/assets/gregtech/textures/gui/machines/AdvancedCraftingTableCharging.png'))
    target=assets/relative;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes((ORIGINAL/source).read_bytes())
    textures[relative]={'source':source,'sha256':hashlib.sha256(target.read_bytes()).hexdigest()}
    return textures

if __name__=='__main__':
    textures=generate(); p=ROOT/'tools/gt6_texture_sources.json'; data=json.loads(p.read_text(encoding='utf-8'));data.update(textures)
    p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print('Generated 60 charging crafting tables, original models, 10 block textures and original GUI.')
