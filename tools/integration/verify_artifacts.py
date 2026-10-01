"""Check current platform jars; packaging evidence is not runtime or gameplay evidence.

Requires Python 3.11+ (CI pins 3.12). Verify one platform with --platform/--jar,
or both with --forge/--neoforge. The core compilation output is authoritative;
two jars agreeing with each other is insufficient if both contain stale classes.
"""
import argparse
import hashlib
import json
import os
import zipfile
from collections import Counter
from pathlib import Path
import tomllib


PLATFORMS = {
    'forge': {'metadata': 'META-INF/mods.toml', 'minecraft': '1.20.1', 'loader': 'forge'},
    'neoforge': {'metadata': 'META-INF/neoforge.mods.toml', 'minecraft': '1.21.1', 'loader': 'neoforge'},
}

# Both production platforms must retain the restored gameplay implementations.
# Shared-core agreement alone also accepts a material-only NeoForge skeleton.
GAMEPLAY_CLASSES = (
    'com/gregtech/gregtech/registry/GTBlocks.class',
    'com/gregtech/gregtech/registry/GTBlockEntities.class',
    'com/gregtech/gregtech/registry/GTMachines.class',
    'com/gregtech/gregtech/worldgen/GTFeatures.class',
    'com/gregtech/gregtech/blockentity/machine/SmeltingCrucibleBlockEntity.class',
)


def read_properties(path):
    """Read the literal key=value properties used by this build's metadata."""
    properties = {}
    for line in path.read_text(encoding='utf-8').splitlines():
        line = line.strip()
        if line and not line.startswith(('#', '!')) and '=' in line:
            key, value = line.split('=', 1)
            properties[key.strip()] = value.strip()
    required = ('mod_id', 'mod_version', 'mod_license', 'minecraft_version',
                'minecraft_version_range', 'forge_version_range', 'loader_version_range')
    missing = [key for key in required if not properties.get(key)]
    if missing:
        raise ValueError(f'{path}: missing metadata properties: {missing}')
    if properties['mod_id'] != 'gregtech6':
        raise ValueError('The loader mod ID must be gregtech6')
    if properties['minecraft_version'] != PLATFORMS['forge']['minecraft']:
        raise ValueError('The Forge target must remain Minecraft 1.20.1')
    if properties['minecraft_version_range'] != '[1.20.1]':
        raise ValueError('The Forge metadata must target exactly Minecraft 1.20.1')
    return properties


def compiled_core_hashes(core_output):
    classes = sorted(core_output.rglob('*.class'))
    if not classes:
        raise ValueError('No compiled shared-core classes; build :core:classes first')
    return {path.relative_to(core_output).as_posix(): hashlib.sha256(path.read_bytes()).hexdigest()
            for path in classes}


def test_only_entries(repo):
    """Only forbid explicitly separate test source sets, preserving baseline main GameTests."""
    roots = (repo / 'core/src/test', repo / 'src/bootstrapGameTest',
             repo / 'neoforge/src/bootstrapGameTest')
    class_stems, resources = set(), set()
    for root in roots:
        java = root / 'java'
        class_stems.update(path.relative_to(java).with_suffix('').as_posix()
                           for path in java.rglob('*.java'))
        resource_root = root / 'resources'
        resources.update(path.relative_to(resource_root).as_posix()
                         for path in resource_root.rglob('*') if path.is_file())
    # Retain known exclusions even if test sources are accidentally removed.
    class_stems.update({
        'com/gregtech/gregtech/core/CoreBehaviorContracts',
        'com/gregtech/gregtech/gametest/IntegrationBootstrapTests',
        'com/gregtech/gregtech/platform/neoforge/gametest/SharedCoreBootstrapGameTests',
    })
    return class_stems, resources


def check_metadata(raw, platform, properties):
    spec = PLATFORMS[platform]
    metadata = tomllib.loads(raw.decode('utf-8'))
    if metadata.get('modLoader') != 'javafml':
        raise ValueError('modLoader must be javafml')
    expected_loader_range = (properties['loader_version_range'] if platform == 'forge' else '[1,)')
    if metadata.get('loaderVersion') != expected_loader_range:
        raise ValueError(f'loaderVersion must be {expected_loader_range!r}')
    if metadata.get('license') != properties['mod_license']:
        raise ValueError('Descriptor license differs from the preserved root declaration')
    mods = metadata.get('mods')
    if not isinstance(mods, list) or len(mods) != 1 or not isinstance(mods[0], dict):
        raise ValueError('Descriptor must declare exactly one integration mod')
    mod = mods[0]
    for key, expected in (('modId', properties['mod_id']), ('version', properties['mod_version']),
                          ('displayName', properties['mod_name'])):
        if mod.get(key) != expected:
            raise ValueError(f'{key} must be {expected!r}, found {mod.get(key)!r}')
    all_dependencies = metadata.get('dependencies', {})
    if not isinstance(all_dependencies, dict) or set(all_dependencies) != {properties['mod_id']}:
        raise ValueError('Dependencies must belong to the configured integration mod')
    dependencies = all_dependencies[properties['mod_id']]
    if not isinstance(dependencies, list) or any(not isinstance(dep, dict) for dep in dependencies):
        raise ValueError('Dependencies must be an array of tables')
    ids = [dep.get('modId') for dep in dependencies]
    if any(count > 1 for count in Counter(ids).values()):
        raise ValueError('Duplicate dependency modIds')
    opposite_loader = 'neoforge' if platform == 'forge' else 'forge'
    if opposite_loader in ids:
        raise ValueError(f'{platform} descriptor must not depend on {opposite_loader}')
    expected_ranges = {
        'minecraft': f"[{spec['minecraft']}]",
        # Neo version/range matches the pinned P1 module; update both on a deliberate upgrade.
        spec['loader']: (properties['forge_version_range'] if platform == 'forge' else '[21.1.243,)'),
    }
    for dependency_id, expected_range in expected_ranges.items():
        matching = [dep for dep in dependencies if dep.get('modId') == dependency_id]
        if len(matching) != 1:
            raise ValueError(f'Missing required {dependency_id} dependency')
        dep = matching[0]
        required = dep.get('mandatory') is True if platform == 'forge' else dep.get('type') == 'required'
        if not required or dep.get('side') != 'BOTH' or dep.get('versionRange') != expected_range:
            raise ValueError(f'{dependency_id} must be required on BOTH sides with range {expected_range!r}')
    return {'mod_id': mod['modId'], 'mod_version': mod['version'],
            'minecraft': spec['minecraft'], 'loader': spec['loader'],
            'loader_version_range': expected_loader_range, 'dependency_ranges': expected_ranges,
            'license': metadata['license']}


