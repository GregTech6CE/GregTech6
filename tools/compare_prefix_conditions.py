"""Compare GT6's ore-prefix conditions (``OP.java``) with the port's predicates (``MaterialPrefix``).

GT6 spells each prefix's condition as a boolean expression over *item-generator flags*
(``wireFine = … .setCondition(new Or(WIRES, new And(PARTS, SMITHABLE)))``), where a token may be

  * a generator flag (``PLATES``, ``PROJECTILES``, …; the ones ``MaterialForms`` carries),
  * another prefix (``gem``, ``nugget``, ``toolHeadArrow`` — conditions compose),
  * a material *tag* that is not a generator flag (``SMITHABLE``, ``CRYSTAL``, ``TRANSPARENT``,
    ``STRETCHY``, ``BOUNCY``, ``BRITTLE``, ``STONE``, ``PEARL``),
  * a helper call (``typemin(1)``).

The port approximates the same conditions with predicates over ``MaterialProperty`` plus the imported
flag table, and factors them into ``HAS_*`` helpers — so a flag counts as consulted when it appears in
the prefix body **or** anywhere in the helper it calls, transitively.

This tool balances parentheses (nested ``And(Or(…), …)`` is common), expands the port's helpers, and
writes ``docs/prefix-condition-comparison.json`` with, per shared prefix: GT6's flags/prefix refs/tags,
the port's flags/properties, and the generator flags the port never consults.

Usage:  python tools/compare_prefix_conditions.py
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import json
import pathlib
import re

GT6 = pathlib.Path(r"F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java")
PREFIX = GT6 / "gregapi/data/OP.java"
PORT = native_path(pathlib.Path("src/main/java/com/gregtech/gregtech/data/MaterialPrefix.java"))
PORT_REGISTRY = native_path(pathlib.Path("src/main/java/com/gregtech/gregtech/data/MaterialPrefixes.java"))
PORT_BLOCKS = native_path(pathlib.Path("src/main/java/com/gregtech/gregtech/api/prefix/BlockMaterialPrefix.java"))
FORMS = native_path(pathlib.Path("src/main/java/com/gregtech/gregtech/data/generated/MaterialForms.java"))
OUT = pathlib.Path("docs/prefix-condition-comparison.json")

GT6_ENTRY = re.compile(r"^\s*(\w+)\s*=\s*create\(", re.M)
SET_CONDITION = re.compile(r"\.setCondition\(")
PORT_ENTRY = re.compile(r"^\s*(\w+)\s*=\s*(\w+)\(\s*\"(\w+)\"", re.M)
# The port's *registry* (``MaterialPrefixes``) is the authoritative name list: it holds the item
# prefixes that delegate to ``MaterialPrefix`` and the block prefixes that delegate to
# ``BlockMaterialPrefix`` (``block("blockGem")``), so comparing against ``MaterialPrefix`` alone
# reported families such as pipes, cables, ore variants or block forms as missing.
PORT_REGISTERED = re.compile(r'\b(?:register|block)\(\s*"(\w+)"')
# Every predicate the port factors a condition into — ``HAS_*`` (properties) and ``GT6_*`` (flags) both.
# The terminator allows a trailing ``// comment`` (``… has(m, "PLANTS");   // GT6 OP.plantGt* = PLANTS``),
# otherwise a definition's body swallows the next one.
PORT_HELPER = re.compile(
    r"Predicate<GTMaterial>\s+(\w+)\s*=\s*(.*?);(?:\s*//[^\n]*)?\s*$", re.S | re.M)
TOKEN = re.compile(r"\b([A-Za-z_]\w*)\b")

OPERATORS = {"And", "Or", "Not", "AndNot", "NAND", "XOR", "new", "TRUE", "FALSE", "T", "F",
             "typemin", "typemax", "if", "return", "m"}
# ``EMPTY`` is GT6's sentinel for "no requirement" (only the Empty material carries it), so
# ``Or(toolHeadArrow, EMPTY)`` means "has arrow heads" — treating it as a required flag would report a
# gap for every prefix that uses it.
SENTINELS = {"EMPTY"}
# Tokens that are neither generator flags nor tags: GT6 helper predicates / java noise.
HELPER_WORDS = {"typemin", "Empty", "isEmpty", "length", "size"}


def balanced(text: str, start: int, open_ch: str = "(", close_ch: str = ")") -> str:
    depth = 0
    for index in range(start, len(text)):
        if text[index] == open_ch:
            depth += 1
        elif text[index] == close_ch:
            depth -= 1
            if depth == 0:
                return text[start:index + 1]
    return text[start:]


def generator_flags() -> set[str]:
    text = FORMS.read_text(encoding="utf-8")
    line = re.search(r"FLAGS = java\.util\.List\.of\((.*?)\);", text, re.S)
    if not line:
        return set()
    return set(re.findall(r'"([A-Z_]+)"', line.group(1)))


def gt6_prefixes() -> dict[str, str]:
    """Prefix name -> its condition expression (balanced), for every ``create(...)`` entry."""
    text = PREFIX.read_text(encoding="utf-8", errors="replace")
    out: dict[str, str] = {}
    for match in GT6_ENTRY.finditer(text):
        condition = SET_CONDITION.search(text, match.end(), match.end() + 6000)
        if not condition:
            continue
        out[match.group(1)] = balanced(text, condition.end() - 1)[1:-1]
    return out


def classify(expression: str, prefix_names: set[str], flags: set[str]):
    tokens = [token for token in TOKEN.findall(expression)
              if token not in OPERATORS and token not in SENTINELS]
    used_flags = {token for token in tokens if token in flags}
    refs = {token for token in tokens if token in prefix_names and token != ""}
    tags = {token for token in tokens
            if token not in flags and token not in prefix_names and token not in HELPER_WORDS}
    return used_flags, refs, tags


def port_helpers() -> dict[str, str]:
    """Predicate helpers from both files: ``MaterialPrefix`` (item forms) and ``MaterialPrefixes``
    (block forms define theirs as locals inside ``defineBlocks()``, e.g. ``hasSmithableParts``)."""
    text = PORT.read_text(encoding="utf-8") + "\n" + PORT_REGISTRY.read_text(encoding="utf-8")
    return {name: body for name, body in PORT_HELPER.findall(text)}


def expand(body: str, helpers: dict[str, str], seen: set[str]) -> set[str]:
    """Generator flags consulted by a predicate body, following the port's helpers transitively.

    Helper names are matched by lookup (``hasSmithableParts``, ``HAS_DUST``, ``GT6_GEMS`` …) because
    the port's naming is not uniform — the block forms define theirs as lowercase locals.
    """
    flags = set(re.findall(r'MaterialForms\.has\(m,\s*"(\w+)"\)', body))
    for name, helper_body in helpers.items():
        if name in seen or not re.search(r"\b" + re.escape(name) + r"\b", body):
            continue
        seen.add(name)
        flags |= expand(helper_body, helpers, seen)
    return flags


def port_prefixes(flags: set[str]):
    """Per-prefix condition terms for every name the port's registry declares.

    Item prefixes carry their condition in ``MaterialPrefix`` (``def("registry", …)``); block prefixes
    carry theirs in ``MaterialPrefixes`` (``block("blockGem")`` builder chains, which may delegate to an
    item prefix by name) — both are expanded through the port's predicate helpers.
    """
    text = PORT.read_text(encoding="utf-8")
    entries = list(PORT_ENTRY.finditer(text))
    helpers = port_helpers()
    out = {}
    for index, match in enumerate(entries):
        end = entries[index + 1].start() if index + 1 < len(entries) else len(text)
        body = text[match.start():end]
        out[match.group(1)] = {
            "flags": sorted(expand(body, helpers, set())),
            "properties": sorted(set(re.findall(r"MaterialProperty\.(\w+)", body))),
            "helpers": sorted(set(re.findall(r"\b([A-Z][A-Z0-9_]*\w*)\b", body))),
        }

    registry = PORT_REGISTRY.read_text(encoding="utf-8")
    names = PORT_REGISTERED.findall(registry)
    blocks = list(re.finditer(r"\bblock\(\"(\w+)\"\)", registry))
    for index, match in enumerate(blocks):
        name = match.group(1)
        end = blocks[index + 1].start() if index + 1 < len(blocks) else len(registry)
        body = registry[match.start():end]
        flags_found = expand(body, helpers, set())
        # block conditions often delegate to an item prefix (``condition(MaterialPrefix.oreRaw)``)
        for delegate in set(re.findall(r"MaterialPrefix\.(\w+)", body)):
            flags_found |= set(out.get(delegate, {}).get("flags", []))
        out.setdefault(name, {
            "flags": sorted(flags_found),
            "properties": sorted(set(re.findall(r"MaterialProperty\.(\w+)", body))),
            "helpers": sorted(set(re.findall(r"\b([A-Z][A-Z0-9_]*\w*)\b", body))),
        })
    for name in names:
        out.setdefault(name, {"flags": [], "properties": [], "helpers": []})
    return out


def port_names() -> set[str]:
    """Every prefix name the port knows: registry entries, item delegates and block delegates.

    The registry (*MaterialPrefixes*) is the front door. Include delegates too so the audit also
    catches a field that exists in Java but has not been entered into the registry.
    """
    names = set(PORT_REGISTERED.findall(PORT_REGISTRY.read_text(encoding="utf-8")))
    names |= set(re.findall(r"MaterialPrefix\s+(\w+)\s*;", PORT.read_text(encoding="utf-8")))
    names |= set(re.findall(r"BlockMaterialPrefix\s+(\w+)\s*;",
                            PORT_BLOCKS.read_text(encoding="utf-8")))
    return names


def dead_flags() -> set[str]:
    """Flags the generated table declares but no material carries (dead conditions in the original)."""
    text = FORMS.read_text(encoding="utf-8")
    declared = re.search(r"FLAGS = java\.util\.List\.of\((.*?)\);", text, re.S)
    flags = set(re.findall(r'"([A-Z_]+)"', declared.group(1))) if declared else set()
    carried: set[str] = set()
    for row in re.findall(r'^\s+"[^"|]+\|([A-Z_,]*)"', text, re.M):
        carried |= {flag for flag in row.split(",") if flag}
    return flags - carried


def main() -> None:
    flags = generator_flags()
    dead = dead_flags()
    gt6 = gt6_prefixes()
    port = port_prefixes(flags)
    names = port_names()
    shared = sorted(set(gt6) & names)

    conditions = {}
    missing = []
    dead_only = []
    parsed = 0
    for name in shared:
        used_flags, refs, tags = classify(gt6[name], set(gt6), flags)
        terms = port.get(name, {"flags": [], "properties": [], "helpers": []})
        has_condition = bool(terms["helpers"]) or bool(terms["properties"])
        parsed += 1 if has_condition else 0
        consulted = set(terms["flags"])
        # A condition written over a dead flag (DIRTY_DUSTS, PIPES, TOOLS …) is false in the original:
        # aligning to it would invent content, so only live flags count as gaps.
        live = used_flags - dead
        absent = sorted(live - consulted) if has_condition else []
        if has_condition and used_flags & dead:
            dead_only.append((name, sorted(used_flags & dead)))
        conditions[name] = {
            "gt6Expression": gt6[name],
            "gt6Flags": sorted(used_flags),
            "gt6DeadFlags": sorted(used_flags & dead),
            "gt6PrefixRefs": sorted(refs),
            "gt6Tags": sorted(tags),
            "portFlags": terms["flags"],
            "portProperties": terms["properties"],
            "portConditionParsed": has_condition,
            "missingFlags": absent,
        }
        if absent:
            missing.append((name, sorted(live), absent, terms["flags"]))

    report = {
        "gt6Prefixes": len(gt6),
        "portPrefixNames": len(names),
        "shared": len(shared),
        "sharedWithParsedPortCondition": parsed,
        "generatorFlags": sorted(flags),
        "deadFlags": sorted(dead),
        "gt6Only": sorted(set(gt6) - names),
        "portOnly": sorted(names - set(gt6)),
        "prefixesWithMissingFlags": len(missing),
        "conditions": conditions,
    }
    OUT.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    print(f"GT6 prefixes with a condition: {len(gt6)}; port prefix names: {len(names)}; shared: {len(shared)}")
    print(f"  shared prefixes whose port condition was parsed here: {parsed}")
    print(f"GT6-only prefixes: {len(report['gt6Only'])} ({report['gt6Only'][:12]})")
    print(f"port-only prefixes: {len(report['portOnly'])} (e.g. {report['portOnly'][:8]})")
    print(f"dead flags (declared, carried by no material): {len(dead)}: {', '.join(sorted(dead))}")
    print(f"shared prefixes conditioned on a dead flag (never aligned): {len(dead_only)}: "
          + ", ".join(f"{name}({','.join(f)})" for name, f in dead_only))
    print(f"shared prefixes whose live GT6 flags the port never consults: {len(missing)}")
    for name, used, absent, consulted in missing:
        print(f"  {name:20} GT6 {','.join(used):45} missing {','.join(absent):22} port {','.join(consulted)}")
    print("wrote", OUT)


if __name__ == "__main__":
    main()
