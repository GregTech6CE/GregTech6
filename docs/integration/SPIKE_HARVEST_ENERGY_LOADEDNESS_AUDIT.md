# Spike lifecycle, per-state harvesting and adjacent energy loadedness

## Proven defects and corrections

- Falling Spike normal placement, scheduling, original sound event and landing already pass. Actual failed landing produced one item but lost FALLING/secondary material identity. Extend the existing falling-drop mixin only for Spike stateful output, retaining the existing ore branch byte-for-byte and all registry IDs/serializers.
- Spike source harvesting depends on the selected material, not a whole-family tag. Native matrix covers30 states ×5 vanilla tiers and real survival low/high-tool breaking. Required qualities: Copper0,Lead1,Steel2,Titanium3,Blue/Red Steel2,Gold/Silver2,TungstenSteel4,Adamantium5. Known vanilla/GT tools receive the bounded state-aware check. Unknown tiers/interface-only tools fall through the entire old handler unchanged; installed third-party unknown-tool compatibility is not claimed runtime-tested.
- Two adjacent energy lookup paths (`GTEnergySides.getNeighbor`, rotation radial polling) now check `hasChunkAt` before `getBlockEntity`. Original `TileEntityBase01Root:97,288–291` defaults to no-load; original long-distance transformer deliberately opts out. Modern intentional long-distance BFS is untouched. Native tests reproduce and fix isolated FULL-border reads, with positive loaded neighbors, signs, conversion/waste and active-emission controls. They do not prove normally ticking machines encounter naturally absent neighbors or establish full unload/restart behavior.

## Verification

Corrected573 baseline: Forge286/291 and Neo277/282 pass; same five failures (one falling identity, two harvest, two energy). Previous561 and new normal-placement control pass. Fixed573: Forge291/291 and Neo282/282 pass, zero failures/errors/skips. Full builds/core,142 Python checks,491REMI groups,1033 shared packaged classes per loader and SDK112core/46native pass. Input/final snapshots match. Receipt: `work/validation/spike-harvest-energy-fixed/local-validation-receipt.md`.

Both fixed native matrices fully execute:30 placement state/sound cases,150 vanilla harvest queries, real failed-fall state retention and survival drop controls. Source licensing retained; no original reference tree edited.

## Fixture history and limitations

Earlier failing-drop item count was intermittent because items could drift off a one-cell slab before the delayed query. A template-owned catch floor and whole-template query retain the exact-one requirement and unchanged failure slab/variant assertion. Both corrected baselines reach the identity failure; no production change was used to hide count failures.

An old long-running crank test failed because the normal-noise test world contained a natural source-water column above its open fixture. Saved-world evidence showed the crank replaced by flowing water while mounting stones survived. A sealed, template-owned chamber fixes the test environment; all500-tick/operator/energy assertions remain. No crank production behavior was changed. One diagnostic run was interrupted by environment restart and has no terminal result; its partial evidence is preserved.

Sediment tests previously wrote40 native cells outside their one-cell template. They now reserve6×16×6, check loaded/pristine cells and restore all state in finally; exact feature assertions remain. This isolation defect is independent of, and was not blamed for, the proven natural-water crank failure.

No GPU screenshot, old-world migration, arbitrary external tool installation, or two-JVM restart claim is made. Failure-baseline and interruption evidence remain separate from the final pass.