def inspect(path, platform, required_core, properties, forbidden_tests):
    if not path.is_file():
        raise ValueError(f'No platform mod jar: {path}')
    if path.suffix.lower() != '.jar':
        raise ValueError(f'Expected a .jar artifact: {path}')
    metadata_path = PLATFORMS[platform]['metadata']
    with zipfile.ZipFile(path) as archive:
        counts = Counter(archive.namelist())
        duplicates = [name for name, count in counts.items() if count > 1]
        if duplicates:
            raise ValueError(f'{path}: duplicate ZIP entries: {duplicates[:10]}')
        missing_gameplay = [name for name in GAMEPLAY_CLASSES if counts.get(name) != 1]
        if missing_gameplay:
            raise ValueError(f'{path}: incomplete platform gameplay content: {missing_gameplay}')
        if metadata_path not in counts:
            raise ValueError(f'{path}: missing {metadata_path}')
        other_descriptors = {spec['metadata'] for spec in PLATFORMS.values()} - {metadata_path}
        if other_descriptors.intersection(counts):
            raise ValueError(f'{path}: contains the other platform loader descriptor')
        try:
            metadata = check_metadata(archive.read(metadata_path), platform, properties)
        except (ValueError, TypeError, UnicodeError) as error:
            raise ValueError(f'{path}: invalid {metadata_path}: {error}') from error
        stems, resources = forbidden_tests
        contamination = [name for name in counts
                         if name in resources or name.startswith('data/gregtech_bootstrap/')
                         or (name.endswith('.class') and any(
                             name == stem + '.class' or name.startswith(stem + '$') for stem in stems))]
        if contamination:
            raise ValueError(f'{path}: test-only entries: {contamination[:10]}')
        hashes = {}
        for name, expected_hash in required_core.items():
            if counts.get(name) != 1:
                raise ValueError(f'{path}: missing shared class {name}')
            digest = hashlib.sha256(archive.read(name)).hexdigest()
            if digest != expected_hash:
                raise ValueError(f'{path}: shared class differs from current core output: {name}')
            hashes[name] = digest
        return {
            'path': str(path.resolve()), 'sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
            'bytes': path.stat().st_size, 'zip_entries': len(counts),
            'metadata_path': metadata_path, 'metadata': metadata, 'core_class_sha256': hashes,
        }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--platform', choices=PLATFORMS)
    parser.add_argument('--jar', type=Path)
    parser.add_argument('--forge', type=Path)
    parser.add_argument('--neoforge', type=Path)
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    single = args.platform is not None or args.jar is not None
    if single:
        if not (args.platform and args.jar) or args.forge or args.neoforge:
            parser.error('Use --platform forge|neoforge --jar PATH, or --forge PATH --neoforge PATH')
        targets = {args.platform: args.jar}
    else:
        if not (args.forge and args.neoforge):
            parser.error('Both --forge and --neoforge are required for a dual-platform check')
        targets = {'forge': args.forge, 'neoforge': args.neoforge}
    repo = Path(__file__).resolve().parents[2]
    try:
        properties = read_properties(repo / 'gradle.properties')
        core = compiled_core_hashes(repo / 'core/build/classes/java/main')
        forbidden_tests = test_only_entries(repo)
        artifacts = {platform: inspect(path, platform, core, properties, forbidden_tests)
                     for platform, path in targets.items()}
        if len(artifacts) == 2 and artifacts['forge']['core_class_sha256'] != artifacts['neoforge']['core_class_sha256']:
            raise ValueError('Shared-core bytecode differs between platform jars')
    except (OSError, ValueError, TypeError, zipfile.BadZipFile) as error:
        parser.exit(1, f'Packaging failed: {error}\n')
    result = {'status': 'passed', 'scope': 'packaging-only; no gameplay or runtime claim',
              'mod_id': properties['mod_id'], 'mod_version': properties['mod_version'],
              'build_commit': os.environ.get('GITHUB_SHA'),
              'shared_classes': len(core), 'compiled_core_class_sha256': core, **artifacts}
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(result, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    print(f"Packaging passed: {len(core)} current shared classes; verified {' + '.join(targets)} metadata; no duplicate or test-only entries")


if __name__ == '__main__':
    main()
