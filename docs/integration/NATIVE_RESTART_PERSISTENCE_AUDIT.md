# Native two-JVM chunk persistence audit

2026-10-24. **Passed on Forge 1.20.1 / 47.4.20 and NeoForge 1.21.1 / 21.1.243:** new disposable-world bee/reactor chunk state survives an orderly two-JVM development GameTest restart. This is test publication, not a production persistence fix.

## Recorded result

Baseline `9fed8aeadde6546ec1763c63be73ad0bd741b910` plus the seven v3 restart test/source-template files. Each loader used its own newly created, initially empty disposable directory. Each phase ran exactly one required test in a separate native JVM, passed, saved normally and exited zero.

| Loader | Writer → reader PID | Write / read elapsed | Unchanged royal-genome nonce |
| --- | --- | --- | --- |
| Forge | 265 → 207 | 4m18s / 5m37s | `7c482c11-87e5-4910-b1d8-16f007d86b95` |
| NeoForge | 252 → 202 | 2m52s / 2m34s | `4d1420e2-d990-4ac8-a776-9fc87ed4a690` |

Reader logs explicitly reported `warm=false`. Canonical world paths matched each writer; no world copying, repair or read-phase reseeding occurred. Actual `session.lock` acquisition/release after each writer established lock release without changing its bytes. Source/template/compiled restart-class/JAR hashes were unchanged between phases. The marker SHA256 remained:

- Forge: `c60bdaab56fa7e42a20dc66e740c3139beac2b6ee4d85c3266b2576aa008f32c`
- NeoForge: `9a8d53890d814e05481393fcbac4fe1fd2049391108fb4e22d6006e5b66e000d`

Local raw evidence is retained under `work/validation/restart-persistence-v3/`: four XML reports, logs, exit statuses and phase provenance JSONs; `<loader>-pair-public-checker.json`; `<loader>-code-before-read.json`; lock-release receipts; identical input/final snapshots; and `restart-proof-receipt.md`. These ignored workspace artifacts are not packaged with the mod. Public checker pair validation passed against both receipt pairs. Its 13 Python regression tests also passed. The independent normal 601-case exclusion regression passed (Forge 305 + NeoForge 296); neither restart phase executed in its 11 selected namespaces. Dual-platform builds, core contracts, 491 REMI checks and package exclusion checks passed. The public Python checker and workflow additions did not change the Java/NBT inputs. Subsequent upstream integration is recorded separately below.

## What the fixture establishes

The tracked [writer](../../multiversion/src/bootstrapGameTest/java/com/gregtech/gregtech/gametest/RestartWriteTests.java), [reader](../../multiversion/src/bootstrapGameTest/java/com/gregtech/gregtech/gametest/RestartReadTests.java) and [shared assertions](../../multiversion/src/bootstrapGameTest/java/com/gregtech/gregtech/gametest/RestartFixture.java) use contract `gt6-bee-reactor-restart-v3-y304`.

- Fixed absolute base `(20000000,304,20000000)` is independent of GameTest template placement. Both phases check actual dimension build bounds. Writer preflights all 324 cells of its bounded 9×4×9 checkpoint: **air only, no existing block entities**. It then places a stone shell and native machines. Every touched remote chunk is loaded FULL without ticking tickets; non-ticking is asserted. A CREATE_NEW attempt file prevents retrying the same attempted world.
- Normal and advanced apiaries receive native princess/drone inputs. Actual `serverTick()` calls create state: 1,201 ticks yield a queen with life 1,199, countdown 1,200 and naturally generated pending brood; 1,101 ticks yield a waiting princess with countdown 99. No BE NBT injection or fabricated progress is used. Native random brood is recorded rather than predicted.
- Single/four reactor cores retain stopped state and 64,320 L Steam, exceeding the old nominal 64,000 boundary, with capacity 10,240,000. This is fluid-state persistence, not active reactor energy production.
- The marker contains comparison-only canonical persistent NBT strings, never decoded into a block entity. Reader loads existing native chunks and checks every persisted inventory/brood/progress/rod/heat/fluid field plus the unique nonce in actual royal genomes. A missing checkpoint fails; the reader does not place, fill, load injected NBT or repair anything before comparison.
- Only after exact recovery checks, one native bee tick must give queen life 1,198 / princess countdown 98, leaving inventory and pending brood unchanged. This checks continuation without duplicate output on that tick, not full queen-death discharge.

The ordinary isolated dirty-bit tests remain necessary: initial placement and orderly shutdown saving can hide mutation-specific missing dirty marks.

## Earlier failures and scope limits

Two previous Forge writers failed setup prerequisites, not persistence assertions. At Y80 the first found generated terracotta; the second correctly refused a natural `gt6ce:rock` BE. Neither launched a reader. Both failed worlds were preserved. The final pair used a wholly new directory and a fixed Y304 band with stronger air/no-BE checks, not deletion of the obstruction or a relaxed assertion.

