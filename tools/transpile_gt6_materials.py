#!/usr/bin/env python3
"""Transpile GregTech 6 MT.java material definitions into GregTech6 Java data files."""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import re
import sys
import textwrap
from modular_materials import write_catalog
from readable_data_names import readable_java
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT.parent / "gregtech6-master/gregtech6-master/src/main/java/gregapi/data/MT.java"
OUT = native_path(ROOT / "src/main/java/com/gregtech/gregtech/data/generated")
MODELS = native_path(ROOT / "src/main/resources/assets/gt6ce/models/item/material")
TEXTURES = native_path(ROOT / "src/main/resources/assets/gt6ce/textures/item/material_icons")
PREFIXES = ("dust", "ingot", "nugget", "plate", "oreraw", "gem")

SKIP_FACTORIES = {
    # Placeholder factories and mod-compat ones.  "mix"/"mixdust"/"dcmp" are here on purpose for now:
    # their materials have to be carried in through EXTRA_ENTRIES one by one, because un-skipping the
    # factories wholesale makes GT6's own duplicate declarations collide ("Duplicate readable material
    # names in Compounds").  See §32 of the porting notes.
    "tier", "unused", "deprecated", "invalid", "unknown", "gem_aa", "handle", "brick",
    "mix", "mixdust", "dcmp", "hexorium",
}

SKIP_MATERIAL_FIELDS = {
    "Ma", "Magic", "y", "v", "n", "p", "e", "Photon", "Neutrino", "Neutron", "Proton", "Electron",
}

SKIP_MATERIAL_IDS = {0, 1, 2, 3, 4, 5, 4000, 4001, 4002, 4003, 4004, 4005}

METAL_FACTORIES = {
    "metal", "transmetal", "alkali", "alkaline", "lanthanide", "actinide", "refractmetal",
    "platingroup", "posttrans", "precmetal", "noblemetal", "metalloid", "metalore",
    "metalmachine", "metalmachore", "metal_", "metalore_", "metalmachine_", "metalmachore_",
    "setal", "setalore", "setalmachine", "setalmachore", "setal_", "setalore_", "setalmachine_", "setalmachore_",
    "cetal", "cetalore", "cetalmachine", "cetalmachore", "cetal_", "cetalore_", "cetalmachine_", "cetalmachore_",
    "metalnd", "metalmachnd", "alloynd", "alloymachnd",
}

ALLOY_FACTORIES = {
    "alloy", "alloyore", "alloymachine", "alloymachore", "alloy_", "alloyore_", "alloymachine_", "alloymachore_",
    "slloy", "slloyore", "slloymachine", "slloymachore", "slloy_", "slloyore_", "slloymachine_", "slloymachore_",
    "clloy", "clloyore", "clloymachine", "clloymachore", "clloy_", "clloyore_", "clloymachine_", "clloymachore_",
}

ORE_FACTORIES = {"ore", "oredust", "oredustdcmp", "oredustcent", "oredustelec", "oreRaw"}

GEM_FACTORIES = {
    "gem", "valgem", "valgemdcmp", "valgemcent", "valgemelec", "diamond", "sapphire", "emerald", "garnet",
    "jasper", "tigereye", "aventurine", "fluorite", "crystal", "crystaldcmp", "crystalcent", "crystalelec",
}

GAS_FACTORIES = {"gas", "gasdcmp", "gasflam", "gasexpl", "gascent", "gaselec", "gaschem", "gaschemdcmp",
                 "gaschemflam", "gaschemexpl", "gaschemcent", "gaschemelec", "gasacid", "gasaciddcmp",
                 "gasacidflam", "gasacidexpl", "gasacidcent", "gasacidelec", "diatomicgas", "noblegas"}

LIQUID_FACTORIES = {"lqud", "lqudflam", "lqudexpl", "lquddcmp", "lqudcent", "lqudelec", "lqudchem",
                    "lqudchemdcmp", "lqudchemflam", "lqudchemexpl", "lqudchemcent", "lqudchemelec", "diatomic"}

ELEMENT_FACTORIES = {
    "element", "nonmetal", "diatomic", "polyatomic", "unknown",
}

STONE_FACTORIES = {"stone", "stonecent", "brick"}

WOOD_FACTORIES = {"wood", "woodnormal"}

DUST_FACTORIES = {"dust", "dustcent", "dustdcmp", "dustelec", "dustimpure", "dustpure", "dustrefined",
                  "blaze", "powder"}

SET_MAP = {
    "SET_METALLIC": "METALLIC",
    "SET_SHINY": "SHINY",
    "SET_COPPER": "COPPER",
    "SET_DULL": "DULL",
    "SET_FINE": "FINE",
    "SET_RAD": "DULL",
    "SET_ROUGH": "ROUGH",
    "SET_STONE": "STONE",
    "SET_SAND": "SAND",
    "SET_WOOD": "WOOD",
    "SET_DIAMOND": "DIAMOND",
    "SET_EMERALD": "EMERALD",
    "SET_QUARTZ": "QUARTZ",
    "SET_RUBY": "RUBY",
    "SET_REDSTONE": "REDSTONE",
    "SET_GAS": "FINE",
    "SET_FLUID": "FINE",
    "SET_POWDER": "POWDER",
    "SET_LAPIS": "LAPIS",
    "SET_GLASS": "GLASS",
    "SET_CUBE": "CUBE",
    "SET_CUBE_SHINY": "SHINY",
    "SET_GEM_VERTICAL": "GEM_VERTICAL",
    "SET_GEM_HORIZONTAL": "GEM_HORIZONTAL",
    "SET_SPACE": "SPACE",
    "SET_HEX": "HEX",
    "SET_SHARDS": "SHARDS",
    "SET_MAGNETIC": "MAGNETIC",
    "SET_NONE": "DULL",
    "SET_FIERY": "FIERY",
    "SET_FLINT": "FLINT",
    "SET_PRISMARINE": "PRISMARINE",
    "SET_OPAL": "OPAL",
    "SET_LEAF": "LEAF",
    "SET_FOOD": "FOOD",
    "SET_PAPER": "PAPER",
    "SET_BRICK": "BRICK",
    "SET_PLASMA": "PLASMA",
    "SET_LIGNITE": "LIGNITE",
    "SET_RUBBER": "RUBBER",
    "SET_NETHERSTAR": "SHINY",
}

