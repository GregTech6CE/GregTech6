package com.gregtech.gregtech.content.logistics;

import com.gregtech.gregtech.api.inventory.MassStorageMaterialForms;
import com.gregtech.gregtech.blockentity.inventory.MassStorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Item endpoints for GT6's import, export, storage, defragmentation and dump passes. */
public final class LogisticsItemRouter {
    private record Endpoint(BlockPos targetPos, IItemHandler items, ItemStack filter,
                            int targetSize, boolean bulkStorage) {}
    private record Reservation(Predicate<ItemStack> matches) {
        boolean matches(ItemStack candidate) {
            return matches.test(candidate);
        }
    }
    private record Transfer(int items, int conversionUsed) {}

    private LogisticsItemRouter() {}

    /** Reusable endpoint scan so fluid and item routes can share each GT6 logic-CPU cycle. */
    public static Session collect(Level level, LogisticsNetwork.Snapshot network) {
        List<List<Endpoint>> imports = List.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        List<List<Endpoint>> exports = List.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        List<List<Endpoint>> storages = List.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        List<Endpoint> dumps = new ArrayList<>();
        List<Reservation> reserved = new ArrayList<>();
        for (BlockPos hostPos : network.positions()) {
            if (!level.hasChunkAt(hostPos)) continue;
            var hostEntity = level.getBlockEntity(hostPos);
            // GT6's Logistics Mass Storage is its own network tile. It has no bus cover:
            // an empty store is Generic, while a selected template makes it Semi-Filtered.
            if (hostEntity instanceof LogisticsStorageHost direct) {
                int priority = direct.itemStoragePriority();
                if (priority >= 1 && priority <= 3) {
                    IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, hostPos, (Direction)null);
                    if (handler != null) {
                        ItemStack selected = direct.itemStorageFilter();
                        ItemStack filter = selected == null ? ItemStack.EMPTY : selected.copy();
                        storages.get(priority - 1).add(new Endpoint(hostPos, handler, filter, 0,
                                hostEntity instanceof MassStorageBlockEntity));
                        if (!filter.isEmpty()) {
                            reserved.add(new Reservation(candidate ->
                                    LogisticsCoverInteraction.matches(filter, candidate)));
                        }
                        if (hostEntity instanceof MassStorageBlockEntity storage && !storage.template().isEmpty()) {
                            ItemStack template = storage.template().copy();
                            reserved.add(new Reservation(candidate ->
                                    MassStorageMaterialForms.reserves(template, candidate)));
                        }
                    }
                }
            }
            if (!(hostEntity instanceof LogisticsCoverHost host)) continue;
            if(hostEntity instanceof com.gregtech.gregtech.content.cover.PanelCoverHost panels&&panels.panels().stopped())continue;
            for (Direction face : Direction.values()) {
                ItemStack cover = host.logisticsCovers().get(face);
                LogisticsCoverType type = LogisticsCoverType.of(cover);
                if (type == null || !type.routesItems() || type.role() == LogisticsCoverType.Role.DISPLAY) continue;
                ItemStack filter = type.filtered() ? LogisticsCoverInteraction.itemFilter(cover,level.registryAccess()) : ItemStack.EMPTY;
                if (type.filtered() && filter.isEmpty()) continue;
                BlockPos targetPos = hostPos.relative(face);
                if (!level.hasChunkAt(targetPos)) continue;
                var target = level.getBlockEntity(targetPos);
                // GT6 skips covers aimed into another logistics tile to avoid loops.
                if (target instanceof LogisticsHost logistics && logistics.canLogistics(null)) continue;
                if (target == null) continue;
                // GT6's semi-filtered item tiles elevate an unmodified generic bus to
                // Semi priority and reserve their selected items from the Dump phase.
                LogisticsItemFilter semiFilter = !type.filtered() && type.role() != LogisticsCoverType.Role.DUMP
                        && target instanceof LogisticsSemiFilteredItem semi
                        ? semi.logisticsItemFilter() : null;
                ItemStack nativeFilter = !type.filtered() && type.role() != LogisticsCoverType.Role.DUMP
                        && target instanceof MassStorageBlockEntity storage
                        ? storage.template() : ItemStack.EMPTY;
                if (!filter.isEmpty()) {
                    ItemStack selected = filter.copy();
                    reserved.add(new Reservation(candidate -> LogisticsCoverInteraction.matches(selected, candidate)));
                }
                if (semiFilter != null) reserved.add(new Reservation(semiFilter::matches));
                if (!nativeFilter.isEmpty()) {
                    ItemStack selected = nativeFilter.copy();
                    reserved.add(new Reservation(candidate -> MassStorageMaterialForms.reserves(selected, candidate)));
                }
                IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, face.getOpposite());
                if (handler == null) continue;
                Endpoint endpoint = new Endpoint(targetPos, handler, filter,
                        type.targetStackSize() ? LogisticsCoverInteraction.targetStackSize(cover) : 0,
                        target instanceof MassStorageBlockEntity);
                if (type.role() == LogisticsCoverType.Role.DUMP) dumps.add(endpoint);
                else {
                    int value = LogisticsCoverInteraction.value(cover);
                    int rank = (value & 3) == 0 && (semiFilter != null || !nativeFilter.isEmpty()) ? 1
                            : type.effectivePriority(value) - 1;
                    (switch (type.role()) {
                        case IMPORT -> imports;
                        case EXPORT -> exports;
                        case STORAGE -> storages;
                        default -> throw new IllegalStateException("Unsupported item logistics role " + type.role());
                    }).get(rank).add(endpoint);
                }
            }
        }
        return new Session(level, imports, exports, storages, dumps, reserved);
    }

    public static final class Session {
        private final Level level;
        private final List<List<Endpoint>> imports;
        private final List<List<Endpoint>> exports;
        private final List<List<Endpoint>> storages;
        private final List<Endpoint> dumps;
        private final List<Reservation> reserved;
        private int lastConversionUsed;

        private Session(Level level, List<List<Endpoint>> imports, List<List<Endpoint>> exports,
                        List<List<Endpoint>> storages, List<Endpoint> dumps, List<Reservation> reserved) {
            this.level = level;
            this.imports = imports;
            this.exports = exports;
            this.storages = storages;
            this.dumps = dumps;
            this.reserved = reserved;
        }

        /** First successful GT6 source→destination pair at these priority ranks. */
        public int tryRankPair(int exportRank, int importRank, int conversion) {
            return tryPair(LogisticsCoverType.Role.IMPORT, importRank,
                    LogisticsCoverType.Role.EXPORT, exportRank, conversion);
        }

        public int tryPair(LogisticsCoverType.Role sourceRole, int sourceRank,
                           LogisticsCoverType.Role destinationRole, int destinationRank, int conversion) {
            lastConversionUsed = 0;
            if (conversion <= 0) return 0;
            List<Endpoint> sources = (sourceRole == LogisticsCoverType.Role.IMPORT ? imports : storages).get(sourceRank);
            List<Endpoint> destinations = (destinationRole == LogisticsCoverType.Role.EXPORT ? exports : storages).get(destinationRank);
            for (Endpoint source : sources)
                for (Endpoint destination : destinations) {
                    if (source.targetPos.equals(destination.targetPos)) continue;
                    Transfer moved = movePair(level, source, destination, conversion, List.of());
                    if (moved.items() > 0) {
                        lastConversionUsed = moved.conversionUsed();
                        return moved.items();
                    }
                }
            return 0;
        }

        /** GT6 empties generic storage into dump buses only for unreserved item types. */
        public int tryDump(int conversion) {
            lastConversionUsed = 0;
            if (conversion <= 0) return 0;
            for (Endpoint source : storages.get(0))
                for (Endpoint destination : dumps) {
                    if (source.targetPos.equals(destination.targetPos)) continue;
                    Transfer moved = movePair(level, source, destination, conversion, reserved);
                    if (moved.items() > 0) {
                        lastConversionUsed = moved.conversionUsed();
                        return moved.items();
                    }
                }
            return 0;
        }

        /** GT6 tracks successful conversion-loop iterations, not transferred item count. */
        public int lastConversionUsed() { return lastConversionUsed; }
    }

    private static Transfer movePair(Level level, Endpoint source, Endpoint destination, int conversion,
                                List<Reservation> excluded) {
        int total = 0;
        int successfulIterations = 0;
        for (int i = 0; i < conversion; i++) {
            int moved = moveOne(level, source, destination, excluded);
            if (moved <= 0) break;
            total += moved;
            successfulIterations++;
            // GT6 repeats only when both target-stack-size controls are variable (zero).
            if (source.targetSize != 0 || destination.targetSize != 0) break;
        }
        return new Transfer(total, successfulIterations);
    }

    private static int moveOne(Level level, Endpoint source, Endpoint destination,
                               List<Reservation> excluded) {
        int sourceMaximum = source.targetSize == 0 ? 64 : source.targetSize;
        int sourceMinimum = source.targetSize == 0 ? 1 : source.targetSize;
        int outputMaximum = destination.targetSize == 0 ? 64 : destination.targetSize;
        int outputMinimum = destination.targetSize == 0 ? 1 : destination.targetSize;
        for (int from = 0; from < source.items.getSlots(); from++) {
            ItemStack existing = source.items.getStackInSlot(from);
            if (existing.isEmpty() || existing.getCount() < sourceMinimum
                    || !LogisticsCoverInteraction.matches(source.filter, existing)
                    || !LogisticsCoverInteraction.matches(destination.filter, existing)
                    || excluded.stream().anyMatch(reservation -> reservation.matches(existing))) continue;
            ItemStack available = source.items.extractItem(from, sourceMaximum, true);
            if (available.isEmpty() || available.getCount() < sourceMinimum) continue;
            for (int to = 0; to < destination.items.getSlots(); to++) {
                ItemStack occupied = destination.items.getStackInSlot(to);
                // Bulk storage's visible stack is capped at one ordinary stack.
                // Its simulated insert, not that visible count, determines room;
                // it can also accept another GT6 form of the same material.
                if (!destination.bulkStorage && !occupied.isEmpty()
                        && !ItemStack.isSameItemSameComponents(occupied, available)) continue;
                int maximumFinal = Math.min(outputMaximum,
                        Math.min(available.getMaxStackSize(), destination.items.getSlotLimit(to)));
                int occupiedCount = destination.bulkStorage ? 0 : occupied.getCount();
                int capacity = maximumFinal - occupiedCount;
                int offered = Math.min(available.getCount(), capacity);
                if (offered < sourceMinimum || occupiedCount + offered < outputMinimum) continue;
                ItemStack remainder = destination.items.insertItem(to, available.copyWithCount(offered), true);
                int accepted = offered - remainder.getCount();
                if (accepted < sourceMinimum || occupiedCount + accepted < outputMinimum) continue;
                ItemStack extracted = source.items.extractItem(from, accepted, false);
                if (extracted.isEmpty()) continue;
                ItemStack rejected = destination.items.insertItem(to, extracted, false);
                int transferred = extracted.getCount() - rejected.getCount();
                if (!rejected.isEmpty()) {
                    ItemStack returned = source.items.insertItem(from, rejected, false);
                    if (!returned.isEmpty()) Block.popResource(level, source.targetPos, returned);
                }
                if (transferred > 0) return transferred;
            }
        }
        return 0;
    }
}
