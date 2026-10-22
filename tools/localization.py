#!/usr/bin/env python3
"""One exact-source Chinese pipeline. Check by default; --write regenerates zh_cn.

Unknown port keys use en_us verbatim. Adding a Chinese alias requires an explicit
original key in tools/localization/aliases.json, never a guessed translation.
tools/localization/adopted.json may copy a CrucibleCraft sentence only when the
English matches exactly. Placeholder indexes may be aligned; no other character
is rewritten, and unmatched port text stays English.
"""
import argparse
import gzip
import hashlib
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
LANG_PATH = Path('core/src/main/resources/assets/gt6ce/lang')
CONFIG_PATH = Path('tools/localization')
FORMAT = re.compile(r'%(?:(\d+)\$)?([A-Za-z%]|$)')
NUMERIC_FORMAT = re.compile(r'%(\d+\$)?[\d.]*[df]')
PLACEHOLDER = re.compile(r'%(?:\d+\$)?s')
HAN = re.compile(r'[\u3400-\u9fff\uf900-\ufaff]')


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


def canonical_sentence(value):
    """Compare sentences after placeholder spelling is ignored."""
    return PLACEHOLDER.sub('%s', value)


def align_placeholders(english, copied):
    """Use the English argument markers without changing any translated word."""
    markers = PLACEHOLDER.findall(english)
    found = PLACEHOLDER.findall(copied)
    if len(markers) != len(found):
        raise ValueError('Copied translation has a different placeholder count')
    marker = iter(markers)
    return PLACEHOLDER.sub(lambda _match: next(marker), copied)


def cruciblecraft_language(repo):
    lang = (repo.parent.parent / 'cruciblecraft-template-1.21.1'
            / 'src/generated/resources/assets/cruciblecraft/lang')
    english, chinese = lang / 'en_us.json', lang / 'zh_cn.json'
    if english.is_file() and chinese.is_file():
        return read_json(english), read_json(chinese)
    return None


def load_adopted(repo):
    """Verbatim CrucibleCraft copies. Absent file means this repo has none."""
    path = repo / CONFIG_PATH / 'adopted.json'
    if not path.is_file():
        return {}
    entries = read_json(path)['entries']
    sibling = cruciblecraft_language(repo)
    resolved = {}
    for key, row in entries.items():
        if not isinstance(row, dict) or any(not isinstance(row.get(field), str) for field in
                                            ('english', 'source_key', 'source_english', 'source_value')):
            raise ValueError('Adopted entry is incomplete: ' + key)
        if canonical_sentence(row['english']) != canonical_sentence(row['source_english']):
            raise ValueError('Adopted English is not the CrucibleCraft sentence: ' + key)
        if not HAN.search(row['source_value']):
            raise ValueError('Adopted copy has no Chinese text: ' + key)
        value = align_placeholders(row['english'], row['source_value'])
        if canonical_sentence(value) != canonical_sentence(row['source_value']):
            raise ValueError('Placeholder alignment changed translated text: ' + key)
        if sibling is not None:
            source_english, source_chinese = sibling
            if (source_english.get(row['source_key']) != row['source_english']
                    or source_chinese.get(row['source_key']) != row['source_value']):
                raise ValueError('Adopted copy drifted from CrucibleCraft: ' + key)
        resolved[key] = {**row, 'value': value}
    return resolved


