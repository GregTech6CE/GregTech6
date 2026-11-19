"""Verify a packaged release against processed resource roots (stdlib only).

Usage: python tools/resources/check_packaged_resources.py MOD.jar --resources CORE_OUTPUT NODE_OUTPUT
Checks that the jar carries no template index, no duplicate or empty entries and no runtime-served
stack groups, and that every static model, blockstate, TSV table and PNG matches the processed
output byte for byte. PNG bytes are compared with the processed output directories, which the build
filters independently of the committed textures; check_png_metadata.py audits that filtering.
This verifies packaging, not Minecraft's rendered appearance.
"""

import argparse
import sys
from pathlib import Path
from zipfile import ZipFile

INDEX = "META-INF/gt6ce/model-json-aliases.json"
STACK_GROUPS = "assets/gt6ce/stack_groups/"
MODEL_DIRS = ("assets/gt6ce/models/", "assets/gt6ce/blockstates/")
ASSET_DIR = "assets/gt6ce/"


def is_table(path):
    """A TSV table directly under the mod asset directory."""
    return path.startswith(ASSET_DIR) and path.endswith(".tsv") and "/" not in path[len(ASSET_DIR):]


def is_tracked(path):
    """A resource the release ships as a static file, compared byte for byte with the jar."""
    if path.endswith(".png"):
        return True
    if is_table(path):
        return True
    return path.endswith(".json") and path.startswith(MODEL_DIRS)


def collect_roots(roots):
    """Read every tracked resource from the processed roots; reject duplicates and stack groups."""
    expected = {}
    groups = []
    for root in roots:
        if not root.is_dir():
            raise ValueError(f"Missing processed resource root: {root}")
        for file in root.rglob("*"):
            if not file.is_file():
                continue
            path = file.relative_to(root).as_posix()
            if path.startswith(STACK_GROUPS):
                groups.append(path)
                continue
            if not is_tracked(path):
                continue
            if path in expected:
                raise ValueError(f"Duplicate resource across roots: {path}")
            expected[path] = file.read_bytes()
    if groups:
        raise ValueError(f"Runtime-served stack groups in processed roots: {sorted(groups)[:5]}")
    return expected


def verify(jar, roots, max_static_json):
    expected = collect_roots(roots)
    with ZipFile(jar) as archive:
        infolist = archive.infolist()
        names = [info.filename for info in infolist]
        if INDEX in names:
            raise ValueError(f"Template index must not be packaged: {INDEX}")
        if len(set(names)) != len(infolist):
            raise ValueError("Duplicate ZIP paths")
        non_empty_dirs = set()
        for info in infolist:
            if not info.is_dir():
                parts = info.filename.split("/")
                for depth in range(1, len(parts)):
                    non_empty_dirs.add("/".join(parts[:depth]) + "/")
        empty_dirs = [info.filename for info in infolist if info.is_dir() and info.filename not in non_empty_dirs]
        if empty_dirs:
            raise ValueError(f"Empty directory entries: {empty_dirs[:5]}")
        groups = sorted(path for path in names if path.startswith(STACK_GROUPS))
        if groups:
            raise ValueError(f"Runtime-served stack groups packaged: {groups[:5]}")
        packaged = {path for path in names if is_tracked(path)}
        missing = sorted(expected.keys() - packaged)
        if missing:
            raise ValueError(f"Missing from JAR: {missing[:5]}")
        unexpected = sorted(packaged - expected.keys())
        if unexpected:
            raise ValueError(f"Unexpected JAR resources: {unexpected[:5]}")
        for path, payload in expected.items():
            if archive.read(path) != payload:
                raise ValueError(f"Resource bytes changed: {path}")
    static_json = sum(1 for path in expected if path.startswith(MODEL_DIRS))
    if static_json > max_static_json:
        raise ValueError(f"Static model/blockstate JSONs: {static_json} exceeds {max_static_json}")
    tables = sum(1 for path in expected if is_table(path))
    pngs = sum(1 for path in expected if path.endswith(".png"))
    print(
        f"JAR: {jar.stat().st_size:,} bytes; entries: {len(infolist)}; "
        f"static model/blockstate JSONs: {static_json}; TSV tables: {tables}; PNGs: {pngs}"
    )


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("jar", type=Path)
    parser.add_argument("--resources", nargs="+", type=Path, required=True)
    parser.add_argument("--max-static-json", type=int, default=450)
    args = parser.parse_args()
    try:
        verify(args.jar, args.resources, args.max_static_json)
    except (OSError, ValueError) as error:
        print(f"error: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
