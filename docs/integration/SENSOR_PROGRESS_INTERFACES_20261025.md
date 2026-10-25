# Progress interfaces and bulk-storage weight — 2026-10-25

## Scope and source contracts

This bounded follow-up starts from main `7a42fca`, after the casing and first sensor-interface repair. It addresses missing host progress providers and their sensor/cover/relay routing, plus bulk-storage weight. It does not claim complete sensor parity.

Read-only references:

- [Original GT6 `3703e40308c8c030763fd6297dea8b210d2a77b1`](https://github.com/GregTech6/gregtech6/tree/3703e40308c8c030763fd6297dea8b210d2a77b1).
- [Higher-version reference `607771f74acb0fe647f7e9412676e72094799545`](https://github.com/wolfram0108/gregtech6_w/tree/607771f74acb0fe647f7e9412676e72094799545).

Both references agree on these raw progress contracts:

- Mass storage: full stored count / nominal base capacity, not the legal automation stack or the extra overflow allowance. Original `MultiTileEntityMassStorage:553–554`; higher-version `:565–566`.
- Sealed barrels: fermentation time / required fermentation time, not fluid occupancy. Original `TileEntityBase08Barrel:278–279`; higher-version `:288–289`.
- Fluid pipes: existing transferred-volume counter / per-channel capacity × channel count. Original `MultiTileEntityPipeFluid:516–517`; higher-version `:537–538`. Native leakage contributes to this counter; priority vanilla-cauldron drainage does not in the source.
- Electric wires: current transfer-interval amperes / rated amperage, not wattage. Original `MultiTileEntityWireElectric:232–233`; higher-version `:236–237`.
- Axles: current transfer-interval power / rated power, not rotational energy. Original `MultiTileEntityAxle:166–167`; higher-version `:167–168`.
- ZPM dechargers: working buffer / **41,943,040** (`131072 × 320 × one slot`), not the artifact's two-trillion-unit charge. Both original dechargers inherit `TileEntityBase10EnergyBatBox:215–216`; original registration `Loader_MultiTileEntities:1000–1001` supplies the input and inventory size.

The repair exposes existing state and does not change transfer-counter reset times, energy transactions, fluid distribution, or fermentation timing. Bulk weight reads the full stored template count even while packed. Original weightometer inventory enumeration does not include the separately stored fractional `partialUnits`; those fractions become measurable when they complete an inventory item. Existing full-count Itemometer behavior and automation stack limits remain unchanged.

## Interface and routing choices

Five passive hosts implement the existing `ProgressSensorSource`. ZPM retains its existing switchable control and adds buffer progress. A shared read-only resolver supplies source-first observation to sensors, cover fallback, and extender progress operations.

Passive hosts do not become global `MachineControl.Provider` objects. This avoids inventing switch/running capabilities and avoids changing the scanner's existing state sections or charges. Real machine controls retain their switch/mode behavior.

Extender and bridge progress retains existing side mapping, cycle protection, and loaded-chunk checks. Lazy value/maximum access avoids recursively reading both measurements at every relay hop. Progress covers retain the original quantisation: half progress produces **7**, or **8** when inverted.

## Completed targeted native validation

The targeted group contains nine cases on each loader:

- **Baseline:** Forge **9 semantic failures**, NeoForge **9 semantic failures**. Nine production files were restored to `7a42fca`; only the new read-only resolver remained as diagnostic test support. No baseline production code referenced it, and the actual host-provider/routing omissions remained. This is a documented diagnostic seam, not a claim of an untouched baseline binary. An instrumented test endpoint explicitly implements the pre-existing progress interface so the baseline fixture compiles without adding that interface to production barrels.
- **Fixed:** Forge **9/9 PASS**, NeoForge **9/9 PASS**, **18/18 total**, verified by exact native XML identities and failure counts.

Executed assertions include:

- Native bulk insertion, overflow, tape packing, unsealing, extraction, full-count progress and existing Itemometer preservation.
- Actual sealed-barrel fermentation advancing to 128/1,024, immediate saved-data observation, explicit unseal reset, and inert fluid reporting no fermentation progress.
- Real pipe-to-drum delivery and conservation, blocked/reset intervals, source cauldron exception, and benign nonproof gas leakage.
- Real wire-to-laser and axle-to-rotation-engine transactions at different packet sizes/speeds, distinguishing amperes/power from energy.
- Both ZPM dechargers performing native refill/emission through `EnergyTransfer` into a typed, sided packet-ledger endpoint; stop/resume behavior, buffer persistence and saved-buffer bounds.
- Actual cover attachment/configuration/output/removal, and unchanged scanner text and returned **512-unit** charge for the tested passive drum.
- Native extender/bridge routing, side-sensitive endpoint instrumentation, four-hop lazy getter counts, cycle recovery, unsupported relay variants, and existing ZPM switch forwarding.
- Bulk weight at 256 stored iron blocks, packed reads, legal 64-item extraction, and fractional-unit completion.

Each test restores its owned 343-cell fixture and removes only newly generated item entities.

### Transparent test corrections

An initial compile failure was a missing test import, not a product failure. The baseline evidence checker initially assumed old progress-cover attachment would fail; old code actually attached and measured fluid occupancy. Its expected failure classification was corrected to the observed wrong-progress assertion without weakening the native test.

The first fixed run passed eight cases and failed a mistaken raw-weight oracle: the test assumed iron density **7.875**, but original `MT.java:414` specifies **7.874**, stored as `7.874F` in the port; original `OP.java:351` defines a solid block as `9U`. Diagnostic evidence confirmed 256 `gt6ce:block_solid_iron` items, per-item mass `7874.000064605248` kg and total **2015744.0165389434 kg**. The independent expected literal was corrected to `2015744.016539` with a tighter `0.000001` tolerance and explicit count/template/density/amount witnesses. Integer display, packing, extraction and fraction assertions were unchanged. No production change was made to accommodate this test correction.

## Full validation status

**Final local validation passed on unchanged runtime/test sources.** Forge executed and passed all 484 required cases; NeoForge all 475, for **959/959**. The admission list now also requires 23 existing cases per loader that previously executed without explicit presence checks; all prior 941 identities are preserved. Both builds, core/source checks, 309 prescribed CI Python tests, 491 REMI checks, sediment registration, mod packaging and SDK closure passed. The 18 targeted cases and diagnostic baselines are separate evidence. Hosted CI for this new commit is pending at publication.

## Explicit remaining limits

- A separate barrel lifecycle defect remains unfixed here: after load, the recipe-cache resolution path resets saved fermentation work. The targeted case verifies the immediate saved observation, not resumed work after the next tick. Exact recipe-change/legacy-save policy requires a separate repair.
- The unloaded relay check uses a detached native relay with the real server level and no outside-fixture writes. It proves that lookup does not force-load the absent target, not placed cross-chunk reload or crash persistence.
- Scanner validation checks returned scan charge and diagnostic text, not an actual scanner-item battery debit. Native weight/temperature/progress scanner sections remain separate work.
- The packet receiver and side-sensitive barrel endpoint are explicit test instrumentation; they are not claims of additional production-machine parity.
- Full item-pipe progress/network alignment, fluid-display-only item roles, all storage subclasses, natural tick-order sampling cadence, and visual/client behavior are outside this patch.
