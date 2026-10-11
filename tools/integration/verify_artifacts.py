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
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from localization import synchronize


PLATFORMS = {
    'forge': {'metadata': 'META-INF/mods.toml', 'minecraft': '1.20.1', 'loader': 'forge'},
    'neoforge': {'metadata': 'META-INF/neoforge.mods.toml', 'minecraft': '1.21.1', 'loader': 'neoforge'},
}

# Both production platforms must retain the restored gameplay implementations.
# Shared-core agreement alone also accepts a material-only NeoForge skeleton.
GAMEPLAY_CLASSES = (
    'com/gregtech/gregtech/registry/GTBlocks.class',
    'com/gregtech/gregtech/registry/GTBlockEntities.class',
    'com/gregtech/gregtech/worldgen/GTFeatures.class',
    'com/gregtech/gregtech/item/GunToolItem.class',
    'com/gregtech/gregtech/item/PocketToolItem.class',
    'com/gregtech/gregtech/item/ElectricToolItem.class',
    'com/gregtech/gregtech/content/tool/ElectricUtilityHarvest.class',
    'com/gregtech/gregtech/item/behavior/BehaviorPlugLeak.class',
)
PLATFORM_GAMEPLAY_CLASSES = {
    'forge': (
        'com/gregtech/gregtech/registry/GTMachines.class',
        'com/gregtech/gregtech/blockentity/machine/SmeltingCrucibleBlockEntity.class',
    ),
    'neoforge': (
        'com/gregtech/gregtech/platform/neoforge/machine/BasicMachineRegistries.class',
        'com/gregtech/gregtech/platform/neoforge/smeltery/SmeltingCrucibleEntity.class',
        'com/gregtech/gregtech/client/RopeClientColors.class',
    ),
}


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


def current_platform_hashes(repo, platform, artifact, required_core):
    """Forge uses current SRG output; NeoForge ships the named compiler output."""
    if platform == 'forge':
        mapped = repo / 'build/forge-intermediates' / artifact.name
        if not mapped.is_file():
            raise ValueError(f'Missing current Forge reobfuscated input: {mapped}; run :assemble')
        with zipfile.ZipFile(mapped) as archive:
            return {name: hashlib.sha256(archive.read(name)).hexdigest()
                    for name in archive.namelist()
                    if name.endswith('.class') and name not in required_core}
    output = repo / 'neoforge/build/classes/java/main'
    result = {path.relative_to(output).as_posix(): hashlib.sha256(path.read_bytes()).hexdigest()
              for path in output.rglob('*.class')
              if path.relative_to(output).as_posix() not in required_core}
    if not result:
        raise ValueError('No current NeoForge platform compilation output')
    return result


def test_only_entries(repo):
    """All test source sets are separate; none belong in a production mod JAR."""
    roots = (repo / 'core/src/test', repo / 'src/bootstrapGameTest',
             repo / 'neoforge/src/bootstrapGameTest', repo / 'src/productionSmoke',
             repo / 'neoforge/src/productionSmoke')
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
        'com/gregtech/gregtech/jei/JeiMachineIndexTests',
    })
    return class_stems, resources


def is_test_entry(name, stems, resources):
    # Constant-time class lookup instead of testing every entry against every
    # fixture. Keep namespace exclusions even if a test is moved/deleted later.
    return (name in resources or name.startswith((
        'data/gregtech_bootstrap/', 'data/gregtech_repair/', 'data/gregtech_sensor_source/',
        'com/gregtech/gregtech/core/',
        'com/gregtech/gregtech/gametest/',
        'com/gregtech/gregtech/platform/neoforge/gametest/'))
        or (name.endswith('.class') and name[:-6].split('$', 1)[0] in stems))


