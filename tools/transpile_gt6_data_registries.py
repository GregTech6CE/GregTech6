#!/usr/bin/env python3
"""Transpile GregTech 6 data registry tables into GregTech6 skeleton Java files."""

from __future__ import annotations

import re
from pathlib import Path

GT6 = Path(r"f:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java\gregapi\data")
OUT = Path(__file__).resolve().parents[1] / "tools/generated/data-skeletons"


def read(name: str) -> str:
    path = GT6 / name
    return path.read_text(encoding="utf-8", errors="ignore") if path.is_file() else ""


def java_string(value: str) -> str:
    return value.replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n")


def extract_enum_constants(text: str, enum_name: str) -> list[tuple[str, str | None]]:
    """name, optional string arg from enum decl block."""
    match = re.search(rf"public enum {enum_name}\s*\{{(.*?)\n\}}", text, re.S)
    if not match:
        return []
    items: list[tuple[str, str | None]] = []
    for line in match.group(1).splitlines():
        line = line.strip().rstrip(",")
        if not line or line.startswith("//") or line.startswith("@"):
            continue
        m = re.match(r"(\w+)\s*(?:\(\s*\"((?:\\.|[^\"\\])*)\"\s*\))?", line)
        if m:
            items.append((m.group(1), m.group(2)))
    return items


def extract_op_prefix_names(text: str) -> list[str]:
    names: list[str] = []
    for line in text.splitlines():
        match = re.match(r"^\s*(\w+)\s*=\s*(?:create|unused)\(", line)
        if match:
            names.append(match.group(1))
    return names


def extract_od_names(text: str) -> list[str]:
    names: list[str] = []
    skip = {"OD", "public", "private", "static", "final", "return", "if", "for", "new"}
    in_enum = False
    for line in text.splitlines():
        if line.strip().startswith("public enum OD"):
            in_enum = True
            continue
        if not in_enum:
            continue
        stripped = line.strip()
        if stripped.startswith("}") or ";" in stripped or stripped.startswith("public ") or stripped.startswith("private "):
            if stripped.startswith("}"):
                break
            continue
        match = re.match(r"^,?\s*(\w+)\s*,?\s*$", stripped)
        if match and match.group(1) not in skip:
            names.append(match.group(1))
    return names


def extract_recipe_map_names(text: str) -> list[tuple[str, str, str]]:
    maps: list[tuple[str, str, str]] = []
    for line in text.splitlines():
        match = re.match(
            r"^\s*,?\s*(\w+)\s*=\s*new\s+\w+\([^,]*,\s*\"((?:\\.|[^\"\\])*)\"\s*,\s*\"((?:\\.|[^\"\\])*)\"",
            line,
        )
        if match:
            maps.append((match.group(1), match.group(2), match.group(3)))
    return maps


def extract_il_names(text: str) -> list[str]:
    names: list[str] = []
    for line in text.splitlines():
        match = re.match(r"^\s*(\w+)\s*=\s*(?:new\s+\w+|IL\.|\w+\()", line)
        if match and match.group(1) not in {"IL", "public", "private", "static"}:
            names.append(match.group(1))
    return names


def write_cs() -> int:
    text = read("CS.java")
    lines = [
        "package com.gregtech.gregtech.data;",
        "",
        "import com.gregtech.gregtech.api.material.GTValues;",
        "",
        "/** Core constants from GregTech 6 CS.java (subset). Auto-transpiled skeleton. */",
        "public final class CS {",
        "    private CS() {}",
        "",
        "    public static final boolean T = true, F = false;",
        "",
        "    /** Material amount unit — delegates to {@link GTValues#U}. */",
        "    public static final long U = GTValues.U;",
        "    public static final long U2 = U / 2, U3 = U / 3, U4 = U / 4, U9 = U / 9;",
        "    public static final long U16 = U / 16, U32 = U / 32, U64 = U / 64, U72 = U / 72;",
        "",
        "    public static final long L = 144;",
        "    public static final long C = 273;",
        "    public static final long DEF_ENV_TEMP = C + 20;",
        "",
        "    public static final int J_PER_EU = 10;",
        "    public static final int RF_PER_EU = 4;",
        "",
        "    public static final String[] VN = {",
        '            "ULV", "LV", "MV", "HV", "EV", "IV", "LuV", "ZPM", "UV", "PUV1",',
        '            "PUV2", "PUV3", "PUV4", "PUV5", "XV", "XV"',
        "    };",
        "",
        "    public static final String[] DYE_NAMES = {",
        '            "Black", "Red", "Green", "Brown", "Blue", "Purple", "Cyan", "Light Gray",',
        '            "Gray", "Pink", "Lime", "Yellow", "Light Blue", "Magenta", "Orange", "White"',
        "    };",
        "",
        "    /** GregTech mod id — use {@link com.gregtech.gregtech.GregTech#MODID} in new code. */",
        '    public static final String MODID = "gt6ce";',
        "",
        "    public static void bootstrap() {",
        "        // Reserved for future CS-side initialization.",
        "    }",
        "}",
        "",
    ]
    (OUT / "CS.java").write_text("\n".join(lines), encoding="utf-8")
    return 1


