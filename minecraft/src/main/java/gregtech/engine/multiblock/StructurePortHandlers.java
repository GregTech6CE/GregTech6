package gregtech.engine.multiblock;

import gregtech.api.machine.MachineFace;
import gregtech.api.multiblock.*;
import gregtech.content.multiblock.LargeMachineLayouts;
import gregtech.engine.multiblock.tracking.TrackedMultiblock;
import gregtech.engine.stack.StackCompat;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
//?}

/** Stable capability views resolve the current owner/rules on every call, including after reload. */
public final class StructurePortHandlers {
    private StructurePortHandlers() {}

    /** Returns accepted packets; the controller retains responsibility for its energy accounting. */
    public static long emitEnergy(TrackedMultiblock machine, gregtech.content.data.GregTechTags.Tag type, long size, long amount) {
        var entity = machine.trackedEntity();
        var tracker = gregtech.engine.multiblock.tracking.StructureTracking.tracker(entity);
        if (tracker == null || entity.isRemoved() || amount <= 0) return 0;
        long sent = 0;
        for (var position : tracker.energyOutputs()) {
            for (var side : net.minecraft.core.Direction.values()) {
                sent += emitEnergy(machine, position, side, type, size, amount - sent);
                if (sent == amount) return sent;
            }
        }
        return sent;
    }

    /** Explicit face variant lets native machines retain their GT6 output ordering. */
    public static long emitEnergy(TrackedMultiblock machine, net.minecraft.core.BlockPos position,
                                  net.minecraft.core.Direction side, gregtech.content.data.GregTechTags.Tag type,
                                  long size, long amount) {
        var entity = machine.trackedEntity();
        var level = entity.getLevel();
        if (entity.isRemoved() || level == null || level.isClientSide || !level.hasChunkAt(position) || amount <= 0) return 0;
        if (!(level.getBlockEntity(position) instanceof gregtech.content.blockentity.machine.MultiblockPortBlockEntity part)
                || !part.isBoundTo(entity.getBlockPos()) || !part.isEnergyEmittingTo(type, side, false)) return 0;
        var target = position.relative(side);
        if (!level.hasChunkAt(target)) return 0;
        var receiver = level.getBlockEntity(target);
        if (receiver == null || receiver == entity
                || receiver instanceof gregtech.content.blockentity.machine.MultiblockPortBlockEntity other
                    && other.isBoundTo(entity.getBlockPos())) return 0;
        // A passive casing does not acquire an independent generator's environmental checks.
        return Math.max(0, Math.min(amount, gregtech.engine.energy.EnergyTransfer.insertEnergyInto(
                type, side.getOpposite(), size, amount, entity, receiver)));
    }

    public record View(MultiblockPortOwner owner, MultiblockLayout.Role role, StructurePorts ports, MachineFace face) {
        boolean face(PortChannel channel) { return role.allows(channel) && ports.rule(channel).allowsFace(face); }
        boolean slot(PortChannel channel, int slot) { return face(channel) && ports.rule(channel).allowsSlot(slot); }
        IItemHandler items() { return owner.portItems(role, face); }
        IFluidHandler fluids() { return owner.portFluids(role, face); }
    }

