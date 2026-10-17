# Stonecutter pilot (paused)

Branch: `experiment/stonecutter-emi`, based on `93ee75f68`.

Stonecutter 0.7.11 is pinned to keep the existing Gradle 8.8 toolchain. The official
0.8/0.9 branch entrypoints require Gradle 9. References were read from the official
GitHub mirror (`0.7` commit `fffcb7151d83bf6e40f3631131143fb08c06f230`, LGPL-3.0).
No upstream implementation was copied into this project.

On 2026-10-17 both version nodes configured and ran their prepare tasks successfully
in 1m1s. Generate tasks reported **NO-SOURCE**: native EMI source migration and
connection to platform compilation have **not** been implemented or validated.
The user then requested prioritizing an EMI grouping regression; the pilot was
paused here. This scaffold must not be represented as a completed migration.

Planned scope: 11 native EMI classes, 8 identical pairs and 3 pairs with item/fluid
API differences. Keep pure Java core, native loader builds and resources in place.
The nodes will provide processed sources; they are not independently compilable
mods. Do not merge this scaffold into main before source wiring and dual-platform
validation are complete.

Setup log: `work/stonecutter-20261017/setup.log`.
