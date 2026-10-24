# Positive ore Centrifuge: Copper, Galena and Nether Quartz

2026-10-24. **Final role-corrected verification passed: Forge 313 + NeoForge 304 = 617 actual native cases, with no failures, errors or skips.** Both unchanged-production baselines reproduced the six new failures; the failed first correction and diagnostic history below are retained, not current failure status.

This bounded slice restores the original purified-ore Centrifuge rows for three already reviewed materials. It does not include the separately proposed material-quality/importer batch.

## Original contract: six exact rows

Every input below is one item. Every output is the named material's **`crushedCentrifugedTiny`** form, in the listed order. Ordered output chances are **100%, 10%, 10%, 10%** (`10000,1000,1000,1000`), with no fluid inputs or outputs and **16 EU/t** throughout.

| Material / core ID / quality | Input prefix | Ordered refined-tiny outputs | Duration |
| --- | --- | --- | ---: |
| Copper / 290 / 0 | `crushedPurified` | 9 Copper; 9 Cobaltite; 9 Gold; 9 Nickel | 144 ticks |
| Copper / 290 / 0 | `crushedPurifiedTiny` | 1 Copper; 1 Cobaltite; 1 Gold; 1 Nickel | 16 ticks |
| Galena / 9117 / 0 | `crushedPurified` | 9 Galena; 9 Sphalerite; 9 Silver; 9 Lead | 144 ticks |
| Galena / 9117 / 0 | `crushedPurifiedTiny` | 1 Galena; 1 Sphalerite; 1 Silver; 1 Lead | 16 ticks |
| Nether Quartz / 8346 / 3 | `crushedPurified` | 9 Nether Quartz; 9 Barite; 9 Efrine; 9 Void Quartz | 576 ticks |
| Nether Quartz / 8346 / 3 | `crushedPurifiedTiny` | 1 Nether Quartz; 1 Barite; 1 Efrine; 1 Void Quartz | 64 ticks |

Work is `144 × (quality + 1)` for whole input and `16 × (quality + 1)` for tiny input. The first three byproducts are distinct slots, not only the second byproduct. Whole refined ore and nine refined-tiny primary outputs have equal nominal material weight; the correction concerns forms, available routes, byproduct coverage/grade/probability and work, not a claim that primary mass differs.

Galena's smelting target and Nether Quartz's ore multiplier/smelting target are not consumers of this Centrifuge rule. None is changed or applied to its output amounts.

## Source evidence

Pinned references:

- GregTech6 original: `3703e40308c8c030763fd6297dea8b210d2a77b1`.
- GregTech6 W: `607771f74acb0fe647f7e9412676e72094799545`.

Original `src/main/java/gregtech/loaders/c/Loader_Recipes_Ores.java:383,464–470` supplies the chance array, first-three selection, ambient phase, output forms/counts and work. W counterparts are lines 387 and 468–474. The two Centrifuge statements match across these snapshots after indentation removal. Original/W file SHA256s are respectively `b94dc194c7557f341ebacd895cc18c2fada8c5e4cd33b45784c971f921db4aa3` and `78d0542179b63bd6206377da7e23a2e70543f8b8086afebb1e182e7ff3461423`.

Eligibility is independently established from effective original factory/group flags, **not** inferred from generated port items or `.ores(...)` byproduct declarations:

- Copper: `MT.java:418` copper factory and metal ancestry, `G_INGOT_ORES` / `G_INGOT_MACHINE_ORES`; quality 0 at line 586; byproducts at 3040.
- Galena: ore-dust factory ancestry and explicit `G_GEM_ORES` at 3717; default quality 0; byproducts at 3034.
- Nether Quartz: quartz factory's `G_QUARTZ_ORES` at 179; quality 3 at 1554; byproducts at 3205.
- `TD.java` defines the ORES-containing groups; `OP.java:135–140` carries their eligibility through purified/refined prefixes. `.ores(...)` only appends byproducts (`OreDictMaterial.java:1239`).

All nine byproduct melting points are above ambient 293 K: Cobaltite 1014, Gold 1337, Nickel 1728; Sphalerite 540, Silver 1234, Lead 600; Barite 1853, Efrine 2246, Void Quartz 1986. Original `OreDictMaterial.fluid` chooses phase by temperature (1299–1303). These are positive solid-phase facts, not assumptions that an absent fluid holder means solid. The facts adapter validates the matching port thermal values.

Detailed paired factory/quality/thermal provenance and file hashes remain in local `work/positive-ore-centrifuge/source-evidence.json` and `README.md`.