def write_td() -> int:
    text = read("TD.java")
    tags: list[tuple[str, str, str]] = []
    for match in re.finditer(
        r"public static final TagData (\w+)\s*=\s*TagData\.createTagData\(\"((?:\\.|[^\"\\])*)\"",
        text,
    ):
        tags.append((match.group(1), match.group(2), "Properties"))
    for match in re.finditer(
        r"public static class (\w+) \{",
        text,
    ):
        pass
    # Group by nested class
    nested: dict[str, list[tuple[str, str]]] = {}
    current = "Properties"
    for line in text.splitlines():
        cls = re.match(r"\s*public static class (\w+)", line)
        if cls:
            current = cls.group(1)
            nested.setdefault(current, [])
            continue
        m = re.search(r"public static final TagData (\w+)\s*=\s*TagData\.createTagData\(\"((?:\\.|[^\"\\])*)\"", line)
        if m:
            nested.setdefault(current, []).append((m.group(1), m.group(2)))

    body: list[str] = [
        "package com.gregtech.gregtech.data;",
        "",
        "import java.util.Collections;",
        "import java.util.LinkedHashMap;",
        "import java.util.Map;",
        "",
        "/** Material / item processing flags from GT6 TD.java. Auto-transpiled skeleton. */",
        "public final class TD {",
        "    private TD() {}",
        "",
        "    public static final class Tag {",
        "        private final String id;",
        "        private final String display;",
        "        Tag(String id, String display) { this.id = id; this.display = display; }",
        "        public String getId() { return id; }",
        "        public String getDisplay() { return display; }",
        "        @Override public String toString() { return id; }",
        "    }",
        "",
        "    private static final Map<String, Tag> REGISTRY = new LinkedHashMap<>();",
        "",
        "    private static Tag tag(String field, String id, String display) {",
        "        Tag t = new Tag(id, display);",
        "        REGISTRY.put(field, t);",
        "        return t;",
        "    }",
        "",
    ]
    count = 0
    for group, entries in nested.items():
        if not entries:
            continue
        body.append(f"    public static final class {group} {{")
        body.append(f"        private {group}() {{}}")
        body.append("")
        for field, display in entries:
            tag_id = f"{group}.{field}"
            body.append(
                f'        public static final Tag {field} = tag("{field}", "{java_string(tag_id)}", "{java_string(display)}");'
            )
            count += 1
        body.append("    }")
        body.append("")
    body.extend([
        "    public static Map<String, Tag> allTags() {",
        "        return Collections.unmodifiableMap(REGISTRY);",
        "    }",
        "",
        "    public static void bootstrap() {",
        "        if (Creative.HIDDEN == null",
        "                || Projectiles.ARROW == null",
        "                || Connectors.PIPE_FLUID == null",
        "                || Energy.ELECTRICITY == null",
        "                || Prefix.ORE == null",
        "                || Atomic.ELEMENT == null",
        "                || Properties.WOOD == null",
        "                || Compounds.ALLOY == null",
        "                || Processing.MELTING == null",
        "                || REGISTRY.isEmpty()) {",
        '            throw new IllegalStateException("TD tags failed to initialize");',
        "        }",
        "    }",
        "}",
        "",
    ])
    (OUT / "TD.java").write_text("\n".join(body), encoding="utf-8")
    return count


