"""Rebuild capsule meshes from the audited GT6 catalog. Original textures are already imported."""
import json
from pathlib import Path
from rebuild_container_tool_models import box, layered, write, ASSETS

ROOT = Path(__file__).resolve().parents[1]


def main():
    rows = json.loads((ROOT / 'docs/capsule-cell-source.json').read_text(encoding='utf-8'))
    parts = [box([5, 1, 6], [11, 11, 10], up='top', down='bottom'),
             box([6, 1, 5], [10, 11, 11], up='top', down='bottom'),
             box([6, 0, 6], [10, 12, 10], up='top', down='bottom')]
    path = 'models/block/machine/container/cell.json'
    layered(path, 'tanks/cell', parts)
    model = json.loads((ASSETS / path).read_text(encoding='utf-8'))
    # Separate backing behind transparent side windows. The fluid renderer lies between
    # this backing and the outer colored/overlay shell, avoiding coplanar flicker.
    for layer in ['colored', 'overlay']:
        inside = []
        for part in parts:
            low, high = list(part['from']), list(part['to'])
            for axis in [0, 2]:
                low[axis] += .032
                high[axis] -= .032
            element = box(low, high, 'insides', up=None, down=None)
            if layer == 'colored':
                for face in element['faces'].values():
                    face['tintindex'] = 0
            inside.append(element)
        model['children']['inside_' + layer] = {
            'textures': {'insides': f'gt6ce:block/machines/tanks/cell/{layer}/insides'},
            'elements': inside, 'render_type': 'minecraft:cutout'}
    write(path, model)
    for row in rows:
        name = 'fluid_' + row['id']
        write(f'models/item/{name}.json', {'parent': 'gt6ce:block/machine/container/cell'})
        write(f'blockstates/{name}.json', {'variants': {'': {'model': 'gt6ce:block/machine/container/cell'}}})


if __name__ == '__main__':
    main()
