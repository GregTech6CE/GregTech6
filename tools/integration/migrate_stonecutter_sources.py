"""One-time, audited relocation of every native Java source into Stonecutter.

Shared files use version conditions; genuinely version-exclusive classes use node
source overrides. No business code or literal may change. Nested comment escaping
follows Stitcher's documented /^ ... ^/ representation (Kikugie, LGPL-3.0).
"""
from pathlib import Path
import argparse
import bisect
import difflib
import hashlib
import json
import re

from check_source import JAVA_TOKEN
from check_stonecutter_emi import signature

ROOT = Path(__file__).resolve().parents[2]
NODES = {"forge": "1.20.1-forge", "neoforge": "1.21.1-neoforge"}
PREFIXES = {"forge": ROOT / "src", "neoforge": ROOT / "neoforge/src"}
SUPERSCRIPTS = "⁰¹²³⁴⁵⁶⁷⁸⁹"
ESCAPES = re.compile(r"/\^([⁰¹²³⁴⁵⁶⁷⁸⁹]?)|([⁰¹²³⁴⁵⁶⁷⁸⁹]?)\^/|/\*|\*/")


def digest(value):
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def semantic_digest(value):
    return digest(json.dumps(signature(value), ensure_ascii=False, separators=(',', ':')))


def inactive(value):
    def escape(match):
        if match[0] == "/*": return "/^"
        if match[0] == "*/": return "^/"
        existing = match[1] if match[0].startswith('/') else match[2]
        depth = SUPERSCRIPTS.index(existing) if existing else 0
        if depth == 9: raise ValueError("Nested source comment depth exceeds Stitcher limit")
        return "/^" + SUPERSCRIPTS[depth + 1] if match[0].startswith('/') else SUPERSCRIPTS[depth + 1] + "^/"
    return ESCAPES.sub(escape, value)


def safe_boundaries(text):
    offsets = [0] + [m.end() for m in re.finditer('\n', text)]
    if offsets[-1] != len(text): offsets.append(len(text))
    unsafe = set()
    for token in JAVA_TOKEN.finditer(text):
        if '\n' not in token[0]: continue
        lo = bisect.bisect_right(offsets, token.start())
        hi = bisect.bisect_left(offsets, token.end())
        unsafe.update(range(lo, hi))
    return set(range(len(offsets))) - unsafe


