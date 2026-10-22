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
