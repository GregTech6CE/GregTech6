"""Guard source-default sediment registration and optional final JAR resource identity.

The old port-only seam must not be attached automatically. Keep its configured and placed
feature IDs for datapack compatibility; do not disable original clay pits or river black sand.
Example: python tools/integration/check_sediment_worldgen_registration.py --jar path/to/forge.jar --jar path/to/neo.jar
"""
import argparse
import json
from pathlib import Path
import zipfile

FEATURES = ('gt_surface_deposits', 'gt_pits', 'gt_black_sand')


def check_modifiers(entries, label):
    found = {}
    for name, raw in entries:
        data = json.loads(raw)
        # Only attachment entries count; a removal modifier is not a generator.
        if not data.get('type', '').endswith(':add_features'):
            continue
        features = data.get('features', [])
        if isinstance(features, str):
            features = [features]
        for feature in features:
            found[feature] = found.get(feature, 0) + 1
    if found.get('gt6ce:gt_surface_deposits', 0):
        raise ValueError(f'{label}: port-only surface seams still attached')
    for feature in ('gt6ce:gt_pits', 'gt6ce:gt_black_sand'):
        if found.get(feature, 0) != 1:
            raise ValueError(f'{label}: expected exactly one {feature}, got {found.get(feature, 0)}')


def identities():
    return [f'data/gt6ce/worldgen/{kind}/{name}.json'
            for kind in ('configured_feature', 'placed_feature') for name in FEATURES]


def check_source(root):
    for version, loader in (('1.20.1-forge', 'forge'), ('1.21.1-neoforge', 'neoforge')):
        directory = root / f'multiversion/versions/{version}/src/main/resources/data/gt6ce/{loader}/biome_modifier'
        check_modifiers(((str(f), f.read_text()) for f in directory.glob('*.json')), version)
        if (directory / 'gt_surface_deposits.json').exists():
            raise ValueError(f'{version}: obsolete seam modifier file still exists')
    for identity in identities():
        path = root / 'core/src/main/resources' / identity
        if not path.is_file():
            raise ValueError(f'configured/placed compatibility identity missing: {path}')
        json.loads(path.read_text())


def check_jar(path):
    with zipfile.ZipFile(path) as jar:
        names = set(jar.namelist())
        for loader in ('forge', 'neoforge'):
            obsolete = f'data/gt6ce/{loader}/biome_modifier/gt_surface_deposits.json'
            if obsolete in names:
                raise ValueError(f'{path}: stale deleted modifier is still packaged: {obsolete}')
        modifiers = [name for name in names if name.startswith('data/gt6ce/')
                     and '/biome_modifier/' in name and name.endswith('.json')]
        check_modifiers(((name, jar.read(name)) for name in modifiers), str(path))
        for identity in identities():
            if identity not in names:
                raise ValueError(f'{path}: compatibility identity missing: {identity}')
            json.loads(jar.read(identity))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument('--jar', type=Path, action='append', default=[])
    args = parser.parse_args()
    try:
        check_source(args.root)
        for jar in args.jar:
            check_jar(jar)
    except (ValueError, OSError, zipfile.BadZipFile) as error:
        raise SystemExit(str(error)) from error
    print(f'PASS: both loader sediment registrations and {len(args.jar)} final JAR(s); compatibility IDs retained')