METHOD_LINE_RE = re.compile(
    r"static\s+OreDictMaterial\s+(\w+)\s*\(\)\s*\{return\s+(\w+)\s*\((.*)\);\s*\}"
)

ASSIGN_RE = re.compile(r"^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(\w+)\s*\(")

# Hand-curated entries kept in the generated file (their GT6 originals use skipped factories),
# pinned after the named anchor entry of the given holder class. Port MT.java references them.
EXTRA_ENTRIES = {
    ("Compounds", "NiobiumTitanium"): [
        ("Graphene", '            Graphene = alloy(9175, "Graphene", 0x3C3C3C).setTextureSet(MaterialTextureSet.METALLIC).setStats(4000, 5500, 2.26F).setTooltipChemical("C"),'),
        ("Superconductor", '            Superconductor = alloy(9516, "Superconductor", 0xFFFFFF).setTextureSet(MaterialTextureSet.METALLIC).setStats(1000, 3000, 1.0F),'),
        # GT6 declares both through factories the port does not emit wholesale (`dcmp`, `mixdust`):
        # Eudialyte 8421 is a decomposable gem ore (MT.java:1513, SET_LAPIS, ore multiplier 5,
        # byproducts Zircon/RareEarth/Hf/Pb, and a Biomes-O-Plenty ore), "Hydrated Coal" 8335 is a
        # mixed dust (MT.java:1534, SET_LIGNITE). Without them the tokens i:gem:Eudialyte,
        # omd:Eudialyte and omd:HydratedCoal cannot resolve — see §32 of the porting notes.
        ("Eudialyte", '            Eudialyte = gem(8421, "Eudialyte", 0x9B6072, MaterialTextureSet.LAPIS, MaterialProperty.GEM, MaterialProperty.ORE),'),
        ("HydratedCoal", '            HydratedCoal = dust(8335, "Hydrated Coal", 0x464664),'),
    ],
}

# Ore-processing chained calls carried over from MT.java (see GTMaterial counterparts).
ORE_PROC_METHODS = ("ores", "setSmelting", "setCrushing", "setOreMultiplier")

# U-fraction constants available in the port's com.gregtech.gregtech.data.CS.
U_CONSTANTS = {1: "CS.U", 2: "CS.U2", 3: "CS.U3", 4: "CS.U4", 9: "CS.U9",
               16: "CS.U16", 32: "CS.U32", 64: "CS.U64", 72: "CS.U72"}

# Nested holder classes of the original gregapi.data.MT; their fields are parsed flat.
MT_NESTED_PREFIXES = ("OREMATS.", "STONES.", "WOODS.")


def extract_balanced_args(text: str, open_index: int) -> str | None:
    depth = 0
    in_string = False
    escape = False
    for i in range(open_index, len(text)):
        ch = text[i]
        if in_string:
            if escape:
                escape = False
            elif ch == "\\":
                escape = True
            elif ch == '"':
                in_string = False
            continue
        if ch == '"':
            in_string = True
            continue
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
            if depth == 0:
                return text[open_index + 1 : i]
    return None


def parse_assignment(line: str) -> tuple[str, str, str] | None:
    match = ASSIGN_RE.match(line)
    if not match:
        return None
    field, factory = match.group(1), match.group(2)
    open_paren = line.index("(", match.end() - 1)
    args = extract_balanced_args(line, open_paren)
    if args is None:
        return None
    return field, factory, args

STRING_RE = re.compile(r'"((?:\\.|[^"\\])*)"')
INT_RE = re.compile(r"-?\d+")
FLOAT_RE = re.compile(r"-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?")


def read_source() -> str:
    return SRC.read_text(encoding="utf-8", errors="ignore")


def split_args(arg_str: str) -> list[str]:
    args: list[str] = []
    current: list[str] = []
    depth = 0
    in_string = False
    escape = False
    for ch in arg_str:
        if in_string:
            current.append(ch)
            if escape:
                escape = False
            elif ch == "\\":
                escape = True
            elif ch == '"':
                in_string = False
            continue
        if ch == '"':
            in_string = True
            current.append(ch)
            continue
        if ch in "([{":
            depth += 1
        elif ch in ")]}":
            depth -= 1
        if ch == "," and depth == 0:
            args.append("".join(current).strip())
            current = []
            continue
        current.append(ch)
    tail = "".join(current).strip()
    if tail:
        args.append(tail)
    return args


def extract_color(args: list[str]) -> str:
    for a in args:
        if a.lower().startswith("0x"):
            return a if a.startswith("0x") else "0x" + a[2:]

    set_idx = next((i for i, a in enumerate(args) if a.startswith("SET_")), None)
    if set_idx is not None:
        rgb: list[int] = []
        for a in args[set_idx + 1 :]:
            if INT_RE.fullmatch(a):
                v = int(a)
                if 0 <= v <= 255:
                    rgb.append(v)
                    if len(rgb) >= 3:
                        break
        if len(rgb) >= 3:
            return f"0x{rgb[0]:02X}{rgb[1]:02X}{rgb[2]:02X}"

    str_idx = next((i for i, a in enumerate(args) if STRING_RE.search(a)), None)
    if str_idx is not None:
        rgb = []
        for a in args[str_idx + 1 :]:
            if INT_RE.fullmatch(a):
                v = int(a)
                if 0 <= v <= 255:
                    rgb.append(v)
                    if len(rgb) >= 3:
                        break
        if len(rgb) >= 3:
            return f"0x{rgb[0]:02X}{rgb[1]:02X}{rgb[2]:02X}"

    return "0xC8C8C8"


def parse_strings_and_numbers(args: list[str]) -> dict:
    strings = [m.group(1) for a in args for m in [STRING_RE.search(a)] if m]
    numbers: list[str] = []
    for a in args:
        if STRING_RE.search(a):
            continue
        if a.startswith("SET_"):
            continue
        if re.match(r"[A-Z_][A-Za-z0-9_.]*$", a):
            continue
        if INT_RE.fullmatch(a) or FLOAT_RE.fullmatch(a):
            numbers.append(a)
    texture = next((a for a in args if a.startswith("SET_")), None)
    material_id = int(float(numbers[0])) if numbers else -1
    return {
        "id": material_id,
        "name": strings[0] if strings else None,
        "local": strings[1] if len(strings) > 1 else (strings[0] if strings else None),
        "symbol": strings[1] if len(strings) > 1 else None,
        "numbers": numbers,
        "texture": texture,
        "magical": "MAGICAL" in args,
        "explicit_hazards": [flag for flag in ("EXPLOSIVE", "FLAMMABLE") if flag in args],
        "color": extract_color(args),
    }


