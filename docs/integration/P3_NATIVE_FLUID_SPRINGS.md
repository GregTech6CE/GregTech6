# P3 — Native fluid springs

Retain source1 identities, spring/amount/active NBT,7 spring rows, full7-layer crater and actual spring block/ticker over existing fluid/material registries. Share full crater executor and probability helpers in core and consume them from both platforms. Adopt masson dirty-on-activation and positive loaded amount on both platforms. Keep source1 valid amount behavior and NBT identity; defer different finite-fluid emission and worldgen claim policies rather than import a disconnected second fluid registry.

- Source1 chooses one of7 rows before probability; other projects use independent per-row gates.
- Source1 accepts a bedrock ore at center and does not enforce ore/spring claim exclusion; masson checks whole floor and brokestar replays coordinate ore gates.
- Source1 indicators helper is not called by place; indicator behavior remains incomplete.
- Emitter creates a full source above, without finite-volume increase/horizontal spread of brokestar/masson.
- No fluid-specific nozzle rendering; invalid legacy fluid ID can still fail parse; no old-save compatibility proven.
- Actual generation, fluid extraction, gameplay, client rendering, Forge runtime, independent reload, old saves and new production jars unverified.

Dual compile passed19s. One grouped ordinary Neo startup covers this and the preceding mineral/black-sand/Nether waves; startup outcome recorded separately. The overall port is still incomplete.
