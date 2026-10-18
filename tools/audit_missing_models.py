"""Finds model references that point at files the mod does not ship.

The client loader resolves every `model` / `parent` reference in a blockstate or model JSON by asking
the resource manager for that file; a miss throws `FileNotFoundException` and the vanilla
`ModelBakery` logs the whole stack for **each** occurrence. A single missing iconset model therefore
costs ~10 log lines *per blockstate variant that references it* - the 2026-09-18 crash report
contained 59,192 of those exceptions (~590k log lines, a 99 MB `latest.log` for an 8 minute session),
which is both a log flood and a lot of allocation churn at startup.

This tool walks every JSON under `assets/gt6ce/` and checks each gregtech-namespaced reference:

  * `blockstates/*.json`        -> `variants.*.model`, `multipart[].apply.model`
  * `models/**/*.json`          -> `parent`, `textures.*` values that are model paths? (no - textures
                                   are separate) and `overrides[].model`

References to namespaces other than `gregtech` and to vanilla paths are resolved against
`assets/minecraft` only when the file exists in this repository (vanilla assets are not part of it, so
they are reported separately as "external" instead of missing).

Usage:
  python tools/audit_missing_models.py            # report
  python tools/audit_missing_models.py --quiet    # only the summary line
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import argparse
import collections
import io
import json
import os
import re
import sys

ASSETS = os.path.join(str(native_path('src')), 'main', 'resources', 'assets')
# Forward slashes on purpose: `rel` below is normalised with `replace('\\', '/')`, so an
# os.path.join() built prefix never matches on Windows and silently skipped every model file.
MODEL_DIRS = ('blockstates', 'models/block', 'models/item')


def model_path_exists(reference):
    """Whether `namespace:path` names a model file this repository ships.

    A reference without a namespace means `minecraft:` (vanilla), not `gt6ce:`.
    """
    if ':' in reference:
        namespace, path = reference.split(':', 1)
    else:
        namespace, path = 'minecraft', reference
    candidate = os.path.join(ASSETS, namespace, 'models', path + '.json')
    return os.path.exists(candidate)


def collect_references(node, out):
    """Every `model` / `parent` string in a JSON tree."""
    if isinstance(node, dict):
        for key, value in node.items():
            if key in ('model', 'parent') and isinstance(value, str):
                out.append(value)
            else:
                collect_references(value, out)
    elif isinstance(node, list):
        for item in node:
            collect_references(item, out)


def iter_json_files(root):
    """Every `models/**` / `blockstates/**` JSON below `root`, as (full path, relative path)."""
    for dirpath, _dirs, files in os.walk(root):
        for name in files:
            if not name.endswith('.json'):
                continue
            full = os.path.join(dirpath, name)
            rel = os.path.relpath(full, root).replace('\\', '/')
            if rel.startswith(MODEL_DIRS):
                yield full, rel


def strip_boms(root):
    """Remove a leading UTF-8 byte-order mark from every JSON file; return how many changed."""
    fixed = 0
    for full, rel in iter_json_files(root):
        raw = open(full, 'rb').read()
        if raw.startswith(b'\xef\xbb\xbf'):
            open(full, 'wb').write(raw[3:])
            print('  stripped %s' % rel)
            fixed += 1
    print('stripped %d byte-order marks' % fixed)
    return 0


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--quiet', action='store_true')
    parser.add_argument('--fix-bom', action='store_true',
                        help='strip leading byte-order marks from JSON files instead of reporting')
    args = parser.parse_args()

    gregtech_dir = os.path.join(ASSETS, 'gt6ce')
    if args.fix_bom:
        return strip_boms(gregtech_dir)
    missing = collections.Counter()
    missing_sources = collections.defaultdict(set)
    external = collections.Counter()
    bom_files = []
    checked_files = 0
    checked_refs = 0

    for root, _dirs, files in os.walk(gregtech_dir):
        for name in files:
            if not name.endswith('.json'):
                continue
            full = os.path.join(root, name)
            rel = os.path.relpath(full, gregtech_dir).replace('\\', '/')
            if not rel.startswith(MODEL_DIRS):
                continue
            checked_files += 1
            raw = open(full, 'rb').read()
            if raw.startswith(b'\xef\xbb\xbf'):
                # A byte-order mark is not whitespace: strict JSON readers reject the file.
                bom_files.append(rel)
            try:
                data = json.loads(raw.decode('utf-8-sig'))
            except Exception as error:                     # noqa: BLE001 - report and continue
                missing['<unreadable: %s>' % error] += 1
                missing_sources['<unreadable: %s>' % error].add(rel)
                continue
            refs = []
            collect_references(data, refs)
            for reference in refs:
                checked_refs += 1
                namespace = reference.split(':', 1)[0] if ':' in reference else 'minecraft'
                if not model_path_exists(reference):
                    if namespace != 'gt6ce':
                        external[reference] += 1
                    else:
                        missing[reference] += 1
                        missing_sources[reference].add(rel)

    print('%d files, %d references checked' % (checked_files, checked_refs))
    if not args.quiet:
        if missing:
            print()
            print('=== missing gregtech models (%d distinct) ===' % len(missing))
            grouped = collections.Counter()
            for reference, count in missing.items():
                grouped[re.sub(r'/[^/]+$', '/', reference)] += 1
            for folder, count in grouped.most_common():
                print('  %-46s %d distinct' % (folder, count))
            print()
            for reference, count in missing.most_common(30):
                sources = sorted(missing_sources[reference])[:3]
                print('  %-58s referenced by %d file(s) e.g. %s'
                      % (reference, count, ', '.join(sources)))
        if external:
            print()
            print('=== references into other namespaces (resolved at runtime, not checked) ===')
            for reference, count in external.most_common(10):
                print('  %-58s %d' % (reference, count))
    print()
    if bom_files:
        print('=== JSON files that start with a byte-order mark (%d) ===' % len(bom_files))
        for rel in bom_files[:10]:
            print('  ' + rel)
        print('  (strip them: python tools/audit_missing_models.py --fix-bom)')
        print()
    print('missing gregtech model files: %d' % len(missing))
    print('files with a BOM: %d' % len(bom_files))
    return 1 if (missing or bom_files) else 0


if __name__ == '__main__':
    sys.exit(main())
