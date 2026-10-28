package gregtech.engine.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
*///?} else {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
//?}

/** Face auto-I/O and the item/fluid transfer helpers used by a basic machine. */
public final class MachineAutoIO {
    public interface Host {
        MachineInventory autoIOInventory();
        Direction autoIORelativeToAbsolute(int relDir);
        boolean autoIOShutters(Direction side);
        boolean autoIOAllowsItemTraffic(Direction side, ItemStack stack, boolean inserting);
        boolean autoIOAcceptsInput(ItemStack stack);
        IFluidHandler autoIOFluidOutputTarget(Level level, BlockPos pos, Direction absolute);
        BlockEntity autoIOOwner();
    }

    private final Host host;
    private final AdjacentCapabilityCache<IItemHandler> adjacentItems;
    private final AdjacentCapabilityCache<IFluidHandler> adjacentFluids;

    public MachineAutoIO(Host host) {
        this.host = host;
//? if >=1.21.1 {
/*        this.adjacentItems = new AdjacentCapabilityCache<>(host.autoIOOwner(), net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK);
        this.adjacentFluids = new AdjacentCapabilityCache<>(host.autoIOOwner(), net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK);
*///?} else {
        this.adjacentItems = new AdjacentCapabilityCache<>(host.autoIOOwner(), ForgeCapabilities.ITEM_HANDLER);
        this.adjacentFluids = new AdjacentCapabilityCache<>(host.autoIOOwner(), ForgeCapabilities.FLUID_HANDLER);
//?}
    }

    public void inputItems(Level level, BlockPos pos, int relDir) {
        MachineInventory inventory = host.autoIOInventory();
        if (inventory == null || !inventory.hasItemInputSpace()) return;
        Direction absolute = host.autoIORelativeToAbsolute(relDir);
        if (host.autoIOShutters(absolute)) return;
        BlockPos adj = pos.relative(absolute);
        if (!level.hasChunkAt(adj)) return;
        BlockEntity be = level.getBlockEntity(adj);

//? if >=1.21.1 {
/*        if (be instanceof gregtech.engine.multiblock.BoundMachinePort part && part.isBoundTo(host.autoIOOwner().getBlockPos())) return;
*///?} else {
        if (be instanceof gregtech.content.blockentity.machine.MultiblockPortBlockEntity part && part.isBoundTo(host.autoIOOwner().getBlockPos())) return;
//?}

        if (be == null) return;
        IItemHandler source = adjacentItems.get(be, absolute.getOpposite());
        if (source == null) return;
        int inputCount = inventory.items().inputCount();
        if (inputCount <= 0) return;
        for (int srcSlot = 0; srcSlot < source.getSlots(); srcSlot++) {
            ItemStack stack = source.getStackInSlot(srcSlot);
            if (stack.isEmpty()) continue;
            if (!host.autoIOAcceptsInput(stack) || !host.autoIOAllowsItemTraffic(absolute, stack, true)) continue;
            for (int i = 0; i < inputCount; i++) {
                if (!host.autoIOAllowsItemTraffic(absolute, stack, true)) break;
                ItemStack remaining = inventory.items().insertItem(i, stack.copy(), true);
                int toTake = stack.getCount() - remaining.getCount();
                if (toTake <= 0) continue;
                ItemStack extracted = source.extractItem(srcSlot, toTake, false);
                if (!extracted.isEmpty()) {
                    ItemStack leftover = inventory.items().insertItem(i, extracted, false);
                    returnOrDrop(leftover, source);
                    if (source.getStackInSlot(srcSlot).isEmpty()) break;
                    stack = source.getStackInSlot(srcSlot);
                }
            }
        }
    }

    public void outputItems(Level level, BlockPos pos, int relDir) {
        MachineInventory inventory = host.autoIOInventory();
        if (inventory == null || !inventory.hasItemOutput()) return;
        Direction absolute = host.autoIORelativeToAbsolute(relDir);
        if (host.autoIOShutters(absolute)) return;
        BlockPos adj = pos.relative(absolute);
        if (!level.hasChunkAt(adj)) return;
        BlockEntity be = level.getBlockEntity(adj);

//? if >=1.21.1 {
/*        if (be instanceof gregtech.engine.multiblock.BoundMachinePort part && part.isBoundTo(host.autoIOOwner().getBlockPos())) return;
        IItemHandler target = be != null ? adjacentItems.get(be, absolute.getOpposite()) : null;
*///?} else {
        if (be instanceof gregtech.content.blockentity.machine.MultiblockPortBlockEntity part && part.isBoundTo(host.autoIOOwner().getBlockPos())) return;
        IItemHandler target = be != null ? adjacentItems.get(be, absolute.getOpposite()) : null;
//?}


        int firstOutputSlot = inventory.items().inputCount();
        int totalSlots = inventory.items().getSlots();

        if (target != null) {
            pushOutputItems(target, absolute);
        } else if (level.isEmptyBlock(adj)) {
            // Drop into air like a hopper
            for (int i = firstOutputSlot; i < totalSlots; i++) {
                ItemStack offered = inventory.items().getStackInSlot(i);
                if (!host.autoIOAllowsItemTraffic(absolute, offered, false)) continue;
                ItemStack extracted = inventory.items().extractItem(i, offered.getCount(), false);
                if (extracted.isEmpty()) continue;
                double x = adj.getX() + 0.5;
                double y = adj.getY() + 0.5;
                double z = adj.getZ() + 0.5;
                double vx = absolute.getStepX() * 0.15;
                double vy = 0.1;
                double vz = absolute.getStepZ() * 0.15;
                ItemEntity entity = new ItemEntity(level, x, y, z, extracted);
                entity.setDeltaMovement(vx, vy, vz);
                entity.setPickUpDelay(10);
                level.addFreshEntity(entity);
            }
        }
    }

