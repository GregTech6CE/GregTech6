"""Give every ``gregtech:tool_shaped`` recipe the mirror rule its GT6 row actually uses.

GT6 sets ``MIR`` per row (``gregapi/util/CR.java:131-163``: ``DEF = BUF|NO_REM``, ``DEF_MIR =
DEF|MIR``), and 93 of its 1188 ``CR.shaped`` rows opt in — the rest do not mirror. 1.20.1 mirrors
shaped recipes by default, so the port has to state the original's answer explicitly, otherwise two
rows that differ only by a mirrored tool symbol (the logistics export/storage buses) collapse into
one craftable recipe.

Provenance: ``docs/gt6-crafting-mirror-flags.json`` (``tools/census_gt6_recipe_mirror_flags.py``)
maps a pattern signature to "some GT6 row with this pattern allows mirroring". A recipe whose pattern
appears in that set keeps mirroring; every other one is written with ``allow_mirror: false``. Recipes
that state the flag by hand (the §16/§17 batches) are left alone; recipes this tool wrote carry
``"_mirror_flags": "gt6-census"`` so a later census can re-decide them.

Usage:  python tools/apply_recipe_mirror_flags.py [--check]
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
RECIPES = native_path(ROOT / "src/main/resources/data/gregtech/recipes")
CENSUS = ROOT / "docs/gt6-crafting-mirror-flags.json"
TOOL_SHAPED = "gregtech:tool_shaped"
MARKER = "_mirror_flags"
MARKER_VALUE = "gt6-census"


def signature(pattern: list[str]) -> str:
    """GT6 pads each row to three characters; the port keeps the original widths."""
    return "\n".join(row.rstrip() for row in pattern if row.strip())


def main() -> None:
    check_only = "--check" in sys.argv
    reset = "--reset" in sys.argv
    census = json.loads(CENSUS.read_text(encoding="utf-8"))
    mirror_patterns = set(census["mirrorPatterns"])

    written = kept = disallowed = allowed = hand_written = cleared = 0
    for path in sorted(RECIPES.rglob("*.json")):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (json.JSONDecodeError, UnicodeDecodeError):
            continue
        if data.get("type") != TOOL_SHAPED:
            continue
        deliberate = "_comment" in data or path.parent.name == "extruder_shapes"
        if reset and "allow_mirror" in data and not deliberate:
            data.pop("allow_mirror", None)
            data.pop(MARKER, None)
            path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            cleared += 1
            continue
        if "allow_mirror" in data and data.get(MARKER) != MARKER_VALUE and deliberate:
            hand_written += 1
            continue
        wanted = signature(data.get("pattern", [])) in mirror_patterns
        if data.get("allow_mirror") is wanted:
            kept += 1
            continue
        if check_only:
            written += 1
            continue
        data["allow_mirror"] = wanted
        data[MARKER] = MARKER_VALUE
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        written += 1
        allowed += 1 if wanted else 0
        disallowed += 1 if not wanted else 0
    verb = "would be written" if check_only else "updated"
    print(f"{cleared} flags cleared; {written} tool_shaped recipes {verb} "
          f"({allowed} mirror like the original, {disallowed} do not); {kept} already match, "
          f"{hand_written} are hand-written")


if __name__ == "__main__":
    main()
