#!/usr/bin/env python3
"""Extract GT6 FoodStatDrink overloads and effects; no Java runtime needed.
Usage: python3 tools/extract_gt6_drinks.py /path/to/Loader_Fluids.java
Reference: GregTech6 3703e40308c8c030763fd6297dea8b210d2a77b1.
Output: core/src/main/java/gregtech/content/data/generated/GTDrinksGen.java
"""
import json
import re
import sys
from pathlib import Path


def without_comments(text):
    """Lex Java comments only outside strings, preserving lines and escaped quotes."""
    out, i, quoted, escaped = [], 0, False, False
    while i < len(text):
        c = text[i]
        if quoted:
            out.append(c)
            if escaped: escaped = False
            elif c == "\\": escaped = True
            elif c == '"': quoted = False
            i += 1
        elif c == '"':
            quoted = True; out.append(c); i += 1
        elif text.startswith('//', i):
            end = text.find('\n', i)
            i = len(text) if end < 0 else end
        elif text.startswith('/*', i):
            end = text.find('*/', i + 2)
            if end < 0: raise ValueError('Unclosed comment')
            out.append(' ' + '\n' * text[i:end+2].count('\n'))
            i = end + 2
        else:
            out.append(c); i += 1
    return ''.join(out)


def require_number(value, integer=False):
    pattern = r'[+-]?\d+' if integer else r'[+-]?(?:\d+(?:\.\d*)?|\.\d+)[fFdD]?'
    if not re.fullmatch(pattern, value):
        raise ValueError('Expected ' + ('integer' if integer else 'number') + ': ' + value)


def arguments(text):
    result, start, depth, quoted, escaped = [], 0, 0, False, False
    for i, char in enumerate(text):
        if quoted:
            if escaped: escaped = False
            elif char == '\\': escaped = True
            elif char == '"': quoted = False
        elif char == '"': quoted = True
        elif char == '(': depth += 1
        elif char == ')':
            if depth == 0:
                result.append(text[start:i].strip())
                return result
            depth -= 1
        elif char == ',' and depth == 0:
            result.append(text[start:i].strip())
            start = i + 1
    raise ValueError('Unclosed constructor')


def extract(source):
    rows = {}
    for line in without_comments(source).splitlines():
        if not line.strip().startswith('new FoodStatDrink('): continue
        args = arguments(line.split('new FoodStatDrink(', 1)[1])
        match = re.match(r'(?:FL.create\()?"([^"]+)"', args[0])
        if match: key = match[1]
        else:
            match = re.match(r'FL\.(\w+)\.(?:make|fluid)\(', args[0])
            if not match: raise ValueError('Unknown fluid: ' + args[0])
            key = match[1]
        action = next(i for i, value in enumerate(args) if value.startswith('EnumAction.'))
        if action not in (7, 12, 13): raise ValueError('Unsupported constructor signature: ' + line)
        require_number(args[2], True)
        require_number(args[3])
        require_number(args[4])
        if not re.fullmatch(r'(?:C\+)?[+-]?\d+(?:\.\d*)?[fF]?', args[5]):
            raise ValueError('Unexpected temperature expression: ' + args[5])
        require_number(args[6])
        if args[action] not in ('EnumAction.drink', 'EnumAction.eat') or any(v not in ('T', 'F') for v in args[action+1:action+4]):
            raise ValueError('Unexpected action/boolean arguments: ' + line)
        stats = args[7:action]
        for value in stats: require_number(value, True)
        # Constructors have either no stats, five stats or six (including radiation).
        if len(stats) not in (0, 5, 6): raise ValueError('Unsupported overload: ' + line)
        stats += ['0'] * (6 - len(stats))
        effects = args[action+4:]
        if len(effects) % 4: raise ValueError('Invalid effects: ' + line)
        rendered = []
        for i in range(0, len(effects), 4):
            for value in effects[i+1:i+4]: require_number(value, True)
            name = json.dumps(re.sub(r'\s+', '', effects[i]))
            rendered.append(f'new DrinkNutritionRows.Effect({name}, {effects[i+1]}, {effects[i+2]}, {effects[i+3]})')
        rows[key] = 'new DrinkNutritionRows.Row(' + ', '.join([json.dumps(key), args[2], args[3].lower(), *stats, str(args[action+1] == 'T').lower(), str(args[action+2] == 'T').lower(), json.dumps(args[action].split('.')[1]), str('.setMilk()' in line).lower(), str('.setExtinguish()' in line).lower(), str('.setExplosive()' in line).lower(), 'List.of(' + ', '.join(rendered) + ')']) + ')'
    return rows


if __name__ == '__main__':
    source = Path(sys.argv[1]).read_bytes()
    import hashlib
    expected = "fdc9a697adc7822159cb8ea82389cc83e6494fc9723551f2983607ed46505fcb"
    if hashlib.sha256(source).hexdigest() != expected:
        raise SystemExit("Source differs from pinned GT6 revision; review constructor compatibility before updating the fingerprint")
    rows = extract(source.decode())
    output = Path(__file__).resolve().parents[1] / 'core/src/main/java/gregtech/content/data/generated/GTDrinksGen.java'
    output.write_text('''package gregtech.content.data.generated;

import gregtech.content.food.DrinkNutritionRows;
import java.util.List;

/** Generated by tools/extract_gt6_drinks.py from GT6 3703e40308c8c030763fd6297dea8b210d2a77b1. */
public final class GTDrinksGen {
    private GTDrinksGen() {}
    public static final List<DrinkNutritionRows.Row> ROWS = List.of(
''' + ',\n'.join('            ' + rows[k] for k in sorted(rows)) + ''');
    public static final List<String> SKIPPED = List.of();
}
''')
    print(f'Extracted {len(rows)} drink definitions')
