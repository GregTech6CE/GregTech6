#!/usr/bin/env python3
"""One exact-source Chinese pipeline. Check by default; --write regenerates zh_cn.

Unknown port keys use en_us verbatim. Adding a Chinese alias requires an explicit
original key in tools/localization/aliases.json, never a guessed translation.
"""
import argparse
import gzip
import hashlib
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
LANG_PATH = Path('core/src/main/resources/assets/gregtech/lang')
CONFIG_PATH = Path('tools/localization')
FORMAT = re.compile(r'%(?:(\d+)\$)?([A-Za-z%]|$)')
NUMERIC_FORMAT = re.compile(r'%(\d+\$)?[\d.]*[df]')


def format_arguments(value):
    """Minecraft Language normalizes numeric formats before TranslatableContents.

    Return referenced one-based arguments. Literal percentage text with no
    arguments is preserved by Minecraft's fallback and is not rewritten.
    """
    value = NUMERIC_FORMAT.sub(lambda m: '%' + (m[1] or '') + 's', value)
    sequential, used, errors = 0, set(), []
    cursor = 0
    for match in FORMAT.finditer(value):
        gap = value[cursor:match.start()]
        if re.search(r'%\d', gap): raise ValueError('unsupported numeric format')
        if '%' in gap: errors.append('unescaped percent')
        cursor = match.end()
        if match[0] == '%%': continue
        if match[2] != 's':
            if match[2]: raise ValueError('unsupported format ' + match[0])
            if match[1] is not None: raise ValueError('missing argument conversion')
            errors.append('unescaped trailing percent'); continue
        if match[1] is None:
            sequential += 1
            used.add(sequential)
        else:
            index = int(match[1])
            if index < 1: errors.append('argument index must be positive')
            used.add(index)
    tail = value[cursor:]
    if re.search(r'%\d', tail): raise ValueError('unsupported numeric format')
    if '%' in tail: errors.append('unescaped percent')
    if used and errors:
        raise ValueError('; '.join(errors))
    return used


def check_english(repo, english, chinese, aliases, source):
    pinned = read_json(repo / CONFIG_PATH / 'english_source.json')['values']
    changed = [key for key, row in pinned.items() if english.get(key) != row['value']]
    if changed:
        raise ValueError('English differs from original source declarations: ' + ', '.join(changed[:12]))
    changed_bindings = []
    english_only = 0
    for key, row in pinned.items():
        if row.get('chinese_source_missing'):
            if (row['chinese_source_missing'] is not True or key in aliases or key in source
                    or row['source_key'] in source or chinese.get(key) != english.get(key)):
                changed_bindings.append(key)
            english_only += 1
        elif aliases.get(key, key) != row['source_key']:
            changed_bindings.append(key)
    if changed_bindings:
        raise ValueError('English/Chinese original identities differ: ' + ', '.join(changed_bindings[:12]))
    formats = 0
    for key, value in english.items():
        if '\ufffd' in value or any(ord(c) < 32 and c not in '\n\t' for c in value):
            raise ValueError('Invalid English text encoding/control character: ' + key)
        if not value and not key.endswith('.tooltip'):
            raise ValueError('Empty English display name: ' + key)
        try:
            expected = format_arguments(value)
            actual = format_arguments(chinese[key])
        except ValueError as error:
            raise ValueError(f'Invalid language format for {key}: {error}') from error
        if expected != actual:
            raise ValueError(f'English/Chinese format arguments differ for {key}: {expected} != {actual}')
        formats += bool(expected)
    return {'keys': len(english), 'exact_original_declarations': len(pinned),
            'original_english_without_chinese_source': english_only,
            'formatted_keys': formats, 'intentional_empty_tooltips': sum(not v for v in english.values())}


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError('Duplicate JSON key: ' + key)
        result[key] = value
    return result


def read_json(path):
    return json.loads(path.read_text(encoding='utf-8-sig'), object_pairs_hook=unique_object)


def json_text(value):
    return json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + '\n'


