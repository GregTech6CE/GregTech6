"""GT6 extender/bridge model and crafting definitions. No GTM code or generated bitmaps."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path

import hashlib
import json
import shutil
from pathlib import Path
from rebuild_container_tool_models import ASSETS, write

ROOT = Path(__file__).resolve().parents[1]
SPECS = [('inventory_extender', 'inv', [' hY', ' M ', 'Yw ']),
         ('tank_extender', 'tank', ['Xh ', ' M ', ' wX']),
         ('inventory_tank_extender', 'inv_tank', ['XhY', ' M ', 'YwX']),
         ('inventory_bridge', 'bridge_inv', ['hY ', ' M ', ' Yw']),
         ('tank_bridge', 'bridge_tank', ['h  ', 'XMX', '  w']),
         ('inventory_tank_bridge', 'bridge_inv_tank', ['hY ', 'XMX', ' Yw']),
         ('universal_extender', 'universal', ['XRY', 'CMG', 'YSX']),
         ('universal_bridge', 'bridge_universal', ['SYR', 'XMX', 'CYG'])]
DIRECTIONS = ('down', 'up', 'north', 'south', 'west', 'east')


def model(texture, front=None, secondary=None):
    children = {}
    for layer in ['colored', 'overlay']:
        faces = {}
        for side in DIRECTIONS:
            key = 'in' if side == front else 'out' if side == secondary else 'side'
            faces[side] = {'texture': '#' + key, 'cullface': side}
            if layer == 'colored':
                faces[side]['tintindex'] = 0
        keys = {face['texture'][1:] for face in faces.values()}
        children[layer] = {'textures': {k: f'gregtech:block/machines/extenders/{texture}/{layer}/{k}' for k in sorted(keys)},
                           'elements': [{'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': faces}], 'render_type': 'minecraft:cutout'}
    return {'parent': 'minecraft:block/block', 'loader': 'forge:composite', 'render_type': 'minecraft:cutout',
            'textures': {'particle': f'gregtech:block/machines/extenders/{texture}/colored/side'}, 'children': children}


def main():
    for name, texture, pattern in SPECS:
        bridge = texture.startswith('bridge_')
        variants = {}
        if bridge:
            write(f'models/block/logistics/{name}.json', model(texture))
            variants[''] = {'model': f'gregtech:block/logistics/{name}'}
            inventory = f'gregtech:block/logistics/{name}'
        else:
            for front in DIRECTIONS:
                for secondary in DIRECTIONS:
                    target = f'block/logistics/{name}/{front}_{secondary}'
                    write(f'models/{target}.json', model(texture, front, secondary))
                    variants[f'facing={front},secondary={secondary}'] = {'model': 'gregtech:' + target}
            inventory = f'gregtech:block/logistics/{name}/north_south'
        write(f'blockstates/{name}.json', {'variants': variants})
        write(f'models/item/{name}.json', {'parent': inventory})
        keys = {'M': 'casing_machine_double_steel' if 'inventory_tank' in name else 'casing_machine_steel',
                'X': 'pipe_medium_steel', 'Y': 'item_pipe_medium_electrum', 'h': 'tool_hammer', 'w': 'tool_wrench'}
        if name.startswith('universal_'):
            keys = {'M': 'casing_machine_quadruple_stainlesssteel', 'X': 'pipe_medium_stainless_steel',
                    'Y': 'item_pipe_medium_platinum', 'S': 'spring_stainlesssteel',
                    'G': 'gear_gt_stainlesssteel', 'R': 'rotor_stainlesssteel', 'C': 'circuit_advanced'}
        used = set(''.join(pattern)) - {' '}
        recipe = {'type': 'gregtech:tool_shaped', 'pattern': pattern,
                  'key': {k: {'item': 'gregtech:' + keys[k]} for k in sorted(used)}, 'result': {'item': 'gregtech:' + name}}
        path = native_path(ROOT / f'src/main/resources/data/gregtech/recipes/logistics/{name}.json')
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(recipe, indent=2) + '\n', encoding='utf-8')


def import_textures(original):
    ledger_path = ROOT / 'tools/gt6_texture_sources.json'
    ledger = json.loads(ledger_path.read_text(encoding='utf-8'))
    for _, texture, _ in SPECS:
        folder = native_path(original / f'src/main/resources/assets/gregtech/textures/blocks/machines/extenders/{texture}')
        for source in folder.rglob('*.png'):
            relative = f'textures/block/machines/extenders/{texture}/' + source.relative_to(folder).as_posix()
            destination = ASSETS / relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, destination)
            ledger[relative] = {'source': source.relative_to(original).as_posix(), 'sha256': hashlib.sha256(source.read_bytes()).hexdigest()}
    ledger_path.write_text(json.dumps(ledger, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    import sys
    if len(sys.argv) > 1:
        import_textures(Path(sys.argv[1]))
    main()
