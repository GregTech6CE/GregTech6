package gregtech.engine.multiblock;

import gregtech.api.machine.MachineFace;
import gregtech.api.multiblock.PortChannel;
import gregtech.api.multiblock.PortRule;
import gregtech.content.blockentity.machine.MultiblockPortBlockEntity;
import gregtech.content.data.GregTechTags;
import gregtech.content.multiblock.LargeMachineLayouts;
import gregtech.content.transport.ItemPipeTransferAdapter;
import gregtech.content.cover.ComponentCoverAccess;
import gregtech.content.cover.PanelCoverHost;
import gregtech.engine.machine.MachineAutoIO;
import gregtech.engine.multiblock.tracking.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
//?}

/** Active transfers use the same per-cell filters as passive capabilities, without caching neighbors. */
public final class StructureOutputs {
    private StructureOutputs() {}

    public record Outlet(BlockPos target, Direction receiver, PortRule rule, PortRule cellRule) {
        boolean slot(int slot) { return rule.allowsSlot(slot) && cellRule.allowsSlot(slot); }
    }

    /** Adapts an existing machine IO declaration to an absolute outlet without another coordinate convention. */
    public static StructureOutput at(TrackedMultiblock machine, PortChannel channel, BlockPos target,
            Direction receiver, boolean buffered) {
        Direction front = LayoutStructures.facing(machine.trackedEntity());
        Direction horizontal = front.getAxis().isHorizontal() ? front : Direction.NORTH;
        var delta = target.subtract(machine.trackedEntity().getBlockPos());
        var right = horizontal.getClockWise();
        var back = horizontal.getOpposite();
        var io = front.getAxis().isHorizontal()
                ? gregtech.api.machine.IoTarget.at(delta.getX() * right.getStepX() + delta.getZ() * right.getStepZ(),
                    delta.getY(), delta.getX() * back.getStepX() + delta.getZ() * back.getStepZ(), face(horizontal, receiver))
                : gregtech.api.machine.IoTarget.at(delta.getX(), -delta.getZ(), -delta.getY() * front.getStepY(), face(horizontal, receiver));
        return buffered ? StructureOutput.buffered(channel, io) : StructureOutput.port(channel, io);
    }

    public static Direction direction(Direction front, MachineFace side) {
        return switch (side) {
            case BOTTOM -> Direction.DOWN;
            case TOP -> Direction.UP;
            case FRONT -> front;
            case BACK -> front.getOpposite();
            case LEFT -> front.getAxis().isHorizontal() ? front.getClockWise() : Direction.EAST;
            case RIGHT -> front.getAxis().isHorizontal() ? front.getCounterClockWise() : Direction.WEST;
        };
    }

    public static MachineFace face(Direction front, Direction side) {
        if (side == Direction.DOWN) return MachineFace.BOTTOM;
        if (side == Direction.UP) return MachineFace.TOP;
        if (!front.getAxis().isHorizontal()) front = Direction.NORTH;
        if (side == front) return MachineFace.FRONT;
        if (side == front.getOpposite()) return MachineFace.BACK;
        return side == front.getClockWise() ? MachineFace.LEFT : MachineFace.RIGHT;
    }

