"""Align Java definition-table columns without changing tokens or literals.

This is deliberately not a general Java formatter. Only complete, single-line
field declarations and adjacent constructor/registration rows are eligible. Strings,
comments, row order, other statements and multiline expressions remain intact.
"""
from collections import defaultdict
from pathlib import Path
import argparse
import re


ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'core/src/main/java/com/gregtech/gregtech'
PROTECTED = re.compile(r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*[\s\S]*?\*/')
FIELD = re.compile(r'^(\s*(?:public|private|protected) static final [\w.<>?, \[\]]+? \w+)\s*=\s*(.+;)$')
CALL = re.compile(r'^(\s*(?:new )?[\w.]+)\(')
TABLE_ROW = re.compile(r'^\s+(?:new [\w.]+|add|register|define|row|entry|metalset|block|put)\(')


def mask(source):
    """Keep offsets and line breaks while hiding literal/comment punctuation."""
    return PROTECTED.sub(lambda m: re.sub(r'[^\r\n]', '~', m.group()), source)


def content_signature(source):
    # Include comments and literal spelling; stripping whitespace alone would
    # miss accidental edits to spaces inside a string or comment.
    return (re.sub(r'\s+', '', mask(source)), PROTECTED.findall(source))


def arguments(expression):
    """Return the first complete call's prefix, arguments and untouched tail."""
    view = mask(expression)
    match = CALL.match(view)
    if not match:
        return None
    opening = match.end() - 1
    depth, start, values = 0, opening + 1, []
    for pos in range(opening, len(view)):
        char = view[pos]
        if char in '([{':
            depth += 1
        elif char in ')]}':
            depth -= 1
            if depth == 0:
                values.append(expression[start:pos].strip())
                if not all(values):
                    return None
                return expression[:opening + 1], values, expression[pos:]
        elif char == ',' and depth == 1:
            values.append(expression[start:pos].strip())
            start = pos + 1
        elif char in '<>':
            # Generic type argument lists and comparisons need a Java parser.
            return None
    return None


def align_calls(rows):
    parsed = [arguments(row) for row in rows]
    groups = defaultdict(list)
    for index, value in enumerate(parsed):
        if value:
            prefix, args, _ = value
            groups[(prefix, len(args))].append(index)
    result = list(rows)
    for indices in groups.values():
        if len(indices) < 2:
            continue
        widths = [max(len(parsed[i][1][column]) for i in indices)
                  for column in range(len(parsed[indices[0]][1]))]
        for i in indices:
            prefix, args, tail = parsed[i]
            result[i] = prefix + ''.join(
                arg + ',' + ' ' * (widths[j] - len(arg) + 1)
                if j < len(args) - 1 else arg
                for j, arg in enumerate(args)) + tail
    return result


def align_java_tables(source):
    lines = source.splitlines(keepends=True)
    views = mask(source).splitlines(keepends=True)
    index = 0
    while index < len(lines):
        field = FIELD.match(lines[index].rstrip('\r\n'))
        is_field = field is not None and FIELD.match(views[index].rstrip('\r\n')) is not None
        is_constructor = TABLE_ROW.match(views[index]) is not None
        if not is_field and not is_constructor:
            index += 1
            continue
        end, rows, heads = index, [], []
        while end < len(lines):
            row = lines[end].rstrip('\r\n')
            if is_field:
                match = FIELD.match(row)
                if not match or not FIELD.match(views[end].rstrip('\r\n')):
                    break
                heads.append(match[1])
                rows.append(match[2])
            else:
                if not TABLE_ROW.match(views[end]) or not arguments(row):
                    break
                rows.append(row)
            end += 1
        if end == index:
            index += 1
            continue
        if len(rows) >= 2:
            aligned = align_calls(rows)
            width = max(map(len, heads)) if is_field else 0
            for offset, row in enumerate(aligned):
                old = lines[index + offset]
                newline = old[len(old.rstrip('\r\n')):]
                lines[index + offset] = (
                    heads[offset].ljust(width) + ' = ' + row if is_field else row) + newline
        index = end
    result = ''.join(lines)
    if content_signature(source) != content_signature(result):
        raise ValueError('Definition formatting changed source content')
    return result


def definition_paths():
    paths = set((JAVA / 'content/material/generated').glob('*.java'))
    paths.update(JAVA / path for path in (
        'content/material/Materials.java', 'data/generated/GT6Materials.java',
        'data/BasicMachineOriginalParams.java', 'data/FluidCatalog.java'))
    for pattern in ('*Catalog.java', '*Definitions.java', 'Original*Data.java'):
        paths.update(JAVA.rglob(pattern))
    return sorted(paths)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('paths', nargs='*', type=Path)
    parser.add_argument('--write', action='store_true', help='Apply whitespace-only changes')
    args = parser.parse_args()
    changed = []
    for path in args.paths or definition_paths():
        before = path.read_bytes().decode('utf-8')
        after = align_java_tables(before)
        if after != align_java_tables(after):
            raise ValueError(f'Formatting is not idempotent: {path}')
        if before != after:
            changed.append(str(path.relative_to(ROOT) if path.is_relative_to(ROOT) else path))
            if args.write:
                path.write_bytes(after.encode('utf-8'))
    print('\n'.join(changed))
    print(f'{len(changed)} definition files ' + ('aligned' if args.write else 'would change'))


if __name__ == '__main__':
    main()