def factory_kind(factory: str) -> str:
    f = factory.rstrip("_")
    if factory in SKIP_FACTORIES or f in SKIP_FACTORIES:
        return "skip"
    if factory in ORE_FACTORIES or f.startswith("oredust"):
        return "ore"
    if factory in ALLOY_FACTORIES or f.startswith("alloy") or f.startswith("slloy") or f.startswith("clloy"):
        return "alloy"
    if factory in GEM_FACTORIES or f.startswith("valgem") or factory == "diamond":
        return "gem"
    if factory in WOOD_FACTORIES or f.startswith("wood"):
        return "wood"
    if factory in STONE_FACTORIES or factory == "brick":
        return "stone"
    if factory in GAS_FACTORIES or factory in LIQUID_FACTORIES:
        return "gas"
    if factory in DUST_FACTORIES or f.startswith("dust"):
        return "dust"
    if factory in {"elec", "gemelec", "crystalelec", "oredustelec", "gaselec", "lqudelec", "dustelec",
                   "stoneelec", "brickelec", "valgemelec"} or f.endswith("elec"):
        return "elec"
    if factory in {"cent", "gemcent", "crystalcent", "oredustcent", "gascent", "lqudcent", "dustcent",
                   "stonecent", "brickcent", "valgemcent", "glowstone", "redstone"} or f.endswith("cent"):
        return "cent"
    if factory == "clay":
        return "clay"
    if factory in METAL_FACTORIES or f.startswith("metal") or f.startswith("setal") or f.startswith("cetal"):
        return "metal"
    if factory in ELEMENT_FACTORIES or f in {"alkali", "alkaline", "lanthanide", "actinide", "transmetal",
                                               "refractmetal", "platingroup", "posttrans", "precmetal", "noblemetal",
                                               "metalloid", "diatomic", "polyatomic", "noblegas", "diatomicgas"}:
        return "element"
    if factory == "create":
        return "create"
    return "dust"


def infer_texture(kind: str, parsed: dict, factory: str | None = None) -> str:
    if parsed.get("texture"):
        return SET_MAP.get(parsed["texture"], "DULL")
    # MT:187-190: the stone helpers inherit SET_STONE even when their
    # processing kind is emitted as elec/cent rather than a plain stone.
    if factory in {"stone", "stonedcmp", "stonecent", "stoneelec"}:
        return "STONE"
    defaults = {
        "metal": "METALLIC",
        "alloy": "METALLIC",
        "ore": "STONE",
        "gem": "RUBY",
        "wood": "WOOD",
        "stone": "STONE",
        "gas": "FINE",
        "element": "FINE",
        "dust": "FINE",
        "create": "DULL",
        "elec": "SHINY",
        "cent": "RUBY",
        "clay": "ROUGH",
    }
    return defaults.get(kind, "DULL")


JAVA_RESERVED = {
    "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
    "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
    "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
    "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
    "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
    "volatile", "while",
}


def java_field_name(field: str) -> str:
    # Flat catalogs need separate keys for qualified woods with root-level names.
    if field in {"WOODS_Cinnamon", "WOODS_Lime", "WOODS_Ash"}:
        field = field.removeprefix("WOODS_")
    name = field.replace("-", "_")
    if not name or not re.match(r"[A-Za-z_][A-Za-z0-9_]*$", name):
        name = "M_" + re.sub(r"[^A-Za-z0-9]", "", field)
    if name[0].isdigit():
        name = "M_" + name
    if name in JAVA_RESERVED:
        name = name + "_"
    return name


def decode_java_string(text: str) -> str:
    try:
        return text.encode("utf-8").decode("unicode_escape")
    except UnicodeDecodeError:
        return text


def subscript(count: int) -> str:
    subs = "₀₁₂₃₄₅₆₇₈₉"
    if count < 0:
        return ""
    if count < 10:
        return subs[count]
    return "".join(subs[int(c)] for c in str(count))


def parse_amount_units(token: str) -> int:
    token = token.strip().replace(" ", "")
    m = re.fullmatch(r"(\d+)\*U", token)
    if m:
        return int(m.group(1))
    if token == "U":
        return 1
    m = re.fullmatch(r"U(\d+)", token)
    if m:
        return 1  # one U9 portion; formula uses ratio to U
    return 1


def parse_component_args(arg_str: str) -> list[tuple[str, str]]:
    tokens = split_args(arg_str)
    if not tokens:
        return []
    start = 1 if tokens and INT_RE.fullmatch(tokens[0]) else 0
    pairs: list[tuple[str, str]] = []
    i = start
    while i + 1 < len(tokens):
        mat = tokens[i].strip()
        amt = tokens[i + 1].strip()
        if re.match(r"[A-Za-z_][A-Za-z0-9_]*$", mat) and ("U" in amt or INT_RE.fullmatch(amt)):
            pairs.append((mat, amt))
            i += 2
        else:
            i += 1
    return pairs


def find_chained_call_args(line: str, method: str) -> list[str]:
    results: list[str] = []
    needle = f".{method}("
    start = 0
    while True:
        idx = line.find(needle, start)
        if idx < 0:
            break
        open_idx = idx + len(needle) - 1
        args = extract_balanced_args(line, open_idx)
        if args is not None:
            results.append(args)
        start = idx + len(needle)
    return results


