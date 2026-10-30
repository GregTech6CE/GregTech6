#!/usr/bin/env python3
"""Diff GT6CE's machine recipes against a dump of the original GT6 recipe tables.

Inputs
    --original DIR   index.json + maps/*.json dumped from original GT6 (the CrucibleCraft
                     gt6_dump/gt6_recipe_dump tree). Only recipes listed there are compared.
    --port DIR       index.json + maps/*.json written by gt6ce's RecipeDump:
                     run the game once with GT6CE_RECIPE_DUMP=<dir> (or -Dgt6ce.recipeDump=<dir>).

Dependencies
    Python 3.10+. Standard library only.

Output
    A text report on stdout: per-map row counts, matched / missing / extra / divergent rows, and
    the missing rows grouped by output. --json PATH additionally writes the full lists (useful for
    turning gaps into cards). Exit status is 0 even when gaps exist; it reports, it does not judge.

How to run
    python tools/compare_gt6_recipes.py --original <gt6-dump>/ --port work/recipe-dump
    python tools/compare_gt6_recipes.py --original <gt6-dump>/ --port work/recipe-dump \
        --map gt.recipe.crusher --limit 20 --json work/recipe-gap.json

Identity
    Material items are compared as GT6 meta items (prefix + material id); the port's per-pair item
    registry names are not used. Fluid names are unified through the original fluid catalog
    (fluid_map.json), so "molten.cobalt" and the port's sanitised "molten_cobalt" compare equal.
    The original's multi-tile forms (pipes, wires and capsule cells) live in
    ``gt.multitileentity`` metas; tools/gt6_multiblock_identities.json maps each meta to the
    ``gt.meta.<form>#<material id>`` identity the port's dump writes for those blocks, and the
    port's ``fluid_cell_<material>`` items resolve to ``gt.meta.capcellcon#<material id>``
    (see CELL_ITEMS below). The original's storage/casing/crate meta names (``storage.dust``,
    ``machine.double``, ``crate.64.raw`` ...) are mapped onto the OP prefix names the port uses.
    Stack data (NBT / components) is absent on both sides, so rows that differ only by stack data
    compare as equal. Rows are keyed by their item/fluid inputs plus counts; a matched row whose
    outputs differ is reported as divergent.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from collections import defaultdict
from pathlib import Path

GT_META = "gregtech:gt.meta."
GT_META_PORT = "gt6ce:gt.meta."
PORT_NAMESPACES = ("gt6ce:", "minecraft:")
TRANSLATION_PREFIXES = ("item.", "block.", "fluid.")
TRANSLATION_NAMESPACES = ("gt6ce", "gregtech", "minecraft")

# GT6's multi-tile forms (pipes, wires, capsule cells) are `gt.multitileentity` metas; the generated
# table maps each meta to "<OP prefix>#<material id>", the identity the port's dump writes for the
# same block. Regenerate with tools/extract_gt6_multiblock_identities.py.
MULTIBLOCK_METAS = json.loads(
    (Path(__file__).with_name("gt6_multiblock_identities.json")).read_text(encoding="utf-8")
)["metas"]

# The original keeps storage/casing/crate blocks in meta items named after the multi-item rather
# than the OP prefix; the port writes the OP prefix name. Map the original names onto that spelling.
FORM_ALIASES = {
    "storage.solid": "blockSolid",
    "storage.dust": "blockDust",
    "storage.plate": "blockPlate",
    "storage.plateGem": "blockPlateGem",
    "storage.gem": "blockGem",
    "storage.ingot": "blockIngot",
    "storage.raw": "blockRaw",
    "machine": "casingMachine",
    "machine.double": "casingMachineDouble",
    "machine.quadruple": "casingMachineQuadruple",
    "machine.dense": "casingMachineDense",
    "crate.raw": "crateGtRaw",
    "crate.gem": "crateGtGem",
    "crate.dust": "crateGtDust",
    "crate.ingot": "crateGtIngot",
    "crate.plate": "crateGtPlate",
    "crate.plateGem": "crateGtPlateGem",
    "crate.64.raw": "crateGt64Raw",
    "crate.64.gem": "crateGt64Gem",
    "crate.64.dust": "crateGt64Dust",
    "crate.64.ingot": "crateGt64Ingot",
    "crate.64.plate": "crateGt64Plate",
    "crate.64.plateGem": "crateGt64PlateGem",
    # Forms the port registers under another spelling of the same form: OP.casingSmall is the port's
    # itemCasing and OP.chemtube its glasstube (see PrefixRegistry.ALIASES), and the original's boule
    # meta item is OP.bouleGt.
    "casingSmall": "itemCasing",
    "chemtube": "glasstube",
    "boule": "bouleGt",
}

# The port's capsule cells are items rather than form items, so the dump cannot name them; their
# material ids must match the capcellcon ids in the generated table.
CELL_ITEMS = {
    "cell_adamantium": 2220, "cell_aluminium": 130, "cell_desh": 8750, "cell_draconium": 8791,
    "cell_draconium_awakened": 8792, "cell_duranium": 8751, "cell_efrine": 8747,
    "cell_elven_elementium": 8722, "cell_gaia_spirit": 8735, "cell_gold": 790,
    "cell_hsla_tungsten_alloy": 8766, "cell_infinity": 8801, "cell_invar": 8661,
    "cell_kreknorite": 1260, "cell_manasteel": 8720, "cell_netherite": 8745,
    "cell_palladium": 460, "cell_plastic": 8218, "cell_stainless_steel": 8636,
    "cell_syrmorite": 8785, "cell_tantalum_hafnium_carbide": 8802, "cell_terrasteel": 8721,
    "cell_thaumium": 8679, "cell_tin": 500, "cell_tin_alloy": 8667, "cell_titanium": 220,
    "cell_trinitanium": 8790, "cell_tungsten": 740, "cell_tungsten_carbide": 8638,
    "cell_tungsten_steel": 8635, "cell_void_metal": 8681, "cell_wax": 8235,
    "cell_wax_amnesic": 8280, "cell_wax_bee": 8236, "cell_wax_magic": 8240,
    "cell_wax_paraffin": 8238, "cell_wax_plant": 8239, "cell_wax_refractory": 8237,
    "cell_wax_soulful": 8281, "cell_workers_alloy": 8780,
}
CAPCELLCON_KEY = "gt.meta.capcellcon#"
CELL_MATERIAL_IDS = {
    int(value.split("#")[1]) for value in MULTIBLOCK_METAS.values() if value.startswith("capcellcon#")
}
# Both spellings must name the same forty materials; a one-sided edit would silently unpair rows.
assert len(CELL_ITEMS) == len(CELL_MATERIAL_IDS) == 40
assert set(CELL_ITEMS.values()) == CELL_MATERIAL_IDS


def normalize_name(text: str) -> str:
    """Reduce a display name or a residual translation key to comparable words."""
    lowered = text.strip().lower()
    for prefix in TRANSLATION_PREFIXES:
        if lowered.startswith(prefix):
            lowered = lowered[len(prefix):]
            head, _, tail = lowered.partition(".")
            if tail and head in TRANSLATION_NAMESPACES:
                lowered = tail
            break
    return re.sub(r"[^a-z0-9]", "", lowered)


# Non-material items whose concept both sides share but whose registry shape differs.
#   * GT6's selector tags are one item with meta = tag number; the port registers one item per tag.
#   * GT6 stone blocks are one item per stone with the variant as meta (BlockStones order); the port
#     registers one block per (stone, variant).
#   * GT6 glass is one item per style with the colour as meta (DYE_OREDICTS order); the port
#     registers one block per (style, colour).
STONE_VARIANTS = (
    "stone", "cobble", "cobble_mossy", "bricks", "bricks_cracked", "bricks_mossy",
    "bricks_chiseled", "smooth", "bricks_reinforced", "bricks_redstoned", "tiles",
    "small_tiles", "small_bricks", "windmill_tiles_a", "windmill_tiles_b", "square_bricks",
)
STONE_NAMES = {
    "granite_black": "granite.black",
    "granite_red": "granite.red",
    "prismarine_dark": "prismarine.dark",
    "prismarine_light": "prismarine.light",
}
GLASS_COLORS = {
    "black": 0, "red": 1, "green": 2, "brown": 3, "blue": 4, "purple": 5, "cyan": 6,
    "light_gray": 7, "gray": 8, "pink": 9, "lime": 10, "yellow": 11, "light_blue": 12,
    "magenta": 13, "orange": 14, "white": 15,
}
STONE_PORT = re.compile(r"gt6ce:stone_(.+)_(" + "|".join(STONE_VARIANTS) + r")$")
GLASS_PORT = re.compile(r"gt6ce:glass_(clear|glow)(_slab)?_(" + "|".join(GLASS_COLORS) + r")$")
# Original panels are multi-tile items: concrete 32452-32467, C-Foam 32468-32483 and asphalt
# 32484-32499, each with the colour as the meta offset (DYE_OREDICTS order). The port registers
# panel_<kind>_<colour> items; PanelCatalog keeps the same original ids.
PANEL_BASE = {"concrete": 32452, "cfoam": 32468, "asphalt": 32484}
PANEL_PORT = re.compile(r"gt6ce:panel_(concrete|cfoam|asphalt)_(" + "|".join(GLASS_COLORS) + r")$")
# Construction slabs: the original keeps one item per family with the colour as meta and uses a
# wildcard meta on the dyeing input (the port registers white-source rows only, so the wildcard
# stands for white). The port has only the C-Foam slab identity so far.
SLAB_PORT = re.compile(
    r"gt6ce:(concrete_reinforced|concrete|asphalt|cfoam)_slab_(" + "|".join(GLASS_COLORS) + r")$"
)
SLAB_PATHS = {"concrete": "concrete", "concrete_reinforced": "concrete.reinforced",
              "asphalt": "asphalt", "cfoam": "cfoam"}
# The construction blocks themselves: one item per family with the colour as meta.
CONSTRUCTION_PORT = re.compile(
    r"gt6ce:(concrete_reinforced|concrete|asphalt|cfoam)_(" + "|".join(GLASS_COLORS) + r")$"
)

# GT6 keeps its whole circuit chain in one `gt.multiitem.technological` item with a meta per part
# (MultiItemTechnological 30000-30499); the port registers one item per id
# (TechnologicalItemDefinitions). Both sides map to gt.tech#<meta>.
TECH_ITEMS = {
    # Circuit plates
    "circuit_plate": 30000, "circuit_plate_copper": 30002, "circuit_plate_gold": 30004,
    "circuit_plate_platinum": 30006, "circuit_plate_magic": 30012,
    "circuit_plate_enderium": 30014, "circuit_plate_signalum": 30016,
    "circuit_plate_hsla": 30099,
    # Circuit wiring
    "circuit_wiring_copper": 30001, "circuit_wire_gold": 30003, "circuit_wire_platinum": 30005,
    "circuit_wire_magic": 30011, "circuit_wire_enderium": 30013, "circuit_wire_signalum": 30015,
    # Circuit parts
    "circuit_part_basic": 30101, "circuit_part_good": 30102, "circuit_part_advanced": 30103,
    "circuit_part_elite": 30104, "circuit_part_master": 30105, "circuit_part_ultimate": 30106,
    "circuit_part_magic": 30111, "circuit_part_enderium": 30113, "circuit_part_signalum": 30115,
    "circuit_part_enderpearl": 30198, "circuit_part_endereye": 30199,
    # Circuit boards
    "circuit_board_basic": 30201, "circuit_board_good": 30202, "circuit_board_advanced": 30203,
    "circuit_board_elite": 30204, "circuit_board_master": 30205, "circuit_board_ultimate": 30206,
    "circuit_board_magic": 30211, "circuit_board_enderium": 30213,
    "circuit_board_signalum": 30215, "circuit_board_hsla_circuit": 30298,
    "circuit_board_power_module": 30299,
    # Soldered circuits (GT6 tiers T1-T6)
    "circuit_basic": 30301, "circuit_good": 30302, "circuit_advanced": 30303,
    "circuit_elite": 30304, "circuit_master": 30305, "circuit_ultimate": 30306,
    "circuit_magic": 30311, "circuit_enderium": 30313, "circuit_signalum": 30315,
    # Crystal circuits
    "crystal_circuit_diamond": 30401, "crystal_circuit_ruby": 30402,
    "crystal_circuit_emerald": 30403, "crystal_circuit_sapphire": 30404,
}


def non_material_key(item: str, meta) -> str | None:
    """Canonical identity for selector tags, stone blocks and glass; None for anything else."""
    if item == "gregapi:gt.integrated_circuit":
        return f"gt.item.integrated_circuit#{meta}"
    tag = re.match(r"gt6ce:integrated_circuit_(\d+)$", item)
    if tag:
        return f"gt.item.integrated_circuit#{tag.group(1)}"
    if item.startswith("gregtech:gt.stone."):
        return f"gt.stone.{item[len('gregtech:gt.stone.'):]}#{meta}"
    stone = STONE_PORT.match(item)
    if stone:
        name = STONE_NAMES.get(stone.group(1), stone.group(1))
        return f"gt.stone.{name}#{STONE_VARIANTS.index(stone.group(2))}"
    if item.startswith("gregtech:gt.block.glass"):
        return f"gt.glass{item[len('gregtech:gt.block.glass'):]}#{meta}"
    if item == "gregtech:gt.multiitem.technological" and isinstance(meta, int) and 30000 <= meta <= 30499:
        return f"gt.tech#{meta}"
    if item == "gregtech:gt.multitileentity" and isinstance(meta, int) and 32452 <= meta <= 32499:
        return f"gt.panel#{meta}"
    panel = PANEL_PORT.match(item)
    if panel:
        return f"gt.panel#{PANEL_BASE[panel.group(1)] + GLASS_COLORS[panel.group(2)]}"
    if item.startswith("gregtech:gt.block.") and item.endswith(".slab.0"):
        family = item[len("gregtech:gt.block."):-len(".slab.0")]
        if family in SLAB_PATHS.values():
            return f"gt.slab.{family}#{15 if meta == '*' or meta == 32767 else meta}"
    slab = SLAB_PORT.match(item)
    if slab:
        return f"gt.slab.{SLAB_PATHS[slab.group(1)]}#{GLASS_COLORS[slab.group(2)]}"
    if item.startswith("gregtech:gt.block.") and not item.endswith(".slab.0"):
        family = item[len("gregtech:gt.block."):]
        if family in SLAB_PATHS.values():
            return f"gt.block.{family}#{15 if meta == '*' or meta == 32767 else meta}"
    block = CONSTRUCTION_PORT.match(item)
    if block:
        return f"gt.block.{SLAB_PATHS[block.group(1)]}#{GLASS_COLORS[block.group(2)]}"
    tech = item[len("gt6ce:"):] if item.startswith("gt6ce:") else item
    if tech in TECH_ITEMS:
        return f"gt.tech#{TECH_ITEMS[tech]}"
    glass = GLASS_PORT.match(item)
    if glass:
        style = "" if glass.group(1) == "clear" else ".glow"
        slab = ".slab.0" if glass.group(2) else ""
        return f"gt.glass{style}{slab}#{GLASS_COLORS[glass.group(3)]}"
    return None


def load_dump(directory: Path) -> dict[str, dict]:
    index_path = directory / "index.json"
    if not index_path.is_file():
        raise SystemExit(f"not a recipe dump (no index.json): {directory}")
    index = json.loads(index_path.read_text(encoding="utf-8"))
    maps: dict[str, dict] = {}
    for entry in index.get("maps", []):
        name = entry.get("nameInternal") or ""
        if not name:
            continue
        path = directory / entry.get("file", f"maps/{name}.json")
        if not path.is_file():
            print(f"warning: {name}: missing file {path}", file=sys.stderr)
            continue
        maps[name] = json.loads(path.read_text(encoding="utf-8"))
    return maps


def fluid_aliases(original_dir: Path) -> dict[str, str]:
    """Map every spelling of an original fluid name onto the original name."""
    path = original_dir / "oredict" / "fluid_map.json"
    if not path.is_file():
        return {}
    aliases: dict[str, str] = {}
    for name in json.loads(path.read_text(encoding="utf-8")):
        aliases[name] = name
        aliases[name.replace(".", "_")] = name
        aliases[name.replace(".", "_").replace("-", "_")] = name
        # Original names keep spaces ("molten hsla"); the port sanitises them to underscores.
        aliases[name.replace(".", "_").replace("-", "_").replace(" ", "_")] = name
    # The port's registry name for a fluid the original calls something else.
    aliases.setdefault("rc lubricant", "lubricant")
    return aliases


def item_key(stack: dict) -> str:
    item = stack.get("item") or ""
    meta = stack.get("meta", 0)
    if item.startswith(GT_META):
        prefix = item[len(GT_META):]
        return f"gt.meta.{FORM_ALIASES.get(prefix, prefix)}#{meta}"
    if item.startswith(GT_META_PORT):
        prefix = item[len(GT_META_PORT):]
        return f"gt.meta.{FORM_ALIASES.get(prefix, prefix)}#{meta}"
    # Pipes, wires and cells: the original's multi-tile metas resolve to the same form identity the
    # port's dump writes for its blocks.
    if item == "gregtech:gt.multitileentity":
        mapped = MULTIBLOCK_METAS.get(str(meta))
        if mapped is not None:
            return "gt.meta." + mapped
    shared = non_material_key(item, meta)
    if shared is not None:
        return shared
    # The port's cell items are not form items; map them to the original shape identity.
    if item.startswith("gt6ce:fluid_cell_"):
        cell = CELL_ITEMS.get(item[len("gt6ce:fluid_"):])
        if cell is not None:
            return CAPCELLCON_KEY + str(cell)
    # Ordinary items have no shared registry name: the original uses its multiitem ids, the port
    # registers one item per concept. Their English display names are the only common identity.
    display = stack.get("displayName")
    if display:
        return f"name:{normalize_name(display)}"
    for namespace in PORT_NAMESPACES:
        if item.startswith(namespace):
            return item[len(namespace):]
    return item


def fluid_key(stack: dict, aliases: dict[str, str]) -> str:
    fluid = stack.get("fluid") or ""
    for namespace in PORT_NAMESPACES:
        if fluid.startswith(namespace):
            fluid = fluid[len(namespace):]
            break
    return aliases.get(fluid, fluid)


def stack_multiset(stacks: list[dict], key) -> tuple[tuple[str, int], ...]:
    counts: dict[str, int] = defaultdict(int)
    for stack in stacks:
        if not stack:  # the original dump keeps null placeholders for empty slots
            continue
        # GT6's anvil rows require an empty work-area slot; the port expresses that as a slot rule.
        if stack.get("item") == "gregapi:gt.empty_slot":
            continue
        counts[key(stack)] += int(stack.get("count", stack.get("amount", 1)) or 1)
    return tuple(sorted(counts.items()))


def signature(recipe: dict, aliases: dict[str, str]) -> tuple:
    return (
        stack_multiset(recipe.get("inputs", []), item_key),
        stack_multiset(recipe.get("fluidInputs", []), lambda s: fluid_key(s, aliases)),
    )


def output_signature(recipe: dict, aliases: dict[str, str]) -> tuple:
    return (
        stack_multiset(recipe.get("outputs", []), item_key),
        stack_multiset(recipe.get("fluidOutputs", []), lambda s: fluid_key(s, aliases)),
    )


def describe_items(stacks: list[dict], key) -> str:
    parts = []
    for stack in stacks:
        if not stack:
            continue
        name = stack.get("displayName") or key(stack)
        parts.append(f"{name} x{stack.get('count', 1)}")
    return " + ".join(parts) or "-"


def describe_fluids(stacks: list[dict], aliases: dict[str, str]) -> str:
    parts = []
    for stack in stacks:
        name = stack.get("displayName") or fluid_key(stack, aliases)
        parts.append(f"{name} {stack.get('amount', 0)}mB")
    return " + ".join(parts) or "-"


def describe_recipe(recipe: dict, aliases: dict[str, str]) -> str:
    inputs = [s for s in (describe_items(recipe.get("inputs", []), item_key),
                          describe_fluids(recipe.get("fluidInputs", []), aliases)) if s != "-"]
    outputs = [s for s in (describe_items(recipe.get("outputs", []), item_key),
                           describe_fluids(recipe.get("fluidOutputs", []), aliases)) if s != "-"]
    return f"{' + '.join(inputs) or '-'} -> {' + '.join(outputs) or '-'}"


def filter_rows(rows: list[dict], include_fake: bool, include_hidden: bool) -> list[dict]:
    kept = []
    for row in rows:
        if not include_fake and row.get("fake"):
            continue
        if not include_hidden and row.get("hidden"):
            continue
        if row.get("enabled") is False:
            continue
        kept.append(row)
    return kept


def compare_map(original: dict, port: dict, aliases: dict[str, str],
                include_fake: bool, include_hidden: bool) -> dict:
    original_rows = filter_rows(original.get("recipes", []), include_fake, include_hidden)
    port_rows = filter_rows(port.get("recipes", []), include_fake, include_hidden)

    # Recipes that only differ by stack data (wood species, circuit settings) share a signature on
    # both sides. Rows are therefore paired per signature group, and "differs" is judged on the
    # group's set of outputs rather than on one arbitrarily chosen row.
    original_by_signature: dict[tuple, list[dict]] = defaultdict(list)
    for row in original_rows:
        original_by_signature[signature(row, aliases)].append(row)
    port_by_signature: dict[tuple, list[dict]] = defaultdict(list)
    for row in port_rows:
        port_by_signature[signature(row, aliases)].append(row)

    matched = 0
    missing: list[dict] = []
    extra: list[dict] = []
    divergent: list[dict] = []
    for sig, original_group in original_by_signature.items():
        port_group = port_by_signature.get(sig, [])
        paired = min(len(original_group), len(port_group))
        if not port_group:
            missing.extend(original_group)
            continue
        missing.extend(original_group[paired:])
        port_outputs = {output_signature(row, aliases) for row in port_group}
        group_divergent = [row for row in original_group
                           if output_signature(row, aliases) not in port_outputs]
        divergent.extend({"original": row, "port": None} for row in group_divergent)
        matched += paired - min(len(group_divergent), paired)
    for sig, port_group in port_by_signature.items():
        original_group = original_by_signature.get(sig, [])
        extra.extend(port_group[len(original_group):])

    return {
        "original": len(original_rows),
        "port": len(port_rows),
        "matched": matched,
        "missing": missing,
        "extra": extra,
        "divergent": divergent,
    }


def print_map_details(name: str, result: dict, aliases: dict[str, str], limit: int) -> None:
    missing = result["missing"]
    if missing:
        by_output: dict[tuple, list[dict]] = defaultdict(list)
        for row in missing:
            by_output[output_signature(row, aliases)].append(row)
        print(f"\n  {name}: {len(missing)} rows only in the original, "
              f"{len(by_output)} distinct outputs (top {min(limit, len(by_output))})")
        ranked = sorted(by_output.values(), key=len, reverse=True)
        for group in ranked[:limit]:
            example = group[0]
            label = describe_recipe(example, aliases)
            print(f"    x{len(group):<4d} {label}")
    divergent = result["divergent"]
    if divergent:
        print(f"\n  {name}: {len(divergent)} rows whose inputs exist but outputs differ "
              f"(top {min(limit, len(divergent))})")
        for pair in divergent[:limit]:
            print(f"    original: {describe_recipe(pair['original'], aliases)}")
    extra = result["extra"]
    if extra:
        print(f"\n  {name}: {len(extra)} rows only in the port (top {min(limit, len(extra))})")
        for row in extra[:limit]:
            print(f"    {describe_recipe(row, aliases)}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--original", required=True, type=Path, help="original GT6 recipe dump")
    parser.add_argument("--port", required=True, type=Path, help="gt6ce recipe dump")
    parser.add_argument("--map", action="append", default=None, help="only this recipe map (repeatable)")
    parser.add_argument("--limit", type=int, default=15, help="detail rows per section (default 15)")
    parser.add_argument("--json", type=Path, default=None, help="write the full report as JSON here")
    parser.add_argument("--include-fake", action="store_true", help="keep viewer-only fake rows")
    parser.add_argument("--include-hidden", action="store_true", help="keep hidden rows")
    args = parser.parse_args()

    aliases = fluid_aliases(args.original)
    original_maps = load_dump(args.original)
    port_maps = load_dump(args.port)

    names = sorted(set(original_maps) | set(port_maps))
    if args.map:
        wanted = set(args.map)
        names = [name for name in names if name in wanted]
        for name in sorted(wanted - set(names)):
            print(f"warning: recipe map not in either dump: {name}", file=sys.stderr)

    report: dict[str, dict] = {}
    for name in names:
        if name not in original_maps:
            rows = filter_rows(port_maps[name].get("recipes", []), args.include_fake, args.include_hidden)
            report[name] = {"original": 0, "port": len(rows), "matched": 0, "missing": [], "extra": rows,
                            "divergent": []}
            continue
        if name not in port_maps:
            rows = filter_rows(original_maps[name].get("recipes", []), args.include_fake, args.include_hidden)
            report[name] = {"original": len(rows), "port": 0, "matched": 0, "missing": rows, "extra": [],
                            "divergent": []}
            continue
        report[name] = compare_map(original_maps[name], port_maps[name], aliases,
                                   args.include_fake, args.include_hidden)

    header = f"{'recipe map':52s} {'original':>9s} {'port':>9s} {'matched':>8s} {'missing':>8s} {'extra':>7s} {'outputs':>8s}"
    print(header)
    print("-" * len(header))
    totals = {"original": 0, "port": 0, "matched": 0, "missing": 0, "extra": 0, "divergent": 0}
    for name, result in sorted(report.items(), key=lambda kv: (len(kv[1]["missing"]), kv[0]), reverse=True):
        print(f"{name:52s} {result['original']:9d} {result['port']:9d} {result['matched']:8d} "
              f"{len(result['missing']):8d} {len(result['extra']):7d} {len(result['divergent']):8d}")
        totals["original"] += result["original"]
        totals["port"] += result["port"]
        totals["matched"] += result["matched"]
        totals["missing"] += len(result["missing"])
        totals["extra"] += len(result["extra"])
        totals["divergent"] += len(result["divergent"])
    print("-" * len(header))
    print(f"{'total':52s} {totals['original']:9d} {totals['port']:9d} {totals['matched']:8d} "
          f"{totals['missing']:8d} {totals['extra']:7d} {totals['divergent']:8d}")
    coverage = (100.0 * totals["matched"] / totals["original"]) if totals["original"] else 0.0
    print(f"\ncoverage: {totals['matched']}/{totals['original']} original rows matched ({coverage:.1f}%)")

    for name, result in sorted(report.items(), key=lambda kv: len(kv[1]["missing"]), reverse=True):
        if result["missing"] or result["divergent"] or result["extra"]:
            print_map_details(name, result, aliases, args.limit)

    if args.json:
        payload = {
            "totals": totals,
            "maps": {
                name: {
                    "original": result["original"],
                    "port": result["port"],
                    "matched": result["matched"],
                    "missing": [describe_recipe(row, aliases) for row in result["missing"]],
                    "extra": [describe_recipe(row, aliases) for row in result["extra"]],
                    "divergent": [describe_recipe(pair["original"], aliases)
                                  for pair in result["divergent"]],
                }
                for name, result in report.items()
            },
        }
        args.json.parent.mkdir(parents=True, exist_ok=True)
        args.json.write_text(json.dumps(payload, ensure_ascii=False, indent=1), encoding="utf-8")
        print(f"\nreport written to {args.json}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
