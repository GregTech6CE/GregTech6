"""GT6 mechanical/key-safe assets and standard Chinese; original PNGs are LGPL-3.0-or-later."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()

import json
import re
import hashlib
from pathlib import Path
from generate_reinforced_chest_assets import specs, ROOT, ASSETS, ORIGINAL


def generate(assets=ASSETS):
    def write(relative, data):
        p=assets/relative
        p.parent.mkdir(parents=True,exist_ok=True)
        p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    lang={locale:json.loads((ASSETS/f'lang/{locale}.json').read_text(encoding='utf-8')) for locale in ('en_us','zh_cn')}
    chinese=dict(re.findall(r'^\s*S:gt.multitileentity\.(\d+)=(.*)$',(ROOT.parent/'GregTech.lang').read_text(encoding='utf-8'),re.M))
    textures={}
    for kind,phase in [('mechanical',''),('keylocked',''),('keylocked','_open')]:
        parts={}
        for layer in ('colored','overlay'):
            entries={}
            for face in ('front','back','side'):
                relative=f'textures/block/machines/safes/{kind}/{layer}{phase}/{face}.png'
                source=str(native_path(f'src/main/resources/assets/gregtech/textures/blocks/machines/safes/{kind}/{layer}{phase}/{face}.png'))
                p=assets/relative
                p.parent.mkdir(parents=True,exist_ok=True)
                p.write_bytes((ORIGINAL/source).read_bytes())
                textures[relative]={'source':source,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()}
                entries[face]='gregtech:'+relative.removeprefix('textures/').removesuffix('.png')
            faces={}
            for face in ('up','down','east','west','north','south'):
                slot='front' if face=='north' else 'back' if face=='south' else 'side'
                faces[face]={'texture':'#'+slot,'cullface':face}
                if layer=='colored': faces[face]['tintindex']=0
            parts[layer]={'textures':entries,'elements':[{'from':[0,0,0],'to':[16,16,16],'faces':faces}], 'render_type':'minecraft:cutout'}
        write(f'models/block/machine/storage/safe_{kind}{phase}.json',{'loader':'forge:composite','parent':'minecraft:block/block','children':parts,
              'textures':{'particle':entries['side']}})
    for suffix,wood_id in specs():
        for keyed in (False,True):
            name=('key_safe_' if keyed else 'safe_')+suffix
            if not keyed and suffix=='steel': name='safe'
            kind='keylocked' if keyed else 'mechanical'
            variants={}
            for facing,rotation in [('north',0),('east',90),('south',180),('west',270),('up',0),('down',0)]:
                for opened in (False,True):
                    variants[f'facing={facing},open={str(opened).lower()}']={'model':f'gregtech:block/machine/storage/safe_{kind}'+('_open' if keyed and opened else ''),'y':rotation}
                    if facing in ('up','down'): variants[f'facing={facing},open={str(opened).lower()}']['x'] = 270 if facing == 'up' else 90
            write(f'blockstates/{name}.json',{'variants':variants})
            write(f'models/item/{name}.json',{'parent':f'gregtech:block/machine/storage/safe_{kind}'})
            standard=wood_id-500+(3000 if keyed else 2000)
            lang['zh_cn']['block.gregtech.'+name]=chinese[str(standard)].strip()
            lang['en_us']['block.gregtech.'+name]=('Key Locked ' if keyed else 'Mechanical ')+lang['en_us']['block.gregtech.chest_'+suffix].removesuffix(' Chest')+' Safe'
    standard_text = dict(re.findall(r"^\s*S:(gt.lang.(?:owner|key).controlled)=(.*)$", (ROOT.parent/"GregTech.lang").read_text(encoding="utf-8"), re.M))
    for locale,data in lang.items():
        data["gt.tooltip.safe.1"] = "15 slots" if locale == "en_us" else "15 格存储空间"
        for mode in ("key", "owner"):
            key = "gt.lang." + mode + ".controlled"
            data[key] = standard_text[key].strip() if locale == "zh_cn" else ("This Block can only be opened with a Key!" if mode == "key" else "This Block can only be interacted with by its Owner!")
    for locale,data in lang.items(): write(f'lang/{locale}.json',data)
    return textures

if __name__=='__main__':
    textures=generate()
    manifest=ROOT/'tools/gt6_texture_sources.json'
    data=json.loads(manifest.read_text(encoding='utf-8')); data.update(textures)
    manifest.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print('Generated 120 safes, 18 original PNG layers and standard translations.')
