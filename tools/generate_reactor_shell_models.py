"""Restore GT6 reactor shell passes and independent hot/cold outlet textures on both loaders.

Source: MultiTileEntityReactorCore1x1/2x2.setBlockBounds2/getTexture2.
The six shell slabs are two pixels thick and render only their two axial faces.
Fuel rods and tank contents remain block-entity render passes, separate from the shell.
"""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
FACES = ('down', 'up', 'north', 'south', 'west', 'east')
BOUNDS = {
    'down': ([0, 0, 0], [16, 2, 16]), 'up': ([0, 14, 0], [16, 16, 16]),
    'north': ([0, 0, 0], [16, 16, 2]), 'south': ([0, 0, 14], [16, 16, 16]),
    'west': ([0, 0, 0], [2, 16, 16]), 'east': ([14, 0, 0], [16, 16, 16]),
}
OPPOSITE = dict(zip(FACES, ('up', 'down', 'south', 'north', 'east', 'west')))


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    content = json.dumps(data, ensure_ascii=False, indent=2) + '\n'
    if not path.exists() or path.read_text(encoding='utf-8') != content:
        path.write_text(content, encoding='utf-8')


def shell(folder, sub, hot, cold):
    textures, elements = {}, []
    for face in FACES:
        texture = 'face1' if face == hot else 'face2' if face == cold else \
                  'bottom' if face == 'down' else 'top' if face == 'up' else 'side1'
        textures[face] = f'gregtech:block/machines/generators/{folder}/{sub}/{texture}'
        quad = {'texture': '#' + face, 'uv': [0, 0, 16, 16]}
        if sub == 'colored':
            quad['tintindex'] = 0
        lower, upper = BOUNDS[face]
        elements.append({'from': lower, 'to': upper,
                         'faces': {face: dict(quad), OPPOSITE[face]: dict(quad)}})
    textures['particle'] = textures['north']
    return {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout',
            'ambientocclusion': False, 'textures': textures, 'elements': elements}


def main():
    assets = ROOT / 'platform-shared/src/main/resources/assets/gregtech'
    for name, folder in (('reactor_core', 'reactor_core_1x1'),
                         ('reactor_core_2x2', 'reactor_core_2x2')):
            path = assets / f'models/block/machine/energy/{name}.json'
            previous = json.loads(path.read_text(encoding='utf-8'))
            variants = {}
            for hot in FACES:
                for cold in FACES:
                    ident = name if hot == cold == 'down' else f'{name}_{hot}_{cold}'
                    model = {'parent': 'minecraft:block/block', 'loader': previous['loader'],
                             'display': previous.get('display', {}),
                             'children': {'colored': shell(folder, 'colored', hot, cold),
                                          'overlay': shell(folder, 'overlay', hot, cold)}}
                    write(path.parent / (ident + '.json'), model)
                    variants[f'hot_outlet={hot},cold_outlet={cold}'] = {
                        'model': 'gregtech:block/machine/energy/' + ident}
            # Identical state JSON is shared; native loader IDs only occur in platform models.
            write(ROOT / f'core/src/main/resources/assets/gregtech/blockstates/{name}.json',
                  {'variants': variants})
    print('Restored six reactor shell passes and 36 hot/cold texture states per core and loader.')


if __name__ == '__main__':
    main()
