package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.fluid.FluidHazards;
import com.gregtech.gregtech.api.fluid.FluidTankGT;
import com.gregtech.gregtech.api.machine.TankSpec;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeInputs;
import com.gregtech.gregtech.block.machine.FluidPipeBlock;
import com.gregtech.gregtech.block.machine.TankBlock;
import com.gregtech.gregtech.data.GregTechConstants;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.registry.GTFluids;
import com.gregtech.gregtech.registry.GTTechnological;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** GT6 fluid container tile entity. Handles wood barrels, plastic canisters, and metal drums. */
public class TankBlockEntity extends BlockEntity implements IFluidHandler {
    private final TankSpec spec;
    private final FluidTankGT tank;
    private long temperature = GregTechConstants.DEF_ENV_TEMP;
    private boolean autoOutput;
    /** GT6 {@code mMode & B[1]}: the barrel is sealed, which makes it ferment its contents. */
    private boolean softHammerState;
    /** GT6 {@code mSealedTime}: ticks the sealed barrel has been fermenting. */
    private long sealedTime;
    /** GT6 {@code mMaxSealedTime}: ticks a full fermentation takes for the current fill level. */
    private long maxSealedTime;
    /** Cached fermenter recipe of the sealed barrel, plus the fluid it was matched for. */
    private Recipe fermentRecipe;
    private Fluid fermentInput;

    private static final String NBT_TEMPERATURE = "gt.temperature";
    private static final String NBT_AUTO_OUTPUT = "gt.auto_output";
    private static final String NBT_SOFT_HAMMER = "gt.soft_hammer";
    private static final String NBT_SEALED_TIME = "gt.sealed_time";
    private static final String NBT_MAX_SEALED_TIME = "gt.max_sealed_time";

    private LazyOptional<IFluidHandler> handler = LazyOptional.of(() -> this);

