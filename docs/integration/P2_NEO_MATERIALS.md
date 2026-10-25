# P2 NeoForge shared material catalog

## Implemented boundary

Based on `ffe109c8834610625802587a360621a657002194`, NeoForge now initializes the **same** shared material classes used by Forge. There is no second material directory, ID allocator, alias map, prefix registry or reaction engine. The source remains the saltnya import (`import/saltnya-snapshot`, `41561782`); brokestar and masson material tables were not blended into it in this step.

`GregTechNeoForge` binds `ModData.bindPresence(ModList.get()::isLoaded)` before invoking any metadata/material holder, then binds the material log sink. It follows the existing domain holder order (`PrefixRegistry`, `ModReferences`, `MaterialGroups`), calls `GTMaterialRegistry.init()`, and applies the single shared `MaterialRoleFlags` helper. Domain `postInit()` runs through common-setup queued work, after those role flags and before GameTests. Static fields on the Neo entry point contain only the mod ID and logger. Forge `Loader_Materials` calls the same helper after `init()`; its subsequent item/block registration and `Loader_MaterialPost` vanilla binding order are retained.

## Role closure and platform seams

Five original sources moved into `core` without changing their packages:

- `GTOreVeins`, `GTStoneLayersGen`, `GTWorldgenMaterials`: original bytes retained exactly, including every ore/layer/contact table and the existing rule scope.
- `GTBedrockOres`: both original Overworld/Nether tables and `BedrockOre` record retained. Only the Minecraft-dependent `forDimension(ResourceKey<Level>)` method moved to Forge `GTBedrockOreDimensions`. The production caller and three existing `NetherWorldgenTests` calls use that helper, with identical expected results.
- `StoneType`: all 27 enum entries, ordering, material links, texture names, multipliers, harvest levels and wither-proof values retained. Only four Minecraft imports and `properties(StoneVariant)` moved to Forge `StoneBlockProperties`; `GTStoneBlock` and `GTStoneSlabBlock` use it. The original hardness/resistance calculations and reinforced-brick multiplier are unchanged. `StoneVariant` remains in Forge and is used as an original-source dependency only by the baseline fixture.

`MaterialRoleFlags.apply()` requires a linked `READY` registry, calls the unchanged `GTWorldgenMaterials.flagOreMaterials()` and then the original `StoneType.values()` STONE loop. The ore rule retains its original inputs, including **only Overworld bedrock ores** in that flagging loop; this step does not silently add the Nether bedrock table to the rule. All original lists and mutable/public visibility remain unchanged. Worldgen feature implementations, dimension selection, block construction and registries stay in the platform source tree.

Original and integrated SHA-256 values for all five sources are recorded in `core/provenance/material-role-extractions.json`: three byte-identical relocations and two narrowly removed Minecraft seams. The original source files remain untouched in `Libs`; their former integration `src/main` copies are removed to prevent duplicate classes. The prior 39-material-source extraction manifest remains unchanged.

These two removed Minecraft methods change their Java owner/type signatures. Existing dependent compiled addons would need to be rebuilt against the new Forge helpers. Persisted material IDs, names, aliases and amount units are unchanged; this does **not** certify old saved worlds.

## Independent original goldens

The original Git blobs for the material/core closure and all five role sources were read and hash checked. They were compiled under fresh GUID output directories with the saved inert game stubs, and bootstrapped through the **original** `GTWorldgenMaterials` method and original STONE loop, rather than the extracted helper. Original `StoneVariant` is also hash checked. `OriginalMaterialRoleProbe` invokes the new four fixtures against these original classes.

The original role observation is SHA-256 `734a7fee39e9326dbfae519c34f3b9a0cb510d7d7a5535858cd04ab0aea99ccb`. It covers each of the 1156 raw registered objects' ID/name and three role booleans, plus all 27 stone descriptors and their ordinal/material/texture/physical metadata. Original counts are 210 `GENERATE_ORE`, 415 `GENERATE_ORE_PROCESSING` and 81 `STONE` objects. Counts include preexisting roles; they are not counts of new mutations by the helper. The JSON provenance defines the sorted UTF-8 row format and stub limitations.

The four new Neo development GameTests check:

