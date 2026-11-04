package gregtech.content.blockentity.machine;

import gregtech.engine.machine.CrucibleSpec;
import gregtech.engine.machine.ITileEntityCrucible;
import gregtech.engine.machine.ITileEntityMold;
import gregtech.api.material.GTMaterial;
import gregtech.api.material.GTValues;
import gregtech.engine.fluid.MoltenFluidHandler;
import gregtech.engine.fluid.MoltenFluidPlans;
import gregtech.engine.material.GTMaterialRegistry;
import gregtech.content.blockentity.tool.ProcessingToolBlockEntity;
import gregtech.content.block.machine.CrucibleFaucetBlock;
import gregtech.content.data.GregTechConstants;
import gregtech.content.data.GregTechTags;
import gregtech.content.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}
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

    /** GT6 auto-pull: {@code true} pours once a second, {@code false} pours on a redstone signal. */
    private boolean autoPull = false;
    private boolean redstoneInput = false;

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

    private static final int AUTO_PULL_INTERVAL = 20;

    private void tickServer() {
        boolean powered = hasRedstoneIncoming();
        if (autoPull) {
            if (level != null && level.getGameTime() % AUTO_PULL_INTERVAL == 0) pullFromCrucible();
        } else if (powered && !redstoneInput) {
            // GT6 pours in redstone mode on a block update while powered; the rising edge avoids retrying every tick.
            pullFromCrucible();
        }
        redstoneInput = powered;
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
        setAutoPull(!autoPull);
        return autoPull;
    }

    /** Force auto-pull mode (soft hammer resets to the redstone trigger). */
    public void setAutoPull(boolean value) {
        if (autoPull == value) return;
        autoPull = value;
        syncToClient();
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
        return level.hasNeighborSignal(worldPosition);
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
        // Delegate to the mold below; an open vessel below always takes one unit.
        BlockEntity below = level != null ? level.getBlockEntity(worldPosition.below()) : null;
        if (below instanceof ITileEntityMold mold) {
            return mold.getMoldRequiredMaterialUnits();
        }
        if (below instanceof ProcessingToolBlockEntity vessel && vessel.acceptsFaucetPour()) {
            return GTValues.U;
        }
        return 0;
    }

    @Override
    public long fillMold(GTMaterial material, long amount, long temperature, int side) {
        if (side != facing.ordinal()) return 0;
        if (material == null) return 0;
        // GT6: acid melts reject non-acid-proof faucets
        if (!spec().acidProof() && material.has(gregtech.api.material.MaterialProperty.ACID)) return 0;
        if (temperature > getMoldMaxTemperature()) {
            meltdown();
            return 0;
        }

        // Push to mold below immediately
        return pushMaterialToMold(material, amount, temperature);
    }

    private long pushMaterialToMold(GTMaterial material, long amount, long temperature) {
        if (level == null) return 0;

        // GT6 falls through extra faucets and open air, then pours into the first mold,
        // bathing pot or mixing bowl; a colliding block stops the search.
        BlockPos below = worldPosition.below();
        while (below.getY() >= level.getMinBuildHeight()) {
            BlockEntity be = level.getBlockEntity(below);
            if (be instanceof CrucibleFaucetBlockEntity) {
                below = below.below();
                continue;
            }
            if (be instanceof ITileEntityMold mold) {
                return mold.fillMold(material, amount, temperature, Direction.UP.ordinal());
            }
            if (be instanceof ProcessingToolBlockEntity vessel && vessel.acceptsFaucetPour()) {
                return fillOpenVessel(below, material, amount);
            }
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) break;
            below = below.below();
        }
        return 0;
    }

    /** GT6 pours one material unit, or the exact remainder below one, into the vessel's own tanks. */
    private long fillOpenVessel(BlockPos pos, GTMaterial material, long amount) {
        long units = Math.min(amount, GTValues.U);
        FluidStack molten = MoltenFluidHandler.moltenFluid(material, 1);
        if (molten.isEmpty()) return 0;
        var ratio = MoltenFluidHandler.ratio(material, molten);
        int millibuckets = MoltenFluidPlans.millibuckets(units, ratio);
        if (millibuckets <= 0) return 0;
        FluidStack pour = molten.copy();
        pour.setAmount(millibuckets);
        IFluidHandler sink = fluidHandlerBelow(pos);
        if (sink == null) return 0;
        // GT6's FL.fillAll accepts the whole portion or nothing, so no material is lost.
        if (sink.fill(pour, IFluidHandler.FluidAction.SIMULATE) != millibuckets) return 0;
        int filled = sink.fill(pour, IFluidHandler.FluidAction.EXECUTE);
        return Math.min(units, MoltenFluidPlans.toUnits(filled, ratio));
    }

    private IFluidHandler fluidHandlerBelow(BlockPos pos) {
//? if >=1.21.1 {
        /*return level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                pos, Direction.UP);
*///?} else {
        BlockEntity below = level.getBlockEntity(pos);
        return below == null ? null
                : below.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER,
                        Direction.UP).orElse(null);
//?}
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
//? if >=1.21.1 {
    /*protected void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);
*///?} else {
    public void load(CompoundTag tag) {
        super.load(tag);
//?}
        if (tag.contains(NBT_FACING)) {
            facing = Direction.from3DDataValue(tag.getInt(NBT_FACING));
        }
        autoPull = tag.getBoolean(NBT_AUTO_PULL);
    }

    @Override
//? if >=1.21.1 {
    /*protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);
*///?} else {
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
//?}
        tag.putInt(NBT_FACING, facing.get3DDataValue());
        tag.putBoolean(NBT_AUTO_PULL, autoPull);
    }

    @Override
//? if >=1.21.1 {
    /*public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup) {
        CompoundTag tag = super.getUpdateTag(lookup);
*///?} else {
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
//?}
        tag.putInt(NBT_FACING, facing.get3DDataValue());
        tag.putBoolean(NBT_AUTO_PULL, autoPull);
        return tag;
    }

    @Override
//? if >=1.21.1 {
    /*public void handleUpdateTag(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.handleUpdateTag(tag,lookup);
*///?} else {
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
//?}
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
//? if >=1.21.1 {
    /*@Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    @Override public void onDataPacket(net.minecraft.network.Connection connection,net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet,net.minecraft.core.HolderLookup.Provider lookup){if(packet.getTag()!=null)handleUpdateTag(packet.getTag(),lookup);}
*///?} else {
//?}
}