def parse_metadata(full_line: str) -> dict:
    meta: dict = {"tooltip": None, "qual": None, "components": []}
    tooltip_m = re.search(r'\.tooltip\("((?:\\.|[^"\\])*)"\)', full_line)
    if tooltip_m:
        meta["tooltip"] = decode_java_string(tooltip_m.group(1))
    qual_m = re.search(r"\.qual\(\s*(\d+)\s*,\s*([\d.]+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)", full_line)
    if qual_m:
        meta["qual"] = (
            int(qual_m.group(1)),
            float(qual_m.group(2)),
            int(qual_m.group(3)),
            int(qual_m.group(4)),
        )
    # Preserve source call order. A single numeric argument is harvest quality,
    # not a replacement for factory/inherited tool type, speed or durability.
    # Material-copy qual(OtherMaterial) and unsupported expressions stay explicit;
    # do not silently reinterpret them as numeric/default-zero quality.
    meta["qual_calls"] = []
    meta["unresolved_qual_calls"] = []
    for args in find_chained_call_args(full_line, "qual"):
        single = re.fullmatch(r"\s*([+-]?\d+)[lL]?\s*", args)
        full = re.fullmatch(r"\s*(\d+)\s*,\s*([\d.]+)\s*,\s*(\d+)\s*,\s*(\d+)\s*", args)
        if single:
            meta["qual_calls"].append(("harvest", int(single.group(1))))
        elif full:
            meta["qual_calls"].append(("stats", (int(full.group(1)), float(full.group(2)),
                                                 int(full.group(3)), int(full.group(4)))))
        else:
            meta["unresolved_qual_calls"].append(args.strip())
    for method in ("uumAloy", "uumMcfg", "setMcfg", "setAloy", "mcfg"):
        for args in find_chained_call_args(full_line, method):
            meta["components"].extend(parse_component_args(args))
    return meta


def build_formula(components: list[tuple[str, str]], symbols: dict[str, str]) -> str:
    if not components:
        return ""
    parts: list[str] = []
    for mat_field, amount_token in components:
        sym = symbols.get(mat_field) or mat_field
        ratio = parse_amount_units(amount_token)
        if ratio > 1:
            parts.append(sym + subscript(ratio))
        else:
            parts.append(sym)
    return "".join(parts)


CS_C = 273
DEFAULT_MELT = 1000
DEFAULT_BOIL = 3000
DEFAULT_DENSITY = 1.0

CHAIN_STATS_METHODS = (
    "uumAloy", "uumMcfg", "setAloy", "setMcfg", "mcfg",
    "heat", "setDensity", "setStats", "setStatsElement",
)


def format_density(density: float) -> str:
    text = f"{density:g}"
    if "." not in text:
        return f"{text}.0F"
    return f"{text}F"


CS_CONSTANTS = {
    "WEIGHT_AIR_G_PER_CUBIC_CENTIMETER": 0.0012,
    "WEIGHT_AIR_KG_PER_CUBIC_METER": 1.2,
}


def eval_stats_expr(expr: str, registry: dict[str, dict[str, float]]) -> float:
    text = re.sub(r"\s*\.\s*", ".", expr.strip())
    if text in CS_CONSTANTS:
        return CS_CONSTANTS[text]
    text = re.sub(r"\bCS\.C\b", str(CS_C), text)
    text = re.sub(
        r"([A-Za-z_][A-Za-z0-9_]*)\.mGramPerCubicCentimeter",
        lambda m: str(registry.get(m.group(1), {}).get("density", DEFAULT_DENSITY)),
        text,
    )
    text = re.sub(
        r"([A-Za-z_][A-Za-z0-9_]*)\.mMeltingPoint",
        lambda m: str(int(registry.get(m.group(1), {}).get("melt", DEFAULT_MELT))),
        text,
    )
    text = re.sub(
        r"([A-Za-z_][A-Za-z0-9_]*)\.mBoilingPoint",
        lambda m: str(int(registry.get(m.group(1), {}).get("boil", DEFAULT_BOIL))),
        text,
    )
    text = re.sub(
        r"([A-Za-z_][A-Za-z0-9_]*)\.mPlasmaPoint",
        lambda m: str(int(registry.get(m.group(1), {}).get("plasma", DEFAULT_BOIL * 3))),
        text,
    )
    if not re.fullmatch(r"[\d+\-*/().\s]+", text):
        raise ValueError(f"unsupported stats expression: {expr!r} -> {text!r}")
    return float(eval(text, {"__builtins__": {}}, {}))


def apply_molecule_average(components: list[tuple[str, str]], registry: dict[str, dict[str, float]]) -> dict[str, float]:
    if not components:
        return {"melt": DEFAULT_MELT, "boil": DEFAULT_BOIL, "density": DEFAULT_DENSITY}
    divider = sum(parse_amount_units(amt) for _, amt in components)
    if divider <= 0:
        return {"melt": DEFAULT_MELT, "boil": DEFAULT_BOIL, "density": DEFAULT_DENSITY}
    melt = boil = dens = 0.0
    for mat, amt in components:
        units = parse_amount_units(amt)
        src = registry.get(mat, {"melt": DEFAULT_MELT, "boil": DEFAULT_BOIL, "density": DEFAULT_DENSITY})
        melt += src["melt"] * units / divider
        boil += src["boil"] * units / divider
        dens += src["density"] * units / divider
    melt_i = max(1, int(melt))
    boil_i = max(melt_i + 1, int(boil))
    return {"melt": melt_i, "boil": boil_i, "density": dens}


def initial_stats_from_factory(parsed: dict) -> dict[str, float]:
    nums = parsed.get("numbers", [])
    if len(nums) >= 6 and parsed.get("symbol"):
        return {
            "melt": int(float(nums[3])),
            "boil": int(float(nums[4])),
            "density": float(nums[5]),
        }
    return {"melt": DEFAULT_MELT, "boil": DEFAULT_BOIL, "density": DEFAULT_DENSITY}


def apply_stats_chain(full_line: str, stats: dict[str, float], registry: dict[str, dict[str, float]]) -> dict[str, float]:
    calls: list[tuple[int, str, str]] = []
    for method in CHAIN_STATS_METHODS:
        needle = f".{method}("
        start = 0
        while True:
            idx = full_line.find(needle, start)
            if idx < 0:
                break
            open_idx = idx + len(needle) - 1
            args = extract_balanced_args(full_line, open_idx)
            if args is not None:
                calls.append((idx, method, args))
            start = idx + len(needle)
    calls.sort(key=lambda item: item[0])

    out = dict(stats)
    for _, method, args in calls:
        parts = split_args(args)
        if method in {"uumAloy", "uumMcfg", "setAloy", "setMcfg", "mcfg"}:
            out = apply_molecule_average(parse_component_args(args), registry)
        elif method == "heat":
            if len(parts) == 1 and re.fullmatch(r"[A-Za-z_][A-Za-z0-9_]*", parts[0]):
                ref = registry.get(parts[0], out)
                out["melt"] = int(ref.get("melt", DEFAULT_MELT))
                out["boil"] = int(ref.get("boil", DEFAULT_BOIL))
            elif len(parts) == 1:
                melt = int(eval_stats_expr(parts[0], registry))
                out["melt"] = melt
                out["boil"] = max(melt + 1, melt * 2)
            elif len(parts) >= 2:
                melt = int(eval_stats_expr(parts[0], registry))
                boil = int(eval_stats_expr(parts[1], registry))
                out["melt"] = melt
                out["boil"] = max(melt + 1, boil)
        elif method == "setDensity" and parts:
            out["density"] = float(eval_stats_expr(parts[0], registry))
        elif method == "setStats" and len(parts) >= 3:
            out["melt"] = int(eval_stats_expr(parts[0], registry))
            out["boil"] = int(eval_stats_expr(parts[1], registry))
            out["density"] = float(eval_stats_expr(parts[2], registry))
        elif method == "setStatsElement" and len(parts) >= 5:
            out["density"] = float(eval_stats_expr(parts[4], registry))
    return out


