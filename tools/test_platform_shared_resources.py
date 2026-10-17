"""The shared resource tree fails the build when a platform tree has the same path."""
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def main():
    fixture = ROOT / 'build' / 'platform-shared-dup-tests'
    duplicated = fixture / 'duplicated'
    distinct = fixture / 'distinct'
    for path in (duplicated / 'shared' / 'assets', duplicated / 'platform' / 'assets',
                 distinct / 'shared' / 'assets', distinct / 'platform' / 'assets'):
        path.mkdir(parents=True, exist_ok=True)
    (duplicated / 'shared' / 'assets' / 'probe.txt').write_text('shared', encoding='utf-8')
    (duplicated / 'platform' / 'assets' / 'probe.txt').write_text('platform', encoding='utf-8')
    (distinct / 'shared' / 'assets' / 'only-shared.txt').write_text('shared', encoding='utf-8')
    (distinct / 'platform' / 'assets' / 'only-platform.txt').write_text('platform', encoding='utf-8')
    (distinct / 'shared' / '.gitkeep').write_bytes(b'')
    (distinct / 'platform' / '.gitkeep').write_bytes(b'')
    command = [str(ROOT / 'gradlew.bat'), 'verifySharedResourceDuplicates', '--offline', '--console=plain',
               '-PsharedDuplicateFixture=' + fixture.as_posix()]
    result = subprocess.run(command, cwd=ROOT, capture_output=True, text=True, encoding='utf-8', errors='replace')
    if result.returncode:
        raise SystemExit(result.stdout[-4000:] + result.stderr[-4000:])
    print('PASS: duplicate shared resource is rejected, distinct files and .gitkeep are not')


if __name__ == '__main__':
    main()
