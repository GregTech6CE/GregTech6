#!/usr/bin/env python3
"""Reproduce GT6's per-material item-generator form table from MT.java + TD.java.

Usage: python tools/extract_gt6_form_flags.py --source <GT6>/src/main/java [--check] [--json PATH]

GT6 decides which item forms a material has through ``TD``'s ``ITEMGENERATOR.*`` TagData flags
(``TD.java:557-580``) and the ``G_*`` form sets built from them (``TD.java:588-613``): a material
declaration passes a set such as ``G_GEM_ORES`` or the individual flags ``PLATES``/``INGOTS``/
``MULTIPLATES`` as "random data" arguments, and GT6's item generators register exactly those forms.

The port instead derives its item forms from its own material properties, which is why forms GT6 has
are missing (e.g. the quartz family's gems/plates/sticks, ``ASBESTOS``' plates, ``PAPER``'s
multi-plates). This script imports the original's answer as a generated table so the port's prefix
conditions can consult it instead of guessing.

The table is keyed by **GT6 material id** (authoritative: ``MT.java`` passes the id as the first
factory argument and the port carries the same id) and, for names that are unambiguous, by the
normalised GT6 field name. Name-only keying was wrong twice over: ``MT.Ke`` is *Trinium* (the port
looks materials up by display name, so the flags of materials such as Trinium, Naquadah or every
renamed wood could never be found), and field names collide after normalisation (``Co`` cobalt vs
``CO`` carbon monoxide, ``Gold`` the wood vs the ``gold()`` factory), which let a pseudo-material
shadow a real one.

Source line references are pinned to GT6 3703e40308c8c030763fd6297dea8b210d2a77b1; an unreviewed
``MT.java``/``TD.java`` fails closed instead of silently re-keying the table. Line endings are
normalised before hashing so a CRLF checkout of the same commit still matches.
"""

from __future__ import annotations

import argparse
import collections
import hashlib
import json
import os
import re
import sys

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
OUT_JAVA = os.path.join(ROOT, "core", "src", "main", "java", "gregtech", "content", "data",
                        "generated", "MaterialForms.java")

PINNED = {
    "MT.java": "12936831b1b5f419dc783c5f32f3cc07a329734ca6c5c11757d52e1be27904b2",
    "TD.java": "c77f9a0691e59b9c720627a49fe0088d31d8ffe3c79c4e59a6c61ced91b9c9ea",
}

SINGLE_FLAG = re.compile(r"(\w+)\s*=\s*TagData\.createTagData\(\"ITEMGENERATOR\.(\w+)\"")
FORM_SET = re.compile(r"(G_\w+)\s*=\s*new TagData\[\]\s*\{([^}]*)\}")
# A *declaration* assigns to a name (`public static final OreDictMaterial X = …`); a factory
# definition (`static OreDictMaterial gold () {`) does not, and treating those as declarations is what
# produced pseudo-materials such as `gold` or `metal_`.
DECL_STATIC = re.compile(r"OreDictMaterial\s+([A-Za-z_][A-Za-z0-9_]*)\s*=")
DECL_ENUM = re.compile(r"^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=")
DEFINITION = re.compile(
    r"^\s*(?:@\w+\s*)?(?:(?:public|protected|private|static|final|synchronized)\s+)*"
    r"OreDictMaterial\s+\w+\s*\(")
# The GT6 material id: the first argument of the declaration's factory call (`element( 1260, …`).
ID_LITERAL = re.compile(r"=\s*\w+\s*\(\s*(\d+)\s*,")
# … and inside a factory body, where the call is not an assignment (`return noblemetal( 790, …)`).
CALL_ID = re.compile(r"(?<![.\w])\w+\s*\(\s*(\d+)\s*,")
IDENT = re.compile(r"(?<![A-Za-z0-9_.])([A-Z][A-Z0-9_]{2,})(?![A-Za-z0-9_])")
FACTORY = re.compile(r"static OreDictMaterial\s+(\w+)\s*\(")
TARGETED = re.compile(r"\b(\w+)\.(?:put|add|mToThis\.add)\(\s*(G_\w+|\w+)\b")


def normalize(name: str) -> str:
    return re.sub(r"[^a-z0-9]", "", name.lower())


def read_source(path: str) -> str:
    """Read a pinned source file, normalising line endings before hashing and parsing."""
    with open(path, "rb") as handle:
        raw = handle.read().replace(b"\r\n", b"\n")
    expected = PINNED.get(os.path.basename(path))
    if expected and hashlib.sha256(raw).hexdigest() != expected:
        raise SystemExit("Unreviewed %s version; expected GT6 3703e403" % os.path.basename(path))
    return raw.decode("utf-8", errors="replace")