def resolve_all_stats(order: list[str], materials: dict[str, tuple[str, str, str]]) -> dict[str, dict[str, float]]:
    registry: dict[str, dict[str, float]] = {}
    for field in order:
        if field not in materials:
            continue
        _factory, args, full_line = materials[field]
        parsed = parse_strings_and_numbers(split_args(args))
        meta = parse_metadata(full_line)
        stats = initial_stats_from_factory(parsed)
        stats = apply_stats_chain(full_line, stats, registry)
        registry[field] = stats
    return registry


def stats_suffix(stats: dict[str, float]) -> str:
    return f".setStats({int(stats['melt'])}, {int(stats['boil'])}, {format_density(stats['density'])})"


def emit_metadata_suffix(field: str, meta: dict, symbols: dict[str, str]) -> str:
    suffix = ""
    tooltip = meta.get("tooltip") or build_formula(meta.get("components", []), symbols)
    if not tooltip:
        tooltip = symbols.get(field, "")
    if tooltip:
        escaped = tooltip.replace("\\", "\\\\").replace('"', '\\"')
        suffix += f'.setTooltipChemical("{escaped}")'
    calls = meta.get("qual_calls")
    if calls is None:  # Older programmatic metadata callers retain their existing contract.
        calls = [("stats", meta["qual"])] if meta.get("qual") else []
    for kind, value in calls:
        if kind == "harvest":
            suffix += f".setToolQuality({value})"
        else:
            tool_types, speed, durability, quality = value
            speed_text = f"{speed:g}" if speed == int(speed) else str(speed)
            suffix += f".setToolStats({tool_types}, {speed_text}F, {durability}, {quality})"
    return suffix


def emit_material(
        field: str, factory: str, parsed: dict, meta: dict, symbols: dict[str, str],
        stats: dict[str, float]) -> str | None:
    kind = factory_kind(factory)
    if kind == "skip":
        return None
    if parsed["id"] < 0 or parsed["id"] >= 10000 or not parsed["name"]:
        return None
    if field in SKIP_MATERIAL_FIELDS and not (field == "Magic" and factory == "woodnormal"):
        return None
    if parsed["id"] in SKIP_MATERIAL_IDS:
        return None
    if parsed["name"] in {"NULL", "Empty", "Organic", "Crystal", "Unknown", "Cobblestone", "Magic"}:
        return None

    tex = infer_texture(kind, parsed, factory)
    color = parsed["color"]
    name = parsed["name"]
    local = parsed["local"] or name
    id_ = parsed["id"]
    nums = parsed["numbers"]
    melt, boil = int(stats["melt"]), int(stats["boil"])
    density_text = format_density(stats["density"])

    props: list[str] = []
    if kind == "metal":
        call = "metal"
        if len(nums) >= 6 and parsed.get("symbol"):
            p, n = int(float(nums[1])), int(float(nums[2]))
            body = f'{call}({id_}, "{name}", "{parsed["symbol"]}", {p}, {n}, {melt}, {boil}, {density_text}, {color})'
        else:
            sym = (parsed.get("symbol") or name[:2]).replace('"', "")
            body = f'metal({id_}, "{name}", "{sym}", 0, 0, {melt}, {boil}, {density_text}, {color})'
    elif kind == "element":
        if len(nums) >= 6 and parsed.get("symbol"):
            p, n = int(float(nums[1])), int(float(nums[2]))
            extra = ", MaterialProperty.GAS" if kind == "gas" or factory in GAS_FACTORIES else ""
            body = f'element({id_}, "{name}", "{parsed["symbol"]}", {p}, {n}, {melt}, {boil}, {density_text}, {color}{extra})'
        else:
            body = f'dust({id_}, "{name}", {color})'
    elif kind == "ore":
        body = f'ore({id_}, "{name}", {color})'
    elif kind == "gem":
        body = f'gem({id_}, "{name}", {color}, MaterialTextureSet.{tex})'
        line_body = body
        if local != name:
            line_body += f'.setLocalName("{local}")'
        line_body += stats_suffix(stats)
        line_body += emit_metadata_suffix(field, meta, symbols)
        return f"            {java_field_name(field)} = {line_body},"
    elif kind == "elec":
        body = f'elec({id_}, "{name}", {color}, MaterialTextureSet.{tex})'
        line_body = body
        if local != name:
            line_body += f'.setLocalName("{local}")'
        line_body += stats_suffix(stats)
        line_body += emit_metadata_suffix(field, meta, symbols)
        return f"            {java_field_name(field)} = {line_body},"
    elif kind == "cent":
        body = f'cent({id_}, "{name}", {color}, MaterialTextureSet.{tex})'
        line_body = body
        if local != name:
            line_body += f'.setLocalName("{local}")'
        line_body += stats_suffix(stats)
        line_body += emit_metadata_suffix(field, meta, symbols)
        return f"            {java_field_name(field)} = {line_body},"
    elif kind == "clay":
        body = f'clay({id_}, "{name}", {color})'
    elif kind == "alloy":
        body = f'alloy({id_}, "{name}", {color})'
    elif kind == "wood":
        if factory == "woodnormal":
            # MT.woodnormal's final numbers are RGB, speed, durability, not heat/density.
            # Its qualified WOODS.Magic field is unrelated to the Magic pseudo-element.
            speed, durability = float(nums[-2]), int(float(nums[-1]))
            props = ', MaterialProperty.MAGICAL' if parsed.get('magical') else ''
            body = f'woodNormal({id_}, "{name}", "{local}", {color}, {speed:g}F, {durability}{props})'
            return f"            {java_field_name(field)} = {body},"
        body = f'wood({id_}, "{name}", "{local}", {color})'
    elif kind == "stone":
        body = f'stone({id_}, "{name}", "{local}", {color})'
    elif kind == "gas":
        body = f'gas({id_}, "{name}", {color})'
    else:
        body = f'dust({id_}, "{name}", {color})'

    # Preserve the two explicit hazard flags already adopted in the port. Other
    # factory-inherited properties still need their own source/consumer audit.
    if field in {"Gunpowder", "Dynamite"}:
        if parsed.get('explicit_hazards') != ['EXPLOSIVE', 'FLAMMABLE']:
            raise ValueError(f'Original hazard flags changed for {field}')
        body += '.put(MaterialProperty.EXPLOSIVE, MaterialProperty.FLAMMABLE)'
    line_body = body + f".setTextureSet(MaterialTextureSet.{tex})"
    if local != name:
        line_body += f'.setLocalName("{local}")'
    embeds_stats = kind == "metal" or (kind == "element" and len(nums) >= 6 and parsed.get("symbol"))
    if not embeds_stats:
        line_body += stats_suffix(stats)
    line_body += emit_metadata_suffix(field, meta, symbols)
    # GT6's diatomic/noble gas helpers are still elements with atomic counts.
    # Keeping the gas factory preserves their forms; the original setStats call
    # supplies the proton/neutron values used by Manual_Elements and chemistry.
    if kind == "gas" and len(nums) >= 6 and parsed.get("symbol"):
        protons, neutrons = int(float(nums[1])), int(float(nums[2]))
        line_body += (f'.setAtomicProperties(new AtomicProperties({protons}, {protons}, '
                      f'{neutrons}, 0)).put(MaterialProperty.ELEMENT)')
    if parsed.get("symbol") and field not in symbols:
        pass  # symbol already used via emit_metadata_suffix fallback
    return f"            {java_field_name(field)} = {line_body},"