## Real unchanged-production baseline

Baseline HEAD `3584f3184eef7c9bda75bce10dc603e4b23b23f8`, plus only six shared native tests and strict checker tests. Input/final snapshots were byte-identical; no production change was included.

| Loader | Actual total | Prior cases passed | New cases failed | Native Gradle / checker exit |
| --- | ---: | ---: | ---: | --- |
| Forge 1.20.1 | 313 | 307 | 6 | 1 / 1 |
| NeoForge 1.21.1 | 304 | 298 | 6 | 1 / 1 |

All **605 pre-existing cases passed**. No new test failed a material identity, ownership, quality, byproduct-order or fixture prerequisite. Each loader reported the same defects:

| Material | Whole-input actual output | Tiny-input actual result |
| --- | --- | --- |
| Copper | 1 whole refined Copper + 1 tiny Gold dust | No accepted matching recipe |
| Galena | 1 whole refined Galena + 1 tiny Silver dust | No accepted matching recipe |
| Nether Quartz | 1 whole refined Nether Quartz + 1 tiny Efrine dust | No accepted matching recipe |

The whole tests stopped at the exact ordered-form/count assertion; the tiny tests stopped at missing-row detection. **Later chance/work assertions were not reached in these failing runs.** The legacy 50% second-byproduct / 120-tick mismatch is separately established from producer source, not claimed as an additional executed baseline assertion.

Local evidence: `work/validation/positive-ore-centrifuge-baseline/local-validation-receipt.md`, platform XML/log/exit/checker records and matching snapshots. XML SHA256: Forge `a5440f4d62af65fe63c67aae5765fe88e4eb0346d528e519df4ad695ab194389`; NeoForge `e2d03b548a2bbf55358b79210e54830342a4fdc465ae0d1d4dc6ddec1cfdb3af`. Elapsed native runs were 8m30s / 4m24s. Full core/package verification was deliberately not repeated for this tests-only baseline.

## First fixed attempt and native registration diagnosis

The first production patch (`94115c21f65ebbdb3cd9d2b0e49115bc11e7461c827f1bc270f9b17fd5bb3828`) compiled but did **not** restore the runtime rows:

- Dual build/core passed in 2m24s; 157 Python guards, 491 REMI groups, package checks (1,039 shared classes per loader) and SDK checks (118 core + 46 native per loader) passed.
- Forge again reported **313 cases: 307 passed, the same six failed**, Gradle/checker exit 1, in 8m21s. Tiny rows remained absent; whole rows retained the legacy outputs. Later chance/work checks again were not reached. No old case failed.
- **NeoForge was not run** after this Forge failure. This attempt is not a 617-case pass. Tracked diff and native test/helper hashes matched before/after; this audit document was added during the gate, so whole-tree byte identity is not claimed.

Evidence: `work/validation/positive-ore-centrifuge-fixed/local-validation-receipt.md`.

A separate, decision-preserving native startup diagnostic found the **first rejection** inside `OreProcessingRecipeLoader.processWashing`, called by loader/bootstrap registration:

| Reviewed profile | Actual unrelated external item causing UNKNOWN | Bound prefix/material |
| --- | --- | --- |
| Copper | `addon_sample:sample_blank_copper` | `sampleBlank` / Copper (290) |
| Galena | `addon_sample:sample_blank_lead` | `sampleBlank` / Lead (820) |
| Nether Quartz | `addon_sample:sample_blank_efrine` | `sampleBlank` / Efrine (8747) |

Those were genuine registered addon items, not failed core-ID/quality/phase prerequisites. The initial guard treated **any external form of any involved material** as ownership of these ore recipes. That was too broad: custom `sampleBlank` is neither a Centrifuge input nor an output here, so the guard returned UNKNOWN and left the legacy producer active.

The diagnostic selected only seven existing `gregtech_ore_thermal_audit` controls. Its terminal owner receipt confirms **7/7 passed**, no failures/errors/skips, Gradle exit 0 in 6m39s, matching source snapshots. This confirms the logging run finished normally, **not** that any of the six positive-ore rows was fixed. No NeoForge diagnostic or full 617-case run occurred there. Evidence: `work/positive-ore-centrifuge/role-fix-native-diagnosis.json` and `work/validation/positive-ore-profile-diagnostic/local-validation-receipt.md`; diagnostic XML SHA256 `148034fc0a96347ed760bc9e4970da0599d4e9563c04abf300c8087c59430a6f`.

## Bounded correction and verification boundary