def merge(forge, neo):
    if forge == neo: return forge, 0
    a, b = forge.splitlines(keepends=True), neo.splitlines(keepends=True)
    sa, sb = safe_boundaries(forge), safe_boundaries(neo)
    ops = difflib.SequenceMatcher(None, a, b, autojunk=False).get_opcodes()
    anchors = {(0, 0), (len(a), len(b))}
    for kind, x1, x2, y1, y2 in ops:
        if kind == 'equal':
            anchors.update((x1 + n, y1 + n) for n in range(x2 - x1 + 1)
                           if x1 + n in sa and y1 + n in sb)
    anchors = sorted(anchors)
    intervals = []
    for kind, x1, x2, y1, y2 in ops:
        if kind == 'equal': continue
        left = max(p for p in anchors if p[0] <= x1 and p[1] <= y1)
        right = min(p for p in anchors if p[0] >= x2 and p[1] >= y2)
        if intervals and left[0] <= intervals[-1][1][0] and left[1] <= intervals[-1][1][1]:
            intervals[-1] = intervals[-1][0], max(right, intervals[-1][1])
        else: intervals.append((left, right))
    result, cursor, regions = [], 0, 0
    for (x1, y1), (x2, y2) in intervals:
        result.extend(a[cursor:x1])
        older, newer = ''.join(a[x1:x2]), ''.join(b[y1:y2])
        if older and not older.endswith('\n'): older += '\n'
        if newer and not newer.endswith('\n'): newer += '\n'
        # A branch containing only ordinary comments is mistaken for disabled
        # code by Stitcher. Preserve those comments outside the condition.
        if signature(older) == signature(newer):
            result.append(older)
            result.extend('\n' + token[0] + '\n' for token in JAVA_TOKEN.finditer(newer)
                          if token[0].startswith(('//', '/*')) and token[0] not in older)
            cursor = x2
            continue
        if signature(older) == ('', []):
            result.append(older)
            older = ''
        if signature(newer) == ('', []):
            result.append(newer)
            newer = ''
        # A ternary '?' at the start of an inactive comment is parsed as a
        # Stonecutter directive. A normal Java comment disambiguates either leg.
        if older.lstrip().startswith(('?', '$')): older = '// Version-specific branch.\n' + older
        if newer.lstrip().startswith(('?', '$')): newer = '// Version-specific branch.\n' + newer
        result.append('//? if >=1.21.1 {\n/*' + inactive(newer) + '*///?} else {\n' + older + '//?}\n')
        regions += 1
        cursor = x2
    result.extend(a[cursor:])
    merged = ''.join(result)
    if signature(merged) != signature(forge): raise ValueError("Active Forge code changed while merging")
    return merged, regions


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    if (ROOT / 'multiversion/migration-manifest.json').exists():
        raise SystemExit('Java migration manifest already exists; refusing to replace its audited baseline')
    changes, records, originals = {}, [], []
    sets = sorted({p.name for prefix in PREFIXES.values() for p in prefix.iterdir()
                   if p.is_dir() and (p / 'java').exists()})
    for source_set in sets:
        roots = {platform: prefix / source_set / 'java' for platform, prefix in PREFIXES.items()}
        files = {platform: {p.relative_to(base).as_posix(): p for p in base.rglob('*.java')}
                 for platform, base in roots.items()}
        for relative in sorted(set(files['forge']) | set(files['neoforge'])):
            sources = {platform: paths[relative].read_text(encoding='utf-8')
                       for platform, paths in files.items() if relative in paths}
            regions = 0
            if len(sources) == 2:
                content, regions = merge(sources['forge'], sources['neoforge'])
                target = ROOT / f'multiversion/src/{source_set}/java/{relative}'
                kind = 'conditional' if regions else 'shared'
            else:
                platform, content = next(iter(sources.items()))
                target = ROOT / f'multiversion/versions/{NODES[platform]}/src/{source_set}/java/{relative}'
                kind = platform + '_only'
            if target.exists(): raise ValueError(f"Refusing to overwrite {target}")
            changes[target] = content
            records.append({'source_set': source_set, 'path': relative, 'kind': kind,
                            'conditional_regions': regions,
                            'source_sha256': {p: digest(s) for p, s in sources.items()},
                            'native_signature': {p: semantic_digest(s) for p, s in sources.items()}})
            originals.extend(paths[relative] for paths in files.values() if relative in paths)
    old = json.loads((ROOT / 'multiversion/emi-migration.json').read_text())
    for row in old['classes']:
        records.append({'source_set': 'main', 'path': 'gregtech/emi/' + row['class'] + '.java',
                        'kind': 'conditional' if row['conditional_regions'] else 'shared',
                        'conditional_regions': row['conditional_regions'], 'source_sha256': row['baseline'],
                        'native_signature': row['native_signature'], 'prior_baseline': old['baseline_commit']})
    manifest = {'baseline_commit': '4e39348c6', 'nodes': NODES, 'source_sets': sets, 'java': records}
    print(json.dumps({'logical_java_sources': len(records), 'old_native_files': len(originals),
                      'conditional_regions': sum(r['conditional_regions'] for r in records),
                      'kinds': {kind: sum(r['kind'] == kind for r in records)
                                for kind in ['shared', 'conditional', 'forge_only', 'neoforge_only']}}), flush=True)
    if not args.apply: return
    for path, value in changes.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(value, encoding='utf-8')
    for path in originals: path.unlink()
    (ROOT / 'multiversion/migration-manifest.json').write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__': main()