    public static IItemHandler items(Supplier<View> current) {
        return new IItemHandler() {
            public int getSlots() {
                var v = current.get();
                var h = v == null ? null : v.items();
                return h == null || !v.face(PortChannel.ITEM_INPUT) && !v.face(PortChannel.ITEM_OUTPUT) ? 0 : h.getSlots();
            }
            public ItemStack getStackInSlot(int slot) {
                var v = current.get(); var h = v == null ? null : v.items();
                return valid(h, slot) && (v.slot(PortChannel.ITEM_INPUT, slot) || v.slot(PortChannel.ITEM_OUTPUT, slot))
                        ? h.getStackInSlot(slot).copy() : ItemStack.EMPTY;
            }
            public int getSlotLimit(int slot) {
                var v = current.get(); var h = v == null ? null : v.items();
                return valid(h, slot) && (v.slot(PortChannel.ITEM_INPUT, slot) || v.slot(PortChannel.ITEM_OUTPUT, slot))
                        ? h.getSlotLimit(slot) : 0;
            }
            public boolean isItemValid(int slot, ItemStack stack) {
                var v = current.get(); var h = v == null ? null : v.items();
                return valid(h, slot) && v.slot(PortChannel.ITEM_INPUT, slot)
                        && matches(v.ports.rule(PortChannel.ITEM_INPUT), stack) && h.isItemValid(slot, stack);
            }
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                var v = current.get(); var h = v == null ? null : v.items();
                return valid(h, slot) && v.slot(PortChannel.ITEM_INPUT, slot)
                        && matches(v.ports.rule(PortChannel.ITEM_INPUT), stack) ? h.insertItem(slot, stack, simulate) : stack;
            }
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                var v = current.get(); var h = v == null ? null : v.items();
                if (amount <= 0 || !valid(h, slot) || !v.slot(PortChannel.ITEM_OUTPUT, slot)) return ItemStack.EMPTY;
                var preview = h.extractItem(slot, amount, true);
                if (!matches(v.ports.rule(PortChannel.ITEM_OUTPUT), preview)) return ItemStack.EMPTY;
                return simulate ? preview : h.extractItem(slot, amount, false);
            }
        };
    }

    public static IFluidHandler fluids(Supplier<View> current) {
        return new IFluidHandler() {
            public int getTanks() {
                var v = current.get(); var h = v == null ? null : v.fluids();
                return h == null || !v.face(PortChannel.FLUID_INPUT) && !v.face(PortChannel.FLUID_OUTPUT) ? 0 : h.getTanks();
            }
            public FluidStack getFluidInTank(int tank) {
                var v = current.get(); var h = v == null ? null : v.fluids();
                return visible(v, h, tank) ? h.getFluidInTank(tank).copy() : FluidStack.EMPTY;
            }
            public int getTankCapacity(int tank) {
                var v = current.get(); var h = v == null ? null : v.fluids();
                return visible(v, h, tank) ? h.getTankCapacity(tank) : 0;
            }
            public boolean isFluidValid(int tank, FluidStack stack) {
                var v = current.get(); var h = v == null ? null : v.fluids();
                return valid(h, tank) && v.slot(PortChannel.FLUID_INPUT, tank)
                        && matches(v.ports.rule(PortChannel.FLUID_INPUT), stack) && h.isFluidValid(tank, stack);
            }
            public int fill(FluidStack stack, FluidAction action) {
                var v = current.get(); var h = v == null ? null : v.fluids();
                if (h == null || !v.face(PortChannel.FLUID_INPUT) || !matches(v.ports.rule(PortChannel.FLUID_INPUT), stack)) return 0;
                var rule = v.ports.rule(PortChannel.FLUID_INPUT);
                if (rule.slots().isEmpty()) return h.fill(stack, action);
                int filled = 0;
                for (int tank : rule.slots()) {
                    if (!valid(h, tank)) continue;
                    var single = v.owner.portFluidTank(v.role, tank, v.face);
                    if (single == null) continue;
                    var offer = stack.copy();
                    offer.setAmount(stack.getAmount() - filled);
                    if (!h.isFluidValid(tank, offer)) continue;
                    filled += single.fill(offer, action);
                    if (filled == stack.getAmount()) break;
                }
                return filled;
            }
            public FluidStack drain(FluidStack stack, FluidAction action) {
                if (stack == null || stack.isEmpty()) return FluidStack.EMPTY;
                return drainSelected(stack, stack.getAmount(), action);
            }
            public FluidStack drain(int amount, FluidAction action) { return drainSelected(null, amount, action); }
            private FluidStack drainSelected(FluidStack requested, int amount, FluidAction action) {
                var v = current.get(); var h = v == null ? null : v.fluids();
                if (h == null || amount <= 0 || !v.face(PortChannel.FLUID_OUTPUT)) return FluidStack.EMPTY;
                var rule = v.ports.rule(PortChannel.FLUID_OUTPUT);
                if (requested != null && !matches(rule, requested)) return FluidStack.EMPTY;
                if (rule.slots().isEmpty() && rule.filters().isEmpty())
                    return requested == null ? h.drain(amount, action) : h.drain(requested, action);
                FluidStack result = FluidStack.EMPTY;
                for (int tank = 0; tank < h.getTanks(); tank++) {
                    if (!rule.allowsSlot(tank)) continue;
                    var content = h.getFluidInTank(tank);
                    if (content.isEmpty() || !matches(rule, content)) continue;
                    var match = requested != null ? requested : result;
                    if (!match.isEmpty() && !sameFluid(match, content)) continue;
                    var single = v.owner.portFluidTank(v.role, tank, v.face);
                    if (single == null) continue;
                    var offer = requested == null ? content.copy() : requested.copy();
                    offer.setAmount(amount - result.getAmount());
                    var drained = single.drain(offer, action);
                    if (drained.isEmpty()) continue;
                    if (result.isEmpty()) result = drained.copy();
                    else result.grow(drained.getAmount());
                    if (result.getAmount() == amount) break;
                }
                return result;
            }
        };
    }

    private static boolean valid(IItemHandler h, int slot) { return h != null && slot >= 0 && slot < h.getSlots(); }
    private static boolean sameFluid(FluidStack first, FluidStack second) {
        return StackCompat.sameFluid(first, second);
    }
    private static boolean valid(IFluidHandler h, int tank) { return h != null && tank >= 0 && tank < h.getTanks(); }
    private static boolean visible(View v, IFluidHandler h, int tank) {
        return valid(h, tank) && (v.slot(PortChannel.FLUID_INPUT, tank) || v.slot(PortChannel.FLUID_OUTPUT, tank));
    }
    static boolean matches(PortRule rule, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (rule.filters().isEmpty()) return true;
        for (String filter : rule.filters()) {
            if (filter.startsWith("#")) {
                if (stack.is(TagKey.create(Registries.ITEM, ResourceLocation.parse(filter.substring(1))))) return true;
            } else if (BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ResourceLocation.parse(filter))) return true;
        }
        return false;
    }
    static boolean matches(PortRule rule, FluidStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (rule.filters().isEmpty()) return true;
        for (String filter : rule.filters()) {
            if (filter.startsWith("#")) {
                if (stack.getFluid().defaultFluidState().is(TagKey.create(Registries.FLUID, ResourceLocation.parse(filter.substring(1))))) return true;
            } else if (BuiltInRegistries.FLUID.getKey(stack.getFluid()).equals(ResourceLocation.parse(filter))) return true;
        }
        return false;
    }

    /** Rejected during declaration compilation/reload, not silently ignored by a pipe at runtime. */
    public static void validate(TrackedMultiblock machine, LargeMachineLayouts.Cell cell) {
        if (cell.ports().rules().isEmpty()) return;
        cell.ports().validate(gregtech.api.multiblock.StructureRole.valueOf(cell.role().name()));
        if (!(machine instanceof MultiblockPortOwner owner)) throw new IllegalArgumentException("Controller has no transport ports");
        var part = cell.block();
        var entity = part instanceof net.minecraft.world.level.block.EntityBlock source
                ? source.newBlockEntity(net.minecraft.core.BlockPos.ZERO, part.defaultBlockState()) : null;
        if (entity instanceof gregtech.content.blockentity.machine.AddonEnergyPartBlockEntity
                || entity instanceof gregtech.content.blockentity.machine.AddonFunctionalPartBlockEntity)
            throw new IllegalArgumentException("Per-cell ports address controller storage; use LayoutPart rules for a part's own storage");
        if (!(entity instanceof gregtech.content.blockentity.machine.MultiblockPortBlockEntity)
                || cell.plain() != null && cell.plain().startsWith("#"))
            throw new IllegalArgumentException("Per-cell ports require an explicit controller-proxy part: " + cell);
        for (var entry : cell.ports().rules().entrySet()) {
            var channel = entry.getKey(); var rule = entry.getValue();
            if (channel.energy()) {
                if (!rule.faces().isEmpty() && owner.portEnergyTypes(cell.role()).stream().noneMatch(type ->
                        channel == PortChannel.ENERGY_OUTPUT ? owner.emitsPortEnergy(cell.role(), type)
                                : owner.acceptsPortEnergy(cell.role(), type)))
                    throw new IllegalArgumentException("Controller has no transport for " + channel);
                continue;
            }
            boolean fluid = channel == PortChannel.FLUID_INPUT || channel == PortChannel.FLUID_OUTPUT;
            var f = fluid ? owner.portFluids(cell.role()) : null;
            var i = fluid ? null : owner.portItems(cell.role());
            int count = fluid ? f == null ? 0 : f.getTanks() : i == null ? 0 : i.getSlots();
            if (count == 0 && !rule.faces().isEmpty())
                throw new IllegalArgumentException("Controller has no storage for " + channel);
            for (int slot : rule.slots())
                if (slot >= count) throw new IllegalArgumentException(channel + " slot " + slot + " outside handler size " + count);
            if (fluid && (!rule.slots().isEmpty() || channel == PortChannel.FLUID_OUTPUT && !rule.filters().isEmpty()))
                for (int tank = 0; tank < count; tank++)
                    if (rule.allowsSlot(tank) && owner.portFluidTank(cell.role(), tank) == null)
                        throw new IllegalArgumentException("Controller cannot address tank " + tank + " separately for " + channel);
            for (String filter : rule.filters()) {
                if (filter.startsWith("#")) continue; // Tags are populated by the subsequent data reload.
                var id = ResourceLocation.parse(filter);
                if (!(fluid ? BuiltInRegistries.FLUID.containsKey(id) : BuiltInRegistries.ITEM.containsKey(id)))
                    throw new IllegalArgumentException("Unknown " + channel + " filter " + id);
            }
        }
    }
}
