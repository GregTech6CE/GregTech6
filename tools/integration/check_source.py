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
IMPORT = re.compile(r'^\s*import\s+(?:static\s+)?([\w.]+)', re.M)


def boundary_errors(path, source, *, core=False):
    # Ignore commented-out imports and annotations; retain line starts.
    code = re.sub(r'"(?:\\.|[^"\\])*"|//[^\n]*|/\*[\s\S]*?\*/',
                  lambda m: m[0] if m[0].startswith('"') else re.sub(r'[^\n]', ' ', m[0]), source)
    imports = IMPORT.findall(code)
    errors = []
    if ('gametest' in path.parts or path.name == 'JeiMachineIndexTests.java'
            or any('.gametest.' in item for item in imports)
            or re.search(r'@(?:[\w.]+\.)?(?:GameTest|GameTestHolder|GameTestGenerator)\b', code)):
        errors.append('GameTest belongs in bootstrapGameTest, not main: ' + str(path))
    if core and any(item.startswith(('net.minecraft.', 'net.minecraftforge.', 'net.neoforged.',
                                    'com.gregtech.gregtech.platform.')) for item in imports):
        errors.append('Shared core must not depend on a loader or Minecraft: ' + str(path))
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
            'scope': 'Source boundaries and exact language only; not runtime/gameplay acceptance.'}


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
