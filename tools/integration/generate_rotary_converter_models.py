"""Original14 motor/dynamo visual states; preserves each platform's existing layered model.

GregTech-6 Team / Gregorius Techneticies, LGPL-3.0-or-later source selection:
MultiTileEntityMotorElectric/Flux and MultiTileEntityDynamoElectric/Flux.getTexture2.
No old asset generators or external source trees are modified.
"""
import argparse
import copy
import hashlib
import itertools
import json
from pathlib import Path

TIERS = ['lv', 'mv', 'hv', 'ev', 'iv']
DEVICES = [(family + '_' + tier, 'motor' in family)
           for family in ['electric_motor', 'electric_dynamo', 'flux_motor', 'flux_dynamo']
           for tier in (TIERS if family.startswith('electric') else TIERS[:2])]
ROTATIONS = {'north': {}, 'east': {'y': 90}, 'south': {'y': 180}, 'west': {'y': 270},
             'up': {'x': 270}, 'down': {'x': 90}}


def write(path, value):
    text = json.dumps(value, ensure_ascii=False, indent=2) + '\n'
    if not path.exists() or path.read_text(encoding='utf-8') != text:
        path.write_text(text, encoding='utf-8')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--repo', type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument('--audit', type=Path, required=True)
    args = parser.parse_args()
    texture_pins, generated = {}, []
    for platform in ['src', 'neoforge/src']:
        assets = args.repo / platform / 'main/resources/assets/gregtech'
        for ident, motor in DEVICES:
            model_path = assets / 'models/block/machine/energy' / (ident + '.json')
            base = json.loads(model_path.read_text(encoding='utf-8'))
            assert base['loader'] == ('forge:composite' if platform == 'src' else 'neoforge:composite')
            overlay = base['children']['layer1']
            assert 'tintindex' not in next(iter(overlay['elements'][0]['faces'].values()))
            kinds = ['ls', 'lf', 'rs', 'rf'] if motor else ['active']
            for kind in kinds:
                active = copy.deepcopy(base)
                target = active['children']['layer1']['textures']
                for face, reference in target.items():
                    assert '/overlay/' in reference
                    target[face] = reference.replace('/overlay/', '/overlay_active' + ('_' + kind if motor else '') + '/')
                    texture = args.repo / 'core/src/main/resources/assets/gregtech/textures' / (target[face].split(':', 1)[1] + '.png')
                    assert texture.is_file(), texture
                    texture_pins[str(texture)] = hashlib.sha256(texture.read_bytes()).hexdigest()
                path = model_path.with_name(ident + '_' + kind + '.json')
                write(path, active)
                generated.append(str(path.relative_to(args.repo)))
            variants = {}
            for facing, rot in ROTATIONS.items():
                for activity in range(3):
                    options = itertools.product([False, True], repeat=2) if motor else [(False, False)]
                    for left, fast in options:
                        key = f'facing={facing},activity={activity}'
                        suffix = ''
                        if motor:
                            key += f',counter_clockwise={str(left).lower()},fast={str(fast).lower()}'
                            if activity > 0: suffix = '_' + ('l' if left else 'r') + ('f' if fast else 's')
                        elif activity > 0: suffix = '_active'
                        variants[key] = {'model': 'gregtech:block/machine/energy/' + ident + suffix, **rot}
            state = assets / 'blockstates' / (ident + '.json')
            write(state, {'variants': variants})
            generated.append(str(state.relative_to(args.repo)))
    args.audit.parent.mkdir(parents=True, exist_ok=True)
    args.audit.write_text(json.dumps({'devices': len(DEVICES), 'motor_variants_per_device': 72,
                                     'dynamo_variants_per_device': 18, 'files': generated,
                                     'existing_shared_texture_sha256': texture_pins}, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'devices': len(DEVICES), 'written_files': len(generated), 'shared_active_textures': len(texture_pins)}))


if __name__ == '__main__':
    main()
