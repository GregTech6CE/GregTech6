#!/usr/bin/env python3
"""Extract every GT6 "Basic Machines" registration's crafting recipe from the
original 1.7.10 source (Loader_MultiTileEntities.java).

Each registration line looks like::

    aMat = MT.DATA.Heat_T[1];  aRegistry.add("Oven ("+aMat.getLocal()+")",
        "Basic Machines", 20001, 20001, aClass, aMat.mToolQuality, 16, aMachine,
        UT.NBT.make(NBT_MATERIAL, aMat, ...),
        "wMh", "BCB",
        'M', OP.casingMachine.dat(aMat), 'C', OP.plateDouble.dat(ANY.Cu),
        'B', Blocks.brick_block);

The trailing varargs after ``UT.NBT.make(...)`` are the shaped-crafting recipe:
the quoted strings are the pattern rows, then alternating character/key pairs.

Output: JSON to stdout (or --out PATH).
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

DEFAULT_SOURCE = Path(
    r"F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java"
    r"\gregtech\loaders\b\Loader_MultiTileEntities.java"
)


def match_paren(text: str, open_index: int) -> int:
    """Return the index of the ')' matching the '(' at open_index, ignoring
    parentheses inside string/char literals."""
    depth = 0
    i = open_index
    in_str: str | None = None
    while i < len(text):
        ch = text[i]
        if in_str:
            if ch == "\\":
                i += 2
                continue
            if ch == in_str:
                in_str = None
        elif ch in "\"'":
            in_str = ch
        elif ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
            if depth == 0:
                return i
        i += 1
    raise ValueError("unbalanced parentheses")


def split_top_level(text: str) -> list[str]:
    """Split a comma separated argument list at top nesting level."""
    parts: list[str] = []
    depth = 0
    in_str: str | None = None
    current: list[str] = []
    i = 0
    while i < len(text):
        ch = text[i]
        if in_str:
            current.append(ch)
            if ch == "\\":
                if i + 1 < len(text):
                    current.append(text[i + 1])
                    i += 2
                    continue
            elif ch == in_str:
                in_str = None
        elif ch in "\"'":
            in_str = ch
            current.append(ch)
        elif ch in "([{":
            depth += 1
            current.append(ch)
        elif ch in ")]}":
            depth -= 1
            current.append(ch)
        elif ch == "," and depth == 0:
            parts.append("".join(current).strip())
            current = []
        else:
            current.append(ch)
        i += 1
    tail = "".join(current).strip()
    if tail:
        parts.append(tail)
    return parts


def machine_key(display_expr: str) -> str:
    """'Oven (' + aMat.getLocal() + ')'  ->  'Oven'."""
    literal = re.match(r'\s*"([^"]*)"', display_expr)
    name = literal.group(1) if literal else display_expr
    return name.strip().rstrip("(").strip()


def tier_expr(line: str, add_index: int) -> str:
    prefix = line[:add_index]
    m = None
    for m in re.finditer(r"aMat\s*=\s*([^;]+);", prefix):
        pass
    return m.group(1).strip() if m else ""


def nbt_params(nbt_call: str) -> dict:
    """Machine parameters carried by the UT.NBT.make(...) argument."""
    def num(key: str, cast=float):
        m = re.search(re.escape(key) + r"\s*,\s*(-?[\d.]+)", nbt_call)
        return cast(m.group(1)) if m else None

    def text(key: str):
        m = re.search(re.escape(key) + r"\s*,\s*(TD\.Energy\.)?(\w+)", nbt_call)
        return m.group(2) if m else None

    return {
        "energy": text("NBT_ENERGY_ACCEPTED"),
        "energy2": text("NBT_ENERGY_ACCEPTED_2"),
        "input": num("NBT_INPUT", int),
        "inputMin": num("NBT_INPUT_MIN", int),
        "inputMax": num("NBT_INPUT_MAX", int),
        "parallel": num("NBT_PARALLEL", int),
        "parallelDuration": "NBT_PARALLEL_DURATION" in nbt_call,
        "hardness": num("NBT_HARDNESS"),
        "resistance": num("NBT_RESISTANCE"),
        "overflow": num("NBT_OVERFLOW", int),
        "texture": None,
    }


def parse_source(path: Path, tab: str = "Basic Machines") -> list[dict]:
    text = path.read_text(encoding="utf-8", errors="replace")
    out: list[dict] = []
    for m in re.finditer(r"aRegistry\.add\(", text):
        start = m.end() - 1
        try:
            end = match_paren(text, start)
        except ValueError:
            continue
        call = text[start + 1 : end]
        if f'"{tab}"' not in call:
            continue
        args = split_top_level(call)
        if len(args) < 2:
            continue
        display = args[0]
        # Find the UT.NBT.make(...) argument, then the recipe varargs after it.
        nbt_idx = next((i for i, a in enumerate(args) if a.startswith("UT.NBT.make(")), None)
        recipe_args = args[nbt_idx + 1 :] if nbt_idx is not None else []
        # Pattern rows are the LEADING quoted strings; the first character literal starts the
        # key/value pairs. (A quoted key value such as "gt:re-battery1" must not be read as a row.)
        patterns: list[str] = []
        for a in recipe_args:
            if re.fullmatch(r'"[^"]*"', a):
                patterns.append(a[1:-1])
            else:
                break
        keys: dict[str, str] = {}
        i = 0
        while i + 1 < len(recipe_args):
            a = recipe_args[i]
            km = re.fullmatch(r"'(.)'", a)
            if km:
                keys[km.group(1)] = recipe_args[i + 1]
                i += 2
            else:
                i += 1
        idm = re.search(r",\s*(\d+)\s*,", call)
        line_no = text.count("\n", 0, m.start()) + 1
        out.append(
            {
                "display": display,
                "machine": machine_key(display),
                "tier": tier_expr(text, m.start()),
                "registryId": idm.group(1) if idm else "",
                "pattern": patterns,
                "keys": keys,
                "params": nbt_params(args[nbt_idx]) if nbt_idx is not None else {},
                "line": line_no,
            }
        )
    return out


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--source", type=Path, default=DEFAULT_SOURCE)
    ap.add_argument("--out", type=Path, default=None)
    ap.add_argument("--tab", default="Basic Machines", help='registry tab, e.g. "Multiblock Machines"')
    args = ap.parse_args()

    if not args.source.exists():
        print(f"source not found: {args.source}", file=sys.stderr)
        return 2
    entries = parse_source(args.source, args.tab)
    payload = {
        "source": str(args.source),
        "tab": args.tab,
        "count": len(entries),
        "machines": sorted({e["machine"] for e in entries}),
        "entries": entries,
    }
    data = json.dumps(payload, ensure_ascii=False, indent=1)
    if args.out:
        args.out.write_text(data, encoding="utf-8")
        print(f"wrote {args.out} ({len(entries)} entries, {len(payload['machines'])} machines)")
    else:
        print(data)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
