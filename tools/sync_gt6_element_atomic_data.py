"""Recover original GT6 atomic counts in the already-split ElementMaterials holder.

The old split-file generator treated several 7-number MT helpers as if they had no
atomic arguments. It emitted zero protons/neutrons, and made a few true elements
plain dusts. The source here is MT.java through transpile_gt6_materials' parser.

Usage: python tools/sync_gt6_element_atomic_data.py [--write]
Without --write, report discrepancies without touching the generated Java file.
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import pathlib
import re
import sys
from format_definition_tables import align_java_tables

import transpile_gt6_materials as source

HOLDER = native_path(pathlib.Path("src/main/java/com/gregtech/gregtech/content/material/generated/ElementMaterials.java"))
ROW = re.compile(r'^(    public static final GTMaterial \w+\s*=\s*)(.*?);$', re.M)
CALL = re.compile(r'^(metal|element|dust|gas)\((\d+),')
ATOMS = re.compile(r'^(?:metal|element)\(\d+,\s*"[^"]+",\s*"[^"]+",\s*(\d+),\s*(\d+),\s*')
DUST = re.compile(r'^dust\((\d+),\s*"([^"]+)",\s*(0x[0-9A-Fa-f]+)\)')


def main() -> int:
    _, materials = source.collect_materials(source.read_source())
    original: dict[int, tuple[str, int, int, str]] = {}
    for field, (factory, args, _line) in materials.items():
        parsed = source.parse_strings_and_numbers(source.split_args(args))
        numbers = parsed["numbers"]
        if (source.factory_kind(factory) not in {"metal", "element", "gas"}
                or not parsed.get("symbol") or len(numbers) < 6):
            continue
        material_id = parsed["id"]
        original.setdefault(material_id, (field, int(float(numbers[1])), int(float(numbers[2])),
                                          source.factory_kind(factory)))

    text = HOLDER.read_text(encoding="utf-8")
    fixes: list[str] = []

    def replace(match: re.Match[str]) -> str:
        prefix, body = match.groups()
        call = CALL.match(body)
        if not call:
            return match.group(0)
        java_kind, material_id = call.group(1), int(call.group(2))
        entry = original.get(material_id)
        if not entry:
            return match.group(0)
        field, protons, neutrons, gt6_kind = entry
        if java_kind in {"metal", "element"}:
            atoms = ATOMS.match(body)
            if not atoms:
                raise ValueError(f"Cannot read atomic constructor: {field} ({material_id})")
            if (int(atoms.group(1)), int(atoms.group(2))) != (protons, neutrons):
                body = body[:atoms.start(1)] + str(protons) + ", " + str(neutrons) + body[atoms.end(2):]
                fixes.append(f"{field}: {atoms.group(1)}/{atoms.group(2)} -> {protons}/{neutrons}")
        elif java_kind == "dust" and gt6_kind == "element":
            dust = DUST.match(body)
            stats = re.search(r'\.setStats\((\d+), (\d+), ([^)]+)\)', body)
            if not dust or not stats:
                raise ValueError(f"Cannot recover elemental dust: {field} ({material_id})")
            atomic_call = (f'element({material_id}, "{dust.group(2)}", '
                           f'"{field}", {protons}, {neutrons}, {stats.group(1)}, '
                           f'{stats.group(2)}, {stats.group(3)}, {dust.group(3)})')
            body = atomic_call + body[dust.end():]
            body = body.replace(stats.group(0), "", 1)
            fixes.append(f"{field}: dust -> element {protons}/{neutrons}")
        elif java_kind == "gas":
            expected = (f'.setAtomicProperties(new AtomicProperties({protons}, {protons}, {neutrons}, 0))'
                        '.put(MaterialProperty.ELEMENT)')
            if expected not in body:
                raise ValueError(f"Gas element {field} ({material_id}) still lacks exact GT6 atomic data")
        return prefix + body + ";"

    fixed = ROW.sub(replace, text)
    print(f"{len(fixes)} atomic discrepancies")
    for change in fixes:
        print("  " + change)
    if "--write" in sys.argv:
        HOLDER.write_text(align_java_tables(fixed), encoding="utf-8")
    return 0 if "--write" in sys.argv or not fixes else 1


if __name__ == "__main__":
    raise SystemExit(main())
