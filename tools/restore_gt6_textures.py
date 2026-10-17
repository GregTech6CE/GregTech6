"""Restore referenced GT6 textures byte-for-byte; never invent fallback artwork.

Pass the original repository root (the directory containing src/main/resources).
Records source paths and checksums, including animation metadata, for review.
"""
import argparse
import hashlib
import json
from pathlib import Path
from check_render_resources import ASSETS, check

SHARED_ASSETS = Path(__file__).resolve().parents[1] / "platform-shared" / "src" / "main" / "resources" / "assets" / "gregtech"

RENAMES = {
    '/machines/heaters/electric/': '/machines/heaters/heat_electric/',
    '/machines/coolers/electric/': '/machines/cooler/cryo_electric/',
    '/sensors/geiger/': '/sensors/geigercounter/',
    '/sensors/weightometric/': '/sensors/heavyweightometer/',
    '/crategt64': '/crategt',
}
for species, icon in [('rubber','rubber'), ('maple','maple'), ('rainbowood','rainbowood'),
                      ('willow','willow'), ('blue_mahoe','bluemahoe')]:
    for old, new in [(f'log_{species}_top', f'log_top_{icon}'), (f'log_{species}', f'log_side_{icon}'),
                     (f'leaves_{species}', f'leaves_{icon}'), (f'planks_{species}', f'planks_{icon}'),
                     (f'sapling_{species}', f'sapling_small_{icon}')]:
        RENAMES[f'gregtech:block/wood/{old}"'] = f'gregtech:block/iconsets/{new}"'


def original_path(resource):
    path = resource.removeprefix('gregtech:')
    head, tail = path.split('/', 1)
    return ({'block': 'blocks', 'item': 'items'}.get(head, head) + '/' + tail + '.png').replace('/material_icons/', '/materialicons/')


def restore(original):
    textures = original / 'src/main/resources/assets/gregtech/textures'
    if not textures.is_dir():
        raise ValueError(f'Original textures not found: {textures}')
    originals = {p.relative_to(textures).as_posix().lower(): p for p in textures.rglob('*.png')}
    manifest_path = ASSETS.parents[4] / 'tools/gt6_texture_sources.json'
    manifest = json.loads(manifest_path.read_text(encoding='utf-8')) if manifest_path.exists() else {}
    models = list((SHARED_ASSETS / 'models').rglob('*.json'))
    changed = 0
    for path in models:
        before = path.read_text(encoding='utf-8')
        data = json.loads(before)
        def repair(node):
            if isinstance(node, list):
                for child in node:
                    repair(child)
            elif isinstance(node, dict):
                if isinstance(node.get('textures'), dict):
                    for key, value in node['textures'].items():
                        encoded = json.dumps(value)
                        for old, new in RENAMES.items():
                            encoded = encoded.replace(old, new)
                        node['textures'][key] = json.loads(encoded)
                for child in node.values():
                    repair(child)
        repair(data)
        after = before if data == json.loads(before) else json.dumps(data, indent=2) + '\n'
        if after != before:
            path.write_text(after, encoding='utf-8')
            changed += 1
    issues, _ = check(models + list((SHARED_ASSETS / 'blockstates').glob('*.json')))
    copied = 0
    for _, kind, resource in issues:
        if kind != 'texture':
            continue
        source = originals.get(original_path(resource).lower())
        target = ASSETS / 'textures' / (resource.split(':')[1] + '.png')
        if source is None or target.exists():
            continue
        for suffix in ('', '.mcmeta'):
            src = Path(str(source) + suffix)
            if not src.exists():
                continue
            dst = Path(str(target) + suffix)
            dst.parent.mkdir(parents=True, exist_ok=True)
            data = src.read_bytes()
            dst.write_bytes(data)
            manifest[dst.relative_to(ASSETS).as_posix()] = {
                'source': src.relative_to(original).as_posix(),
                'sha256': hashlib.sha256(data).hexdigest(),
            }
            copied += 1
    manifest_path.parent.mkdir(parents=True, exist_ok=True)
    manifest_path.write_text(json.dumps(manifest, indent=2, sort_keys=True) + '\n', encoding='utf-8')
    print(f'{changed} model references repaired; {copied} original files copied')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('original', type=Path)
    restore(parser.parse_args().original.resolve())