[ReviewedOreCentrifugeFacts](../../multiversion/src/main/java/com/gregtech/gregtech/content/recipe/ReviewedOreCentrifugeFacts.java) admits only these three builtin profiles. It requires exact core ID/name, no addon ownership, original quality, first-three byproduct identities/order/thermal values and native registered forms with exact material/prefix identity. The corrected ownership guard is **material-and-prefix-role specific**:

- Source material: external `crushedPurified`, `crushedPurifiedTiny` or `crushedCentrifugedTiny` forms cause UNKNOWN.
- Each of the first three byproducts: external `crushedCentrifugedTiny` forms cause UNKNOWN.
- Unrelated forms such as `sampleBlank` do not own these inputs/outputs and must coexist with the builtin ore rows.

Both accepted addon-form declarations and bound external items are checked against those roles. Truly relevant external forms still defer; the fix does not disable ownership protection or replace foreign recipes.

[OreProcessingRecipeLoader](../../multiversion/src/main/java/com/gregtech/gregtech/content/recipe/OreProcessingRecipeLoader.java) delegates only this admitted Centrifuge producer to the existing [SourceOreFollowonRows](../../multiversion/src/main/java/com/gregtech/gregtech/content/recipe/SourceOreFollowonRows.java) algorithm. Collision-respecting `addRecipe1(false, ...)` does not replace a foreign row. Once admitted, the caller skips its own legacy producer even if insertion collides, preventing a legacy fallback from being re-added. No signature-based deletion or recipe-map sweeping is introduced.

Bath, Shredder, Sifter, thermal recycling, material flags/qualities, registry IDs, prefixes and unrelated targets are unchanged. The original overbroad rejection is now established by actual native diagnostics. Correct relevant-form UNKNOWN/collision behavior remains **source-reviewed only**: the six cases do not dynamically establish true competing ore-form ownership, late tag changes or ownership replay.

[PositiveOreCentrifugeTests](../../multiversion/src/bootstrapGameTest/java/com/gregtech/gregtech/gametest/PositiveOreCentrifugeTests.java) uses literal expected rows against real startup maps: exactly one accepted input recipe, actual runtime lookup identity, ordered output forms/counts, slot-aligned chances, fluids and work/EUt. Tests never insert the expected recipe. The strengthened six cases additionally require the actual registered/bound `addon_sample:sample_blank_copper`, `sample_blank_lead` or `sample_blank_efrine` witness, with exact `sampleBlank` prefix and material identity; an absent addon fixture cannot conceal the overbroad-guard regression. All six use `gregtech_ore_source_audit:test_empty`; no extra template or namespace selection is needed.

The public evidence checker adds a separate strict six-ID group, preserving the original 27-case ore-source group and all old identities. Its candidate Python suite passed **28 tests**, including omission/failure/error/skip and namespace-spoof controls on both platforms. Synthetic full reports expect **313 + 304 = 617**; this is not a native pass claim.

Role-specific correction against the clean first-patch base: SHA256 `4a5b4235de1dd05c0c2edc72e0d9d7037c0934fb99be09f59505b0f34ba5e621`; strengthened native tests: `8c02c1bc04a2258b5d058e3ea403828966a0d01479594c92818477e6fec3e1e3`. Both loader cached compilations passed. **Final role-corrected dual-loader verification passed**, independently of the earlier diagnostic and cached compilation evidence; see the terminal result below. The separate next quality/importer proposal remains outside this report and must not be mixed into the 617 snapshot.

## Final verification

Actual full gate: Forge 313/313 in 7m53s and NeoForge 304/304 in 4m32s, native command and strict checker exit 0 on both. All previous 605 cases remain passing. All six new source rows pass with actual unrelated sampleBlank coexistence assertions; none of the expected output/chance/work assertions was relaxed. Dual-platform builds/core, 157 Python guards, 491 REMI groups and package checks passed (1039 shared classes per loader, SDK 118 core + 46 native).

Evidence: `work/validation/positive-ore-centrifuge-fixed-retry/local-validation-receipt.md`. Forge XML SHA256 `eae55fb57f8fb14dcf6ab0675e00ea3b44cb3cf829ab29e370beb18fd274e112`; NeoForge `62c21e64c374425193852d782edef69d68778c02ce476458e477eb83740736d6`. Runtime/test/resource hashes and tracked code diff remained unchanged throughout the gate. Only this audit history document was updated during/after verification. This is registered-recipe/native lookup verification, not a full survival progression or client-rendering test. No material-quality/host-physics changes or new eight-material roster are included in this batch.
