package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.fluid.FluidTankGT;
import com.gregtech.gregtech.api.fluid.FluidHazards;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.content.multiblock.LargeMachineParts;
import com.gregtech.gregtech.content.multiblock.TankValveSpec;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.data.RegisteredFluids;
import com.gregtech.gregtech.registry.GTMultiblocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import java.util.List;

/**
 * GT6 multiblock fluid tank (3x3x3 or 5x5x5). The wall blocks define the
 * boundary and the controller provides fluid I/O with multiplied capacity.
 */
public class MultiblockTankControllerBlockEntity extends GTEnergyBlockEntity implements com.gregtech.gregtech.api.multiblock.MultiblockPortOwner {
    private final com.gregtech.gregtech.api.multiblock.PartBindings<BlockPos,com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role> bindings = new com.gregtech.gregtech.api.multiblock.PartBindings<>();

    int size = 3; // set by block after construction
    @Nullable private final TankValveSpec valveSpec;
    FluidTankGT tank = new FluidTankGT(320_000).setOnChanged(this::setChanged);

    @Nullable private LazyOptional<IFluidHandler> fluidCap;

    public MultiblockTankControllerBlockEntity(BlockPos pos, BlockState state) {
        super(com.gregtech.gregtech.registry.GTBlockEntities.MULTIBLOCK_TANK.get(), pos, state);
        valveSpec = state.getBlock() instanceof com.gregtech.gregtech.block.machine.TankControllerBlock block
                ? block.valveSpec() : null;
        if (state.getBlock() instanceof com.gregtech.gregtech.block.machine.TankControllerBlock block) setSize(block.size());
    }

    public void setSize(int size) {
        if (size != 3 && size != 5) throw new IllegalArgumentException("Tank size: " + size);
        if (valveSpec != null && valveSpec.size() != size) throw new IllegalArgumentException("Tank valve size: " + size);
        this.size = size;
        tank.setCapacity(valveSpec == null ? (size == 5 ? 1_024_000 : 320_000) : valveSpec.capacity());
        if (valveSpec != null) tank.setGasProof(valveSpec.gasProof()).setAcidProof(valveSpec.acidProof())
                .setPlasmaProof(valveSpec.plasmaProof()).setMagicProof(valveSpec.magicProof())
                .setMaxTemperature(valveSpec.meltingPoint());
    }

    @Nullable public TankValveSpec valveSpec() { return valveSpec; }
    public IFluidHandler fluidHandler() { return tankHandler; }

    private Direction facing() { return getBlockState().getValue(DirectionalBlock.FACING); }
    private void release(BlockPos pos) {
        if (level!=null && level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity port)
            port.release(worldPosition);
    }
    @Override public void setRemoved() { bindings.clear(this::release); super.setRemoved(); }
    public boolean isStructureOk() {
        if (level==null || isRemoved()) return false;
        var wall=valveSpec == null ? (size==5?GTMultiblocks.TANK_WALL_DENSE.get():GTMultiblocks.TANK_WALL.get())
                : LargeMachineParts.block(valveSpec.wallId());
        var candidates=new java.util.LinkedHashMap<BlockPos,com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role>();
        boolean valid=com.gregtech.gregtech.api.multiblock.HollowTankStructure.validate(worldPosition,facing(),size,
                level::hasChunkAt,pos -> {
                    if (!level.getBlockState(pos).is(wall)) return false;
                    candidates.put(pos,com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role.FLUID_IO);
                    return true;
                },pos -> level.getBlockState(pos).isAir());
        if (!valid) { bindings.clear(this::release); return false; }
        return bindings.update(candidates,
                pos -> level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity port && port.canBind(worldPosition),
                (pos,role) -> ((MultiblockPortBlockEntity)level.getBlockEntity(pos)).bind(worldPosition,role),this::release);
    }
    @Override public IFluidHandler portFluids(com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role role) {
        return role==com.gregtech.gregtech.api.multiblock.MultiblockLayout.Role.FLUID_IO? tankHandler : null;
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   MultiblockTankControllerBlockEntity be) {
        if (!be.isStructureOk() || be.tank.isEmpty()) return;
        if (be.valveSpec != null && be.processHazard(be.tank.getFluid())) return;
        be.pushFluid();
    }

