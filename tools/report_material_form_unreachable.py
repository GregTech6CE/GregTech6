"""Final completeness check: which port materials still cannot reach GT6 form flags.

``report_material_form_lookup.py`` only joins materials that exist in *both* trees by GT6 id. This
script asks the runtime question for **every** port material declaration it can parse: would
``MaterialForms.of()`` (id first, name second) return anything? The remainder rely on the port's own
material properties — that is expected for the materials the port invented, but a *large* remainder
would mean the import missed a naming scheme.

Usage:  python tools/report_material_form_unreachable.py [--limit N]
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import pathlib
import re
import sys

PORT_MATERIALS = native_path(pathlib.Path("src/main/java/com/gregtech/gregtech/content/material"))
FORMS = native_path(pathlib.Path("src/main/java/com/gregtech/gregtech/data/generated/MaterialForms.java"))
PORT_DECL = re.compile(r"GTMaterial\s+(\w+)\s*=\s*\w+\(\s*(\d+)\s*,\s*\"([^\"]+)\"", re.M)
ROW = re.compile(r'^\s+"([^"|]+)\|([A-Z_,]*)"', re.M)


def normalize(name: str) -> str:
    return "".join(c for c in name.lower() if c.isalnum())


def main() -> None:
    limit = 30
    if "--limit" in sys.argv:
        limit = int(sys.argv[sys.argv.index("--limit") + 1])

    rows = ROW.findall(FORMS.read_text(encoding="utf-8"))
    id_keys = {key for key, _ in rows if key.isdigit()}
    name_keys = {key for key, _ in rows if not key.isdigit()}

    port: dict[int, tuple[str, str]] = {}
    for path in PORT_MATERIALS.rglob("*.java"):
        for name, port_id, display in PORT_DECL.findall(path.read_text(encoding="utf-8", errors="replace")):
            port.setdefault(int(port_id), (name, display))

    reachable = unreachable = 0
    sample = []
    for port_id, (name, display) in sorted(port.items()):
        if str(port_id) in id_keys or normalize(display) in name_keys or normalize(name) in name_keys:
            reachable += 1
        else:
            unreachable += 1
            sample.append((port_id, name, display))
    print(f"port material declarations: {len(port)}")
    print(f"  flags reachable (id or name): {reachable}")
    print(f"  no GT6 flags (port-only materials rely on their own properties): {unreachable}")
    for port_id, name, display in sample[:limit]:
        print(f"    id {port_id:5} {name:26} {display}")


if __name__ == "__main__":
    main()
