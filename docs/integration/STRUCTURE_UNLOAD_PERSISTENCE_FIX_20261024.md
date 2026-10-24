# Structure tracking: native unload persistence repair

## Scope and upstream intent

This follows upstream PR103 (`05777cda3`) integrated with the bounded Sifting work at `b3080d2d0`. The inexpensive structure-query / scheduled-revalidation architecture remains intact. PR103 already supplied the missing shell-unload formed-state notification; the earlier unexecuted local shell-sync draft was retired, not reapplied. This repair narrows that upstream notification when the **controller's own chunk** is unloading, while preserving it for a surviving loaded controller after shell-only unload.

The eight new shared StructureTracking identities use `gt6ce_structure_tracking:test_empty`. Strict selection grows from833 (Forge421 + Neo412) to **849 (Forge429 + Neo420)**; all833 existing identities/template bindings and the optimized matcher remain. No dynamic/generated native cases were added. The field-only mixin exemption remains exact-package/name scoped, with a negative guard against broader exemptions.

## Observed failures and causal correction

The first integrated Forge run had427/429 passing: shell-unload event-coordinate proof and registered multiblock native reload failed. A retry exposed a second fixed-delay fixture race. Waiting a fixed150 ticks, or observing only an unavailable holder, did not establish that the exact target chunk's native unload callback and block-entity removal had completed. The tests now poll those actual events/removal conditions with a350-tick bound inside their existing400-tick timeout, then cross one real server tick before reload. Original callback ordering, fresh object identity, persistence, no-force-load and reform assertions remain; cleanup runs on success, failure and timeout without loading absent chunks.

With those fixture corrections, Forge reached428/429: the persistence case still failed. Actual `ChunkDataEvent.Save` copies contained the correct controller and section state, **7 diamonds and512 energy**. The explicitly reloaded FULL chunk was a different object but held AIR and no controller; no owned FULL Load event was observed. Thus a stale old-chunk object was not the sole cause, and missing block-entity hydration was not justified as another generic wait.

The source chain identifies the production interaction:

1. Native unload removes the pending-unload entry and emits `ChunkEvent.Unload` **before saving that chunk**.
2. PR103's falling-edge path called `ClientFormedSync.send`, including for the controller's own chunk.
3. `ServerLevel.sendBlockUpdated` evaluates collision shapes. Applied `CoverCollisionMixin` routes both collision getters through `CoverWorldInteraction.collision`, which first looks up the block entity even when no covers exist.
4. That lookup can request the now-absent controller FULL chunk before its current contents are written, establishing empty/generated state that the later explicit reload sees.

The underlying machine's constant collision shape does **not** make this call read-free; the initial source-only inference omitting the cover mixin was corrected. Normal I/O already consults pending writes, and native FULL completion hydrates block entities synchronously: forced disk flush, fake unload events, reflected cache clearing, delayed hydration retries or cached-tag reinjection were not used as substitutes.

`StructureWatchIndex.chunkUnloaded` still suspends the tracker, removes dirty work and calls `structureFormedChanged(false)`. It sends the update only when the controller lies in a different chunk, is not removed, and an already-available chunk's live block-entity map contains that same controller. `hasChunkAt`, `getChunkNow` and direct map identity checking do not acquire a chunk or hydrate a saved entity. This is not a global notification suppression or a return to query-time scans.

## Native A/B proof and loader diagnostic compatibility

After the production guard, Forge429/429 passed. The retained observer now saw **2 actual Save +2 actual Load events** for the two fixture chunks; controller Save/Load payloads matched with7 diamonds/512 energy. Fresh native controller/chunk/port identities, controller-only reload while the shell remained unavailable, safe unformed behavior, then two rebound ports and successful reform all passed. This uses native serialization/reloading, not a standalone freshly constructed update tag.

That first corrected Neo run exposed a separate **test diagnostic API** problem: its bus rejects listeners on abstract `ChunkDataEvent`. Two concrete public Save/Load subscribers now delegate to the unchanged private capture body. Both loader classes were checked; no assertion, event payload, production behavior or testcase count changed. The final same-source runs then passed **Forge429 + Neo420 =849**, native and strict checker exits0, with no failure/error/skip and the real Save→Load/7/512 proof on both loaders.

The observer copies only the fixture's two chunks and never mutates/reinjects their NBT. Its world filter intentionally does not record null-level ProtoChunk Load events; absence of such a record alone is not a universal proof of absent reads. The causal conclusion combines the native failure, the full mixin/native source chain and the narrow-fix A/B result.

## Reproducibility and verification

