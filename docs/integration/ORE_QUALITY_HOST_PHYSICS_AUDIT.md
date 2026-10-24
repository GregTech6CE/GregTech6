# Material quality and native ore-host physics audit

2026-10-24. **Joint full local verification PASSED: Forge336 + NeoForge327 =663 actual native cases, with zero failures/errors/skips. Remote CI and merge are not established by this local result.** The corrected unchanged-production baseline reproduced six semantic failures on each loader. Material metadata and host physics are one coherent correction; metadata alone would change mining difficulty against the old incorrect host bases.

Pinned reference commits: GregTech6 original `3703e40308c8c030763fd6297dea8b210d2a77b1`; W `607771f74acb0fe647f7e9412676e72094799545`.

## Numeric quality: exactly40 reviewed declarations

The importer previously recognized `.qual(type,speed,durability,quality)` but dropped numeric `.qual(harvestLevel)`. Original `OreDictMaterial.qual(long)` preserves existing type/speed/durability through the four-argument setter, including durability's minimum1 and quality's0..15 clamp. Both pinned original/W tables contain the same40 numeric calls:

| Cohort | Count | Corrected quality |
| --- | ---: | ---: |
| Rutile, Ilmenite, Bauxite | 3 | 2 |
| Scheelite, Wolframite, Ferberite, Huebnerite, Tungstate, Stolzite, Russellite, Pinalite | 8 | 3 |
| Diduranium Trioxide/five Duranium halides; Tritanium Dioxide/five Tritanium halides | 12 | 4 |
| Tantalum Hafnium Carbide | 1 | 2 |
| Explicit numeric zero-quality calls | 16 | 0, retained |

Thus23 source-positive ores and one non-ore compound gain their declared quality; **all40 gain durability0→1**, including the16 zero-quality cases. These declarations have source type0/speed1 defaults, not copied tool statistics. Ordered numeric calls are emitted without inventing calls for other materials. Seven `.qual(material)` copy overloads remain explicitly deferred: AluminiumAlloy, ConductiveIron, PulsatingIron, ObsidianSteel, ElectricalSteel, EnergeticAlloy and VibrantAlloy. Their full tool-stat copy semantics are not guessed.

Quality is not only a harvest label: it reaches existing recipe work. Native expectations require Bauxite raw Crusher/Hammer384/96 ticks and Scheelite512/128 ticks, at16 EU/t, with literal output forms/counts/chances and actual accepted-row/runtime-lookup checks. No expected recipes are inserted.

### Identity/form preservation and supported SDK controls

A bounded40-identity tool-form fallback uses actual tool type rather than treating positive quality as permission to generate tool heads. Explicit head grants and unrelated legacy policy remain unchanged, including existing Tantalum Hafnium Carbide and Ancient Debris heads. This is not a global form-policy rewrite.

Isolated core graph comparisons contain155,935 rows at both definitions and postInit: only40 material rows differ, only the24 positive quality fields and40 durability fields change. All56,085 item-definition rows are byte-identical, SHA-256 `c3360a6cdc2ac4aa396b7a9bb778f11663171afb4e949709c65f3903f25a5fef`. Identities, forms, aliases, prefixes and other material fields remain unchanged in those comparisons.

Six fresh core JVM controls used a contributor compiled solely against the published SDK and loaded through ServiceLoader. Baseline and candidate both reject an invalid Bauxite pickaxe-head request, accept Bauxite dust once, and accept the explicitly permitted Tantalum Hafnium Carbide pickaxe head once. They verify canonical definitions and link callbacks without mutating builtins. These establish supported core declaration behavior, **not native item registration or a blanket guarantee for private/internal setter overrides**.

Local evidence: `work/material-quality-audit/{source-provenance.json,numeric-reviewed.json,graph-differential.json,importer-results.json}` and `explicit-form-controls/supported-results.json`.

## State-aware host physics and bounded admission

`SourceOreHostPhysics` encodes75 reviewed host/mode tuples from original/W `Loader_Rocks`, `Loader_Ores` and `PrefixBlock`. Harvest level uses each host's offset/minimum and clamped material quality; hardness and resistance use that derived level and literal host bases. Native state hardness, contextual blast resistance, harvest/drop decisions, mining progress and common tooltip consumers share the reviewed facts. Broken-mode hardness must not receive a second legacy speed multiplier.

