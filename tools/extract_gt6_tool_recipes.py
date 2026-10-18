"""Extract GT6's tool crafting data from Loader_Tools.java.

Three families live there:

* ``new OreProcessing_Tool(TOOL, category, useNormalHandle, dismantleable, capacity, voltage,
  handleOverride, toolRecipes, toolHeadRecipes, V, W, X, Y, Z, condition)`` — the 8th argument holds
  the *shaped* recipe of the tool itself (e.g. WRENCH ``{"PhP"," P "," P "}``), the 9th the shaped
  recipe of its *head* (e.g. PICKAXE ``{"PII","f h"}`` / ``{"CGG","f  "}``).
* ``new AdvancedCraftingTool(TOOL, headPrefix, …)`` — the head + handle tools, which GT6 registers as
  a *shapeless* ore recipe (``ShapelessOreRecipe``) and which therefore need no pattern here.
* ``CR.shaped(ToolsGT.sMetaTool.getToolWithStats(TOOL, material, handle), flag, "pattern", …)`` — the
  early tools (flint, obsidian, stone, petrified wood, bone club), written once per material list.

Usage:  python tools/extract_gt6_tool_recipes.py [--json docs/gt6-tool-recipes.json] [--print]
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path, require_managed_writer
if __name__ == "__main__": require_managed_writer()


import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
GT6 = (native_path(ROOT / ".." / "gregtech6-master" / "gregtech6-master" / "src" / "main" / "java"
       / "gregtech" / "loaders" / "a" / "Loader_Tools.java")).resolve()

OP_TOOL = re.compile(r"new OreProcessing_Tool\(")
ADVANCED = re.compile(r"new AdvancedCraftingTool\(([^;]*?)\);", re.S)
EARLY = re.compile(r"CR\.shaped\(ToolsGT\.sMetaTool\.getToolWithStats\(([^;]*?)\);", re.S)
PATTERN_ROWS = re.compile(r'"([^"]{1,3})"')


def call_args(text: str, start: int) -> list[str]:
    """Top-level comma separated arguments of the call whose '(' is at ``start``."""
    depth = 0
    args: list[str] = []
    current: list[str] = []
    index = start
    while index < len(text):
        char = text[index]
        if char in "({[":
            depth += 1
            if depth == 1:
                index += 1
                continue
        elif char in ")}]":
            depth -= 1
            if depth == 0:
                args.append("".join(current).strip())
                return args
        if depth == 1 and char == ",":
            args.append("".join(current).strip())
            current = []
        else:
            current.append(char)
        index += 1
    return args


def patterns(argument: str) -> list[list[str]]:
    if "new String[][]" not in argument:
        return []
    body = argument[argument.index("new String[][]") + len("new String[][]"):]
    out: list[list[str]] = []
    for group in re.finditer(r"\{([^{}]*)\}", body):
        rows = PATTERN_ROWS.findall(group.group(1))
        if rows:
            out.append(rows)
    return out


def flag(argument: str) -> bool:
    return argument.strip() == "T"


def handle_override(argument: str) -> str | None:
    match = re.search(r"MT\.(\w+)", argument)
    return match.group(1) if match else None


def extract_early(text: str) -> list[dict]:
    """GT6's hand-written early tool rows: ``CR.shaped(getToolWithStats(TOOL, m, h), FLAG, "rows"…, keys…)``."""
    out: list[dict] = []
    loop = ""
    for line in text.splitlines():
        header = re.search(r"for \(OreDictMaterial (\w+) : (.*)\) \{", line)
        if header:
            loop = header.group(2).strip()
        for match in re.finditer(r"CR\.shaped\(", line):
            args = call_args(line, match.end() - 1)
            if not args or "getToolWithStats" not in args[0]:
                continue
            head = re.search(r"getToolWithStats\(\s*([A-Za-z_][\w.]*)\s*,\s*([^,]+?)\s*,\s*([^)]+)\)", args[0])
            if not head:
                continue
            rows: list[str] = []
            keys: list[str] = []
            for argument in args[2:]:
                if re.fullmatch(r"'.'", argument):
                    keys.append(argument)                       # the key map starts here
                    continue
                if keys:
                    keys.append(argument)
                else:
                    rows.extend(PATTERN_ROWS.findall(argument))
            key_map = {keys[i].strip("'"): keys[i + 1] for i in range(0, len(keys) - 1, 2)}
            out.append({"tool": head.group(1).split(".")[-1], "material": head.group(2).strip(),
                        "handle": head.group(3).strip(), "rows": rows,
                        "flag": "DEF_MIR" if "DEF_MIR" in args[1] else "DEF",
                        "item": key_map.get("X", ""), "loop": loop})
    return out


def extract() -> dict:
    text = GT6.read_text(encoding="utf-8")
    tools: dict[str, dict] = {}
    for match in OP_TOOL.finditer(text):
        args = call_args(text, match.end() - 1)
        if len(args) < 9:
            continue
        name = args[0].strip()
        entry = tools.setdefault(name, {})
        entry["useNormalHandle"] = flag(args[2])
        entry["toolRecipes"] = patterns(args[7])
        entry["headRecipes"] = patterns(args[8])
        if handle_override(args[6]):
            entry["handleOverride"] = handle_override(args[6])
    advanced: list[dict] = []
    for match in ADVANCED.finditer(text):
        args = [a.strip() for a in match.group(1).split(",")]
        if len(args) < 2:
            continue
        advanced.append({"tool": args[0], "head": args[1],
                         "suggested": handle_override(args[2]) if len(args) > 2 else None})
    early = extract_early(text)
    return {"tools": tools, "advanced": advanced, "early": early}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--json", default="")
    parser.add_argument("--print", action="store_true")
    args = parser.parse_args()

    data = extract()
    shaped = {name: entry for name, entry in data["tools"].items() if entry.get("toolRecipes")}
    heads = {name: entry for name, entry in data["tools"].items() if entry.get("headRecipes")}
    print(f"OreProcessing_Tool rows       : {len(data['tools'])} tools")
    print(f"  with a shaped tool recipe   : {len(shaped)} -> {sorted(shaped)}")
    print(f"  with a shaped head recipe   : {len(heads)} -> {sorted(heads)}")
    print(f"AdvancedCraftingTool (shapeless heads+handle): {len(data['advanced'])}")
    print(f"early CR.shaped tool rows     : {len(data['early'])}")
    if args.print:
        for name, entry in sorted(shaped.items()):
            print(f"  {name:16} tool={entry['toolRecipes']} handle={entry['useNormalHandle']}")
        for name, entry in sorted(heads.items()):
            print(f"  {name:16} head={entry['headRecipes']}")
        for row in data["advanced"]:
            print(f"  advanced {row['tool']:16} head={row['head']} suggested={row['suggested']}")
        for row in data["early"]:
            print(f"  early {row['tool']:16} {row['material']:18} handle={row['handle']:22} {row['rows']} {row['flag']}")
    if args.json:
        out = (ROOT / args.json).resolve()
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(json.dumps(data, indent=2, sort_keys=True), encoding="utf-8")
        print(f"wrote {out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
