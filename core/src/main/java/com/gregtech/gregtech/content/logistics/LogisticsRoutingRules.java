package com.gregtech.gregtech.content.logistics;


/** One GT6 Logistics Core processor pass, shared by the fluid and item channels. */
public final class LogisticsRoutingRules {
    public record Result(int itemsMoved, int fluidMoved, int operations, long energyCost,
                         int usedLogic, int usedConversion) {
        /** Keep the original four-field result usable by existing callers. */
        public Result(int itemsMoved, int fluidMoved, int operations, long energyCost) {
            this(itemsMoved, fluidMoved, operations, energyCost, operations, 0);
        }
    }

    public interface FluidChannel {
        int tryPair(LogisticsCoverDefinition.Role sourceRole, int sourceRank,
                    LogisticsCoverDefinition.Role destinationRole, int destinationRank, int conversion);
    }
    public interface ItemChannel extends FluidChannel {
        int tryDump(int conversion);
        int lastConversionUsed();
    }

    private LogisticsRoutingRules() {}

    public static Result route(int logic, int conversion, FluidChannel fluids, ItemChannel items) {
        if (logic <= 0 || conversion <= 0) return new Result(0, 0, 0, 0);
        int itemsMoved = 0, fluidMoved = 0, operations = 0, usedConversion = 0;
        long energyCost = 0;
        for (int cycle = 0; cycle < logic; cycle++) {
            Result moved = empty();
            // GT6 :451-468: imports prefer exports (filtered→generic), then storage
            // (filtered→generic); each priority pair tries fluid before items.
            for (int destination = 0; destination < 6 && moved.operations() == 0; destination++) {
                var role = destination < 3 ? LogisticsCoverDefinition.Role.EXPORT : LogisticsCoverDefinition.Role.STORAGE;
                int rank = 2 - (destination % 3);
                for (int sourceRank = 0; sourceRank < 3 && moved.operations() == 0; sourceRank++)
                    moved = tryPair(fluids, items, LogisticsCoverDefinition.Role.IMPORT, sourceRank,
                            role, rank, conversion);
            }
            // Unneeded inventory from storage moves into higher-priority exports.
            for (int exportRank = 2; exportRank >= 0 && moved.operations() == 0; exportRank--)
                for (int storageRank = 0; storageRank < 3 && moved.operations() == 0; storageRank++)
                    moved = tryPair(fluids, items, LogisticsCoverDefinition.Role.STORAGE, storageRank,
                            LogisticsCoverDefinition.Role.EXPORT, exportRank, conversion);
            // GT6 defragments only generic storage, filtered before semi-filtered.
            for (int destinationRank = 2; destinationRank >= 1 && moved.operations() == 0; destinationRank--)
                moved = tryPair(fluids, items, LogisticsCoverDefinition.Role.STORAGE, 0,
                        LogisticsCoverDefinition.Role.STORAGE, destinationRank, conversion);
            if (moved.operations() == 0) {
                int dumped = items.tryDump(conversion);
                if (dumped > 0) moved = new Result(dumped, 0, 1, dumped,
                        1, items.lastConversionUsed());
            }
            if (moved.operations() == 0) break;
            itemsMoved += moved.itemsMoved();
            fluidMoved += moved.fluidMoved();
            operations += moved.operations();
            energyCost += moved.energyCost();
            usedConversion = Math.max(usedConversion, moved.usedConversion());
        }
        return new Result(itemsMoved, fluidMoved, operations, energyCost,
                operations, usedConversion);
    }

    private static Result empty() { return new Result(0, 0, 0, 0); }

    private static Result tryPair(FluidChannel fluids, ItemChannel items,
                                  LogisticsCoverDefinition.Role sourceRole, int sourceRank,
                                  LogisticsCoverDefinition.Role destinationRole, int destinationRank, int conversion) {
        int fluid = fluids.tryPair(sourceRole, sourceRank, destinationRole, destinationRank, conversion);
        if (fluid > 0) return new Result(0, fluid, 1, (fluid + 249L) / 250L,
                1, (int) ((fluid + 15_999L) / 16_000));
        int count = items.tryPair(sourceRole, sourceRank, destinationRole, destinationRank, conversion);
        return count > 0 ? new Result(count, 0, 1, count,
                1, items.lastConversionUsed()) : empty();
    }
}
