# Isolated source-NO input regression profiles

These fixed-only gates complement, rather than join, the regular native suite. The normal suite retains its existing namespace list and count guards (833 at introduction). Per loader, `nonore-regressions.yml` runs two explicit groups. Profiles within each group run **sequentially**, each in a separate fresh GameTest JVM/world:

| Profile | Cases | Required evidence |
|---|---:|---|
| `gregtech_nonore_cohort_audit` | 3 | Actual 53 source-NO classifications; 742 accepted-row/lookup observations; 52 eligible materials absent in the bounded maceration/washing roles; accepted Bakelite override, positive/UNKNOWN/addon and preserved dust controls |
| `gregtech_nonore_tag_audit` | 1 | Two actual tag/datapack rounds; source-NO activation/removal; same-input startup winner; ordered pack restoration completed before success; exact cleanup receipt |
| `gregtech_nonore_stop_audit` | 1 | Two active captured recipe identities and warmed lookups cleared at the real server-stopped event; LOWEST observer receipt; foreign metadata/routes absent and startup winner preserved |

The Sifting group adds these independent profiles; none joins normal833:

| Profile | Cases | Required evidence |
|---|---:|---|
| `gregtech_nonore_sifting_audit` | 5 | Six source-NO purified whole/tiny Sifting roles absent in accepted/cold/warm lookup; literal Cinnabar controls and retained legacy Ice IDs |
| `gregtech_nonore_ice_sifting_audit` | 1 | Real input tags activate literal Ice fallback outputs8/4/2/1/2/4/dust1; tiny remains literal during whole datapack override; third reload restores exact packs and removes owned rows |
| `gregtech_nonore_ice_winner_audit` | 1 | Actual accepted startup tiny-input winner survives tags, whole datapack override and restoration by exact identity; separate profile prevents masking the literal tiny oracle |
| `gregtech_nonore_ice_startup_grade_audit` | 1 | Actual public startup upper-grade requests accepted; upper outputs1/1/1 replace8/4/2 fallback during real input-tag reloads, then exact restoration |

The cohort explicitly calls the recipe overlay and is not safe to merge blindly into a concurrently executing global suite. The four-job matrix is loader × group; all four must succeed for the workflow to succeed. Each job has independent disposable worlds and artifacts named with loader, group and commit SHA. Each profile is a new Gradle invocation with `--no-daemon`, one worker, 1536 MiB build heap and 3 GiB native heap. There is no existing-world input/resume option and no automatic retry. A failed process, checker, missing receipt or 25-minute timeout aborts subsequent profiles in that runner invocation; the failed matrix job still fails the workflow. The existing three-profile input group retains a 90-minute outer timeout. The four-profile Sifting group has120minutes: four unchanged25minute per-profile ceilings plus20minutes for setup/checks/artifact handling. This does not weaken any individual timeout or cleanup requirement. Interruption attempts bounded process-group termination/reaping; independently detached tool processes remain the CI VM teardown boundary, not a claimed restoration mechanism. Cancellation/timeouts fail the job and do not imply restoration; worlds remain disposable evidence and must never be reused. The runner never writes an EULA acceptance; it uses only the existing development GameTestServer entrypoint.

`tools/integration/run_nonore_profiles.py --loader forge` (or `neoforge`) runs all seven profiles/13 cases sequentially by default. `--profile-group inputs` runs the original three profiles/five cases; `--profile-group sifting` runs the new four profiles/eight cases. CI explicitly selects one group. The runner requires JDK17/JDK21 via `JAVA17_HOME`/`JAVA21_HOME`. It creates evidence under `work/nonore-ci/`. The workflow retains logs, native XML/exit statuses, command/nonce manifests, markers and hidden receipts even on failure. The stop profile intentionally leaves its private pack in a **dead disposable world**; it does not claim live pack restoration. An XML pass without its corresponding strict receipt fails the gate. No profile proves opening a second world in the same JVM.

The checked-in `nonore-input-source-profile.json` is a bounded original-source provenance ledger, not a recipe-presence oracle for arbitrary external registrations. Omitted identities and Sand remain UNKNOWN. The 52/1 fixture partition is deliberately strict: unexpected external registrations require diagnosis, not deletion of other-owned rows. The original cohort concerns only captured maceration/washing producers. The new Sifting profiles add only owned Sifting emissions and bounded startup Ice grade selection; see `NONORE_SIFTING_AUDIT.md` and `nonore-sifting-source-profile.json`. Neither claims all ore-processing or thermal/crafting routes.

## Comparative baseline evidence

The introduction used separate local baseline/fixed native runs with the **same fixtures** and exact reversal/restoration of the seven-file production patch. Only the source-NO policy case was allowed to fail in baseline; the two controls had to pass. `check_nonore_cohort.py --phase baseline` remains available to validate those archived runs, but CI never rewinds production or expects failures.

`compare_nonore_dust_snapshots.py --baseline-log BASELINE --fixed-log FIXED` compares actual per-loader semantic recipe multisets (duplicates significant). This is a local comparative proof using archived source manifests, not a checked-in universal baseline fingerprint. Fixed CI checks a nonempty dust snapshot and same-run before/after overlay preservation. It does **not** substitute that assertion for cross-revision parity or claim preservation of unrecorded arbitrary item components/NBT/flags.

## Local verified Gradle wrapper

CI's default command is unchanged. For a separately reviewed local launcher, use `--gradle-executable /absolute/path/to/run-local-native.sh --gradle-wrapper-mode`. The executable must resolve to one executable regular file; paths containing spaces remain one argv element. No shell command parsing, shell=True, or executable arguments embedded in the path are supported.

Wrapper mode explicitly delegates `--no-daemon`, worker count and Java/toolchain setup to that launcher, avoiding duplicate options. The runner sets `LOCAL_GRADLE_MAX_WORKERS=1` for the existing verified local wrapper and retains its own 1536 MiB Gradle/3 GiB native heap arguments. The current local script already supplies daemon/workers, toolchain discovery and local proxy/trust configuration; this option does not edit that script or change security settings. An arbitrary wrapper is not automatically trusted to satisfy those contracts. Default CI still validates JDK17/JDK21 before launch; wrapper mode delegates that validation/setup to its explicitly selected launcher. All namespace/world/timeout/receipt restrictions remain identical.
