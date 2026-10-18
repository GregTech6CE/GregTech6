"""Lowercase every file/directory name inside the Gregtech resourcepack materialicons folder.

Only names are touched: file contents, structure and already-lowercase names stay as they are.
Dry-run by default; pass --apply to actually rename.

  python tools/lowercase_materialicons.py            # report what would change
  python tools/lowercase_materialicons.py --apply    # do it
"""
import argparse
import os
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / (
    'run/resourcepacks/Gregtech/assets/gt6ce/textures/block/materialicons'
)


def scan(root):
    """Return (renames, collisions, untouched) for every entry below root (bottom-up)."""
    renames, untouched = [], []
    # Deepest first so contents are renamed before their parent directory.
    for dirpath, dirnames, filenames in os.walk(root, topdown=False):
        parent = Path(dirpath)
        for name in filenames + dirnames:
            lowered = name.lower()
            if name == lowered:
                untouched.append(parent / name)
            else:
                renames.append((parent / name, parent / lowered))

    collisions = []
    sibling_names = {}
    for dirpath, dirnames, filenames in os.walk(root):
        for name in filenames + dirnames:
            sibling_names.setdefault(dirpath.lower(), {}).setdefault(name.lower(), set()).add(name)
    for group in sibling_names.values():
        for names in group.values():
            if len(names) > 1:
                collisions.append(sorted(names))
    return renames, collisions, untouched


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apply', action='store_true', help='perform the renames')
    parser.add_argument('--root', type=Path, default=ROOT)
    args = parser.parse_args()

    root = args.root.resolve()
    if not root.is_dir():
        raise SystemExit(f'not a directory: {root}')

    renames, collisions, untouched = scan(root)
    files = [r for r in renames if r[0].is_file()]
    dirs = [r for r in renames if r[0].is_dir()]
    print(f'root: {root}')
    print(f'rename: {len(files)} files, {len(dirs)} directories')
    print(f'already lowercase (untouched): {len(untouched)}')
    if collisions:
        print(f'!! case-only collisions: {len(collisions)}')
        for names in collisions:
            print('   ', ' <-> '.join(names))
        raise SystemExit('refusing to rename: lowercase names would collide')

    for sample in files[:10] + dirs[:5]:
        print(f'  {sample[0].relative_to(root)}  ->  {sample[1].name}')

    if not args.apply:
        print('dry run; re-run with --apply to rename')
        return

    done = 0
    for src, dst in sorted(renames, key=lambda pair: len(pair[0].parts), reverse=True):
        os.rename(src, dst)
        done += 1
    print(f'renamed {done} entries')

    after, collisions, untouched = scan(root)
    print(f'verify: {len(after)} uppercase names left, {len(collisions)} collisions')


if __name__ == '__main__':
    main()