def parse_patch(raw):
    values = {}
    for line in raw.decode('utf-8-sig').splitlines():
        match = re.fullmatch(r'\s*S:(.*?)=(.*)', line)
        if match:
            key, value = match.groups()
            key = key.strip('"')
            if key in values and values[key] != value:
                raise ValueError('Conflicting source key: ' + key)
            values[key] = value
    if not values:
        raise ValueError('No GT6 language configuration entries')
    return values


def read_patch(path):
    return parse_patch(path.read_bytes())


def load_source(repo=ROOT, external=None):
    config = repo / CONFIG_PATH
    meta = read_json(config / 'source.json')
    raw = gzip.decompress((config / 'GregTech_zh_cn.lang.gz').read_bytes())
    if hashlib.sha256(raw).hexdigest() != meta['sha256']:
        raise ValueError('Pinned GregTech.lang snapshot SHA-256 differs from source.json')
    if external is not None and external.read_bytes() != raw:
        raise ValueError('Supplied GregTech.lang differs from the pinned source; review a source update first')
    source = parse_patch(raw)
    if len(source) != meta['keys']:
        raise ValueError('Pinned source key count differs from source.json')
    return source, meta


def expected_chinese(english, source, aliases):
    for label, mapping in [('English', english), ('source', source), ('aliases', aliases)]:
        if not isinstance(mapping, dict) or any(not isinstance(k, str) or not isinstance(v, str)
                                               for k, v in mapping.items()):
            raise ValueError(label + ' must contain only string keys/values')
    chinese_in_english = [k for k, v in english.items()
                          if re.search(r'[\u3400-\u9fff\uf900-\ufaff\U00020000-\U000323af]', v)]
    if chinese_in_english:
        raise ValueError('Chinese bypasses source bindings in en_us: ' + ', '.join(chinese_in_english[:8]))
    result = {**english, **source}
    for native, original in aliases.items():
        if original not in source:
            raise ValueError(f'Unknown original key for {native}: {original}')
        if native in source and native != original:
            raise ValueError(f'Alias must not override an original key: {native}')
        result[native] = source[original]
    return result


def differences(expected, actual):
    return {
        'missing': sorted(expected.keys() - actual.keys()),
        'unexpected': sorted(actual.keys() - expected.keys()),
        'changed': sorted(k for k in expected.keys() & actual.keys() if expected[k] != actual[k]),
    }


def synchronize(repo=ROOT, *, write=False, external=None):
    source, meta = load_source(repo, external)
    aliases = read_json(repo / CONFIG_PATH / 'aliases.json')
    english = read_json(repo / LANG_PATH / 'en_us.json')
    expected = expected_chinese(english, source, aliases)
    target = repo / LANG_PATH / 'zh_cn.json'
    current = read_json(target)
    delta = differences(expected, current)
    english_report = check_english(repo, english, expected, aliases, source)
    if any(delta.values()):
        if not write:
            raise ValueError('Chinese differs from exact source/aliases/English fallback: ' +
                             json.dumps({k: {'count': len(v), 'examples': v[:8]} for k, v in delta.items()}))
        target.write_text(json_text(expected), encoding='utf-8', newline='\n')
    fallback = sorted(english.keys() - source.keys() - aliases.keys())
    return {'source_sha256': meta['sha256'], 'source_keys': len(source), 'english': english_report,
            'native_aliases': len(aliases), 'chinese_keys': len(expected),
            'english_fallback_count': len(fallback), 'english_fallback_keys': fallback,
            'changes': {k: len(v) for k, v in delta.items()}}


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--repo', type=Path, default=ROOT)
    ap.add_argument('--patch', type=Path, help='Also verify the local original file byte-for-byte')
    ap.add_argument('--write', action='store_true')
    ap.add_argument('--report', type=Path)
    ns = ap.parse_args()
    try:
        result = synchronize(ns.repo, write=ns.write, external=ns.patch)
    except (ValueError, OSError) as error:
        ap.exit(1, f'{error}\n')
    if ns.report:
        ns.report.parent.mkdir(parents=True, exist_ok=True)
        ns.report.write_text(json_text(result), encoding='utf-8')
    print(json.dumps({k: v for k, v in result.items() if k != 'english_fallback_keys'}))


if __name__ == '__main__':
    main()
