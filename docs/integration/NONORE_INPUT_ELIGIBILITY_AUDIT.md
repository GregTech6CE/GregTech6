# Bounded nonore input eligibility

2026-10-24. This repair concerns **generated item inputs in the ore loader's maceration and washing branches**, not removal of every recipe containing a nonore material.

## Source basis

The paired original material trace reviews54 identities whose port ore-processing flags were inferred from generic material-target calls.53 have no original builtin `ORES` property after tracing their declarations, factory/group inheritance and mutations. Sand8100 remains `UNKNOWN` because the W definition is overridable. This is a bounded positive/negative evidence profile—not “missing from an extracted table means forbidden.” Canonical numeric ID/name and builtin ownership are required; addons and unresolved identities retain existing behavior.

References and per-material declaration locations are recorded in [nonore-input-source-profile.json](nonore-input-source-profile.json). Original snapshots are `3703e40308c8c030763fd6297dea8b210d2a77b1` and W `607771f74acb0fe647f7e9412676e72094799545`. `OreDictMaterial.setSmelting` supplies a melting target, not the `ORES` property; `.ores(...)` supplies byproducts. The port importer previously treated `ores`, `setSmelting`, `setCrushing` and `setOreMultiplier` uniformly when emitting `GENERATE_ORE_PROCESSING`. The current runtime `GTMaterial.setSmelting` itself only adds `MELTING` for a positive target.

Existing IDs are deliberately retained. A core definition census still finds all seven raw/crushed-family item forms for each of the53 identities—371 item IDs. None of those53 currently qualifies for the ordinary standard/small ore **block** prefix; item-form reachability must not be confused with generated ore-block availability. Dust and rock forms remain, as do forced Ice gems.

## Exact production boundary

Only these `OreProcessingRecipeLoader` producers change:

- The six crushed-family inputs in `processMaceration`, on Crusher and Shredder.
- The legacy crushed+water Bath row and purified-input Centrifuge row in `processWashing`.

Existing output forms, slot counts, chances, fluids, optimization, energy and duration are retained. For eligible source-NO builtin inputs, immutable recipe definitions are kept dormant instead of registering fabricated input routes. Unknown or supported startup overrides use the original registration path. No item, material property, target link, recipe framework, furnace table or world-generation registration is globally removed.

A dormant definition is not an accepted recipe. Ownership begins only with the exact nonnull `Recipe` returned by normal collision-checked admission. Rejected definitions do not own or displace the incumbent. Cleanup unlinks only accepted identities; it never sweeps recipes by input, output, signature or namespace.

## Supported external inputs and ordering

Actual externally registered forms remain authoritative even when the source material does not generate its own ore forms. Supported accepted startup form requests or explicit foreign/mismatched bindings defer the entire material to its original startup behavior. Restricting this exception to a single requested prefix could strand the purified/refined outputs of a valid external input.

After a supported tag reload, current foreign bindings—including `rawOreChunk → crushedTiny ×3`—can activate the dormant material continuations. Automatic GT self-tags do not activate them. Activation means conservative external defer, **not** a claim that the original material has `ORES`.

The existing overlay restores independent startup rows, rebuilds external tag recipes, activates owned continuations, then applies compatibility and datapack rules. Startup same-input winners retain collision precedence; datapack removal/addition remains final. Removing the tags deactivates only owned continuation rows. Actual accepted identities are excluded from first baseline capture. Server-stop cleanup runs after external/oven unwinding and retains immutable definitions for possible later use.

## Verification status

Both loaders passed the actual isolated supported-tag baseline and the candidate-aware restoring profile on frozen `327089700` plus production candidate `2622de2a…`. This result predates PR #102 (`0238e85e`) and does not establish its integrated native results. The latter reproduced a failing initial source-NO absence assertion before the repair, then verified:

1. PVC tiny/purified continuations absent initially.
2. Real external tags enable raw-chunk and crushed-input routes and their downstream continuations.
3. A public `onRecipesReady` Rubber recipe remains the exact same-input winner during attempted continuation admission, including warmed lookups.
4. A real datapack override has final precedence.
5. A completed restoration reload removes all foreign bindings/routes, returns PVC continuations to absence, preserves independent startup recipes and restores exact original pack order.

These are native one-case profiles on each loader, not proof that every producer for all53 materials is aligned.

