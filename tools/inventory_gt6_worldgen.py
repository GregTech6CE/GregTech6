"""Inventory GT6's worldgen objects vs the port's ports of them.

Lists every `Worldgen*` class under GT6's worldgen packages, whether it appears in
Loader_Worldgen's registration block, and what the port does about it.

The port rarely keeps GT6's class names (it has one `GTRocksFeature` for `WorldgenRocks`, one
`GTTreesFeature` for the nine `WorldgenTree*` genera, …) and GT6 ships several generators for other
mods' dimensions, so the answer comes from `PORTED`/`SKIPPED` below first and only falls back to
grepping the port for the class name. Anything that is neither mapped nor mentioned is genuinely
missing.
"""

# Native paths are generated views; generators must use tools/native_tool.py.
import sys as _native_sys
from pathlib import Path as _NativePath
_native_sys.path.insert(0, str(_NativePath(__file__).resolve().parents[0]))
from native_layout import native_path


import glob
import io
import os
import re

GT6_ROOT = r"F:\Dev\GregTech6\gregtech6-master\gregtech6-master\src\main\java"
PORT_ROOT = str(native_path("src/main/java/com/gregtech/gregtech"))
LOADER = os.path.join(GT6_ROOT, "gregtech", "loaders", "b", "Loader_Worldgen.java")

# GT6 class -> the port file(s) that implement it (checked by hand against §47-§68 of the porting notes).
PORTED = {
    "WorldgenRocks": "worldgen/GTRocksFeature.java",
    "WorldgenDeepOcean": "worldgen/GTDeepOceanFeature.java",
    "WorldgenBlackSand": "worldgen/GTBlackSandFeature.java",
    "WorldgenTurf": "worldgen/GTTurfFeature.java",
    "WorldgenColtan": "worldgen/GTColtanFeature.java",
    "WorldgenOcean": "worldgen/GTWaterBodyFeature.java",
    "WorldgenRiver": "worldgen/GTWaterBodyFeature.java",
    "WorldgenSwamp": "worldgen/GTWaterBodyFeature.java",
    "WorldgenPit": "worldgen/GTPitFeature.java",
    "WorldgenFluidSpring": "worldgen/GTFluidSpringsFeature.java",
    "WorldgenBushes": "worldgen/GTBushesFeature.java",
    "WorldgenFlowers": "worldgen/GTSurfaceFloraFeature.java",
    "WorldgenStone": "worldgen/GTStoneLayersGen.java",
    "WorldgenOresSmall": "worldgen/GTSmallOreFeature.java",
    "WorldgenOresLarge": "worldgen/GTOreVeinFeature.java",
    "WorldgenOresBedrock": "worldgen/GTBedrockOreFeature.java",
    "WorldgenNetherCrystals": "worldgen/GTNetherScatterFeature.java",
    "WorldgenNetherClay": "worldgen/GTNetherDepositFeature.java",
    "WorldgenNetherQuartz": "worldgen/GTNetherDepositFeature.java",
    "WorldgenGlowtus": "worldgen/GTSurfaceFloraFeature.java",
    "WorldgenRacks": "worldgen/GTFeatures.java (rack/bookshelf scatter)",
    "WorldgenSticks": "worldgen/GTRocksFeature.java (twigs)",
    "WorldgenStoneLayers": "worldgen/GTStoneLayerFeature.java",
    "WorldgenFluid": "GT6's fluid-spring base class, see WorldgenFluidSpring",
    "WorldgenLogDry": "worldgen/GTSurfaceFloraFeature.java (dry log scatter)",
    "WorldgenLogFrozen": "worldgen/GTSurfaceFloraFeature.java (frozen log scatter)",
    "WorldgenLogMossy": "worldgen/GTSurfaceFloraFeature.java (mossy log scatter)",
    "WorldgenLogRotten": "worldgen/GTSurfaceFloraFeature.java (rotten log scatter)",
    "WorldgenCenterBiomes": "skipped below",
}
for _genus in ("BlueMahoe", "BlueSpruce", "Cinnamon", "Coconut", "Hazel", "Maple", "Rainbowood",
               "Rubber", "Willow"):
    PORTED["WorldgenTree" + _genus] = "worldgen/GTTreesFeature.java + GTTreeShapes.java"

# GT6 classes the port deliberately does not implement, with the reason.
SKIPPED = {
    "WorldgenObject": "GT6's base class for the generators below, not content",
    "WorldgenBlob": "GT6's base class for blob-shaped generators, not content",
    "WorldgenOnSurface": "GT6's base class for surface generators, not content",
    "WorldgenDungeonGT": "worldgen/GTDungeonFeature.java + worldgen/dungeon/GTDungeonChunk*.java (27-cell ledger: layout + 15 cell types + keys + Nether/End portals, see section 76/79)",
    "WorldgenHives": "worldgen/GTBumbleHivesFeature.java (see section 75)",
    "WorldgenCenterBiomes": "GT6 registers it disabled by default (world-centre feature)",
    "WorldgenStreets": "GT6 registers it disabled by default (world-centre feature)",
    "WorldgenNexus": "GT6 registers it disabled by default (world-centre feature)",
    "WorldgenBeacon": "GT6 registers it disabled by default (world-centre feature)",
    "WorldgenTesting": "GT6 registers it disabled by default (developer test world)",
    "WorldgenAetherRocks": "another mod's dimension (Aether)",
    "WorldgenAlfheimRocks": "another mod's dimension (Botania Alfheim)",
    "WorldgenErebusRocks": "another mod's dimension (Erebus)",
    "WorldgenMarsRocks": "another mod's dimension (Galacticraft)",
    "WorldgenMoonRocks": "another mod's dimension (Galacticraft)",
    "WorldgenPlanetRocks": "another mod's dimensions (Galacticraft)",
    "WorldgenOresVanilla": "vanilla ore generation, replaced by the port's own ore veins/tables",
}

loader = io.open(LOADER, encoding="utf-8", errors="replace").read()

port_sources = {}
for path in glob.glob(os.path.join(PORT_ROOT, "**", "*.java"), recursive=True):
    port_sources[path.replace("\\", "/")] = io.open(path, encoding="utf-8", errors="replace").read()

classes = []
for path in glob.glob(os.path.join(GT6_ROOT, "**", "Worldgen*.java"), recursive=True):
    name = os.path.basename(path)[:-5]
    enabled_in_loader = bool(re.search(r"new\s+" + re.escape(name) + r"\s*\(", loader))
    mentions = [p.split("gregtech/")[-1] for p, text in port_sources.items() if name in text]
    classes.append((name, enabled_in_loader, mentions))

classes.sort()
print("%-28s %-9s %s" % ("GT6 class", "in loader", "port"))
missing = []
for name, enabled, mentions in classes:
    if name in PORTED:
        where = PORTED[name]
    elif name in SKIPPED:
        where = SKIPPED[name]
    elif mentions:
        where = ", ".join(mentions[:2])
    else:
        where = "*** NOT PORTED ***"
        missing.append(name)
    print("%-28s %-9s %s" % (name, "yes" if enabled else "-", where))

print()
print("unclassified: %d %s" % (len(missing), missing))
print("not ported on purpose: %s"
      % sorted(n for n, reason in SKIPPED.items() if reason.startswith("NOT PORTED")))
