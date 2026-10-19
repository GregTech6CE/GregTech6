"""List the `tech:<id>` tokens the transpiled GT6 loaders reference and classify them.

The transpiler turns every `IL.<Name>.get(n)` into a `tech:<snake_case>` spec
(tools/transpile_gt6_chem.py:160-173). `GTTechnological.get(id)` resolves those against the port's
own registry ids, so a GT6 item the port registers under a different id stays unresolved and its
recipe is counted as "accepted-missing content".

This script prints, for every referenced token:
  * OK        — the port registers that exact id
  * ALIASABLE — the port registers an id that is the same item under another spelling
  * MISSING   — no counterpart (real content gap)

Run:  python tools/audit_tech_item_tokens.py [--json docs/tech-item-tokens.json]
"""
from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
GENERATED = native_path(ROOT / "src/main/java/com/gregtech/gregtech")
TECH = GENERATED / "registry/GTTechnological.java"
MULTI = GENERATED / "registry/GTMultiItemsGen.java"

TOKEN = re.compile(r'"tech:([a-z0-9_]+):\d+"')


def port_ids() -> set[str]:
    text = TECH.read_text(encoding="utf-8")
    block = text.split("static final String[] IDS = {", 1)[1].split("};", 1)[0]
    ids = set(re.findall(r'"([a-z0-9_]+)"', block))
    # GT6's IL.* names are not all technological items: the generator modules and most of the food
    # items are multi-items (GTMultiItemsGen, transpiled from GT6's MultiItem* classes), and
    # GTGeneratedChem's ``tech:`` spec resolves those through ForgeRegistries - registered anyway.
    if MULTI.exists():
        entries = MULTI.read_text(encoding="utf-8").split("static final String[] ENTRIES = {", 1)[1]
        entries = entries.split("};", 1)[0]
        # the tooltip flag column is "T", "F" or "" depending on the entry
        ids |= set(re.findall(r'"([a-z0-9_]+)",\s*"[^"]*",\s*"[a-z]+",\s*"[TF]?"', entries))
    # ... and some are blocks (boomstick, dynamite, the rope variants) whose block item is what the
    # recipe refers to; every port registrar spells its ids in a register("id", ...) call, and the
    # rope/dynamite variants pass theirs to the registry helpers of the same name.
    for path in sorted((GENERATED / "registry").glob("*.java")):
        text = path.read_text(encoding="utf-8", errors="replace")
        ids |= set(re.findall(r'\.register\(\s*"([a-z0-9_]+)"', text))
        ids |= set(re.findall(r'\b(?:rope|dynamite)\(\s*"([a-z0-9_]+)"', text))
    return ids


def aliases() -> dict[str, str]:
    """The alias table GTTechnological.get() itself uses, so the audit cannot drift from it."""
    text = TECH.read_text(encoding="utf-8")
    # Comments inside the table may contain ");" (the module aliases explain their ids in a comment),
    # which would truncate a naive split.
    text = re.sub(r"//[^\n]*", "", text)
    head = 'public static final Map<String, String> ALIASES = Map.'
    block = text.split(head, 1)[1].split(");", 1)[0]
    return dict(re.findall(r'Map\.entry\("([a-z0-9_:]+)",\s*"([a-z0-9_:]+)"\)', block)) \
        or dict(re.findall(r'"([a-z0-9_]+)",\s*"([a-z0-9_]+)"', block))


def referenced() -> dict[str, int]:
    counts: dict[str, int] = {}
    catalog = ROOT / "core/src/main/java/com/gregtech/gregtech/content/recipe/generated"
    for path in sorted(catalog.glob("GT*Gen.java")):
        for token in TOKEN.findall(path.read_text(encoding="utf-8")):
            counts[token] = counts.get(token, 0) + 1
    return counts


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--json", default=None)
    args = parser.parse_args()

    ids = port_ids()
    alias = aliases()
    counts = referenced()
    ok: list[str] = []
    aliasable: list[tuple[str, str, int]] = []
    missing: list[tuple[str, int]] = []
    for token, count in sorted(counts.items(), key=lambda kv: (-kv[1], kv[0])):
        if token in ids:
            ok.append(token)
        elif token in alias and (alias[token] in ids or ":" in alias[token]):
            # a namespaced target is a vanilla item (bone meal, hay block, poisonous potato);
            # GTGeneratedChem resolves it through ForgeRegistries
            aliasable.append((token, alias[token], count))
        else:
            missing.append((token, count))

    print(f"referenced tech tokens: {len(counts)}  recipes: {sum(counts.values())}")
    print(f"  OK        {len(ok) + len(aliasable)} tokens / "
          f"{sum(counts[t] for t in ok) + sum(c for _, _, c in aliasable)} recipes")
    print(f"  MISSING   {len(missing)} tokens / {sum(c for _, c in missing)} recipes")
    print("\n-- resolved through GTTechnological.ALIASES (GT6 id -> port id, recipes) --")
    for token, port, count in aliasable:
        print(f"  {token:44} -> {port:44} {count}")
    print("\n-- missing (top 40 by recipe count) --")
    for token, count in missing[:40]:
        print(f"  {token:44} {count}")

    if args.json:
        out = Path(args.json)
        out.write_text(json.dumps({
            "referenced": counts,
            "ok": ok,
            "aliasable": [{"gt6": t, "port": p, "recipes": c} for t, p, c in aliasable],
            "missing": [{"id": t, "recipes": c} for t, c in missing],
        }, indent=2), encoding="utf-8")
        print(f"\nwrote {out}")


if __name__ == "__main__":
    main()
