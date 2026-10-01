package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.machine.CrucibleSpec;
import com.gregtech.gregtech.api.machine.ITileEntityCrucible;
import com.gregtech.gregtech.api.machine.ITileEntityMold;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.block.machine.CrucibleFaucetBlock;
import com.gregtech.gregtech.data.GregTechConstants;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Crucible faucet block entity (GT6 {@code MultiTileEntityFaucet}).
 * <p>
 * Transfers molten material from an adjacent crucible to a mold/basin below.
 * Supports: right-click to manually transfer, auto-pull mode, redstone trigger.
 */
public class CrucibleFaucetBlockEntity extends BlockEntity implements ITileEntityMold {

    /** Facing direction (which side the input connects to). */
    private Direction facing = Direction.NORTH;

    /** Auto-pull mode: continuously pull from crucible when redstone is active. */
    private boolean autoPull = false;

    private static final String NBT_FACING = "gt.faucet.facing";
    private static final String NBT_AUTO_PULL = "gt.faucet.auto_pull";

    private final CrucibleSpec spec;

    public CrucibleFaucetBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.spec = getSpecFromState(state);
        if (state.getBlock() instanceof CrucibleFaucetBlock) {
            this.facing = state.getValue(CrucibleFaucetBlock.FACING);
        }
    }

    public CrucibleFaucetBlockEntity(BlockPos pos, BlockState state) {
        this(GTBlockEntities.CRUCIBLE_FAUCET.get(), pos, state);
    }

    private static CrucibleSpec getSpecFromState(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CrucibleFaucetBlock faucetBlock) {
            return faucetBlock.spec();
        }
        throw new IllegalStateException("CrucibleFaucetBlockEntity placed on non-faucet block: " + block);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrucibleFaucetBlockEntity be) {
        if (!level.isClientSide) {
            be.tickServer();
        }
    }

    private void tickServer() {
        // Auto-pull: every 20 ticks (1 second) when enabled and has redstone signal
        // Or when autoPull is always-on (no redstone required)
        if (autoPull) {
            boolean shouldPull = !autoPull || hasRedstoneIncoming();
            if (shouldPull) {
                pullFromCrucible();
            }
        }
    }

    /**
     * Attempt to pull molten material from the connected crucible and push it downward to mold/basin.
     * {@code fillMoldAtSide} → {@code fillMold} → {@code pushMaterialToMold} handles the full transfer chain.
     */
    public boolean pullFromCrucible() {
        if (level == null) return false;

        BlockEntity adjacent = level.getBlockEntity(worldPosition.relative(facing));
        if (!(adjacent instanceof ITileEntityCrucible crucible)) {
            return false;
        }

        return crucible.fillMoldAtSide(this, facing.getOpposite().ordinal(), facing.ordinal());
    }

    /**
     * Toggle auto-pull mode (monkey wrench).
     */
    public boolean toggleAutoPull() {
        autoPull = !autoPull;
        syncToClient();
        return autoPull;
    }

    /**
     * Set facing direction (for wrench rotation).
     */
    public void setFacing(Direction newFacing) {
        if (newFacing.getAxis().isHorizontal()) {
            this.facing = newFacing;
            syncToClient();
        }
    }

    private boolean hasRedstoneIncoming() {
        if (level == null) return false;
        return level.hasSignal(worldPosition, Direction.UP) ||
               level.hasSignal(worldPosition, Direction.DOWN) ||
               level.hasSignal(worldPosition, facing) ||
               level.hasSignal(worldPosition, facing.getOpposite());
    }

    // === ITileEntityMold implementation ===

    @Override
    public boolean isMoldInputSide(int side) {
        // Faucet only accepts input from the facing side (the crucible connection)
        return side == facing.ordinal();
    }

    @Override
    public long getMoldMaxTemperature() {
        return spec.meltDownTemperatureK();
    }

    @Override
    public long getMoldRequiredMaterialUnits() {
        // Delegate to the mold below
        BlockEntity below = level != null ? level.getBlockEntity(worldPosition.below()) : null;
        if (below instanceof ITileEntityMold mold) {
            return mold.getMoldRequiredMaterialUnits();
        }
        return 0;
    }

    @Override
    public long fillMold(GTMaterial material, long amount, long temperature, int side) {
        if (side != facing.ordinal()) return 0;
        if (material == null) return 0;
        // GT6: acid melts reject non-acid-proof faucets
        if (!spec().acidProof() && material.has(com.gregtech.gregtech.api.material.MaterialProperty.ACID)) return 0;
        if (temperature > getMoldMaxTemperature()) {
            meltdown();
            return 0;
        }

        // Push to mold below immediately
        return pushMaterialToMold(material, amount, temperature);
    }

    private long pushMaterialToMold(GTMaterial material, long amount, long temperature) {
        if (level == null) return 0;

        BlockPos below = worldPosition.below();
        int iterations = 0;
        while (below.getY() >= level.getMinBuildHeight() && iterations < 16) {
            BlockEntity be = level.getBlockEntity(below);
            if (be instanceof CrucibleFaucetBlockEntity) {
                below = below.below();
                iterations++;
                continue;
            }
            if (be instanceof ITileEntityMold mold) {
                return mold.fillMold(material, amount, temperature, Direction.UP.ordinal());
            }
            break;
        }
        return 0;
    }

    @Override
    public long getMoldContentAmount() {
        // Faucet doesn't store material, it passes through immediately
        return 0;
    }

    @Override
    public GTMaterial getMoldContentMaterial() {
        return null;
    }

    private void meltdown() {
        SmelteryBlockEntityHelper.meltdown(level, worldPosition);
    }

    // === NBT ===

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(NBT_FACING)) {
            facing = Direction.from3DDataValue(tag.getInt(NBT_FACING));
        }
        autoPull = tag.getBoolean(NBT_AUTO_PULL);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(NBT_FACING, facing.get3DDataValue());
        tag.putBoolean(NBT_AUTO_PULL, autoPull);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt(NBT_FACING, facing.get3DDataValue());
        tag.putBoolean(NBT_AUTO_PULL, autoPull);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        if (tag.contains(NBT_FACING)) {
            facing = Direction.from3DDataValue(tag.getInt(NBT_FACING));
        }
        if (tag.contains(NBT_AUTO_PULL)) {
            autoPull = tag.getBoolean(NBT_AUTO_PULL);
        }
    }

    private void syncToClient() {
        SmelteryBlockEntityHelper.syncToClient(this);
    }

    // Accessors
    public Direction getFacing() { return facing; }
    public boolean isAutoPull() { return autoPull; }
    public CrucibleSpec spec() { return spec; }
}
