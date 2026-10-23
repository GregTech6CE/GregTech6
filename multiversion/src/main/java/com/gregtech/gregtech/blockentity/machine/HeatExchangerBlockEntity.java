/* Gregorius Techneticies / GregTech-6 Team; LGPL-3.0-or-later.
 * MultiTileEntityGeneratorHotFluid: buffered fuel, top HU and front cooled fluid. */
package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.energy.EnergyTransfer;
import com.gregtech.gregtech.api.fluid.*;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.tool.*;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.block.machine.HeatExchangerBlock;
import com.gregtech.gregtech.content.multiblock.HeatExchangerRules;
import com.gregtech.gregtech.data.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}
import java.util.*;

public final class HeatExchangerBlockEntity extends GTEnergyBlockEntity implements IFluidHandler, FluidToolTarget,
        com.gregtech.gregtech.api.machine.HeatHandler {
    private final HeatExchangerRules.Settings settings;
    private final FluidTankGT hot, cold;
    private long heat, history;
    private boolean active;
    private Recipe lastRecipe;
    public HeatExchangerBlockEntity(BlockPos pos, BlockState state) {
        super(com.gregtech.gregtech.registry.GTBlockEntities.HEAT_EXCHANGER.get(), pos, state);
        settings = ((HeatExchangerBlock)state.getBlock()).spec().settings();
        hot = new FluidTankGT(settings.inputCapacity()).setOnChanged(this::setChanged);
        cold = new FluidTankGT().setOnChanged(this::setChanged);
    }
    public long tankAmount(int tank) { return tank == 0 ? hot.getAmount() : cold.getAmount(); }
    public HeatExchangerRules.Settings settings() { return settings; }

    // ── HeatHandler: the exchanger buffers HU for its upward outlets ────────────────────────

    @Override public long heatStored() { return heat; }
    @Override public long heatCapacity() { return settings.bufferTarget(); }
    @Override public long temperatureK() { return com.gregtech.gregtech.data.GregTechConstants.DEF_ENV_TEMP; }

    @Override public long insertHeat(long signedHU, boolean simulate) {
        if (signedHU == 0) return 0;
        if (signedHU > 0) {
            long accepted = heat < 0 ? signedHU : Math.min(signedHU, Long.MAX_VALUE - heat);
            if (!simulate && accepted > 0) {
                heat += accepted;
                setChanged();
            }
            return accepted;
        }
        if (signedHU == Long.MIN_VALUE) return 0;
        long removed = Math.min(Math.max(0, heat), -signedHU);
        if (!simulate && removed > 0) {
            heat -= removed;
            setChanged();
        }
        return -removed;
    }

    @Override public long extractHeat(long signedHU, boolean simulate) {
        if (signedHU == 0) return 0;
        if (signedHU > 0) {
            long removed = Math.min(Math.max(0, heat), signedHU);
            if (!simulate && removed > 0) {
                heat -= removed;
                setChanged();
            }
            return removed;
        }
        if (signedHU == Long.MIN_VALUE) return 0;
        return -insertHeat(-signedHU, simulate);
    }
    public void tick() {
        if (level == null || level.isClientSide) return;
        long offered = Math.min(settings.rate(), heat);
        if (offered > 0) {
            EnergyTransfer.emitEnergyToSide(GregTechTags.Energy.HU, Direction.UP, 1, offered, this);
            heat -= offered; // Original wastes heat even without a connected consumer.
            if (settings.efficiency() < 1 || level.random.nextInt(settings.efficiency()) == 0) {
                BlockPos fire = worldPosition.offset(level.random.nextInt(5) - 2, level.random.nextInt(4) - 1, level.random.nextInt(5) - 2);
                if (level.hasChunkAt(fire) && level.isEmptyBlock(fire)) {
                    var flame = net.minecraft.world.level.block.BaseFireBlock.getState(level, fire);
                    if (flame.canSurvive(level, fire)) level.setBlockAndUpdate(fire, flame);
                }
            }
        }
        if (heat < settings.bufferTarget()) {
            active = false;
            if (cold.getAmount() < settings.outputBackpressure()) {
                var result = HotFluidFuelCycle.charge(settings, hot, cold, heat, history, lastRecipe);
                heat = result.heat(); lastRecipe = result.recipe(); active = result.active();
            }
        }
        history = (history << 1) | (active ? 1 : 0);
        var state = getBlockState();
        if (state.getValue(HeatExchangerBlock.ACTIVE) != (history != 0))
            level.setBlock(worldPosition, state.setValue(HeatExchangerBlock.ACTIVE, history != 0), 3);
        BlockPos front = worldPosition.relative(state.getValue(HeatExchangerBlock.FACING));
        if (!cold.isEmpty() && level.hasChunkAt(front)) {
//? if >=1.21.1 {
            /*var target = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, front, state.getValue(HeatExchangerBlock.FACING).getOpposite());
*///?} else {
            var owner = level.getBlockEntity(front);
            var target = owner == null ? null : owner.getCapability(ForgeCapabilities.FLUID_HANDLER, state.getValue(HeatExchangerBlock.FACING).getOpposite()).orElse(null);
//?}
            if (target != null) cold.drain(target.fill(cold.getFluid().copy(), FluidAction.EXECUTE), FluidAction.EXECUTE);
        }
        setChanged();
    }
    public InteractionResult interact(Player player, InteractionHand hand) {
        var held = player.getItemInHand(hand);
        if (GTToolHelper.matchesTool(held, GTToolType.PLUNGER)) {
            if (!level.isClientSide) {
                var tank = cold.isEmpty() ? hot : cold;
                if (!tank.isEmpty()) { tank.setEmpty(); GTToolHelper.damageForUse(held, 1, player); }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (GTToolHelper.matchesTool(held, GTToolType.MAGNIFYING_GLASS)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Input: ").append(com.gregtech.gregtech.api.multiblock.MultiblockTools.tankContent(hot, "Empty")), false);
                player.displayClientMessage(Component.literal("Output: ").append(com.gregtech.gregtech.api.multiblock.MultiblockTools.tankContent(cold, "Empty")), false);
                GTToolHelper.damageForUse(held, 1, player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
    private final IFluidHandler tap = new IFluidHandler() {
        private FluidTankGT tank() { return cold.isEmpty() ? hot : cold; }
        public int getTanks() { return 1; }
        public FluidStack getFluidInTank(int slot) { return slot == 0 ? tank().getFluid().copy() : FluidStack.EMPTY; }
        public int getTankCapacity(int slot) { return tank().getCapacity(); }
        public boolean isFluidValid(int slot, FluidStack stack) { return false; }
        public int fill(FluidStack stack, FluidAction action) { return 0; }
        public FluidStack drain(FluidStack stack, FluidAction action) { return tank().drain(stack, action); }
        public FluidStack drain(int amount, FluidAction action) { return tank().drain(amount, action); }
    };
    @Override public IFluidHandler fluidToolHandler(Direction side, boolean filling) { return isRemoved() ? null : filling ? new FluidPort(this, true, false) : tap; }
    @Override public int getTanks() { return 2; }
    @Override public FluidStack getFluidInTank(int tank) { return tank == 0 ? hot.getFluid().copy() : tank == 1 ? cold.getFluid().copy() : FluidStack.EMPTY; }
    @Override public int getTankCapacity(int tank) { return tank == 0 ? hot.getCapacity() : tank == 1 ? cold.getCapacity() : 0; }
    @Override public boolean isFluidValid(int tank, FluidStack stack) { return tank == 0 && FuelRecipeMaps.Hot.containsInput(stack); }
    @Override public int fill(FluidStack stack, FluidAction action) { return isFluidValid(0, stack) ? hot.fill(stack, action) : 0; }
    @Override public FluidStack drain(FluidStack stack, FluidAction action) { return cold.drain(stack, action); }
    @Override public FluidStack drain(int amount, FluidAction action) { return cold.drain(amount, action); }
//? if >=1.21.1 {
    /*public IFluidHandler fluidCapability(Direction side) { return isRemoved() ? null : this; }
*///?} else {
    private LazyOptional<IFluidHandler> fluids = LazyOptional.of(() -> this);
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) { return cap == ForgeCapabilities.FLUID_HANDLER ? fluids.cast() : super.getCapability(cap, side); }
    @Override public void invalidateCaps() { super.invalidateCaps(); fluids.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); fluids = LazyOptional.of(() -> this); }
//?}
    @Override public boolean isEnergyType(GregTechTags.Tag type, Direction side, boolean emitting) { return emitting && type == GregTechTags.Energy.HU; }
    @Override public boolean isEnergyEmittingTo(GregTechTags.Tag type, Direction side, boolean theoretical) { return (side == null || side == Direction.UP) && super.isEnergyEmittingTo(type, side, theoretical); }
    @Override public Collection<GregTechTags.Tag> getEnergyTypes(Direction side) { return List.of(GregTechTags.Energy.HU); }
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag type, Direction side) { return 0; }
    @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag type, Direction side) { return settings.rate(); }
    @Override public long getEnergySizeOutputMin(GregTechTags.Tag type, Direction side) { return settings.rate(); }
    @Override public long getEnergySizeOutputMax(GregTechTags.Tag type, Direction side) { return settings.rate(); }
    @Override public long getEnergyOffered(GregTechTags.Tag type, Direction side, long size) { return Math.min(settings.rate(), heat); }
    @Override public long getEnergyDemanded(GregTechTags.Tag type, Direction side, long size) { return 0; }
    @Override public long getEnergyStored(GregTechTags.Tag type, Direction side) { return type == GregTechTags.Energy.HU ? heat : 0; }
    @Override public long getEnergyCapacity(GregTechTags.Tag type, Direction side) { return type == GregTechTags.Energy.HU ? settings.bufferTarget() : 0; }
//? if >=1.21.1 {
    /*@Override protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        var input = new CompoundTag(); var output = new CompoundTag();
        hot.writeToNBT(input, lookup); cold.writeToNBT(output, lookup);
*///?} else {
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        var input = new CompoundTag(); var output = new CompoundTag();
        hot.writeToNBT(input); cold.writeToNBT(output);
//?}
        tag.put("gt.hot", input); tag.put("gt.cold", output);
        tag.putLong("gt.energy", heat); tag.putLong("gt.active.data", history); tag.putBoolean("gt.active", active);
    }
//? if >=1.21.1 {
    /*@Override protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup); hot.readFromNBT(tag.getCompound("gt.hot"), lookup); cold.readFromNBT(tag.getCompound("gt.cold"), lookup);
*///?} else {
    @Override public void load(CompoundTag tag) {
        super.load(tag); hot.readFromNBT(tag.getCompound("gt.hot")); cold.readFromNBT(tag.getCompound("gt.cold"));
//?}
        heat = Math.max(0, tag.getLong("gt.energy")); history = tag.getLong("gt.active.data"); active = tag.getBoolean("gt.active");
    }
}
