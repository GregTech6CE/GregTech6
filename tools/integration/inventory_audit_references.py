"""Find explicit original-source references in audit receipts; this is not a parity score.

Consume an inventory made by inventory_alignment_scope.py. Audit receipts have several
historical schemas, including paths as dictionary keys and normalized-text hashes.
Only a path with a matching source hash is accepted. Referenced files may still have
unreviewed behavior; absent references may simply live in other evidence formats.
"""
from collections import defaultdict
from pathlib import Path
import argparse
import hashlib
import json
import re

ROOT = Path(__file__).resolve().parents[2]
HASH_FIELDS = ('sha256', 'source_sha256', 'raw_sha256', 'sha256_at_archive', 'observed_sha256')


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def original_path(value, original_files):
    if not isinstance(value, str) or not value.endswith('.java'):
        return None
    relative = value.replace('\\', '/').split('src/main/java/')[-1]
    return relative if relative in original_files else None


def inspect_receipt(value, original_files, source_root):
    """Return accepted paths and nonmatching records, without guessing from basenames."""
    accepted, nonmatching = set(), set()

    def inspect(record, keyed_path=None):
        if isinstance(record, dict):
            paths = {original_path(path, original_files)
                     for path in [keyed_path, *record.values()] if isinstance(path, str)} - {None}
            digests = {record[key] for key in HASH_FIELDS
                       if isinstance(record.get(key), str) and re.fullmatch('[0-9a-fA-F]{64}', record[key])}
            for path in paths:
                raw_digest = original_files[path]['sha256']
                normalized_digest = sha((source_root / path).read_bytes().replace(b'\r\n', b'\n'))
                if digests & {raw_digest, normalized_digest}:
                    accepted.add(path)
                elif digests:
                    nonmatching.add(path)
            for key, child in record.items():
                inspect(child, key)
        elif isinstance(record, list):
            for child in record:
                inspect(child)

    inspect(value)
    return accepted, nonmatching


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--inventory', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    inventory = json.loads(args.inventory.read_text(encoding='utf-8'))
    source_root = Path(inventory['original_root'])
    original_files = {row['path']: row for row in inventory['original_files']}
    references = defaultdict(set)
    receipts, nonmatching = [], {}
    for path in sorted((ROOT / 'docs/integration/verification').glob('*audit*.json')):
        raw = path.read_bytes()
        relative = path.relative_to(ROOT).as_posix()
        accepted, historical = inspect_receipt(json.loads(raw.decode('utf-8-sig')), original_files, source_root)
        for source in accepted:
            references[source].add(relative)
        receipts.append({'path': relative, 'sha256': sha(raw), 'accepted_original_paths': len(accepted)})
        if historical:
            nonmatching[relative] = sorted(historical)

    loader = source_root / 'gregtech/loaders/b/Loader_MultiTileEntities.java'
    registered_classes = set(re.findall(r'aRegistry\.add\([^;\n]*?,\s*(\w+)\.class', loader.read_text(encoding='utf-8-sig')))
    candidates = [row for path, row in sorted(original_files.items())
                  if Path(path).stem in registered_classes and path not in references]
    report = {
        'schema': 1,
        'meaning': 'Explicit hash references only, not proof of review completeness, implementation, alignment or a completion percentage.',
        'receipt_scope': 'docs/integration/verification/*audit*.json; other receipt names and prose are not scanned.',
        'inventory_sha256': sha(args.inventory.read_bytes()),
        'original_root': str(source_root),
        'original_files': len(original_files),
        'port_files': len(inventory['port_files']),
        'original_inventory_rows_sha256': sha(json.dumps(inventory['original_files'], sort_keys=True).encode()),
        'port_inventory_rows_sha256': sha(json.dumps(inventory['port_files'], sort_keys=True).encode()),
        'port_groups': inventory['port_groups'],
        'opaque_paths': inventory['opaque_paths'],
        'receipts': receipts,
        'exact_original_references': {path: {'sha256': original_files[path]['sha256'], 'receipts': sorted(names)}
                                      for path, names in sorted(references.items())},
        'registered_class_literal_count': len(registered_classes),
        'unreferenced_registered_candidates': candidates,
        'nonmatching_historical_records': nonmatching,
        'limits': ['Candidates use class literals from the original registration loader, not a complete inheritance or conditional-registration graph.',
                   'Historical hash differences are not classified as source regressions.']}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'receipt_count': len(receipts), 'explicit_original_references': len(references),
                      'unreferenced_registered_candidates': len(candidates), 'port_files': report['port_files'],
                      'opaque_paths': report['opaque_paths']}))


if __name__ == '__main__':
    main()
