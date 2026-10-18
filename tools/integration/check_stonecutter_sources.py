"""Validate complete native source-set generation without accepting a narrower subset."""
from pathlib import Path
import argparse
import hashlib
import json
import sys

from check_stonecutter_emi import signature

ROOT = Path(__file__).resolve().parents[2]


def digest(text):
    value = json.dumps(signature(text), ensure_ascii=False, separators=(',', ':')).encode()
    return hashlib.sha256(value).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--baseline', action='store_true', help='Also prove zero gameplay/resource changes during migration')
    args = parser.parse_args()
    manifest = json.loads((ROOT / 'multiversion/migration-manifest.json').read_text())
    updates_file = ROOT / 'multiversion/migration-test-updates.json'
    updates = json.loads(updates_file.read_text())['updates'] if updates_file.exists() else []
    approved = {(r['source_set'], r['path']): r for r in updates}
    if any(r['source_set'] != 'productionSmoke' for r in updates):
        raise ValueError('Migration updates may only change independent production probes, never mod sources/resources')
    updated_outputs = 0
    failures, summary = [], {}
    for platform, node in manifest['nodes'].items():
        summary[platform] = {}
        native = ROOT / ('src' if platform == 'forge' else 'neoforge/src')
        for source_set in manifest['source_sets']:
            expected = {r['path']: r for r in manifest['java']
                        if r['source_set'] == source_set and platform in r['native_signature']}
            generated = ROOT / f'multiversion/versions/{node}/build/generated/stonecutter/{source_set}/java'
            overrides = ROOT / f'multiversion/versions/{node}/src/{source_set}/java'
            if not args.baseline:
                common = ROOT / f'multiversion/src/{source_set}/java'
                expected = {p.relative_to(base).as_posix(): None for base in (common, overrides)
                            for p in base.rglob('*.java')}
            actual = {}
            for base in [generated, overrides]:
                for file in base.rglob('*.java'):
                    path = file.relative_to(base).as_posix()
                    if path in actual: failures.append(f'duplicate {platform}/{source_set}/{path}')
                    actual[path] = file
            if set(actual) != set(expected):
                failures.extend(f'missing {platform}/{source_set}/{p}' for p in set(expected) - set(actual))
                failures.extend(f'extra {platform}/{source_set}/{p}' for p in set(actual) - set(expected))
            for path in set(actual) & set(expected):
                text = actual[path].read_text(encoding='utf-8')
                if args.baseline and digest(text) != expected[path]['native_signature'][platform]:
                    update = approved.get((source_set, path), {})
                    if (update.get('old_signature', {}).get(platform) == expected[path]['native_signature'][platform]
                            and update.get('new_signature', {}).get(platform) == digest(text)):
                        updated_outputs += 1
                    else:
                        failures.append(f'changed code/literals {platform}/{source_set}/{path}')
            if list((native / source_set / 'java').rglob('*.java')):
                failures.append(f'old independently maintained source set remains: {native}/{source_set}')
            summary[platform][source_set] = len(actual)
    resource_summary = {}
    for platform, node in manifest['nodes'].items():
        resource_summary[platform] = {}
        native = ROOT / ('src' if platform == 'forge' else 'neoforge/src')
        for source_set in manifest.get('resource_source_sets', []):
            expected = {r['path']: r for r in manifest['resources']
                        if r['source_set'] == source_set and platform in r['native_signature']}
            if not args.baseline:
                roots = [ROOT / f'multiversion/src/{source_set}/resources',
                         ROOT / f'multiversion/versions/{node}/src/{source_set}/resources']
                expected = {p.relative_to(base).as_posix(): None for base in roots for p in base.rglob('*') if p.is_file()}
            actual = {}
            for base in [ROOT / f'multiversion/versions/{node}/build/generated/stonecutter/{source_set}/resources',
                         ROOT / f'multiversion/versions/{node}/src/{source_set}/resources']:
                for file in base.rglob('*'):
                    if not file.is_file(): continue
                    path = file.relative_to(base).as_posix()
                    if path in actual: failures.append(f'duplicate resource {platform}/{source_set}/{path}')
                    actual[path] = file
            failures.extend(f'missing resource {platform}/{source_set}/{p}' for p in set(expected) - set(actual))
            failures.extend(f'extra resource {platform}/{source_set}/{p}' for p in set(actual) - set(expected))
            for path in set(actual) & set(expected):
                if not args.baseline: continue
                data = actual[path].read_bytes()
                if expected[path]['format'] == 'json':
                    data = json.dumps(json.loads(data), sort_keys=True, ensure_ascii=False, separators=(',', ':')).encode()
                if hashlib.sha256(data).hexdigest() != expected[path]['native_signature'][platform]:
                    failures.append(f'changed resource {platform}/{source_set}/{path}')
            if any(p.is_file() for p in (native / source_set / 'resources').rglob('*')):
                failures.append(f'old resource tree remains: {native}/{source_set}')
            resource_summary[platform][source_set] = len(actual)
    # The adapter used by text-based tools must not change any generated Java code/literal.
    sys.path.insert(0, str(ROOT / 'tools'))
    from native_layout import prepare, native_inputs
    view = prepare(ROOT)
    projection_count = 0
    for platform, source_set, kind, name, file in native_inputs(ROOT):
        if kind != 'java': continue
        projected = view / f'{platform}/src/{source_set}/java/{name}'
        if digest(projected.read_text(encoding='utf-8')) != digest(file.read_text(encoding='utf-8')):
            failures.append(f'tooling projection changed {platform}/{source_set}/{name}')
        projection_count += 1
    report = {'status': 'failed' if failures else 'passed', 'baseline_commit': manifest['baseline_commit'] if args.baseline else None,
              'mode': 'migration-baseline' if args.baseline else 'current-inventory', 'tooling_java_checked': projection_count,
              'approved_test_output_updates': updated_outputs,
              'source_sets': summary, 'resources': resource_summary, 'failures': failures}
    output = ROOT / ('build/reports/stonecutter/native-source-parity.json' if args.baseline else 'build/reports/stonecutter/native-source-inventory.json')
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'status': report['status'], 'source_sets': summary,
                      'approved_test_output_updates': updated_outputs,
                      'failure_count': len(failures), 'first_failures': failures[:12]}))
    if failures: sys.exit(1)


if __name__ == '__main__': main()
