"""Inventory source scope for the GT6 parity review; counts are not parity claims.

Run with an explicit original source root. Only authored source trees are read;
Stonecutter outputs, build products and preserved snapshots inside this repo are excluded.
"""
from pathlib import Path
from collections import Counter
import argparse
import hashlib
import json
import re

ROOT = Path(__file__).resolve().parents[2]


def collect(roots, relative_to):
    rows = []
    for root in roots:
        for path in sorted(root.rglob('*.java')):
            raw = path.read_bytes()
            text = raw.decode('utf-8-sig')
            rows.append({'path': path.relative_to(relative_to).as_posix(),
                         'sha256': hashlib.sha256(raw).hexdigest(),
                         'lines': len(text.splitlines())})
    return rows


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--original', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    original = args.original.resolve()
    if not (original / 'gregtech').is_dir() or not (original / 'gregapi').is_dir():
        parser.error('--original must be the original src/main/java containing gregtech and gregapi')
    roots = [ROOT / 'core/src/main/java', ROOT / 'core/src/api/java',
             ROOT / 'multiversion/src/main/java', ROOT / 'multiversion/src/api/java']
    for node in ('1.20.1-forge', '1.21.1-neoforge'):
        roots.extend(ROOT / f'multiversion/versions/{node}/src/{s}/java' for s in ('main', 'api'))
    port = collect(roots, ROOT)
    upstream = collect([original], original)
    original_groups = Counter('/'.join(row['path'].split('/')[:2]) for row in upstream)
    package_groups = Counter(row['path'].split('/com/gregtech/gregtech/')[-1].split('/')[0]
                             for row in port)
    evidence = {'schema': 1, 'original_root': str(original),
                'meaning': 'File inventory only. No file is declared behaviorally aligned by name or count.',
                'original_files': upstream, 'port_files': port,
                'original_groups': dict(sorted(original_groups.items())),
                'port_groups': dict(sorted(package_groups.items())),
                'largest_port_sources': sorted(port, key=lambda row: -row['lines'])[:25],
                'opaque_paths': [r['path'] for r in port if re.search(r'/(?:loader|loaders|[abc])/',r['path'])]}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(evidence, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    print(json.dumps({'original_files': len(upstream), 'port_files': len(port),
                      'opaque_paths': evidence['opaque_paths'], 'original_groups': evidence['original_groups'],
                      'largest_port_sources': evidence['largest_port_sources'][:5]},ensure_ascii=False))


if __name__ == '__main__':
    main()
