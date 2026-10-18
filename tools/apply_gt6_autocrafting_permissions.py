"""Retain source automatic-crafting permissions on exact material conversion rows.

Gregorius Techneticies / GregTech-6 Team, LGPL-3.0-or-later references:
Loader_Recipes_Handlers AdvancedCrafting1ToY/XToY registrations; CR.NO_AUTO.
Match prefix, material, occupied-cell count and output count, never pattern alone.
No external-mod recipes are invented for the eight CR.NO_AUTO compatibility rows.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path

from census_gt6_recipe_mirror_flags import split_top_level

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT.parent / 'gregtech6-master/gregtech6-master/src/main/java'
RECIPES = ROOT / 'core/src/main/resources/data/gt6ce/recipes'
MARKER = '_autocrafting_source'


def constructor_args(line: str, start: int) -> list[str]:
    depth = 1
    for i in range(start, len(line)):
        if line[i] == '(':
            depth += 1
        elif line[i] == ')':
            depth -= 1
            if depth == 0:
                return split_top_level(line[start:i])
    raise ValueError('Unclosed source constructor')


def source_rows(source: Path) -> tuple[list[dict], dict[tuple, dict]]:
    path = source / 'gregtech/loaders/c/Loader_Recipes_Handlers.java'
    rows, fixed = [], {}
    for number, line in enumerate(path.read_text(encoding='utf-8').splitlines(), 1):
        for match in re.finditer(r'new AdvancedCrafting(1ToY|XToY)\(', line):
            args = constructor_args(line, match.end())
            if match.group(1) == '1ToY':
                input_prefix, output_prefix, output_count, permission = args[:4]
                input_count = '1'
            else:
                input_prefix, input_count, output_prefix, output_count, permission = args[:5]
            if permission not in ('T', 'F'):
                raise ValueError(f'Unresolved source permission at {path}:{number}: {permission}')
            row = dict(line=number, input=input_prefix, input_count=input_count,
                       output=output_prefix, output_count=output_count,
                       kind=match.group(1),
                       autocraftable=permission == 'T', expression=match.group(0) + ', '.join(args) + ')')
            rows.append(row)
            if re.fullmatch(r'\w+', input_prefix) and input_prefix != 'tPrefix' and input_count.isdigit() and output_count.isdigit():
                key = input_prefix, int(input_count), output_prefix, int(output_count)
                if key in fixed and fixed[key]['autocraftable'] != row['autocraftable']:
                    raise ValueError(f'Conflicting source permissions: {key}')
                fixed[key] = row
    # Every single-input source constructor reserves an offset, including absent material outputs.
    groups = {}
    for row in rows:
        if row['kind'] == '1ToY' and row['input'] != 'tPrefix' and not row['input'].startswith('wireGt['):
            group = groups.setdefault(row['input'], [])
            row['selector_offset'] = len(group)
            group.append(row)
    for group in groups.values():
        for row in group:
            row['selector_variants'] = len(group)
    return rows, fixed


def snake(value: str) -> str:
    return re.sub(r'([a-z0-9])([A-Z])', r'\1_\2', value).lower()


def form(ingredient: dict, prefixes: set[str]) -> tuple | None:
    if not isinstance(ingredient, dict):
        return None
    if set(ingredient) == {'item'} and ingredient['item'].startswith('gt6ce:'):
        value = ingredient['item'][9:]
        wire = re.fullmatch(r'wire_(\d{2})_(.+)', value)
        if wire and 1 <= int(wire[1]) <= 16:
            return 'wire', int(wire[1]), wire[2]
        for prefix in sorted(prefixes, key=lambda v: -len(snake(v))):
            stem = snake(prefix) + '_'
            if value.startswith(stem):
                return prefix, value[len(stem):]
    if set(ingredient) == {'tag'}:
        tag = ingredient['tag']
        for prefix in prefixes:
            for namespace in ('forge:', 'gt6ce:'):
                stem = namespace + snake(prefix) + 's/'
                if tag.startswith(stem):
                    return prefix, tag[len(stem):]
    return None


def cells(data: dict) -> list[dict]:
    if data.get('type') == 'minecraft:crafting_shapeless' or (
            data.get('type') == 'gt6ce:tool_shapeless' and MARKER in data):
        return data.get('ingredients', [])
    if data.get('type') == 'minecraft:crafting_shaped':
        key = data.get('key', {})
        return [key[c] for row in data.get('pattern', []) for c in row if c != ' ']
    return []


def bind(data: dict, rows: list[dict], fixed: dict, prefixes: set[str]) -> dict | None:
    occupied = cells(data)
    if not occupied or len(occupied) > 9 or any(v != occupied[0] for v in occupied):
        return None
    required = form(occupied[0], prefixes)
    result = data.get('result', {})
    product = form({'item': result.get('item', '')}, prefixes)
    if not required or not product:
        return None
    count = result.get('count', 1)
    if required[0] == product[0] == 'wire':
        small, big = sorted((required[1], product[1]))
        if small == big or big % small or required[2] != product[2]:
            return None
        ratio = big // small
        if required[1] == small and len(occupied) == ratio and count == 1 and ratio < 10:
            return next(row for row in rows if row['input'].startswith('wireGt[tSmall'))
        if required[1] == big and len(occupied) == 1 and count == ratio:
            row = dict(next(row for row in rows if row['input'].startswith('wireGt[tBig')))
            divisors = [size for size in range(1, big) if big % size == 0]
            row['selector_offset'] = divisors.index(small)
            row['selector_variants'] = len(divisors)
            return row
        return None
    if len(required) == len(product) == 2 and required[1] == product[1]:
        return fixed.get((required[0], len(occupied), product[0], count))
    return None


def run(source: Path, check: bool, report_path: Path | None) -> int:
    rows, fixed = source_rows(source)
    prefixes = {key[0] for key in fixed} | {key[2] for key in fixed}
    bindings, pending = [], 0
    for path in sorted(RECIPES.rglob('*.json')):
        original = path.read_text(encoding='utf-8')
        data = json.loads(original)
        row = bind(data, rows, fixed, prefixes)
        if row is None:
            continue
        marker = f"GT6 Loader_Recipes_Handlers:{row['line']} ({'allowed' if row['autocraftable'] else 'disabled'})"
        selector = {}
        if row['kind'] == '1ToY':
            if data['type'] not in ('minecraft:crafting_shapeless', 'gt6ce:tool_shapeless'):
                raise ValueError(f'Single-input source conversion needs a native selector: {path}')
            selector = {'type': 'gt6ce:tool_shapeless',
                        'gregtech_form_variants': row['selector_variants'],
                        'gregtech_form_offset': row['selector_offset']}
        if (data.get('gregtech_autocraftable') is not row['autocraftable'] or data.get(MARKER) != marker
                or any(data.get(key) != value for key, value in selector.items())):
            pending += 1
            if not check:
                data['gregtech_autocraftable'] = row['autocraftable']
                data[MARKER] = marker
                data.update(selector)
                path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
        bindings.append(dict(recipe=path.relative_to(RECIPES).as_posix(), source_line=row['line'],
                             autocraftable=row['autocraftable'], selector=selector))
    external = []
    for path in sorted(source.rglob('*.java')):
        if '/compat/' not in path.as_posix():
            continue
        for number, line in enumerate(path.read_text(encoding='utf-8').splitlines(), 1):
            if re.search(r'CR\.(?:DEF\w*NAC\w*|NO_AUTO)\b', line):
                external.append(dict(path=path.relative_to(source).as_posix(), line=number,
                                     expression=line.strip(), status='external content not ported'))
    if report_path:
        report_path.parent.mkdir(parents=True, exist_ok=True)
        source_file = source / 'gregtech/loaders/c/Loader_Recipes_Handlers.java'
        report = dict(source_root=str(source), conversion_file_sha256=hashlib.sha256(source_file.read_bytes()).hexdigest(),
                      conversion_constructor_sites=rows, static_bindings=bindings,
                      external_no_auto_sites=external, pending_changes=pending,
                      evidence_boundary='Exact source quantity/material bindings and single-input offsets; custom GT recipes excluded except previously source-bound rows. Not a Minecraft runtime test.')
        report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(f'{len(rows)} source conversion sites; {len(bindings)} exact static bindings; {len(external)} unported external NO_AUTO sites; {pending} {"pending" if check else "updated"}')
    return 1 if check and pending else 0


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, default=SOURCE)
    parser.add_argument('--check', action='store_true')
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    raise SystemExit(run(args.source, args.check, args.report))