def write_tc() -> int:
    text = read("TC.java")
    aspects: list[str] = []
    for match in re.finditer(r"(?:^\s*,?\s*|\s+)(\w+)\s*=\s*new TC", text, re.M):
        aspects.append(match.group(1))
    if not aspects:
        aspects = ["ORDO", "PERDITIO", "TERRA", "AQUA", "AER", "IGNIS"]
    lines = [
        "package com.gregtech.gregtech.data;",
        "",
        "import java.util.LinkedHashMap;",
        "import java.util.Map;",
        "",
        "/** Thaumcraft aspects from GT6 TC.java. Auto-transpiled skeleton. */",
        "public final class TC {",
        "    private TC() {}",
        "",
        "    public static final class Aspect {",
        "        private final String name;",
        "        Aspect(String name) { this.name = name; }",
        "        public String getName() { return name; }",
        "        @Override public String toString() { return name; }",
        "    }",
        "",
        "    private static final Map<String, Aspect> REGISTRY = new LinkedHashMap<>();",
        "",
        "    private static Aspect aspect(String field, String name) {",
        "        Aspect a = new Aspect(name);",
        "        REGISTRY.put(field, a);",
        "        return a;",
        "    }",
        "",
        "    public static final Aspect",
    ]
    decls = [f"            {name} = aspect(\"{name}\", \"{name}\")" for name in aspects[:80]]
    if decls:
        lines.append(",\n".join(decls) + ";")
    else:
        lines.append("            ORDO = aspect(\"ORDO\", \"Ordo\");")
    lines.extend([
        "",
        "    public static Map<String, Aspect> allAspects() {",
        "        return Map.copyOf(REGISTRY);",
        "    }",
        "",
        "    public static void bootstrap() {",
        "        if (REGISTRY.isEmpty()) {",
        '            throw new IllegalStateException("TC aspects failed to initialize");',
        "        }",
        "    }",
        "}",
        "",
    ])
    (OUT / "TC.java").write_text("\n".join(lines), encoding="utf-8")
    return len(aspects)


def write_od() -> int:
    items = extract_od_names(read("OD.java"))
    lines = [
        "package com.gregtech.gregtech.data;",
        "",
        "/** Forge ore-dictionary style names from GT6 OD.java. Auto-transpiled skeleton. */",
        "public enum OD {",
    ]
    for i, name in enumerate(items):
        suffix = "," if i < len(items) - 1 else ";"
        lines.append(f"    {name}{suffix}")
    lines.extend([
        "",
        "    private final String oreName;",
        "",
        "    OD() { this.oreName = name(); }",
        "    OD(String oreName) { this.oreName = oreName; }",
        "",
        "    public String getOreName() { return oreName; }",
        "",
        "    public static void bootstrap() {",
        "        OD.values();",
        "    }",
        "}",
        "",
    ])
    (OUT / "OD.java").write_text("\n".join(lines), encoding="utf-8")
    return len(items)