def map_amount_expr(token: str) -> str | None:
    """Map a GT6 U-fraction amount expression (U, U2, 3*U4, U*2, 0, ...) to a port Java expression."""
    text = token.strip().replace(" ", "")
    if re.fullmatch(r"\d+", text):
        return text
    m = re.fullmatch(r"(?:(\d+)\*)?U(\d*)", text)
    if m:
        mult = int(m.group(1)) if m.group(1) else 1
        div = int(m.group(2)) if m.group(2) else 1
    else:
        m = re.fullmatch(r"U(\d*)\*(\d+)", text)
        if not m:
            return None
        div = int(m.group(1)) if m.group(1) else 1
        mult = int(m.group(2))
    base = U_CONSTANTS.get(div, f"(CS.U / {div})")
    return base if mult == 1 else f"{mult} * {base}"


def resolve_material_token(token: str, placement: dict[str, str],
                           aliases: dict[str, str] | None = None) -> str | None:
    """Map an MT.java material reference (Fe, OREMATS.Galena, ...) to a generated-field reference."""
    text = token.strip()
    for prefix in MT_NESTED_PREFIXES:
        if text.startswith(prefix):
            text = text[len(prefix):]
            break
    if not re.fullmatch(r"[A-Za-z_][A-Za-z0-9_]*", text):
        return None
    if aliases:
        for _ in range(8):
            if text in placement or text not in aliases:
                break
            text = aliases[text]
    return placement.get(text)


ALIAS_RE = re.compile(r"\b([A-Za-z_]\w*)\s*=\s*(?:OREMATS\.|STONES\.|WOODS\.)?([A-Za-z_]\w*)\s*[,;]")


def collect_aliases(source: str) -> dict[str, str]:
    """Plain field aliases like {@code VanadiumPentoxide = V2O5,} in MT.java."""
    aliases: dict[str, str] = {}
    for alias, target in ALIAS_RE.findall(source):
        if alias != target and alias not in aliases:
            aliases[alias] = target
    return aliases


NAME_TERNARY_RE = re.compile(r'^"((?:\\.|[^"\\])*)"\.equals\(aNameOreDict\)\s*\?\s*(\S+)\s*:\s*(\S+)$')


def eval_name_ternary(arg: str, material_name: str | None) -> str:
    """Evaluate a helper-factory {@code "X".equals(aNameOreDict) ? A : B} argument for a material."""
    match = NAME_TERNARY_RE.match(arg.strip())
    if not match or material_name is None:
        return arg
    return match.group(2) if match.group(1) == material_name else match.group(3)


def find_ore_proc_calls(full_line: str) -> list[tuple[int, str, str]]:
    calls: list[tuple[int, str, str]] = []
    for method in ORE_PROC_METHODS:
        needle = f".{method}("
        start = 0
        while True:
            idx = full_line.find(needle, start)
            if idx < 0:
                break
            args = extract_balanced_args(full_line, idx + len(needle) - 1)
            if args is not None:
                calls.append((idx, method, args))
            start = idx + len(needle)
    calls.sort(key=lambda item: item[0])
    return calls


HELPER_DEF_RE = re.compile(r"static\s+OreDictMaterial\s+(\w+)\s*\([^)]*\)\s*\{\s*return\s+(\w+)\s*\(")
STANDALONE_OWNER_RE = re.compile(r"^((?:OREMATS|STONES|WOODS)\.)?([A-Za-z_][A-Za-z0-9_]*)\s*\.")


def collect_helper_calls(source: str) -> dict[str, list[tuple[str, str]]]:
    """Ore-proc calls embedded in MT.java factory helper methods, including inherited base-factory calls."""
    info: dict[str, tuple[str, list[tuple[str, str]]]] = {}
    for line in source.splitlines():
        match = HELPER_DEF_RE.search(line)
        if not match:
            continue
        calls = [(method, args) for _idx, method, args in find_ore_proc_calls(line)]
        info[match.group(1)] = (match.group(2), calls)

    resolved: dict[str, list[tuple[str, str]]] = {}

    def resolve(name: str, seen: set[str]) -> list[tuple[str, str]]:
        if name in resolved:
            return resolved[name]
        if name not in info or name in seen:
            return []
        seen.add(name)
        base, calls = info[name]
        result = list(resolve(base, seen)) + calls
        resolved[name] = result
        return result

    for name in info:
        resolve(name, set())
    return {name: calls for name, calls in resolved.items() if calls}


