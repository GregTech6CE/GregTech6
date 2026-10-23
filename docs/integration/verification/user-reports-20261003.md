# 2026-10-03 user report batch

The overall port is still incomplete. Current build/runtime results are recorded separately; this is the work list.

- Water compatibility: general water family/type/tag behavior, vanilla and GT water-surface plants, waterlogged placement and container API. Native Forge and NeoForge water + tool/power suite passed 11 tests each in `work/water-woods-native3.log`; final packaged binaries still pending.
- Ordinary wood material source parsing: all 110 MT.woodnormal rows restored, including Magicwood and three qualified field collisions. 1,212 source fixture assertions and full graph differential verified; shared check passed.
- Measuring pots, capsule containers, barometer cylinders, taps: NeoForge item RGB callbacks added; actual client verification pending.
- Track inventory sprites: 30 track families and railroad now use generated flat sprites; client verification pending.
- Glass, concrete, reinforced concrete, spikes inventory display: missing vanilla block display parent restored; client verification pending.
- Bottle crate variants: 29 available-plank variants and 60 storage-metal variants registered on both platforms; existing treated-plank id retained, shared nine-slot entity type expanded, source recipes, native RGB and frame resource pack added. Runtime/client verification pending.
- Generated dungeon scaffolds/redstone connections: generation queues connection-sensitive states for native chunk postprocessing; GT scaffold shape updates schedule its support/design tick. Natural generation runtime verification pending.
- Dried/mossy wood and related natural blocks: all four Log1 kinds now have source hand/saw plank recipes, plank stick recipes, six coolant cutting routes, lathe and coke processing, and source compositions. NeoForge now uses the actual fallen-log class. Four Cinnamonwood rod rows also restored. Runtime recipe verification pending.
- Natural magnetite sands: real sand identities bound as canonical blockDust on both platforms, enabling existing crafting and box/unbox routes. NeoForge gains missing 9U compositions and native material/form tags. Runtime recipe verification pending.
- Chest placement experience: server auto-roll tick removed, following original onBlockActivated2 opening trigger. The newly launched delayed-tick regression was deliberately cancelled when the user requested fewer tests; no pass claimed.
- Invalid renderer-only iconset registrations: 12 renderer identities excluded from the generic block generator, including three gear sprites, gearbox shell/axle, crate shell, hatch, machine shell, piston sprites, rendering_error and lantern overlay. Real typed registries and sprites retained. Registration behavior only compile-verified in this batch.
- Nether/End dungeon portal purpose: room source audit confirms an unlit vanilla Nether frame and an active vanilla End portal; current room implementations already follow that. Separately, existing miniature portal blocks incorrectly teleport entities instead of relaying items/fluids/signals. That implementation remains pending and must not be reported as fixed.
- Rotational pump missing textures: source pump rotation textures recovered unchanged from the complete reference port, real layered idle shell model and item parent restored; client verification pending. Active overlay state behavior remains to review.
- EMI untranslated material tags/reload work from the previous batch: pending language resource work; do not disable warnings or claim solved.

Original references remain read-only. No issue comments or closures are authorized.

On 2026-10-04 the user requested fewer tests. Subsequent work uses one batch compilation/package check and a lightweight shared check; no repeated GameTest server or client launches. Source changes after `water-woods-native3.log` have no new gameplay pass. See `user-reports-20261004.md` for the batch receipt.