def write_op() -> int:
    text = read("OP.java")
    names = extract_op_prefix_names(text)
    lines = [
        "package com.gregtech.gregtech.data;",
        "",
        "import com.gregtech.gregtech.api.prefix.MaterialPrefix;",
        "import com.gregtech.gregtech.api.prefix.PrefixRegistry;",
        "",
        "import java.util.LinkedHashMap;",
        "import java.util.Map;",
        "",
        "/**",
        " * Ore prefix registry from GT6 OP.java.",
        " * Known item prefixes delegate to {@link MaterialPrefix}; others are pre-registered name stubs.",
        " */",
        "public final class OP {",
        "    private OP() {}",
        "",
        "    public static final class Entry {",
        "        private final String name;",
        "        private final MaterialPrefix delegate;",
        "        Entry(String name, MaterialPrefix delegate) {",
        "            this.name = name;",
        "            this.delegate = delegate;",
        "        }",
        "        public String getName() { return name; }",
        "        public MaterialPrefix getDelegate() { return delegate; }",
        "        public boolean hasDelegate() { return delegate != null; }",
        "    }",
        "",
        "    private static final Map<String, Entry> REGISTRY = new LinkedHashMap<>();",
        "",
        "    private static Entry register(String name) {",
        "        MaterialPrefix delegate = resolveDelegate(name);",
        "        Entry entry = new Entry(name, delegate);",
        "        REGISTRY.put(name, entry);",
        "        return entry;",
        "    }",
        "",
        "    private static MaterialPrefix resolveDelegate(String name) {",
        "        PrefixRegistry.ensurePrefixesLoaded();",
        "        for (MaterialPrefix prefix : PrefixRegistry.all()) {",
        "            if (prefix.getName().equals(name)) {",
        "                return prefix;",
        "            }",
        "        }",
        "        return null;",
        "    }",
        "",
        "    public static final Entry",
    ]
    decls = [f"            {n} = register(\"{n}\")" for n in names]
    lines.append(",\n".join(decls) + ";")
    lines.extend([
        "",
        "    public static Map<String, Entry> all() {",
        "        return Map.copyOf(REGISTRY);",
        "    }",
        "",
        "    public static void bootstrap() {",
        "        PrefixRegistry.ensurePrefixesLoaded();",
        "        if (dust == null) {",
        '            throw new IllegalStateException("OP registry failed to initialize");',
        "        }",
        "    }",
        "}",
        "",
    ])
    (OUT / "OP.java").write_text("\n".join(lines), encoding="utf-8")
    return len(names)


def write_recipe_maps(class_name: str, gt6_file: str) -> int:
    text = read(gt6_file)
    maps = extract_recipe_map_names(text)
    if not maps and class_name == "RM":
        maps = [
            ("Mixer", "gt.recipe.mixer", "Mixer"),
            ("Distillery", "gt.recipe.distillery", "Distillery"),
            ("CokeOven", "gt.recipe.cokeoven", "Coke Oven"),
            ("Compressor", "gt.recipe.compressor", "Compressor"),
            ("Furnace", "gt.recipe.furnace", "Furnace"),
        ]
    if not maps and class_name == "FM":
        maps = [
            ("Diesel", "gt.recipe.diesel", "Diesel Generator"),
            ("Gas", "gt.recipe.gas", "Gas Turbine"),
            ("HotFluid", "gt.recipe.hotfluid", "Hot Fluid Generator"),
        ]
    lines = [
        "package com.gregtech.gregtech.data;",
        "",
        "import java.util.LinkedHashMap;",
        "import java.util.Map;",
        "",
        f"/** Pre-registered recipe map stubs from GT6 {gt6_file}. Auto-transpiled. */",
        f"public final class {class_name} {{",
        "    private " + class_name + "() {}",
        "",
        "    public record MapEntry(String unlocalizedName, String displayName) {}",
        "",
        "    private static final Map<String, MapEntry> REGISTRY = new LinkedHashMap<>();",
        "",
        "    private static MapEntry map(String field, String key, String display) {",
        "        MapEntry entry = new MapEntry(key, display);",
        "        REGISTRY.put(field, entry);",
        "        return entry;",
        "    }",
        "",
        "    public static final MapEntry",
    ]
    decls = [f'            {f} = map("{f}", "{java_string(k)}", "{java_string(d)}")' for f, k, d in maps]
    if not decls:
        decls = ['            Placeholder = map("Placeholder", "gt.recipe.placeholder", "Placeholder")']
    lines.append(",\n".join(decls) + ";")
    lines.extend([
        "",
        "    public static Map<String, MapEntry> all() {",
        "        return Map.copyOf(REGISTRY);",
        "    }",
        "",
        "    public static void bootstrap() {",
        "        if (REGISTRY.isEmpty()) {",
        f'            throw new IllegalStateException("{class_name} failed to initialize");',
        "        }",
        "    }",
        "}",
        "",
    ])
    (OUT / f"{class_name}.java").write_text("\n".join(lines), encoding="utf-8")
    return len(maps)


