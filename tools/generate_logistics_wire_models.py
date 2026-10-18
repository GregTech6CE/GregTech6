#!/usr/bin/env python3
"""Derive the 6/16 Logistics Wire arms from this port's existing laser-fiber geometry.

Both wires use GT6's six-connection shape. The source fiber is 4/16 wide; the
Logistics Wire registration specifies PX_P[6], so its square cross-section is 6/16.
No GTM source or texture is used.
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import json
import sys
from pathlib import Path

ROOT = native_path(Path(__file__).resolve().parents[1] / "src/main/resources/assets/gt6ce")
SOURCE = ROOT / "models/block/machine/energy"
TARGET = ROOT / "models/block/machine/logistics"


def transformed(source: Path) -> bytes:
    data = json.loads(source.read_text(encoding="utf-8"))

    def rewrite(value):
        if isinstance(value, dict):
            return {key: rewrite(item) for key, item in value.items()}
        if isinstance(value, list):
            return [rewrite(item) for item in value]
        if isinstance(value, str):
            return (value.replace("fiber_wire_overlay", "logistics_wire_overlay")
                    .replace("fiber_wire", "logistics_wire")
                    .replace("machine/energy/laser_fiber_", "machine/logistics/logistics_wire_"))
        return value

    data = rewrite(data)

    def widen(value):
        if isinstance(value, dict):
            if "from" in value and "to" in value and len(value["from"]) == 3:
                for key in ("from", "to"):
                    value[key] = [{6: 5, 10: 11}.get(v, v) for v in value[key]]
            for item in value.values():
                widen(item)
        elif isinstance(value, list):
            for item in value:
                widen(item)

    widen(data)
    return (json.dumps(data, indent=2, ensure_ascii=False) + "\n").encode("utf-8")


def main() -> int:
    check = "--check" in sys.argv[1:]
    outputs = {
        ROOT / "blockstates/logistics_wire.json": transformed(ROOT / "blockstates/laser_fiber_wire.json"),
        ROOT / "models/item/logistics_wire.json": transformed(ROOT / "models/item/laser_fiber_wire.json"),
    }
    for name in ("center", "down", "up", "north", "south", "west", "east"):
        outputs[TARGET / f"logistics_wire_{name}.json"] = transformed(SOURCE / f"laser_fiber_{name}.json")
    stale = [path for path, contents in outputs.items() if not path.exists() or path.read_bytes() != contents]
    if check:
        print("logistics wire models: OK" if not stale else "stale: " + ", ".join(str(p) for p in stale))
        return bool(stale)
    for path, contents in outputs.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(contents)
    print(f"wrote {len(outputs)} Logistics Wire models/blockstate files")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