def statements(text: str) -> list[str]:
    """Join physical lines into declaration statements by paren balance."""
    out: list[str] = []
    buf = ""
    depth = 0
    for raw in text.splitlines():
        line = raw.split("//")[0]
        if not buf and not line.strip():
            continue
        buf += line + "\n"
        depth += line.count("(") - line.count(")")
        if depth <= 0:
            if buf.strip():
                out.append(buf)
            buf = ""
            depth = 0
    if buf.strip():
        out.append(buf)
    return out


def parse_td(text: str) -> dict[str, set[str]]:
    """TagData identifier -> set of ITEMGENERATOR flag names."""
    table: dict[str, set[str]] = {}
    for name, flag in SINGLE_FLAG.findall(text):
        table.setdefault(name, set()).add(flag)
    sets = FORM_SET.findall(text)
    # resolve form sets, which may reference other form sets or single flags
    pending = {name: [m.strip() for m in members.split(",") if m.strip()] for name, members in sets}
    for _ in range(6):   # a handful of passes is enough for the original's nesting depth
        changed = False
        for name, members in list(pending.items()):
            resolved: set[str] = set()
            complete = True
            for member in members:
                member = member.split(".")[-1]
                if member in table:
                    resolved |= table[member]
                elif member in pending:
                    complete = False
                    resolved |= table.get(member, set())
                # anything else is a non-generator flag (MORTAR, TRANSPARENT, ...) -> ignored
            if resolved or complete:
                if table.get(name) != resolved:
                    table[name] = resolved
                    changed = True
        if not changed:
            break
    return table


def balanced(text: str, start: int, opener: str = "{", closer: str = "}") -> str:
    """Text from ``start`` (which must hold ``opener``) through its matching ``closer``."""
    depth = 0
    for index in range(start, len(text)):
        if text[index] == opener:
            depth += 1
        elif text[index] == closer:
            depth -= 1
            if depth == 0:
                return text[start : index + 1]
    return text[start:]


def split_args(argument_list: str) -> list[str]:
    """Top-level comma split of an argument list (no surrounding parentheses)."""
    parts, depth, current = [], 0, ""
    for char in argument_list:
        if char in "([{":
            depth += 1
        elif char in ")]}":
            depth -= 1
        if char == "," and depth == 0:
            parts.append(current)
            current = ""
        else:
            current += char
    if current.strip():
        parts.append(current)
    return [p.strip() for p in parts if p.strip()]


def balanced_body(text: str, start: int) -> str:
    depth = 0
    for index in range(start, len(text)):
        if text[index] == "{":
            depth += 1
        elif text[index] == "}":
            depth -= 1
            if depth == 0:
                return text[start : index + 1]
    return text[start:]


def factory_overloads(text: str) -> dict[str, list[tuple[list[str], bool, str]]]:
    """Factory name -> [(parameter list, is-varargs, body)].

    GT6 overloads its material factories and the overload decides the form set: ``metal()`` from an
    element gets ``G_INGOT_ORES`` (the material has ore blocks) while the alloy overload only gets
    ``G_INGOT``. Resolving a factory to a single flag set would therefore hand every alloy an ore —
    the declaration's argument count picks the right overload instead.
    """
    out: dict[str, list[tuple[list[str], bool, str]]] = {}
    for match in FACTORY.finditer(text):
        open_paren = match.end() - 1
        signature = balanced(text, open_paren, "(", ")")
        params = split_args(signature[1:-1])
        varargs = bool(params) and "..." in params[-1]
        body = balanced_body(text, text.index("{", open_paren + len(signature)))
        out.setdefault(match.group(1), []).append((params, varargs, body))
    return out


def accepts(params: list[str], varargs: bool, nargs: int) -> bool:
    if varargs:
        return nargs >= len(params) - 1
    return nargs == len(params)


def factory_calls(body: str, overloads) -> list[tuple[str, int]]:
    """(factory name, argument count) for every call in ``body`` that names a factory."""
    calls = []
    for match in re.finditer(r"(?<![.\w])(\w+)\s*\(", body):
        name = match.group(1)
        if name not in overloads:
            continue
        signature = balanced(body, match.end() - 1, "(", ")")
        calls.append((name, len(split_args(signature[1:-1]))))
    return calls


