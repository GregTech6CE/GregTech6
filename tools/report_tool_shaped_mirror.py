"""Summarise docs/tool-shaped-mirror-report.json (written by HandCraftingTests).

The report has one entry per ``gt6ce:tool_shaped`` recipe in the running game: whether it mirrors,
its pattern shape, whether the pattern is symmetric and how many ingredients match no item at all.
This tool turns that into the numbers the mirror work is judged by.

Usage:  python tools/report_tool_shaped_mirror.py
"""

from __future__ import annotations

import collections
import json
import pathlib

REPORT = pathlib.Path("docs/tool-shaped-mirror-report.json")


def main() -> None:
    if not REPORT.exists():
        raise SystemExit(f"no report at {REPORT} - run the GameTest gate first")
    data = json.loads(REPORT.read_text(encoding="utf-8"))
    groups = collections.Counter()
    mirrors = collections.Counter()
    unresolved_rows = []
    for rid, entry in data.items():
        group = rid.split(":")[-1].split("/")[0]
        groups[group] += 1
        mirrors[(bool(entry["mirror"]), bool(entry["symmetric"]))] += 1
        if entry["unresolvedIngredients"]:
            unresolved_rows.append((rid, entry["shape"]))
    print(f"{len(data)} tool_shaped recipes")
    print("by group:", ", ".join(f"{k}={v}" for k, v in groups.most_common(8)))
    print("mirror x symmetric:")
    for (mirror, symmetric), count in sorted(mirrors.items()):
        print(f"   mirror={mirror!s:5} symmetric={symmetric!s:5} {count}")
    print(f"rows with an ingredient that matches nothing: {len(unresolved_rows)}")
    for rid, shape in unresolved_rows[:12]:
        print(f"   {rid}  {shape}")


if __name__ == "__main__":
    main()
