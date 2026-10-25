# Machine casing crafting and sensor interfaces — 2026-10-25

## Scope and reference baseline

This repair targets the main-line code based on `33444eacb96b69fed879cc5bb16d6ca9c840a0a6`. It restores machine casing workbench recipes and a bounded first group of sensor interfaces. It does not claim that every sensor or every original GT6 feature is aligned.

References were read only:

- Original GT6: [`3703e40308c8c030763fd6297dea8b210d2a77b1`](https://github.com/GregTech6/gregtech6/tree/3703e40308c8c030763fd6297dea8b210d2a77b1).
- Higher-version reference: [`607771f74acb0fe647f7e9412676e72094799545`](https://github.com/wolfram0108/gregtech6_w/tree/607771f74acb0fe647f7e9412676e72094799545).

## Machine casing workbench recipes

Original `Loader_OreProcessing.java:152–155` and the higher-version reference `:156–159` register all four casing grades with `YXX / XwX / XXY`: six plates of the corresponding thickness, two long rods, and a wrench produce one casing. Their condition is `ANTIMATTER.NOT`, not the Welder's smithable/nonflammable conditions.

The repair adds these rows to the shared `OriginalHandCraftingRows`, used by both native recipe-loading adapters. It requires actual registered output and ingredient forms, uses existing material-form ingredients and tool remainders, and disallows mirrored patterns. Existing casing block IDs and Welder recipes are unchanged.

The tested native roster on each loader contains:

- **1,272 eligible recipes** across the four grades.
- **24 missing-parts combinations** intentionally left without an invalid recipe.
- **60 eligible combinations outside the Welder's material conditions**, confirming that the hand recipes are not incorrectly restricted to Welder eligibility.

## Sensor root causes and repair boundaries

The crucible defect involved **both the host boundary and sensor-side measurement**, rather than only a missing display value:

- Small crucibles did not expose their stored material content as a weight source; the large crucible inherits this boundary. The inventory/cache is not the contents of the vessel. Both native hosts now expose content-only mass through `WeightSensorSource`; existing structural-part delegation is retained.
- The weight sensor's inventory fallback used an ore-recovery projection instead of physical item composition. It now reads registered composition first, preserves explicit empty composition, and falls back for missing block/stone/slab composition. Ore recovery multipliers are not applied to mass.
- Weightometer maximum remains **65,535**, including when no target is present; absence must not falsely satisfy FULL mode.
- Crucibles now expose compression as `ceil(content units × 1000 / U9)`, with matching capacity conversion. This is material quantity, not a fluid-mB reading. One ingot reads 9 Gibbl; small/large capacities read 144/3,888 Gibbl.
- Molds and fluid pipes already maintained temperature fields but lacked the sensor interface. The repair exposes those existing fields and limits rather than inventing new temperature simulation.
- Stackometer now counts occupied slots and total available slots, not item counts or stack limits divided by 64.

Source anchors: original `MultiTileEntitySmeltery:388,676–677`, `MultiTileEntityCrucible:444,686–687`; corresponding higher-version lines `425,713–714` and `479,721–722`. Original `OM.java:315–326`, Weightometer classes, `MultiTileEntityStackometer:43–72`, `MultiTileEntityMold:214–220`, and `MultiTileEntityPipeFluid:519–520` establish the measurement contracts.

## Verified native differential evidence

The fresh targeted run contains **13 cases per loader**: six casing cases and seven sensor cases.

- **Baseline:** each loader produces the expected **12 semantic failures and one passing Welder preservation control**. Missing workbench recipes, zero crucible mass, doubled raw-ore mass, missing compression interface, absent temperature exposure, and incorrect occupied-slot behavior are reproduced. Baseline compilation/setup failures are not counted as semantic evidence.
- **Fixed:** Forge **13/13 PASS**, NeoForge **13/13 PASS**, **26/26 total**. Exact case identity and absence of failures are checked from native XML reports.
- Each loader executes **16 actual workbench result-slot takes**: 12 ordinary aluminium/iron/steel crafts and four exact wrench-break-boundary crafts. Tests verify output count, consumption of six plates/two rods, 800 wrench wear, wrong-tool/material/grade rejection, and no mirror acceptance.
- Eight existing aluminium Welder routes remain present with their input counts, output count, duration and EU/t.
- Sensor witnesses include a small crucible reading 995 kg without counting its cache, a formed large crucible reading 1,870 kg directly and through its bound part, and three raw redstone items reading **4,232 rather than 8,464 kg**. Explicit iron composition, unknown items, material blocks, stone/slabs, compression rounding, changing temperatures, and occupied-slot transitions are covered.
- All seven sensor fixtures restore their owned 343-cell areas after each case. Frozen source hashes remain unchanged through the completed targeted runs.

## Final validation status

**Final local validation passed on the unchanged production/test source.** Forge executed 475 passing native tests and NeoForge 466 (941 total); all 895 explicitly required cases were present (452 Forge, 443 NeoForge). The additional executed cases belong to existing main-line structure-family tests. Both builds, core checks, source guards, 299 CI Python tests, 491 REMI checks, sediment registration, mod packaging, and SDK closure passed. SDK verification found 120 core API classes and 46 native API classes per loader. The earlier Python invocation lacked javac on PATH; the complete check was rerun successfully with the verified toolchain. Hosted CI for this new commit remains pending at publication.

Remaining progress-host interoperability work is a separate, unpromoted draft. This patch does not claim progress coverage for every host, display-only fluid-item distinctions, sampling-cadence parity, visual/client validation, or comprehensive sensor completion.
