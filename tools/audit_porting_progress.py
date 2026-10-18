"""Audit how much of GregTech 6 the port still has not implemented.

Reads both source trees plus the reports the GameTests already write, and prints
(and writes docs/porting-progress.json) a coverage snapshot:

  * code scale of both trees
  * subsystem file counts, GT6 next to the port
  * registry counts from the last GameTest run log
  * recipe coverage from docs/transpiled-recipes-coverage.json and
    docs/material-form-conversions.json
  * blocked/missing content from docs/missing-content-tokens.txt

Usage: python tools/audit_porting_progress.py [--log build/gametest.log]
"""

from __future__ import annotations

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import argparse
import json
import os
import re
import sys
from collections import Counter
from pathlib import Path

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = Path(__file__).resolve().parents[1]
GT6 = Path(r"F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java")
PORT_JAVA = native_path(ROOT / "src/main/java/com/gregtech/gregtech")
DOCS = ROOT / "docs"

# label -> (GT6 relative dir, port relative dirs)
SUBSYSTEMS: list[tuple[str, str, tuple[str, ...]]] = [
    ("多方块机器 multiblocks", "gregtech/tileentity/multiblocks", ("content/multiblock",)),
    ("传送门/空间站 portals", "gregtech/tileentity/portals", ()),
    ("传感器 sensors", "gregtech/tileentity/sensors", ("api/sensor", "blockentity/sensor", "block/sensor")),
    ("能源机器 energy", "gregtech/tileentity/energy", ("blockentity/energy", "block/energy", "content/energy", "api/energy")),
    ("电池 batteries", "gregtech/tileentity/batteries", ("api/energy",)),
    ("储罐 tanks", "gregtech/tileentity/tanks", ("blockentity/machine",)),
    ("工具方块 tools", "gregtech/tileentity/tools", ("blockentity/tool", "block/tool", "content/tool")),
    ("容器/库存 inventories", "gregtech/tileentity/inventories", ("blockentity/inventory", "block/inventory", "api/inventory")),
    ("面板 panels", "gregtech/tileentity/panels", ("content/cover",)),
    ("可放置物 placeables", "gregtech/tileentity/placeables", ()),
    ("植物 plants", "gregtech/tileentity/plants", ()),
    ("其它机器 misc", "gregtech/tileentity/misc", ()),
    ("延伸器 extenders", "gregtech/tileentity/extenders", ("content/transport",)),
    ("自动工具 autotools", "gregtech/tileentity/autotools", ()),
    ("电脑/CC computer", "gregtech/tileentity/computer", ()),
    ("食物 food", "gregtech/tileentity/food", ()),
    ("红石 redstone", "gregtech/tileentity/redstone", ()),
    ("覆盖板 covers", "gregapi/cover", ("content/cover", "api/sensor")),
    ("物品工具 items/tools", "gregtech/items/tools", ("api/tool", "client/tool")),
    ("物品行为 behaviors", "gregtech/items/behaviors", ("blockentity/behavior",)),
    ("木头方块 blocks/wood", "gregtech/blocks/wood", ("block/wood",)),
    ("树木 blocks/tree", "gregtech/blocks/tree", ()),
    ("工具方块 blocks/tool", "gregtech/blocks/tool", ("block/tool",)),
    ("石材 blocks/stone", "gregtech/blocks/stone", ("block/stone",)),
    ("流体方块 blocks/fluids", "gregtech/blocks/fluids", ("block/misc",)),
    ("配方加载器 loaders", "gregtech/loaders/c", ("loaders/c",)),
    ("兼容模块 compat", "gregtech/compat", ("integration",)),
    ("世界生成 worldgen", "gregtech/worldgen", ("worldgen",)),
    ("GUI", "gregapi/gui", ("client/gui",)),
    ("渲染 render", "gregapi/render", ("client",)),
    ("网络 network", "gregapi/network", ("network",)),
    ("矿物词典层 oredict", "gregapi/oredict", ("api/prefix", "api/material")),
    ("伤害 damage", "gregapi/damage", ()),
    ("附魔 enchants", "gregapi/enchants", ()),
    ("木头字典 wooddict", "gregapi/wooddict", ()),
    ("核心修改 asm", "gregtech/asm", ()),
]

# keyword probes for things that live in no single directory on the GT6 side
PROBES: list[tuple[str, str]] = [
    ("手册/书籍 books", r"MultiItemBooks|Loader_Books|LoaderBookList|MultiTileEntityBookShelf|Behavior_WrittenBook"),
    ("养蜂 apiculture", r"IItemBumbleBee|MultiTileEntityBumbleHive|WorldgenHives"),
    ("地牢 dungeon", r"DungeonChunk"),
    ("作物 crops", r"MultiTileEntityCrop|CropStick|Loader_Recipes_Crops"),
    ("物流 logistics", r"MultiTileEntityLogistics|LogisticsCore"),
    ("战利品表 loot", r"Loader_Loot|LootPool|LootTable"),
    ("村民交易 trades", r"TradeOffer|MerchantRecipe|VillagerProfession"),
]


def count_java(root: Path) -> tuple[int, int]:
    files = list(root.rglob("*.java"))
    lines = 0
    for path in files:
        try:
            lines += sum(1 for _ in path.open("r", encoding="utf-8", errors="replace"))
        except OSError:
            pass
    return len(files), lines