    public void inputFluids(Level level, BlockPos pos, int relDir) {
        MachineInventory inventory = host.autoIOInventory();
        if (inventory == null || !inventory.hasFluidInputSpace()) return;
        Direction absolute = host.autoIORelativeToAbsolute(relDir);
        if (host.autoIOShutters(absolute)) return;
        BlockPos adj = pos.relative(absolute);
        if (!level.hasChunkAt(adj)) return;
        BlockEntity be = level.getBlockEntity(adj);

//? if >=1.21.1 {
/*        if (be instanceof gregtech.engine.multiblock.BoundMachinePort part && part.isBoundTo(host.autoIOOwner().getBlockPos())) return;
*///?} else {
        if (be instanceof gregtech.content.blockentity.machine.MultiblockPortBlockEntity part && part.isBoundTo(host.autoIOOwner().getBlockPos())) return;
//?}

        if (be == null) return;
        IFluidHandler source = adjacentFluids.get(be, absolute.getOpposite());
        if (source == null) return;
        FluidStack drained = source.drain(1000, IFluidHandler.FluidAction.SIMULATE);
        if (drained.isEmpty()) return;
        int filled = inventory.fill(drained, IFluidHandler.FluidAction.SIMULATE);
        if (filled <= 0) return;
        FluidStack toDrain = drained.copy();
        toDrain.setAmount(filled);
        FluidStack actuallyDrained = source.drain(toDrain, IFluidHandler.FluidAction.EXECUTE);
        if (!actuallyDrained.isEmpty()) {
            inventory.fill(actuallyDrained, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    public void outputFluids(Level level, BlockPos pos, int relDir) {
        MachineInventory inventory = host.autoIOInventory();
        if (inventory == null || !inventory.hasFluidOutput()) return;
        Direction absolute = host.autoIORelativeToAbsolute(relDir);
        if (host.autoIOShutters(absolute)) return;
        IFluidHandler target = host.autoIOFluidOutputTarget(level, pos, absolute);
        if (target == null) return;
        FluidStack drained = inventory.drain(1000, IFluidHandler.FluidAction.SIMULATE);
        if (drained.isEmpty()) return;
        int filled = target.fill(drained, IFluidHandler.FluidAction.SIMULATE);
        if (filled <= 0) return;
        FluidStack offered = drained.copy();
        offered.setAmount(Math.min(filled, drained.getAmount()));
        int accepted = target.fill(offered.copy(), IFluidHandler.FluidAction.EXECUTE);
        if (accepted < 0 || accepted > offered.getAmount())
            throw new IllegalStateException("Fluid handler returned an invalid accepted amount: " + accepted);
        if (accepted > 0) {
            offered.setAmount(accepted);
            inventory.drain(offered, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    /** Deduct only deliveries confirmed by the shared transfer engine. */
    public void pushOutputItems(IItemHandler target, Direction side) {
        MachineInventory inventory = host.autoIOInventory();
        if (inventory == null) return;
        for (int slot = inventory.items().inputCount(); slot < inventory.items().getSlots(); slot++) {
            ItemStack offered = inventory.items().getStackInSlot(slot);
            if (offered.isEmpty() || !host.autoIOAllowsItemTraffic(side, offered, false)) continue;
            var result = gregtech.content.transport.ItemPipeTransferAdapter.transfer(
                    offered.copy(), () -> target);
            if (result.accepted() > 0) inventory.items().extractItem(slot, result.accepted(), false);
        }
    }

    /** A changed source/target must not make an already extracted item disappear. */
    public void returnOrDrop(ItemStack leftover, IItemHandler origin) {
        if (leftover.isEmpty()) return;
        var returned = gregtech.content.transport.ItemPipeTransferAdapter.transfer(
                leftover.copy(), () -> origin);
        ItemStack remainder = leftover.copyWithCount(leftover.getCount() - returned.accepted());
        if (!remainder.isEmpty()) {
            BlockEntity owner = host.autoIOOwner();
            Block.popResource(owner.getLevel(), owner.getBlockPos(), remainder);
        }
    }

    /** Default target lookup; controllers may override the host method to pick another face. */
    public IFluidHandler defaultFluidOutputTarget(Level level, BlockPos pos, Direction absolute) {
        BlockPos adj = pos.relative(absolute);
        if (!level.hasChunkAt(adj)) return null;
        BlockEntity be = level.getBlockEntity(adj);

//? if >=1.21.1 {
/*        if (be instanceof gregtech.engine.multiblock.BoundMachinePort part && part.isBoundTo(host.autoIOOwner().getBlockPos())) return null;
*///?} else {
        if (be instanceof gregtech.content.blockentity.machine.MultiblockPortBlockEntity part && part.isBoundTo(host.autoIOOwner().getBlockPos())) return null;
//?}

        if (be == null) return null;
        return adjacentFluids.get(be, absolute.getOpposite());
    }
}
