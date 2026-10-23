# Sediment, manual crank, bottle contents and pile targeting

## Source-backed changes

- Sediment: original `WorldgenPit` and `WorldgenBlackSand` replace explicit soil hosts, not the entire modern dirt tag. Preserve grass caps and original allowed bare sand/dirt behavior; do not impose a blanket underwater rule. Remove only the port-only surface-deposit biome attachment on both loaders, retaining configured/placed feature identities. Existing terrain is not rewritten. Source and final-JAR guards reject stale extra attachments and require compatibility IDs.
- Crank: original `MultiTileEntityCrank` and `UT.getPlayersWithLastTarget` use player last-target eligibility, not a ten-tick timer or fixed twenty-second lifetime. Emit on subsequent ticks while an eligible player remains alive, in the same world, within six blocks (creative bypasses range), and still targets the crank. Reclicks do not emit immediate energy or add countdown time; full/rejecting receivers do not stop motion. Preserve per-player source output and bounded weak tracking/logout cleanup. Empty-air clicks lacking a modern server event and villager operation remain separate parity limits.
- Bottles: resolve original material expressions such as Hg to the actual ordinary fluid instead of silently fabricating water. Restore five source-defined golden drink fluids: golden apple juice, golden cider, Idun's apple juice, Notch's brew and golden carrot juice. All169 generated bottle identities now resolve. Preserve native capability payloads, potion colors and intentional potion water base; recognize admitted honey/dragon-breath bottles. Unknown bottles display no fictional water. This does not change the port's250mB bottle quantity to original Mercury144mB, or implement source poison/nutrition semantics.
- Piles: shared cover-grid shortcut for owned blocks bypassed the existing pile exclusion. Explicitly reject loose piles in that predicate, retaining stacking/retrieval and legitimate tool/cover selection. Physical collision is unchanged.

## Native validation

Unchanged-production533 baseline: Forge271 cases/261 pass/10 failures; Neo262/252 pass/same10 failures. Prior511 pass; the new legitimate-grid control passes. Failures are2 sediment,2 bottle,5 crank and1 loose-pile cases.

Fixed gate535 passes: Forge272/272 and Neo263/263, zero failures/errors/skips; both builds/core,139 Python guards,491 REMI groups,1029 shared packaged classes per loader and SDK112core/46native pass. Input/final snapshots match. Evidence: `work/validation/sediment-crank-fixed/local-validation-receipt.md`.

The extra fixed-only BottleContents test covers native honey/dragon breath/XP admission, normal/splash/lingering potion payloads, unknown/empty behavior and actual portable-fluid capability payload preservation without draining. It is not represented as an unchanged baseline case.

Crank tests use real interaction and scheduled world ticks, dedicated templates with readiness checks, and500-tick observations. Only readiness is retried; gameplay assertions are not retried into success. Existing old crank assertions requiring immediate click-time energy were corrected to source timing, with baseline sources retained.

Sediment tests invoke actual feature `place()` over bounded native columns with a WorldGenLevel proxy; they do not establish arbitrary natural-world seed/chunk generation. Both final jars additionally pass explicit deleted-resource/retained-ID checks. Obsolete generated copies were removed only from build outputs before rebuilding.

## Evidence limits

Bottle identity/sprite-policy and pile targeting predicates are native tested; no client GPU/framebuffer or screenshot result is claimed. Third-party integration, existing-world terrain repair, full crank entity compatibility, and original bottle quantities/effects remain outside this batch. Reference trees are read-only and upstream source licensing is retained.