def emit_ore_proc_call(owner: str, context: str, method: str, args: str,
                       placement: dict[str, str], aliases: dict[str, str]) -> str | None:
    parts = split_args(args)
    if method == "ores":
        refs = [resolve_material_token(p, placement, aliases) for p in parts]
        if not refs or any(r is None for r in refs):
            missing = [p.strip() for p, r in zip(parts, refs) if r is None]
            print(f"[ore-proc] skip {context}.ores: unknown {missing}", file=sys.stderr)
            return None
        return f"        {owner}.ores({', '.join(refs)});"
    if method in {"setSmelting", "setCrushing"}:
        if len(parts) != 2:
            print(f"[ore-proc] skip {context}.{method}: bad args {args!r}", file=sys.stderr)
            return None
        mat = "null" if parts[0].strip() == "null" else resolve_material_token(parts[0], placement, aliases)
        amount = map_amount_expr(parts[1])
        if mat is None or amount is None:
            print(f"[ore-proc] skip {context}.{method}({args.strip()}): unmapped argument", file=sys.stderr)
            return None
        return f"        {owner}.{method}({mat}, {amount});"
    if method == "setOreMultiplier":
        text = args.strip()
        if not re.fullmatch(r"\d+", text):
            print(f"[ore-proc] skip {context}.setOreMultiplier({text}): non-literal", file=sys.stderr)
            return None
        return f"        {owner}.setOreMultiplier({text});"
    return None


def build_ore_processing(source: str, materials: dict[str, tuple[str, str, str]],
                         placement: dict[str, str]) -> tuple[list[str], int]:
    """Emit Holder.Field.ores(...)/setSmelting(...)/... statements in MT.java source order."""
    helper_calls = collect_helper_calls(source)
    aliases = collect_aliases(source)
    statements: list[str] = []
    skipped = 0
    for raw_line in source.splitlines():
        stripped = raw_line.strip()
        if stripped.startswith("//") or "static OreDictMaterial" in stripped:
            continue
        has_calls = any(f".{m}(" in stripped for m in ORE_PROC_METHODS)
        assign = ASSIGN_RE.match(stripped)
        field = None
        material_name: str | None = None
        inherited: list[tuple[str, str]] = []
        if assign:
            field = assign.group(1)
            stored = materials.get(field)
            if stored is None or stored[2] != stripped.rstrip(","):
                if has_calls:
                    skipped += 1
                    print(f"[ore-proc] skip duplicate/unknown assignment: {stripped[:80]}", file=sys.stderr)
                continue
            raw_inherited = helper_calls.get(assign.group(2), [])
            if raw_inherited:
                material_name = parse_strings_and_numbers(split_args(stored[1])).get("name")
                inherited = [
                    (method, ", ".join(eval_name_ternary(p, material_name) for p in split_args(args)))
                    for method, args in raw_inherited
                ]
        elif has_calls:
            owner_match = STANDALONE_OWNER_RE.match(stripped)
            field = owner_match.group(2) if owner_match else None
            if field is None:
                skipped += 1
                print(f"[ore-proc] skip unattributable statement: {stripped[:80]}", file=sys.stderr)
                continue
        else:
            continue
        if not has_calls and not inherited:
            continue
        owner = placement.get(field)
        if owner is None:
            skipped += 1
            print(f"[ore-proc] skip {field}: material not emitted ({stripped[:60]})", file=sys.stderr)
            continue
        calls = inherited + [(method, args) for _idx, method, args in find_ore_proc_calls(stripped)]
        seen_owners = set()
        for method, args in calls:
            statement = emit_ore_proc_call(owner, field, method, args, placement, aliases)
            if statement is None:
                skipped += 1
            else:
                if owner not in seen_owners:
                    seen_owners.add(owner)
                    statements.append(f"        {owner}.put(MaterialProperty.GENERATE_ORE_PROCESSING);")
                statements.append(statement)
    return statements, skipped


def format_class_fields(entries: list[str]) -> list[str]:
    if not entries:
        return []
    lines = ["        public static final GTMaterial"]
    for i, entry in enumerate(entries):
        body = entry.strip()
        if body.endswith(","):
            body = body[:-1]
        suffix = ";" if i == len(entries) - 1 else ","
        lines.append(f"            {body}{suffix}")
    return lines


def collect_materials(source: str) -> tuple[list[str], dict[str, tuple[str, str, str]]]:
    methods: dict[str, tuple[str, str]] = {}
    for line in source.splitlines():
        match = METHOD_LINE_RE.search(line)
        if match:
            methods[match.group(1)] = (match.group(2), match.group(3))

    order: list[str] = []
    materials: dict[str, tuple[str, str, str]] = {}

    for line in source.splitlines():
        parsed = parse_assignment(line)
        if parsed is None:
            continue
        field, factory, args = parsed
        if factory == "woodnormal" and field in {"Cinnamon", "Lime", "Ash"}:
            field = "WOODS_" + field
        full_line = line.strip().rstrip(",")
        if factory in methods and args == "":
            base_factory, base_args = methods[factory]
            if field not in materials:
                order.append(field)
                materials[field] = (base_factory, base_args, full_line)
            continue
        if factory in methods:
            continue
        if field in materials:
            continue
        order.append(field)
        materials[field] = (factory, args, full_line)

    return order, materials


def infer_alias_symbol(full_line: str, symbols: dict[str, str]) -> str:
    meta = parse_metadata(full_line)
    components = meta.get("components", [])
    if len(components) == 1:
        return symbols.get(components[0][0], "")
    steal_m = re.search(r"\.steal\((\w+)\)", full_line)
    if steal_m:
        return symbols.get(steal_m.group(1), "")
    return ""


def build_symbol_map(materials: dict[str, tuple[str, str, str]]) -> dict[str, str]:
    symbols: dict[str, str] = {}
    for field, (factory, args, full_line) in materials.items():
        parsed = parse_strings_and_numbers(split_args(args))
        meta = parse_metadata(full_line)
        if meta.get("tooltip"):
            symbols[field] = meta["tooltip"]
        elif parsed.get("symbol") and len(parsed.get("numbers", [])) >= 8:
            symbols[field] = parsed["symbol"]

    changed = True
    while changed:
        changed = False
        for field, (_factory, _args, full_line) in materials.items():
            alias = infer_alias_symbol(full_line, symbols)
            if alias and symbols.get(field) != alias:
                symbols[field] = alias
                changed = True
    return symbols


