# P3 — Native pit, peat, clay, water and rock generation

Keep complete source1 overworld resource-generation execution on the existing shared material/fluid/rock registry. Move exact48x48 original pit mask into core, together with5 clay pit identities, pit/peat/seam numeric rules and litter RNG choice; both actual Forge and native executors consume these rules. No separate source2/source3 ore/material/world registry imported. Prior source audits retain other-world implementations as candidates; this wave does not claim a tested replacement or that source1 universally best.

Actual gt_pits, gt_turf, gt_surface_deposits, gt_water_bodies and gt_rocks feature registrations, configured/placed features and native biome modifiers are connected. Source sea-level windows, mask anchors, sediment/rock/wood gates, host replacement, fluid conversion priority and flint/meteoric material rock contents are retained.15 resource records keep original data bytes or change only native biome modifier type.

- Actual newly generated terrain, pit cross-chunk mask writes, fluid conversion and source survival resource access remain unverified.
- 5 clay pit rows exclude original default-disabled red clay and optional PFAA entries; source1 seams intentionally expose top clay unlike original GT6 grass cap.
- Water-body passes use source ocean then river excluding ocean then swamp priority; fluid block availability depends on existing native fluid registration.
- Source pit soil replacement and water conversion can affect chunk contents; no old-world replay/remapping promised.
- Nether deposits/crystal blocks, bedrock/deep-ocean/black-sand/coltan/fluid springs/dungeons and remaining source features are not claimed complete.
- Full gameplay/client/Forge runtime/independent world restart/old saves/new jars pending.

Grouped core/Neo/Forge compile passed19s. One grouped ordinary Neo200tick start covers bee, surface life and terrain registration/data; its result recorded separately. No extra fixture. Sources untouched and The overall port is still incomplete.

Grouped ordinary Neo startup passed1m59s after core archive ZIP64 entry-limit repair.960 bee display rows,80 crop processing rows,2 bumble crafting rows and6421total crafting recipes loaded; DedicatedServer200ticks normally saved/stopped. Receipt:verification/p3-neoforge-bumble-surface-terrain-startup.json. No retry for nonblocking Mojang key fetch timeout. Actual gameplay/client terrain generation/Forge/restart/old saves/new jars remain unverified.