Both pinned loaders explicitly exempt development GameTestServer in their native EULA constructor patch. No EULA file was accepted or changed. An ordinary dedicated-server EULA prompt still requires user action; this exemption is not transplanted to that flow.

Native source/disassembly inspection shows Neo's GameTest bootstrap constructs new `LevelSettings` / `PrimaryLevelData` with a flat preset while retaining the existing storage access and loading existing region chunks. Forge instead supplies the normally loaded WorldStem. Thus this proves **chunk/block-entity state**, not complete world-metadata preservation or ordinary dedicated-server acceptance. It does not establish crash recovery, autosave timing, user/old-save migration, cross-loader compatibility, multiblock structure persistence, full survival gameplay or active reactor energy retention. Reader intentionally advances progress, so post-read region hashes need not equal post-write hashes.

## Reproduction (Linux, Bash)

Use JDK17 as launcher and installed JDK17/JDK21 toolchains (`JAVA17_HOME`, `JAVA21_HOME`); Python 3.9+. Run from repository root using the supported Gradle wrapper. Run one loader at a time, never both namespaces in one JVM. Do not modify code/resources between phases. Preserve a source/resource hash manifest before writing, then a compiled-class/JAR manifest after writing and verify both before/after reading; the raw audit's code maps show the files covered.

Choose one configuration; repeat the complete sequence with a **new** directory for the other:

```bash
loader=forge; node=1.20.1-forge; directory_property=gameTestDirectory
# Alternative:
# loader=neoforge; node=1.21.1-neoforge; directory_property=neoforgeGameTestDirectory

set -euo pipefail
export JAVA_HOME="$JAVA17_HOME"
mkdir -p work
export GT6_RESTART_DIRECTORY="$(mktemp -d "$PWD/work/gt6-restart-$loader-XXXXXXXX")"
test -z "$(find "$GT6_RESTART_DIRECTORY" -mindepth 1 -print -quit)"
evidence="$PWD/work/restart-evidence/$(basename "$GT6_RESTART_DIRECTORY")"
mkdir -p "$evidence"
git rev-parse HEAD > "$evidence/head.txt"

run_phase() {
  phase="$1"
  status=0
  bash ./gradlew --no-daemon --max-workers=1 --console=plain \
    -Dorg.gradle.java.installations.fromEnv=JAVA17_HOME,JAVA21_HOME \
    -Dorg.gradle.java.installations.auto-download=false \
    '-Dorg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8' \
    -PaddonSample=true -PgameTestHeap=3g \
    "-P$directory_property=$GT6_RESTART_DIRECTORY" \
    "-PgameTestNamespaces=gregtech_restart_$phase" \
    "-PgameTestReport=$evidence/$phase.xml" \
    ":multiversion:$node:runGameTestServer" \
    > "$evidence/$phase.log" 2>&1 || status=$?
  printf '%s\n' "$status" > "$evidence/$phase.exit"
  python tools/integration/check_restart_phase.py "$phase" \
    "$evidence/$phase.xml" "$evidence/$phase.log" "$status" \
    "$GT6_RESTART_DIRECTORY" > "$evidence/$phase-provenance.json"
}

run_phase write
```

**Stop on any failure.** Never reuse a failed attempted world, rerun the writer over it, copy a saved fixture or repair state. Before reading, verify source/class/JAR hashes and lock release. For the actual world recorded by the writer, this Linux check acquires/releases the existing lock without writing it:

```bash
python - "$evidence/write-provenance.json" <<'PY'
import fcntl, json, sys
from pathlib import Path
world = Path(json.loads(Path(sys.argv[1]).read_text())['world'])
with (world / 'session.lock').open('r+b') as stream:
    fcntl.lockf(stream, fcntl.LOCK_EX | fcntl.LOCK_NB)
    fcntl.lockf(stream, fcntl.LOCK_UN)
PY

run_phase read
python tools/integration/check_restart_phase.py pair \
  "$evidence/write-provenance.json" "$evidence/read-provenance.json" \
  > "$evidence/pair.json"
python -X utf8 -m unittest tools.tests.test_restart_phase_evidence
```

The phase checker requires exact one-case identity, zero supplied exit status, complete save/shutdown logs, a terminated PID and nonempty world/region/marker artifacts. Its `pair` command checks phase order, matching world/nonce, distinct matching writer/reader PIDs and unchanged marker hash/size at the exact canonical recorded-world marker path. It compares **already validated local receipts**; it does not itself prove fresh-directory creation, acquire the world lock, verify code provenance, authenticate logs or decode region contents. Those remain the runner's provenance responsibilities and the native tests' state assertions. Keep phase receipts immediately after their respective runs, not regenerated later from changed files. Restart namespaces remain excluded from the ordinary full suite; do not append these two dependent phases to its expected-case list.
