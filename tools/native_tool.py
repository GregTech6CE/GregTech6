"""Run an existing generator transactionally against Stonecutter tooling views."""
from pathlib import Path
import os
import subprocess
import sys
from native_layout import prepare, reconcile, changes


def main():
    repo = Path(__file__).resolve().parents[1]
    if len(sys.argv) == 2 and sys.argv[1] == '--discard-view-changes':
        print(prepare(repo, force=True)); return 0
    if len(sys.argv) == 2 and sys.argv[1] == '--prepare':
        print(prepare(repo)); return 0
    if len(sys.argv) < 2:
        print('Usage: python tools/native_tool.py tools/<generator>.py [arguments]'); return 2
    script = (repo / sys.argv[1]).resolve()
    if not script.is_relative_to(repo / 'tools') or not script.is_file():
        raise ValueError('Expected a repository Python tool')
    prepare(repo)
    if changes(repo): raise RuntimeError('Uncommitted tooling-view edits; preserve them before running another generator')
    environment = dict(os.environ, GT_NATIVE_TOOL_ACTIVE='1')
    result = subprocess.run([sys.executable, '-X', 'utf8', str(script), *sys.argv[2:]], cwd=repo, env=environment)
    if result.returncode:
        print('Generator failed; managed native sources were not changed. View edits remain in build/native-tools.', file=sys.stderr)
        return result.returncode
    changed, written = reconcile(repo)
    print(f'Native tool: {changed} view edits reconciled into {written} managed files. Regenerate before further checks.')
    return 0


if __name__ == '__main__': sys.exit(main())
