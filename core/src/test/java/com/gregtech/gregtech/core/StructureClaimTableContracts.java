package com.gregtech.gregtech.core;

import com.gregtech.gregtech.api.multiblock.StructureClaimTable;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** Ownership transitions independent of Minecraft availability, scan order and scheduling budget. */
public final class StructureClaimTableContracts {
    private int assertions;

    public static void main(String[] args) {
        System.out.println("Structure claim table contracts passed: " + verify());
    }

    public static int verify() {
        var contracts = new StructureClaimTableContracts();
        contracts.atomicContention();
        contracts.reservationWinsBeforeEvaluation();
        contracts.boundedOwnership();
        contracts.reverseIndexesAndCleanup();
        contracts.invalidBatchDoesNotPartiallyClaim();
        return contracts.assertions;
    }

    private StructureClaimTable table() { return new StructureClaimTable(part -> Math.floorDiv(part, 16)); }

    private void atomicContention() {
        var table = table();
        check(table.claim(0, Long.MIN_VALUE), "all long values are valid owners, including minimum");
        check(table.claim(0, Long.MIN_VALUE), "reloaded owner recognizes its own existing claim");
        check(!table.claimAll(List.of(-1L, 0L, 16L), Long.MAX_VALUE), "one occupied cell rejects an atomic batch");
        check(table.owner(-1) == null && table.owner(16) == null, "failed batch leaves free cells untouched");
        check(!table.release(0, Long.MAX_VALUE), "competitor cannot release the owner's part");
        check(table.claim(32, Long.MAX_VALUE) && !table.claim(0, Long.MAX_VALUE)
                && Long.valueOf(Long.MAX_VALUE).equals(table.owner(32)), "per-cell failure preserves prior successful cells");
        table.releaseController(Long.MAX_VALUE);
        check(table.claimAll(List.of(-1L, 0L, 16L, 16L), Long.MIN_VALUE), "own claim and duplicates accepted");
        check(table.size() == 3 && table.loadedParts(Long.MIN_VALUE).size() == 3, "idempotent indexes");
        check(table.controllers().equals(Set.of(Long.MIN_VALUE)), "owner reverse index has one entry");
    }

    private void reservationWinsBeforeEvaluation() {
        var table = table();
        table.claimAll(List.of(-17L, -16L, -1L, 0L, 16L), 100);
        table.chunkUnloaded(-1, owner -> owner == 100);
        check(table.size() == 3 && table.reservationCount() == 2, "only parts in the absent negative chunk move");
        check(table.owner(-1) == null && Long.valueOf(100).equals(table.reservedOwner(-1)), "reservation is separate");
        check(!table.canClaim(-1, 200), "unloaded part is reserved against a competitor");
        table.chunkLoaded(-1);
        check(table.reservationCount() == 0 && table.size() == 5, "chunk-load notification restores ownership immediately");
        for (int delayedTicks = 0; delayedTicks < 10; delayedTicks++)
            check(!table.claim(-1, 200), "no original evaluation or chunk query needed to exclude competitors");
        check(table.claim(-1, 100), "original owner can reevaluate after an arbitrary delay");
        check(table.release(-1, 100) && table.claim(-1, 200), "normal release permits the waiting competitor");
    }

    private void boundedOwnership() {
        var table = table();
        table.claimAll(List.of(0L, 16L), 100);
        table.chunkUnloaded(1, owner -> true);
        table.controllerUnloaded(100);
        check(table.reservationCount() == 0, "reservation does not outlive a loaded controller");
        check(Long.valueOf(100).equals(table.owner(0)), "controller unload preserves loaded-part claim");
        check(!table.claim(0, 200), "missing controller cannot be robbed while its part remains loaded");
        table.chunkUnloaded(0, owner -> false);
        check(table.size() == 0 && table.controllers().isEmpty(), "absent owner and absent part leave no entry");
        table.chunkLoaded(0);
        check(table.claim(0, 200), "one part-chunk unload bounds ownership of the absent controller");

        // The two controller/part unload callback orders must converge.
        var opposite = table();
        opposite.claim(0, 100);
        opposite.controllerUnloaded(100);
        opposite.chunkUnloaded(0, owner -> false);
        check(opposite.size() == 0 && opposite.reservationCount() == 0, "controller-first unload also drops ownership");
    }

    private void reverseIndexesAndCleanup() {
        var table = table();
        table.claimAll(List.of(0L, 16L, 32L), 100);
        table.claim(1, 200);
        table.chunkUnloaded(1, owner -> true);
        check(!table.release(16, 200), "reservation cannot be released by a stale competitor");
        check(table.reservedParts(100).equals(Set.of(16L)), "reservation reverse index");
        Set<Long> snapshot = table.loadedParts(100);
        check(table.releaseController(100).equals(Set.of(0L, 16L, 32L)), "disassembly releases loaded and absent parts");
        check(snapshot.equals(Set.of(0L, 32L)), "returned indexes are immutable snapshots");
        check(table.size() == 1 && table.reservationCount() == 0, "other controller remains untouched");
        check(table.controllers().equals(Set.of(200L)), "empty reverse indexes removed");
        table.chunkLoaded(1);
        check(table.owner(16) == null, "released reservation cannot resurrect on load");
        table.chunkUnloaded(0, owner -> true);
        check(table.reservationCount() == 1, "other controller can still reserve its part");
        table.clear();
        table.chunkLoaded(0);
        check(table.size() == 0 && table.reservationCount() == 0 && table.controllers().isEmpty(), "world teardown clears all indexes");
        check(table.claim(1, 300), "cleared coordinate is reusable");
    }

    private void invalidBatchDoesNotPartiallyClaim() {
        var table = table();
        boolean rejected = false;
        try {
            table.claimAll(Arrays.asList(0L, null), 100);
        } catch (NullPointerException expected) {
            rejected = true;
        }
        check(rejected && table.size() == 0, "invalid candidate batch cannot leave a partial claim");
        table.claim(0, 100);
        rejected = false;
        try {
            table.chunkUnloaded(0, owner -> { throw new IllegalStateException("availability failed"); });
        } catch (IllegalStateException expected) {
            rejected = true;
        }
        check(rejected && table.size() == 1 && table.reservationCount() == 0,
                "availability failure precedes all unload mutations");
    }

    private void check(boolean result, String message) {
        assertions++;
        if (!result) throw new AssertionError(message);
    }
}
