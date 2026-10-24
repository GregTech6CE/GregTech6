# Structure slabs, swamp generation boundaries and recipe-overlay controls

## Bounded fixes

- #65: existing vanilla cobblestone/mossy-cobblestone slabs follow the selected local stone layer. Preserve TOP/BOTTOM as existing GT slab facing and waterlogging; dry DOUBLE becomes the corresponding full block. Existing GT slabs, other slab/stair families, unusual wet doubles and missing targets are unchanged. This implements the reported structure-material consistency request, not an original GT6 stair feature.
- The report image is a pillager outpost at Y74 on1.21.1. Native watchtower resources contain exactly those two slab hosts. Both loaders register stone layers at UNDERGROUND_ORES(6), after outposts/villages at SURFACE_STRUCTURES(4); native decoration source/bytecode confirms ordering. Existing generation height/deep-layer limits remain. No old chunks or later player placements are rewritten; stairs/walls are not added or claimed fixed.
- #68 partial: original `WorldgenSwamp` permits overriding existing GT water only inside swamp columns. Retain original chunk-wide vanilla-water eligibility and ocean/river admission; correct only the misplaced swamp-column condition. No fluid movement, assimilation, `isSame`, registry identity or temperature change. This is not a claim that the whole open water-terrain issue is resolved.

## Validation

Corrected583 baseline: Forge294/296 and Neo285/287 pass, same two semantic failures. Prior573 and all three new recipe-overlay controls pass. Fixed583: Forge296/296,Neo287/287, zero failures/errors/skips. Full builds/core,142 Python checks,491REMI groups,1033 shared packaged classes per loader and SDK112core/46native pass. Input/final snapshots match. Receipt: `work/validation/stone-water-fixed-retry/local-validation-receipt.md`.

Worldgen tests invoke real features against registered native blocks in bounded WorldGenLevel proxies, including actual chosen full-block material controls, half/waterlogged/double variants, unaffected GT slabs and four paired-biome water scenarios. They do not establish arbitrary natural-seed full-world coverage. Native pipeline/source ordering separately establishes applicability to the reported structure stage.

Overlay controls warm actual Sifting lookups and test supported startup-addon identity preservation, parsed datapack override/repetition/restoration, exact unlink, independent rows and dynamic providers. No supported late-grade mutation defect was demonstrated; no speculative ownership-refresh production change was promoted.

## Preserved fixture history

The first slab test addressed the GameTest controller at local zero; corrected to a dedicated3³ template center, retaining loaded/no-BE checks and state restoration. An isolated overlay test wrongly required a fluid input; corrected only the map's item-only admission tuple, with ownership/cache assertions intact.

First fixed run included the correct Spike drop plus natural gravel items entering a roofless long-running fixture. Saved terrain confirms overhead gravel, not exact individual trajectories. The test now seals only its owned upper volume y12..23, never touches the controller/lower terrain, clears ambient natural entities only before setup and retains unfiltered exact-one/state assertions. The discarded full-template draft was never applied. Failed and interrupted evidence remains separate from final pass.

Original references and licensing are unchanged; no user world was touched. Existing-terrain repair and third-party client rendering remain outside this result.
