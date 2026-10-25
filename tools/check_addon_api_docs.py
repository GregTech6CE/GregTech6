#!/usr/bin/env python3
"""Check documented SDK references, bilingual sections, anchors and limit sources.

This is a source checker, not a Java compiler. Fenced examples are compiled by
compileAddonSample where represented there. Shorthand call receivers and external
types are explicitly classified in addon_api_docs_contract.json.
"""
from __future__ import annotations

import argparse
from collections import Counter
from functools import lru_cache
import json
from pathlib import Path
import re
import sys
from urllib.parse import unquote

ROOT = Path(__file__).resolve().parents[1]
CONTRACT = Path(__file__).with_name('addon_api_docs_contract.json')
DOCS = ('docs/ADDON_API.md', 'docs/ADDON_API.zh-CN.md')


def prose(text):
    # Preserve line numbers, including for backtick or tilde fences.
    lines, fence = [], None
    for line in text.splitlines():
        marker = re.match(r'^\s*(`{3,}|~{3,})', line)
        if marker:
            if fence is None:
                fence = marker[1][0]
            elif marker[1][0] == fence:
                fence = None
            lines.append('')
        else:
            lines.append('' if fence else line)
    return '\n'.join(lines)


def slug(title):
    title = re.sub(r'<[^>]*>', '', title).lower().replace('`', '')
    return re.sub(r'[^\w\-\s]', '', title).replace(' ', '-')


def anchors(text):
    result, used = set(), Counter()
    for _, title, *_ in sections(text):
        base = slug(title)
        result.add(base + (f'-{used[base]}' if used[base] else ''))
        used[base] += 1
    result.update(re.findall(r'<a\s+id="([^"]+)"\s*></a>', text))
    return result


def sections(text):
    result = []
    for line in prose(text).splitlines():
        heading = re.match(r'^(#{1,6}) (.+)', line)
        if heading:
            result.append([len(heading[1]), heading[2], 0, []])
        elif result and line.startswith('|'):
            cells = len(re.split(r'(?<!\\)\|', line)) - 2
            if not result[-1][3] or result[-1][3][-1][1] is False:
                result[-1][3].append([cells, True, 1])
            else:
                result[-1][3][-1][2] += 1
        elif result and result[-1][3]:
            result[-1][3][-1][1] = False
    # Count fences under the nearest heading independently of table parsing.
    index, inside = -1, False
    for line in text.splitlines():
        if re.match(r'^\s*(`{3,}|~{3,})', line):
            if not inside and index >= 0:
                result[index][2] += 1
            inside = not inside
        elif not inside and re.match(r'^#{1,6} ', line):
            index += 1
    return result


@lru_cache(maxsize=4)
def java_index(repo):
    index = {}
    for root in ('core/src/api/java', 'multiversion/src/api/java'):
        for path in sorted((repo / root).rglob('*.java')):
            source = path.read_text(encoding='utf-8')
            clean = re.sub(r'/\*.*?\*/|//[^\n]*|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'',
                           lambda m: re.sub(r'[^\n]', ' ', m[0]), source, flags=re.S)
            depths, depth, stack, ends = [], 0, [], {}
            for offset, char in enumerate(clean):
                depths.append(depth)
                depth += (char == '{') - (char == '}')
                if char == '{':
                    stack.append(offset)
                elif char == '}' and stack:
                    ends[stack.pop()] = offset
            declarations = []
            for declaration in re.finditer(r'\b(class|interface|record|enum)\s+(\w+)\b', clean):
                body = clean.find('{', declaration.end())
                if body < 0:
                    continue
                end = ends.get(body, len(clean))
                parent = next((d for d in reversed(declarations) if d[1] < body < d[2]), None)
                name = (parent[0] + '.' if parent else '') + declaration[2]
                declarations.append((name, body, end))
                names, methods = set(), {}
                if declaration[1] == 'record':
                    header = clean[declaration.end():body]
                    components = re.search(r'\((.*?)\)', re.sub(r'@\w+', '', header), re.S)
                    if components:
                        for component in components[1].split(','):
                            member = component.split()[-1]
                            names.add(member)
                            methods[member] = [[]]
                if declaration[1] == 'enum':
                    enum_header = clean[body + 1:end].split(';', 1)[0]
                    names.update(re.findall(r'\b[A-Z][A-Z_0-9]*\b', enum_header))
                    names.update(('values', 'valueOf'))
                index[name] = {'names': names, 'methods': methods, 'source': source, 'path': path,
                               'body': body, 'end': end, 'depth': depths[body] + 1}
            # Declarations at the immediate class depth only: a sibling builder or
            # a method invocation in a body must not satisfy another type's member.
            pattern = r'(?m)^[ \t]*(?:@\w+[ \t]+)*(?:(?:public|protected|private|static|default|final|abstract|synchronized)[ \t]+)*(?:<[\w, ?]+>[ \t]+)?[\w.$<>?, \[\]\t]+[ \t]+(\w+)[ \t]*\(([^()]*)\)[ \t]*(?:throws[\w., ]+)?[;{]'
            for match in re.finditer(pattern, clean):
                owner = next((index[name] for name, body, end in reversed(declarations)
                              if body < match.start(1) < end and depths[match.start(1)] == index[name]['depth']), None)
                if owner is None:
                    continue
                params = []
                for param in match[2].split(','):
                    param = re.sub(r'@\w+\s*', '', param).strip()
                    if param:
                        params.append(param.split()[-2:])
                owner['names'].add(match[1])
                owner['methods'].setdefault(match[1], []).append(params)
            for match in re.finditer(r'\b(?:public\s+)?(?:static\s+)?(?:final\s+)?[\w.<>?\[\]]+\s+(\w+)\s*=', clean):
                for name, body, end in reversed(declarations):
                    entry = index[name]
                    if body < match.start(1) < end and depths[match.start(1)] == entry['depth']:
                        entry['names'].add(match[1])
                        break
    # Unqualified nested types are used in prose only where unambiguous.
    aliases = {}
    for name, entry in index.items():
        if '.' in name:
            aliases.setdefault(name.rsplit('.', 1)[1], []).append(entry)
    for name, entries in aliases.items():
        if name not in index and len(entries) == 1:
            index[name] = entries[0]
    return index


