"""GT6 compartment drawer assets. Original textures: GregTech-6 Team, LGPL-3.0-or-later."""

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
            relative=f'textures/block/machines/drawers/quad/{layer}/{part}.png'
            source=str(native_path(f'src/main/resources/assets/gt6ce/textures/blocks/machines/drawers/quad/{layer}/{part}.png'))
            p=assets/relative; p.parent.mkdir(parents=True,exist_ok=True); p.write_bytes((ORIGINAL/source).read_bytes())
            textures[relative]={'source':source,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()}
    opposite={'down':'up','up':'down','north':'south','south':'north','west':'east','east':'west'}
    # GT6 chooses top/bottom by world side, not by rotating the whole cube when facing vertically.
    for facing in opposite:
        children={}
        for layer in ('colored','overlay'):
            faces={}
            for face in opposite:
                part='front' if face==facing else 'back' if face==opposite[facing] else {'up':'top','down':'bottom'}.get(face,'side')
                faces[face]={'texture':f'#'+part,'cullface':face}
                if layer=='colored': faces[face]['tintindex']=0
            children[layer]={'render_type':'minecraft:cutout','textures':{p:f'gt6ce:block/machines/drawers/quad/{layer}/{p}' for p in ('bottom','top','front','back','side')},
                'elements':[{'from':[0,0,0],'to':[16,16,16],'faces':faces}]}
        write(f'models/block/machine/storage/drawer_quad_{facing}.json',{'loader':'forge:composite','parent':'minecraft:block/block','children':children,
            'textures':{'particle':'gt6ce:block/machines/drawers/quad/colored/side'}})
    lang={locale:json.loads((ASSETS/f'lang/{locale}.json').read_text(encoding='utf-8')) for locale in ('en_us','zh_cn')}
    chinese=dict(re.findall(r'^\s*S:gt.multitileentity\.(\d+)=(.*)$',(ROOT.parent/'GregTech.lang').read_text(encoding='utf-8'),re.M))
    for suffix,wood_id in specs():
        name='drawer_quad' if suffix=='stainless_steel' else 'drawer_quad_'+suffix
        write(f'blockstates/{name}.json',{'variants':{'facing='+face:{'model':'gt6ce:block/machine/storage/drawer_quad_'+face} for face in opposite}})
        write(f'models/item/{name}.json',{'parent':'gt6ce:block/machine/storage/drawer_quad_north'})
        lang['zh_cn']['block.gt6ce.'+name]=chinese[str(wood_id-500+4000)].strip()
        lang['en_us']['block.gt6ce.'+name]=lang['en_us']['block.gt6ce.chest_'+suffix].removesuffix(' Chest')+' Compartment Drawer'
    for locale,data in lang.items():
        data['gt.tooltip.drawer.1']='Four independent pages, 36 slots each' if locale=='en_us' else '四个独立页面，每页 36 格'
        data['gt.tooltip.drawer.2']='Click a front quadrant to open; monkey wrench toggles sided automation' if locale=='en_us' else '点击正面对应分区打开；活动扳手切换分面自动化'
        data['message.gt6ce.drawer.sided']='Automation-Access: Sided' if locale=='en_us' else '自动化访问：按侧面分区'
        data['message.gt6ce.drawer.anywhere']='Automation-Access: Anywhere' if locale=='en_us' else '自动化访问：所有格子'
        data['gui.gt6ce.drawer.page']='%s — Page %s' if locale=='en_us' else '%s — 第 %s 页'
        write(f'lang/{locale}.json',data)
    return textures

if __name__=='__main__':
    textures=generate(); p=ROOT/'tools/gt6_texture_sources.json'; data=json.loads(p.read_text(encoding='utf-8'));data.update(textures)
    p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print('Generated 60 material drawers, six direction models and 10 original textures.')