Unknown/missing hosts or modes are **not default stone**. Noncanonical/addon-owned blocks, unreviewed states and EXPLOSIVE materials retain prior admission/passthrough behavior. Custom tool/component speed is not rewritten. GT effective speed remains category-, tier-, usability- and power-gated. Existing block/material IDs and fluid-motion behavior are not changed. Source mud is hand-droppable but has no reviewed native host identity, so no speculative mapping is added; sediment BROKEN modes are not silently admitted.

### Six sediment modes: drops are not effective-tool speed

Sand, red sand and gravel in NORMAL/SMALL modes use original `Material.sand`, which permits drops without a required tool. Actual `BlockState.requiresCorrectToolForDrops`, native player drop decisions and hand progress must reflect that: hand, wrong-tool and below-tier drops are permitted for these six tuples. A GT pickaxe can still be **ineffective for speed** on shovel-preferred sediment. Hand progress uses the harvestable30 divisor at the source hardness, not the unharvestable100 divisor.

The earlier candidate's mandatory-shovel assumption was an incorrect test oracle and is explicitly retracted. Paired source plus official Forge1.7.10 hooks and SHA1-verified Mojang bytecode establish distinct `toolNotRequired` and `adventureExempt` flags. Old LH tooltip code checked the latter and could advertise a tool requirement despite hand-droppable material. The new English/Chinese optional-tool text deliberately describes the actual drop rule, with preferred shovel but no mandatory tier. **This is an intentional modern UI correction, not byte-identical old LH parity.** Only the six proven modes use it. See [source correction and provenance](ORE_HOST_PHYSICS_SOURCE_CORRECTION.md).

## Actual baseline chronology

All baselines below used unchanged production at `6135f4b83c3a32948cc2551471ce0f1fa6b7afb7`; before/after source snapshots matched.

1. **Initial prerequisite failure:** Forge62 contained55 old passes and7 new failures. Five physics cases stopped at the owned-cell initially-air check because fixture coordinates were off by one; this is **not physics-defect reproduction**. NeoForge was not run. Quality stopped at Rutile metadata and the bundled Bauxite prerequisite. Core independently failed Rutile expected2/got0; importer8 yielded2 passes,5 failures and1 missing-diagnostic-key error.
2. **Coordinate/order correction:** both loaders62 =56 passes +6 semantic failures. Moving only readiness-owned coordinates and placing the retained quality assertion after literal recipe checks exposed native hardness, progress, harvest, tooltip, Rutile quality and Bauxite work failures. All55 prior selected cases and unknown-host passthrough passed. Later assertions beyond the first failure are not claimed executed.
3. **Source-corrected V2:** the harvest test now asserts the true sediment no-tool state flag first, before later harvest controls. Both loaders again report exact62 =56 passes +6 semantic failures, with no fixture/prerequisite failures:

| Failure | Actual first failing evidence |
| --- | --- |
| Host hardness/blast case | Black granite NORMAL hardness expected12, actual9 |
| Harvest case | Material.sand state incorrectly requires a correct tool for drops |
| Mining progress case | SMALL source hardness ratio expected1.5, actual1.0 |
| Tooltip case | Source resistance below4 should omit common blast row |
| Quality metadata case | Rutile numeric quality mismatch |
| Recipe work case | Bauxite raw Crusher actual128 ticks/16 EU/t instead of384/16 |

V2 Forge exited1 in4m29s; NeoForge exited1 in2m41s. Strict gameplay checkers correctly rejected the failures; separate exact discovery checks passed, which is not gameplay success. These targeted62-case runs are **not full663 runs**.

Evidence: `work/validation/ore-quality-physics-baseline-v2/local-validation-receipt.md`. V2 XML SHA-256: Forge `1ed0d561eafa8069af363a10eb35a495c144eec13202d6f5bbb580601fac5962`; NeoForge `0333b93eb120bb7250d8da18d869f3171668c6ae562296d5d20ae1ef07fb2bc4`. Initial/corrected receipts remain in sibling baseline directories.

## Reproduction and gate scope

Use the repository Gradle wrapper and installed JDK17/JDK21 toolchains, not a private wrapper. For focused source/core controls:

