package gregtech.engine.logistics;

import gregtech.content.logistics.LogisticsCoverDefinition;

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
                    LogisticsCoverDefinition.Role destinationRole, int destinationRank, int conversionProcessors);
    }
    public interface ItemChannel extends FluidChannel {
        int tryDump(int conversionProcessors);
        int lastConversionUsed();
    }

    private LogisticsRoutingRules() {
    }

    public static Result route(int logicProcessors, int conversionProcessors,
                               FluidChannel fluids, ItemChannel items) {
        if (logicProcessors <= 0 || conversionProcessors <= 0)
            return new Result(0, 0, 0, 0);
        int itemsMoved = 0, fluidMoved = 0, operations = 0, usedConversion = 0;
        long energyCost = 0;
        for (int passIndex = 0; passIndex < logicProcessors; passIndex++) {
            Result passResult = empty();
            // GT6 :451-468: imports prefer exports (filtered→generic), then storage
            // (filtered→generic); each priority pair tries fluid before items.
            for (int destinationIndex = 0; destinationIndex < 6 && passResult.operations() == 0; destinationIndex++) {
                var destinationRole = destinationIndex < 3
                        ? LogisticsCoverDefinition.Role.EXPORT : LogisticsCoverDefinition.Role.STORAGE;
                int priorityRank = 2 - (destinationIndex % 3);
                for (int sourceRank = 0; sourceRank < 3 && passResult.operations() == 0; sourceRank++)
                    passResult = tryPair(fluids, items, LogisticsCoverDefinition.Role.IMPORT, sourceRank,
                            destinationRole, priorityRank, conversionProcessors);
            }
            // Unneeded inventory from storage moves into higher-priority exports.
            for (int exportRank = 2; exportRank >= 0 && passResult.operations() == 0; exportRank--)
                for (int storageRank = 0; storageRank < 3 && passResult.operations() == 0; storageRank++)
                    passResult = tryPair(fluids, items, LogisticsCoverDefinition.Role.STORAGE, storageRank,
                            LogisticsCoverDefinition.Role.EXPORT, exportRank, conversionProcessors);
            // GT6 defragments only generic storage, filtered before semi-filtered.
            for (int destinationRank = 2; destinationRank >= 1 && passResult.operations() == 0; destinationRank--)
                passResult = tryPair(fluids, items, LogisticsCoverDefinition.Role.STORAGE, 0,
                        LogisticsCoverDefinition.Role.STORAGE, destinationRank, conversionProcessors);
            if (passResult.operations() == 0) {
                int dumpedItems = items.tryDump(conversionProcessors);
                if (dumpedItems > 0)
                    passResult = new Result(dumpedItems, 0, 1, dumpedItems,
                        1, items.lastConversionUsed());
            }
            if (passResult.operations() == 0)
                break;
            itemsMoved += passResult.itemsMoved();
            fluidMoved += passResult.fluidMoved();
            operations += passResult.operations();
            energyCost += passResult.energyCost();
            usedConversion = Math.max(usedConversion, passResult.usedConversion());
        }
        return new Result(itemsMoved, fluidMoved, operations, energyCost,
                operations, usedConversion);
    }

    private static Result empty() {
        return new Result(0, 0, 0, 0);
    }

    private static Result tryPair(FluidChannel fluids, ItemChannel items,
                                  LogisticsCoverDefinition.Role sourceRole, int sourceRank,
                                  LogisticsCoverDefinition.Role destinationRole, int destinationRank,
                                  int conversionProcessors) {
        int fluidAmount = fluids.tryPair(sourceRole, sourceRank, destinationRole, destinationRank,
                conversionProcessors);
        if (fluidAmount > 0)
            return new Result(0, fluidAmount, 1, (fluidAmount + 249L) / 250L,
                1, (int) ((fluidAmount + 15_999L) / 16_000));
        int itemCount = items.tryPair(sourceRole, sourceRank, destinationRole, destinationRank, conversionProcessors);
        return itemCount > 0 ? new Result(itemCount, 0, 1, itemCount,
                1, items.lastConversionUsed()) : empty();
    }
}