    /** A buffered outlet is explicitly declared by GT6; it does not make the wall a passive port. */
    public static Outlet resolve(TrackedMultiblock machine, StructureOutput output) {
        var entity = machine.trackedEntity();
        var level = entity.getLevel();
        if (entity.isRemoved() || level == null || level.isClientSide || !level.hasChunkAt(entity.getBlockPos())) return null;
        var tracker = StructureTracking.tracker(entity);
        if (output.requiresFormed() && (tracker == null || !tracker.formed())) return null;
        Direction front = LayoutStructures.facing(entity);
        var t = output.target();
        BlockPos target = gregtech.content.multiblock.AxialStructureTransform.at(
                entity.getBlockPos(), front, t.right(), t.up(), t.back());
        Direction receiver = direction(front, t.receiver());
        BlockPos source = target.relative(receiver);
        if (!level.hasChunkAt(source) || !level.hasChunkAt(target)) return null;
        BlockEntity neighbor = level.getBlockEntity(target);
        if (neighbor == entity || neighbor instanceof BoundMachinePort part && part.isBoundTo(entity.getBlockPos())) return null;
        var sourceEntity = level.getBlockEntity(source);
        if (output.channel() == PortChannel.ITEM_OUTPUT || output.channel() == PortChannel.FLUID_OUTPUT) {
            Direction side = receiver.getOpposite();
            if (sourceEntity instanceof MachineAutoIO.Host host && host.autoIOShutters(side)) return null;
            if (sourceEntity instanceof PanelCoverHost host && host.panels().shuttered(side)) return null;
        }
        var cell = cellAt(machine, source);
        if (output.requiresPort() && !source.equals(entity.getBlockPos())) {
            if (cell == null || !cell.role().allows(output.channel())
                    || !(level.getBlockEntity(source) instanceof MultiblockPortBlockEntity part)
                    || !part.isBoundTo(entity.getBlockPos())) return null;
        }
        var rule = cell == null ? PortRule.ALL : cell.ports().rule(output.channel());
        MachineFace from = face(front, receiver.getOpposite());
        if (!output.rule().allowsFace(from) || !rule.allowsFace(from)) return null;
        return new Outlet(target, receiver, output.rule(), rule);
    }

    private static LargeMachineLayouts.Cell cellAt(TrackedMultiblock machine, BlockPos source) {
        var pattern = StructurePlan.compile(machine);
        if (pattern == null) return null;
        var offset = source.subtract(machine.trackedEntity().getBlockPos());
        LargeMachineLayouts.Cell[] found = {null};
        pattern.locate(offset.getX(), offset.getY(), offset.getZ(), (repeat, index, copy) -> {
            if (!repeat || copy < pattern.groupLimit(pattern.repeatGroup(index), machine.trackedEntity().getLevel().getHeight()))
                found[0] = repeat ? pattern.declaredRepeatCell(index) : pattern.declaredCell(index);
        });
        return found[0];
    }

    public static IFluidHandler fluidTarget(TrackedMultiblock machine, StructureOutput output) {
        Outlet outlet = resolve(machine, output);
        return outlet == null ? null : fluids(machine, outlet);
    }

