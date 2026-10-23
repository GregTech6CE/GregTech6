# CI material snapshot repair and version 0.0.0

Failed run: https://github.com/SaltNya/GregTech6/actions/runs/36901215336

Both platform annotations report the same material snapshot assertion; the required-builds job then fails because its dependencies failed. The exact assertion was reproduced locally with `:core:materialBehaviorContracts` before changing its baseline.

## Cause and correction

The catalog snapshot includes `GTMaterial.getSourceMod().name`. The authorized rename changed `ModReferences.GT.name` from `GregTech` to `GregTech 6 Community Edition`, invalidating the pinned definitions and post-init fingerprints. Restoring only that display name in an ignored, temporary probe recovered both exact original fingerprints. Counts and all other observed fields remain preserved. See `core/provenance/community-material-baseline.json`.

The test keeps its full observation and uses the verified Community Edition fingerprints. No assertions or material fields were removed. The Forge CI task is now `:build`, so its matrix job targets the root Forge project instead of selecting build tasks across all subprojects.

The current source also retained calls to the missing shared `MoldCastingRules`; that previously implemented class was restored verbatim from the preserved integration snapshot, with its provenance recorded separately.

The user requested version `0.0.0` and both final platform JARs. Both platforms consume the root version property. Build commands and current artifact names are recorded in `docs/work/BUILDING.md`.

## Validation scope

Local pre-fix material task: failed with the same CI assertion.

Name-only differential probe: both original SHA-256 fingerprints recovered exactly.

Post-fix material task: passed, 6,154 assertions. Forge Java compilation: passed.

Remote confirmation: the user pushed commit `dbd38be949378aca72ea0174501a95c6d7c1f547`. Run https://github.com/SaltNya/GregTech6/actions/runs/36903728760 completed successfully in 4m 43s, including both platform jobs and the required-builds aggregate. Two packaging reports were produced. No remote push or workflow dispatch was performed.

Final local command: `gradlew.bat --console=plain :core:check :build :neoforge:build`. Result: **BUILD SUCCESSFUL in 11m 17s**, 28 actionable tasks (24 executed, 4 up-to-date). Both production platform artifacts were produced.

Packaging verification passed against all **89 current compiled shared-core classes**. Both descriptors have loader ID `gregtech6`, display name `GregTech 6 Community Edition`, and version `0.0.0`; both JARs have no duplicate entries or forbidden test-only entries. The final copies match the original artifact checksums. The complete machine-readable receipt is `work/release-0.0.0-packaging.json` and accompanies the delivered JARs as `packaging-report.json`.

- Forge final file: `build/libs/gregtech6-1.20.1-forge-0.0.0.jar`, 38,125,156 bytes.
- NeoForge final file: `neoforge/build/libs/gregtech6-neoforge-1.21.1-0.0.0.jar`, 5,811,239 bytes.
- Delivery: chat outputs `release-0.0.0/`, with both JARs, `SHA256SUMS.txt`, bilingual `RELEASE_NOTES.md`, and the packaging receipt.

No new client/server startup, gameplay or world reload is claimed by this change. Overall integration and version parity remain incomplete; the earlier full snapshot's runtime evidence does not automatically cover this reinitialized source tree.

## Follow-up: NeoForge artifact content gap

The user questioned the 5 MB NeoForge artifact. Direct ZIP inspection confirms this is a source-content gap, rather than a size optimization or missing current compiled core: Forge contains 1,843 class files, 74,536 asset entries and 2,368 data entries; NeoForge contains 95 class files, 12,959 asset entries and zero data entries. The current NeoForge platform contains only five Java source files for lifecycle, material item registration and client display. Its entry point explicitly states that gameplay adapters remain pending. The preserved `84be9ccb2e` snapshot has 941 NeoForge Java sources and 351 shared-core Java sources, including machines and world generation. That advanced implementation remains recoverable but is absent from the current source tree.

The delivered Release notes were corrected to identify the NeoForge artifact as a material-system preview. Build/packaging success must not be presented as complete cross-version gameplay support. The existing artifact bytes and checksums were not changed.
