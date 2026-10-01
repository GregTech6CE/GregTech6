package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.fluid.FluidTankGT;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTMultiblocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import java.util.List;

/**
 * GT6 Large Boiler multiblock: 3x3 heat transmitter base below a 3x3x3 shell
 * of boiler walls, controller embedded front-center of the bottom wall layer.
 * Heat enters base parts, water enters bottom walls and steam leaves upper walls. Boils water at 1 HU -> 2 L steam (200 L steam per
 * 1 L water) and pushes steam to adjacent fluid handlers.
 */
public class LargeBoilerControllerBlockEntity extends GTEnergyBlockEntity implements com.gregtech.gregtech.api.multiblock.MultiblockPortOwner {

    private static final long STEAM_PER_HU = com.gregtech.gregtech.content.multiblock.LegacyBoilerProcessing.STEAM_PER_HU;
    private static final long STEAM_PER_WATER = com.gregtech.gregtech.content.multiblock.LegacyBoilerProcessing.STEAM_PER_WATER;
    private static final long HEAT_CAPACITY = com.gregtech.gregtech.content.multiblock.LegacyBoilerProcessing.HEAT_CAPACITY;

    private long heat;
    private final FluidTankGT waterTank = new FluidTankGT(16_000).setOnChanged(this::setChanged);
    private final FluidTankGT steamTank = new FluidTankGT(64_000).setOnChanged(this::setChanged);

    private final com.gregtech.gregtech.api.multiblock.PartBindings<BlockPos,com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role> boundParts = new com.gregtech.gregtech.api.multiblock.PartBindings<>();


    public LargeBoilerControllerBlockEntity(BlockPos pos, BlockState state) {
        super(com.gregtech.gregtech.registry.GTBlockEntities.LARGE_BOILER.get(), pos, state);
    }

    public boolean isStructureOk() {
        if (level == null || isRemoved()) return false;
        if (!validate()) { releaseParts(); return false; }
        var next = new java.util.LinkedHashMap<BlockPos,com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role>();
        for (var cell:com.gregtech.gregtech.content.multiblock.BoilerStructure.LAYOUT.cells()) {
            BlockPos pos=cell.at(worldPosition,facing());
            if (pos.equals(worldPosition) || cell.role()==com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role.AIR) continue;
            next.put(pos,cell.role());
        }
        return boundParts.update(next,
                pos -> level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity port && port.canBind(worldPosition),
                (pos,role) -> ((MultiblockPortBlockEntity)level.getBlockEntity(pos)).bind(worldPosition,role),this::releasePart);
    }

    private Direction facing() {return getBlockState().getValue(HorizontalDirectionalBlock.FACING);}
    private void releasePart(BlockPos pos) {
        if(level!=null && level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity port)
            port.release(worldPosition);
    }
    private void releaseParts() { boundParts.clear(this::releasePart); }
    @Override public void setRemoved() {releaseParts();super.setRemoved();}

    private boolean validate() {
        return com.gregtech.gregtech.content.multiblock.BoilerStructure.LAYOUT.matches(worldPosition,facing(),level::hasChunkAt,(pos,role) -> {
            if(pos.equals(worldPosition)) return true;
            if(role==com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role.AIR) return level.getBlockState(pos).isAir();
            var expected=role==com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role.HEAT_INPUT?GTMultiblocks.HEAT_TRANSMITTER.get():GTMultiblocks.BOILER_WALL.get();
            return level.getBlockState(pos).is(expected) && level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity port && port.canBind(worldPosition);
        });
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    public static void serverTick(Level level, BlockPos pos, BlockState state, LargeBoilerControllerBlockEntity be) {
        if (!be.isStructureOk()) return;
        be.boil();
        be.pushSteam();
    }

    private void boil() {
        if (heat <= 0 || waterTank.isEmpty()) return;
        var steamFluid = com.gregtech.gregtech.registry.GTFluids.still("Steam");
        if (steamFluid == null || !steamFluid.isBound()) return;

        var plan=com.gregtech.gregtech.content.multiblock.LegacyBoilerProcessing.plan(heat,waterTank.getAmount(),steamTank.getCapacity()-steamTank.getAmount());
        long water=plan.water(),steam=plan.steam();if(water<=0)return;

        waterTank.drain((int) water, IFluidHandler.FluidAction.EXECUTE);
        heat -= steam / STEAM_PER_HU;
        FluidStack produced = new FluidStack(steamFluid.get(), (int) steam);
        if (steamTank.getFluid().isEmpty()) {
            steamTank.setFluid(produced);
        } else if (FluidStack.isSameFluidSameComponents(steamTank.getFluid(),produced)) {
            steamTank.add(steam);
        }
        setChanged();
    }

    private void pushSteam() {
        if (steamTank.isEmpty() || level == null) return;
        for (var cell:com.gregtech.gregtech.content.multiblock.BoilerStructure.LAYOUT.cells()) {
            if (cell.role()!=com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role.FLUID_OUTPUT) continue;
            BlockPos port=cell.at(worldPosition,facing());
            for (Direction dir:Direction.values()) {
                BlockPos next=port.relative(dir);
                if (boundParts.contains(next) || next.equals(worldPosition) || !level.hasChunkAt(next)) continue;
                var target=level.getBlockEntity(next);
                if (target==null) continue;
                var handler=level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,next,dir.getOpposite());
                if(handler!=null){int moved=handler.fill(steamTank.drain(4000,IFluidHandler.FluidAction.SIMULATE),IFluidHandler.FluidAction.EXECUTE);if(moved>0)steamTank.drain(moved,IFluidHandler.FluidAction.EXECUTE);}
                if(steamTank.isEmpty()) return;
            }
        }
    }

    // ── HU acceptance ────────────────────────────────────────────────────────