    /** GT6 checks a filled, formed vessel once per tick, after allowing fluid into its walls. */
    private boolean processHazard(FluidStack fluid) {
        var entry = com.gregtech.gregtech.registry.GTFluids.entryForFluid(fluid.getFluid());
        if (temperature(fluid, entry) >= valveSpec.meltingPoint()) {
            meltDown(fluid);
            return true;
        }
        if (!valveSpec.magicProof() && entry != null && entry.hasFlag(RegisteredFluids.FluidFlags.MAGIC)) {
            // GT6 replaces these cells with Thaumcraft Flux Goo/Gas. No Flux block exists in this port;
            // clearing those same cells preserves the vessel loss until that integration is available.
            damageShell(Blocks.AIR.defaultBlockState());
            return true;
        }
        if (!valveSpec.acidProof() && FluidHazards.isAcid(fluid.getFluid())) {
            damageShell(Blocks.AIR.defaultBlockState());
            return true;
        }
        if ((!valveSpec.plasmaProof() && FluidHazards.isPlasma(fluid.getFluid()))
                || (!valveSpec.gasProof() && FluidHazards.isGas(fluid.getFluid()))
                || (entry != null && entry.hasFlag(RegisteredFluids.FluidFlags.POWER_CONDUCTING))
                || (valveSpec.simpleOnly() && !isSimple(fluid, entry))) {
            tank.setEmpty();
            fizz();
            return true;
        }
        return false;
    }

    private static int temperature(FluidStack fluid, @Nullable RegisteredFluids.FluidEntry entry) {
        return entry == null ? fluid.getFluid().getFluidType().getTemperature(fluid) : entry.temperature();
    }

    private static boolean isSimple(FluidStack fluid, @Nullable RegisteredFluids.FluidEntry entry) {
        // Vanilla water and lava are explicitly in GT6 FluidsGT.SIMPLE; an unknown add-on fluid is not.
        var type = fluid.getFluid();
        return type == Fluids.WATER || type == Fluids.FLOWING_WATER
                || type == Fluids.LAVA || type == Fluids.FLOWING_LAVA
                || entry != null && entry.hasFlag(RegisteredFluids.FluidFlags.SIMPLE);
    }