1. All 1156 objects, 1101 unique positive IDs, 1519 name entries and 109 prefixes; canonical IDs/aliases, positive composition links, loader presence and Cu/Sn/Bronze identities.
2. Full original role fingerprint/role counts and 27 stone descriptors; domain post-init mappings, fixed Cu/Sn/Bronze physical values, ordered bronze composition, prefix fractions and Kelvin/litre boundaries.
3. The original cold/hot Cu/Sn reaction: 3.5U Cu plus 1U Sn rejects at 1356K and yields 4U bronze plus 0.5U Cu at 1357K, preserving exactly 2,918,916,000 material units.
4. The original explicit annealed-copper alternate recipe and hematite/carbon/calcite reduction, with fixed quantities and original Fe melting point 1811K; cold/no-flux rejection and fixed iron yield. Reduction consumes gas/flux inputs without accounting them as iron yield, so this fixture does not assert whole-input unit conservation for that original reaction.

Expected values are literals retained from the original fixture/source observation, not recomputed using the extracted implementation. The role fingerprint observes existing objects and tables; it is a test-only observation, not a second runtime catalog. The original three P1 Neo bootstrap tests are unchanged.

## Validation performed

Both the original and current-core standalone runs passed all four material fixtures. Current main core also compiled independently with `javac --release 17`, UTF-8 and no game classpath; `jdeps` reported only `java.base`. Current-core main classes and test/stub classes use separate output directories. Every verifier invocation starts in a fresh GUID directory, so stale classes cannot satisfy it.

Reproduce with JDK 17 and Python (standard library only), from the repository root:

```powershell
python core/verify-material-roles.py original --check --jdk-home 'C:\Program Files\Java\jdk-17.0.4'
python core/verify-material-roles.py current --check --jdk-home 'C:\Program Files\Java\jdk-17.0.4'
```

In the isolated worktree, Java 17 launched Gradle and Java 17/21 toolchains compiled the respective platform sources. `--no-daemon --console=plain --max-workers=2 :core:check :compileJava :neoforge:compileJava :neoforge:compileBootstrapGameTestJava` passed in 52 seconds. The core check actually executed its 64 core assertions and 6154 material assertions. An initial compile failed on the three old dimension-method references in the existing Nether test class; updating those references to the Forge helper resolved it. Existing deprecation warnings remained. No Gradle source/configuration was edited.

After adding the fixed 1811K Fe input, explicit role counts and composition-link assertions, `:neoforge:compileBootstrapGameTestJava` passed again in 21 seconds; both fresh original/current standalone runs also passed the final four fixtures and reproduced the same role SHA. This remains compilation and inert-stub execution, not a real game run.

## Acceptance limits and next gate

No game, client, server, jar, reobfuscation or publish task was run in this isolated worktree. Successful bootstrap-source compilation and inert-stub fixtures are not actual GameTest execution. The parent must merge/review this commit and run the four new GameTests with real NeoForge, in addition to the three P1 bootstrap tests, and rerun Forge bootstrap/build/artifact gates against the enlarged shared core.

Neo still lacks material item/form registration, ore/stone block registration, worldgen feature/configuration registration, vanilla item/unification/composition bindings, UI/rendering, fluid/container integration and material-bearing saved-world coverage. Calling domain `postInit()` does not provide those adapters. In particular, unlike Forge's `Loader_MaterialPost`, Neo does not call the vanilla item-binding hooks yet. There is no complete Neo gameplay path claim from this change.

The earlier material baseline snapshots deliberately exclude worldgen/stone role flags and vanilla item binding effects; this new role baseline supplements rather than replaces them. Forge/Neo runtime, client, dedicated server, full gameplay and world save/reload remain separate acceptance gates. Source authorization remains unresolved under `LICENSE_REVIEW.md`; no root license was replaced, no third-party permission was expanded, and no binary was published.

## 2026-10-25 runtime fixture note

The original 1156-material / 109-prefix figures and role fingerprint above describe the preserved source snapshot. The live NeoForge bootstrap fixture now checks the subsequently adopted core catalog (1160 materials / 132 prefixes) and the optional addon-sample profile, while strictly reproducing the original role fingerprint after only documented source-restoration normalization. The original fixture is frozen under `core/src/test/fixtures/original-material-roles/`. See [the targeted verification record](VERIFICATION.md#2026-10-25--卡55后续neo材料启动基准专项) and its linked JSON receipt; the source-snapshot provenance is unchanged.
