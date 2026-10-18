"""List the item prefixes the port actually registers (MaterialPrefix.java definitions).

`MaterialPrefix` definitions use several helpers (`def`, `tex`, `child`, ...), so a plain
`name = def(` grep undercounts. This lists every `NAME = helper(` line so a row list written against
GT6's `OP` prefixes can be checked against the port before it is used.

Run:  python tools/list_material_prefixes.py [name ...]
"""
from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PREFIX = native_path(ROOT / "src/main/java/com/gregtech/gregtech/data/MaterialPrefix.java")

DEFINITION = re.compile(r"^\s{4,}(\w+)\s*=\s*(\w+)\(", re.M)


def main() -> None:
    text = PREFIX.read_text(encoding="utf-8")
    definitions = DEFINITION.findall(text)
    names = [name for name, _ in definitions]
    if len(sys.argv) > 1:
        for wanted in sys.argv[1:]:
            hit = [helper for name, helper in definitions if name == wanted]
            print(f"{wanted:28} {'defined via ' + hit[0] if hit else 'NOT DEFINED'}")
        return
    print(f"{len(names)} prefixes defined in MaterialPrefix.java")
    helpers: dict[str, int] = {}
    for _, helper in definitions:
        helpers[helper] = helpers.get(helper, 0) + 1
    print("helpers:", ", ".join(f"{k}={v}" for k, v in sorted(helpers.items())))
    print(", ".join(names))


if __name__ == "__main__":
    main()