def references(text):
    for line_no, line in enumerate(prose(text).splitlines(), 1):
        for match in re.finditer(r'(?<!`)`([^`\n]+)`(?!`)', line):
            yield line_no, match[1]


def reference_errors(text, index, contract):
    errors = []
    externals = contract['external_types']
    ignored = contract['non_api_tokens']
    internal = contract['internal_types']
    for line, token in references(text):
        if token in ignored:
            continue
        # Qualified Java names are accepted in service resource paths elsewhere.
        value = token.removeprefix('gregtech.api.')
        if value != token:
            value = re.sub(r'^(?:\w+\.)*?([A-Z])', r'\1', value)
        match = re.fullmatch(r'([A-Z][\w]*(?:\.[A-Za-z_]\w*)*)(?:\((.*)\))?', value)
        receiver = None
        if match:
            chain, args = match.groups()
            root = chain.split('.')[0]
            if root in externals or root in internal or token in contract.get('internal_members', {}):
                continue
            if root not in index:
                errors.append(f'{line}: unclassified type reference `{token}`')
                continue
            parts = chain.split('.')
            type_name = parts.pop(0)
            while parts and type_name + '.' + parts[0] in index:
                type_name += '.' + parts.pop(0)
            if not parts:
                continue
            if len(parts) > 1:
                errors.append(f'{line}: unresolved member chain `{token}`')
                continue
            receiver, member = type_name, parts[0]
        else:
            call = re.fullmatch(r'(\w+)\((.*)\)', token)
            if not call:
                continue
            member, args = call.groups()
            receiver = contract['call_contexts'].get(member)
            if receiver is None:
                if args and all(re.fullmatch(r'[A-Za-z_$][\w.<>?\[\]]+\s+[A-Za-z_$]\w*', p.strip()) for p in args.split(',')):
                    errors.append(f'{line}: declaration needs an explicit receiver: `{token}`')
                continue
        if receiver:
            if '.Services.' in value:
                errors.append(f'{line}: bootstrap Services is not an addon entry: `{token}`')
            entry = index[receiver]
            if member not in entry['names']:
                errors.append(f'{line}: unknown member `{receiver}.{member}`')
            # Validate only declaration-style arguments; invocation expressions and
            # ellipses do not claim Java parameter names.
            if args and '...' not in args:
                params = [p.strip().split() for p in args.split(',')]
                if all(len(p) == 2 and re.fullmatch(r'[A-Za-z_$][A-Za-z0-9_$.<>?\[\]]*', p[0]) for p in params):
                    if params not in entry['methods'].get(member, []):
                        errors.append(f'{line}: parameter declaration differs from source: `{receiver}.{member}({args})`')
    return errors


def constant(index, ref, seen=()):
    if isinstance(ref, dict):
        match = re.search(ref['pattern'], index[ref['source']]['source'])
        if not match:
            raise ValueError(f'Limit source changed: {ref}')
        return int(match[ref.get('group', 1)].replace('_', '').rstrip('Ll')) * ref.get('factor', 1)
    if ref in seen:
        raise ValueError(f'Constant cycle: {ref}')
    owner, name = ref.rsplit('.', 1)
    source = index[owner]['source']
    match = re.search(r'\b' + re.escape(name) + r'\s*=\s*([^;]+);', source)
    if not match:
        raise ValueError(f'No constant initializer: {ref}')
    value = re.sub(r'AddonLimits\.live\((.*)\)', r'\1', match[1].strip())
    if re.fullmatch(r'[\d_]+[Ll]?', value):
        return int(value.rstrip('Ll').replace('_', ''))
    if re.fullmatch(r'\w+\.\w+', value):
        return constant(index, value, seen + (ref,))
    raise ValueError(f'Unsupported constant expression: {ref} = {value}')