The final source delta has exactly five paths: the two native test files, `StructureWatchIndex.java`, and the strict checker plus its Python tests. Starting from `b3080d2d0c875018626679bfff59fef7bafc6cd4`, the reviewed patch order is:

| Patch SHA256 prefix | Purpose |
|---|---|
| `2071b0d53c40` | Eight exact strict identities and negative guards |
| `472b7a2ea705` | Shell/native unload completion boundaries and cleanup |
| `2ccbbda3d0f0` | Actual Save/Load diagnostics; controller-unload boundary |
| `1cdf0639e874` | Surviving-loaded-controller notification guard |
| `eead768b578c` | Concrete Save/Load diagnostic subscribers |

Independent temporary-index replay reproduced all five active files byte-for-byte. The tested source diff SHA256 is `8524de0847e8c8a1a01e5bcaaccdd7577edf45824e2d0b7c1ab5a76f8a402d11`. This document is a sixth, documentation-only path; adding it is not represented as a new gameplay run.

Final native XML/logs and local gate receipts are under `work/validation/sifting-pr103-concrete-events/`; baseline evidence is under `sifting-pr103-integrated/`, `sifting-pr103-fixture-retry/` and `sifting-pr103-persistence-diagnostic/`. Ordered patch hashes, final source hashes and exact replay evidence are retained under `work/pr103-structure-closeout-audit/final-audit/` (ignored local artifacts, not assumed to accompany a checkout).

Completed local checks: dual-loader build/core checks (including up-to-date tasks),232 Python guards,491 REMI groups, both packages with1,048 shared classes, and SDK119 core +46 native API classes. Packaging/SDK checks are structural, not external-addon gameplay execution.

The separate seven-profile Sifting/raw-input runner also passed **26/26 (13 per loader)** in fresh isolated JVMs/worlds: cohort3, restoring1, actual-stop1, Sifting5, literal Ice1, tiny startup winner1 and startup grade1 per loader. Native and strict receipt checks passed; these cases are **not** counted in849. Input/final source snapshots are identical. The terminal local receipt SHA256 is `03f1b7d30d4fe52a6ef58d52b29e42c50d556b993434048fbbd18ad16f06a7eb`; its artifact manifest records XML/snapshot/JAR hashes. The stop profile retains its private pack only in the dead disposable world and does not claim live restoration or a same-JVM second world.

These receipts validate the pinned PR103/Sifting source above, not PR105; the separate merged-source result follows below.

## PR105 merged-source validation

Upstream PR105 (`9f4f066bb`) was merged at `0e6e54a3255da0d6eff918f6c73f0f5aacfb809c`, preserving the reviewed own-unload guard and native fixtures. The focused workflow now includes `gt6ce_structure_incremental`; four exact shared identities/template bindings were added without dropping any of the prior849. The three-path admission patch is SHA256 `3ebfefd2ce54cf95bc886a9b8510f20e79fe98a95a8a9a4fe8e4a1b5a375c587`; the tested source diff is `e9a5746ad9d3cad66518330f61742140440c4263de136b87bb239535d3a42084`.

The merged source passed **857/857 selected native cases (Forge433 + Neo424)** at the existing **3g** test heap, with native/strict exits0 and no failures, errors or skips. All four new incremental cases ran, including unchanged2000-step fuzz for each of four families and incremental/full-scan consistency checks. Existing native unload/persistence controls also passed; no tests or assertions were weakened to fit the heap. Upstream's focused6g receipt was not treated as proof that6g was required here.

Dual-loader build/core checks (including up-to-date tasks), **233 Python guards** and **491 REMI groups** passed. Both packages (1,048 shared classes), SDKs (119 core +46 native API classes), mappings/refmaps and physics-JAR checks also passed. Input/final source snapshots are identical; the terminal receipt SHA256 is `d19ce1b016587d21f76b927b267dfc779023459c610f31333f9baf987bd664f9`. Evidence is under `work/validation/sifting-pr105-integrated/`; this documentation-only amendment does not represent another native execution.

To avoid redundant repeated gates in this batch, the seven-profile isolated runner was **not rerun after PR105**. Its26-case result above remains pinned only to the earlier PR103/Sifting source; it is neither added to857 nor represented as a merged-source pass. The restart, old-save, external-addon and whole-chain limits below remain unchanged.

## Limits

These results prove the bounded selected server regressions and native live-world chunk persistence, not a full process restart, old-save migration, real GPU/client rendering, arbitrary addon behavior or whole-chain ore parity. The existing Sifting source-profile/UNKNOWN and late-binding limits remain in [NONORE_SIFTING_AUDIT.md](NONORE_SIFTING_AUDIT.md). Overall port completion is not claimed.
