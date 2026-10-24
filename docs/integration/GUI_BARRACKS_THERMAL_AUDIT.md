# Fluid slot amounts, barracks doors and bounded thermal recycling

## Changes

- Fluid slot overlay follows original `FL.display(tank,T,T)` stack-count semantics: `max(1, min(127, amount/1000))`; ordinary count1 is not drawn. Thus1–1999L has no quantity overlay,8000–8999L shows8, and127000L or more shows127, without k/M/B. Shared machine screen uses native item-overlay right edge x+17 minus text width, y+9, white shadowed text. Exact FluidStack quantity, tooltip and texture selection are unchanged.312 core assertions and16 source predicates cover boundaries, coordinates and pose restoration; two native SlotFluid cases preserve exact data. No GPU screenshot proof is claimed.
- Four-bed barracks doors had reversed translation from original metadata. Swap facing for four doors' upper/lower halves without moving coordinates or changing hinges; native closed collision now overlaps adjoining half-slab walls instead of leaving a5/16 gap. Actual generation, all halves, neighbor updates, opening/closing and repeated generation pass both loaders. Only new generation changes; existing structures are not automatically repaired.
- Bounded source Cinnabar thermal recycling targets Mercury, not guessed molten Cinnabar. Original `RecipeMapHandlerPrefix`/`Loader_Recipes_Handlers` exclude raw/dirty prefixes but retain eligible clean/refined/dust forms. Added source floors and work formulas produce8 accepted rows per map (Melter/Smelter),16 total: dust48mB,small12,tiny5,purified60,refined66,purifiedTiny6,refinedTiny7,blockDust432; block work34ticks, others16, all16EU/t. Sub-mB forms do not create rows. Existing unrelated/explicit/addon/UNKNOWN recipes and item IDs remain unchanged.
- FluidCatalog records identity provenance only for entries actually created by its material factory. Mercury ownership requires that exact object and canonical core identity, not a matching field string. Three fresh-JVM core scenarios prove positive factory creation and both public external-preemption paths; external entries remain UNKNOWN. Existing300K fluid metadata is not changed.

## Baseline and fixed evidence

Unchanged-production557 baseline: Forge277/283 and Neo268/274 pass; six failures each (door gap plus five positive thermal cases); previous535 pass. Rawcraft's three native-manager tests already pass. Actual baseline thermal census had zero matching accepted rows because missing GenMolten_Cinnabar caused legacy emission to skip; do not describe hypothetical legacy code as an observed molten output.

Fixed native561 passes: Forge285/285,Neo276/276, zero failure/error/skipped results. The two GUI SlotFluid tests are new fixed-phase controls, not unchanged baseline cases. Dual build/core, GUI active/generated source checks,491REMI groups,1033 shared packaged classes per loader and SDK112core/46native pass.

An initial Python guard rejected a namespace string-prefix order. After both native runs, only YAML namespace ordering changed, with all11 identifiers unchanged. Revalidation passes142 Python checks and source verification; native production/test inputs remain identical. Failed guard evidence retained. Final receipt: `work/validation/gui-barracks-thermal-fixed/local-validation-receipt.md`.

## Raw gem crafting finding and limitations

Original positional crafting is present for oreRaw/Cinnabar, not every ore block/rawOreChunk: one occupied cell at2×2 slots0,2 or3×3 slots0,2,4,6,8 yields one gem. Other positions and extra occupied cells reject; a stack of64 in one cell still outputs one; GT autocrafting is disabled. Both actual native recipe managers pass these source conditions. No route was blindly deleted based on missing recipe-viewer display.

This remains a bounded Cinnabar/source-profile repair, not all materials' thermal recycling or arbitrary late external-form reload parity. Client graphics, old-world door migration and full survival gameplay remain outside the automated result. Source references were read-only and upstream licensing retained.
