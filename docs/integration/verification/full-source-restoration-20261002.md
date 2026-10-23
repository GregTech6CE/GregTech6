# Restore previously ported source to the current repository

## Trigger and scope

The user compared the supplied NeoForge 0.0.0 artifact with the older `initial-alignment-20261001-d86cdfbcad` artifact. The older artifact contains 1,954 classes and is 35,879,955 bytes; the superseded material-only artifact contains 95 classes and is 5,811,239 bytes. The current reinitialized repository omitted the later platform implementation. A successful build of that early tree did not establish cross-version functionality.

On the user's explicit request, restore the recorded implementation from `84be9ccb2ec2db43e15efe3d4f03c9098d9828c5` to `F:\Dev\GregtTech6New\GregTech6`. Restored source includes 1,117 Forge, 351 shared-core and 941 NeoForge Java files. Source, shared resources, platform resources and the matching core/NeoForge build configurations were recovered together. Root properties, Community Edition identity, updated material checks, English/Chinese README, user integration documents, CI workflow and origin were preserved.

The recovery reference `refs/remotes/recovery/complete-main` retains the available implementation history and original authors. Original notices/provenance were recovered. Missing later integration records were added; overlapping old summary documents were archived under `docs/integration/recovered-84be9ccb2e/`. No donor directory or third-party license was changed. Per-file recovery/move receipt is in ignored `work/complete-restoration-files.json`; a durable summary is `core/provenance/full-snapshot-restoration-20261002.json`.

## Necessary corrections

- The newer material prefix implementation lowercases explicit texture filenames with `Locale.ROOT`. Restoring that implementation changes the pinned snapshot after the authorized display rename. A temporary old-prefix shadow recovers the exact Community Edition fingerprints, and the combined old-prefix/old-name shadows recover the exact original import fingerprints. The current fingerprints are recorded in `core/provenance/restored-prefix-baseline.json`; no observation fields or assertions were dropped.
- The user-reported CI job at https://github.com/SaltNya/GregTech6/actions/runs/36909053404/job/110526845454 ran commit `3f0fad2` without this baseline correction and failed at the same material assertion. Local commit `92fb199fd` contains the correction and removes unused static Minecraft assets from the pure Java contract classpaths. Local checks then pass. No push to origin or remote run was performed.
- Packaging now requires GTBlocks, GTBlockEntities and GTFeatures on both platforms, plus Forge GTMachines/SmeltingCrucibleBlockEntity or NeoForge BasicMachineRegistries/SmeltingCrucibleEntity. These checks supplement the current compiled shared-core classes, metadata, duplicate and test-content checks. The known material-only artifact is correctly rejected at this gate. The initial gate used Forge class names for both loaders and was corrected to their actual corresponding implementation names before final delivery.
- NeoForge's description now reads the root description property and no longer identifies the restored implementation as merely an initial platform skeleton.

## Local build

Command: `gradlew.bat --console=plain -PdirectCoreResources=true :core:check :distributionJar :neoforge:jar`.

The existing optional direct-resource path embeds static shared resource files without first copying the entire catalog. Their archive paths remain the same. CI retains normal resource processing. Core contract classpaths now use compiled Java classes and their declared dependencies because those checks do not load the Minecraft asset catalog.

Existing core checks passed: 64 basic, 34 machine specification, 6,154 material, and 53 thermal assertions. The final build completed successfully in 8m 12s (22 actionable tasks, 18 executed and 4 up-to-date).

Final packaging passed against all 571 current compiled shared-core classes; both platforms have the required gameplay classes, correct name/ID/version, no duplicate entries and no prohibited test-only content. Delivered copies match the build output hashes.

- Forge: `gregtech6-1.20.1-forge-0.0.0.jar`, 38,527,588 bytes.
- NeoForge: `gregtech6-neoforge-1.21.1-0.0.0.jar`, 35,890,474 bytes; 1,955 class files, 73,730 asset files and 228 data files.
- Every one of the older supplied NeoForge artifact's 1,954 class paths is present in the restored artifact. Its additional class is the shared GregTechIdentity. This presence comparison does not assert unchanged method bodies or gameplay equivalence.
- Delivery replaces the previous files under chat outputs `release-0.0.0/`. The superseded artifact is retained only under ignored `work/superseded-material-only-release/`. Updated release notes identify the restored implementation and do not claim that the previous material-only CI success proves this tree.
- Complete packaging receipt: ignored `work/release-0.0.0-packaging.json`, delivered as `packaging-report.json`, alongside `SHA256SUMS.txt` and bilingual `RELEASE_NOTES.md`.

No new client/server startup, complete survival gameplay, actual world reload or old-save compatibility test is claimed. Restoring the previously implemented content does not mark the full three-source integration complete.
