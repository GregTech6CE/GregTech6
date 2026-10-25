# Cinnabar Bath and barrel resume corrections — 2026-10-25

## Scope and verified status

This batch corrects one automatic Cinnabar washing recipe and one sealed-barrel persistence defect. It does not claim complete mineral-processing or machine parity.

Against the pre-fix production baseline, **each loader ran 134 targeted native cases: four expected semantic failures and 130 passes**. The four failures were two Cinnabar water-shortcut checks and two barrel resume checks; the other two new barrel controls already passed. With the fixes, **134/134 passed on Forge and 134/134 on NeoForge**: six new cases plus 128 existing source-audit cases per loader. No assertion weakening or test-only production helper was needed.

**Final local validation passed on unchanged runtime/test sources: 971/971 native cases**, with all 490 Forge and 481 NeoForge required identities executed successfully. All 959 prior identities are preserved. Both builds, core/source checks, 314 prescribed CI Python tests, 491 REMI checks, sediment registration, packaging and SDK closure passed. The targeted differential runs above are separate evidence. Hosted CI for this commit is pending at publication.

Evidence: `work/validation/cinnabar-barrel-gate/{baseline-controller.log,fixed-controller.log,admission-preservation.json,frozen-9-path-manifest.json}`; each loader's `build/reports/gametest/cinnabar-barrel-{baseline,fixed}.xml`. The four semantic failures were checked against exact assertion messages, not accepted merely because the baseline process failed.

## Cinnabar: remove only the incorrect automatic producer

Original reference commit `3703e40308c8c030763fd6297dea8b210d2a77b1`, `gregtech/loaders/c/Loader_Recipes_Ores.java:184–187`, registers three explicit Cinnabar Cryotheum Bath routes: dust block→exquisite gem, crushed ore→flawless gem, nine tiny crushed ore→flawless gem. Its Sluice routes and chance vector are at 379–420. These are not a generic water-only Bath shortcut.

`OreProcessingRecipeLoader.processWashing` now skips its own legacy water-Bath addition only for the existing conservative `ReviewedCinnabarFacts` profile. It does not delete recipes by signature or search/remove third-party registrations. External form/profile metadata continues to defer through that existing eligibility guard; independent startup and public datapack additions remain legitimate. The test fixture explicitly requires the reviewed builtin profile, so absence is not asserted for every addon configuration.

The two new native cases verify absence of the automatic water shortcut, all three exact Cryotheum Bath rows, and four water Sluice routes retaining nine output slots/eight 300-chance byproducts. A parsed public datapack addition supplies an explicit water Bath override; two apply/remove cycles with warmed native lookups verify the override wins and removal restores baseline absence. Unrelated source row object identities remain unchanged. The actual server RecipeManager is restored in finally.

This exercises the supported `MachineRecipeOverlay.apply` entry point, **not a disk/tag/event resource reload or cold addon-startup winner fixture**.

## Sealed barrel: preserve work while rebuilding transient recipe cache

Original `gregapi/tileentity/tank/TileEntityBase08Barrel.java:60–83,188–207` persists sealed work and resolves its transient recipe after load without discarding valid work. Maximum duration is derived again; invalid or gaseous recipes reset work.

The port restored saved work, then unconditionally zeroed it on the next cache-resolution tick. `TankBlockEntity.tickFermentation` now retains work for a valid resolved recipe, recomputes the maximum, and resets invalid lookup state. Existing changed-fluid, explicit unseal/reseal, empty-content, and normal recipe-transition behavior remains intact.

Four native cases cover:
- Actual 50 mB apple-juice fill and 128 native fermentation calls; binary NBT serialization/deserialization; replacement by a genuinely new registered block entity; next native tick 129/1024; continuation to 25 mB cider and a fresh subsequent recipe.
- The same completion path with the derived maximum absent from saved NBT.
- Reloaded invalid water, gaseous-output biomass, gaseous nitrogen and empty-content reset controls.
- Live cached-fluid change, unseal, binary save/new-entity reload while unsealed, and explicit reseal.

Fixtures reject pre-existing block entities, snapshot/restore their bounded cells, and remove only newly spawned item entities. They call the actual native tick entry point synchronously. They do **not** claim a cold JVM restart, actual chunk unload/reload, item harvesting, or server scheduling coverage.

Original and current saves have no persisted recipe identity. A changed matching recipe or edited valid fluid/work NBT therefore uses the newly resolved recipe while retaining work, as original does; no new signature or migration policy was invented. The existing port increments on cache resolution while original spends that tick resolving only. This bounded repair preserves port cadence and does not claim exact global timing parity. Live recipe-map invalidation remains outside this fix.
