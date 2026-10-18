"""Resolve GT6 material names to the port's material accessors (Materials.X / OreMaterials.X ...).

Usage: python tools/resolve_port_materials.py Lignite Coal Bauxite ...
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import glob
import io
import re
import sys

FILES = sorted(glob.glob(str(native_path("src/main/java/com/gregtech/gregtech/content/material/**/*.java")), recursive=True))


def main():
    names = sys.argv[1:]
    if not names:
        print(__doc__)
        return
    for name in names:
        found = []
        for path in FILES:
            text = io.open(path, encoding="utf-8", errors="replace").read()
            cls = path.replace("\\", "/").split("/")[-1][:-5]
            for match in re.finditer(
                    r'public static final GTMaterial\s+' + re.escape(name) + r'\s*=\s*([^;]+);', text):
                found.append((cls, match.group(1).strip()[:90]))
        if not found:
            print("%-16s ??" % name)
            continue
        for cls, decl in found:
            accessor = "OreMaterials" if cls == "OreMaterials" else cls
            print("%-16s %s.%s   <- %s" % (name, accessor, name, decl))
        # Aliases live in Materials.java; point them out explicitly.
        alias = [c for c, _ in found if c == "Materials"]
        if alias:
            print("%-16s (also exposed as Materials.%s)" % ("", name))


if __name__ == "__main__":
    main()
