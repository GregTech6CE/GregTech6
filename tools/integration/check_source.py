"""Fast source boundary and exact-language checks; never launch Minecraft."""
import argparse
import json
from pathlib import Path
import re
import sys
import time

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from localization import ROOT, json_text, synchronize

SOURCE_ROOTS = ('core/src/main', 'src/main', 'src/generated',
                'neoforge/src/main', 'neoforge/src/generated')
CJK = re.compile(r'[\u3400-\u4dbf\u4e00-\u9fff\uf900-\ufaff\U00020000-\U000323af]')
# Text blocks precede ordinary strings. Comments/characters must also be consumed
# whole so quotes inside them cannot manufacture apparent imports or translations.
JAVA_TOKEN = re.compile(
    r'//[^\r\n]*|/\*[\s\S]*?\*/|"""(?:\\[\s\S]|(?!""")[^\\])*"""'
    r'|"(?:\\[\s\S]|[^"\\])*"|\'(?:\\[\s\S]|[^\'\\])*\'')
UNICODE_ESCAPE = re.compile(r'u+([0-9a-fA-F]{4})')
SURROGATE_PAIR = re.compile(r'([\ud800-\udbff])([\udc00-\udfff])')
PLATFORM_REFERENCE = re.compile(
    r'\b(?:net\s*\.\s*(?:minecraft|minecraftforge|neoforged)'
    r'|com\s*\.\s*gregtech\s*\.\s*gregtech\s*\.\s*platform)\s*\.')


def java_unicode(source):
    """Java expands eligible Unicode escapes before it recognizes comments/literals.

    Keep escaped backslashes literal; never recursively interpret the result.
    This also catches fully-qualified platform names written with Unicode escapes.
    """
    if '\\u' not in source:
        return source
    result, index, backslashes, escaped = [], 0, 0, False
    while index < len(source):
        char = source[index]
        match = (UNICODE_ESCAPE.match(source, index + 1)
                 if char == '\\' and (escaped or backslashes % 2 == 0) else None)
        if match:
            char = chr(int(match[1], 16))
            index = match.end()
            escaped = True
        else:
            index += 1
            escaped = False
        result.append(char)
        backslashes = backslashes + 1 if char == '\\' else 0
    return ''.join(result)


def java_code_and_literals(source):
    source = java_unicode(source)
    code, literals, cursor, line = [], [], 0, 1
    for match in JAVA_TOKEN.finditer(source):
        gap = source[cursor:match.start()]
        line += gap.count('\n')
        code.append(gap)
        token = match[0]
        if token.startswith(('"', "'")):
            value = SURROGATE_PAIR.sub(
                lambda m: chr(0x10000 + ((ord(m[1]) - 0xd800) << 10) + ord(m[2]) - 0xdc00), token)
            literals.append((line, value))
        code.append(re.sub(r'[^\r\n]', ' ', token))
        line += token.count('\n')
        cursor = match.end()
    code.append(source[cursor:])
    return ''.join(code), literals


def boundary_errors(path, source, *, core=False):
    code, literals = java_code_and_literals(source)
    errors = []
    if ('gametest' in path.parts or path.name == 'JeiMachineIndexTests.java'
            or re.search(r'\b(?:\w+\s*\.\s*)+gametest\s*\.', code)
            or re.search(r'@\s*(?:\w+\s*\.\s*)*(?:GameTest|GameTestHolder|GameTestGenerator)\b', code)):
        errors.append('GameTest belongs in bootstrapGameTest, not main: ' + str(path))
    if core and PLATFORM_REFERENCE.search(code):
        errors.append('Shared core must not depend on a loader or Minecraft: ' + str(path))
    for line, value in literals:
        if CJK.search(value):
            errors.append(f'Chinese literal bypasses exact-source language resources: {path}:{line}')
    return errors


def check(repo):
    started = time.perf_counter()
    errors, count = [], 0
    for name in SOURCE_ROOTS:
        base = repo / name
        for path in sorted((base / 'java').rglob('*.java')):
            count += 1
            errors.extend(boundary_errors(path.relative_to(repo), path.read_text(encoding='utf-8-sig'),
                                          core=name.startswith('core/')))
        resources = base / 'resources'
        for folder in ('structures', 'structure'):
            for structures in (resources / 'data').glob('*/' + folder):
                for path in structures.glob('test_*'):
                    errors.append('Test resource belongs in bootstrapGameTest: ' + str(path.relative_to(repo)))
        for namespace in ('gregtech_repair', 'gregtech_bootstrap', 'gregtech_sensor_source'):
            path = resources / 'data' / namespace
            if path.exists() and any(p.is_file() for p in path.rglob('*')):
                errors.append('Test namespace belongs in bootstrapGameTest: ' + str(path.relative_to(repo)))
        if not name.startswith('core/'):
            for locale in ('en_us', 'zh_cn'):
                path = resources / f'assets/gregtech/lang/{locale}.json'
                if path.exists():
                    errors.append('Platform language shadows shared core: ' + str(path.relative_to(repo)))
    if errors:
        raise ValueError('\n'.join(errors))
    language = synchronize(repo)
    return {'status': 'passed', 'java_sources': count, 'language': language,
            'seconds': round(time.perf_counter() - started, 3),
            'scope': 'Code boundaries, Chinese literal exclusion and exact language only; not runtime/gameplay acceptance.'}


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--repo', type=Path, default=ROOT)
    ap.add_argument('--output', type=Path)
    ns = ap.parse_args()
    try:
        report = check(ns.repo)
    except (ValueError, OSError) as error:
        ap.exit(1, f'{error}\n')
    if ns.output:
        ns.output.parent.mkdir(parents=True, exist_ok=True)
        ns.output.write_text(json_text(report), encoding='utf-8')
    print(f"Source policy passed: {report['java_sources']} Java sources, "
          f"{report['language']['source_keys']} original keys, "
          f"{report['language']['native_aliases']} aliases in {report['seconds']}s")


if __name__ == '__main__':
    main()
