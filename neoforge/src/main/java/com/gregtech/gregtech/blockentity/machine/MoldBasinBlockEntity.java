package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.machine.CrucibleSpec;
import com.gregtech.gregtech.api.machine.ITileEntityCrucible;
import com.gregtech.gregtech.api.machine.ITileEntityMold;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.block.machine.MoldBasinBlock;
import com.gregtech.gregtech.data.GregTechConstants;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.registry.GTBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Mold basin block entity (GT6 {@code MultiTileEntityBasin}).
 * <p>
 * Produces solid metal blocks when filled and cooled. Only accepts input from the top face.
 * Supports auto-input from adjacent crucibles and item pickup with pincers.
 */
public class MoldBasinBlockEntity extends BlockEntity implements ITileEntityMold {

    /** Current temperature (environment temperature when empty). */
    private long temperature = GregTechConstants.DEF_ENV_TEMP;

    /** Material currently in the basin. */
    @Nullable
    private GTMaterial contentMaterial = null;

    /** Amount of material in the basin (in units). */
    private long contentAmount = 0;

    /** Auto-pull directions bitmask. */
    private byte autoPullDirections = 0;

    /** Solidified output waiting for pickup (stays in container). */
    private ItemStack solidOutput = ItemStack.EMPTY;

    /** ARGB tint of the solidified material (for rendering). */
    private int solidOutputTint = 0xFFFFFFFF;

    private static final String NBT_TEMPERATURE = "gt.temperature";
    private static final String NBT_CONTENT_MATERIAL = "gt.content.material";
    private static final String NBT_CONTENT_AMOUNT = "gt.content.amount";
    private static final String NBT_AUTO_PULL = "gt.basin.auto_pull";
    private static final String NBT_SOLID_OUTPUT = "gt.basin.solid_output";
    private static final String NBT_SOLID_OUTPUT_TINT = "gt.basin.solid_tint";

    private final CrucibleSpec spec;