def java_files(path: Path) -> int:
    return len(list(path.rglob("*.java"))) if path.exists() else 0


def probe(pattern: str, root: Path) -> int:
    rx = re.compile(pattern)
    return sum(1 for path in root.rglob("*.java") if rx.search(path.name) or rx.search(
        path.open("r", encoding="utf-8", errors="replace").read(200_000)))


def read_json(path: Path) -> dict:
    if not path.exists():
        return {}
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return {}


def registry_counts(log: Path) -> dict:
    out: dict[str, object] = {}
    if not log.exists():
        return out
    loaded = re.compile(r"loaded: (\d+) materials, (\d+) items, (\d+) blocks")
    fluids = re.compile(r"Registered (\d+) GT6 fluids")
    # A startup message may begin with "Queued" but continue on the next log line.
    # Never let a registration category consume another timestamped message.
    queued = re.compile(r"Queued (\d+) ([^,\r\n]+?) for registration")
    tail = log.read_text(encoding="utf-8", errors="replace")
    for match in loaded.finditer(tail):
        out["materials"], out["items"], out["blocks"] = (int(g) for g in match.groups())
    for match in fluids.finditer(tail):
        out["fluids"] = int(match.group(1))
    entries = {}
    for match in queued.finditer(tail):
        entries[match.group(2).strip()] = int(match.group(1))
    if entries:
        out["queued"] = entries
    return out


def missing_tokens(path: Path) -> dict:
    if not path.exists():
        return {}
    kinds: Counter[str] = Counter()
    total = 0
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        total += 1
        kinds[line.split(":", 1)[0].lstrip("\ufeff")] += 1
    return {"total": total, "byPrefix": dict(kinds.most_common())}


def refresh_missing_tokens(log: Path, path: Path) -> dict:
    """Snapshot the run's unresolved content from the log, then report on the fresh list.

    The log line is the live truth ("accepted-missing content (N): a, b, c" from GTGeneratedChem);
    the text file is only its checked-in copy, so it is rewritten here instead of read as-is.
    """
    tokens: list[str] = []
    if log.exists():
        match = re.search(r"accepted-missing content \(\d+\): ([^\n]*)",
                          log.read_text(encoding="utf-8", errors="replace"))
        if match:
            tokens = sorted({token.strip() for token in match.group(1).split(",") if token.strip()})
    if tokens:
        path.write_text("\n".join(tokens) + "\n", encoding="utf-8")
    return missing_tokens(path)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--log", default=str(ROOT / "build/gametest.log"))
    args = parser.parse_args()

    port_files, port_lines = count_java(PORT_JAVA)
    gt_files, gt_lines = count_java(GT6)

    subsystems = []
    for label, gt_rel, port_rels in SUBSYSTEMS:
        gt_count = java_files(GT6 / gt_rel)
        port_count = sum(java_files(PORT_JAVA / rel) for rel in port_rels)
        subsystems.append({"label": label, "gt6": gt_count, "port": port_count})

    probes = [{"label": label, "gt6": probe(pattern, GT6), "port": probe(pattern, PORT_JAVA)}
              for label, pattern in PROBES]

    transpiled = read_json(DOCS / "transpiled-recipes-coverage.json")
    conversions = read_json(DOCS / "material-form-conversions.json")
    report = {
        "scale": {
            "port": {"files": port_files, "lines": port_lines},
            "gt6": {"files": gt_files, "lines": gt_lines},
        },
        "subsystems": subsystems,
        "probes": probes,
        "registry": registry_counts(Path(args.log)),
        "recipes": {
            "transpiled": transpiled,
            "recipeMapTotals": conversions.get("recipeMapTotals", {}),
            "formConversions": conversions.get("byKind", {}),
            "craftingGridConversions": conversions.get("craftingGridRecipes"),
        },
        "missingContent": refresh_missing_tokens(Path(args.log), DOCS / "missing-content-tokens.txt"),
    }
    DOCS.mkdir(exist_ok=True)
    (DOCS / "porting-progress.json").write_text(
        json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")

    print(f"PORT: {port_files} java files / {port_lines} lines")
    print(f"GT6 : {gt_files} java files / {gt_lines} lines")
    print(f"ratio: files {port_files / gt_files:.2f}, lines {port_lines / gt_lines:.2f}")
    print("\n-- subsystems (gt6 files -> port files) --")
    for row in subsystems:
        print(f"  {row['label']:28} {row['gt6']:4} -> {row['port']:4}")
    print("\n-- probes (files matching, gt6 vs port) --")
    for row in probes:
        print(f"  {row['label']:28} {row['gt6']:4} vs {row['port']:4}")
    print("\n-- registry --")
    print("  " + json.dumps(report["registry"], ensure_ascii=False))
    print("\n-- recipes --")
    print("  transpiled sets: " + json.dumps(
        {k: v for k, v in transpiled.items() if k.startswith("set.")}, ensure_ascii=False))
    print("  totals: added={} skipped={}".format(
        transpiled.get("totalAdded"), transpiled.get("totalSkipped")))
    print("  machine conversions: " + json.dumps(conversions.get("byKind", {}), ensure_ascii=False))
    print("  crafting conversions: " + str(conversions.get("craftingGridRecipes")))
    print("\n-- missing content tokens --")
    print("  " + json.dumps(report["missingContent"], ensure_ascii=False))
    print("\nwrote docs/porting-progress.json")


if __name__ == "__main__":
    main()
