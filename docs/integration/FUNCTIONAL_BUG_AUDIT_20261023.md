# Functional bug audit: 2026-10-23, first verification batch

Baseline: `bc0bef31849e5b19750885157ff366ee30300243` on main. This continues the portable-vessel/wooden-bucket audit commits without replacing the project architecture. This is a scoped bug-fix batch, **not completion of whole-mod parity**.

## Read-only source references

- Original GregTech6: https://github.com/GregTech6/gregtech6/tree/3703e40308c8c030763fd6297dea8b210d2a77b1
- Higher-version reference: https://github.com/wolfram0108/gregtech6_w/tree/607771f74acb0fe647f7e9412676e72094799545
- Existing original copyright/license notices remain; no reference repository is modified and no original implementation file is imported wholesale.

## Corrected contracts

| Area | Defect and repair | Required verification |
|---|---|---|
| Radaway (ALIGN-422) | Item callback and food Finish event both applied -50. Remove duplicate item callback, keep registered food event as single owner. Original `MultiItemFood:912`, `FoodStat:153–173`. | Last pill, stacked/repeated pills, zero clamp, ordinary food control through actual item-use ticks on both loaders. |
| Wooden bucket (ALIGN-432 and transaction boundaries) | Permit water levels 1/2 top-up, retain empty/full rules; never replace lava/powder snow. Check world permission before capabilities, require successful block write before consumption; client simulates and server commits. Original `Behavior_Bucket_Simple:185–200`. | Nine bands, survival/creative, levels 0–3, non-water rejection. Current creative preservation intentionally retained. |
| Pump | Floor packet acceptance could strand 30-RU input at8190 below8192 forever. Restore original final-whole-packet ceil and matching demand/face gate; clamp negative restored buffer. Original `MultiTileEntityPump.doInject:263–271`. | Actual shared rule contract for every supported speed/sign, final-packet overshoot, refill, invalid/extreme inputs. Platform world pumping remains a separate acceptance boundary. |
| Packet buffer API | Multiply-before-bounding overflow and negative restored energy. Divide capacity/energy first, bound packets before multiplication, clamp restored energy nonnegative. | Actual API simulation/commit conservation and long extremes. API correctness is not a claim every machine uses this class. |
| Garbage menu (ALIGN-403 and partial transfer) | Empty hotbar SWAP copied a view without deducting pool. Explicitly extract. QUICK_MOVE could lose partial leftovers through Inventory.add success/creative semantics; use native container transfer and restore every remainder. | Real menu clicks, survival/creative, all hotbar/offhand, occupied/invalid targets, full/partial/no inventory space, stack sizes64/16/1, adjacent pool entries. |
| USB/HDD selector (ALIGN-289) | Host reset every panel removal before selector-aware shared callback. Remove redundant reset. | All16 modes and actual file read after emitter removal; selector removal still resets. |
| Bees (ALIGN-164/165/171) | Clear old bee corpses rather than re-kill them; skip empty primary drone slot so lone princess reaches promotion; dirty native chunk on life/countdown/brood changes. Original ordinary/advanced Bumbliary and `TileEntityBase05Inventories.slotKill`. | Both layouts, pairing/generation transition, dead outputs/non-bee preservation, occupied/blocked slots, unsuitable offspring, native dirty bit and serialization. Independent disk restart remains unverified. |

## Test execution and honest boundaries

- Actual Java17 compilation of all core API/main/test sources succeeded; 120 separately launched core contract processes passed. New contracts: packet buffer8020, pump40901, Radaway138 assertions. Existing deprecation warnings remain.
- 28 source-policy/evidence unit tests passed. The evidence-parser fixture output is **not a GameTest execution result**.
- Local native Gradle execution stopped before source compilation because the offline cache lacks Stonecutter0.7.11. No native compile pass is claimed from this attempt.
- Full local language/native-layout guard invocation cannot pass without generated native tooling views; absent-view failures are retained in local logs, not bypassed. CI generates views before running these guards.
- Focused CI now runs on main as well as the existing verification branch and requires exact passing XML evidence: Forge19 and NeoForge15 selected cases for this batch. Build success alone is not gameplay success. Consult the Actions result for the exact commit, not this pre-execution document, for native completion.
- Full natural survival chains, client rendering, independent process saves/restarts, old-world migration, multiplayer synchronization and optional-mod compatibility remain separate unverified contracts.

## Deliberately preserved and continuing

No broad fluid movement port; retain previously approved gas adaptation. No speculative changes to Radaway duration, Geiger stack limit, severe radiation policy, stopped instrument readout, creative bucket consumption, bee advanced-slot overlap or existing full-output policies. Material/user translation tables are not changed by this batch.

