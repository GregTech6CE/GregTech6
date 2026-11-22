package gregtech.content.blockentity.machine;

import gregtech.api.machine.BlockFace;

import gregtech.engine.fluid.FluidContainerInteraction;
import gregtech.engine.fluid.FluidHazards;
import gregtech.engine.fluid.FluidTankGT;
import gregtech.engine.fluid.FluidTransportEnvironment;
import gregtech.engine.machine.TankSpec;
import gregtech.engine.tool.GTToolHelper;
import gregtech.content.block.machine.FluidPipeBlock;
import gregtech.content.block.machine.TankBlock;
import gregtech.content.blockentity.GTBlockEntity;
import gregtech.content.data.GregTechConstants;
import gregtech.content.registry.GTBlockEntities;
import gregtech.content.registry.GTFluids;
import gregtech.engine.stack.NbtCompat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.capabilities.Capabilities;


import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
*///?} else {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** GT6 fluid container tile entity. Handles wood barrels, plastic canisters, and metal drums. */
//? if >=1.21.1 {
/*public class TankBlockEntity extends GTBlockEntity implements IFluidHandler, gregtech.engine.fluid.FluidToolTarget, gregtech.content.cover.PanelCoverHost, gregtech.api.sensor.ProgressSensorSource {
*///?} else {
public class TankBlockEntity extends GTBlockEntity implements IFluidHandler, gregtech.engine.fluid.FluidToolTarget, gregtech.content.cover.PanelCoverHost, gregtech.api.sensor.ProgressSensorSource {    private final java.util.Map<Direction, LazyOptional<IFluidHandler>> componentFaceCaps = new java.util.EnumMap<>(Direction.class);

//?}
    private final gregtech.content.cover.ComponentCoverStorage componentCovers = new gregtech.content.cover.ComponentCoverStorage(this);
    @Override public net.minecraft.world.item.ItemStack getCover(Direction side) { return componentCovers.get(side); }
    @Override public boolean attachCover(Direction side, net.minecraft.world.item.ItemStack stack) { return componentCovers.attach(side,stack); }
    @Override public net.minecraft.world.item.ItemStack removeCover(Direction side) { return componentCovers.remove(side); }
    @Override public gregtech.content.cover.PanelCoverRuntime panels() { return componentCovers.panels(); }
    @Override public boolean consumesTool(ItemStack tool) {
        return GTToolHelper.isMachineWrench(tool)
                || GTToolHelper.isSoftHammer(tool)
                || GTToolHelper.isMagnifyingGlass(tool);
    }
    public void tickComponentCovers() { componentCovers.tick(); }
    public void dropComponentCovers() { componentCovers.drop(); }

    private final TankSpec spec;
    private final FluidTankGT tank;
    private long temperature = GregTechConstants.DEF_ENV_TEMP;
    private boolean autoOutput;
    /** GT6 {@code mMode & B[1]}: the barrel is sealed, which makes it ferment its contents. */
    private boolean softHammerState;
//? if >=1.21.1 {
    /*private boolean fluidAccessKnown;
    private int blockedFluidMask;
*///?}