    public MoldBasinBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.spec = getSpecFromState(state);
    }

    public MoldBasinBlockEntity(BlockPos pos, BlockState state) {
        this(GTBlockEntities.MOLD_BASIN.get(), pos, state);
    }

    private static CrucibleSpec getSpecFromState(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof MoldBasinBlock basinBlock) {
            return basinBlock.spec();
        }
        throw new IllegalStateException("MoldBasinBlockEntity placed on non-basin block: " + block);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MoldBasinBlockEntity be) {
        if (!level.isClientSide) {
            be.tickServer();
        }
    }

    private void tickServer() {
        // Update temperature toward environment
        long envTemp = environmentTemperature();
        temperature=com.gregtech.gregtech.api.machine.crucible.MoldCastingRules.cool(temperature,envTemp);

        // Clear empty content
        if (contentAmount <= 0) {
            contentMaterial = null;
        }

        // Auto-pull from adjacent crucibles
        boolean auto = autoPullDirections != 0 && contentMaterial == null;
        if (auto) {
            for (Direction side : Direction.values()) {
                if ((autoPullDirections & directionToBit(side)) != 0) {
                    BlockEntity be = level.getBlockEntity(worldPosition.relative(side));
                    if (be instanceof ITileEntityCrucible crucible) {
                        if (crucible.fillMoldAtSide(this, side.getOpposite().ordinal(), getInputSideFor(side))) {
                            break;
                        }
                    }
                }
            }
        }

        // Check for meltdown
        if (contentMaterial != null && temperature > getMoldMaxTemperature()) {
            meltdown();
            return;
        }

        // Check for solidification (only if no mold is above — mold handles shaped output)
        boolean hasMoldAbove = level != null && level.getBlockEntity(worldPosition.above()) instanceof MoldBlockEntity;
        if (!hasMoldAbove && contentMaterial != null && temperature < contentMaterial.getMeltingPoint() && contentAmount > 0) {
            solidify();
        }

        // Proximity burn
        if (contentMaterial != null && temperature > 350) {
            SmelteryFireHelper.proximityBurn(level, worldPosition, temperature);
        }

        setChanged();
    }

    private void solidify() {
        if (contentMaterial == null || contentAmount <= 0) return;

        // Basin always produces OP.blockSolid (full block) — cools gradually
        GTMaterial solidMaterial = getSolidifyingMaterial(contentMaterial);
        ItemStack output = GTBlocks.getStack(BlockMaterialPrefix.blockSolid, solidMaterial);
        // Clear content BEFORE storeOutputItem sync so client receives consistent state
        contentMaterial = null;
        contentAmount = 0;
        if (!output.isEmpty()) {
            solidOutputTint = solidMaterial.getColor() | 0xFF000000;
            storeOutputItem(output);
        }
    }

    /** Store solidified output in container instead of auto-popping. */
    private void storeOutputItem(ItemStack stack) {
        if (solidOutput.isEmpty()) {
            solidOutput = stack;
        } else {
            spawnOutputItem(stack);
        }
        syncToClient();
    }

    /** Get the stored solid output (for hopper/automation extraction). */
    public ItemStack getSolidOutput() {
        return solidOutput;
    }

    /** Extract the stored solid output (returns the stack, clears internal storage). */
    public ItemStack extractSolidOutput() {
        ItemStack result = solidOutput;
        solidOutput = ItemStack.EMPTY;
        solidOutputTint = 0xFFFFFFFF;
        syncToClient();
        return result;
    }

    public int getSolidOutputTint() {
        return solidOutputTint;
    }

    private void spawnOutputItem(ItemStack stack) {
        if (level == null) return;
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 1.0; // Spawn above the basin
        double z = worldPosition.getZ() + 0.5;
        ItemEntity entity = new ItemEntity(level, x, y, z, stack);
        level.addFreshEntity(entity);
    }

    private void meltdown() {
        SmelteryBlockEntityHelper.meltdown(level, worldPosition);
        contentMaterial = null;
        contentAmount = 0;
    }

    private long environmentTemperature() {
        return SmelteryBlockEntityHelper.environmentTemperature(level, worldPosition);
    }

    private GTMaterial getSolidifyingMaterial(GTMaterial molten) {
        // In a full implementation, this would look up mTargetSolidifying
        return molten;
    }

    /**
     * Toggle auto-input for a specific horizontal direction.
     */
    public boolean toggleAutoInput(Direction side) {
        if (side != Direction.UP && !side.getAxis().isHorizontal()) return false;
        autoPullDirections ^= directionToBit(side);
        return true;
    }

    /**
     * Pick up output item with pincers after solidification.
     * First checks for solidified output, then molten content (if cooled enough).
     * @param usingPincers whether player is using pincers (protects from hot solid damage)
     */
    public boolean tryPickupWithPincers(Player player, boolean usingPincers) {
        if (level == null || level.isClientSide) return false;

        // Check for already-solidified output first
        if (!solidOutput.isEmpty()) {
            ItemStack output = extractSolidOutput();
            if (!player.getInventory().add(output)) {
                spawnOutputItem(output);
            }
            syncToClient();
            return true;
        }

        if (contentMaterial == null || contentAmount <= 0) return false;
        if (temperature >= contentMaterial.getMeltingPoint()) return false;

        GTMaterial solidMaterial = getSolidifyingMaterial(contentMaterial);
        ItemStack output = GTBlocks.getStack(BlockMaterialPrefix.blockSolid, solidMaterial);
        if (output.isEmpty()) return false;

        if (!usingPincers) {
            SmelteryBlockEntityHelper.applyHeatDamage(player, temperature);
        }
        contentMaterial = null;
        contentAmount = 0;
        temperature = environmentTemperature();

        if (!player.getInventory().add(output)) {
            spawnOutputItem(output);
        }
        syncToClient();
        return true;
    }

    /** Drop content as item when block is removed. */
    public void dropContent() {
        // Drop solidified output first
        if (!solidOutput.isEmpty()) {
            spawnOutputItem(solidOutput.copy());
            solidOutput = ItemStack.EMPTY;
        }
        if (level == null || contentMaterial == null || contentAmount <= 0) return;
        GTMaterial solidMaterial = getSolidifyingMaterial(contentMaterial);
        ItemStack output = GTBlocks.getStack(BlockMaterialPrefix.blockSolid, solidMaterial);
        if (!output.isEmpty()) {
            spawnOutputItem(output);
        }
        contentMaterial = null;
        contentAmount = 0;
    }

    /**
     * Right-click pour: trigger a pour from adjacent crucible/crossing into this basin.
     */
    public InteractionResult tryPour(Player player) {
        if (level == null || level.isClientSide) return InteractionResult.PASS;
        if (contentMaterial != null || !solidOutput.isEmpty()) return InteractionResult.PASS;

        for (Direction side : Direction.values()) {
            BlockEntity be = level.getBlockEntity(worldPosition.relative(side));
            if (be instanceof ITileEntityCrucible crucible) {
                if (crucible.fillMoldAtSide(this, side.getOpposite().ordinal(), getInputSideFor(side))) {
                    syncToClient();
                    return InteractionResult.CONSUME;
                }
            }
        }
        return InteractionResult.PASS;
    }

    /** Clear content after mold above consumes it for shaped output. */
    public void clearContent() {
        contentMaterial = null;
        contentAmount = 0;
        temperature = environmentTemperature();
        syncToClient();
    }

    // === ITileEntityMold implementation ===

    @Override
    public boolean isMoldInputSide(int side) {
        // Basin only accepts input from the top face
        return side == Direction.UP.ordinal();
    }

    @Override
    public long getMoldMaxTemperature() {
        return spec.meltDownTemperatureK();
    }

    @Override
    public long getMoldRequiredMaterialUnits() {
        // Basin produces a solid block — requires 9 ingots worth (1 full block)
        return GTValues.U * 9;
    }

    @Override
    public long fillMold(GTMaterial material, long amount, long temp, int side) {
        if (material == null) return 0;
        if (contentMaterial != null) return 0; // Already filled
        if (!solidOutput.isEmpty()) return 0; // Solid output not yet extracted
        if (!isMoldInputSide(side)) return 0;
        // GT6: acid melts reject non-acid-proof basins
        if (!spec().acidProof() && material.has(com.gregtech.gregtech.api.material.MaterialProperty.ACID)) return 0;

        long required = getMoldRequiredMaterialUnits();
        if (required <= 0) return 0;
        if (amount >= required) {
            contentMaterial = material;
            contentAmount = required;
            this.temperature = temp;
            syncToClient();
            return required;
        }
        return 0;
    }

    @Override
    public long getMoldContentAmount() {
        return contentAmount;
    }

    @Override
    public GTMaterial getMoldContentMaterial() {
        return contentMaterial;
    }

    private int getInputSideFor(Direction direction) {
        return direction.ordinal();
    }

    private static byte directionToBit(Direction d) {
        return SmelteryBlockEntityHelper.directionToBit(d);
    }

    // === NBT ===

    @Override
    protected void loadAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.loadAdditional(tag,lookup);
        temperature = tag.contains(NBT_TEMPERATURE) ? tag.getLong(NBT_TEMPERATURE) : GregTechConstants.DEF_ENV_TEMP;
        autoPullDirections = tag.contains(NBT_AUTO_PULL) ? tag.getByte(NBT_AUTO_PULL) : 0;
        if (tag.contains("gt.basin.has_content")) {
            if (tag.getBoolean("gt.basin.has_content") && tag.contains(NBT_CONTENT_MATERIAL)) {
                contentMaterial = GTMaterialRegistry.get(tag.getString(NBT_CONTENT_MATERIAL));
            } else {
                contentMaterial = null;
            }
        } else if (tag.contains(NBT_CONTENT_MATERIAL)) {
            contentMaterial = GTMaterialRegistry.get(tag.getString(NBT_CONTENT_MATERIAL));
        } else {
            contentMaterial = null;
        }
        contentAmount = tag.contains(NBT_CONTENT_AMOUNT) ? tag.getLong(NBT_CONTENT_AMOUNT) : 0;
        if (tag.contains("gt.basin.has_solid")) {
            if (tag.getBoolean("gt.basin.has_solid") && tag.contains(NBT_SOLID_OUTPUT)) {
                solidOutput = ItemStack.parseOptional(lookup,tag.getCompound(NBT_SOLID_OUTPUT));
            } else {
                solidOutput = ItemStack.EMPTY;
            }
        } else if (tag.contains(NBT_SOLID_OUTPUT)) {
            solidOutput = ItemStack.parseOptional(lookup,tag.getCompound(NBT_SOLID_OUTPUT));
        } else {
            solidOutput = ItemStack.EMPTY;
        }
        solidOutputTint = tag.contains(NBT_SOLID_OUTPUT_TINT) ? tag.getInt(NBT_SOLID_OUTPUT_TINT) : 0xFFFFFFFF;
    }

    @Override
    protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);
        tag.putLong(NBT_TEMPERATURE, temperature);
        tag.putByte(NBT_AUTO_PULL, autoPullDirections);
        if (contentMaterial != null) {
            tag.putString(NBT_CONTENT_MATERIAL, contentMaterial.getName());
        }
        tag.putLong(NBT_CONTENT_AMOUNT, contentAmount);
        if (!solidOutput.isEmpty()) {
            tag.put(NBT_SOLID_OUTPUT, solidOutput.saveOptional(lookup));
        }
        tag.putInt(NBT_SOLID_OUTPUT_TINT, solidOutputTint);
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup) {
        CompoundTag tag = super.getUpdateTag(lookup);
        tag.putLong(NBT_TEMPERATURE, temperature);
        tag.putByte(NBT_AUTO_PULL, autoPullDirections);
        tag.putBoolean("gt.basin.has_content", contentMaterial != null);
        if (contentMaterial != null) {
            tag.putString(NBT_CONTENT_MATERIAL, contentMaterial.getName());
        }
        tag.putLong(NBT_CONTENT_AMOUNT, contentAmount);
        // Always write solid output info so client can detect clears
        tag.putBoolean("gt.basin.has_solid", !solidOutput.isEmpty());
        if (!solidOutput.isEmpty()) {
            tag.put(NBT_SOLID_OUTPUT, solidOutput.saveOptional(lookup));
        }
        tag.putInt(NBT_SOLID_OUTPUT_TINT, solidOutputTint);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.handleUpdateTag(tag,lookup);
        if (tag.contains(NBT_TEMPERATURE)) temperature = tag.getLong(NBT_TEMPERATURE);
        if (tag.contains(NBT_AUTO_PULL)) autoPullDirections = tag.getByte(NBT_AUTO_PULL);
        if (tag.getBoolean("gt.basin.has_content")) {
            if (tag.contains(NBT_CONTENT_MATERIAL)) {
                contentMaterial = GTMaterialRegistry.get(tag.getString(NBT_CONTENT_MATERIAL));
            }
        } else {
            contentMaterial = null;
        }
        if (tag.contains(NBT_CONTENT_AMOUNT)) contentAmount = tag.getLong(NBT_CONTENT_AMOUNT);
        if (tag.getBoolean("gt.basin.has_solid")) {
            if (tag.contains(NBT_SOLID_OUTPUT)) {
                solidOutput = ItemStack.parseOptional(lookup,tag.getCompound(NBT_SOLID_OUTPUT));
            }
        } else {
            solidOutput = ItemStack.EMPTY;
        }
        solidOutputTint = tag.contains(NBT_SOLID_OUTPUT_TINT) ? tag.getInt(NBT_SOLID_OUTPUT_TINT) : 0xFFFFFFFF;
    }

    private void syncToClient() {
        SmelteryBlockEntityHelper.syncToClient(this);
    }

    // Accessors
    public long getTemperature() { return temperature; }
    public CrucibleSpec spec() { return spec; }
    public byte getAutoPullDirections() { return autoPullDirections; }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    @Override public void onDataPacket(net.minecraft.network.Connection connection,net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet,net.minecraft.core.HolderLookup.Provider lookup){if(packet.getTag()!=null)handleUpdateTag(packet.getTag(),lookup);}
}
