# Solid-ore Centrifuge: eight-profile expansion

2026-10-24. **Full corrected verification passed: Forge 329 + NeoForge 320 = 649 actual native cases.** Both targeted unchanged-production baselines reproduced all 16 new semantic failures before correction.

The preceding Copper/Galena/Nether Quartz slice passed its full local 617-case gate and was pushed as `9ce86aa7e`; its remote CI failed in two existing fixtures: Forge313 had only the drill-torch cancellation/air assertion failure, and NeoForge304 had only the stone-layer slab seed-search failure. All six positive-ore cases passed on both remote loaders. The current649 branch state supersedes that snapshot and is not merged; the full retry includes both test-only fixture corrections. See the [torch fixture audit](TORCH_PLACEMENT_FIXTURE_AUDIT.md). See [that audit](POSITIVE_ORE_CENTRIFUGE_AUDIT.md). This expansion does not include pending quality/importer or host-physics work.

## Sixteen literal source rows

Each material has one whole-input and one tiny-input case. Inputs are one `crushedPurified` or `crushedPurifiedTiny`, respectively. All four ordered outputs use **`crushedCentrifugedTiny`**: primary material first, then the three byproducts below. Counts are **9 in every slot for whole input, 1 in every slot for tiny input**. Ordered chances are **100%, 10%, 10%, 10%** (`10000,1000,1000,1000`); all rows use **16 EU/t**, with no input/output fluids.

| Material / core ID / quality | Ordered byproducts | Whole ticks | Tiny ticks |
| --- | --- | ---: | ---: |
| Chalcopyrite / 9111 / 0 | Pyrite; Cobaltite; Cadmium | 144 | 16 |
| Pentlandite / 9145 / 0 | Hematite; Sulfur; Cobaltite | 144 | 16 |
| Sphalerite / 9130 / 0 | Cadmium; Gallium; Zinc | 144 | 16 |
| Tetrahedrite / 9132 / 0 | Copper; Antimony; Zinc | 144 | 16 |
| Pyrite / 9125 / 0 | Sulfur; Phosphorus; Hematite | 144 | 16 |
| Nickel / 280 / 2 | Cobaltite; Cooperite; Hematite | 432 | 48 |
| Cooperite / 9116 / 0 | Palladium; Nickel; Iridium | 144 | 16 |
| YellowLimonite / 9137 / 0 | Nickel; BrownLimonite; Cobaltite | 144 | 16 |

Work is `144/16 × (quality + 1)`. Pyrite's Phosphorus is **compound ID8208, melting at829 K**, not elemental Phosphor. Gallium melts at302 K, above ambient293 K; all reviewed byproducts have positively established solid ambient phase, not merely absent fluid holders.

## Source and implementation boundaries

References are original `3703e40308c8c030763fd6297dea8b210d2a77b1` and W `607771f74acb0fe647f7e9412676e72094799545`. Original `Loader_Recipes_Ores.java:383,464–470` and W `:387,468–474` define this producer. Effective ORES eligibility comes from factory/group ancestry, not port item existence or `.ores(...)` (which only appends byproducts).

The frozen paired census is local `work/positive-ore-centrifuge/expansion-census/census.json`, SHA-256 `3425db7651b0954bdb2adb2108b8459b44bf86d80dcc2cdcb767a37b921e7872`. It records all20 identities, original/W declaration chains, quality, thermal calculations, first-three ordering and compatibility chronology. Conditional later `GT_API_Post` additions to five owners append after the initial three entries and do not change their order. The earlier fluorite group append does not include these owners. Cached core item definitions are explicitly not live registry or accepted-recipe evidence.

Production patch `932cb9766de44464c6afbaffc56a206e244cca2545cdd7ad7a709dd1a902f70e` adds only eight literal profiles and comments to `ReviewedOreCentrifugeFacts`. It preserves the previous three profiles, guard/algorithm methods, collision-respecting insertion and legacy suppression. No global material/prefix metadata changes are included. Unknown identities, changed facts and relevant externally owned forms still defer as `UNKNOWN`; unrelated custom prefixes do not own these ore routes.

## Actual targeted baseline, not a full 649-case run

Only `gregtech_ore_source_audit` was selected, with unchanged production at `9ce86aa7e` and the new tests/checker. Before/after source snapshots were byte-identical.

| Loader | Exact selected cases | Prior controls passed | New semantic failures | Process result |
| --- | ---: | ---: | ---: | --- |
| Forge | 55 | 39 | 16 | Exit1; 4m34s |
| NeoForge | 55 | 39 | 16 | Exit1; 3m7s |

On **each loader**, all eight tiny inputs had no accepted row; all eight whole inputs retained the wrong ordered forms/counts: one whole refined primary plus tiny dust of only the second byproduct. Material identity, ownership, quality, byproduct and native form prerequisites passed before these assertions. Later chance/work assertions were not reached: their mismatch is source-derived, not separately demonstrated by these failed assertions.

Exact discovery checks rejected extras/duplicates and verified all55 identities/template namespaces. Their successful exit means **discovery**, not gameplay success. Unrelated namespaces were intentionally not run; this baseline does not establish that all prior617 cases passed again.

Local evidence: `work/validation/ore-centrifuge-eight-baseline/local-validation-receipt.md`, loader XML/logs and input/final snapshots. XML SHA-256:

- Forge: `ac7fabc224e92075e1d579ea939a14f305a8bce58f272303d87465b6e9775777`.
- NeoForge: `8016ed9739b04f5ef4940d04e99d8652e7360154eddee01a848686badfd6574d`.

## Tests and remaining gate

`SolidOreCentrifugeTests` supplies16 literal whole/tiny methods under `gregtech_ore_source_audit:test_empty`. They require actual native registry IDs and prefix/material binding, literal source quality/first-three ordering, exactly one accepted matching recipe, actual runtime lookup identity, ordered outputs/counts/chances, fluid arrays and literal work/EUt. They neither insert expected recipes nor call the producer. Actual unrelated `sampleBlank` witnesses cover Cadmium/Copper/Nickel/Palladium; Pentlandite/Pyrite use Copper only as the explicitly labeled shared addon-enabled control.

The strict checker extension preserves all617 previous IDs and the original27 source cases; both-loader missing/failed/errored/skipped/wrong-namespace and qualified/class-suffix spoof controls passed in the29-test Python candidate suite. Cached compilation passed both native APIs, but compilation and synthetic reports are not native success.

**Fixed gate passed: Forge329 + NeoForge320 =649 actual cases**, plus dual-platform build/core, 161 Python guards, 491 REMI groups and package/SDK checks. These are accepted recipe-map/lookup tests, not complete machine inventory/energy/cycle execution. Relevant external-form rejection, collisions and late lifecycle behavior remain source-reviewed rather than dynamically proved by these positives. No whole-mod parity or release-readiness claim follows.

## Final local 649-case receipt

Forge 329/329 (7m39s), NeoForge 320/320 (4m10s), zero failures/errors/skips, native and strict-checker exits 0. Dual/core build, 161 Python guards, 491 REMI groups, 1039 shared packaged classes per loader and SDK 118 core + 46 native passed. Runtime/test/CI inputs remained unchanged during the gate; documentation updates are recorded separately. Evidence: `work/validation/ore-centrifuge-eight-fixed-retry/local-validation-receipt.md`. Forge XML SHA256 `924819b1dce8bf7948943a729b1d3f2a9845a1650e7a091b2bd16459c69552f8`; NeoForge `fddc4111b1b35f3fc0a0c621b99fb2d8e50c9e713865d3b26030dd659860154f`. This does not relabel the earlier remote 9ce86aa7e failure as passed.