def check(repo=ROOT, texts=None, contract=None):
    contract = contract or json.loads(CONTRACT.read_text(encoding='utf-8'))
    texts = texts or [(repo / p).read_text(encoding='utf-8') for p in DOCS]
    index = java_index(repo)
    sources = dict(index)
    for name, source_path in contract.get('source_files', {}).items():
        sources[name] = {'source': (repo / source_path).read_text(encoding='utf-8')}
    errors = []
    shapes = [sections(text) for text in texts]
    if len(shapes[0]) != len(shapes[1]):
        errors.append('Bilingual heading counts differ')
    for left, right in zip(*shapes):
        expected = contract['headings'].get(left[1])
        if expected != right[1] or left[0] != right[0]:
            errors.append(f'Bilingual heading mismatch: {left[1]} / {right[1]}')
        if left[2:] != right[2:]:
            errors.append(f'Bilingual code/table shape mismatch: {left[1]} / {right[1]}')
    for language, text in enumerate(texts):
        where = DOCS[language]
        errors.extend(f'{where}:{e}' for e in reference_errors(text, index, contract))
        local_anchors = anchors(text)
        for anchor in re.findall(r'\]\(#([^)]*)\)', prose(text)):
            if unquote(anchor) not in local_anchors:
                errors.append(f'{where}: broken anchor #{anchor}')
        for target in re.findall(r'\]\(([^)\s]+)\)', prose(text)):
            if '://' in target or target.startswith('#'):
                continue
            relative, _, anchor = unquote(target).partition('#')
            linked = (repo / where).parent / relative
            if not linked.exists():
                errors.append(f'{where}: missing linked file {relative}')
            elif anchor and linked.suffix == '.md' and anchor not in anchors(linked.read_text(encoding='utf-8-sig')):
                errors.append(f'{where}: broken cross-file anchor {target}')
        toc = re.search(r'^## (?:Contents|目录)\n(.*?)(?=^## )', prose(text), re.M | re.S)
        if toc:
            listed = re.findall(r'^\s*- \[(.+)\]\(#([^)]*)\)', toc[1], re.M)
            expected_toc = [(title, slug(title)) for level, title, *_ in shapes[language]
                            if level >= 2 and title not in ('Contents', '目录')]
            if listed != expected_toc:
                errors.append(f'{where}: contents does not match heading order')
        else:
            errors.append(f'{where}: missing contents')
        if language == 0:
            for block in prose(text).split('\n\n'):
                stripped = block.strip()
                if stripped and not stripped.startswith(('|', '#', '>', '-', '*')) and len(stripped.split()) > 150:
                    errors.append(f'{where}: paragraph exceeds 150 words: {stripped[:70]}')
        for row in contract['limits']:
            marker = f'<!-- limit:{row["id"]} -->'
            matches = [line for line in text.splitlines() if marker in line]
            if len(matches) != 1:
                errors.append(f'{where}: limit {row["id"]} must have one marked row')
                continue
            # Only numeric cells are checked; type identifiers must not contribute digits.
            cell = matches[0].split('|')[row['cell']]
            numbers = [int(n.replace(',', '').replace('_', '').replace('−', '-')) for n in re.findall(r'(?<!\w)[−-]?\d[\d,_]*(?!\w)', cell)]
            expected = [constant(sources, v) if isinstance(v, (str, dict)) else v for v in row['values']]
            if numbers != expected:
                errors.append(f'{where}: limit {row["id"]}: {numbers} != source {expected}')
        for row in contract.get('patterns', []):
            lines = [line for line in text.splitlines() if f'<!-- pattern:{row["id"]} -->' in line]
            if len(lines) != 1:
                errors.append(f'{where}: pattern {row["id"]} must have one marked row')
                continue
            owner, name = row['source'].rsplit('.', 1)
            pattern = re.search(r'\b' + re.escape(name) + r'\s*=\s*Pattern.compile\("((?:\\.|[^"\\])*)"\)', sources[owner]['source'])
            if not pattern:
                errors.append(f'{where}: missing source pattern {row["source"]}')
            elif json.loads('"' + pattern[1] + '"') not in lines[0].split('|')[row['cell']]:
                errors.append(f'{where}: name pattern {row["id"]} differs from source')
        for rule in contract.get('source_assertions', []):
            if not re.search(rule['pattern'], sources[rule['source']]['source']):
                errors.append(f'{where}: source rule changed: {rule["reason"]}')
        # Concrete example code may repeat useful values; narrative bounds belong
        # to the marked limits section, so there is one place to update them.
        outside = re.sub(r'^## (?:Limits|上限)\n.*?(?=^## )', '', prose(text), flags=re.M | re.S)
        for repeated in ('1_000_000_000_000', '1,024,000,000', '100,000'):
            if repeated in outside:
                errors.append(f'{where}: duplicated limit {repeated} outside Limits')
    return errors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo', type=Path, default=ROOT)
    args = parser.parse_args()
    try:
        errors = check(args.repo)
    except (OSError, ValueError, KeyError) as error:
        print(f'Addon API documentation source/contract error: {error}', file=sys.stderr)
        return 1
    if errors:
        print('\n'.join(errors), file=sys.stderr)
        return 1
    print('Addon API docs passed: references, bilingual sections, anchors and limits')
    return 0


if __name__ == '__main__':
    sys.exit(main())