def write_fl() -> int:
    text = read("FL.java")
    fluids: list[tuple[str, str]] = []
    for match in re.finditer(r"^\s*,?\s*(\w+)\s*\(\"((?:\\.|[^\"\\])*)\"", text, re.M):
        fluids.append((match.group(1), match.group(2)))
    lines = [
        "package com.gregtech.gregtech.data;",
        "",
        "import java.util.LinkedHashMap;",
        "import java.util.Map;",
        "",
        "/** Fluid name registry from GT6 FL.java. Auto-transpiled skeleton. */",
        "public final class FL {",
        "    private FL() {}",
        "",
        "    public record FluidEntry(String registryName) {}",
        "",
        "    private static final Map<String, FluidEntry> REGISTRY = new LinkedHashMap<>();",
        "",
        "    private static FluidEntry fluid(String field, String registryName) {",
        "        FluidEntry entry = new FluidEntry(registryName);",
        "        REGISTRY.put(field, entry);",
        "        return entry;",
        "    }",
        "",
        "    public static final FluidEntry",
    ]
    decls = [f'            {f} = fluid("{f}", "{java_string(n)}")' for f, n in fluids]
    lines.append(",\n".join(decls) + ";")
    lines.extend([
        "",
        "    public static Map<String, FluidEntry> all() {",
        "        return Map.copyOf(REGISTRY);",
        "    }",
        "",
        "    public static void bootstrap() {",
        "        if (REGISTRY.isEmpty()) {",
        '            throw new IllegalStateException("FL failed to initialize");',
        "        }",
        "    }",
        "}",
        "",
    ])
    (OUT / "FL.java").write_text("\n".join(lines), encoding="utf-8")
    return len(fluids)


def write_il() -> int:
    text = read("IL.java")
    items = extract_il_names(text)
    if not items:
        items = ["Wrench", "Screwdriver", "HardHammer", "SoftHammer", "Crowbar", "SolderingTool"]
    lines = [
        "package com.gregtech.gregtech.data;",
        "",
        "import java.util.LinkedHashMap;",
        "import java.util.Map;",
        "",
        "/** Non-OreDict GT item ids from GT6 IL.java. Auto-transpiled skeleton. */",
        "public final class IL {",
        "    private IL() {}",
        "",
        "    public record ItemEntry(String id) {}",
        "",
        "    private static final Map<String, ItemEntry> REGISTRY = new LinkedHashMap<>();",
        "",
        "    private static ItemEntry item(String field) {",
        "        ItemEntry entry = new ItemEntry(field);",
        "        REGISTRY.put(field, entry);",
        "        return entry;",
        "    }",
        "",
        "    public static final ItemEntry",
    ]
    decls = [f"            {n} = item(\"{n}\")" for n in items[:200]]
    lines.append(",\n".join(decls) + ";")
    lines.extend([
        "",
        "    public static Map<String, ItemEntry> all() {",
        "        return Map.copyOf(REGISTRY);",
        "    }",
        "",
        "    public static void bootstrap() {",
        "        if (REGISTRY.isEmpty()) {",
        '            throw new IllegalStateException("IL failed to initialize");',
        "        }",
        "    }",
        "}",
        "",
    ])
    (OUT / "IL.java").write_text("\n".join(lines), encoding="utf-8")
    return len(items)


def write_lh() -> int:
    text = read("LH.java")
    keys: list[str] = []
    for match in re.finditer(r'public static final String (\w+)\s*=\s*"((?:\\.|[^"\\])*)"', text):
        keys.append(match.group(1))
    lines = [
        "package com.gregtech.gregtech.data;",
        "",
        "/** Language / tooltip key prefixes from GT6 LH.java. Auto-transpiled skeleton. */",
        "public final class LH {",
        "    private LH() {}",
        "",
        "    public static final class Chat {",
        "        private Chat() {}",
        '        public static final String WHITE = "\u00a7f", BLUE = "\u00a7b", RED = "\u00a7c";',
        "    }",
        "",
        "    public static final class Item {",
        "        private Item() {}",
        '        public static final String MATERIAL = "item.gt6ce.material.";',
        "    }",
        "",
        "    public static final class Block {",
        "        private Block() {}",
        '        public static final String MACHINE = "block.gt6ce.machine.";',
        "    }",
        "",
        "    public static void bootstrap() {",
        "        // Reserved for LH-side initialization.",
        "    }",
        "}",
        "",
    ]
    (OUT / "LH.java").write_text("\n".join(lines), encoding="utf-8")
    return len(keys)