Issues65 (structure shape materials),68 (water assimilation/expansion) and69 (Xaero coloring) remain distinct open work; a map fix cannot prove terrain assimilation fixed. Earlier closed issues remain based on their earlier recorded evidence.

Next passes cover recipes/material processing, remaining machine/transport transactions, covers/logistics, tools/plants/mobs, world generation, rendering/integration, and disk/client lifecycle boundaries. Cumulative ALIGN identifiers are not a count of unresolved bugs. Deliberate modern adaptations and missing features are tracked separately from confirmed defects.

## Second verification batch: machine I/O, machining remains, tool placement

This extends the first batch rather than marking the full coverage matrix complete.

- Basic machine output planning now reserves both capacity and fluid identity in each initially empty tank. Previously different fluid outputs could reserve the same tank and consume inputs before becoming blocked; queued outputs were retained, so this is not described as material loss. Matching/split output reservations and component/tag identity are tested separately.
- Active machine item input/output and air ejection use the same own-face cover policy as passive sided capabilities. Fluid auto-output remains unchanged because original source deliberately uses its raw tank path.
- Existing lathe/cutter rows whose original handlers enable remains now return the exact pulverization target and quantized remainder. No new generic roller recipes or material table changes. Tests inspect actual registered rows, independent source-unit anchors, and the real cutter admission/flush with blocked remainder output.
- Ordinary builder wand uses the clicked face/local hit coordinates and assembled head material quality, not a fixed upward placement/reach. Source-stock placement goes through the loader transaction so cancellation restores the world without consuming stock or tool wear.
- Mining-drill torch placement similarly commits stock only after its own cancellable placement transaction succeeds. Creative stock/held drill identity/charge are preserved; placement listeners are removed after tests.
- The first run additionally discovered that registered manual/button selector covers were absent from CoverItems admission. Those two exact IDs are now recognized; the strict USB/HDD test retains actual selected-file reads and reset behavior for all three selector types.

Required selected native cases now total Forge29 and NeoForge25, including the earlier regressions. Ten new cases exercise real registered machines/items, current private transaction methods where explicitly stated, native server interactions and loader placement events. Fixture-driven transaction calls are not full natural production-chain evidence. Native compilation and execution are required by CI for the exact published commit; no pass is inferred from source inspection.

Source chains include original `MultiTileEntityBasicMachine.canOutput/doInputItems/doOutputItems`, `RecipeMapHandlerPrefix` and enabled `Loader_Recipes_Handlers` rows, `OM.pulverize`, ordinary `Behavior_Builderwand` (not its Thaumcraft special branch), and registered drilling Torch behavior. Placement wrappers were checked against official Forge47.4.20 sources and NeoForge21.1.243 sources/userdev patches. Full optional-plugin protection compatibility remains outside these native cancellation tests.

## Third verification batch: clone persistence and radiation immunity

- Non-death player replacement now deep-copies only the food-statistics compound, preserving unrelated destination data. Missing original food state clears stale replacement state; death still clears food and radiation. Both pinned loaders automatically copy only PlayerPersisted, not this mod's root food compound. The earlier Forge test that expected a pre-seeded replacement value is corrected to assert original-value transfer.
- Reactor exposure delegates to the existing shared radiation-protection predicate, retaining server/alive/dose and undead/arthropod guards. This restores existing creative, iron-golem/wither and full-suit protection without changing severe-dose direct-damage policy, armor tags, species compatibility or global fluid capacity behavior.
- Five additional native cases per loader use real ServerPlayer.restoreFrom (not synthetic Clone events), detached-compound mutation checks, death/absent controls and native exposure targets/armor. Selected totals become Forge34 / NeoForge30; source assertions or serialized tags alone are not treated as successful player restoration.
- Reactor cooling/output order, steam-expanded capacity and handheld instrument dispatch remain separate investigated work, not silently included in this patch.

## Fourth verification batch: registered handheld instrument consumers

Registered electrometer/tachometer items now dispatch to actual cable/axle/gearbox counters, including original connected gearbox input/output face diagnostics. A read-only gearbox predicate exposes its existing recorded input mask; transfer timing and stopped-readout policies are unchanged. Unsupported/wrong-category targets return PASS and consume no instrument.

Four exact project-owned message keys reuse existing direction translations. Dedicated allowlist/placeholder/English-drift/source-binding collision checks protect the language pipeline; original TeamNED bytes and existing Jade scope remain untouched. This is new instrument localization, not a claim of copying original translated sentences.

