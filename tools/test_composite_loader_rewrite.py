"""NeoForge's resource copy rewrites only the shared composite loader bytes."""
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
FORGE = b'"loader": "forge:composite"'
NEO = b'"loader": "neoforge:composite"'


def main():
    fixture = ROOT / 'build' / 'composite-loader-fixture'
    fixture.mkdir(parents=True, exist_ok=True)
    source = b'{\r\n  "loader": "forge:composite",\r\n  "keep": "forge:composite"\r\n}\r\n'
    (fixture / 'source.json').write_bytes(source)
    command = [str(ROOT / 'gradlew.bat'), 'verifyCompositeLoaderRewrite', '--offline', '--console=plain',
               '-PcompositeLoaderFixture=' + fixture.as_posix()]
    result = subprocess.run(command, cwd=ROOT, capture_output=True, text=True, encoding='utf-8', errors='replace')
    if result.returncode:
        raise SystemExit(result.stdout[-4000:] + result.stderr[-4000:])
    rewritten = (fixture / 'rewritten.json').read_bytes()
    expected = source.replace(FORGE, NEO)
    if rewritten != expected:
        raise SystemExit(f'rewrite changed more than the loader id:\n{rewritten!r}')
    print('PASS: composite loader rewrite keeps every other byte')


if __name__ == '__main__':
    main()
