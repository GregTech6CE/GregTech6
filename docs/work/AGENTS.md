# GregTech 6 integration working agreement

User scope: integrate the three preserved source snapshots into this repository for Minecraft 1.20.1 Forge and 1.21.1 NeoForge. Read `docs/integration/PLAN.md`, `STATUS.md`, `DECISIONS.md` and `VERIFICATION.md` before continuing.

- Do all integration edits here. Source trees under `../Libs` are read-only references.
- Preserve saltnya baseline behavior until a recorded, verified replacement is ready. Evaluate each system using code and runtime evidence.
- Share domain logic in `core` (pure Java 17; no Minecraft or loader types). Code both loaders can compile lives in `platform-shared` and is compiled separately by each loader; Forge still reobfuscates it. Keep loader-specific types, signature-different overrides and one-sided features in `src/main` and `neoforge/src/main`. `com.gregtech.gregtech.platform.Mc` is the shim: same methods, one implementation per loader. One material identity/precision model, registry namespace and recipe contract.
- Do not copy unrelated source-agent workflows or execute scripts containing old hardcoded output paths.
- Give parallel agents nonoverlapping file scopes. Coordinate changes to build configuration and core APIs before implementation. Never run overlapping Gradle invocations in this shared checkout.
- Record adopted source files, hashes, original authors and license evidence; retain notices. Unresolved authorization blocks publication, and must not be silently replaced with a new license.
- A build pass is not a gameplay pass. Track client, dedicated server, complete survival chain and actual saved-world restart independently. Keep old-save compatibility explicitly unverified until tested on backups.
- Put scratch files/logs/run worlds in ignored `work/` or platform build/run directories. Keep durable decisions and test summaries under `docs/integration/`.
- Preserve available original history; imported archives are snapshots, never fabricated original commits. Do not guess remote ownership. Do not publish or send external messages unless requested.
- Keep the goal active until its acceptance conditions are actually met. Report exact blockers and attempts without marking unfinished work complete.