The subsequent three-case cohort baseline/fixed comparison passed its strict phase gates on both loaders at PR #102 base `0238e85e`. The same five fixture files were retained while the exact seven production files were reversed for baseline and restored for fixed; their original hashes were verified. Each baseline had two passing controls and only the expected source-policy failure (728 admitted eligible role observations). Each fixed run passed all three cases. Every run recorded53 source-NO identities ×14 scoped roles =742 actual accepted-row/lookup observations. The controlled startup classified **52 eligible source-NO materials and one accepted Bakelite crushed-form override**, with zero additional external deferrals. This observed fixture partition is not a universal assertion that arbitrary addon configurations contain exactly52 eligible materials.

Fixed runs had no accepted/selected recipes for those52 materials in the scoped maceration/washing roles. Bakelite, known-ore, Sand-UNKNOWN, addon, rock, Wood Mortar and forced Ice-form controls remained valid. Actual73 dust recipe rows were canonically equal between baseline and fixed on each loader, including represented forms/counts, chances, work and fluids. This cross-phase comparison is distinct from the same-run overlay-preservation assertion; it does not claim arbitrary unrecorded NBT/components/flags parity.

Cohort native XML SHA256:
- Forge baseline: `279302454550974019e021aa9fcfc9e6d2d86aadfbccc29ec8c7717f5cc84fe9`; fixed: `2e115ac478581c5f452d9f40804c0ba09c7ac899d310d96f0d94af4f39c18370`.
- NeoForge baseline: `622e835d5bc81e45ddace868df7bf5bb53d0eb136115f6a681f5c35f9ed3f0a3`; fixed: `71fdf918b8bbc923b2bec1204628b1d97bd73bcee60b61ed865b30f890ac788b`.

These isolated profiles are separate from the regular native suite. They do not establish whole-chain alignment for all53 identities or complete coverage of other producers.

The separate active-row shutdown profile passed on both loaders on PR #102 base `0238e85e` plus the same production candidate: a real `ServerStoppedEvent` observation after production cleanup and a mandatory post-test receipt verified two captured active rows and their warmed lookups absent, foreign bindings/routes absent, and the startup winner retained. XML success alone was insufficient. This is distinct from the earlier restoring pair and does not constitute a full regular-suite run. Two fresh JVMs cannot prove opening a second integrated world in the same JVM. A same-ledger clear/reconcile contract is narrower than that integration claim.

## Final integrated verification

The final frozen PR #102-based source set passed the regular dual-loader suite: **833 cases = Forge421 + NeoForge412**, with identical pre/post source snapshots. This preserves the existing regular namespace/count guards; the mutating isolated profiles were not silently merged into that count.

The checked-in isolated runner was separately verified locally through the separately reviewed explicit Gradle-wrapper mode. Each loader ran cohort3, restoring1 and actual-stop1 sequentially in separate fresh JVMs/worlds: **five cases per loader, ten separate passes**, with strict phase/cleanup/stop receipts. The separate fixed-only immutable-ownership mechanics profile also passed once per loader (**two further passes**). These totals are separate from833. Local invocation of the migrated runner is not a claim that GitHub-hosted workflow execution has completed.

The final build/core checks,210 Python tests,491 material search/index groups, addon SDK validation and native mixin packaging guards passed. The source-NO cohort evidence remains the bounded observed52/1 partition described above; the ownership mechanics control does not prove a second world opened in the same JVM. No whole-chain53-material alignment or untested producer removal is claimed.

Final regular-suite XML SHA256:
- Forge: `4e9b3a1941fc1003e33c3531b2e6ea3755aa7cc36cba0f9bf10255f9724c9694`.
- NeoForge: `b6f40a4898ddf05770be0b800d26b3d4c5bd80cf10101dbece712d3327964d93`.

## Deliberately retained surfaces / next work

The following producers are outside this repair and remain unchanged:

- Raw-ore Crusher/Hammer entry recipes; special dense/icon/vanilla ore entries.
- Sluice, Sifting and MagneticSeparator; ByProductList display rows.
- PartRecipeLoader and native FurnaceSmeltingRecipes cooking/mirror routes.
- AnvilShreddingRecipes, whose required empty second slot differs from ordinary single-input admission.
- Crucible display recipes and live composition-based consumption.
- Packing/unpacking/boxing and slot-sensitive manual form-conversion crafting.
- Raw crate utility processing and other independent explicit recipes.

Generic dust/ingot recycling, legitimate rocks, Wood Mortar, forced gem forms, addon-owned rows and external producer inputs are preserved. Missing source thermal behavior, if found independently, must be addressed as its own recipe gap—not concealed by a blanket nonore prohibition.
