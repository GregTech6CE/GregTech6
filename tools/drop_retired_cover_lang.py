"""Drop the retired port-only cover items' language keys (one key per line, line-based so the rest of
the file is untouched)."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()

import pathlib

KEYS = (
    "item.gregtech.cover_conveyor",
    "item.gregtech.cover_pump",
    "item.gregtech.cover_redstone_repeater",
    "item.gregtech.cover_redstone_torch",
    "item.gregtech.cover_robot_arm",
    "item.gregtech.cover_tag_selector",
)

for name in ("en_us.json", "zh_cn.json"):
    path = native_path(pathlib.Path("src/main/resources/assets/gregtech/lang")) / name
    lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
    kept = [line for line in lines if not any(f'"{key}"' in line for key in KEYS)]
    removed = len(lines) - len(kept)
    if removed:
        path.write_text("".join(kept), encoding="utf-8")
    print(f"{name}: removed {removed} lines, {len(kept)} remain")
