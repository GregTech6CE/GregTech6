"""Generate the Java mask constant of GT6's WorldgenPit.SHAPE (48x48) so the port's pit generator
uses GT6's exact pit outline instead of a hand-copied (and easily mistyped) table."""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import os
import re

GT6 = os.path.join("..", "gregtech6-master", "gregtech6-master", "build", "sources", "java",
                   "gregtech", "worldgen", "WorldgenPit.java")
OUT = os.path.join(str(native_path("src")), "main", "java", "com", "gregtech", "gregtech", "worldgen", "GTPitShape.java")

with open(GT6, encoding="utf-8", errors="replace") as handle:
    text = handle.read()

start = text.index("SHAPE")
start = text.index("{", start)
# Rows look like {F,F,T,...,T}, so collect the braces groups of 48 entries.
rows = []
for match in re.finditer(r"\{([FT](?:,[FT])*)\}", text[start:]):
    cells = match.group(1).split(",")
    if len(cells) != 48:
        continue
    rows.append("".join("T" if cell == "T" else "F" for cell in cells))
    if len(rows) == 48:
        break

if len(rows) != 48:
    raise SystemExit("expected 48 shape rows, found %d" % len(rows))
inside = sum(row.count("T") for row in rows)

lines = [
    "package com.gregtech.gregtech.worldgen;",
    "",
    "/**",
    " * GT6's {@code WorldgenPit.SHAPE} — the 48x48 outline of a sand/clay pit, one string per Z row",
    " * with one character per X column ({@code T} = part of the pit). Generated from the original",
    " * source by {@code tools/generate_pit_shape.py} so the mask cannot drift.",
    " *",
    " * <p>The generator anchors the mask 16 blocks before the chunk it runs in, so one pit spans a",
    " * 3x3 chunk area; %d of the 2304 cells belong to the pit." % inside,
    " */",
    "public final class GTPitShape {",
    "    private GTPitShape() {}",
    "",
    "    public static final int SIZE = 48;",
    "",
    "    /** Rows indexed by Z offset, characters by X offset (GT6 {@code SHAPE[x][z]}). */",
    "    public static final String[] ROWS = {",
]
for row in rows:
    lines.append('        "%s",' % row)
lines += [
    "    };",
    "",
    "    /** GT6's {@code SHAPE[x][z]}. */",
    "    public static boolean at(int x, int z) {",
    "        return x >= 0 && x < SIZE && z >= 0 && z < SIZE && ROWS[z].charAt(x) == 'T';",
    "    }",
    "",
    "    /** Number of cells the pit covers (GT6's mask). */",
    "    public static int cells() {",
    "        int count = 0;",
    "        for (String row : ROWS) for (int i = 0; i < row.length(); i++) if (row.charAt(i) == 'T') count++;",
    "        return count;",
    "    }",
    "}",
]

with open(OUT, "w", encoding="utf-8", newline="\n") as handle:
    handle.write("\n".join(lines) + "\n")
print("wrote %s (%d rows, %d pit cells)" % (OUT, len(rows), inside))