    private static IFluidHandler fluids(TrackedMultiblock machine, Outlet outlet) {
        var level = machine.trackedEntity().getLevel();
//? if >=1.21.1 {
        /*return level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, outlet.target(), outlet.receiver());
*///?} else {
        var target = level.getBlockEntity(outlet.target());
        return target == null ? null : target.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER,
                outlet.receiver()).resolve().orElse(null);
//?}
    }

    /** The supplied tank is the controller's physical tank; the index is in its port's index space. */
    public static int fluid(TrackedMultiblock machine, StructureOutput output, int tankIndex,
            IFluidHandler tank, int maximum) {
        if (output.channel() != PortChannel.FLUID_OUTPUT) throw new IllegalArgumentException("Not a fluid outlet");
        Outlet outlet = resolve(machine, output);
        if (outlet == null || maximum <= 0 || !outlet.slot(tankIndex)) return 0;
        var source = machine.trackedEntity().getLevel().getBlockEntity(outlet.target().relative(outlet.receiver()));
        if (source instanceof PanelCoverHost host)
            tank = ComponentCoverAccess.fluids(host, outlet.receiver().getOpposite(), tank);
        FluidStack offered = tank.drain(maximum, IFluidHandler.FluidAction.SIMULATE);
        if (!StructurePortHandlers.matches(outlet.rule(), offered) || !StructurePortHandlers.matches(outlet.cellRule(), offered)) return 0;
        var target = fluids(machine, outlet);
        if (target == null) return 0;
        int capacity = target.fill(offered.copy(), IFluidHandler.FluidAction.SIMULATE);
        if (capacity <= 0) return 0;
        offered.setAmount(Math.min(capacity, offered.getAmount()));
        int accepted = target.fill(offered.copy(), IFluidHandler.FluidAction.EXECUTE);
        if (accepted < 0 || accepted > offered.getAmount()) throw new IllegalStateException("Invalid fluid receiver amount: " + accepted);
        if (accepted > 0) {
            offered.setAmount(accepted);
            tank.drain(offered, IFluidHandler.FluidAction.EXECUTE);
            machine.trackedEntity().setChanged();
        }
        return accepted;
    }

    public static int items(TrackedMultiblock machine, StructureOutput output, IItemHandler source, int firstSlot) {
        if (output.channel() != PortChannel.ITEM_OUTPUT) throw new IllegalArgumentException("Not an item outlet");
        Outlet outlet = resolve(machine, output);
        if (outlet == null) return 0;
        var level = machine.trackedEntity().getLevel();
        var sourceEntity = level.getBlockEntity(outlet.target().relative(outlet.receiver()));
        Direction side = outlet.receiver().getOpposite();
        if (sourceEntity instanceof PanelCoverHost host) source = ComponentCoverAccess.items(host, side, source);
        int moved = 0;
        for (int slot = firstSlot; slot < source.getSlots(); slot++) {
            if (!outlet.slot(slot)) continue;
            var offer = source.extractItem(slot, Integer.MAX_VALUE, true);
            if (!StructurePortHandlers.matches(outlet.rule(), offer) || !StructurePortHandlers.matches(outlet.cellRule(), offer)) continue;
            if (sourceEntity instanceof MachineAutoIO.Host host && !host.autoIOAllowsItemTraffic(side, offer, false)) continue;
            if (output.dropIntoAir() && level.isEmptyBlock(outlet.target())) {
                var extracted = source.extractItem(slot, offer.getCount(), false);
                if (!extracted.isEmpty()) {
                    MachineAutoIO.dropOutput(level, outlet.target(), side, extracted);
                    moved += extracted.getCount();
                }
                continue;
            }
            var result = ItemPipeTransferAdapter.transfer(offer, () -> {
                if (resolve(machine, output) == null) return null;
//? if >=1.21.1 {
                /*return level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                        outlet.target(), outlet.receiver());
*///?} else {
                var neighbor = level.getBlockEntity(outlet.target());
                return neighbor == null ? null : neighbor.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER,
                        outlet.receiver()).resolve().orElse(null);
//?}
            });
            if (result.accepted() > 0) {
                source.extractItem(slot, result.accepted(), false);
                moved += result.accepted();
            }
        }
        if (moved > 0) machine.trackedEntity().setChanged();
        return moved;
    }

    /** Energy accounting (including deliberate GT6 dissipation) remains with the converter. */
    public static long energy(TrackedMultiblock machine, StructureOutput output, GregTechTags.Tag type, long size, long packets) {
        if (output.channel() != PortChannel.ENERGY_OUTPUT) throw new IllegalArgumentException("Not an energy outlet");
        Outlet outlet = resolve(machine, output);
        if (outlet == null || packets <= 0) return 0;
        var entity = machine.trackedEntity();
        var receiver = entity.getLevel().getBlockEntity(outlet.target());
        if (receiver == null) return 0;
        var source = outlet.target().relative(outlet.receiver());
        if (output.requiresPort() && !source.equals(entity.getBlockPos()))
            return StructurePortHandlers.emitEnergy(machine, source, outlet.receiver().getOpposite(), type, size, packets);
        return Math.max(0, Math.min(packets, gregtech.engine.energy.EnergyTransfer.insertEnergyInto(
                type, outlet.receiver(), size, packets, entity, receiver)));
    }
}
