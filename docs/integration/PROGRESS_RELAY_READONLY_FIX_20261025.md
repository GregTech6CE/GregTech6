# Read-only progress relay regression repair

## Problem and scope

The live MachineControl capability checks introduced before dc37fd66 correctly reject passive tanks, storage and pipes as switch/mode targets, but also prevented progress meters and progress panels on universal extenders from reaching their read-only ProgressSensorSource endpoints.

Add a sided ProgressSources.Provider path for lazy read-only observations. Extender observations retain the existing progressOperation endpoint-removal, loaded-chunk and cycle guards. MachineControl implementations and their live availability/switch/mode behavior are unchanged. PanelCoverRuntime uses this path only for PROGRESS panels; other panels retain their prior behavior.

The expanded existing native relay case covers real meter/panel values and inversion, missing endpoints, endpoint replacement/removal and world rebinding, bridge null-side rejection, passive control feature rejection, cycles, unloaded targets and linear lookup behavior. The removal fixture explicitly reinstalls the endpoint because native chunk lookup evicts a removed block entity; clearing a detached instance alone does not restore its world binding.

## Verification boundaries

- On dc37fd66eb6d2810b0e4d5c57236897d768fe167: unchanged failure reproduced on both loaders; repaired targeted groups passed 9/9 each. Full native run passed Forge 509/509 and NeoForge 500/500, with all 1005 strictly admitted identities covered. Dual-loader builds, core/API checks, packaging/platform SDK checks, Python 213 tests, REMI 491 groups and sediment checks passed.
- Upstream was then fast-forwarded to 810a855b41fc1d69aaccd4040baf98d31049a949 without changing the four repaired source files. Read-only integration review found no overlap with the new utility/structure changes.
- Following the newly added AGENTS.md validation policy, the new baseline was checked only with Forge gregtech_sensor_progress: 9/9 passed. New-baseline full/NeoForge suites were NOT run. The earlier full counts must not be attributed to this newer baseline, which adds other regression cases.
- New-baseline XML SHA-256: 930fb9b65b5fddd921369c3b261cc4ae09f5286f397a593abf13d97b4528163d.

This change does not claim complete sensor or ore-processing parity with original GT6.
