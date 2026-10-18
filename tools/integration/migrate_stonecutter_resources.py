"""Move all native resources to shared Stonecutter input and genuine node overrides."""
from pathlib import Path
import hashlib
import json

ROOT = Path(__file__).resolve().parents[2]
NODES = {'forge': '1.20.1-forge', 'neoforge': '1.21.1-neoforge'}
PREFIXES = {'forge': ROOT / 'src', 'neoforge': ROOT / 'neoforge/src'}


def fingerprint(data, mode):
    if mode == 'json':
        data = json.dumps(json.loads(data), sort_keys=True, ensure_ascii=False, separators=(',', ':')).encode()
    return hashlib.sha256(data).hexdigest()


def main():
    manifest_path = ROOT / 'multiversion/migration-manifest.json'
    manifest = json.loads(manifest_path.read_text())
    if 'resources' in manifest: raise SystemExit('Refusing to replace an existing resource baseline')
    rows, writes, removals = [], {}, []
    source_sets = sorted({p.name for prefix in PREFIXES.values() for p in prefix.iterdir()
                          if p.is_dir() and (p / 'resources').is_dir()})
    maximum_loaders = 0
    for source_set in source_sets:
        roots = {p: prefix / source_set / 'resources' for p, prefix in PREFIXES.items()}
        files = {p: {file.relative_to(base).as_posix(): file for file in base.rglob('*') if file.is_file()}
                 for p, base in roots.items()}
        for relative in sorted(set(files['forge']) | set(files['neoforge'])):
            blobs = {p: paths[relative].read_bytes() for p, paths in files.items() if relative in paths}
            mode = 'json' if relative.endswith(('.json', '.mcmeta')) else 'bytes'
            signatures = {p: fingerprint(data, mode) for p, data in blobs.items()}
            if len(blobs) == 2:
                target = ROOT / f'multiversion/src/{source_set}/resources/{relative}'
                writes[target] = blobs['forge']
                converted = blobs['forge'].replace(b'forge:composite', b'neoforge:composite')
                # A replacement is shared only if its complete JSON matches the old Neo resource.
                if signatures['forge'] == signatures['neoforge']:
                    kind = 'shared'
                elif mode == 'json' and fingerprint(converted, mode) == signatures['neoforge']:
                    kind = 'loader'
                    maximum_loaders = max(maximum_loaders, blobs['forge'].count(b'forge:composite'))
                else:
                    kind = 'node_override'
                    writes[ROOT / f'multiversion/versions/{NODES["neoforge"]}/src/{source_set}/resources/{relative}'] = blobs['neoforge']
            else:
                platform, content = next(iter(blobs.items()))
                kind = platform + '_only'
                writes[ROOT / f'multiversion/versions/{NODES[platform]}/src/{source_set}/resources/{relative}'] = content
            rows.append({'source_set': source_set, 'path': relative, 'kind': kind, 'format': mode,
                         'native_signature': signatures,
                         'source_sha256': {p: hashlib.sha256(data).hexdigest() for p, data in blobs.items()}})
            removals.extend(paths[relative] for paths in files.values() if relative in paths)
    for path, data in writes.items():
        if path.exists(): raise ValueError(f'Refusing to overwrite {path}')
    for path, data in writes.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    for path in removals: path.unlink()
    manifest['resource_source_sets'] = source_sets
    manifest['max_composite_loaders_per_file'] = maximum_loaders
    manifest['resources'] = rows
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'logical_resources': len(rows), 'old_files': len(removals),
                      'max_composite_loaders_per_file': maximum_loaders,
                      'kinds': {kind: sum(r['kind'] == kind for r in rows)
                                for kind in sorted({r['kind'] for r in rows})}}))


if __name__ == '__main__': main()
