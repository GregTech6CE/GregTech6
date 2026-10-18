"""Verify one loader's compile-only addon SDK. This is not addon runtime evidence."""
from pathlib import Path
import argparse, hashlib, json, zipfile
from collections import Counter
from verify_artifacts import compiled_core_hashes

LOADERS = {
    'forge': {'core_version': 61, 'platform_version': 61},
    'neoforge': {'core_version': 61, 'platform_version': 65},
}


def class_version(data):
    if data[:4] != b'\xca\xfe\xba\xbe':
        raise ValueError('not a class file')
    return int.from_bytes(data[6:8], 'big')


def inspect(path, repo, loader):
    if loader not in LOADERS:
        raise ValueError('unknown loader ' + loader)
    core = compiled_core_hashes(repo / 'core/build/classes/java/api')
    platform_dir = repo / ('build/classes/java/api' if loader == 'forge' else 'neoforge/build/classes/java/api')
    platform = compiled_core_hashes(platform_dir)
    overlap = set(core).intersection(platform)
    if overlap:
        raise ValueError('core and platform API class names overlap: ' + ', '.join(sorted(overlap)[:5]))
    required = {**core, **platform}
    versions = LOADERS[loader]
    with zipfile.ZipFile(path) as archive:
        counts = Counter(archive.namelist())
        if any(count != 1 for count in counts.values()):
            raise ValueError('Duplicate SDK entries')
        actual = {name for name in counts if name.endswith('.class')}
        if actual != set(required):
            missing = sorted(set(required) - actual)[:8]
            extra = sorted(actual - set(required))[:8]
            raise ValueError(f'SDK classes differ from the API compilation; missing {missing}; extra {extra}')
        for name, expected in required.items():
            data = archive.read(name)
            version = class_version(data)
            expected_version = versions['core_version'] if name in core else versions['platform_version']
            if hashlib.sha256(data).hexdigest() != expected or version != expected_version:
                raise ValueError(f'Stale SDK class or unexpected class version {version}: {name}')
        if any(name.startswith(('assets/', 'data/')) or name in ('META-INF/mods.toml', 'META-INF/neoforge.mods.toml')
               for name in counts):
            raise ValueError('SDK contains runtime assets or a loader descriptor')
        for name, source in [('LICENSE', 'LICENSE'), ('GPL-3.0.txt', 'docs/licenses/GPL-3.0.txt'), ('NOTICE', 'NOTICE')]:
            if counts.get('META-INF/gregtech6/' + name) != 1 or archive.read('META-INF/gregtech6/' + name) != (repo / source).read_bytes():
                raise ValueError('SDK license/notice differs: ' + name)
        if any('META-INF/gregtech6/' + name in counts for name in ['COPYING.LESSER', 'COPYING', 'LICENSE.txt']):
            raise ValueError('SDK contains superseded duplicate licenses')
    return {
        'path': str(path.resolve()), 'loader': loader, 'bytes': path.stat().st_size,
        'sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
        'core_api_classes': len(core), 'platform_api_classes': len(platform),
        'compile_only': True, 'external_addon_runtime_verified': False,
    }


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jar', type=Path, required=True)
    parser.add_argument('--loader', choices=tuple(LOADERS), required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    result = inspect(args.jar, Path(__file__).resolve().parents[2], args.loader)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(f"Addon SDK packaging passed ({args.loader}): {result['core_api_classes']} core API classes, "
          f"{result['platform_api_classes']} platform API classes; no runtime assets")
