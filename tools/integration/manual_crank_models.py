"""GT6 MultiTileEntityCrank:125-144: frontspin and opposite axle rotations."""
from pathlib import Path
import json


def crank_resources():
    resources = {}
    variants = {}
    for active in (False, True):
        name = 'crank_active' if active else 'crank'
        front = 'frontspin' if active else 'front'
        children = {}
        for layer in ('colored', 'overlay'):
            textures = {'front': f'gt6ce:block/machines/tools/crank/{layer}/{front}'}
            if layer == 'colored':
                textures.update(side='gt6ce:block/iconsets/axle_clockwise' if active else 'gt6ce:block/iconsets/axle',
                                back='gt6ce:block/iconsets/axle_counterclockwise' if active else 'gt6ce:block/iconsets/axle')
            elements = []
            for low, high in (([2, 2, 13], [14, 14, 15]), ([5, 5, 15], [11, 11, 16])):
                faces = {}
                for side in ('down', 'up', 'north', 'south', 'west', 'east'):
                    if layer == 'overlay' and side != 'north':
                        continue
                    face = {'texture': '#front' if side == 'north' else '#back' if side == 'south' else '#side'}
                    if layer == 'colored':
                        face['tintindex'] = 0
                    faces[side] = face
                elements.append({'from': low, 'to': high, 'faces': faces})
            children[layer] = {'textures': textures, 'elements': elements, 'render_type': 'minecraft:cutout'}
        resources[f'models/block/machine/tool/{name}.json'] = {
            'parent': 'minecraft:block/block', 'loader': 'forge:composite',
            'textures': {'particle': f'gt6ce:block/machines/tools/crank/colored/{front}'}, 'children': children}
        for facing, rotation in (('north', {}), ('east', {'y': 90}), ('south', {'y': 180}),
                                 ('west', {'y': 270}), ('up', {'x': 270}), ('down', {'x': 90})):
            variants[f'active={str(active).lower()},facing={facing}'] = {
                'model': f'gt6ce:block/machine/tool/{name}', **rotation}
    resources['blockstates/crank.json'] = {'variants': variants}
    return resources


def write_crank(root):
    for relative, content in crank_resources().items():
        target = root / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(content, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    repo = Path(__file__).resolve().parents[2]
    for relative, content in crank_resources().items():
        source = 'core' if relative.startswith('blockstates/') else 'multiversion'
        target = repo / source / 'src/main/resources/assets/gt6ce' / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(content, indent=2) + '\n', encoding='utf-8')