    private void fizz() {
        if (level != null) level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH,
                SoundSource.BLOCKS, 0.5F, 1.0F);
    }

    /** Acid removes random cells; magic uses the same radius but lacks a 1.20 Flux block. */
    private void damageShell(BlockState replacement) {
        if (level == null) return;
        Level world = level;
        int radius = size / 2;
        BlockPos centre = worldPosition.relative(facing().getOpposite(), radius);
        tank.setEmpty();
        fizz();
        for (int x = -radius; x <= radius; x++) for (int y = -radius; y <= radius; y++)
            for (int z = -radius; z <= radius; z++) {
                if (world.random.nextInt(3) == 0)
                    world.setBlock(centre.offset(x, y, z), replacement, 3);
            }
        // The original always removes/replaces the valve even when its random roll misses.
        world.setBlock(worldPosition, replacement, 3);
    }

    /** GT6 Tank3x3x3/Tank5x5x5 meltdown traverses the entire hollow cube and its neighbours. */
    private void meltDown(FluidStack fluid) {
        if (level == null) return;
        Level world = level;
        int radius = size / 2;
        BlockPos centre = worldPosition.relative(facing().getOpposite(), radius);
        boolean vanillaLava = fluid.getFluid() == Fluids.LAVA || fluid.getFluid() == Fluids.FLOWING_LAVA;
        boolean enoughLava = tank.getAmount() >= 1_000;
        for (int x = -radius; x <= radius; x++) for (int y = -radius; y <= radius; y++)
            for (int z = -radius; z <= radius; z++) {
                BlockPos cell = centre.offset(x, y, z);
                for (Direction side : Direction.values()) {
                    BlockPos neighbour = cell.relative(side);
                    if (!world.hasChunkAt(neighbour)) continue;
                    BlockState neighbourState = world.getBlockState(neighbour);
                    if (!neighbourState.is(Blocks.FIRE) && neighbourState.getFluidState().isEmpty()
                            && neighbourState.getCollisionShape(world, neighbour).isEmpty())
                        world.setBlock(neighbour, Blocks.FIRE.defaultBlockState(), 3);
                }
                if (world.random.nextInt(4) == 0)
                    world.setBlock(cell, Blocks.FIRE.defaultBlockState(), 3);
            }
        // GT6's lava-pool special case exists only on the 3x3x3 tank.
        if (size == 3 && vanillaLava && enoughLava)
            world.setBlock(centre, Blocks.LAVA.defaultBlockState(), 3);
        tank.setEmpty();
        world.setBlock(worldPosition, Blocks.FIRE.defaultBlockState(), 3);
    }

    private void pushFluid() {
        if (tank.isEmpty() || level==null) return;
        var fluid=tank.getFluid();
        var face=facing();
        var type=fluid.getFluid().getFluidType();
        if (!com.gregtech.gregtech.content.multiblock.TankFlowRules.canAutoOutput(face,
                com.gregtech.gregtech.registry.GTFluids.isGas(fluid),type.getDensity(fluid))) return;
        var next=worldPosition.relative(face);
        if (!level.hasChunkAt(next)) return;
        var target=level.getBlockEntity(next);
        if (target==null) return;
        target.getCapability(ForgeCapabilities.FLUID_HANDLER,face.getOpposite()).ifPresent(handler -> {
            var offered=tank.drain(Integer.MAX_VALUE,IFluidHandler.FluidAction.SIMULATE);
            int moved=handler.fill(offered,IFluidHandler.FluidAction.EXECUTE);
            if (moved>0) tank.drain(moved,IFluidHandler.FluidAction.EXECUTE);
        });
    }

    // ── Energy (tank uses none) ──────────────────────────────────────────────

    @Override public boolean isEnergyType(GregTechTags.Tag e, @Nullable Direction side, boolean emitting) { return false; }
    @Override public java.util.Collection<GregTechTags.Tag> getEnergyTypes(@Nullable Direction side) { return List.of(); }
    @Override public boolean isEnergyAcceptingFrom(GregTechTags.Tag e, @Nullable Direction side, boolean th) { return false; }
    @Override public boolean isEnergyEmittingTo(GregTechTags.Tag e, @Nullable Direction side, boolean th) { return false; }
    @Override public long getEnergySizeInputRecommended(GregTechTags.Tag e, @Nullable Direction s) { return 0; }
    @Override public long getEnergySizeOutputRecommended(GregTechTags.Tag e, @Nullable Direction s) { return 0; }
    @Override public long getEnergyOffered(GregTechTags.Tag e, @Nullable Direction s, long size) { return 0; }
    @Override public long getEnergyDemanded(GregTechTags.Tag e, @Nullable Direction s, long size) { return 0; }
    @Override public long doInject(GregTechTags.Tag e, @Nullable Direction s, long size, long amount, boolean doInject) { return 0; }
    @Override public long getEnergyStored(GregTechTags.Tag e, @Nullable Direction s) { return 0; }
    @Override public long getEnergyCapacity(GregTechTags.Tag e, @Nullable Direction s) { return 0; }

    // ── Fluid I/O ────────────────────────────────────────────────────────────

    private final IFluidHandler tankHandler = new IFluidHandler() {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tankIdx) { return tankIdx==0?tank.getFluid().copy():FluidStack.EMPTY; }
        @Override public int getTankCapacity(int tankIdx) { return tankIdx==0?tank.getCapacity():0; }
        @Override public boolean isFluidValid(int tankIdx, FluidStack stack) { return tankIdx==0 && !stack.isEmpty(); }
        @Override public int fill(FluidStack resource, FluidAction action) { return isStructureOk() ? tank.fill(resource, action) : 0; }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) { return isStructureOk() ? tank.drain(resource, action) : FluidStack.EMPTY; }
        @Override public FluidStack drain(int maxDrain, FluidAction action) { return isStructureOk() ? tank.drain(maxDrain, action) : FluidStack.EMPTY; }
    };

    @Override
    public <T> @org.jetbrains.annotations.NotNull LazyOptional<T> getCapability(
            @org.jetbrains.annotations.NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER && !isRemoved()) {
            if (fluidCap == null || !fluidCap.isPresent()) fluidCap = LazyOptional.of(() -> tankHandler);
            return fluidCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override public void invalidateCaps() {
        super.invalidateCaps();
        if (fluidCap != null) { fluidCap.invalidate(); fluidCap = null; }
    }

    // ── NBT ──────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        CompoundTag f = new CompoundTag();
        tank.writeToNBT(f);
        tag.put("gt.tank", f);
        tag.putInt("gt.size", size);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("gt.tank")) tank.readFromNBT(tag.getCompound("gt.tank"));
        tank.setOnChanged(this::setChanged);
        setSize(size); // The placed valve, not item NBT, owns capacity and proof flags.
    }
}
