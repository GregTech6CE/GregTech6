"""Verify an isolated compile-only addon SDK against current API outputs.

The pure core SDK uses Java 17. Loader SDKs include the same core API and their
own native API, never main implementation classes or runtime assets.
"""
from pathlib import Path
import argparse
import hashlib
import json
import zipfile
from collections import Counter

from verify_artifacts import compiled_core_hashes
from native_layout import platform_build
from check_addon_api_closure import class_names, violations

LOADERS = {'core': 61, 'forge': 61, 'neoforge': 65}


def inspect(path, repo, loader='core'):
    if loader not in LOADERS:
        raise ValueError('unknown loader ' + loader)
    core_dir = repo / 'core/build/classes/java/api'
    core = compiled_core_hashes(core_dir)
    platform_dir = None if loader == 'core' else platform_build(repo, loader) / 'classes/java/api'
    platform = {} if platform_dir is None else compiled_core_hashes(platform_dir)
    overlap = set(core).intersection(platform)
    if overlap:
        raise ValueError('core and native API class names overlap: ' + ', '.join(sorted(overlap)[:8]))
    required = {**core, **platform}
    allowed = class_names(core_dir)
    if platform_dir:
        allowed |= class_names(platform_dir)
    errors = violations(core_dir, class_names(core_dir), 'core')
    if platform_dir:
        errors.extend(violations(platform_dir, allowed, loader))
    if errors:
        raise ValueError('SDK API closure failed: ' + '; '.join(errors[:12]))

    with zipfile.ZipFile(path) as archive:
        bad = archive.testzip()
        if bad:
            raise ValueError('Corrupt SDK ZIP entry: ' + bad)
        counts = Counter(archive.namelist())
        if any(count != 1 for count in counts.values()):
            raise ValueError('Duplicate SDK entries')
        actual = {name for name in counts if name.endswith('.class')}
        if actual != set(required):
            missing, extra = sorted(set(required)-actual), sorted(actual-set(required))
            raise ValueError(f'SDK differs from public API output; missing={missing[:8]}; extra={extra[:8]}')
        for name, expected in required.items():
            data = archive.read(name)
            version = int.from_bytes(data[6:8], 'big') if data[:4] == b'\xca\xfe\xba\xbe' else 0
            required_version = 61 if name in core else LOADERS[loader]
            if hashlib.sha256(data).hexdigest() != expected or version != required_version:
                raise ValueError(f'Stale SDK class or unexpected class version {version}: {name}')
        if any(name.startswith(('assets/', 'data/')) or name in ('META-INF/mods.toml', 'META-INF/neoforge.mods.toml')
               for name in counts):
            raise ValueError('SDK contains runtime assets or a loader descriptor')
        for name, source in [('LICENSE','LICENSE'), ('GPL-3.0.txt','docs/licenses/GPL-3.0.txt'), ('NOTICE','NOTICE')]:
            entry = 'META-INF/gregtech6/' + name
            if counts.get(entry) != 1 or archive.read(entry) != (repo/source).read_bytes():
                raise ValueError('SDK license/notice differs: ' + name)
        if any('META-INF/gregtech6/'+name in counts for name in ('COPYING.LESSER','COPYING','LICENSE.txt')):
            raise ValueError('SDK contains superseded duplicate licenses')
    return {'path':str(path.resolve()), 'loader':loader, 'bytes':path.stat().st_size,
            'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),
            'core_api_classes':len(core), 'platform_api_classes':len(platform),
            'shared_classes':len(core), 'api_closure_checked':True, 'zip_crc_checked':True,
            'compile_only':True, 'external_addon_runtime_verified':False}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jar', type=Path, required=True)
    parser.add_argument('--loader', choices=tuple(LOADERS), default='core')
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    try:
        result = inspect(args.jar, Path(__file__).resolve().parents[2], args.loader)
    except (ValueError, OSError, zipfile.BadZipFile) as error:
        parser.exit(1, f'Addon SDK packaging failed: {error}\n')
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2)+'\n', encoding='utf8')
    print(f"Addon SDK packaging passed ({args.loader}): {result['core_api_classes']} core API + "
          f"{result['platform_api_classes']} native API classes; closed and no implementation/assets")
