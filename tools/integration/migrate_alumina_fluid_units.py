#!/usr/bin/env python3
"""Source-guarded exact-U migration of eight existing Alumina chemistry rows.

Run writes through tools/native_tool.py; --check is read-only. This does not
reinterpret legacy m tokens or expand the original coal family. The chemistry
transpiler calls migrate_chemistry so full regeneration keeps this migration.
"""
from __future__ import annotations
import argparse
import ast
import hashlib
import json
from pathlib import Path
import re
import sys

BASELINE_SHA256 = 'f9e541855f9773c596fcd7c1150f733379b64a0492c605a5308b82b231ce7c25'
U = 648_648_000
LONG_MIN, LONG_MAX = -(1 << 63), (1 << 63) - 1
CHEM = 'src/main/java/gregtech/loaders/c/Loader_Recipes_Chem.java'
CONSTANTS = 'src/main/java/gregapi/data/CS.java'
EXPECTED_FORMS = {(coal, amount, alumina, count, duration) for coal, amount, count, duration in
                  [('blockDust', 1, 30, 6144), ('dust', 3, 10, 2048),
                   ('dustTiny', 27, 10, 2048), ('dustSmall', 6, 5, 1024)] for alumina in ('dust', 'ingot')}
EXPECTED_EXPRESSIONS = {
    'blockDust': ('U24', 'U12', 'U*27', '7*U80', 'U*12'),
    'dust': ('U72', 'U36', 'U*9', '7*U240', 'U*4'),
    'dustTiny': ('U72', 'U36', 'U*9', '7*U240', 'U*4'),
    'dustSmall': ('U144', 'U72', 'U2*9', '7*U480', 'U*2'),
}


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def uncomment(text: str) -> str:
    return re.sub(r'//[^\n]*|/\*.*?\*/', lambda m: '\n' * m[0].count('\n'), text, flags=re.S)


def compact(text: str) -> str:
    return re.sub(r'\s+', '', text)


def balanced(text: str, opening: int, left='(', right=')') -> tuple[str, int]:
    if text[opening] != left:
        raise ValueError('Expected opening delimiter')
    depth = 0
    for i in range(opening, len(text)):
        if text[i] == left:
            depth += 1
        elif text[i] == right:
            depth -= 1
            if depth == 0:
                return text[opening + 1:i], i + 1
    raise ValueError('Unbalanced audited source')


def args(text: str) -> list[str]:
    result, start, depth = [], 0, 0
    for i, char in enumerate(text):
        if char in '([{': depth += 1
        elif char in ')]}': depth -= 1
        elif char == ',' and depth == 0:
            result.append(text[start:i].strip()); start = i + 1
    result.append(text[start:].strip())
    return result


def source_constants(path: Path) -> dict[str, int]:
    text = uncomment(path.read_text(encoding='utf-8'))
    declaration = re.search(r'public\s+static\s+final\s+long\s+U\s*=\s*(\d+)\s*,([^;]+);', text)
    if declaration is None or int(declaration[1]) != U:
        raise ValueError('Original material U definition changed')
    constants = {'U': U}
    for part in declaration[2].split(','):
        match = re.fullmatch(r'\s*(U\d+)\s*=\s*U\s*/\s*(\d+)\s*', part)
        if match is None or match[1] != 'U' + match[2]:
            raise ValueError('Unknown original unit constant: ' + part)
        constants[match[1]] = U // int(match[2])
    return constants


