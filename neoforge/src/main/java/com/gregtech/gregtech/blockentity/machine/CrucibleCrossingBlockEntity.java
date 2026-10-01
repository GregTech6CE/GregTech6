package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.machine.CrucibleSpec;
import com.gregtech.gregtech.api.machine.ITileEntityCrucible;
import com.gregtech.gregtech.api.machine.ITileEntityMold;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.block.machine.CrucibleCrossingBlock;
import com.gregtech.gregtech.data.GregTechConstants;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Crucible crossing block entity (GT6 {@code MultiTileEntityCrossing}).
 * <p>
 * A 4-way channel block that routes molten material from any adjacent crucible
 * to all other horizontal directions. Supports redstone control.
 */
public class CrucibleCrossingBlockEntity extends BlockEntity implements ITileEntityCrucible {

    /** Redstone control state. */
    private boolean redstonePowered = false;

    /** Lock ID for preventing infinite routing loops. */
    private long lockId = 0;

    private static final String NBT_REDSTONE = "gt.crossing.redstone";

    private final CrucibleSpec spec;

    /** Global lock counter (prevents routing loops). */
    private static long sLockCounter = 0;
    private static boolean sLockActive = false;

    public CrucibleCrossingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.spec = getSpecFromState(state);
    }

    public CrucibleCrossingBlockEntity(BlockPos pos, BlockState state) {
        this(GTBlockEntities.CRUCIBLE_CROSSING.get(), pos, state);
    }

    private static CrucibleSpec getSpecFromState(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CrucibleCrossingBlock crossingBlock) {
            return crossingBlock.spec();
        }
        throw new IllegalStateException("CrucibleCrossingBlockEntity placed on non-crossing block: " + block);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrucibleCrossingBlockEntity be) {
        if (!level.isClientSide) {
            be.tickServer();
        }
    }

    private void tickServer() {
        // Check for redstone signal
        boolean hasRedstone = hasRedstoneIncoming();
        if (hasRedstone != redstonePowered) {
            redstonePowered = hasRedstone;
            syncToClient();
        }
    }

    private boolean hasRedstoneIncoming() {
        if (level == null) return false;
        return level.hasSignal(worldPosition, Direction.UP) || level.hasSignal(worldPosition, Direction.DOWN);
    }

    // === ITileEntityCrucible implementation ===

    @Override
    public boolean fillMoldAtSide(ITileEntityMold mold, int crucibleSide, int moldSide) {
        if (level == null || redstonePowered) return false;

        // Start a new routing wave if this is the outermost call
        boolean startedRouting = !sLockActive;
        if (startedRouting) {
            sLockActive = true;
            sLockCounter++;
        }

        // Prevent revisiting this crossing in the same routing wave
        if (lockId == sLockCounter) return false;
        lockId = sLockCounter;

        // Route to all other horizontal directions except the source
        boolean result = false;
        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP || dir == Direction.DOWN) continue;
            if (dir.ordinal() == crucibleSide) continue;

            BlockEntity be = level.getBlockEntity(worldPosition.relative(dir));
            if (be instanceof ITileEntityCrucible crucible) {
                if (crucible.fillMoldAtSide(mold, dir.getOpposite().ordinal(), moldSide)) {
                    result = true;
                }
            }
        }

        if (startedRouting) {
            sLockActive = false;
        }
        return result;
    }

    @Override
    public long getCrucibleTemperature() {
        // Crossing doesn't store temperature, but needs to implement the interface
        return GregTechConstants.DEF_ENV_TEMP;
    }

    @Override
    public long getCrucibleContentAmount() {
        // Crossing doesn't store material
        return 0;
    }

    // === NBT ===

    @Override
    protected void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);
        redstonePowered = tag.getBoolean(NBT_REDSTONE);
    }

    @Override
    protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);
        tag.putBoolean(NBT_REDSTONE, redstonePowered);
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup) {
        CompoundTag tag = super.getUpdateTag(lookup);
        tag.putBoolean(NBT_REDSTONE, redstonePowered);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.handleUpdateTag(tag,lookup);
        if (tag.contains(NBT_REDSTONE)) {
            redstonePowered = tag.getBoolean(NBT_REDSTONE);
        }
    }

    private void syncToClient() {
        SmelteryBlockEntityHelper.syncToClient(this);
    }

    // Accessors
    public boolean isRedstonePowered() { return redstonePowered; }
    public CrucibleSpec spec() { return spec; }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    @Override public void onDataPacket(net.minecraft.network.Connection connection,net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet,net.minecraft.core.HolderLookup.Provider lookup){if(packet.getTag()!=null)handleUpdateTag(packet.getTag(),lookup);}
}
