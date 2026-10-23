# P3 — Native grass soils

Use source1 actual7 non-spreading plant-soil grass variants and3 ordinary aged variants, with exact10-ID/side/top catalog shared by both factories. Native Silk Touch uses existing registry-aware OreHarvest; native soil hook adapts explicit water-adjacent sugar-cane gate. No duplicate grass registry or placeholder substituted for the colored grass class. Source2/3 spring indicator designs remain recorded in native-fluid-springs-integration.json, and their alternate generation policies are deferred.

- Native PlantType replacement recognizes BushBlock plains and water-adjacent sugar cane; other-mod custom soil types remain deferred.
- Colored grass has silk-touch self drop/otherwise dirt and no spread, while3 dry/moldy/rotten variants remain ordinary source icon blocks.
- Spring indicator helper now resolves blocks but source worldgen still never calls it.
- These10 registrations/resources are compile-covered only, not covered by preceding server startup; actual drops/soil/client/reload unverified.

Grouped dual compilation passed 19s. D010 no extra startup/fixture. The overall port is still incomplete.
