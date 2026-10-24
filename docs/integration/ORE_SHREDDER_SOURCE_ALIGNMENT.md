# Six-form ore Shredder source alignment

2026-10-24. **The pre-integration full795 fixed gate passed: Forge402 + NeoForge393, zero failures/errors/skips.** Both targeted baselines reproduced66 new output failures per loader. Integration with subsequent main PR #101 requires a separate831-case gate and remains pending.

## Eleven materials, six rows each

The bounded roster is Copper290, Galena9117, NetherQuartz8346, Chalcopyrite9111, Pentlandite9145, Sphalerite9130, Tetrahedrite9132, Pyrite9125, Nickel280, Cooperite9116 and YellowLimonite9137. All have positively reviewed original ORES/MORTAR eligibility and self/U pulver targets. Nine have quality0, Nickel quality2 and NetherQuartz quality3.

Every input is one item. Outputs below are **separate count1 slots**, all of the same pulver material and all guaranteed (`10000` chance). Repeated `dustDiv72` slots remain separate. Every row uses16 EU/t and no fluids.

| Input prefix | Ordered output prefixes | Input/output amount | q0 ticks | Nickel q2 | NetherQuartz q3 |
| --- | --- | ---: | ---: | ---: | ---: |
| `crushed` | `dust`, `dustTiny`, `dustDiv72` | 9U/8 | 18 | 54 | 72 |
| `crushedPurified` | `dust`, `dustSmall` | 10U/8 | 20 | 60 | 80 |
| `crushedCentrifuged` | `dust`, `dustSmall`, `dustTiny`, `dustDiv72` | 11U/8 | 22 | 66 | 88 |
| `crushedTiny` | `dustTiny`, `dustDiv72` | 9U/72 | 2 | 6 | 8 |
| `crushedPurifiedTiny` | `dustTiny`, `dustDiv72`, `dustDiv72` | 10U/72 | 3 | 7 | 9 |
| `crushedCentrifugedTiny` | `dustTiny`, `dustDiv72`, `dustDiv72`, `dustDiv72` | 11U/72 | 3 | 8 | 10 |

Work is `max(1, ceil(max(inputUnits, outputUnits) / U × 16 × (quality + 1)))`. Tiny durations must not be floored. This is not uniformly faster: some quality3 whole routes require more work than the former64/32-tick producer.

The former Shredder producer yielded one dust for crushed/purified whole, eleven dustTiny for refined whole, or one dustTiny for each tiny input. Relative to the source fractions, those omit1/8U,1/4U,11/72U,1/72U,2/72U and3/72U respectively. This describes nominal recipe-output material, **not a measured full-machine inventory loss**.

## Source and admission boundaries

Pinned references: original `3703e40308c8c030763fd6297dea8b210d2a77b1`, W `607771f74acb0fe647f7e9412676e72094799545`. Original `Loader_Recipes_Handlers.java:145–150` and W150–155 supply the six MORTAR rows. Original `OP.java:135–140,154–157` supplies their prefix weights; `RecipeMapHandlerPrefix.java:107–131,204–226` and `UT.Code.units:1677–1682` establish explicit slots and rounded-up work. The paired census is local `work/ore-shredder-audit/source-census.json`, SHA-256 `19720c967c3c8fa15deb8bb83021ef44ec03b424b9cdea167cd2b522c3dd248b`.

`RecipeMapHandlerPrefixShredding` selects `mTargetPulver.mMaterial`; it **does not scale explicit output counts by pulver target amount**. Self/U is a conservative admission restriction for this roster, not ratio-scaling semantics. Smelting targets, generifying targets and Centrifuge byproducts do not redirect these rows. W has additional alias matching/dedup behavior elsewhere: matching row/work rules do not establish whole-handler lifecycle equivalence.

`ReviewedOreShredderFacts` requires exact core ID/name, expected quality, no addon material ownership or antimatter, self/U target and all ten actual native forms with exact prefix/material binding. Accepted external declarations or bound foreign items in the six input/four output roles cause `UNKNOWN`; unrelated custom `sampleBlank`/plate forms do not own these routes. Unknown or changed facts retain existing behavior. Relevant external ownership/collision branches remain source-reviewed rather than dynamically proved by these positive cases.