def check_metadata(raw, platform, properties):
    spec = PLATFORMS[platform]
    metadata = tomllib.loads(raw.decode('utf-8'))
    if metadata.get('modLoader') != 'javafml':
        raise ValueError('modLoader must be javafml')
    expected_loader_range = (properties['loader_version_range'] if platform == 'forge' else '[1,)')
    if metadata.get('loaderVersion') != expected_loader_range:
        raise ValueError(f'loaderVersion must be {expected_loader_range!r}')
    if metadata.get('license') != properties['mod_license']:
        raise ValueError('Descriptor license differs from the current root declaration')
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
        bad_entry = archive.testzip()
        if bad_entry:
            raise ValueError(f'{path}: corrupt ZIP entry: {bad_entry}')
        counts = Counter(archive.namelist())
        duplicates = [name for name, count in counts.items() if count > 1]
        if duplicates:
            raise ValueError(f'{path}: duplicate ZIP entries: {duplicates[:10]}')
        required_gameplay = GAMEPLAY_CLASSES + PLATFORM_GAMEPLAY_CLASSES[platform]
        missing_gameplay = [name for name in required_gameplay if counts.get(name) != 1]
        if missing_gameplay:
            raise ValueError(f'{path}: incomplete platform gameplay content: {missing_gameplay}')
        repo = Path(__file__).resolve().parents[2]
        native_hashes = current_platform_hashes(repo, platform, path, required_core)
        for name, expected in native_hashes.items():
            if counts.get(name) != 1 or hashlib.sha256(archive.read(name)).hexdigest() != expected:
                raise ValueError(f'{path}: platform class differs from current mapped/compiler output: {name}; run :assemble')
        if metadata_path not in counts:
            raise ValueError(f'{path}: missing {metadata_path}')
        other_descriptors = {spec['metadata'] for spec in PLATFORMS.values()} - {metadata_path}
        if other_descriptors.intersection(counts):
            raise ValueError(f'{path}: contains the other platform loader descriptor')
        try:
            metadata = check_metadata(archive.read(metadata_path), platform, properties)
        except (ValueError, TypeError, UnicodeError) as error:
            raise ValueError(f'{path}: invalid {metadata_path}: {error}') from error
        if properties['mod_license'] == 'LGPL-3.0-or-later':
            repo = Path(__file__).resolve().parents[2]
            for name, source in (('LICENSE', 'LICENSE'), ('GPL-3.0.txt', 'docs/licenses/GPL-3.0.txt'), ('NOTICE', 'NOTICE')):
                if counts.get('META-INF/gregtech6/' + name) != 1 or archive.read('META-INF/gregtech6/' + name) != (repo / source).read_bytes():
                    raise ValueError(f'{path}: missing/stale current license or notice: {name}')
            if any('META-INF/gregtech6/' + name in counts for name in ('COPYING.LESSER', 'COPYING', 'LICENSE.txt')):
                raise ValueError(f'{path}: contains the superseded duplicate license layout')
        stems, resources = forbidden_tests
        contamination = [name for name in counts if is_test_entry(name, stems, resources)]
        if contamination:
            raise ValueError(f'{path}: test-only entries: {contamination[:10]}')
        for locale in ('en_us', 'zh_cn'):
            language = f'assets/gregtech/lang/{locale}.json'
            if archive.read(language) != (repo / 'core/src/main/resources' / language).read_bytes():
                raise ValueError(f'{path}: language differs from the checked shared source: {language}')
        mixin_verification = None
        if platform == 'forge':
            config = json.loads(archive.read('gregtech.mixins.json'))
            refmap_name = config['refmap']
            if counts.get(refmap_name) != 1:
                raise ValueError(f'{path}: missing Mixin refmap {refmap_name}')
            refmap = json.loads(archive.read(refmap_name))
            current = Path(__file__).resolve().parents[2] / 'build/tmp/compileJava' / refmap_name
            if archive.read(refmap_name) != current.read_bytes():
                raise ValueError(f'{path}: Mixin refmap differs from current compiler output')
            mappings = refmap.get('mappings', {})
            if not mappings or mappings != refmap.get('data', {}).get('searge'):
                raise ValueError(f'{path}: incomplete named-to-SRG Mixin mappings')
            # These two mixins target Forge API methods with remap=false.
            # ComponentCoverCapabilitiesMixin targets getCapability on GT classes;
            # neither those classes nor Forge capability method names are obfuscated.
            api_mixins = {"WaterContainerMixin", "ComponentCoverCapabilitiesMixin"}
            required_mappings = {config['package'].replace('.', '/') + '/' + name
                                 for name in config['mixins'] + config.get('client', [])
                                 if name not in api_mixins}
            if not required_mappings.issubset(mappings):
                raise ValueError(f'{path}: missing complete Mixin mappings: {sorted(required_mappings - mappings.keys())}')
            target = 'spawnAtLocation(Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;'
            expected = 'Lnet/minecraft/world/entity/Entity;m_19998_(Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;'
            if mappings.get('com/gregtech/gregtech/mixin/EntityFallingOreDropMixin', {}).get(target) != expected:
                raise ValueError(f'{path}: missing falling-ore production mapping')
            manifest = archive.read('META-INF/MANIFEST.MF').decode('utf-8').replace('\r\n ', '')
            if 'MixinConfigs: gregtech.mixins.json' not in manifest:
                raise ValueError(f'{path}: missing production Mixin manifest registration')
            mixin_verification = {'refmap': refmap_name, 'mapped_classes': len(mappings),
                                  'sha256': hashlib.sha256(archive.read(refmap_name)).hexdigest(),
                                  'unmapped_forge_api_mixins': sorted(api_mixins)}
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
            'zip_crc_checked': True, 'mixin_verification': mixin_verification,
            'current_platform_classes_checked': len(native_hashes),
            'current_platform_classes_sha256': hashlib.sha256(
                json.dumps(native_hashes, sort_keys=True).encode('utf-8')).hexdigest(),
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
        language = synchronize(repo)
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
              'language_source_sha256': language['source_sha256'],
              'language_aliases': language['native_aliases'],
              'shared_classes': len(core), 'compiled_core_class_sha256': core, **artifacts}
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(result, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    print(f"Packaging passed: {len(core)} current shared classes; verified {' + '.join(targets)} metadata; no duplicate or test-only entries")


if __name__ == '__main__':
    main()