def exact_units(expression: str, constants: dict[str, int]) -> int:
    def visit(node):
        if isinstance(node, ast.Constant) and type(node.value) is int: value = node.value
        elif isinstance(node, ast.Name) and node.id in constants: value = constants[node.id]
        elif isinstance(node, ast.UnaryOp) and isinstance(node.op, (ast.UAdd, ast.USub)):
            operand = visit(node.operand); value = operand if isinstance(node.op, ast.UAdd) else -operand
        elif isinstance(node, ast.BinOp):
            a, b = visit(node.left), visit(node.right)
            if isinstance(node.op, ast.Add): value = a + b
            elif isinstance(node.op, ast.Sub): value = a - b
            elif isinstance(node.op, ast.Mult): value = a * b
            elif isinstance(node.op, ast.Div):
                if b == 0: raise ValueError('Zero divisor')
                value = (abs(a) // abs(b)) * (-1 if (a < 0) != (b < 0) else 1)
            else: raise ValueError('Unsupported original unit operator')
        else: raise ValueError('Unsupported original unit expression')
        if not LONG_MIN <= value <= LONG_MAX: raise ValueError('Original signed-long overflow')
        return value
    try:
        return visit(ast.parse(expression.strip(), mode='eval').body)
    except (SyntaxError, RecursionError) as exc:
        raise ValueError('Malformed original unit expression') from exc


def legacy_milli_units(expression: str) -> int:
    # The existing producer contract is used ONLY to identify the exact old rows.
    # New token quantities always use exact_units, never this early-rounded path.
    expression = re.sub(r'\bU(\d+)?\b', lambda m: str(1000 if not m[1] else max(1, round(1000 / int(m[1])))), expression)
    if not re.fullmatch(r'[0-9+\-*/() ]+', expression): raise ValueError('Unverified legacy expression')
    return int(eval(expression, {'__builtins__': {}}, {}))


def material_fluid(expression: str, material: str, selector: str, rounding: str, constants: dict[str, int]) -> dict:
    match = re.fullmatch(r'MT\.' + material + r'\.' + selector + r'\((.+),([TF])\)', compact(expression))
    if match is None or match[2] != rounding:
        raise ValueError('Original fluid selector/rounding changed: ' + expression)
    amount = exact_units(match[1], constants)
    if amount <= 0: raise ValueError('Expected positive original material quantity')
    return {'material': material, 'selector': selector, 'expression': match[1], 'source_units': amount,
            'round': 'up' if rounding == 'T' else 'down',
            'old_token': f'm:{material}:{legacy_milli_units(match[1])}',
            'new_token': f'mu:{material}:{selector}:{amount}:{"up" if rounding == "T" else "down"}'}


def parse_source(chemistry: Path, constants_file: Path) -> list[dict]:
    constants = source_constants(constants_file)
    text = uncomment(chemistry.read_text(encoding='utf-8'))
    loops = list(re.finditer(r'for\s*\(\s*OreDictMaterial\s+tMat\s*:\s*ANY\.Coal\.mToThis\s*\)\s*if\s*\(\s*tMat\s*!=\s*MT\.Graphene\s*\)\s*\{', text))
    selected = [(loop, balanced(text, loop.end() - 1, '{', '}')[0]) for loop in loops]
    selected = [(loop, body) for loop, body in selected if 'MT.Na3AlF6.liquid(' in compact(body)]
    if len(selected) != 1: raise ValueError('Expected one explicitly Graphene-excluding Alumina coal loop')
    loop, body = selected[0]
    if len(re.findall(r'RM\.\w+\s*\.addRecipe', body)) != 8:
        raise ValueError('Audited specialized loop recipe count changed')
    matches = list(re.finditer(r'RM\.Electrolyzer\s*\.addRecipe2\s*\(', body))
    if len(matches) != 8: raise ValueError('Expected eight original specialized calls')
    rows = []
    for match in matches:
        call, _ = balanced(body, match.end() - 1); parts = args(call)
        if len(parts) != 8 or compact(parts[0]) != 'T' or compact(parts[1]) != '16':
            raise ValueError('Original recipe shape/power changed')
        duration = int(parts[2])
        coal = re.fullmatch(r'OP\.(blockDust|dust|dustTiny|dustSmall)\.mat\(tMat,(\d+)\)', compact(parts[3]))
        alumina = re.fullmatch(r'OP\.(dust|ingot)\.mat\(MT\.Al2O3,(\d+)\)', compact(parts[4]))
        if coal is None or alumina is None: raise ValueError('Original specialized item identity changed')
        key = (coal[1], int(coal[2]), alumina[1], int(alumina[2]), duration)
        if key not in EXPECTED_FORMS: raise ValueError('Original specialized item ratio changed')
        groups = []
        for group in parts[5:7]:
            group = compact(group)
            if not group.startswith('FL.array(') or not group.endswith(')'): raise ValueError('Original fluid array changed')
            elements = args(group[len('FL.array('):-1])
            if len(elements) != 2: raise ValueError('Original fluid arity changed')
            groups.append(elements)
        fluids = [material_fluid(groups[0][0], 'Na3AlF6', 'liquid', 'T', constants),
                  material_fluid(groups[0][1], 'AlF3', 'liquid', 'T', constants),
                  material_fluid(groups[1][0], 'CO2', 'gas', 'F', constants),
                  material_fluid(groups[1][1], 'F', 'gas', 'F', constants)]
        output = re.fullmatch(r'OM\.dust\(MT\.Al,(.+)\)', compact(parts[7]))
        if output is None or exact_units(output[1], constants) != int(alumina[2]) * 2 * U // 5:
            raise ValueError('Original aluminium output changed')
        if tuple(f['expression'] for f in fluids) + (output[1],) != EXPECTED_EXPRESSIONS[coal[1]]:
            raise ValueError('Original specialized fluid/output expressions changed')
        item_in = [f'i:{coal[1]}:Coal:{coal[2]}', f'i:{alumina[1]}:Al2O3:{alumina[2]}']
        item_out = [f'omd:Al:{legacy_milli_units(output[1])}']
        def array(tokens): return 'sp(' + ', '.join(json.dumps(token) for token in tokens) + ')'
        def render(token):
            return f'        r("Electrolyzer", 16, {duration}, null, {array(item_in)}, ' + array([f[token] for f in fluids[:2]]) + ', ' + array([f[token] for f in fluids[2:]]) + ', ' + array(item_out) + ');'
        rows.append({'key': key, 'source_line': text[:loop.end()].count('\n') + body[:match.start()].count('\n') + 1,
                     'source_call': compact(call), 'fluids': fluids, 'old_line': render('old_token'), 'new_line': render('new_token')})
    if {tuple(r['key']) for r in rows} != EXPECTED_FORMS: raise ValueError('Duplicate/missing original specialized form')
    return rows


def migrate_rows(content: str, rows: list[dict]) -> str:
    old_counts = [content.splitlines().count(row['old_line']) for row in rows]
    new_counts = [content.splitlines().count(row['new_line']) for row in rows]
    if old_counts == [0] * 8 and new_counts == [1] * 8: return content
    if old_counts != [1] * 8 or new_counts != [0] * 8:
        raise ValueError('Expected exactly eight unmigrated or eight migrated rows, never mixed or missing')
    for row in rows: content = content.replace(row['old_line'] + '\n', row['new_line'] + '\n')
    return content


def migrate_chemistry(content: str, chemistry: Path, constants_file: Path) -> str:
    """Full producer hook: only the eight verified source calls change their fluid tokens."""
    return migrate_rows(content, parse_source(chemistry, constants_file))


def prepare(content: str, source: Path, comparison: Path) -> tuple[str, str]:
    rows = parse_source(source / CHEM, source / CONSTANTS)
    other = parse_source(comparison / CHEM, comparison / CONSTANTS)
    semantic = lambda rs: [{k: v for k, v in row.items() if k != 'source_line'} for row in rs]
    if semantic(rows) != semantic(other): raise ValueError('Original references disagree on specialized recipe semantics')
    legacy = content
    for row in rows: legacy = legacy.replace(row['new_line'] + '\n', row['old_line'] + '\n')
    if sha(legacy.encode()) != BASELINE_SHA256:
        raise ValueError('Unrelated generated content differs from the audited baseline; do not overwrite it')
    result = migrate_rows(content, rows)
    if sum(line.startswith('        r(') and '"mu:' in line for line in result.splitlines()) != 8:
        raise ValueError('Unexpected dimensioned-token row count')
    if len(re.findall(r'"mu:[^"]+"', result)) != 32: raise ValueError('Expected exactly 32 dimensioned fluid tokens')
    report = {'scope': 'eight existing Coal-only Alumina rows; only four fluid tokens per row change',
              'source_unit': U, 'generated_baseline_sha256': BASELINE_SHA256, 'generated_sha256': sha(result.encode()),
              'row_count': 8, 'new_token_count': 32,
              'sources': [{'reference': root.name, 'files': [{'path': path, 'sha256': sha((root/path).read_bytes())}
                           for path in (CHEM, CONSTANTS)]} for root in (source, comparison)],
              'rows': rows}
    return result, json.dumps(report, indent=2) + '\n'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--compare-source', type=Path, required=True)
    parser.add_argument('--repo', type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument('--java-output', type=Path)
    parser.add_argument('--report-output', type=Path)
    parser.add_argument('--check', action='store_true')
    arguments = parser.parse_args()
    sys.path.insert(0, str(arguments.repo / 'tools'))
    from native_layout import require_managed_writer
    if not arguments.check: require_managed_writer()
    canonical = arguments.repo / 'core/src/main/java/com/gregtech/gregtech/content/recipe/generated/GTChemGen.java'
    java_output = arguments.java_output or canonical
    report_output = arguments.report_output or arguments.repo / 'docs/integration/verification/alumina-fluid-unit-migration-20261023.json'
    output, report = prepare(canonical.read_text(encoding='utf-8'), arguments.source, arguments.compare_source)
    for path, value in [(java_output, output), (report_output, report)]:
        if arguments.check:
            if not path.is_file() or path.read_text(encoding='utf-8') != value: raise ValueError('Stale generated output: ' + str(path))
        else:
            path.parent.mkdir(parents=True, exist_ok=True); path.write_text(value, encoding='utf-8')
    print('Both original references agree: 8 rows, 32 exact-U tokens; unrelated generated bytes preserved')


if __name__ == '__main__': main()
