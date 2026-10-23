# P3 — Native surface life

Use source1 complete8-berry table/growth/harvest/packing and exact9 diggable identities plus4 fallen-log tools, keeping current material IDs and units. brokestar wild bush explicitly cuts berry NBT/growth/harvest and is decorative, so it cannot replace the complete controller. masson stores full berry ItemStack, accepts tags/vanilla berries and resets growth on harvest, but shown tick increments without source1 rainfall/light bonuses and its placement biome set differs. Keep baseline behavior and document stronger compatibility candidates. Share exact growth math and source flora biome/count/probability/color catalog across both versions; Minecraft items/block resolution remains at platform boundary.

14 real surface blocks: berry bush, seven original diggable soils/clays plus two legacy aliases, four fallen logs. Complete original128-tick/256-overflow growth with rain/light bonuses, stage colors, harvest, item berry packing, falling loose soils/stationary clays/4 exact material drops/slowing and log axe/saw/knife conversion cost1000 are connected. Native berry custom data uses BLOCK_ENTITY_DATA and registry-aware BE storage.8 berry colors, growth math and four fallen-log/flora biome/probability/color catalog are shared and Forge actually uses them. Native gt_bushes and gt_surface_flora features now spawn real blocks;55 resource records retain exact bytes or adapt composite/biome modifier loader boundaries.

- Actual harvest/growth/berry placement/packing and worldgen/client rendering not yet verified.
- Native plant sustain hook rejects clay, accepts vanilla bush/sugarcane and otherwise delegates DEFAULT; old Forge PlantType categories have no direct native equivalent.
- Source8 specific berry identities preserved; masson broader tag/vanilla berry setters and resetting growth on harvest are candidates, not silently adopted.
- Source bush4-side scatter is a single-block adaptation, no original connected cluster sides; enhanced aether ground growth absent.
- Shared source glowtus gate includes swamps; masson only jungle, neither blanket-preferred.
- Original source log-shape placement can overwrite neighboring blocks; generated-source chunks and compatibility still unverified.
- Dedicated-server startup for these two batches deferred to one grouped startup; Forge runtime, full survival, independent reload, old saves and fresh jars pending.

Grouped core/Neo/Forge compile passed20s after native ResourceLocation import repaired; no new fixture/individual startup. Source trees unchanged; The overall port is still incomplete.

Grouped ordinary Neo startup passed1m59s after core archive ZIP64 entry-limit repair.960 bee display rows,80 crop processing rows,2 bumble crafting rows and6421total crafting recipes loaded; DedicatedServer200ticks normally saved/stopped. Receipt:verification/p3-neoforge-bumble-surface-terrain-startup.json. No retry for nonblocking Mojang key fetch timeout. Actual gameplay/client terrain generation/Forge/restart/old saves/new jars remain unverified.
