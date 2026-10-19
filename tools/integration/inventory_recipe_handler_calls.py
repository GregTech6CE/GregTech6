"""Index explicit RM map handler registrations for the original-source audit.

Counts describe constructor call sites, not material-expanded recipe counts.
Loop bounds, outer conditions and indirect registrations still need review.
"""
from __future__ import annotations

import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import re

from check_source import java_source_view


def inventory(source: Path) -> dict:
    raw = source.read_bytes()
    code = java_source_view(raw.decode("utf-8-sig")).code
    pattern = re.compile(r"\bRM\.(\w+)\s*\.add\s*\(\s*new\s+(\w+)")
    maps = {}
    for match in pattern.finditer(code):
        entry = maps.setdefault(match[1], {"calls": [], "handler_classes": Counter()})
        entry["calls"].append({"line": code.count("\n", 0, match.start()) + 1, "handler": match[2]})
        entry["handler_classes"][match[2]] += 1
    return {
        "scope": __doc__.strip(),
        "source": str(source.resolve()),
        "source_sha256": hashlib.sha256(raw).hexdigest(),
        "call_sites": sum(len(entry["calls"]) for entry in maps.values()),
        "map_count": len(maps),
        "maps": dict(sorted(maps.items())),
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.output.resolve() == args.source.resolve():
        parser.error("Output must not overwrite the original source")
    report = inventory(args.source)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"call_sites": report["call_sites"], "maps": report["map_count"]}))


if __name__ == "__main__":
    main()
