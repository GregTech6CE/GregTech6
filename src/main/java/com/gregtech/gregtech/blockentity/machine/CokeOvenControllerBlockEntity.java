package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.fluid.FluidTankGT;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTMultiblocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * Coke oven multiblock controller. 3x3x3 hollow of coke oven bricks,
 * heated by HU. Processes coal/wood into coke/charcoal and creosote oil.
 */
public class CokeOvenControllerBlockEntity extends GTEnergyBlockEntity implements com.gregtech.gregtech.api.inventory.BlockContents, com.gregtech.gregtech.api.multiblock.StructureController {

    static final long HEAT_CAPACITY = 32_000;
    static final long HU_PER_OP = 512;
    static final long TICKS_PER_OP = 200;

    long heat;
    long progress;
    final FluidTankGT outputTank = new FluidTankGT(16_000).setOnChanged(this::setChanged);
    final ItemStackHandler items = new ItemStackHandler(2);

    long lastCheckTime;
    boolean structureOk;

    @Nullable LazyOptional<IFluidHandler> fluidCap;
    @Nullable LazyOptional<IItemHandler> itemCap;

    public CokeOvenControllerBlockEntity(BlockPos pos, BlockState state) {
        super(com.gregtech.gregtech.registry.GTBlockEntities.COKE_OVEN.get(), pos, state);
    }

    public boolean isStructureOk() {
        if (level == null) return false;
        long now = level.getGameTime();
        if (lastCheckTime != 0 && now - lastCheckTime < 40) return structureOk;
        lastCheckTime = now;
        structureOk = validate();
        return structureOk;
    }