    public TankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.spec = specFromState(state);
        this.tank = new FluidTankGT(spec.capacity())
                .setOnChanged(this::setChanged)
                .setGasProof(spec.gasProof())
                .setAcidProof(spec.acidProof())
                .setPlasmaProof(spec.plasmaProof())
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
                SmelteryBlockEntityHelper.meltdown(level, worldPosition);
                tank.setFluid(FluidStack.EMPTY);
                return;
            }

            // GT6 TileEntityBase08Barrel:170-186 — the hazard checks sit between the meltdown check
            // and the barrel's own modes, and they eat the contents before anything else runs.
            if (FluidHazards.proofRejects(fluid.getFluid(), spec.gasProof(), spec.acidProof(), spec.plasmaProof())) {
                corrode(fluid);
                return;
            }

            // Auto-output
            if (autoOutput && tank.getAmount() > 0) {
                autoOutputFluid(fluid);
            }

            // GT6 TileEntityBase08Barrel#onTick2:188-207 — a sealed barrel ferments its content.
            if (softHammerState) tickFermentation(fluid);
        } else if (softHammerState && sealedTime > 0) {
            // GT6 clears the seal when the barrel runs dry.
            resetSeal();
        }

        setChanged();
    }

    // ── Fluid hazards (GT6 TileEntityBase08Barrel) ───────────────────────────

    /**
     * GT6 {@code TileEntityBase08Barrel:170-186}: a barrel holding a fluid its proof flags do not
     * cover loses the whole content at once (not the pipe's few units a tick) and fizzes, and acid
     * also dissolves the barrel itself with {@code setToAir()} - the one case where the vessel is
     * destroyed outright and with no chance roll, unlike the pipe's {@code rng(100) == 0}.
     *
     * <p>The temperature meltdown stays above this and keeps its own
     * {@link SmelteryBlockEntityHelper#meltdown} (GT6 {@code :162}), which fills the block with lava;
     * acid instead leaves air, which is what {@code removeBlock} does here.
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

    // ── Barrel fermentation (GT6 TileEntityBase08Barrel) ─────────────────────

    /**
     * Ticks the sealed-barrel fermentation.
     * <p>
     * Original: the barrel looks {@code RM.Fermenter} up with {@code ST.tag(0)} as its sole
     * item input and no power
     * and only accepts recipes whose input and output are both non-gaseous — a barrel cannot hold
     * pressure. The duration is GT6's
     * {@code ceil(|EU/t * duration| * max(1, tank) / max(1, recipeInput))}, and on completion the
     * whole tank is replaced by the recipe's first fluid output scaled by the same ratio
     * ({@code FL.mul(output, tank, input)}).
     * </p>
     */
    private void tickFermentation(FluidStack fluid) {
        if (fermentRecipe != null && fermentInput != fluid.getFluid()) resetSeal();
        if (fermentRecipe == null) {
            fermentRecipe = findFermentationRecipe(fluid);
            fermentInput = fermentRecipe == null ? null : fluid.getFluid();
            sealedTime = 0;
            maxSealedTime = 0;
            if (fermentRecipe == null) return;
        }
        if (maxSealedTime <= 0) {
            maxSealedTime = sealedDuration(fermentRecipe, tank.getAmount());
            if (maxSealedTime <= 0) return;
        }
        if (sealedTime < maxSealedTime) {
            sealedTime++;
            setChanged();
            return;
        }

        FluidStack in = fermentRecipe.mFluidInputs[0];
        FluidStack out = fermentRecipe.mFluidOutputs[0];
        long produced = com.gregtech.gregtech.api.fluid.BarrelFermentationRules.output(out.getAmount(),tank.getAmount(),in.getAmount());
        tank.setFluid(new FluidStack(out.getFluid(), (int) Math.min(Math.max(1, produced), tank.getCapacity())));
        resetSeal();
        setChanged();
    }

    /** GT6's {@code UT.Code.divup(max(1, |EUt * duration|) * max(1, amount), max(1, input))}. */
    public static long sealedDuration(Recipe recipe, long amount) {
        return com.gregtech.gregtech.api.fluid.BarrelFermentationRules.duration(recipe.mEUt,recipe.mDuration,amount,recipe.mFluidInputs[0].getAmount());
    }

    /**
     * The barrel's fermenter recipe for a fluid: matches on fluid type and scales the amount,
     * but cannot satisfy recipes that require a flower or other item. GT6 passes its virtual
     * {@code ST.tag(0)} as the sole item input to RM.Fermenter.findRecipe; the preceding
     * {@code NI} argument is the unused special slot, not the item inputs. Gaseous inputs and
     * outputs are rejected.
     */
    public static Recipe findFermentationRecipe(FluidStack fluid) {
        if (fluid.isEmpty()) return null;
        var selector = GTTechnological.selectorTag(0);
        if (selector == null) return null;
        ItemStack itemInput = new ItemStack(selector);
        for (Recipe recipe : MachineRecipeMaps.Fermenter.mRecipeList) {
            if (!recipe.mEnabled || recipe.mFakeRecipe) continue;
            if (recipe.mInputs.length != 1 || !RecipeInputs.matches(recipe.mInputs[0], itemInput)) continue;
            if (recipe.mFluidInputs.length != 1 || recipe.mFluidOutputs.length == 0) continue;
            FluidStack input = recipe.mFluidInputs[0];
            FluidStack output = recipe.mFluidOutputs[0];
            if (input == null || output == null || !input.isFluidEqual(fluid)) continue;
            if (GTFluids.isGas(input) || GTFluids.isGas(output)) continue;
            return recipe;
        }
        return null;
    }

    private void resetSeal() {
        sealedTime = 0;
        maxSealedTime = 0;
        fermentRecipe = null;
        fermentInput = null;
    }

    public long getSealedTime() { return sealedTime; }
    public long getMaxSealedTime() { return maxSealedTime; }
    public FluidStack getFermentationOutput() {
        return fermentRecipe == null || fermentRecipe.mFluidOutputs.length == 0
                ? FluidStack.EMPTY : fermentRecipe.mFluidOutputs[0];
    }

    private void autoOutputFluid(FluidStack fluid) {
        boolean isGas = fluid.getFluid().getFluidType().isLighterThanAir();
        Direction[] outputs = isGas ? Direction.values() : new Direction[]{Direction.DOWN};
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

            IFluidHandler target = be.getCapability(ForgeCapabilities.FLUID_HANDLER, side.getOpposite()).orElse(null);
            if (target == null) continue;

            int toSend = FluidTankGT.bindInt(Math.min(tank.getAmount(), 1000L));
            FluidStack drained = tank.drain(toSend, IFluidHandler.FluidAction.SIMULATE);
            if (drained.isEmpty()) continue;
            int filled = target.fill(drained, IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) {
                tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    private long environmentTemperature() {
        return SmelteryBlockEntityHelper.environmentTemperature(level, worldPosition);
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
            LazyOptional<IFluidHandler> cap = be.getCapability(ForgeCapabilities.FLUID_HANDLER, side.getOpposite());
            if (!cap.isPresent()) continue;
            IFluidHandler target = cap.orElse(null);
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
        if (!com.gregtech.gregtech.platform.forge.transport.FluidContainerInteraction.use(player, hand, this))
            return InteractionResult.PASS;
        setChanged();
        return InteractionResult.CONSUME;
    }

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
        // GT6 TileEntityBase08Barrel:251-254 and TileEntityBase08FluidContainer:420 — the same three
        // questions as the corrosion tick, asked at the fill gate so an unproofed hazard never gets
        // in at all. This is the "reject" half of accept/reject/destroy.
        if (FluidHazards.proofRejects(stack.getFluid(), spec.gasProof(), spec.acidProof(), spec.plasmaProof())) {
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
        // The gate has to be here as well as in isFluidValid: Forge treats isFluidValid as a query
        // only, while GT6 rejects inside fill (TileEntityBase08Barrel:248-255).
        if (resource.isEmpty() || !isFluidValid(0, resource)) return 0;
        return tank.fill(resource, action);
    }

    @NotNull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) { return tank.drain(resource, action); }

    @NotNull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) { return tank.drain(maxDrain, action); }

    // === Capability ===

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            // When auto-output is enabled and the tank has fluid, block input from the output side
            // to prevent backflow: liquid outputs DOWN, gas outputs UP
            if (side != null && autoOutput && !tank.isEmpty()) {
                boolean isGas = tank.getFluid().getFluid().getFluidType().isLighterThanAir();
                if (isGas && side == Direction.UP) return LazyOptional.empty();
                if (!isGas && side == Direction.DOWN) return LazyOptional.empty();
            }
            return handler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handler.invalidate();
    }

    // === NBT ===

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        temperature = tag.contains(NBT_TEMPERATURE) ? tag.getLong(NBT_TEMPERATURE) : GregTechConstants.DEF_ENV_TEMP;
        autoOutput = tag.contains(NBT_AUTO_OUTPUT) && tag.getBoolean(NBT_AUTO_OUTPUT);
        softHammerState = tag.contains(NBT_SOFT_HAMMER) && tag.getBoolean(NBT_SOFT_HAMMER);
        sealedTime = tag.contains(NBT_SEALED_TIME) ? tag.getLong(NBT_SEALED_TIME) : 0;
        maxSealedTime = tag.contains(NBT_MAX_SEALED_TIME) ? tag.getLong(NBT_MAX_SEALED_TIME) : 0;
        fermentRecipe = null;
        fermentInput = null;
        if (tag.contains("gt.tank")) tank.readFromNBT(tag.getCompound("gt.tank"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong(NBT_TEMPERATURE, temperature);
        tag.putBoolean(NBT_AUTO_OUTPUT, autoOutput);
        tag.putBoolean(NBT_SOFT_HAMMER, softHammerState);
        tag.putLong(NBT_SEALED_TIME, sealedTime);
        tag.putLong(NBT_MAX_SEALED_TIME, maxSealedTime);
        CompoundTag tankTag = new CompoundTag();
        tank.writeToNBT(tankTag);
        tag.put("gt.tank", tankTag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putLong(NBT_TEMPERATURE, temperature);
        tag.putBoolean(NBT_AUTO_OUTPUT, autoOutput);
        tag.putBoolean(NBT_SOFT_HAMMER, softHammerState);
        tag.putLong(NBT_SEALED_TIME, sealedTime);
        tag.putLong(NBT_MAX_SEALED_TIME, maxSealedTime);
        CompoundTag tankTag = new CompoundTag();
        tank.writeToNBT(tankTag);
        tag.put("gt.tank", tankTag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        if (tag.contains(NBT_TEMPERATURE)) temperature = tag.getLong(NBT_TEMPERATURE);
        if (tag.contains(NBT_AUTO_OUTPUT)) autoOutput = tag.getBoolean(NBT_AUTO_OUTPUT);
        if (tag.contains(NBT_SOFT_HAMMER)) softHammerState = tag.getBoolean(NBT_SOFT_HAMMER);
        if (tag.contains(NBT_SEALED_TIME)) sealedTime = tag.getLong(NBT_SEALED_TIME);
        if (tag.contains(NBT_MAX_SEALED_TIME)) maxSealedTime = tag.getLong(NBT_MAX_SEALED_TIME);
        if (tag.contains("gt.tank")) tank.readFromNBT(tag.getCompound("gt.tank"));
    }

    public long getTemperature() { return temperature; }
    public FluidTankGT getFluidTank() { return tank; }
    public boolean isAutoOutput() { return autoOutput; }
    public boolean getSoftHammerState() { return softHammerState; }

    public void toggleAutoOutput() {
        autoOutput = !autoOutput;
        setChanged();
    }

    /**
     * GT6 {@code TileEntityBase08Barrel#onToolClick2} (soft hammer): an empty barrel is unsealed,
     * otherwise the seal is toggled. Either way the fermentation progress is discarded.
     */
    public void toggleSoftHammerState() {
        if (tank.isEmpty()) {
            softHammerState = false;
        } else {
            softHammerState = !softHammerState;
        }
        resetSeal();
        setChanged();
    }
}