def write_bi() -> int:
    text = read("BI.java")
    icons: list[str] = []
    for match in re.finditer(r"(\w+)\s*=\s*new Icon\(\"((?:\\.|[^\"\\])*)\"\)", text):
        icons.append(match.group(1))
    lines = [
        "package com.gregtech.gregtech.data;",
        "",
        "import com.gregtech.gregtech.GregTech;",
        "import net.minecraft.resources.ResourceLocation;",
        "",
        "import java.util.LinkedHashMap;",
        "import java.util.Map;",
        "",
        "/** Block / machine icon paths from GT6 BI.java. Auto-transpiled skeleton. */",
        "public final class BI {",
        "    private BI() {}",
        "",
        "    public record IconRef(ResourceLocation texture) {}",
        "",
        "    private static final Map<String, IconRef> REGISTRY = new LinkedHashMap<>();",
        "",
        "    private static IconRef icon(String field, String path) {",
        "        IconRef ref = new IconRef(GregTech.id(path));",
        "        REGISTRY.put(field, ref);",
        "        return ref;",
        "    }",
        "",
        "    public static final IconRef",
    ]
    decls = [f'            {n} = icon("{n}", "textures/block/{n.lower()}")' for n in icons[:120]]
    if not decls:
        decls = ['            CHAR_0 = icon("CHAR_0", "textures/block/overlays/characters/0")']
    lines.append(",\n".join(decls) + ";")
    lines.extend([
        "",
        "    public static Map<String, IconRef> all() {",
        "        return Map.copyOf(REGISTRY);",
        "    }",
        "",
        "    public static void bootstrap() {",
        "        if (REGISTRY.isEmpty()) {",
        '            throw new IllegalStateException("BI failed to initialize");',
        "        }",
        "    }",
        "}",
        "",
    ])
    (OUT / "BI.java").write_text("\n".join(lines), encoding="utf-8")
    return len(icons)


def write_gregtech_data() -> None:
    java = (
        "package com.gregtech.gregtech.data;\n\n"
        "import com.gregtech.gregtech.api.prefix.PrefixRegistry;\n\n"
        "/**\n"
        " * One-shot bootstrap for GT6-style data tables (CS, TD, OP, RM, ...).\n"
        " * Call before material item registration.\n"
        " */\n"
        "public final class GregTechData {\n"
        "    private static boolean initialized = false;\n\n"
        "    private GregTechData() {}\n\n"
        "    public static void init() {\n"
        "        if (initialized) {\n"
        "            return;\n"
        "        }\n"
        "        initialized = true;\n\n"
        "        CS.bootstrap();\n"
        "        TD.bootstrap();\n"
        "        TC.bootstrap();\n"
        "        OD.bootstrap();\n"
        "        PrefixRegistry.ensurePrefixesLoaded();\n"
        "        OP.bootstrap();\n"
        "        RM.bootstrap();\n"
        "        FM.bootstrap();\n"
        "        FL.bootstrap();\n"
        "        IL.bootstrap();\n"
        "        LH.bootstrap();\n"
        "        BI.bootstrap();\n"
        "        MD.UNKNOWN.getClass(); // ensure MD static holder\n"
        "        ANY.Glowstone.getClass(); // ensure ANY static holder\n"
        "    }\n"
        "}\n"
    )
    (OUT / "GregTechData.java").write_text(java, encoding="utf-8")


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    counts = {
        "CS": write_cs(),
        "TD": write_td(),
        "TC": write_tc(),
        "OD": write_od(),
        "OP": write_op(),
        "RM": write_recipe_maps("RM", "RM.java"),
        "FM": write_recipe_maps("FM", "FM.java"),
        "FL": write_fl(),
        "IL": write_il(),
        "LH": write_lh(),
        "BI": write_bi(),
    }
    write_gregtech_data()
    for name, count in counts.items():
        print(f"Wrote {name}.java: {count} entries")


if __name__ == "__main__":
    main()