Production patch `e7b5b526b39882f90bebeaa9ad42c6263643b914da55d21d3369e5e79126a2d3` adds the bounded adapter and changes three loader sites. It preserves the adjacent **Crusher** builder calls, retires only the admitted legacy Shredder producer, and delegates to the unchanged shared source-row helper. Collision-respecting insertion does not replace foreign recipes or re-add the retired legacy row after collision. No recipe sweep, material/prefix metadata change or item-ID change is included.

Dust-grade **input** handlers, generic recycling, manual Mortar, non-MORTAR families and non-self targets are outside this slice.

## Actual targeted baseline

Production remained unchanged at `94b628253`; only tests/checker additions were present, and input/final snapshots matched. Only `gregtech_ore_source_audit` was selected:

| Loader | Exact selected | Prior passed | New semantic failures | Exit / duration |
| --- | ---: | ---: | ---: | --- |
| Forge | 128 | 62 | 66 | 1 / 4m16s |
| NeoForge | 128 | 62 | 66 | 1 / 2m41s |

Every new failure reached the exact ordered Shredder output forms/counts assertion. All preceding source identity/quality/self-target/U, native form, actual addon witness and **adjacent Crusher accepted-row/lookup controls passed**. There were no fixture failures. Later Shredder chance/work assertions were not reached and are not separately claimed demonstrated by this baseline. Exact discovery found no missing/extra/duplicate identities; it is not gameplay success. **This was not a full795 baseline.**

Evidence: `work/validation/ore-shredder-baseline/local-validation-receipt.md` and loader XML/discovery reports. XML SHA-256: Forge `21f679c92ef737cb4bec7961979b351372500ebad2af557e57bfb2099a10eca5`; NeoForge `2bde3b8cfc0766fd0e225fe1117629e9ddf7d0f176957b474ab70083eb6f1252`.

## Tests, checker and remaining gate

`OreShredderSourceTests` contains66 literal methods under `gregtech_ore_source_audit:test_empty`. They inspect real accepted rows and runtime lookup, ordered forms/counts/chances, fluid arrays and work; they neither insert desired recipes nor invoke the producer. Copper/Nickel require their actual unrelated same-material sampleBlank witness; other owners use Copper only as an enabled-addon control. These are recipe-map/source-input tests, **not full machine cycles**.

The strict66-case checker group retains all663 previous identities and template bindings, including owned builder fixtures. Missing/failed/errored/skipped/wrong-template or spoofed qualified names reject the new cases; selection is not reduced. The optional indexing change only preselects candidate identities by final method-name segment and then executes the **unchanged matching predicates**, including historical legacy suffix behavior. Its rebased proof compared154 real XML reports,8155 singleton/seeded variants and10 full synthetic controls; a permanent collision/legacy-suffix regression checks that both identities sharing a method remain necessary. The36-test candidate suite passed; synthetic reports are not native795 evidence.

The pre-integration full795 gate passed with dual/core builds,184 Python guards,491 REMI groups,1045 shared packaged classes per loader and SDK118 core +46 native; actual physics JAR/refmap guards passed. Use the repository Gradle wrapper and the full namespace/toolchain configuration in `.github/workflows/issue-regressions.yml`, then the public `tools/integration/check_issue_gametest_report.py` for each platform. The full checker deliberately rejects a128-case targeted report. No integrated831 pass, remote-CI recovery or merge is claimed here.

## Pre-integration receipt

Evidence: `work/validation/ore-shredder-fixed/local-validation-receipt.md`. Forge402 in10m52s, XML SHA256 `63340457b8e9a1f149957f8388d3b94ecf0b8e2510eb9472f32dfd4667948c82`; Neo393 in4m49s, XML `63688cdca812f8caeba30f450b7f168237f59af0c6e3e2caf65bfc54bc0f92b8`. Native/strict checks exited0. Runtime/test inputs remained fixed; the audit document was the only added non-runtime file. New upstream `4dc02e6ca` structure tracking is not certified by this result.
