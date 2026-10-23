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
