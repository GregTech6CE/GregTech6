"""GT6 vessel and tool meshes in sixteenths of a block; no bitmap generation.

Bounds follow the corresponding MultiTileEntity classes. Dynamic contents,
anvil workpieces and coin inserts are intentionally separate from the empty mesh.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SHARED_ASSETS = ROOT / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"
ASSETS = ROOT / 'src/main/resources/assets/gregtech'
DIRECTIONS = ('west', 'east', 'north', 'south', 'up', 'down')


def box(low, high, side='sides', **faces):
    textures = dict.fromkeys(DIRECTIONS, side)
    textures.update(faces)
    return {'from': low, 'to': high, 'faces': {f: {'texture': '#' + t} for f, t in textures.items() if t}}


def bowl(x0, x1, bottom, top, thickness=1):
    t = thickness
    return [
        box([x0,bottom,x0], [x0+t,top,x1], east='insides', up='top', down='bottom'),
        box([x0,bottom,x0], [x1,top,x0+t], south='insides', up='top', down='bottom'),
        box([x1-t,bottom,x0], [x1,top,x1], west='insides', up='top', down='bottom'),
        box([x0,bottom,x1-t], [x1,top,x1], north='insides', up='top', down='bottom'),
        box([x0,bottom,x0], [x1,bottom+t,x1], up='top', down='bottom'),
    ]


def bowl_table():
    # GT6's three Table subclasses raise the ordinary vessel by 8 px and add
    # one full-width plinth underneath (render pass 7 in each original class).
    return bowl(0, 16, 8, 16, 2) + [
        box([0, 0, 0], [16, 8, 16], 'tableside', up=None, down='tablebottom')]


VESSELS = {
    'jug': ('tanks/jug', [
        box([5,10,6], [6,14,10], east='insides', up='top', down='bottom'),
        box([6,10,5], [10,14,6], south='insides', up='top', down='bottom'),
        box([10,10,6], [11,14,10], west='insides', up='top', down='bottom'),
        box([6,10,10], [10,14,11], north='insides', up='top', down='bottom'),
        box([3,0,3], [13,10,13], up='top', down='bottom')]),
    'cup': ('tanks/cup', [
        box([5,1,6], [6,5,10], east='insides', up='top', down='bottom'),
        box([6,1,5], [10,5,6], south='insides', up='top', down='bottom'),
        box([10,1,6], [11,5,10], west='insides', up='top', down='bottom'),
        box([6,1,10], [10,5,11], north='insides', up='top', down='bottom'),
        box([6,0,6], [10,1,10], up='top', down='bottom')]),
    'measuring_pot': ('tanks/measuring_pot', [
        box([4,1,5], [5,8,11], east='insides', up='top', down='bottom'),
        box([5,1,4], [11,8,5], south='insides', up='top', down='bottom'),
        box([11,1,5], [12,8,11], west='insides', up='top', down='bottom'),
        box([5,1,11], [11,8,12], north='insides', up='top', down='bottom'),
        box([5,0,5], [11,1,11], up='top', down='bottom')]),
    'thermos': ('tanks/thermos', [box([4,0,4], [12,16,12], 'side', up='top', down='bottom')]),
    'barometer_gas_cylinder': ('tanks/barometer_gas_cylinder', [
        box([4,0,5], [12,8,11], up='top', down='bottom'),
        box([5,0,4], [11,8,12], up='top', down='bottom'),
        box([5,8,5], [11,9,11], up='top', down='bottom'),
        box([7,9,7], [9,16,9], up='top', down='bottom'),
        box([6,10,6], [10,14,10], 'barometer')]),
}

TOOLS = {
    'anvil': ('@gregtech:block/material_icons/metallic/blocksolid', [
        box([2,0,4],[14,4,12]), box([4,4,6],[12,8,10]), box([1,8,4],[15,12,12]),
        box([15,8,5],[16,12,11]), box([0,9,4],[1,11,12])]),
    'scaffold': ('@gregtech:block/iconsets/plate', [
        box([0,14,0],[16,16,16]), box([2,12,0],[4,14,16]),
        box([6,10,0],[10,14,16]), box([12,12,0],[14,14,16])]),
    'advanced_button': ('@gregtech:block/machines/redstone/buttons/advanced/0/0/colored_off',
                        [box([4,4,14],[12,12,16])]),
    'mixing_bowl': ('tools/mixing_bowl', bowl(0,16,0,8,2)),
    'mixing_bowl_table': ('tools/mixing_bowl', bowl_table()),
    'bathing_pot': ('tools/bathing_pot', bowl(0,16,0,8,2)),
    'bathing_pot_table': ('tools/bathing_pot', bowl_table()),
    'bathing_pot_wood': ('tools/bathing_pot_wood', bowl(0,16,0,8,2)),
    'bathing_pot_table_wood': ('tools/bathing_pot_wood', bowl_table()),
    'juicer': ('tools/juicer', bowl(2,14,0,4,2)[:-1] + [
        box([2,0,2], [14,1,14], up='top', down='bottom'),
        box([6,0,6], [10,7,10], 'middleside', up='middletop', down='bottom')]),
    'plant_pot': ('plantpot', [box([0,10,0],[16,16,16],'side',up='top',down='bottom'),
                             box([1,0,1],[15,10,15],'side',up='top',down='bottom')]),
    'bumbliary': ('tools/bumbliary', [box([0,0,0],[16,16,16],up='top',down='bottom')]),
    'sap_bag': ('tools/sapbag', [box([5,0,0],[11,7,6],'side',up='top',down='bottom')]),
    'fluid_funnel': ('tools/funnel', [box([5,9,0],[11,10,6],'side',up='top',down='bottom'),
                                    box([6,8,0],[10,9,4],'side',up='top',down='bottom'),
                                    box([7,7,0],[9,8,2],'side',up='top',down='bottom')]),
    'tap': ('tools/tap', [box([6,6,2],[10,7,4],'side',up='top',down='bottom'),
                         box([7,4,0],[9,6,4],'side',up='top',down='bottom'),
                         box([7,3,4],[9,6,6],'side',up='top',down='bottom')]),
    'cap_nozzle': ('tools/capnozzle', [box([6,3,1],[10,7,6],'side',up='top',down='bottom'),
                                     box([7,4,0],[9,6,2],'side',up='top',down='bottom')]),
    'coin_mold': ('tools/mold_coinage', [box([0,0,0],[16,12,16],'side',up='top',down='bottom'),
                                       box([4,12,4],[12,14,12],'side',up='hole',down='bottom'),
                                       box([11,12,5],[11,14,11],None,west='side'),
                                       box([5,12,11],[11,14,11],None,north='side'),
                                       box([5,12,5],[5,14,11],None,east='side'),
                                       box([5,12,5],[11,14,5],None,south='side')]),
}


def write(path, data):
    relative = Path(path)
    root = SHARED_ASSETS if relative.parts and relative.parts[0] in ('models', 'blockstates') else ASSETS
    path = root / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(data, indent=2) + '\n'
    if not path.exists() or path.read_text(encoding='utf-8') != text:
        path.write_text(text, encoding='utf-8')


def layered(path, texture, parts):
    children = {}
    for layer in ('colored', 'overlay'):
        elements = json.loads(json.dumps(parts))
        keys = {face['texture'][1:] for part in elements for face in part['faces'].values()}
        textures = {key: f'gregtech:block/machines/{texture}/{layer}/{key}' for key in sorted(keys)}
        if layer == 'colored':
            for part in elements:
                for face in part['faces'].values(): face['tintindex'] = 0
        children[layer] = {'textures': textures, 'elements': elements, 'render_type': 'minecraft:cutout'}
    write(path, {'parent': 'minecraft:block/block', 'loader': 'forge:composite', 'render_type': 'minecraft:cutout',
                 'textures': {'particle': next(iter(children['colored']['textures'].values()))}, 'children': children})


def main():
    for name, (texture, parts) in VESSELS.items():
        layered(f'models/block/machine/container/{name}.json', texture, parts)
        write(f'models/item/fluid_{name}.json', {'parent': f'gregtech:block/machine/container/{name}'})
    for name, (texture, parts) in TOOLS.items():
        # Specialized stateful models must not be replaced by the legacy four-face mesh.
        if name == 'advanced_button':
            from generate_advanced_button_assets import main as generate_button
            generate_button()
            continue
        if name == 'scaffold':
            from generate_scaffold_models import main as generate_scaffold
            generate_scaffold()
            continue
        if texture.startswith('@'):
            elements = json.loads(json.dumps(parts))
            for part in elements:
                for face in part['faces'].values(): face['tintindex'] = 0
            write(f'models/block/tool/{name}.json', {'parent':'minecraft:block/block',
                  'textures':{'particle':texture[1:],'sides':texture[1:]}, 'elements':elements})
        else:
            layered(f'models/block/tool/{name}.json', texture, parts)
        variants = {
            f'facing={face}': {'model': f'gregtech:block/tool/{name}', 'y': turn}
            for face, turn in [('north',0),('east',90),('south',180),('west',270)]}
        if name in {'tap', 'fluid_funnel', 'cap_nozzle'}:
            variants['facing=down'] = {'model': 'gregtech:block/tool/' + ('fluid_funnel_down' if name == 'fluid_funnel' else name)}
        write(f'blockstates/{name}.json', {'variants': variants})
    layered('models/block/tool/fluid_funnel_down.json', 'tools/funnel', [
        box([5,2,5],[11,3,11],'side',up='top',down='bottom'),
        box([6,1,6],[10,2,10],'side',up='top',down='bottom'),
        box([7,0,7],[9,1,9],'side',up='top',down='bottom')])
    source = ['package com.gregtech.gregtech.content.tool;', '',
              '/** Generated by tools/rebuild_container_tool_models.py; same bounds as the visible mesh. */',
              'public final class OriginalToolShapes {', '    private OriginalToolShapes() {}',
              '    public static int[][] bounds(String id) {', '        return switch (id) {']
    for name, (_, parts) in TOOLS.items():
        values = ', '.join('{' + ','.join(map(str, p['from']+p['to'])) + '}' for p in parts)
        source.append(f'            case "{name}" -> new int[][] {{{values}}};')
    source += ['            default -> throw new IllegalArgumentException("Unknown tool mesh: " + id);',
               '        };', '    }', '}']
    java = ROOT/'src/main/java/com/gregtech/gregtech/content/tool/OriginalToolShapes.java'
    java.parent.mkdir(parents=True, exist_ok=True)
    text = '\n'.join(source)+'\n'
    if not java.exists() or java.read_text(encoding='utf-8') != text: java.write_text(text, encoding='utf-8')
    # Fuel rods are rendered from the original single rod mesh, not invented flat sprites.
    layered('models/block/machine/container/reactor_rod.json', 'generators/reactor_rods',
            [box([6,0,6],[10,16,10],up='top',down='top')])
    for path in (SHARED_ASSETS/'models/item').glob('fuel_rod_*.json'):
        write(path.relative_to(SHARED_ASSETS), {'parent': 'gregtech:block/machine/container/reactor_rod'})
    for name, voltage, inset, height in [('lv',32,5,11),('mv',128,4,11),('hv',512,3,11),('ev',2048,2,13),('iv',2048,2,13)]:
        # The old port's extra IV item retains the largest GT6 standard battery shell.
        parts = [box([inset,0,inset],[16-inset,height,16-inset],up='top',down='bottom')]
        textures = {f: f'gregtech:block/machines/batteries/eu/standard/{voltage}/{f}' for f in ('top','bottom','sides')}
        write(f'models/item/battery_{name}.json', {'parent':'minecraft:block/block','textures':textures,'elements':parts})
    tools = {
        'electric_drill': ('iconsets/handle_electric_drill', 'iconsets/tip_electric_drill'),
        'electric_screwdriver': ('iconsets/handle_electric_screwdriver', 'material_icons/metallic/toolheadscrewdriver'),
        'electric_chainsaw': ('iconsets/power_unit_lv', 'material_icons/metallic/toolheadchainsaw'),
        'electric_wrench': ('iconsets/power_unit_lv', 'material_icons/metallic/toolheadwrench'),
    }
    for name, (body, tip) in tools.items():
        write(f'models/item/{name}.json', {'parent':'minecraft:item/handheld', 'textures': {
            f'layer{i}': 'gregtech:item/' + texture for i,texture in enumerate([body,body+'_overlay',tip,tip+'_overlay'])}})
    for path in (SHARED_ASSETS/'models/block/magnet').glob('magnet_*.json'):
        if 'electromagnetic' in path.stem:
            layered(path.relative_to(SHARED_ASSETS), 'magnets/magnet_electric',
                    [box([0,0,0],[16,16,16],'side',north='front',south='back')])
        else:
            parts = [box([0,0,0],[16,16,16])]
            for face in parts[0]['faces'].values(): face['tintindex'] = 0
            write(path.relative_to(SHARED_ASSETS), {'parent':'minecraft:block/block',
                  'textures':{'sides':'gregtech:block/material_icons/magnetic/blocksolid'}, 'elements':parts})
    print(f'Rebuilt {len(VESSELS)} portable vessels, {len(TOOLS)} tool meshes, fuel rods, batteries, electric tools and magnets')


if __name__ == '__main__': main()
