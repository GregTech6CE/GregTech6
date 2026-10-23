# Aluminium workability / welding audit

Status: local dual-platform fixed gate passed, 407/407 native cases. Remote CI is a separate gate.

## Correction and root cause

The user correctly reported original Aluminium double ingot welding takes **32 ticks**. The initial investigation incorrectly read absence of an explicit FURNACE token in the Al declaration as absence of the effective flag, and prematurely claimed original384 ticks. That declaration-only claim is retracted, not an alternative oracle. Earlier incorrect notes remain archived as retracted evidence.

Original MT.java569 calls aluminium();401 calls posttrans with factory melting argument933;78 forwards to elemental metal;62 conditionally inserts nested EXTRUDER_SIMPLE/FURNACE/MORTAR when the factory argument is<=1200. OreDictMaterial.put(Object...) recursively inserts the array flags. Original-w equivalent lines576/408/84/68 agree. qual() does not remove them. Original recipe handlers use effective FURNACE, not source declaration text. Legacy extractor misses this propagation and also uses field-name keys for canonical aliases (ALIGN-010).

Original Loader_Recipes_Handlers334–348 supplies easy ingot/pipe timings;360 fixes easy rotor at16*4=64. The recipe handlers' explicit overload arguments, not stale comments or mass-derived assumptions, are the oracle. No original game runtime was launched; original claims are source-traced.

## Scope of production changes

- Additive reviewed, source-hash-pinned profile:61 FURNACE,83 MORTAR,1 SOFT positive corrections;93 proven EXTRUDER_SIMPLE identities;121 union identities. Legacy sets remain unchanged.
- Effective factory argument, not final material temperature, controls elemental threshold. Wax factory is unconditional; WaxRefractory .heat(2600) does not erase its flags. Canonical ID joins include GoldInductive versus source field InductiveAlloy.
- New positive overlay resolves identity, rejects AddonMaterials.owns, then matches builtin ID plus canonical normalized name. Existing fallback behavior stays separate. Public addon registration allocates addon IDs; it cannot currently manufacture exact builtin ownership collisions. Ownership guard is defense-in-depth; no private registry-state surgery is used as a public compatibility claim.
- Easy rotor uses original explicit64, not generic4.25 material units yielding68. Hard-material times are preserved (Titanium double ingot source control512).
- Capsule simple extrusion recognizes proven positives, while retaining existing choices and using registered forms only. Baseline12 capsule controls already pass; this is compatibility coverage, not evidence of a prior capsule failure.
- No selector production change: original ST.tag count0 is retained; modern selector is a RecipeCatalystLike, and consumption already retains it. Native tests verify actual transaction retention.
- No display production change: EMI reports base recipe time, machine throughput/overclock determines active work ticks. Compound baseline assertion does not establish a units defect.

This is **not** a complete effective-material evaluator. Unresolved identities remain UNKNOWN, never prohibited. No global negative ore eligibility follows from missing profile entries. Separate ignored stone ledger identifies74 additional direct factory positives; intentionally excluded from this bounded fix.

## Independent original route expectations and runtime reachability

All rows below use16 recipe power (Welder LU/t). These are base recipe ticks, not wall-clock completion time.

|Al route|Original ticks|Number of routes|
|---|---|---:|
|ingot2/3/4/5/9 -> double/triple/quadruple/quintuple/block|32/48/64/80/144|5|
|bolt4 -> stick; bolt8 -> long stick; stick2 -> long stick|8/16/16|3|
|curved plate4 + ring -> rotor|64|1|
|tiny/small/medium/large/huge pipe|16/16/48/96/192|5|
|basic/double/quadruple/dense casing, each short-rod and long-rod route|128/224/416/896|8|

Total22. Pipe shapes are independently pinned: curved plates1/1/3/6/12, selectors1/2/3/4/5, output counts2/1/1/1/1. Native lookup tests inspect the accepted recipe, output identity/count, power/duration, material consumption and selector retention. They do not simply test intended producer formula or use producer entries as an oracle.

Baseline22-route method stops at missing Al FURNACE assertion; it does **not** prove actual timings or consumption of all22 rows. Fixed success is required before claiming them runtime-tested. Likewise inherited-profile method stops at Al flag, so later alias/Wax/Titanium assertions remain baseline-unreached. Work/display case fails duration==32 AND unit==ticks; failure alone does not isolate duration versus units or exercise later work arithmetic. Source predicts double-ingot32, T1work512/T2work1024, with nominal32 energy16 active ticks; native test covers arithmetic, not scheduling/wall-clock delivery.

## Exact unchanged-production baseline

Base f079e19b6c786de6f24d47454965481363ac46b9. Terminal evidence: work/validation/material-workability-baseline-template-retry.

|Platform|Tests|Pass|Fail|
|---|---:|---:|---:|
|Forge|201|196|5|
|NeoForge|192|187|5|

