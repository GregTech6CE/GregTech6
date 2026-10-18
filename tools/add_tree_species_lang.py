"""Add the language keys of the four new GT6 tree species (hazel, cinnamon, coconut, blue spruce)
to both language files.

Textual insertion instead of a json round-trip: the language files are hand-formatted one key per
line, and re-dumping them would rewrite every line. The new keys are inserted next to
``block.gregtech.sapling_rubber`` with the same indentation.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import os

LANG = os.path.join(str(native_path("src")), "main", "resources", "assets", "gregtech", "lang")
ANCHOR = '"block.gregtech.sapling_rubber":'
NEW = {
    "en_us.json": [
        ("sapling_hazel", "Sapling Hazel"),
        ("sapling_cinnamon", "Sapling Cinnamon"),
        ("sapling_coconut", "Sapling Coconut"),
        ("sapling_bluespruce", "Sapling Bluespruce"),
    ],
    "zh_cn.json": [
        ("sapling_hazel", "榛木树苗"),
        ("sapling_cinnamon", "桂皮树苗"),
        ("sapling_coconut", "椰木树苗"),
        ("sapling_bluespruce", "蓝云杉树苗"),
    ],
}

for name, entries in NEW.items():
    path = os.path.join(LANG, name)
    with open(path, encoding="utf-8") as handle:
        lines = handle.readlines()
    existing = [line for line in lines if '"block.gregtech.%s"' % entries[0][0] in line]
    if existing:
        print("%s: already has %d of the new keys, skipping" % (name, len(existing)))
        continue
    for index, line in enumerate(lines):
        if ANCHOR in line:
            indent = line[:len(line) - len(line.lstrip())]
            block = ["%s\"block.gregtech.%s\": \"%s\",\n" % (indent, key, value) for key, value in entries]
            lines[index:index] = block
            print("%s: inserted %d keys after line %d" % (name, len(block), index + 1))
            break
    else:
        raise SystemExit("%s: anchor %s not found" % (name, ANCHOR))
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        handle.writelines(lines)
