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
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.minecraft.core.registries.BuiltInRegistries;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.ForgeRegistries;
//?}

/**
 * Fixed spring variants take their fluid and roll interval from the registry definition.
 * Legacy carriers keep an explicit output and interval. Server ticks activate on replaceable
 * headspace, then roll before overwriting the block directly above; the original global
 * startup cadence, finite accumulation and horizontal source growth are separate contracts.
 */
public class FluidSpringBlockEntity extends BlockEntity {

    /** Default legacy roll bound; fixed variants use the interval in their catalog definition. */
    public static final int DEFAULT_AMOUNT = 1000;

//? if >=1.21.1 {
    /*public static final net.neoforged.neoforge.client.model.data.ModelProperty<Fluid> MODEL_FLUID = new net.neoforged.neoforge.client.model.data.ModelProperty<>();
*///?} else {
    public static final net.minecraftforge.client.model.data.ModelProperty<Fluid> MODEL_FLUID = new net.minecraftforge.client.model.data.ModelProperty<>();
//?}

    private String fluidId = "";
    private int amount = DEFAULT_AMOUNT;
    private boolean active;

    public FluidSpringBlockEntity(BlockPos pos, BlockState state) {
        super(GTBlockEntities.FLUID_SPRING.get(), pos, state);
    }

    private com.gregtech.gregtech.worldgen.FluidSpringRules.Spring definition() {
        return ((com.gregtech.gregtech.block.FluidSpringBlock) getBlockState().getBlock()).spring();
    }

    public String fluidId() {
        return definition() == null ? fluidId : definition().fluidId();
    }

    public int amount() {
        return definition() == null ? amount : definition().amount();
    }

    public boolean active() {
        return active;
    }

    /** Metadata view for tools: output fluid and nominal roll bound, not a drainable inventory. */
    public FluidStack springFluid() {
        Fluid fluid = fluid();
        return fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, amount());
    }

    public Fluid fluid() {
//? if >=1.21.1 {
        /*return fluidId().isEmpty() ? null : BuiltInRegistries.FLUID.get(ResourceLocation.parse(fluidId()));
*///?} else {
        return fluidId().isEmpty() ? null : ForgeRegistries.FLUIDS.getValue(ResourceLocation.parse(fluidId()));
//?}
    }

    /** Upgrade recognized legacy spring metadata to the fixed source variant/rate. */
    public void setSpring(String fluidId, int amount) {
        var variant = com.gregtech.gregtech.registry.GTFluidSprings.byFluid(fluidId);
        if (variant == null) {
            if (definition() == null) {
                this.fluidId = fluidId == null ? "" : fluidId;
                this.amount = Math.max(1, amount);
                setChanged();
            }
            return;
        }
        if (level == null) {
            setBlockState(variant.defaultBlockState());
            this.fluidId = "";
            return;
        }
        if (level.isClientSide) return;
        boolean wasActive = active;
        level.setBlock(worldPosition, variant.defaultBlockState(), 3);
        if (level.getBlockEntity(worldPosition) instanceof FluidSpringBlockEntity next) {
            next.fluidId = "";
            next.active = wasActive;
            next.setChanged();
        }
    }

    public static <T extends FluidSpringBlockEntity> void serverTick(Level level, BlockPos pos, BlockState state, T be) {
        be.tick();
    }

    /** Server activation and randomized output for this port's fixed and legacy springs. */
    public void tick() {
        if (level == null || level.isClientSide) return;
        if (definition() == null && !fluidId.isEmpty() && com.gregtech.gregtech.registry.GTFluidSprings.byFluid(fluidId) != null) {
            setSpring(fluidId, amount);
            return;
        }
        BlockState above = level.getBlockState(worldPosition.above());
        Fluid fluid = fluid();
        if (fluid == null) return;
        boolean sameFluidAbove = !above.getFluidState().isEmpty() && above.getFluidState().getType() == fluid;
        if (!active) {
            // This port waits for air or replaceable headspace before setting active.
            if (above.isAir() || above.canBeReplaced()) {
                active = true;
                setChanged();
            }
            else return;
        }
        if (!com.gregtech.gregtech.worldgen.FluidSpringRules.rolls(level.random::nextInt, amount())) return;
        emit();
    }

    /**
     * Replaces only the block directly above with the output block's default state.
     * Rewriting an existing source does not implement the original horizontal source growth
     * or finite-fluid accumulation.
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
        // This lookup assumes matching fluid/block keys; it does not query a fluid-owned world block.
//? if >=1.21.1 {
        /*ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid);
        return id == null ? null : BuiltInRegistries.BLOCK.get(id);
*///?} else {
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(fluid);
        return id == null ? null : ForgeRegistries.BLOCKS.getValue(id);
//?}
    }

    /** Nominal roll interval after positivity clamping; not the first-output delay. */
    public static int expectedTicks(int amount) {
        return com.gregtech.gregtech.worldgen.FluidSpringRules.positiveAmount(amount);
    }

    /** Uses the supplied RNG and the port's clamped roll interval. */
    public static boolean rollsThisTick(RandomSource random, int amount) {
        return com.gregtech.gregtech.worldgen.FluidSpringRules.rolls(random::nextInt, amount);
    }

    @Override
//? if >=1.21.1 {
    /*protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
*///?} else {
    public void load(CompoundTag tag) {
        super.load(tag);
//?}
        fluidId = definition() == null ? tag.getString("spring") : "";
        amount = com.gregtech.gregtech.worldgen.FluidSpringRules.positiveAmount(tag.contains("amount") ? tag.getInt("amount") : DEFAULT_AMOUNT);
        active = tag.getBoolean("active");
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
//? if >=1.21.1 {
    /*protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
*///?} else {
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
//?}
        if (definition() == null) {
            if (!fluidId.isEmpty()) tag.putString("spring", fluidId);
            tag.putInt("amount", amount);
        }
        if (active) tag.putBoolean("active", true);
    }

//? if >=1.21.1 {
    /*@Override
    public net.neoforged.neoforge.client.model.data.ModelData getModelData() {
*///?} else {
    @Override
    public net.minecraftforge.client.model.data.ModelData getModelData() {
//?}
        Fluid current = fluid();
//? if >=1.21.1 {
        /*return net.neoforged.neoforge.client.model.data.ModelData.builder().with(MODEL_FLUID,
                current == null ? net.minecraft.world.level.material.Fluids.WATER : current).build();
*///?} else {
        return net.minecraftforge.client.model.data.ModelData.builder().with(MODEL_FLUID,
                current == null ? net.minecraft.world.level.material.Fluids.WATER : current).build();
//?}
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
//? if >=1.21.1 {
    /*@Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup) {
        return saveWithoutMetadata(lookup);
    }
*///?} else {
    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }
//?}
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) requestModelDataUpdate();
    }
}