    private boolean validate() {
        return com.gregtech.gregtech.content.multiblock.ControllerStructureLayouts.matches(this);
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    public static void serverTick(Level level, BlockPos pos, BlockState state, CokeOvenControllerBlockEntity be) {
        if (!be.isStructureOk()) return;
        long now = level.getGameTime();
        if (now % 20 == 0) { // process once per second
            be.tickCooking(level, pos);
        }
        be.pushCreosote(level);
    }

    void tickCooking(Level level, BlockPos pos) {
        ItemStack input = items.getStackInSlot(0);
        if (input.isEmpty() || heat < HU_PER_OP) return;

        // Check output slot
        ItemStack output = items.getStackInSlot(1);
        if (output.getCount() >= output.getMaxStackSize()) return;

        ItemStack result = ItemStack.EMPTY;
        int creosoteMb = 0;

        if (input.is(Items.COAL)) {
            result = com.gregtech.gregtech.registry.GTItems.getStack(
                    com.gregtech.gregtech.data.MaterialPrefix.gem,
                    com.gregtech.gregtech.content.material.Materials.CoalCoke, 1);
            creosoteMb = 500;
        } else if (isLogOrWood(input)) {
            result = new ItemStack(Items.CHARCOAL);
            creosoteMb = 100;
        }

        if (result.isEmpty() && creosoteMb == 0) return;

        if (!output.isEmpty() && !ItemStack.isSameItem(output, result)) return;

        var creosote = com.gregtech.gregtech.registry.GTFluids.still("Oil_Creosote");
        if (creosoteMb > 0 && (creosote == null || !creosote.isPresent()
                || outputTank.fill(new FluidStack(creosote.get(), creosoteMb),
                IFluidHandler.FluidAction.SIMULATE) != creosoteMb)) return;

        heat -= HU_PER_OP;
        input.shrink(1);
        if (output.isEmpty()) items.setStackInSlot(1, result);
        else output.grow(1);

        if (creosoteMb > 0) {
            outputTank.fill(new FluidStack(creosote.get(), creosoteMb), IFluidHandler.FluidAction.EXECUTE);
        }
        setChanged();
    }

    static boolean isLogOrWood(ItemStack stack) {
        String name = stack.getDescriptionId();
        return name.contains("log") || name.contains("wood")
                || name.contains("plank") || name.contains("_wood");
    }

    void pushCreosote(Level level) {
        if (outputTank.isEmpty() || level == null) return;
        for (Direction dir : Direction.values()) {
            if (outputTank.isEmpty()) break;
            var be = level.getBlockEntity(worldPosition.relative(dir));
            if (be == null) continue;
            IFluidHandler target = be.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite())
                    .resolve().orElse(null);
            if (target == null) continue;
            FluidStack offer = outputTank.drain(1000, IFluidHandler.FluidAction.SIMULATE);
            if (offer.isEmpty()) continue;
            int filled = target.fill(offer, IFluidHandler.FluidAction.SIMULATE);
            if (filled <= 0) continue;
            FluidStack moved = outputTank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
            if (!moved.isEmpty()) target.fill(moved, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    // ── HU acceptance ────────────────────────────────────────────────────────

    @Override public boolean isEnergyType(GregTechTags.Tag e, @Nullable Direction side, boolean emitting) {
        return !emitting && e == GregTechTags.Energy.HU;
    }
    @Override public java.util.Collection<GregTechTags.Tag> getEnergyTypes(@Nullable Direction side) {
        return List.of(GregTechTags.Energy.HU);
    }
    @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag e, @Nullable Direction side, boolean th) { return e == GregTechTags.Energy.HU; }
    @Override public boolean isEnergyEmittingTo(GregTechTags.Tag e, @Nullable Direction side, boolean th) { return false; }
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag e, @Nullable Direction s) { return e == GregTechTags.Energy.HU ? 512 : 0; }
    @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag e, @Nullable Direction s) { return 0; }
    @Override public long getEnergyOffered(GregTechTags.Tag e, @Nullable Direction s, long size) { return 0; }
    @Override public long getEnergyDemanded(GregTechTags.Tag e, @Nullable Direction s, long size) {
        if (e != GregTechTags.Energy.HU || size <= 0) return 0;
        return Math.max(0, HEAT_CAPACITY - heat) / size;
    }
    @Override public long doInject(GregTechTags.Tag e, @Nullable Direction s, long size, long amount, boolean doInject) {
        if (e != GregTechTags.Energy.HU || amount <= 0 || size <= 0) return 0;
        long space = Math.max(0, HEAT_CAPACITY - heat);
        long accepted = Math.min(amount * size, space) / size;
        if (doInject && accepted > 0) { heat += accepted * size; setChanged(); }
        return accepted;
    }
    @Override public long getEnergyStored(GregTechTags.Tag e, @Nullable Direction s) { return e == GregTechTags.Energy.HU ? heat : 0; }
    @Override public long getEnergyCapacity(GregTechTags.Tag e, @Nullable Direction s) { return e == GregTechTags.Energy.HU ? HEAT_CAPACITY : 0; }

    // ── Capabilities ─────────────────────────────────────────────────────────

    @Override
    public <T> @org.jetbrains.annotations.NotNull LazyOptional<T> getCapability(
            @org.jetbrains.annotations.NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            if (fluidCap == null || !fluidCap.isPresent()) fluidCap = LazyOptional.of(() -> outputTank);
            return fluidCap.cast();
        }
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (itemCap == null || !itemCap.isPresent()) itemCap = LazyOptional.of(() -> items);
            return itemCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override public void invalidateCaps() {
        super.invalidateCaps();
        if (fluidCap != null) { fluidCap.invalidate(); fluidCap = null; }
        if (itemCap != null) { itemCap.invalidate(); itemCap = null; }
    }

    // ── NBT ──────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("gt.heat", heat);
        tag.putLong("gt.progress", progress);
        tag.put("gt.items", items.serializeNBT());
        CompoundTag f = new CompoundTag();
        outputTank.writeToNBT(f);
        tag.put("gt.output", f);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        heat = tag.getLong("gt.heat");
        progress = tag.getLong("gt.progress");
        if (tag.contains("gt.items")) items.deserializeNBT(tag.getCompound("gt.items"));
        if (tag.contains("gt.output")) outputTank.readFromNBT(tag.getCompound("gt.output"));
    }
    @Override public void dropContents() {
        if (level == null || level.isClientSide) return;
        com.gregtech.gregtech.api.inventory.BlockContents.drop(this, items);
        setChanged();
    }
}
