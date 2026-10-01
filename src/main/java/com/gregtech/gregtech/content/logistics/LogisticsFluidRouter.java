package com.gregtech.gregtech.content.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;

/** The fluid half of GT6's import/export, storage and defragmentation passes. */
public final class LogisticsFluidRouter {
    private record Endpoint(BlockPos targetPos, IFluidHandler handler, FluidStack filter) {}

    private LogisticsFluidRouter() {}

    /** Collect only loaded, externally facing fluid capabilities; no block entity is retained by the network. */
    public static Session collect(Level level, LogisticsNetwork.Snapshot network) {
        List<List<Endpoint>> imports = ranks();
        List<List<Endpoint>> exports = ranks();
        List<List<Endpoint>> storages = ranks();
        if (level.isClientSide || network == null || network.size() == 0)
            return new Session(imports, exports, storages);

        for (BlockPos hostPos : network.positions()) {
            if (!level.hasChunkAt(hostPos)) continue;
            var hostEntity = level.getBlockEntity(hostPos);
            // GT6's Logistics Tank is a storage tile in the network itself. It has no bus cover:
            // a never-filled tank is Generic; once a fluid type is learned it is Semi-Filtered,
            // including after the last unit is withdrawn.
            if (hostEntity instanceof LogisticsStorageHost direct) {
                int priority = direct.fluidStoragePriority();
                if (priority >= 1 && priority <= 3) {
                    IFluidHandler handler = hostEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, null)
                            .resolve().orElse(null);
                    if (handler != null) {
                        var fluid = direct.fluidStorageFilter();
                        FluidStack filter = fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, 1);
                        storages.get(priority - 1).add(new Endpoint(hostPos.immutable(), handler, filter));
                    }
                }
            }
            if (!(hostEntity instanceof LogisticsCoverHost host)) continue;
            for (Direction face : Direction.values()) {
                ItemStack cover = host.logisticsCovers().get(face);
                LogisticsCoverType type = LogisticsCoverType.of(cover);
                if (type == null || !type.routesFluids()
                        || (type.role() != LogisticsCoverType.Role.IMPORT
                        && type.role() != LogisticsCoverType.Role.EXPORT
                        && type.role() != LogisticsCoverType.Role.STORAGE)) continue;

                FluidStack filter = type.filtered() ? LogisticsCoverInteraction.fluidFilter(cover) : FluidStack.EMPTY;
                if (type.filtered() && filter.isEmpty()) continue;
                BlockPos targetPos = hostPos.relative(face);
                if (!level.hasChunkAt(targetPos)) continue;
                var target = level.getBlockEntity(targetPos);
                // A cover looking into the same logistics network is not an external endpoint.
                if (target instanceof LogisticsHost logistics && logistics.canLogistics(null)) continue;
                if (target == null) continue;
                // GT6 classifies a generic bus aimed at an item semi-filter solely as an
                // item route, even when that tile also relays a fluid capability.
                if (!type.filtered() && target instanceof LogisticsSemiFilteredItem) continue;
                IFluidHandler handler = target.getCapability(ForgeCapabilities.FLUID_HANDLER,
                        face.getOpposite()).resolve().orElse(null);
                if (handler == null) continue;

                int rank = type.effectivePriority(LogisticsCoverInteraction.value(cover)) - 1;
                Endpoint endpoint = new Endpoint(targetPos.immutable(), handler, filter.copy());
                (switch (type.role()) {
                    case IMPORT -> imports;
                    case EXPORT -> exports;
                    case STORAGE -> storages;
                    default -> throw new IllegalStateException("Unsupported fluid logistics role " + type.role());
                }).get(rank).add(endpoint);
            }
        }
        return new Session(imports, exports, storages);
    }

    private static List<List<Endpoint>> ranks() {
        return List.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    /** One successful pair at most, matching one GT6 logic-cycle operation. Ranks: Generic=0, Semi=1, Filtered=2. */
    public static final class Session {
        private final List<List<Endpoint>> imports;
        private final List<List<Endpoint>> exports;
        private final List<List<Endpoint>> storages;

        private Session(List<List<Endpoint>> imports, List<List<Endpoint>> exports,
                        List<List<Endpoint>> storages) {
            this.imports = imports;
            this.exports = exports;
            this.storages = storages;
        }

        public int tryRankPair(int exportRank, int importRank, int conversion) {
            return tryPair(LogisticsCoverType.Role.IMPORT, importRank,
                    LogisticsCoverType.Role.EXPORT, exportRank, conversion);
        }

        public int tryPair(LogisticsCoverType.Role sourceRole, int sourceRank,
                           LogisticsCoverType.Role destinationRole, int destinationRank, int conversion) {
            if (conversion <= 0 || destinationRank < 0 || destinationRank >= 3
                    || sourceRank < 0 || sourceRank >= 3)
                return 0;
            int limit = (int) Math.min(Integer.MAX_VALUE, 16_000L * conversion);
            // GT6's moveFluids(list,list) walks imports first and stops on the first transfer.
            List<Endpoint> sources = (sourceRole == LogisticsCoverType.Role.IMPORT ? imports : storages).get(sourceRank);
            List<Endpoint> destinations = (destinationRole == LogisticsCoverType.Role.EXPORT ? exports : storages).get(destinationRank);
            for (Endpoint source : sources)
                for (Endpoint destination : destinations) {
                    if (source.targetPos.equals(destination.targetPos)) continue;
                    int moved = movePair(source, destination, limit);
                    if (moved > 0) return moved;
                }
            return 0;
        }
    }

    private static int movePair(Endpoint source, Endpoint destination, int limit) {
        if (!source.filter.isEmpty() && !destination.filter.isEmpty()
                && source.filter.getFluid() != destination.filter.getFluid()) return 0;

        // A fluid handler may expose several tanks. Asking it to drain the tank's full FluidStack
        // preserves NBT through both sides, while the GT6 filters compare fluid identity only.
        for (int tank = 0; tank < source.handler.getTanks(); tank++) {
            FluidStack candidate = source.handler.getFluidInTank(tank);
            if (candidate.isEmpty() || !matches(source.filter, candidate)
                    || !matches(destination.filter, candidate)) continue;
            FluidStack request = candidate.copy();
            request.setAmount(limit);
            FluidStack available = source.handler.drain(request, IFluidHandler.FluidAction.SIMULATE);
            if (available.isEmpty() || !matches(source.filter, available)
                    || !matches(destination.filter, available)) continue;
            if (available.getAmount() > limit) available.setAmount(limit);
            int accepted = destination.handler.fill(available, IFluidHandler.FluidAction.SIMULATE);
            if (accepted <= 0) continue;

            FluidStack extraction = available.copy();
            extraction.setAmount(Math.min(accepted, available.getAmount()));
            FluidStack drained = source.handler.drain(extraction, IFluidHandler.FluidAction.EXECUTE);
            if (drained.isEmpty()) continue;
            if (!matches(source.filter, drained) || !matches(destination.filter, drained)) {
                source.handler.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                continue;
            }
            // Recheck after the real drain: a handler is allowed to change between simulation and
            // execution. A reduced fill is returned to the source rather than silently discarded.
            int moved = destination.handler.fill(drained, IFluidHandler.FluidAction.EXECUTE);
            moved = Math.max(0, Math.min(moved, drained.getAmount()));
            if (moved < drained.getAmount()) {
                FluidStack remainder = drained.copy();
                remainder.setAmount(drained.getAmount() - moved);
                source.handler.fill(remainder, IFluidHandler.FluidAction.EXECUTE);
            }
            if (moved > 0) return moved;
        }
        return 0;
    }

    private static boolean matches(FluidStack filter, FluidStack candidate) {
        return filter.isEmpty() || filter.getFluid() == candidate.getFluid();
    }
}