def resolve_flags(name: str, nargs: int, overloads, table, memo, depth: int = 0) -> set[str]:
    """Form flags of one factory call with ``nargs`` arguments, following its delegation chain."""
    if depth > 6:
        return set()
    key = (name, nargs)
    if key in memo:
        return memo[key]
    memo[key] = set()                        # guards cycles between factories
    flags: set[str] = set()
    for params, varargs, body in overloads.get(name, []):
        if not accepts(params, varargs, nargs):
            continue
        for ident in IDENT.findall(body):
            flags |= table.get(ident, set())
        for callee, callee_args in factory_calls(body, overloads):
            flags |= resolve_flags(callee, callee_args, overloads, table, memo, depth + 1)
    memo[key] = flags
    return flags


def resolve_id(name: str, nargs: int, overloads, memo: dict, depth: int = 0) -> int | None:
    """GT6 material id of one factory call, following its delegation chain.

    ``Au = gold()`` carries no id in the declaration: ``gold()`` returns ``noblemetal(790, "Gold",
    "Au", …)``. Without following that chain the id lookup would miss every material that the
    original declares through a helper factory.
    """
    if depth > 6:
        return None
    key = (name, nargs)
    if key in memo:
        return memo[key]
    memo[key] = None                          # guards cycles between factories
    for params, varargs, body in overloads.get(name, []):
        if not accepts(params, varargs, nargs):
            continue
        literal = CALL_ID.search(body)
        if literal:
            value = int(literal.group(1))
            if value > 0:
                memo[key] = value
                return value
        for callee, callee_args in factory_calls(body, overloads):
            value = resolve_id(callee, callee_args, overloads, memo, depth + 1)
            if value:
                memo[key] = value
                return value
    return None


def collect(mt_text: str, table: dict[str, set[str]]):
    """Per-declaration form data plus the factory report for the JSON summary."""
    overloads = factory_overloads(mt_text)
    memo: dict[tuple[str, int], set[str]] = {}
    id_memo: dict = {}

    materials: dict[str, dict] = {}
    multi = 0
    unresolved = 0
    targeted: list[tuple[str, set[str]]] = []
    for stmt in statements(mt_text):
        # a bare "X.put(G_..., ...)" adds forms to an already declared material
        targets = TARGETED.findall(stmt)
        if targets and not DECL_ENUM.match(stmt.splitlines()[0]):
            for name, tag in targets:
                targeted.append((name, table.get(tag, set())))
        # factory *definitions* are not declarations (they used to become pseudo-materials such as
        # `gold` for `static OreDictMaterial gold ()`, shadowing the wood `Gold`)
        if DEFINITION.match(stmt):
            continue
        decls: list[str] = []
        m = DECL_STATIC.search(stmt)
        if m:
            decls.append(m.group(1))
        else:
            for line in stmt.splitlines():
                m2 = DECL_ENUM.match(line)
                if m2:
                    decls.append(m2.group(1))
        if not decls:
            continue
        if len(decls) > 1:
            multi += 1
        ids = {int(value) for value in ID_LITERAL.findall(stmt) if int(value) > 0}
        # declarations that call a helper factory (`Au = gold()`) keep their id inside the factory
        if not ids:
            for match in re.finditer(r"=\s*(\w+)\s*\(", stmt):
                called = match.group(1)
                if called not in overloads:
                    continue
                signature = balanced(stmt, match.end() - 1, "(", ")")
                value = resolve_id(called, len(split_args(signature[1:-1])), overloads, id_memo)
                if value:
                    ids.add(value)
        flags: set[str] = set()
        names: set[str] = set()
        for ident in IDENT.findall(stmt):
            if ident in table and table[ident]:
                flags |= table[ident]
                names.add(ident)
        # the factory overload the declaration calls decides the base form set
        for match in re.finditer(r"=\s*(\w+)\s*\(", stmt):
            called = match.group(1)
            if called not in overloads:
                continue
            signature = balanced(stmt, match.end() - 1, "(", ")")
            flags |= resolve_flags(called, len(split_args(signature[1:-1])),
                                   overloads, table, memo)
            for ident in IDENT.findall(stmt):
                if ident in table and table[ident] and ident.startswith("G_"):
                    names.add(ident)
        if not flags:
            unresolved += 1
            continue
        for name in decls:
            entry = materials.setdefault(name, {"flags": set(), "sets": set(), "ids": set()})
            entry["flags"] |= flags
            entry["sets"] |= {n for n in names if n.startswith("G_")}
            entry["ids"].update(ids)

    for name, flags in targeted:
        if name in materials:
            materials[name]["flags"] |= flags

    # Per-overload flag summary: the arity is part of the key because the overload decides the set.
    factory_report: dict[str, list[str]] = {}
    for name, entries in sorted(overloads.items()):
        for params, _varargs, _body in entries:
            key = "%s/%d" % (name, len(params))
            flags = resolve_flags(name, len(params), overloads, table, memo)
            if flags:
                factory_report[key] = sorted(flags)

    return materials, factory_report, unresolved, multi


