# Sluice source-form output alignment — 2026-10-25

Final local validation passed on the compatibility-corrected source: 987 required normal native cases, two separate fresh-JVM startup cases, and ten existing lifecycle regression cases. Hosted CI for this commit is pending at publication.

## Scope

This is a bounded **Sluice fine-output** correction, not a complete ore-processing rewrite. Only captured built-in Sluice producers whose fine outputs reach the reviewed source materials are rebound. Inputs, main outputs, water/slurry identities and amounts, ordered chances, EU/t and duration are preserved. Shipped item IDs are not removed.

Original GT6 disables automatic generation of six forms for ten exact materials:

- Forms: `crushed`, `crushedTiny`, `crushedPurified`, `crushedPurifiedTiny`, `crushedCentrifuged`, `crushedCentrifugedTiny`.
- Materials: Aluminium (130), Silicon (140), Titanium (220), Iron (260), Niobium (410), Tantalum (730), Tungsten (740), Adamantium (2220), Dilithium (8317), Fluorine (90).

A generation exclusion is not blanket logical form invalidity. Supported, natively usable explicit external/addon forms remain authoritative; unsupported tag-only configurations preserve the existing canonical route. The new rule is recipe-local and enabled only during the captured Sluice factory admission. Fine selection follows `purifiedTiny → refinedTiny → dustTiny`, retaining material/count.

**Not fixed by this slice:** eligibility of unsupported crushed-input routes, their remaining machine recipes, or the existing 53-material continuation policy. Raw/rock/dust routes are unchanged. Fluorine remains outside this capture where the earlier nonore producer policy owns the route. Primary outputs are deliberately unchanged.

## Actual native cohort

The final corrected full native runs on both loaders measured the same 80 accepted Sluice rows: ten parent identities × whole/tiny × Water/MnWtr/DistW/SpDew. No global fixed count was invented; the test derives the registered cohort and compares exact accepted identities.

| Parent | ID |
|---|---:|
| Aluminium |130|
| Silicon |140|
| Titanium |220|
| Iron |260|
| Niobium |410|
| Tantalum |730|
| Tungsten |740|
| Adamantium |2220|
| Dilithium |8317|
| Dolamide |8753|

Sorted row-key SHA-256 (UTF-8, one key per line, final LF):
`1140c177cc5f95c53b2fcaa6e55a229def8e347fd204d6df99383e425d65d5c3`.

The observed MagneticSeparator cohort was **zero**, so the new Magnetic capture/helper was removed before final validation. Its original direct registration block is restored exactly; full fingerprint controls still check that map remains unchanged. No changed Magnetic-output behavior is claimed as tested.

Observed interior-empty fine slots: **zero**. No manufactured missing-fine fixture is claimed. Factory logic retains interior empty positions, but this run did not exercise one; native admission trims trailing empties, consistent with original output normalization. Native chance arrays align to retained output positions; original inert trailing chance entries are not a produced-output difference.

## Lifecycle and compatibility

- Sluice factories capture copied native templates but admit rows only after the normal overlay has rebuilt bound external tags. No early alias-cache initialization is introduced.
- A foreign fine may be selected only when actual `RecipeInputs.matches(canonical, candidate)` accepts it. A unique usable explicit/native mapping wins; unusable tag aliases do not overrule it. Multiple usable candidates, or configured aliases with no usable candidate, defer conservatively to canonical (UNKNOWN). With no external configuration the source dust fallback applies. This does not add missing global tiny/purified/refined tag bindings or claim complete tag-only compatibility.
- Exact accepted recipe objects, not map-wide signature searches, establish ownership. Independently admitted collision winners are never claimed or removed. Baseline capture excludes owned rows; clear/rebuild invalidates warmed lookup selections and admits fresh owned objects.
- Repeated overlay tests compare complete row fingerprint multisets, including serialized item/fluid metadata, chances, flags, work, catalysts and viewer alternatives. Raw Cinnabar controls and unchanged Magnetic rows are retained.
- All in-process tag/metadata/fixture mutations restore synchronously in `finally`. A public stop-handler invocation checks same-JVM cleanup only: **not an actual server stop, restart, or second-world proof**.

## Validation status

