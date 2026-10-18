"""GT6 dynamite: full/embedded shape and inactive/lit textures, all six faces."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()

import json
import shutil
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
ASSETS = native_path(ROOT / 'src/main/resources/assets/gregtech')
ORIGINAL = ROOT.parent / 'gregtech6-master/gregtech6-master/src/main/resources/assets/gregtech/textures/blocks/machines/tools/dynamite'
def generate():
    def write(path, data):
        path.parent.mkdir(parents=True,exist_ok=True)
        path.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
    for active in (False,True):
        for sunk in (False,True):
            suffix=('_active' if active else '')+('_sunk' if sunk else '')
            children={}
            for layer in ('colored','overlay'):
                folder=layer+('_active' if active else '')
                for face in ('front','back','side'):
                    target=ASSETS/f'textures/block/machines/tools/dynamite/{folder}/{face}.png'
                    target.parent.mkdir(parents=True,exist_ok=True)
                    shutil.copyfile(ORIGINAL/folder/f'{face}.png',target)
                    animation=ORIGINAL/folder/f'{face}.png.mcmeta'
                    if animation.exists(): shutil.copyfile(animation,Path(str(target)+'.mcmeta'))
                faces={side:{'texture':'#'+('front' if side=='north' else 'back' if side=='south' else 'side')} for side in ('north','south','east','west','up','down')}
                if layer=='colored':
                    for face in faces.values(): face['tintindex']=0
                children[layer]={'textures': {f:f'gregtech:block/machines/tools/dynamite/{folder}/{f}' for f in ('front','back','side')},
                    'elements':[{'from':[5,5,14 if sunk else 0],'to':[11,11,16],'faces':faces}], 'render_type':'minecraft:cutout'}
            write(ASSETS/f'models/block/machine/tool/dynamite{suffix}.json', {'parent':'minecraft:block/block','loader':'forge:composite',
                'textures':{'particle':'gregtech:block/machines/tools/dynamite/colored/side'},'children':children})
    states={}
    for facing,x,y in (('north',0,0),('east',0,90),('south',0,180),('west',0,270),('up',270,0),('down',90,0)):
        for active in (False,True):
            for sunk in (False,True):
                suffix=('_active' if active else '')+('_sunk' if sunk else '')
                states[f'armed={str(active).lower()},facing={facing},sunk={str(sunk).lower()}']={'model':'gregtech:block/machine/tool/dynamite'+suffix,'x':x,'y':y}
    for name in ('dynamite','boomstick','strong_dynamite'):
        write(ASSETS/f'blockstates/{name}.json',{'variants':states})
        write(ASSETS/f'models/item/{name}.json',{'parent':'gregtech:block/machine/tool/dynamite'})
if __name__=='__main__': generate()
