# P3 — Native black sand and Nether deposits

Keep source1 full falling sand/material/dust identity and river-only48x48 pit-mask host rules, on the existing shared three-sand catalog. Native SandBlock removal is adapted to concrete FallingBlock with original dust-color/own-block drop behavior. Reuse already registered12 actual crystal ores and original dense rock/quartz ores, without duplicate registrations. Both platforms now use one exact12-crystal catalog/quartz noise layer data and complete1500-step one-neighbor crystal grow executor. brokestar offers air-below-ceiling seed and explicit dimension-seeded bounds but different seed/anchor behavior; keep source1 baseline pending recorded comparison, rather than pretend source1 universally best. No matching masson filename implementation is claimed adopted in this wave.

Actual gt_black_sand and gt_nether_deposits registrations,15 source resource records and native singular sand/shovel tags connected. Source yellow/red/blue clays use already registered surface identities; Nether red clay and two quartz seams use real existing ore/material blocks. River biome exclusions/48x48 mask, quartz fixed noise heights, natural-ceiling gate and crystal one-neighbor walk retained. No duplicate crystal or rock-ore family introduced.

- New sand registrations/datapack rows not yet runtime-covered; the previous bee startup predates this batch.
- Actual river deposits/gravity/shovel drops/material composition and Nether clay/quartz/crystal generation unverified.
- Source1 crystal seed replaces ceiling itself; brokestar seeds air below ceiling, and uses coordinate/dimension seeded stream with explicit bounds. Source1 kept pending source-fidelity/runtime comparison.
- Source1 crystal walk has no explicit vertical-bound guard and noise cache is static unbounded by world lifetime.
- Source1 Nether feature relies on native biome modifier for dimension gating; direct invocation has no dimension check.
- Full survival/client rendering/Forge runtime/reload/old saves/fresh jars pending.

Grouped core/Neo/Forge compile passed19s; no new fixture or individual startup. All source trees unchanged and The overall port is still incomplete.