def generate_java(source: str, order: list[str], materials: dict[str, tuple[str, str, str]]) -> str:
    buckets: dict[str, list[str]] = {
        "Elements": [],
        "Compounds": [],
        "Ores": [],
        "Stones": [],
        "Woods": [],
    }

    placement: dict[str, str] = {}
    seen_ids: set[int] = set()
    symbols = build_symbol_map(materials)
    stats_registry = resolve_all_stats(order, materials)
    for field in order:
        if field not in materials:
            continue
        factory, args, full_line = materials[field]
        parsed = parse_strings_and_numbers(split_args(args))
        meta = parse_metadata(full_line)
        if parsed["id"] in seen_ids and parsed["id"] > 0:
            continue
        stats = stats_registry.get(field, {"melt": DEFAULT_MELT, "boil": DEFAULT_BOIL, "density": DEFAULT_DENSITY})
        emitted = emit_material(field, factory, parsed, meta, symbols, stats)
        if not emitted:
            continue
        seen_ids.add(parsed["id"])
        kind = factory_kind(factory)
        if kind in {"metal", "element", "gas"} and parsed.get("symbol"):
            class_name = "Elements"
        elif kind in {"ore"}:
            class_name = "Ores"
        elif kind in {"stone"}:
            class_name = "Stones"
        elif kind in {"wood"}:
            class_name = "Woods"
        else:
            class_name = "Compounds"
        buckets[class_name].append(emitted)
        placement[field] = f"{class_name}.{java_field_name(field)}"

    for (class_name, anchor), extras in EXTRA_ENTRIES.items():
        entries = buckets[class_name]
        anchor_idx = next((i for i, e in enumerate(entries) if f" {anchor} = " in e), None)
        insert_at = len(entries) if anchor_idx is None else anchor_idx + 1
        for offset, (extra_field, extra_line) in enumerate(extras):
            entries.insert(insert_at + offset, extra_line)
            placement[extra_field] = f"{class_name}.{java_field_name(extra_field)}"

    ore_proc, _skipped = build_ore_processing(source, materials, placement)
    print(f"Carried over {len(ore_proc)} ore-processing calls ({_skipped} skipped)")

    lines = [
        "package com.gregtech.gregtech.data.generated;",
        "",
        "import com.gregtech.gregtech.api.material.GTMaterial;",
        "import com.gregtech.gregtech.api.material.AtomicProperties;",
        "import com.gregtech.gregtech.api.material.MaterialProperty;",
        "import com.gregtech.gregtech.api.material.MaterialTextureSet;",
        "import com.gregtech.gregtech.data.CS;",
        "",
        "import static com.gregtech.gregtech.data.ImportedMaterialData.*;",
        "",
        "/** Auto-transpiled from GregTech 6 {@code gregapi.data.MT}. Do not edit by hand. */",
        "public final class GT6Materials {",
        "    private GT6Materials() {}",
        "",
        "    public static void register() {",
        "        touchAll();",
        "        applyOreProcessing();",
        "    }",
        "",
        "    public static void touchAll() {",
        "        // Class literals alone do not initialize nested static material fields; touch one constant per holder.",
        "        GTMaterial marker = Elements.Ac;",
        "        marker = Compounds.Adamantine;",
        "        marker = Ores.AgI;",
        "        marker = Stones.Andesite;",
        "        marker = Woods.Acacia;",
        "        if (marker == null) {",
        "            throw new IllegalStateException(\"GT6Materials failed to initialize\");",
        "        }",
        "    }",
        "",
        "    /** Ore byproducts, smelting/crushing targets, and ore multipliers from GT6 {@code MT.java}. */",
        "    public static void applyOreProcessing() {",
    ]
    lines.extend(ore_proc)
    lines.append("    }")
    lines.append("")

    for class_name, entries in buckets.items():
        if not entries:
            continue
        lines.append(f"    public static final class {class_name} {{")
        lines.append(f"        private {class_name}() {{}}")
        lines.append("")
        lines.extend(format_class_fields(entries))
        lines.append("    }")
        lines.append("")

    lines.append("}")
    lines.append("")
    return "\n".join(lines)


def split_ore_processing(java: str) -> tuple[str, str]:
    """Keep material fields compatible with existing tools, emit processing separately."""
    signature = "    public static void applyOreProcessing() {\n"
    start = java.index(signature) + len(signature)
    end = java.index("\n    }", start)
    body = java[start:end]
    processing = """package com.gregtech.gregtech.data.generated;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.data.CS;
import static com.gregtech.gregtech.data.generated.GT6Materials.*;

/** Generated ore processing links. Edit tools/transpile_gt6_materials.py, not this file. */
public final class MaterialOreProcessing {
    private MaterialOreProcessing() {}
    public static void apply() {
""" + body + "\n    }\n}\n"
    return java[:start] + "        MaterialOreProcessing.apply();" + java[end:], processing


def generate_material_models() -> int:
    created = 0
    for set_dir in sorted(TEXTURES.iterdir()):
        if not set_dir.is_dir():
            continue
        set_name = set_dir.name
        out_dir = MODELS / set_name
        out_dir.mkdir(parents=True, exist_ok=True)
        for prefix in PREFIXES:
            model_path = out_dir / f"{prefix}.json"
            if model_path.exists():
                continue
            overlay = f"{prefix}_overlay"
            content = f"""{{
  "parent": "minecraft:item/generated",
  "textures": {{
    "layer0": "gt6ce:item/material_icons/{set_name}/{prefix}",
    "layer1": "gt6ce:item/material_icons/{set_name}/{overlay}"
  }}
}}
"""
            model_path.write_text(content, encoding="utf-8")
            created += 1
    return created


def main() -> None:
    source = read_source()
    order, materials = collect_materials(source)
    java, processing = split_ore_processing(generate_java(source, order, materials))
    OUT.mkdir(parents=True, exist_ok=True)
    out_file = OUT / "GT6Materials.java"
    write_catalog(java)
    (OUT / "MaterialOreProcessing.java").write_text(readable_java(processing), encoding="utf-8")
    models = generate_material_models()
    print(f"Parsed {len(materials)} raw entries, wrote {out_file}")
    print(f"Created {models} shared material model JSON files")


if __name__ == "__main__":
    main()