def java_source(materials: dict[str, dict], generator_flags: list[str]) -> str:
    # Keyed by GT6 material id (authoritative) plus every normalised name that is unambiguous: two
    # names that normalise identically (Co/CO, Gold/gold) are dropped instead of letting a HashMap
    # decide which flags win.
    by_name: dict[str, list[str]] = collections.defaultdict(list)
    for name, data in materials.items():
        if data["flags"]:
            by_name[normalize(name)].append(name)
    by_id: dict[int, set[str]] = {}
    for name, data in materials.items():
        if not data["flags"]:
            continue
        for material_id in data.get("ids", set()):
            by_id.setdefault(material_id, set()).update(data["flags"])

    lines = [
        "package gregtech.content.data.generated;",
        "",
        "import gregtech.api.material.GTMaterial;",
        "",
        "import java.util.Collections;",
        "import java.util.HashMap;",
        "import java.util.HashSet;",
        "import java.util.Map;",
        "import java.util.Set;",
        "",
        "/**",
        " * GT6's per-material item-generator flags: which item forms the original registers for a",
        " * material (its \"form set\", {@code TD.java:557-613} and the declarations in {@code MT.java}).",
        " * <p>",
        " * GT6 hands each material a form set such as {@code G_GEM_ORES} or the individual flags",
        " * {@code PLATES}/{@code INGOTS}/{@code MULTIPLATES} and then registers exactly those forms; the",
        " * port derives its forms from its own material properties instead, so forms the original has are",
        " * missing (the quartz family's gems/plates/sticks, {@code Asbestos}' plates, {@code Paper}'s",
        " * multi-plates, ...). This table carries the original's answer so {@code MaterialPrefix}'s",
        " * conditions can consult it rather than approximate it.",
        " * <p>",
        " * Rows are keyed by GT6 material id first ({@code <id>|…}, the first argument of the declaration's",
        " * factory in {@code MT.java}, which the port carries over) and by the normalised declaration name",
        " * second ({@code <name>|…}, lowercased, non-alphanumerics dropped). Ids come first because names",
        " * are not unique: {@code MT.Ke} is Trinium while the port looks materials up by display name, and",
        " * {@code Co} cobalt / {@code CO} carbon monoxide, {@code Gold} the wood (with a {@code gold()}",
        " * factory of its own) normalise alike - ambiguous names are left out entirely.",
        " * <p>",
        " * GENERATED by tools/extract_gt6_form_flags.py - do not edit by hand.",
        " */",
        "public final class MaterialForms {",
        "    private MaterialForms() {}",
        "",
        "    /** Flags in {@code TD.java} order. */",
        "    public static final java.util.List<String> FLAGS = java.util.List.of(",
        "            " + ", ".join('"' + flag + '"' for flag in generator_flags) + ");",
        "",
        "    /** One line per material: {@code <GT6 id or normalised name>|<FLAG>,<FLAG>,…}. */",
        "    private static final String[] DATA = {",
    ]
    for material_id in sorted(by_id):
        lines.append('            "%d|%s",' % (material_id, ",".join(sorted(by_id[material_id]))))
    for name, data in sorted(materials.items()):
        if not data["flags"]:
            continue
        key = normalize(name)
        if len(by_name[key]) != 1:
            continue
        lines.append('            "%s|%s",' % (key, ",".join(sorted(data["flags"]))))
    lines.extend([
        "    };",
        "",
        "    private static Map<String, Set<String>> byMaterial;",
        "",
        "    private static Map<String, Set<String>> table() {",
        "        Map<String, Set<String>> index = byMaterial;",
        "        if (index == null) {",
        "            index = new HashMap<>();",
        "            for (String line : DATA) {",
        "                int split = line.indexOf('|');",
        "                if (split < 0) continue;",
        "                Set<String> flags = new HashSet<>();",
        "                for (String flag : line.substring(split + 1).split(\",\")) flags.add(flag);",
        "                index.put(line.substring(0, split), Collections.unmodifiableSet(flags));",
        "            }",
        "            byMaterial = index;",
        "        }",
        "        return index;",
        "    }",
        "",
        "    /**",
        "     * The forms GT6 registers for a material; empty when the original declares none.",
        "     * <p>",
        "     * Looked up by GT6 material id first (the port carries the original's ids) and by the",
        "     * material's name second, for the materials the import declares with its own id.",
        "     */",
        "    public static Set<String> of(GTMaterial material) {",
        "        if (material == null) return Set.of();",
        "        Set<String> supplemental = gregtech.content.material.SupplementalMaterials.forms(material);",
        "        if (!supplemental.isEmpty()) return supplemental;",
        "        Set<String> flags = table().get(Integer.toString(material.getId()));",
        "        if (flags == null) flags = table().get(normalize(material.getName()));",
        "        return flags == null ? Set.of() : flags;",
        "    }",
        "",
        "    /**",
        "     * Whether GT6 registers the given item-generator form for a material.",
        "     * An addon material that declared ores also has {@code ORES}. That overlay is not part of the generated table.",
        "     */",
        "    public static boolean has(GTMaterial material, String flag) {",
        "        if (material != null && \"ORES\".equals(flag)",
        "                && gregtech.engine.addon.AddonMaterials.hasOreForm(material)) return true;",
        "        return of(material).contains(flag);",
        "    }",
        "",
        "    /** How many materials the original gives this form to. */",
        "    public static int count(String flag) {",
        "        int total = 0;",
        "        for (Set<String> flags : table().values()) if (flags.contains(flag)) total++;",
        "        return total;",
        "    }",
        "",
        "    public static String normalize(String name) {",
        "        StringBuilder builder = new StringBuilder(name.length());",
        "        for (int i = 0; i < name.length(); i++) {",
        "            char c = Character.toLowerCase(name.charAt(i));",
        "            if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9') builder.append(c);",
        "        }",
        "        return builder.toString();",
        "    }",
        "}",
    ])
    return "\n".join(lines) + "\n"


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", required=True, type=str,
                        help="GT6 checkout's src/main/java directory")
    parser.add_argument("--check", action="store_true",
                        help="fail when the committed table differs instead of rewriting it")
    parser.add_argument("--json", type=str, default=None,
                        help="also write the factory/material summary as JSON")
    args = parser.parse_args()

    td_java = os.path.join(args.source, "gregapi", "data", "TD.java")
    mt_java = os.path.join(args.source, "gregapi", "data", "MT.java")
    td_text = read_source(td_java)
    mt_text = read_source(mt_java)

    table = parse_td(td_text)
    generator_flags = sorted({flag for flags in table.values() for flag in flags})
    materials, factory_report, unresolved, multi = collect(mt_text, table)
    source = java_source(materials, generator_flags)

    counts: dict[str, int] = {}
    for data in materials.values():
        for flag in data["flags"]:
            counts[flag] = counts.get(flag, 0) + 1
    print("materials with a form set: %d  (declarations without forms: %d)" % (len(materials), unresolved))
    for flag, count in sorted(counts.items(), key=lambda kv: -kv[1]):
        print("  %-20s %d" % (flag, count))

    if args.json:
        report = {
            "source": {"td": td_java, "mt": mt_java},
            "generatorFlags": generator_flags,
            "factories": factory_report,
            "materials": {
                name: {"flags": sorted(data["flags"]), "sets": sorted(data["sets"]),
                       "ids": sorted(data["ids"])}
                for name, data in sorted(materials.items())
            },
            "statementsWithoutForms": unresolved,
            "statementsWithMultipleDeclarations": multi,
        }
        with open(args.json, "w", encoding="utf-8", newline="\n") as handle:
            json.dump(report, handle, indent=2, ensure_ascii=False)
        print("wrote " + args.json)

    if args.check:
        try:
            with open(OUT_JAVA, "r", encoding="utf-8", newline="") as handle:
                current = handle.read().replace("\r\n", "\n")
        except FileNotFoundError:
            current = ""
        if current != source:
            current_lines = current.splitlines()
            source_lines = source.splitlines()
            for index in range(min(len(current_lines), len(source_lines))):
                if current_lines[index] != source_lines[index]:
                    print("first difference at line %d:\n  committed: %s\n  generated: %s"
                          % (index + 1, current_lines[index][:160], source_lines[index][:160]))
                    break
            else:
                print("line count differs: committed %d, generated %d"
                      % (len(current_lines), len(source_lines)))
            print("MaterialForms is stale; rerun without --check to regenerate")
            sys.exit(1)
        print("MaterialForms matches the source")
        return

    os.makedirs(os.path.dirname(OUT_JAVA), exist_ok=True)
    with open(OUT_JAVA, "w", encoding="utf-8", newline="\n") as handle:
        handle.write(source)
    print("wrote " + OUT_JAVA)


if __name__ == "__main__":
    main()
