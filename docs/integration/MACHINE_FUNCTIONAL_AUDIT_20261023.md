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
