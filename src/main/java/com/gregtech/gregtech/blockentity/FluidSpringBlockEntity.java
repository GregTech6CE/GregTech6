package com.gregtech.gregtech.blockentity;

import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * GT6's fluid spring ({@code MultiTileEntityFluidSpring}).
 *
 * <p>GT6's spring sits at bedrock carrying one fluid and 6000 mB of it (oil), 3000 (gas) or 500
 * (geothermal water). It activates as soon as the block above it is free and then — with a
 * {@code 1/amount} chance per tick — pushes that fluid up: if the block above already holds the same
 * fluid it raises its level, otherwise it places a fresh source block. The port keeps that: the spring
 * is a {@link com.gregtech.gregtech.block.FluidSpringBlock} with this block entity, and the fluid it
 * emits is the world block the port registers for the spring fluids.
 */
public class FluidSpringBlockEntity extends BlockEntity {
    /** GT6's fluids per spring kind: oil 6000, gas 3000, geothermal water 500, lava 1000. */
    public static final int DEFAULT_AMOUNT = 1000;

    public static final net.minecraftforge.client.model.data.ModelProperty<Fluid> MODEL_FLUID = new net.minecraftforge.client.model.data.ModelProperty<>();

    private String fluidId = "";
    private int amount = DEFAULT_AMOUNT;
    private boolean active;

    public FluidSpringBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.FLUID_SPRING.get(), pos, state);
    }

    public String fluidId() { return fluidId; }

    public int amount() { return amount; }

    public boolean active() { return active; }

    /** GT6 {@code mFluid} — the fluid this spring pushes up, as a stack of its stored amount. */
    public FluidStack springFluid() {
        Fluid fluid = fluid();
        return fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, amount);
    }

    public Fluid fluid() {
        return fluidId.isEmpty() ? null : ForgeRegistries.FLUIDS.getValue(ResourceLocation.parse(fluidId));
    }

    public void setSpring(String fluidId, int amount) {
        this.fluidId = fluidId == null ? "" : fluidId;
        this.amount = com.gregtech.gregtech.worldgen.FluidSpringRules.positiveAmount(amount);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public static <T extends FluidSpringBlockEntity> void serverTick(Level level, BlockPos pos, BlockState state, T be) {
        be.tick();
    }

    /** GT6's {@code onTick2}: activate when the space above frees up, then emit at {@code 1/amount}. */
    public void tick() {
        if (level == null || level.isClientSide) return;
        BlockState above = level.getBlockState(worldPosition.above());
        Fluid fluid = fluid();
        if (fluid == null) return;
        boolean sameFluidAbove = !above.getFluidState().isEmpty() && above.getFluidState().getType() == fluid;
        if (!active) {
            // GT6 waits for a free spot above and activates then.
            if (above.isAir() || above.canBeReplaced()) { active = true; setChanged(); }
            else return;
        }
        if (!com.gregtech.gregtech.worldgen.FluidSpringRules.rolls(level.random::nextInt,amount)) return;
        emit();
    }

    /**
     * One GT6 production step ({@code if (tProduce)}): the block above is filled with the spring's
     * fluid. GT6 raises the level when the same fluid is already there and places a source otherwise.
     *
     * @return true when the world changed
     */
    public boolean emit() {
        if (level == null) return false;
        Fluid fluid = fluid();
        if (fluid == null) return false;
        Block fluidBlock = fluid.getFluidType() == null ? null : fluidBlockOf(fluid);
        if (fluidBlock == null || fluidBlock == Blocks.AIR) return false;
        BlockPos target = worldPosition.above();
        if (level.isOutsideBuildHeight(target)) return false;
        BlockState above = level.getBlockState(target);
        if (!above.isAir() && !above.canBeReplaced() && above.getFluidState().isEmpty()) return false;
        return level.setBlock(target, fluidBlock.defaultBlockState(), 3);
    }

    private static Block fluidBlockOf(Fluid fluid) {
        FluidType type = fluid.getFluidType();
        if (type == null) return null;
        // The world block shares the fluid's registry path (registered by Loader_Fluids).
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(fluid);
        return id == null ? null : ForgeRegistries.BLOCKS.getValue(id);
    }

    /** Test/tools helper: how many ticks GT6's roll needs on average for this spring. */
    public static int expectedTicks(int amount) {
        return com.gregtech.gregtech.worldgen.FluidSpringRules.positiveAmount(amount);
    }

    /** A deterministic roll for tests: GT6 uses {@code rng(amount) == 0}. */
    public static boolean rollsThisTick(RandomSource random, int amount) {
        return com.gregtech.gregtech.worldgen.FluidSpringRules.rolls(random::nextInt,amount);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        fluidId = tag.getString("spring");
        amount = com.gregtech.gregtech.worldgen.FluidSpringRules.positiveAmount(tag.contains("amount") ? tag.getInt("amount") : DEFAULT_AMOUNT);
        active = tag.getBoolean("active");
        if(level!=null&&level.isClientSide) { requestModelDataUpdate(); level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3); }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!fluidId.isEmpty()) tag.putString("spring", fluidId);
        tag.putInt("amount", amount);
        if (active) tag.putBoolean("active", true);
    }

    @Override public net.minecraftforge.client.model.data.ModelData getModelData() {
        Fluid current=fluid();
        return net.minecraftforge.client.model.data.ModelData.builder().with(MODEL_FLUID,current==null?net.minecraft.world.level.material.Fluids.WATER:current).build();
    }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public void onLoad() {
        super.onLoad();
        if(level!=null&&level.isClientSide) requestModelDataUpdate();
    }
}