    private static final String NBT_TEMPERATURE = "gt.temperature";
    private static final String NBT_AUTO_OUTPUT = "gt.auto_output";
    private static final String NBT_SOFT_HAMMER = "gt.soft_hammer";

    
//? if >=1.21.1 {
//?} else {
    private LazyOptional<IFluidHandler> handler = LazyOptional.of(() -> this);
//?}

//? if >=1.21.1 {
    /*protected TankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
*///?} else {
    public TankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
//?}
        super(type, pos, state);
        this.spec = specFromState(state);
        this.tank = new FluidTankGT(spec.capacity())
//? if >=1.21.1 {
                /*.setOnChanged(this::contentsChanged)
*///?} else {
                .setOnChanged(this::setChanged)
//?}
                .setMaxTemperature(spec.maxTemperature());
    }

    public TankBlockEntity(BlockPos pos, BlockState state) {
        this(GTBlockEntities.TANK.get(), pos, state);
    }

    private static TankSpec specFromState(BlockState state) {
        if (state.getBlock() instanceof TankBlock tb) return tb.spec();
        throw new IllegalStateException("TankBlockEntity on non-tank block");
    }

    public TankSpec spec() { return spec; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TankBlockEntity be) {
        if (!level.isClientSide) be.tickServer();
    }

    private void tickServer() {
        tickComponentCovers();
        // Temperature drift toward environment
        long envTemp = environmentTemperature();
        if (temperature > envTemp) {
            temperature = Math.max(envTemp, temperature - Math.min(5, temperature - envTemp));
        } else if (temperature < envTemp) {
            temperature = Math.min(envTemp, temperature + Math.min(5, envTemp - temperature));
        }

        // Check for fluid incompatibility
        FluidStack fluid = tank.getFluid();
        if (!fluid.isEmpty()) {
            // GT6 TileEntityBase08Barrel checks the stored fluid's own temperature, not the
            // ambient temperature of the block entity. This also handles an over-hot fluid
            // restored from NBT or inserted through a nonstandard integration.
            if (fluidTemperature(fluid) >= spec.maxTemperature()) {
//? if >=1.21.1 {
                /*FluidTransportEnvironment.meltdown(level, worldPosition);
*///?} else {
                SmelteryBlockEntityHelper.meltdown(level, worldPosition);
//?}
                tank.setFluid(FluidStack.EMPTY);
                return;
            }

            // GT6 TileEntityBase08Barrel — the hazard checks sit between the meltdown check
            // and the barrel's own modes, and they eat the contents before anything else runs.
            if (FluidHazards.hasUnprotectedPhysicalHazard(fluid.getFluid(), spec.protections())) {
                corrode(fluid);
                return;
            }

            // GT6 TileEntityBase08Barrel only fills adjacent tanks while unsealed.
            if (autoOutput && !softHammerState && tank.getAmount() > 0) {
                autoOutputFluid(fluid);
            }

        }

        setChanged();
    }

    // ── Fluid hazards (GT6 TileEntityBase08Barrel) ───────────────────────────

    /**
     * GT6 {@code TileEntityBase08Barrel}: a barrel holding a fluid its proof flags do not
     * cover loses the whole content at once (not the pipe's few units a tick) and fizzes, and acid
     * also dissolves the barrel itself with {@code setToAir()}, without the pipe's chance roll.
     * This method only covers the current gas/acid/plasma path; it does not implement the
     * original barrel's magic, simple-fluid and power-conducting checks.
     *
     * <p>The port's temperature meltdown above uses a separate lava/heat helper.
     * Original barrel meltdown chooses lava or fire from its contents; that distinction
     * is not implemented by the helper. Acid instead leaves air here.
     */
    private void corrode(FluidStack fluid) {
        tank.setEmpty();
        if (level == null) return;
        level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.0F);
        if (FluidHazards.isAcid(fluid.getFluid())) {
            level.removeBlock(worldPosition, false);
        }
        setChanged();
    }

    @Override public long progressValue(BlockFace side) { return 0; }
    @Override public long progressMaximum(BlockFace side) { return 0; }

    private void autoOutputFluid(FluidStack fluid) {
        // GT6 TileEntityBase08Barrel: gases fill both vertical neighbours, other lighter-than-air
        // fluids rise, and everything else falls.
        Direction[] outputs = FluidHazards.isGas(fluid.getFluid())
                ? new Direction[]{Direction.UP, Direction.DOWN}
                : new Direction[]{FluidHazards.isLighterThanAir(fluid.getFluid()) ? Direction.UP : Direction.DOWN};
        for (Direction side : outputs) {
            if (tank.isEmpty()) break;
            BlockPos targetPos = worldPosition.relative(side);
            BlockEntity be = level.getBlockEntity(targetPos);
            if (be == null) continue;

            // If target is a pipe, only output if its facing toward us is connected
            if (be instanceof FluidPipeBlockEntity) {
                BlockState pipeState = level.getBlockState(targetPos);
                if (!pipeState.getValue(FluidPipeBlock.propFor(side.getOpposite()))) continue;
            }

//? if >=1.21.1 {
            /*IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK,targetPos,side.getOpposite());
*///?} else {
            IFluidHandler target = be.getCapability(ForgeCapabilities.FLUID_HANDLER, side.getOpposite()).orElse(null);
//?}
            if (target == null) continue;

            // Original TileEntityBase08Barrel uses FL.move(tank, adjacent) with Long.MAX_VALUE:
            // one transfer per tick is limited only by the target's capacity, not an arbitrary
            // per-tick amount. Forge's FluidStack amount is an int, so clamp there and no lower.
            int toSend = FluidTankGT.bindInt(tank.getAmount());
            FluidStack drained = tank.drain(toSend, IFluidHandler.FluidAction.SIMULATE);
            if (drained.isEmpty()) continue;
            int filled = target.fill(drained, IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) {
                tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    private long environmentTemperature() {
//? if >=1.21.1 {
        /*return FluidTransportEnvironment.environmentTemperature(level, worldPosition);
*///?} else {
        return SmelteryBlockEntityHelper.environmentTemperature(level, worldPosition);
//?}
    }

    /** Dump fluids to adjacent tanks when block is broken (Factorio-style). */
    public void dumpFluidsToAdjacent() {
        if (level == null || level.isClientSide) return;
        if (tank.isEmpty()) return;
        FluidStack fluid = tank.getFluid();
        for (Direction side : Direction.values()) {
            if (tank.isEmpty()) break;
            BlockEntity be = level.getBlockEntity(worldPosition.relative(side));
            if (be == null) continue;
//? if >=1.21.1 {
            /*IFluidHandler target=level.getCapability(Capabilities.FluidHandler.BLOCK,worldPosition.relative(side),side.getOpposite());
*///?} else {
            LazyOptional<IFluidHandler> cap = be.getCapability(ForgeCapabilities.FLUID_HANDLER, side.getOpposite());
            if (!cap.isPresent()) continue;
            IFluidHandler target = cap.orElse(null);
//?}
            if (target == null) continue;
            FluidStack toSend = tank.getFluid();
            if (toSend.isEmpty()) break;
            int filled = target.fill(toSend, IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    /** Use the platform transaction so replacement containers, stacks and creative mode stay consistent. */
    public InteractionResult handleUse(Player player, InteractionHand hand) {
        if (level == null || level.isClientSide) return InteractionResult.PASS;
        if (!FluidContainerInteraction.use(player, hand, this))
            return InteractionResult.PASS;
        setChanged();
        return InteractionResult.CONSUME;
    }

// === IFluidHandler ===

    @Override
    public int getTanks() { return 1; }

    @NotNull
    @Override
    public FluidStack getFluidInTank(int t) { return tank.getFluid(); }

    @Override
    public int getTankCapacity(int t) { return tank.getCapacity(); }

    @Override
    public boolean isFluidValid(int t, @NotNull FluidStack stack) {
        if (!stack.isEmpty() && fluidTemperature(stack) >= spec.maxTemperature()) return false;
        if (spec.simpleOnly()) {
            // Simple-only: water, lava, milk, and basic fluids
            Fluid f = stack.getFluid();
            if (f != Fluids.WATER && f != Fluids.LAVA && f != Fluids.FLOWING_WATER && f != Fluids.FLOWING_LAVA) {
                // In GT6 this checks fluid sets. For now allow basic vanilla fluids.
            }
        }
        // Current block-capability gate. GT6's portable-item fill checked protection here,
        // whereas its placed barrel accepted into the tank and handled hazards on tick.
        if (FluidHazards.hasUnprotectedPhysicalHazard(stack.getFluid(), spec.protections())) {
            return false;
        }
        return tank.isFluidValid(stack);
    }

    /** GT6 {@code FL.temperature}: use its registered temperature before Forge's fluid fallback. */
    private static long fluidTemperature(FluidStack stack) {
        var entry = GTFluids.entryForFluid(stack.getFluid());
        return entry == null ? stack.getFluid().getFluidType().getTemperature(stack) : entry.temperature();
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        // Sealed vessels reject both directions, matching the original barrel's lock.
        if (softHammerState) return 0;
        // Enforce the current validation policy here as well: callers need not consult
        // isFluidValid before invoking fill. Original block/item policies differ (see above).
        if (resource.isEmpty() || !isFluidValid(0, resource)) return 0;
        return tank.fill(resource, action);
    }

    @NotNull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return softHammerState ? FluidStack.EMPTY : tank.drain(resource, action);
    }

    @NotNull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return softHammerState ? FluidStack.EMPTY : tank.drain(maxDrain, action);
    }

    /** Attachments bypass the facing capability, which covers and auto-output faces can close. */
    @Override
    public IFluidHandler fluidToolHandler(Direction side, boolean filling) {
        if (isRemoved()) return null;
        return filling ? new gregtech.engine.fluid.FluidPort(this, true, false)
                : new gregtech.engine.fluid.FluidPort(this, false, true);
    }

    // === Capability ===

    /** Face mask the barrel pushes into; those faces stay open as outputs, not inputs. */
    private int blockedFluidMask() {
        if (!autoOutput || tank.isEmpty()) return 0;
        Fluid fluid = tank.getFluid().getFluid();
        if (FluidHazards.isGas(fluid)) {
            return (1 << Direction.UP.ordinal()) | (1 << Direction.DOWN.ordinal());
        }
        Direction side = FluidHazards.isLighterThanAir(fluid) ? Direction.UP : Direction.DOWN;
        return 1 << side.ordinal();
    }

//? if >=1.21.1 {
    /*public @Nullable IFluidHandler capabilityHandler(@Nullable Direction side) {
        if (side != null && (blockedFluidMask() & (1 << side.ordinal())) != 0) return null;
*///?} else {
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            // When auto-output is enabled and the tank has fluid, block input on the output faces
            // to prevent backflow: liquid outputs DOWN, gas outputs UP and DOWN
            if (side != null && (blockedFluidMask() & (1 << side.ordinal())) != 0) return LazyOptional.empty();
            return side==null?handler.cast():componentFaceCaps.computeIfAbsent(side,face->LazyOptional.of(()->gregtech.content.cover.ComponentCoverAccess.fluids(this,face,this))).cast();
        }
//?}
//? if >=1.21.1 {
        /*return side==null?this:gregtech.content.cover.ComponentCoverAccess.fluids(this,side,this);
*///?} else {
        return super.getCapability(cap, side);
//?}
    }

//? if >=1.21.1 {
    /*public void contentsChanged(){
        setChanged();
        if (level != null) {
            int blocked = blockedFluidMask();
            // The handler reads contents and covers live. Only output-side availability needs
            // rediscovery; invalidating it on every amount change discards neighbouring caches.
            boolean accessChanged = !fluidAccessKnown || blockedFluidMask != blocked;
            fluidAccessKnown = true;
            blockedFluidMask = blocked;
            if (accessChanged) level.invalidateCapabilities(worldPosition);
            if (!level.isClientSide) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
        }
*///?} else {
    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handler.invalidate();
        componentFaceCaps.values().forEach(LazyOptional::invalidate);componentFaceCaps.clear();
//?}
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    // === NBT ===

    /**
     * Sparse harvested data, following GT6 Team / Gregorius Techneticies
     * TileEntityBase08Barrel.writeItemNBT2 and FluidTankGT.writeToNBT (LGPL-3.0-or-later).
     * Original persistence: Copyright (c) 2025 GregTech-6 Team.
     * World saves remain complete. Retained fluid filters, covers and nondefault settings
     * still make this a data-bearing item, so recovery cannot discard them.
     */
    public CompoundTag saveItemData(@Nullable HolderLookup.Provider lookup) {
        CompoundTag tag = NbtCompat.saveWithoutMetadata(this, lookup);
        if (temperature == GregTechConstants.DEF_ENV_TEMP) tag.remove(NBT_TEMPERATURE);
        if (!autoOutput) tag.remove(NBT_AUTO_OUTPUT);
        if (!softHammerState) tag.remove(NBT_SOFT_HAMMER);
        if (tag.contains("gt.painted", net.minecraft.nbt.Tag.TAG_BYTE) && !tag.getBoolean("gt.painted"))
            tag.remove("gt.painted");
        if (tank.getFluidLong().isEmpty() && tank.getAmount() == 0
                && tank.baseCapacity() == spec.capacity()) tag.remove("gt.tank");
//? if >=1.21.1 {
        /*if (!tag.isEmpty()) tag.putString("id",
                net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(getType()).toString());
*///?} else {
        // Forge creates these wrappers even when there is no persistent capability data.
        // Never discard a nonempty wrapper, or an unexpected tag type from an integration.
        for (String key : java.util.List.of("ForgeCaps", "ForgeData")) {
            if (tag.contains(key, net.minecraft.nbt.Tag.TAG_COMPOUND) && tag.getCompound(key).isEmpty())
                tag.remove(key);
        }
//?}
        return tag;
    }

    @Override
    protected void loadData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.loadData(tag, lookup);
        componentCovers.load(tag, lookup);
        temperature = tag.contains(NBT_TEMPERATURE) ? tag.getLong(NBT_TEMPERATURE) : GregTechConstants.DEF_ENV_TEMP;
        autoOutput = tag.contains(NBT_AUTO_OUTPUT) && tag.getBoolean(NBT_AUTO_OUTPUT);
        softHammerState = tag.contains(NBT_SOFT_HAMMER) && tag.getBoolean(NBT_SOFT_HAMMER);
        if (tag.contains("gt.tank")) NbtCompat.readTank(tank, tag.getCompound("gt.tank"), lookup);
//? if >=1.21.1 {
        /*fluidAccessKnown = false;
*///?}
    }

    @Override
    protected void saveData(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.saveData(tag, lookup);
        componentCovers.save(tag, lookup);
        tag.putLong(NBT_TEMPERATURE, temperature);
        tag.putBoolean(NBT_AUTO_OUTPUT, autoOutput);
        tag.putBoolean(NBT_SOFT_HAMMER, softHammerState);
        CompoundTag tankTag = new CompoundTag();
        NbtCompat.writeTank(tank, tankTag, lookup);
        tag.put("gt.tank", tankTag);
    }

    @Override
    protected void writeUpdateTag(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.writeUpdateTag(tag, lookup);
        tag.putLong(NBT_TEMPERATURE, temperature);
        tag.putBoolean(NBT_AUTO_OUTPUT, autoOutput);
        tag.putBoolean(NBT_SOFT_HAMMER, softHammerState);
        CompoundTag tankTag = new CompoundTag();
        NbtCompat.writeTank(tank, tankTag, lookup);
        tag.put("gt.tank", tankTag);
        componentCovers.save(tag, lookup);
    }

    @Override
    protected void readUpdateTag(CompoundTag tag, @Nullable HolderLookup.Provider lookup) {
        super.readUpdateTag(tag, lookup);
        componentCovers.load(tag, lookup);
        if (tag.contains(NBT_TEMPERATURE)) temperature = tag.getLong(NBT_TEMPERATURE);
        if (tag.contains(NBT_AUTO_OUTPUT)) autoOutput = tag.getBoolean(NBT_AUTO_OUTPUT);
        if (tag.contains(NBT_SOFT_HAMMER)) softHammerState = tag.getBoolean(NBT_SOFT_HAMMER);
        if (tag.contains("gt.tank")) NbtCompat.readTank(tank, tag.getCompound("gt.tank"), lookup);
    }

    public long getTemperature() { return temperature; }
    public FluidTankGT getFluidTank() { return tank; }
    public boolean isAutoOutput() { return autoOutput; }
    public boolean getSoftHammerState() { return softHammerState; }

    public void toggleAutoOutput() {
        autoOutput = !autoOutput;
//? if >=1.21.1 {
        /*contentsChanged();
*///?} else {
        setChanged();
//?}
    }

    /** Current soft-hammer action: unseal an empty barrel or toggle a filled one. */
    public void toggleSoftHammerState() {
        if (tank.isEmpty()) {
            softHammerState = false;
        } else {
            softHammerState = !softHammerState;
        }
        setChanged();
    }
}
