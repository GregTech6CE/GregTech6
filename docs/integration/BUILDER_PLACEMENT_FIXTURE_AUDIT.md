# Builder placement: owned scan-plane fixtures

2026-10-24. **Both modified cases passed on both loaders in the full local663 fixed gate.**
This test-only correction was not part of the earlier649-case run or its
`6135f4b83c3a32948cc2551471ce0f1fa6b7afb7` publication.
No production tool/placement behavior changes and no new GameTest identities.

## Source-proven risks, not observed CI failures

The two affected methods are `builderUsesHeadQualityAndHitCoordinates` and
`builderCancellationRollsBackWorldStockAndWear` in `ToolPlacementTransactionTests`.
Both used the native `test_empty` template (verified NBT size 1×1×1), but read, wrote and
cleaned local `(4,20,4)` and its surroundings, outside that template. Downloaded prior
Forge/NeoForge XML passed these two cases; the observed remote placement failure was
the separate drill-torch fixture. Do not relabel these risks as observed CI failures.

Actual `GTMaterial.setToolStats(..., quality)` and generated material definitions give
Wood quality 0 and Steel quality 2. `BehaviorBuilderWand` uses `max(0, quality+1)`: Wood
scans 9 source cells, Steel 49, on the plane perpendicular to the clicked face.

- Quality test prepared only 2/3 matching source cells inside those respective planes,
  leaving 7/46 uncontrolled. An additional natural STONE at relative `(-1,0,0)` with
  a placeable block above adds a copy and violates the expected 6/5 inventory counts.
- Cancellation test prepared only one of 49 EAST-plane source cells. An additional
  STONE at relative `(0,-1,0)` with a placeable eastern destination adds another canceled
  placement event, violating the expected first event count of one.
- Both used `removeBlock` as though it guaranteed AIR. Actual mapped native `Level`
  code replaces a removed block with its fluid's legacy block; water/lava can remain.
  A wet outside-range guard or cancellation destination invalidates the air assertions
  without any production placement defect.

## Derived owned volume

`runInOwnedBuilderFixture` derives the maximum radius from the actual Wood/Steel wand
heads. For current radius 3, the dedicated `builder_placement_empty` NBT is **11×10×10**:

| Geometry | Local coordinates |
|---|---|
| Support origin | `(5,5,5)` = `(radius+2)` on each axis |
| Owned chamber, inclusive | `(1,1,1)` through `(10,9,9)` |
| Sealed dry interior, inclusive | `(2,2,2)` through `(9,8,8)` |
| UP source/destination | X/Z ±3, Y 0/+1 from origin |
| EAST source/destination | Y/Z ±3, X 0/+1 from origin |
| Explicit outside-range quality guard | X +4, Y 0/+1 from origin |

Size is `(2r+5, 2r+4, 2r+4)`, not an arbitrary enlarged cube. The extra X extent covers
that +4 guard and its shell margin. The shell never overlaps a scanned source plane.
Every chamber cell is asserted inside the actual template and away from the structure
controller **before** readiness waits or any world mutation. A future radius increase
that no longer fits fails ownership rather than writing beyond the fixture.

Only loaded/entity-ticking readiness retries. Sealing, precondition controls and each
original transaction body execute once. Setup seeds extra matching source cells and
native WATER/LAVA witnesses; native `removeBlock` first proves the liquids survive,
then explicit AIR clears and verifies every interior cell before interactions. Ambient
setup drops are discarded only before the body, never during/after transaction assertions.
All cleanup stays in the owned chamber. Players are repositioned near the support;
click faces and local hit fractions 1/.8/.5 are unchanged.

All original gameplay assertions remain verbatim: Wood/Steel radius and stock counts,
outside-edge exclusion, TOP slab hit geometry, cancellation/accepted/blocked/creative
event counts, inventory and wear, and no loose refund. `builderSixFacesCommitExactlyOnce`
(including its existing far bounded snapshot) and the complete drill-torch fixture remain
byte-for-byte unchanged. Exact namespace bindings change only these two existing cases
to `gregtech_issue_audit:builder_placement_empty`; both native resource paths are supplied.

## Verification boundary

- Both modified native test projections compile against cached Forge/JDK 17 and
  NeoForge/JDK 21 APIs, without Gradle.
- 34 Python evidence tests pass, including rejection of old/wrong template namespaces,
  missing cases, failures, errors and skips for both affected identities.
- Source-derived geometric enumeration covers all Wood9/Steel49 UP/EAST source and
  destination cells plus the outside-range guard. Original assertions/native clicks,
  SixFaces/torch byte identity, one-shot body and readiness-only retry are checked.
- Independent review and isolated temporary-index application verify the intended paths
  and candidate bytes; detailed receipts remain under `work/builder-owned-fixture-draft/`.
- Preparatory synthetic reports used336 Forge /327 NeoForge; those reports were not actual game
  runs. All 663 identities and their existing templates are preserved except the two intended
  builder namespace changes; the quality/physics roster is unchanged.

The full local663 retry passed: Forge336/336 in8m50s, NeoForge327/327 in4m32s, native and strict checker exits0 with no failures/errors/skips. Both modified builder cases passed with their owned-template bindings. Input/final snapshots were byte-identical. The first fixed attempt had stopped before native execution at localization provenance; the separately scoped four-file project-translation provenance correction allowed the fresh retry to complete, without weakening builder assertions.

Evidence: `work/validation/ore-quality-physics-fixed-retry/local-validation-receipt.md`; full results and XML hashes are in the [quality/physics audit](ORE_QUALITY_HOST_PHYSICS_AUDIT.md). Cached/geometric evidence above remains historical preparation, not a substitute for this native result. These were proactive fixture risks, not previously observed remote builder failures. No remote-CI or merge result is claimed.