def check_english(repo, english, chinese, aliases, source, *, pinned=None):
    # Importers validate their proposed declarations before touching any file.
    # Normal checks still read the accepted declarations from disk.
    if pinned is None:
        pinned = read_json(repo / CONFIG_PATH / 'english_source.json')['values']
    adopted = load_adopted(repo)
    for key, row in adopted.items():
        if key in english and english[key] != row['english']:
            raise ValueError('Adopted English drifted: ' + key)
    changed = [key for key, row in pinned.items() if english.get(key) != row['value']]
    if changed:
        raise ValueError('English differs from original source declarations: ' + ', '.join(changed[:12]))
    changed_bindings = []
    english_only = 0
    for key, row in pinned.items():
        if row.get('chinese_source_missing'):
            copied = adopted[key]['value'] if key in adopted and adopted[key]['english'] == english.get(key) else None
            if copied is not None:
                if chinese.get(key) != copied:
                    changed_bindings.append(key)
            elif (row['chinese_source_missing'] is not True or key in aliases or key in source
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


def stone_rock_heading(english, source):
    """Use the patch's rock suffix for GT6's STONE naming branch, as requested.

    Keep every original whole-name entry unchanged: ore-bearing rocks still use
    those entries, while plain stone rocks have their own material-name template.
    """
    key = 'item.gt6ce.rock_gt_stone'
    if key not in english:
        return {}
    if english[key] != '%s Rock':
        raise ValueError('Stone rock template differs from the GT6 STONE naming rule')
    material = source['gt.material.GraniteBlack']
    original = source['oredict.rockGtGraniteBlack']
    if original.count(material) != 1:
        raise ValueError('Original stone rock name must contain its material exactly once')
    suffix = original.partition(material)[2]
    if not suffix or '%' in suffix:
        raise ValueError('Original stone rock suffix is missing or formatted')
    return {key: '%s' + suffix}


# Exact shared keys used by Jade providers; no general message/tooltip exemption.
JADE_SHARED_KEYS = frozenset({
    'gt.axial.state.enabled',
    'gt.axial.state.overloaded',
    'gt.axial.state.stopped',
    'gt.tooltip.axial.dynamo',
    'gt.tooltip.axial.steam',
    'gt6ce.fusion.output',
    'gt6ce.fusion.startup',
    'gt6ce.jade.rock_content',
    'gt6ce.jade.twig_content',
    'message.gt6ce.extender.disabled',
    'message.gt6ce.extender.enabled',
    'message.gt6ce.extender.mode',
    'message.gt6ce.extender.unavailable',
    'message.gt6ce.reactor.detail',
    'message.gt6ce.reactor.stopped',
})

def is_jade_project_key(key):
    return key.startswith(('jade.gt6ce.', 'config.jade.plugin_gt6ce.')) or key in JADE_SHARED_KEYS


def load_jade_project(repo=ROOT):
    """Narrow user-authorized translations of modern Jade UI, not source copies."""
    path = repo / CONFIG_PATH / 'jade_project.json'
    if not path.exists():
        return {}
    rows = read_json(path)['values']
    for key, row in rows.items():
        if not is_jade_project_key(key):
            raise ValueError('Project Jade translation outside authorized namespace: ' + key)
        if not isinstance(row, dict) or any(not isinstance(row.get(field), str) or not row[field]
                                            for field in ('english', 'value')):
            raise ValueError('Incomplete project Jade translation: ' + key)
        if format_arguments(row['english']) != format_arguments(row['value']):
            raise ValueError('Project Jade format arguments differ: ' + key)
    return rows


def expected_chinese(english, source, aliases, *, repo=ROOT):
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
    result.update(stone_rock_heading(english, source))
    for key, row in load_adopted(repo).items():
        if key not in english:
            continue
        if english[key] != row['english']:
            raise ValueError('Adopted English drifted: ' + key)
        if key in aliases or key in source:
            raise ValueError('Adopted copy overrides an original binding: ' + key)
        result[key] = row['value']
    adopted_keys = load_adopted(repo).keys()
    for key, row in load_jade_project(repo).items():
        if english.get(key) != row['english']:
            raise ValueError('Project Jade English drifted or key missing: ' + key)
        if key in aliases or key in source or key in adopted_keys:
            raise ValueError('Project Jade translation overrides source binding: ' + key)
        result[key] = row['value']
    if any(k.startswith('emi.group.gt6ce.') for k in english):
        from integration.remi_group_titles import headings
        result.update(headings(repo, result))
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
    expected = expected_chinese(english, source, aliases, repo=repo)
    derived_titles = {}
    # Viewer headings have an explicitly requested presentation-only projection:
    # use generic category headings derived from source names or the requested UI vocabulary.
    # Native item/block translations remain under the exact-source rules above.
    if any(k.startswith('emi.group.gt6ce.') for k in english):
        from integration.remi_group_titles import headings
        derived_titles = headings(repo, expected)
        expected.update(derived_titles)
        for key, value in headings(repo, english).items():
            if english.get(key) != value:
                raise ValueError('English EMI heading differs from its native source: ' + key)
    target = repo / LANG_PATH / 'zh_cn.json'
    current = read_json(target)
    delta = differences(expected, current)
    english_report = check_english(repo, english, expected, aliases, source)
    if any(delta.values()):
        if not write:
            raise ValueError('Chinese differs from exact source/aliases/English fallback: ' +
                             json.dumps({k: {'count': len(v), 'examples': v[:8]} for k, v in delta.items()}))
        target.write_text(json_text(expected), encoding='utf-8', newline='\n')
    adopted = load_adopted(repo)
    missing_adopted = sorted(key for key in adopted if key not in english)
    if missing_adopted:
        raise ValueError('Adopted key is not in English: ' + ', '.join(missing_adopted[:8]))
    rock_heading = stone_rock_heading(english, source)
    fallback = sorted(english.keys() - source.keys() - aliases.keys() - derived_titles.keys()
                      - rock_heading.keys() - adopted.keys() - load_jade_project(repo).keys())
    return {'source_sha256': meta['sha256'], 'source_keys': len(source), 'english': english_report,
            'native_aliases': len(aliases), 'chinese_keys': len(expected), 'derived_emi_headings': len(derived_titles),
            'derived_stone_rock_templates': len(rock_heading), 'adopted_cruciblecraft_copies': len(adopted),
            'project_jade_translations': len(load_jade_project(repo)),
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
