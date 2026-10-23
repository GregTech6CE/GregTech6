# Machine and functional-block audit continuation

User priority: machines, functional blocks, actual missing functionality, and multiblock cross-chunk lifecycle. Baseline main `4049d81b8` passed both builds and 114 selected native cases. The prior bounded CI pass is not completion of the broader audit.

## Machine menu lifetime

Basic machines and both apiaries checked only the removed flag; hopper and queue providers inherited a menu that checked only non-null inventory. Native server container handlers rely on `stillValid`, so distant or wrong-world viewers could keep a menu valid. The new guards require a live original block entity, same world, at most eight blocks, and a loaded owner chunk before querying its identity. Client null-owner handling and inventory transactions are unchanged.

Five shared native tests use registered blocks and their real menu providers. They cover nearby and exact-boundary players, too-far and other-dimension players, removed/destroyed/replaced owners, and the replacement's own valid menu. This tests validity predicates, not a network disconnect or actual automatic GUI-close flow. Selected totals become Forge64 / NeoForge60; exact-commit native execution is still required. No test substitutes simulated chunk availability for real unloading.

## Remaining priority checks

- Multiblock loaded-cell checks, binding repair, cross-border adjacent energy/item/fluid I/O, and unload/reform runtime evidence are under review.
- Forge derived capability lifetimes and cache invalidation are under review separately from menus.
- Zero-mB textual recipe inputs mean presence only, with no consumption or additional reserve. Native stack identity must survive copying; implementation and specialized-consumer compatibility are being verified.
- Confirmed absent source features include storage inserters, charging lockers, magic-field absorbers and LU crystal chargers. These are missing execution/registration paths, not translation-name gaps. They are not silently implemented as aliases of existing machines.
- Existing ordinary locker interaction direction/equipment rules remain a separate bounded candidate. Plant corrections and independent two-JVM bee/reactor restart drafts are deferred behind the user's machine priority. No old-save compatibility claim.

## Cross-border neighbor reads and mixed layout alternatives

Six BasicMachine adjacent I/O paths now check loadedness before reading the neighbor: primary and auxiliary energy polling, automatic item input/output, automatic fluid input and fluid-output target lookup. This preserves current transfer/filter logic but prevents absent chunks being force-loaded or treated as empty air for item ejection. Native tests place a real registered large centrifuge at positive and negative remote chunk borders, form its complete loaded shell, and call its native tick; a second test checks unavailable I/O retains stock and a subsequently loaded chest receives exactly that stock. These are no-force-load tests, not evidence of a complete natural multiblock unload/reload cycle.

PR #93 permits plain-block/GT-part alternatives. Validation previously returned only a boolean and then bound using the first branch's metadata: a stone alternative could be cast to a missing port, or a port alternative could remain unbound. Validation now retains the actual matched branch for binding/designs, including only complete repeat copies. Two native tests use the public declaration converter and an isolated layout override on a real controller implementation, exercise both choice orders, plain/port transitions, role isolation, incomplete repeats and invalidation/reformation. The layout override is an explicit fixture boundary, not a mock of matching/binding logic.

The earlier menu commit `9d93a848e` passed both main workflows and all 124 native cases. These four new shared tests raise selected totals to Forge68 / NeoForge64; their exact-commit native execution is pending at authoring time.
