# P3 — Native mineral world generation

Keep source1 full mineral/existing material identity baseline while actual Forge and Neo world mutations call one shared bedrock vein RNG/shape algorithm, coltan Gaussian region/count/material math, inverse rarity weighting, four pylon tiers and24 Nether litter rows. Existing core GTBedrockOres28 overworld+7 Nether rows feed native dimension boundary. brokestar has independent per-chunk row draws and original shape callback core; masson additionally preserves first-success shell fill and correct floor-to-water mapping. These are stronger original-fidelity candidates requiring recorded behavior replacement; do not import their disconnected material/ore registries or silently change current world generation policy.

Actual gt_bedrock_ores, gt_coltan, gt_deep_ocean and gt_nether_scatter registrations plus13 configured/placed/native modifier records are connected. Complete35-row bedrock/indicator flower/rock paths, central coltan bedrock vein plus small and large scatters, prismarine pylon/ore-host geometry and ground-dependent quartz/glowstone/debris/gloomstone/flint litter are retained on existing registries.

- Native registration/datapack startup of this batch and real newly generated terrain unverified; preceding bee startup does not cover this wave.
- Source1 picks one weighted row on16x16chunk grid, whereas brokestar/masson roll each catalog row independently per chunk. Source1 baseline retained, original-fidelity alternative recorded rather than declaring it best.
- Source1 small bedrock ore uses same large ore placement; muffin host shell is not explicitly filled and forced-center placement is not included in return flag.
- Source1 tail upper bound is seaLevel used as relative height to minBuildHeight, unlike masson absolute water-level conversion; negative-floor reach can stop below actual sea level.
- Source1 coltan distance uses int-square arithmetic; extreme coordinates can overflow; exactly one gaussian center is baseline.
- Deep ocean biome gate is only vanilla deep_ocean; newer deep ocean variants and mod dimensions absent.
- Actual resource conservation/survival/drops/client rendering/Forge runtime/independent world reload/old saves/fresh jars pending.

Grouped compilation passed19s; no new fixture or individual startup. Sources untouched and The overall port is still incomplete.
