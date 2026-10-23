# Signal-wire torch and repeater cover behavior

Status (2026-10-23): dual-loader baseline established; candidate applied, fixed validation pending. This draft does not claim a passing 453-case release gate.

## Original contract and provenance

Read-only references:
- GregTech6/gregtech6 commit `3703e40308c8c030763fd6297dea8b210d2a77b1`.
- wolfram0108/gregtech6_w commit `607771f74acb0fe647f7e9412676e72094799545`.

Original paths are beneath `src/main/java/`:

| Source | Contract retained |
| --- | --- |
| `gregapi/cover/covers/CoverRedstoneTorch.java:41-43` | Invert the host wire's positive/nonpositive signal. |
| `gregapi/cover/covers/CoverRedstoneRepeater.java:41-43` | Threshold buffer: positive host signal yields 15, otherwise 0. **Not an adjustable-delay vanilla repeater.** |
| `gregapi/cover/covers/AbstractCoverAttachmentTorch.java:35-40,49-70` | Close/intercept the attachment's connector face; emit weak and strong 15/0 from stored post-tick visuals. No added delay configuration or vanilla locking. |
| `gregapi/cover/CoverData.java:147-153` | Visual transitions synchronize and request neighbor updates. |
| `gregapi/tileentity/base/TileEntityBase06Covers.java:405-415` | Cover output overrides ordinary wire output on the queried physical face. |
| `gregapi/tileentity/connectors/MultiTileEntityWireRedstoneInsulated.java:108-118,162-166` | Adjacent GT wires use connected-wire protocol; ordinary blocks use vanilla incoming signal. A disconnected attachment does not directly bridge two GT wires. |
| `gregapi/block/multitileentity/MultiTileEntityBlock.java:231,271` | Generic nonnormal source block remains a vanilla redstone connection source. Closing the GT wire port does not remove the cover's vanilla output identity. |

The `gregtech6_w` torch/repeater implementations are identical; its abstract attachment changes porting headers/imports, not this signal logic. Bare wire inherits the original insulated superclass contract. Earlier attachment eligibility, explicit/manual port state and geometry fixes are retained.

## Confirmed baseline and bounded changes

The same three frozen tests ran on Forge 1.20.1 and NeoForge 1.21.1:

1. **Solid-block relay failed.** Native RedAlloy bare/cable, both covers, all six output faces and five input transitions. Cover weak output, strong output and the adjacent stone's world signal were correct (15 for active rows), but a separate receiving wire stayed at 0 instead of `15 * UNIT - loss`. `SignalWireBlockEntity.output` suppressed the stored cover output during network source sampling. The change permits only torch/repeater stored outputs during sampling; uncovered wire and other cover suppression remains intact.
2. **Dust connection gate failed.** Both covers on bare/cable, four horizontal faces, three input transitions. Native dust power and the isolated dust's arm were already correct; only `canConnectRedstone` returned false. Do not characterize this baseline as broken dust power or missing arms. `SignalWireBlock.canConnectRedstone` now recognizes these attachments without reopening their GT connector port.
3. **Controls passed before the fix.** Uncovered stone/dust feedback clears; a covered face rejects incoming power; a closed cover face does not create a direct wire-to-wire bridge. The same assertions remain in the fixed run.

A separate static renderer correction selects the attachment's lit texture using synchronized blockstate `POWER > 0` instead of unsynchronized server-only `wire.signal()`. No cover geometry or texture asset is changed.

Baseline evidence: `work/validation/cover-signal-removal-baseline/{forge,neoforge}-baseline.xml`.
- Forge XML SHA256: `d960a1bfb340ca06879611b72d23596321f74228ae64cd90cac653cca80b8e30`.
- NeoForge XML SHA256: `86510d337af7be512e53916971fab31fc48b6177843446b8e83c1ab79ea07909`.
- Combined baseline: Forge 227/231 passed; NeoForge 218/222 passed. Each had these two signal failures plus two independent cover-removal failures. No missing/skipped cases; previous 441 cases passed. These are expected-failure baseline results, **not a successful 453-case gate**.

## Verification boundary

`SignalCoverBehaviorTests` uses registered native blocks, vanilla item-first cover/dust placement, real redstone block inputs, actual solid conductors and neighbor updates. It explicitly drives the registered block-entity ticker and fallback cover lifecycle at isolated saved/restored world positions; it never substitutes direct `panels.afterTick()` assertions for a circuit.

It does **not** establish natural scheduled world-tick/chunk-ticket timing, vanilla repeater delay parity, or client GPU appearance. The renderer correction is source/state-path reasoning, not screenshot/GPU proof. An optional fourth L-junction dust-arm test remains an ignored future case and is not included in the frozen three-test comparison or 453-case count.

Final fixed gate passed: Forge231/231 and NeoForge222/222, dual builds/core,137 Python guards,491 REMI groups,1023 shared packaged classes per loader and SDK112 core/46 native. Evidence: work/validation/cover-signal-removal-fixed-retry/local-validation-receipt.md. Input/final snapshots match; remote CI is separately required.