    @Override
    public boolean isEnergyType(GregTechTags.Tag energyType, @Nullable Direction side, boolean emitting) {
        return !emitting && energyType == GregTechTags.Energy.HU;
    }

    @Override
    public java.util.Collection<GregTechTags.Tag> getEnergyTypes(@Nullable Direction side) {
        return List.of(GregTechTags.Energy.HU);
    }

    @Override
    public boolean isEnergyAcceptingFrom(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        return false;
    }

    @Override
    public boolean isEnergyEmittingTo(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        return false;
    }

    @Override
    public long getEnergySizeInputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) {
        return energyType == GregTechTags.Energy.HU ? 512 : 0;
    }

    @Override
    public long getEnergySizeOutputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) {
        return 0;
    }

    @Override
    public long getEnergyOffered(GregTechTags.Tag energyType, @Nullable Direction side, long size) {
        return 0;
    }

    @Override
    public long getEnergyDemanded(GregTechTags.Tag energyType, @Nullable Direction side, long size) {
        return 0; // HU belongs to the heat-transmitter base parts.
    }

    @Override
    public long doInject(GregTechTags.Tag energyType, @Nullable Direction side, long size, long amount, boolean doInject) { return 0; }

    @Override
    public long getEnergyStored(GregTechTags.Tag energyType, @Nullable Direction side) {
        return energyType == GregTechTags.Energy.HU ? heat : 0;
    }

    @Override
    public long getEnergyCapacity(GregTechTags.Tag energyType, @Nullable Direction side) {
        return energyType == GregTechTags.Energy.HU ? HEAT_CAPACITY : 0;
    }

    // ── Fluids: accept water, expose steam ───────────────────────────────────

    private static boolean boilerWater(FluidStack stack) {
        if(stack.isEmpty() || stack.getFluid().getFluidType().getTemperature()>=400) return false;
        var key=net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(stack.getFluid());
        return key!=null && key.getPath().contains("water");
    }

    private final IFluidHandler fluids = new IFluidHandler() {
        @Override public int getTanks() { return 2; }
        @Override public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? waterTank.getFluid() : steamTank.getFluid();
        }
        @Override public int getTankCapacity(int tank) {
            return (int) (tank == 0 ? waterTank.getCapacity() : steamTank.getCapacity());
        }
        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && boilerWater(stack);
        }
        @Override public int fill(FluidStack resource, FluidAction action) {
            if (!isStructureOk() || !boilerWater(resource)) return 0;
            return waterTank.fill(resource, action);
        }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            if (!isStructureOk() || resource.isEmpty() || !FluidStack.isSameFluidSameComponents(resource,steamTank.getFluid())) return FluidStack.EMPTY;
            return steamTank.drain(resource.getAmount(), action);
        }
        @Override public FluidStack drain(int maxDrain, FluidAction action) {
            return isStructureOk() ? steamTank.drain(maxDrain, action) : FluidStack.EMPTY;
        }
    };

    private IFluidHandler port(boolean input) {
        return new IFluidHandler() {
            public int getTanks() { return 1; }
            public FluidStack getFluidInTank(int tank) { return (input?waterTank:steamTank).getFluid().copy(); }
            public int getTankCapacity(int tank) { return (input?waterTank:steamTank).getCapacity(); }
            public boolean isFluidValid(int tank,FluidStack stack) { return input && fluids.isFluidValid(0,stack); }
            public int fill(FluidStack stack,FluidAction action) { return input?fluids.fill(stack,action):0; }
            public FluidStack drain(FluidStack stack,FluidAction action) { return input?FluidStack.EMPTY:fluids.drain(stack,action); }
            public FluidStack drain(int amount,FluidAction action) { return input?FluidStack.EMPTY:fluids.drain(amount,action); }
        };
    }
    private final IFluidHandler waterPort=port(true), steamPort=port(false);
    @Override public IFluidHandler portFluids(com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role role) {
        return role==com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role.FLUID_INPUT?waterPort:
                role==com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role.FLUID_OUTPUT?steamPort:null;
    }
    @Override public java.util.Collection<GregTechTags.Tag> portEnergyTypes(com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role role) {
        return role==com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role.HEAT_INPUT?List.of(GregTechTags.Energy.HU):List.of();
    }
    @Override public long portEnergyStored(com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role role,GregTechTags.Tag type) {return portEnergyTypes(role).contains(type)?heat:0;}
    @Override public long portEnergyCapacity(com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role role,GregTechTags.Tag type) {return portEnergyTypes(role).contains(type)?HEAT_CAPACITY:0;}
    @Override public long injectPortEnergy(com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role role,GregTechTags.Tag type,long size,long amount,boolean execute) {
        if (!portEnergyTypes(role).contains(type) || size<=0 || amount<=0 || !isStructureOk()) return 0;
        long accepted=Math.min(amount,Math.max(0,HEAT_CAPACITY-heat)/size);
        if(execute&&accepted>0) {heat+=accepted*size;setChanged();}
        return accepted;
    }

    public IFluidHandler fluidCapability(Direction side){return isRemoved()?null:waterPort;}

    // ── NBT ──────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);
        tag.putLong("gt.heat", heat);
        CompoundTag w = new CompoundTag();
        waterTank.writeToNBT(w,lookup);
        tag.put("gt.water", w);
        CompoundTag s = new CompoundTag();
        steamTank.writeToNBT(s,lookup);
        tag.put("gt.steam", s);
    }

    @Override
    protected void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);
        heat = Math.max(0,tag.getLong("gt.heat"));
        if (tag.contains("gt.water")) waterTank.readFromNBT(tag.getCompound("gt.water"),lookup);
        if (tag.contains("gt.steam")) steamTank.readFromNBT(tag.getCompound("gt.steam"),lookup);
    }
}
