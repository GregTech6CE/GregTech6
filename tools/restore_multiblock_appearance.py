"""Restore dedicated GT6 controller/part textures, overlays and block item models.

Copies original assets byte-for-byte. Does not implement machine mechanics.

Two kinds of entries:

* ``MAINS``/``PARTS`` -- controllers and structural walls/coils whose texture folder the port
  already pinned down; their blockstates (with ``facing``/``lit``/``running`` variants) already
  exist and are only re-pointed at the restored models.
* ``MAIN_SUFFIXES``/``MAIN_EXPLICIT`` -- the remaining ``LargeMachineParts`` blocks (GT6 ids
  17001-18108). GT6 picks those textures with ``NBT_TEXTURE`` in
  ``Loader_MultiTileEntities.java`` ("Multiblock Machines" registrations), e.g.
  ``17302 Large Stainless Steel Crucible`` -> ``"crucible"``. The port registers them as plain
  ``MultiblockPortBlock`` instances (no blockstate properties), so a missing blockstate is
  synthesised as the single anonymous variant ``steel_wall`` uses.

Usage::

    python tools/restore_multiblock_appearance.py          # write assets (idempotent)
    python tools/restore_multiblock_appearance.py --check  # verify only, exit 1 when incomplete
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path

from pathlib import Path
import hashlib,json,shutil,sys
ROOT=Path(__file__).resolve().parents[1]
ORIGINAL=ROOT.parent/'gregtech6-master/gregtech6-master'
ASSETS=native_path(ROOT/'src/main/resources/assets/gt6ce')
SOURCE=native_path(ORIGINAL/'src/main/resources/assets/gregtech/textures/blocks/machines')
MAINS={
    'heat_exchanger_main':'multiblockmains/largeheatexchanger',
    'coke_oven_main':'basicmachines/cokeoven',
    'distillation_tower_main':'basicmachines/distillationtower',
    'cryo_distillation_main':'basicmachines/cryodistillationtower',
    'implosion_compressor_main':'basicmachines/implosioncompressor',
    # Fusion has dedicated running states: tools/restore_fusion_assets.py.
}
PARTS={
    'dense_tungsten_wall':'metalwalldense/0',
    'dense_titanium_wall':'metalwalldense/0',
    'tungstensteel_wall':'metalwall/0',
    'titanium_wall':'metalwall/0',
    'invar_wall':'metalwall/0',
    'steel_wall':'metalwall/0',
    'large_nichrome_coil':'coil/0',
    'large_carborundum_coil':'coil/0',
    'sluice_part':'sluiceparts/0',
    'crusher_wheels':'crusherwheels/0',
    'shredder_blades':'shredderblades/0',

    'centrifuge_part':'centrifugeparts/0','electrolyzer_part':'electrolyzerparts/0',
    'heat_transmitter':'heatacceptor/0','distillation_tower_part':'distillationtowerparts/0',
    'tank_wall':'metalwall/0','tank_wall_dense':'metalwalldense/0',
    'implosion_compressor_wall':'metalwalldense/0','fusion_reactor_wall':'coil/0',
}
# GT6 texture folder per part family, taken from `NBT_TEXTURE` in Loader_MultiTileEntities.java.
MAIN_SUFFIXES={
    '_tank_main_valve':'multiblockmains/tankmetal',                # 17002-17067, 3x3x3 / 5x5x5 tanks
    '_boiler_main_barometer':'multiblockmains/largeboiler',        # 17201-17205
    '_steam_turbine_main_housing':'multiblockmains/largeturbine',  # 17211-17214
    '_gas_turbine_main_housing':'multiblockmains/gasturbine',      # 17231-17234
    '_dynamo_main_housing':'multiblockmains/largedynamo',          # 17221-17224
    '_crucible':'multiblockmains/crucible',                        # 17302-17312
    '_wall':'multiblockparts/metalwall/0',                         # 18004-18032 structural walls
    '_coil':'multiblockparts/coil/0',                              # 18040-18045 large coils
}
# Dense walls share the `_wall` suffix but use the denser GT6 design (each "part" texture folder holds
# one design subfolder per GT6 `NBT_DESIGNS` value; the port uses design 0 like the walls above).
DENSE_WALLS={'dense_lead_wall','dense_tungsten_wall','dense_titanium_wall','dense_adamantium_wall',
             'dense_invar_wall','dense_bronze_wall','dense_tantalum_hafnium_carbide_wall'}
MAIN_EXPLICIT={
    'wood_tank_main_valve':'multiblockmains/tankwood',            # 17001, wood 3x3x3 tank
    'wood_wall':'multiblockparts/woodwall/0',                     # 18001, wood wall
    'von_da_graagg_generator':'multiblockmains/vondagraagg',      # 17996
    'lightning_rod':'multiblockmains/lightningrod',               # 18104
    'lightning_rod_electric_output':'multiblockmains/lightningrod',    # 17998
    'bedrock_mining_drill_controller':'multiblockmains/bedrockdrill',  # 17999
    'bedrock_mining_drill_head':'multiblockmains/bedrockdrill',        # 18103
}


def main_folder(name):
    """GT6 texture folder of a LargeMachineParts name, or None when the part has no main texture."""
    if name in DENSE_WALLS:return 'multiblockparts/metalwalldense/0'
    if name in MAIN_EXPLICIT:return MAIN_EXPLICIT[name]
    for suffix,folder in MAIN_SUFFIXES.items():
        if name.endswith(suffix):return folder
    return None


def owned_directory(name):
    """Directory the part's existing blockstate already points into, or None when it has none.

    Parts hosted in `block/machine/matter` (coils and processor units of the matter fabricator) or
    `block/machine/fusion` (walls, coils and processor units of the fusion reactor) belong to those
    generators; only a part that points at - or still lacks - `block/machine/multiblock/<name>` is
    this tool's business.
    """
    path=ASSETS/f'blockstates/{name}.json'
    if not path.is_file():return None
    refs={variant['model'] for variant in json.loads(path.read_text(encoding='utf-8'))['variants'].values()}
    folders={ref.rsplit('/',1)[0] for ref in refs if ref.startswith('gt6ce:')}
    return folders.pop().rsplit('/',1)[-1] if len(folders)==1 else None


# The port's part table is the source of truth: every `new Part(id,"name","material")` is a block.
import re as _re
PARTS_ROOT=ROOT/'core/src/main/java/com/gregtech/gregtech/content/multiblock'
PART_NAMES=[m.group(1) for file in ('SharedLargeMachineParts.java','OriginalMultiblockPartData.java')
            for m in _re.finditer(r'new Part\(\d+,\s*"([a-z0-9_]+)"',
                                 (PARTS_ROOT/file).read_text(encoding='utf-8'))]
# Parts whose blockstate still points at a stand-in model (`wood_wall` used the barrel model) but that
# GT6 textures as a structural wall; listing them here lets this tool take them over and re-point the
# blockstate at `block/machine/multiblock/<name>`.
FORCE_INCLUDE={'wood_wall'}
GENERATED={name:main_folder(name) for name in PART_NAMES
           if main_folder(name)
           and (owned_directory(name) in (None,'multiblock') or name in FORCE_INCLUDE)}
# Names whose blockstate already declares blockstate properties; those files must exist.
LEGACY=set(MAINS)|set(PARTS)
MAPPING=MAINS|{name:'multiblockparts/'+path for name,path in PARTS.items()}|GENERATED

def write(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def model(folder,state='',front='side'):
    children={}
    for layer in ['colored','overlay']:
        faces={};textures={}
        for face,part in [('up','top'),('down','bottom'),('north','front'),('south','back'),('west','left'),('east','right')]:
            stem=layer+state if (SOURCE/folder/(layer+state)).is_dir() else layer
            if part=='front' and (SOURCE/folder/(layer+'_front')).is_dir():
                relative=f'{folder}/{layer}_front/{front}.png'
            else:
                candidates=[f'{folder}/{stem}/{name}.png' for name in [part,'side','sides']]
                relative=next((name for name in candidates if (SOURCE/name).is_file()),None)
            if relative is None or not (SOURCE/relative).is_file(): continue
            textures[face]='gt6ce:block/machines/'+relative[:-4]
            faces[face]={'texture':'#'+face,'cullface':face}
            if layer=='colored':faces[face]['tintindex']=0
        if faces:
            children[layer]={'parent':'minecraft:block/block','render_type':'minecraft:cutout',
                    'textures':textures,'elements':[{'from':[0,0,0],'to':[16,16,16],'faces':faces}]}
    if 'colored' not in children or len(children['colored']['elements'][0]['faces'])!=6:
        raise ValueError(f'Incomplete original base: {folder}')
    return {'parent':'minecraft:block/block','loader':'forge:composite',
            'textures':{'particle':next(iter(children['colored']['textures'].values()))},'children':children}

def restore():
    manifest_path=ROOT/'tools/gt6_texture_sources.json'
    manifest=json.loads(manifest_path.read_text(encoding='utf-8'))
    copied=0;synthesised=0
    for name,folder in MAPPING.items():
        if not (SOURCE/folder).is_dir():raise FileNotFoundError(SOURCE/folder)
        for source in (SOURCE/folder).rglob('*'):
            if not source.is_file() or not (source.name.endswith('.png') or source.name.endswith('.png.mcmeta')):continue
            target=ASSETS/'textures/block/machines'/source.relative_to(SOURCE)
            target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,target);copied+=1
            manifest[target.relative_to(ASSETS).as_posix()]={'source':source.relative_to(ORIGINAL).as_posix(),'sha256':hashlib.sha256(source.read_bytes()).hexdigest()}
        ref='gt6ce:block/machine/multiblock/'+name
        base_model=model(folder)
        if name in ('heat_exchanger_main',):
            for child in base_model['children'].values():
                for element in child.get('elements',[]):
                    faces=element['faces'];side='down'
                    faces[side],faces['north']=faces['north'],faces[side]
                    faces[side]['cullface']=side;faces['north']['cullface']='north'
        write(ASSETS/f'models/block/machine/multiblock/{name}.json',base_model)
        path=ASSETS/f'blockstates/{name}.json'
        if path.is_file():
            states=json.loads(path.read_text(encoding='utf-8'))
        elif name in LEGACY:
            # A block with declared properties must keep its hand-written variant list.
            raise FileNotFoundError(path)
        else:
            states={'variants':{'':{'model':ref}}};synthesised+=1
        for key,variant in states['variants'].items():
            suffix=''
            if 'lit=true' in key and (SOURCE/folder/'overlay_active').is_dir():suffix='_active'
            elif 'running=true' in key and (SOURCE/folder/'overlay_running').is_dir():suffix='_running'
            vertical='up' if 'facing=up' in key else 'down' if 'facing=down' in key else None
            output_suffix=suffix+('_'+vertical if vertical else '')
            write(ASSETS/f'models/block/machine/multiblock/{name}{output_suffix}.json',base_model if name in ('heat_exchanger_main',) else model(folder,suffix,'top' if vertical=='up' else 'bottom' if vertical=='down' else 'side'))
            variant['model']=ref+output_suffix
        write(path,states)
        write(ASSETS/f'models/item/{name}.json',{'parent':ref})
    write(manifest_path,manifest)
    print(f'Restored {len(MAPPING)} block definitions ({len(GENERATED)} from LargeMachineParts); '
          f'{synthesised} blockstates synthesised, {copied} texture/metadata files copied')


def model_refs(name):
    """Every model this part's blockstate and item model point at."""
    states=json.loads((ASSETS/f'blockstates/{name}.json').read_text(encoding='utf-8'))
    refs=[variant['model'] for variant in states['variants'].values()]
    item=json.loads((ASSETS/f'models/item/{name}.json').read_text(encoding='utf-8'))
    if 'parent' in item:refs.append(item['parent'])
    return refs


def check():
    """Report parts that are still missing a blockstate, a model or an item model."""
    missing=[]
    for name in sorted(MAPPING):
        blockstate=ASSETS/f'blockstates/{name}.json'
        item=ASSETS/f'models/item/{name}.json'
        if not blockstate.is_file():missing.append(f'{name}: no blockstate');continue
        if not item.is_file():missing.append(f'{name}: no item model');continue
        for ref in model_refs(name):
            if not ref.startswith('gt6ce:'):
                raise ValueError(f'{name}: unexpected namespace in {ref}')
            if not (ASSETS/f'models/{ref.split(":",1)[1]}.json').is_file():
                missing.append(f'{name}: {ref} missing')
    print(f'checked {len(MAPPING)} part definitions')
    for problem in missing:print('  MISSING',problem)
    print('RESULT:','OK' if not missing else f'{len(missing)} problems')
    return 1 if missing else 0


if __name__=='__main__':
    if '--check' in sys.argv:sys.exit(check())
    restore()