Same5 failures on each: missing FURNACE in22-route test; missing FURNACE in inherited-profile test; actual Copper rotor16/68 versus expected16/64; compound Al display duration/unit assertion; separate CO2 gas-phase vent assertion. All previous381 cases plus2 new capsule controls pass. Strict checkers reject both failed reports. No skipped/missing/error cases.

CO2 is separate bounded engine work: baseline verifies real registered gas with density756 and lighterThanAir=false, pane prerequisite passes, open-air vent fails; later flower assertion is unreached. Do not attribute this failure to workability.

## Infrastructure and fixture history

1. First baseline attempt interrupted by environment restart during spawn preparation; no terminal exit/XML, not gameplay evidence.
2. Fresh infrastructure retry compiled but Forge collection failed because template="minecraft:empty" was prefixed into invalid gregtech_issue_audit:minecraft:empty; no XML, Neo not started. This is a test-fixture error, not production recipe failure.
3. Authorized correction changes only5 annotations to existing template="test_empty". Final API-neutral native class hashcc65b597... is frozen unchanged between baseline/fixed. No production helper references introduced into baseline. Earliercbce class is superseded/archived.
4. After both terminal baselines, upstream main a2efcf208 integrated via local merge42f60e6d8. Production patch excludes native class and historical build.gradle hunk; separately rebased build task preserves addonEnergyTransferContracts and originalMaterialFluidUnitContracts. Full fixed gate targets407 cases following upstream additions; release is not a success result.

## Fixed validation

On the integrated upstream baseline (local merge42f60e6d8), both complete native suites pass: Forge208/208 and NeoForge199/199, with strict exact-case checks across all four selected namespaces. All22 Aluminium accepted recipe routes, selector retention, inherited identity controls and display/work arithmetic now reach and pass their assertions. CO2 pane/air/flower controls also pass.

Dual-platform build and core contracts passed;126 Python guards,486 REMI checks,1015 shared packaged classes per loader,112 core SDK classes and46 native SDK classes per loader passed. Input/final source snapshots match. Native durations: Forge11m9s, NeoForge7m6s. Evidence: work/validation/material-workability-fixed/ and its local-validation-receipt.md.

The local packaging helper initially matched newly added sources jars as binaries. It was corrected to the same binary-classifier exclusion used upstream; the failed helper log is retained. This was not a production jar validation failure. No source was changed during the gate. Hashes below identify tested sources and baseline evidence; they are not a claim that original GT6 was launched.

## SHA256 input/evidence inventory

- `tools/extract_gt6_workability.py`: `83d95d94804d018cebc07b9538d82f18c859373ce1011653f77afc8285a34ba6`
- `tools/workability_positive_profile.py`: `db8cc2b7da6b25274f9e905d8ddf8137d4fb85ef8a351a9d42e3e42cfc784e4d`
- `tools/data/gt6_workability_positive_profile.json`: `6482a211cd6649ca57917bf02e4a4269b70eae414b585c1a0c633fe3a81c5139`
- `core/src/main/java/com/gregtech/gregtech/data/generated/MaterialWorkability.java`: `b73bde70c624e61f11a1026a60b3074eb7bc82a3a6ef4538b8a3350cea555692`
- `multiversion/src/main/java/com/gregtech/gregtech/content/recipe/WelderFamilyRecipes.java`: `6842a153744f736943bb550f17d3302c05ab94b9b8a8dd82079b5c33f2304421`
- `multiversion/src/main/java/com/gregtech/gregtech/content/recipe/CapsuleCellRecipes.java`: `e8ba265f462feb46b3e7e796dad7d10793383908afca0b0b3386599dd12ac87a`
- `core/src/test/java/com/gregtech/gregtech/core/MaterialWorkabilityContracts.java`: `37b836e692d6b5a35de85e63d707ac328d4d94280f0a7da34aaa88d35cde6c9e`
- `core/build.gradle`: `b3547d66ee5bd3c6f594e67517e2427a7ee6e240c6177b1e77d4efd1ff27363f`
- `multiversion/src/bootstrapGameTest/java/com/gregtech/gregtech/gametest/AluminiumWeldingAuditTests.java`: `cc65b597a8d2e451ec41b4242b65be019c3511e5b671554bcc1f9cbf4d2ef450`
- `work/references/gregtech6-original/src/main/java/gregapi/data/MT.java`: `12936831b1b5f419dc783c5f32f3cc07a329734ca6c5c11757d52e1be27904b2`
- `work/references/gregtech6-w/src/main/java/gregapi/data/MT.java`: `9530c229de3483ab7be300c49ce1e08e44bcabfe9bdebd7844becefbd4782728`
- `work/validation/material-workability-baseline-template-retry/forge-baseline.xml`: `7720a47b21e66164ec3a7d952b791b6d1e77288df86e960d8a9e1471982b9982`
- `work/validation/material-workability-baseline-template-retry/neoforge-baseline.xml`: `2e0b8e103c69479483b3dcd5d21ede7110d2f502b91b65254e2bece80104d968`