Four cases per loader use registered instrument callbacks and real wire/charging-table, axle startup and gearbox/transformer transfers, recording only chat delivery. Native compilation/runtime remains gated by CI. Required totals are now Forge38 / NeoForge34; no full electrical-network or survival-chain certification is inferred.

## Verified native checkpoints

- First batch final `a59bf9e44565439f29de8924acbb89a4775dfe4a`: [focused run39877968391](https://github.com/GregTech6CE/GregTech6/actions/runs/39877968391), Forge19/NeoForge15 passed; [dual build39877968386](https://github.com/GregTech6CE/GregTech6/actions/runs/39877968386) passed. After fast-forward to main, [main focused39878627071](https://github.com/GregTech6CE/GregTech6/actions/runs/39878627071) and [main build39878627150](https://github.com/GregTech6CE/GregTech6/actions/runs/39878627150) passed again.
- Earlier first-batch attempts correctly failed on selector admission, a loader-specific test API, and the unanchored consumption fixture. These checks were not disabled. The consumption fixture was isolated at an actual test position with no gravity; precise120→70→20→0 and item-count assertions remain.
- Second/third batches final `69d66fb8851752e01de7ca5aa82bf3e305406509`: [focused39878636024](https://github.com/GregTech6CE/GregTech6/actions/runs/39878636024), Forge34/NeoForge30 passed; [dual build39878636010](https://github.com/GregTech6CE/GregTech6/actions/runs/39878636010) passed. These are native cases, distinct from Python XML-parser fixtures and the120 pure-Java contract processes.
- These checkpoints do not cover later instrument/reactor changes; require the Actions checks of the exact later commit. No broad old-save/client/multiplayer acceptance is implied.

## Fifth–seventh verification batches: reactor, worldgen boundaries, live menus

- Native fill simulation/commit consult fluid-sensitive capacity, matching the original dispatch; ordinary tanks retain default bounds. Reactor-local occupied Steam capacity reports10,240,000L. Existing output exports once before queued world-END cooling, preserving genuine blocked-output rod-loss policy and stopped residual cooling. Tests cover both registered core sizes,401L distilled water→64,160L Steam, actual queue drain, valid/full/wrong/no sinks and saved tank serialization. Independent process restart is not claimed. Generic add and capacity key format are unchanged; differing fluid/type APIs alone do not establish an ordinary GT still-fluid key bug.
- Origin road obstruction classification includes explicit vanilla log/plank/wooden-component tags, not an exhaustive substitute for every legacy wooden material. Native block-state classification checks preserve separate block identity and leaf/stone controls.
- Water/pit column methods report placement only for successful writes. The first encountered water and eligible-attempt pit-depth decisions remain unchanged, even on failure. No deeper source promotion, new retry, generation probability, origin exclusion, spring dispatch or fluid-motion change. Two tests inject rejected setBlock results through an explicitly labeled delegating writer over native reads; they are fault-contract tests, not reports of ordinary-world refusal.
- Garbage bin/recycler server menus require the live original owner, same level and distance at most8 blocks. Generic/client constructors and extraction policies remain unchanged. Native tests call actual provider/menu validity for near/boundary/far/removed/destroyed/replaced/cross-level cases; packet-driven automatic closure is a separate boundary. No inventory-size normalization or oversized-save truncation: ordinary missing tags were verified not to cause the initially suspected zero-slot defect.

Nine added cases per loader make required selected totals Forge47 / NeoForge43. These patches remain dependent on the exact commit's native build/test results. Later evidence must not be inferred from an earlier green batch.

## Review correction: powder-snow container placement

Adversarial review identified a regression in this audit's earlier builder useOn change: vanilla SolidBucketItem mutates the real context hand despite receiving a copied placement stack. Official1.20.1/1.21.1 server bytecode confirms survival substitutes an empty bucket;1.21.1 creative also assigns the synthetic context stack. This could replace the held wand.

The follow-up handles only the exact vanilla powder-snow bucket/native replacements. It retains the source inventory slot, restores the original wand in finally after the known hand replacement, and commits the returned empty bucket to the depleted source slot only on successful survival placement. Canceled manufactured containers are discarded; creative stock/wear is unchanged. No pre-event hand swap or manual replacement of loader protection/snapshot code. Arbitrary different listener/mod hand replacements are not overwritten; an external default-bucket replacement indistinguishable from vanilla remains a compatibility boundary.

Four additional native tests per loader exercise main/offhand × survival/creative, canceled then accepted placement, full inventory, exact stock/container/wand identity/wear and no loose extras. Required totals are Forge51 / NeoForge47. The earlier90-case green runs do not cover this new special-container correction; the exact follow-up commit must pass its98 selected cases before it is considered verified.
