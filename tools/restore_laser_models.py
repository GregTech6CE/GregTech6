"""Restore original GT6 optical device textures and generate six-facing active models."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path

from pathlib import Path
import json
import hashlib
import shutil

ROOT = Path(__file__).resolve().parents[1]
ORIGINAL = ROOT.parent / 'gregtech6-master/gregtech6-master'
ASSETS = native_path(ROOT / 'src/main/resources/assets/gregtech')
SOURCE = native_path(ORIGINAL / 'src/main/resources/assets/gregtech/textures/blocks')
FAMILIES = {
    'co2_laser': ('machines/lasers/laser_electric', ['lv', 'mv', 'hv', 'ev', 'iv']),
    'flux_laser': ('machines/lasers/laser_flux', ['lv', 'mv', 'hv', 'ev', 'iv']),
    'laser_absorber': ('machines/laserabsorbers/electric_laser', ['lv', 'mv', 'hv', 'ev', 'iv']),
    'quantum_energizer': ('machines/quantumenergizer/quantum_laser', ['ev', 'iv', 'luv', 'zpm', 'uv']),
}

def write(path, value):
    path = ASSETS / path
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + '\n', encoding='utf8')

def layer(textures, elements, tinted=False):
    return {'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout',
            'textures': textures, 'elements': elements}

def cube(start, end, tinted=False):
    return {'from': start, 'to': end, 'faces': {
        face: dict(texture='#' + face, **({'tintindex': 0} if tinted else {}))
        for face in ['up', 'down', 'north', 'south', 'east', 'west']}}

def main():
    manifest = ROOT / 'tools/gt6_texture_sources.json'
    entries = json.loads(manifest.read_text(encoding='utf8'))
    def copy(source):
        relative = 'textures/block/' + source.relative_to(SOURCE).as_posix().lower()
        target = ASSETS / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, target)
        entries[relative] = {'source': source.relative_to(ORIGINAL).as_posix(),
                             'sha256': hashlib.sha256(source.read_bytes()).hexdigest()}
        return 'gregtech:' + relative.removeprefix('textures/').removesuffix('.png')
    rotations = {'north': {}, 'east': {'y': 90}, 'south': {'y': 180}, 'west': {'y': 270},
                 'up': {'x': 270}, 'down': {'x': 90}}
    for prefix, (folder, tiers) in FAMILIES.items():
        assert (SOURCE / folder).is_dir(), folder
        for file in (SOURCE / folder).rglob('*'):
            if file.is_file() and (file.suffix == '.png' or file.name.endswith('.png.mcmeta')):
                copy(file)
        for active in (False, True):
            children = {}
            for name, source in [('colored', 'colored'), ('overlay', 'overlay_active' if active else 'overlay')]:
                textures = {face: f'gregtech:block/{folder}/{source}/' +
                            ('front' if face == 'north' else 'back' if face == 'south' else 'side')
                            for face in rotations}
                children[name] = layer(textures, [cube([0, 0, 0], [16, 16, 16], name == 'colored')])
            model = 'block/machine/energy/' + prefix + ('_active' if active else '')
            write('models/' + model + '.json', {'parent': 'minecraft:block/block', 'loader': 'forge:composite',
                  'textures': {'particle': f'gregtech:block/{folder}/colored/side'}, 'children': children})
        for tier in tiers:
            name = prefix + '_' + tier
            write(f'blockstates/{name}.json', {'variants': {
                f'facing={facing},active={str(active).lower()}': dict(
                    model='gregtech:block/machine/energy/' + prefix + ('_active' if active else ''), **rotation)
                for facing, rotation in rotations.items() for active in (False, True)}})
            write(f'models/item/{name}.json', {'parent': 'gregtech:block/machine/energy/' + prefix})
    # Original textures are named after the BlockIcons enum, including their case.
    fiber = next(SOURCE.rglob('FIBER_WIRE.png'))
    overlay = next(SOURCE.rglob('FIBER_WIRE_OVERLAY.png'))
    textures = [copy(fiber), copy(overlay)]
    parts = {'center': ([6, 6, 6], [10, 10, 10]),
             'down': ([6, 0, 6], [10, 6, 10]), 'up': ([6, 10, 6], [10, 16, 10]),
             'north': ([6, 6, 0], [10, 10, 6]), 'south': ([6, 6, 10], [10, 10, 16]),
             'west': ([0, 6, 6], [6, 10, 10]), 'east': ([10, 6, 6], [16, 10, 10])}
    def cable_model(bounds):
        return {'parent': 'minecraft:block/block', 'loader': 'forge:composite',
                'textures': {'particle': textures[0]}, 'children': {
                    str(i): layer({face: texture for face in rotations}, [cube(a, b) for a, b in bounds])
                    for i, texture in enumerate(textures)}}
    for name, bounds in parts.items():
        write(f'models/block/machine/energy/laser_fiber_{name}.json', cable_model([bounds]))
    write('blockstates/laser_fiber_wire.json', {'multipart': [
        dict(**({} if name == 'center' else {'when': {name: 'true'}}),
             apply={'model': f'gregtech:block/machine/energy/laser_fiber_{name}'}) for name in parts]})
    write('models/item/laser_fiber_wire.json', cable_model([parts[k] for k in ('center', 'north', 'south')]))
    manifest.write_text(json.dumps(entries, indent=2, sort_keys=True) + '\n', encoding='utf8')

if __name__ == '__main__':
    main()
