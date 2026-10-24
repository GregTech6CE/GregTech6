# Native drill torch placement fixture

2026-10-24. **Test-only fixture correction passed in the full 649-case dual-loader retry.** Production placement/event/refund behavior is unchanged.

## Observed remote failure and limits

Downloaded Forge artifacts for `9ce86aa7edcdedb7e95ea5802f2f7f1a54cb2f62` contain313 cases with one failure: `ToolPlacementTransactionTests.drillTorchCancellationAndCreativePreserveInventory`, message `torch event denied and world restored`. The assertion combines `listener.calls == 1` with destination-air restoration. The artifact does not identify which operand failed, or record the destination state, seed or coordinates. **The actual remote failure branch/root cause remains unknown.** The preceding full local617 gate passed; that does not establish the remote fixture state.

Local evidence: `work/validation/ore-centrifuge-remote-ci/artifacts/forge-issue-regressions-9ce86aa7edcdedb7e95ea5802f2f7f1a54cb2f62/`, particularly the actual Forge XML and `work/issues-gametest.log`. Do not substitute the previously mislabeled job-log capture for those artifacts.

The old `test_empty` template was1×1×1, but support/destination were at local(4,20,4)/(4,21,4), outside its cleared volume. Setup called `removeBlock(destination,false)` without proving an air destination. Actual mapped Forge `Level` bytecode shows that method replaces the block with `getFluidState(...).createLegacyBlock()`: water/lava may remain, rather than become air. This establishes an unsafe fixture assumption, **not proof that the remote destination actually contained fluid**. It does not justify changing production cancellation logic.

## Owned adversarial regression

Only this test moves to `gregtech_issue_audit:torch_placement_empty`, a dedicated empty9×9×9 template in each loader's native resource directory. A local1..7 chamber stays inside that template and excludes the controller. Readiness alone retries until the owned area is loaded/entity-ticking; sealing, setup and transactions then execute once. A stone boundary isolates ambient terrain/fluids, with explicit AIR clearing inside.

Three AIR/WATER/LAVA cycles each create a fresh player, drill, inventory and player-scoped placement listener. Real native `setBlock`/`removeBlock` calls first prove the seeded air/fluid is retained. Explicit AIR clearing then asserts a dry destination before interaction. This is a deterministic test of the former assumption, not a claimed reproduction of remote terrain.

All original native `useItemOn` calls and assertions remain:

- Canceled placement delivers exactly one event, restores air, retains eight torches and creates no loose refund.
- Accepted placement reaches exactly two events, places the torch and consumes exactly one item.
- Creative placement reaches exactly three events and preserves stock.
- Held drill identity and charge1000 remain unchanged.

Additional all-inventory totals assert8/7/7 torches after cancellation/accepted/creative actions, before cleanup. Ambient item entities are discarded only during initial setup; unfiltered no-refund checks remain after transactions, including the whole chamber after each cycle. Listener removal and explicit-air cleanup are bounded to the owned fixture. No transactional body is retried to conceal failures.

## Verification boundary

Both cached native API compiles passed; the29-test Python evidence suite passed with unchanged649 identities (Forge329/NeoForge320). Only this case's expected template namespace changes. Independent review approved the fixture, and temporary-index application verified exactly the Java test, checker and two intended native NBT resource paths with matching bytes. Final patch SHA-256: `9c41a22f3d77cc12288e94b64e2ad6a2ca961186ead403214f79835c9f01bad1`; local preparation/proofs are in `work/torch-placement-ci-diagnosis/`.

Compilation, synthetic reports and review are not native execution. The completed local full649 gate includes this correction and the separate stone-layer slab fixture correction alongside the [eight-profile Centrifuge expansion](SOLID_ORE_CENTRIFUGE_EXPANSION_AUDIT.md). No production placement fix or completed remote-CI recovery is claimed.

## Final local 649-case receipt

Forge 329/329 (7m39s), NeoForge 320/320 (4m10s), zero failures/errors/skips, native and strict-checker exits 0. Dual/core build, 161 Python guards, 491 REMI groups, 1039 shared packaged classes per loader and SDK 118 core + 46 native passed. Runtime/test/CI inputs remained unchanged during the gate; documentation updates are recorded separately. Evidence: `work/validation/ore-centrifuge-eight-fixed-retry/local-validation-receipt.md`. Forge XML SHA256 `924819b1dce8bf7948943a729b1d3f2a9845a1650e7a091b2bd16459c69552f8`; NeoForge `fddc4111b1b35f3fc0a0c621b99fb2d8e50c9e713865d3b26030dd659860154f`. This does not relabel the earlier remote 9ce86aa7e failure as passed.