```sh
bash ./gradlew --no-daemon :core:materialQualityContracts :core:oreHostPhysicsContracts
python -m unittest tools.tests.test_material_quality_import tools.tests.test_ore_host_tooltip_localization tools.tests.test_issue_ci_evidence
```

For targeted native reproduction, use a fresh disposable directory for each loader:

```sh
bash ./gradlew --no-daemon -PaddonSample=true \
  -PgameTestNamespaces=gregtech_ore_source_audit -PgameTestHeap=3g \
  -PgameTestDirectory=build/quality-audit-fresh \
  -PneoforgeGameTestDirectory=build/quality-audit-fresh \
  -PgameTestReport=build/reports/gametest/quality-audit.xml \
  :multiversion:1.20.1-forge:runGameTestServer
# Repeat with :multiversion:1.21.1-neoforge:runGameTestServer and another fresh directory.
```

That namespace selects62 cases, not663. The full gate must use all namespaces and both tasks in `.github/workflows/issue-regressions.yml`, then `tools/integration/check_issue_gametest_report.py` with the matching `--platform`; the full checker intentionally rejects a targeted report. Full builds/core, source/Python guards and actual package verification—including hardness and required-tool getter mixin mappings—remain mandatory. No mapping exemption is introduced.

Cached dual compilation, isolated969 quality assertions,421 physics assertions, importer8/localization2 controls and synthetic checker results are supporting evidence, separate from the completed full native result below. The authoritative local preparation ledger is `work/ore-generation-audit/quality-physics-combined/reviewed-v2/HANDOFF.md` and its manifest/replay proof.

The completed fixed663 retry also included the separately documented [builder owned-fixture correction](BUILDER_PLACEMENT_FIXTURE_AUDIT.md): a proactive source-proven fixture risk, **not an observed remote builder failure**. No new native case IDs result from that correction. Both modified builder cases passed on both loaders within the full fixed663 gate; the original risk classification remains unchanged.


## Final full local663 result and retained gate history

The first full fixed attempt stopped at localization provenance: `sourcePolicyCheck` rejected the new Chinese `tooltip.gt6ce.ore.hand_harvest_preferred` value because it was absent from the existing source/alias/English-fallback provenance path. Gradle exited1 in59s; Python176 had175 passes and1 error on the same synchronization guard. Native builds/package checks and GameTests did not complete. This was not a native663 failure or success.

The exact four-file correction `dc8b67cfd57c6ecdbd25ba60e50f486f5509e6d2b2e1a66b76399fdaee1f54da` updated `tools/localization.py`, `tools/localization/ore_host_project.json`, its README and the focused localization tests. It records this text as a **project-authored translation**, not a fabricated TeamNED/source translation. Original compressed translation source, source/alias/English provenance and both language resource files stayed unchanged by that correction. Existing assertions were not relaxed.

The fresh retry passed on the frozen tree based on6135 plus the reviewed joint production/guards, exact localization provenance correction and owned builder fixtures:

| Native platform | Actual passed | Native/strict checker exits | Duration | XML SHA-256 |
| --- | ---: | --- | --- | --- |
| Forge | 336/336 | 0 / 0 | 8m50s | `d502fab380f350dc453ce9ad1ceee1569079888ec79fa4938f72e276057dcca2` |
| NeoForge | 327/327 | 0 / 0 | 4m32s | `2f3de747bf6eb680176aaeec6a77b13297700db966f5b86b3d18f2ae89635d4a` |

All649 prior cases and all seven new cases passed, including unknown-host passthrough. Dual build/core passed in4m49s, with actual969 material-quality and421 physics assertions. All182 Python tests and491 REMI groups passed. Actual packaged physics helpers/mixins/configs and standard refmap guards passed without exemptions:1045 shared classes per loader, SDK118 core +46 native per loader. Input/final source snapshots were byte-identical and diff checks clean.

Terminal evidence: `work/validation/ore-quality-physics-fixed-retry/local-validation-receipt.md`, snapshots, XML, strict-checker logs and package receipts. Earlier fixed-attempt evidence remains under `ore-quality-physics-fixed`. No Shredder/optional-checker optimization entered this gate; cold restart was not rerun. This is a full **local** gate result, not remote-CI, merge or release approval.