| Phase | Forge | NeoForge | Meaning |
|---|---|---|---|
| Unchanged baseline, normal eight cases |8 semantic RED|8 semantic RED|Incorrect Dolamide Dilithium fine observed; no missing-helper/compiler failure counted|
| Unchanged baseline, isolated startup |1 semantic RED|1 semantic RED|Real public `onRecipesReady` callback reported `accepted=false`|
| Pre-narrowing targeted fixed normal cases |8 PASS|8 PASS|Intermediate output-only evidence; predates downstream usability correction|
| Narrowed pre-compatibility tree, real isolated startup runner |1 PASS|1 PASS|Intermediate fresh-JVM proof; predates the final candidate-usability guard|
| Narrowed pre-compatibility full normal suite |498 PASS|489 PASS|Intermediate 987 PASS; output-only tests did not establish downstream usability|
| Revised tests against pre-guard resolver |3 semantic RED + 5 control PASS|Not repeated|Three renamed compatibility cases exposed the defect; unrelated setup/helper failures not counted|
| Final compatibility-corrected isolated startup runner |1 PASS|1 PASS|Fresh JVM, exact nonce/receipt, accepted form and two overlay rounds verified|
| Final compatibility-corrected tree, full required normal suite |498 PASS|489 PASS|987 combined; all exact required identities passed, including revised downstream lookup/consumption|
| Existing isolated inputs lifecycle group |5 PASS|5 PASS|Cohort, two real reload phases/restoration, and actual stopped-event observer|

All eight normal cases begin with the same independent literal Dolamide source witness. Their baseline failures are a shared demonstrated fine-form defect, not eight independent bug claims. The dynamic cohort is additional native evidence; the literal Dolamide test independently checks all eight size/water rows and all nine ordered outputs.

The isolated `ore_form_startup` profile is different from the runtime collision test: the sample addon uses the supported startup `MaterialFormRequest(Dilithium, crushedPurifiedTiny)` and actual public `onRecipesReady` registration. It runs under an exact namespace and fresh disposable directory/nonce, asserts independent whole-Dolamide winner identity and tiny-Dolamide explicit form continuation, and verifies two ordinary overlay rounds. It does not mutate accepted-form state at runtime and does not claim actual resource reload or shutdown.

The final startup receipts verified `acceptedCoreForm`, `publicStartupWinner`, `winnerIdentityPreserved`, `rawControlPreserved`, `restored`, `probePassed` and `verifiedRounds=2`. The separate profile does not inflate the 987-case normal requirement. Unsupported tag consumption is established by the final normal suite, not by this startup profile. The existing isolated inputs group additionally verified genuine resource reload and actual stopped-event dispatch for its preexisting rows and foreign metadata; it is not a direct observation of the new ledger at actual shutdown.

## Verified downstream usability on the final tree

The revised tests do not treat membership in `nativeAliases` as machine compatibility. A plain item bearing the actual purifiedTiny tag must first be shown **not** to match or consume through the canonical Dilithium input. The selected canonical UNKNOWN fallback must then resolve to the existing Crusher and Shredder rows and plan one-item consumption from each nine/one-count Sluice fine. Removing the unsupported configuration returns the default dust fallback.

For compatible foreign forms, the tests temporarily use already registered vanilla Redstone/Glowstone dust items with reversible explicit metadata. Before any mutation, they require the original explicit dust composition and verify that neither machine map has an enabled exact single-item recipe for the fixture. This avoids overriding real exact recovery recipes, which have priority over equivalent forms. The tests then use public metadata registration, the normal overlay lifecycle, actual `RecipeInputs.matches`, native `RecipeMap.findRecipe`, warmed exact row identity and `RecipeInputs.consume`. They check unique compatible selection, multiple-compatible ambiguity, and an irrelevant unusable tag not overriding a valid explicit candidate. Exact metadata objects and tags restore in synchronous `finally`; no recipe is deleted to make the fixture pass.

These corrected assertions passed on both native loaders in the final 987-case run. Their pre-guard Forge run ended with exactly three renamed semantic failures and five unchanged controls passing. The checks cover native recipe lookup/consumption planning, not a placed machine tick, pipe transport, or a broad extension of five missing prefix bindings.

## Source anchors

- Original reference commit `3703e40308c8c030763fd6297dea8b210d2a77b1`: `GT6_Main.java:145–150`, `OreDictPrefix.java:353–370`, `OM.java:335–353`, `Loader_Recipes_Ores.java:383,415–420`, `Recipe.java:895`.
- W reference commit `607771f74acb0fe647f7e9412676e72094799545`: `GT6_Main.java:161–166`, `Recipe.java:1116`.
- Native Dolamide identity8753/byproducts: generated `MaterialOreProcessing.java:1019`; source literal Dilithium8317, DiduraniumTrioxide9198, DuraniumHexafluoride9199, DuraniumHexabromide9201.

Final auxiliary checks also passed: 329 prescribed CI Python tests, 491 REMI checks, sediment registration, source consistency, core checks, both builds, packaging and SDK closure (120 core API classes and 46 native API classes per loader). All 24 runtime/test/CI source hashes remained unchanged across final validation. Hosted results must be checked for this exact new commit separately.
