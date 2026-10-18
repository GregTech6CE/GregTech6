"""Original GT6 fusion controller and structural part assets, with material tint layers."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path

from pathlib import Path
import json, shutil, hashlib, gzip, struct
from tools.restore_laser_models import ROOT, ORIGINAL, ASSETS, SOURCE, write, cube, layer

PARTS = {
    'galvanized_steel_wall': ('metalwall', 'Galvanized Steel Wall', '镀锌钢墙'),
    'large_iridium_coil': ('coil', 'Large Iridium Coil', '大型铱线圈'),
    'fusion_ventilation_unit': ('ventilationunit', 'Ventilation Unit', '通风组件'),
    'versatile_processor_unit': ('processorversatile', 'Versatile Quadcore Processor Unit', '通用四核处理器组件'),
    'logic_processor_unit': ('processorlogic', 'Logic Quadcore Processor Unit', '逻辑四核处理器组件'),
    'control_processor_unit': ('processorcontrol', 'Control Quadcore Processor Unit', '控制四核处理器组件'),
    # GT6 gives all three the fusion `NBT_TEXTURE` folder of their kind (metalwalldense /
    # processorstorage), and their blockstates already point at block/machine/fusion/<name>.
    'dense_steel_wall': ('metalwalldense', 'Dense Steel Wall', '致密钢墙'),
    'dense_galvanized_steel_wall': ('metalwalldense', 'Dense Galvanized Steel Wall', '致密镀锌钢墙'),
    'storage_quadcore_processor_unit': ('processorstorage', 'Storage Quadcore Processor Unit', '存储四核处理器组件'),
}

def main():
    manifest=ROOT/'tools/gt6_texture_sources.json'
    entries=json.loads(manifest.read_text(encoding='utf8'))
    def copy(source):
        relative='textures/block/'+source.relative_to(SOURCE).as_posix().lower()
        dest=ASSETS/relative;dest.parent.mkdir(parents=True,exist_ok=True)
        shutil.copyfile(source,dest)
        entries[relative]={'source':source.relative_to(ORIGINAL).as_posix(),'sha256':hashlib.sha256(source.read_bytes()).hexdigest()}
        if source.with_suffix('.png.mcmeta').exists():shutil.copyfile(source.with_suffix('.png.mcmeta'),dest.with_suffix('.png.mcmeta'))
        return 'gt6ce:'+relative.removeprefix('textures/').removesuffix('.png')
    def model(folder,overlay,controller=False):
        children={}
        for name,part in [('base','colored'),('overlay',overlay)]:
            textures={}
            for face in ['up','down','north','south','east','west']:
                file={'up':'top','down':'bottom','north':'front','south':'back','east':'left','west':'right'}[face] if controller else 'top' if face=='up' else 'bottom' if face=='down' else 'side'
                textures[face]=copy(folder/part/(file+'.png'))
            children[name]=layer(textures,[cube([0,0,0],[16,16,16],name=='base')])
        return {'parent':'minecraft:block/block','loader':'forge:composite','textures':{'particle':children['base']['textures']['north']},'children':children}
    for name,(folder,en,zh) in PARTS.items():
        path='block/machine/fusion/'+name
        write('models/'+path+'.json',model(SOURCE/'machines/multiblockparts'/folder/'0','overlay'))
        write('models/item/'+name+'.json',{'parent':'gt6ce:'+path})
        write('blockstates/'+name+'.json',{'variants':{'':{'model':'gt6ce:'+path}}})
    variants={}
    for lit in [False,True]:
        for running in [False,True]:
            overlay='overlay_active' if lit else 'overlay_running' if running else 'overlay'
            path=f'block/machine/fusion/controller_{int(lit)}_{int(running)}'
            write('models/'+path+'.json',model(SOURCE/'machines/basicmachines/fusionreactor',overlay,True))
            for direction,rotation in [('north',0),('east',90),('south',180),('west',270)]:
                variants[f'facing={direction},lit={str(lit).lower()},running={str(running).lower()}']={'model':'gt6ce:'+path,'y':rotation}
    write('blockstates/fusion_reactor_main.json',{'variants':variants})
    write('models/item/fusion_reactor_main.json',{'parent':'gt6ce:block/machine/fusion/controller_0_0'})
    manifest.write_text(json.dumps(entries,indent=2,sort_keys=True)+'\n',encoding='utf8')
    translations={
        'gt6ce.fusion.startup':('Startup remaining: %s LU','启动还需：%s LU'),
        'gt6ce.fusion.output':('Output: %s EU/t','输出：%s EU/t'),
        'gt6ce.jei.assembly_fusion':('19 x 19 x 5. Core: 3 versatile, 12 logic, 12 control processors in any order. LU: middle ring except four outer cardinal EU outlets. Items/fluids: upper/lower ring. Identical continuous reactions keep startup charge.','19×19×5。核心内放置 3 通用、12 逻辑、12 控制处理器，可交换位置。中层环壁输入 LU，四个最外侧轴线端口输出 EU；上下环壁输入输出物品和流体。连续同配方无需重复启动。'),
    }
    for name,(_,en,zh) in PARTS.items():translations['block.gt6ce.'+name]=(en,zh)
    for i,locale in enumerate(['en_us','zh_cn']):
        path=ASSETS/f'lang/{locale}.json';data=json.loads(path.read_text(encoding='utf8'))
        data.update({k:v[i] for k,v in translations.items()});path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    path=native_path(ROOT/'src/main/resources/data/minecraft/tags/blocks/mineable/pickaxe.json')
    data=json.loads(path.read_text(encoding='utf8'))
    for name in PARTS:
        if 'gt6ce:'+name not in data['values']:data['values'].append('gt6ce:'+name)
    path.write_text(json.dumps(data,indent=2)+'\n',encoding='utf8')
    # Empty GameTest region large enough for the full ring and all four external receivers.
    def string(s):return struct.pack('>H',len(s))+s.encode()
    def named(kind,name,data):return bytes([kind])+string(name)+data
    nbt=named(10,'',named(3,'DataVersion',struct.pack('>i',3465))+named(9,'size',bytes([3])+struct.pack('>iiii',3,25,9,25))+named(9,'entities',bytes([10])+struct.pack('>i',0))+named(9,'blocks',bytes([10])+struct.pack('>i',0))+named(9,'palette',bytes([10])+struct.pack('>i',1)+named(8,'Name',string('minecraft:air'))+b'\x00')+b'\x00')
    path=native_path(ROOT/'src/main/resources/data/gt6ce/structures/test_fusion_empty.nbt');path.write_bytes(gzip.compress(nbt,mtime=0))

if __name__=='__main__':main()
