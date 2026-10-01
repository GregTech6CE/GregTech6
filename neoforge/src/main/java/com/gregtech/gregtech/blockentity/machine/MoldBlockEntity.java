package com.gregtech.gregtech.blockentity.machine;

import com.gregtech.gregtech.api.machine.CrucibleSpec;
import com.gregtech.gregtech.api.machine.crucible.MoldShapes;
import com.gregtech.gregtech.api.machine.ITileEntityCrucible;
import com.gregtech.gregtech.api.machine.ITileEntityMold;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.prefix.BlockMaterialPrefix;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.block.machine.MoldBlock;
import com.gregtech.gregtech.data.GregTechConstants;
import com.gregtech.gregtech.registry.GTBlockEntities;
import com.gregtech.gregtech.registry.GTBlocks;
import com.gregtech.gregtech.registry.GTItems;
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
 * Mold block entity (GT6 {@code MultiTileEntityMold}).
 * <p>
 * Supports: chisel shape selection (5x5 grid), auto-input from crucibles, redstone control,
 * soft hammer reset, and pincers item pickup.
 */
public class MoldBlockEntity extends BlockEntity implements ITileEntityMold {

    private final CrucibleSpec spec;

    /** 5x5 cavity bitmask — starts empty (all cells solid). Chisel sets bits to carve cavities. GT6: bit=1 means cavity. */
    private int moldShape = 0;

    /** Auto-pull directions bitmask (sides from which to auto-pull material). */
    private byte autoPullDirections = 0;

    /** Redstone control mode: requires redstone signal to auto-pull. */
    private boolean useRedstone = false;

    /** Current temperature (environment temperature when empty). */
    private long temperature = GregTechConstants.DEF_ENV_TEMP;

    /** Material currently in the mold. */
    @Nullable
    private GTMaterial contentMaterial = null;

    /** Amount of material in the mold (in units). */
    private long contentAmount = 0;

    /** Whether the content has solidified (stays visible at 2px height). */
    private boolean contentSolidified = false;

    /** Solidified output item for pickup (set when pickup happens, not on solidify). */
    private ItemStack solidOutput = ItemStack.EMPTY;

    private static final String NBT_SHAPE = "gt.mold.shape";
    private static final String NBT_AUTO_PULL = "gt.mold.auto_pull";
    private static final String NBT_USE_REDSTONE = "gt.mold.use_redstone";
    private static final String NBT_TEMPERATURE = "gt.temperature";
    private static final String NBT_CONTENT_MATERIAL = "gt.content.material";
    private static final String NBT_CONTENT_AMOUNT = "gt.content.amount";
    private static final String NBT_CONTENT_SOLIDIFIED = "gt.content.solidified";
    private static final String NBT_SOLID_OUTPUT = "gt.mold.solid_output";

    public MoldBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.spec = getSpecFromState(state);
    }

    public MoldBlockEntity(BlockPos pos, BlockState state) {
        this(GTBlockEntities.MOLD.get(), pos, state);
    }

    private static CrucibleSpec getSpecFromState(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof MoldBlock moldBlock) {
            return moldBlock.spec();
        }
        throw new IllegalStateException("MoldBlockEntity placed on non-mold block: " + block);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MoldBlockEntity be) {
        if (!level.isClientSide) {
            be.tickServer();
        }
    }

    private void tickServer() {
        // Update temperature toward environment
        long envTemp = environmentTemperature();
        temperature = com.gregtech.gregtech.api.machine.crucible.MoldCastingRules.cool(temperature, envTemp);

        // Clear empty content
        if (contentAmount <= 0) {
            if (contentMaterial != null) {
                contentMaterial = null;
            }
        }

        // Auto-pull from adjacent crucibles
        boolean auto = autoPullDirections != 0 && contentMaterial == null;
        if (auto) {
            boolean needsRedstone = useRedstone && !hasRedstoneIncoming();
            if (!needsRedstone) {
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
        }

        // Check for meltdown
        long effectiveTemp = temperature;
        GTMaterial effectiveMat = contentMaterial;
        long effectiveAmt = contentAmount;
        MoldBasinBlockEntity basin = getBasinBelow();
        if (basin != null && effectiveMat == null) {
            effectiveMat = basin.getMoldContentMaterial();
            effectiveAmt = basin.getMoldContentAmount();
            effectiveTemp = environmentTemperature();
        }
        if (effectiveMat != null && effectiveTemp > getMoldMaxTemperature()) {
            meltdown();
            if (basin != null && effectiveMat == basin.getMoldContentMaterial()) {
                basin.clearContent();
            }
            return;
        }

        // Check for solidification
        if (contentMaterial != null && temperature < contentMaterial.getMeltingPoint() && contentAmount > 0) {
            solidify();
        } else if (contentMaterial == null && basin != null) {
            GTMaterial basinMat = basin.getMoldContentMaterial();
            if (basinMat != null && basin.getMoldContentAmount() > 0
                    && environmentTemperature() < basinMat.getMeltingPoint()) {
                solidifyFromBasin(basin);
            }
        }

        // Proximity burn — damage nearby entities when content is hot
        long burnSourceTemp = contentMaterial != null ? temperature : 0;
        if (burnSourceTemp == 0 && basin != null) {
            GTMaterial basinMat = basin.getMoldContentMaterial();
            if (basinMat != null && basin.getMoldContentAmount() > 0) {
                burnSourceTemp = basin.getTemperature();
            }
        }
        if (burnSourceTemp > 350) {
            SmelteryFireHelper.proximityBurn(level, worldPosition, burnSourceTemp);
        }

        setChanged();
    }

    private void solidify() {
        if (contentMaterial == null || contentAmount <= 0) return;
        // Mark as solidified — content stays visible in mold at 2px height, cools gradually
        contentSolidified = true;
        syncToClient();
    }

    private void spawnOutputItem(ItemStack stack) {
        if (level == null) return;
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 0.5;
        double z = worldPosition.getZ() + 0.5;
        ItemEntity entity = new ItemEntity(level, x, y, z, stack);
        level.addFreshEntity(entity);
    }

    /** Store solidified output in container instead of auto-popping. */
    private void storeOutputItem(ItemStack stack) {
        if (solidOutput.isEmpty()) {
            solidOutput = stack;
        } else {
            // Fallback: if output slot is full, spawn as entity
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
        syncToClient();
        return result;
    }

    private void solidifyFromBasin(MoldBasinBlockEntity basin) {
        if (basin == null) return;
        GTMaterial basinMat = basin.getMoldContentMaterial();
        if (basinMat == null || basin.getMoldContentAmount() <= 0) return;

        // Produce shaped output from basin content + mold shape — cools gradually
        if (contentMaterial == null) {
            contentMaterial = basinMat;
            contentAmount = getMoldRequiredMaterialUnits();
            contentSolidified = true;
            temperature = basin.getTemperature();
            basin.clearContent();
            syncToClient();
        }
    }

    @Nullable
    private MoldBasinBlockEntity getBasinBelow() {
        if (level == null) return null;
        BlockEntity be = level.getBlockEntity(worldPosition.below());
        return be instanceof MoldBasinBlockEntity basin ? basin : null;
    }

    private void meltdown() {
        SmelteryBlockEntityHelper.meltdown(level, worldPosition);
        contentMaterial = null;
        contentAmount = 0;
    }

    private boolean hasRedstoneIncoming() {
        if (level == null) return false;
        for (Direction side : Direction.values()) {
            if (level.hasSignal(worldPosition, side)) {
                return true;
            }
        }
        return false;
    }

    private long environmentTemperature() {
        return SmelteryBlockEntityHelper.environmentTemperature(level, worldPosition);
    }

    @Nullable
    public Object getMoldRecipePrefix() {
        MoldShapes.Recipe recipe = MoldShapes.recipe(moldShape);
        return recipe == null ? null : recipe.blockSolid() ? BlockMaterialPrefix.blockSolid : recipe.itemPrefix();
    }

    private boolean isNuggetFallback() {
        return MoldShapes.isNuggetFallback(moldShape);
    }

    private GTMaterial getSolidifyingMaterial(GTMaterial molten) {
        // In a full implementation, this would look up mTargetSolidifying
        // For now, return the same material (it won't solidify until we implement that)
        return molten;
    }

    /**
     * GT6 chisel shape selection on the 5x5 top surface. Toggles cells on/off.
     */
    public boolean trySelectShape(Player player, InteractionHand hand, double hitX, double hitZ) {
        if (contentMaterial != null) return false;
        if (hitX < 2 / 16.0D || hitX > 14 / 16.0D) return false;
        if (hitZ < 2 / 16.0D || hitZ > 14 / 16.0D) return false;

        int gridX = (int) ((hitX - 2 / 16.0D) / (12.0 / 16.0 / 5.0));
        int gridZ = (int) ((hitZ - 2 / 16.0D) / (12.0 / 16.0 / 5.0));
        gridX = Math.max(0, Math.min(4, gridX));
        gridZ = Math.max(0, Math.min(4, gridZ));

        int bit = 1 << (gridZ * 5 + gridX);
        // Set cavity bit (GT6: bit=1 = carved away, bit=0 = solid)
        moldShape |= bit;
        if (level != null) {
            level.playSound(player, worldPosition, SoundEvents.METAL_BREAK, SoundSource.BLOCKS, 0.5F, 0.5F);
        }
        syncToClient();
        return true;
    }

    /**
     * Right-click pour: trigger a pour from adjacent crucible/crossing into this mold.
     * GT6 behavior: empty mold + adjacent crucible/crossing → right-click triggers fillMoldAtSide.
     */
    public InteractionResult tryPour(Player player) {
        if (level == null || level.isClientSide) return InteractionResult.PASS;
        if (contentMaterial != null || !solidOutput.isEmpty()) return InteractionResult.PASS;

        // Search adjacent blocks for crucible or crossing
        for (Direction side : Direction.values()) {
            BlockEntity be = level.getBlockEntity(worldPosition.relative(side));
            if (be instanceof ITileEntityCrucible crucible) {
                if (crucible.fillMoldAtSide(this, side.getOpposite().ordinal(), getInputSideFor(side))) {
                    syncToClient();
                    return InteractionResult.CONSUME;
                }
            }
        }
        // Also try direct pour from basin below (if basin has content from a connected source)
        MoldBasinBlockEntity basin = getBasinBelow();
        if (basin != null) {
            BlockEntity be = level.getBlockEntity(worldPosition.below());
            // The basin IS the mold below, no further action needed here
        }
        return InteractionResult.PASS;
    }

    /**
     * Toggle auto-input for a specific horizontal direction.
     */
    public boolean toggleAutoInput(Direction side) {
        if (!side.getAxis().isHorizontal()) return false;
        byte bit = directionToBit(side);
        autoPullDirections ^= bit;
        return true;
    }

    /**
     * Toggle redstone control mode (when clicking top face).
     */
    public void toggleRedstoneControl() {
        useRedstone = !useRedstone;
        if (!useRedstone) {
            // Soft hammer resets both redstone and auto-pull
            autoPullDirections = 0;
        }
    }

    /**
     * Reset all settings (soft hammer).
     */
    public void resetSettings() {
        useRedstone = false;
        autoPullDirections = 0;
    }

    /**
     * Pick up output item with pincers / empty hand.
     * First checks for solidified output, then molten content (if cooled enough).
     * When stacked on a basin, picks up from the basin using the mold's shape.
     * @param usingPincers whether player is using pincers (protects from hot solid damage)
     */
    public boolean tryPickupWithPincers(Player player, boolean usingPincers) {
        if (level == null || level.isClientSide) return false;

        // Check for solid output item first
        if (!solidOutput.isEmpty()) {
            ItemStack output = extractSolidOutput();
            if (!player.getInventory().add(output)) {
                spawnOutputItem(output);
            }
            syncToClient();
            return true;
        }

        // Pickup solidified content from mold or basin below
        MoldBasinBlockEntity basin = getBasinBelow();
        boolean pickedUp = false;

        if (contentMaterial != null && contentAmount > 0 && contentSolidified) {
            if (temperature >= contentMaterial.getMeltingPoint()) return false;
            GTMaterial solidMaterial = getSolidifyingMaterial(contentMaterial);
            Object prefix = getMoldRecipePrefix();
            if (prefix != null && solidMaterial != null) {
                ItemStack output;
                if (prefix instanceof BlockMaterialPrefix blockPrefix) {
                    output = GTBlocks.getStack(blockPrefix, solidMaterial);
                } else if (prefix instanceof MaterialPrefix materialPrefix) {
                    output = GTItems.getStack(materialPrefix, solidMaterial);
                    if (isNuggetFallback()) {
                        output.setCount(Integer.bitCount(moldShape));
                    }
                } else {
                    output = ItemStack.EMPTY;
                }
                if (!output.isEmpty()) {
                    // Heat damage: if solid is above skin-burn temperature and no pincers, damage player
                    if (!usingPincers) {
                        SmelteryBlockEntityHelper.applyHeatDamage(player, temperature);
                    }
                    if (!player.getInventory().add(output)) {
                        spawnOutputItem(output);
                    }
                    contentMaterial = null;
                    contentAmount = 0;
                    contentSolidified = false;
                    temperature = environmentTemperature();
                    pickedUp = true;
                }
            }
        } else if (contentMaterial == null && basin != null) {
            return tryPickupFromBasin(player, basin, usingPincers);
        }

        if (pickedUp) {
            syncToClient();
        }
        return pickedUp;
    }

    private boolean tryPickupFromBasin(Player player, MoldBasinBlockEntity basin, boolean usingPincers) {
        GTMaterial basinMat = basin.getMoldContentMaterial();
        if (basinMat == null || basin.getMoldContentAmount() <= 0) return false;
        if (environmentTemperature() >= basinMat.getMeltingPoint()) return false;

        GTMaterial solidMaterial = getSolidifyingMaterial(basinMat);
        Object prefix = getMoldRecipePrefix();
        if (prefix == null || solidMaterial == null) return false;

        ItemStack output;
        if (prefix instanceof BlockMaterialPrefix blockPrefix) {
            output = GTBlocks.getStack(blockPrefix, solidMaterial);
        } else if (prefix instanceof MaterialPrefix materialPrefix) {
            output = GTItems.getStack(materialPrefix, solidMaterial);
            if (isNuggetFallback()) {
                output.setCount(Integer.bitCount(moldShape));
            }
        } else {
            return false;
        }

        if (output.isEmpty()) return false;

        if (!usingPincers) {
            SmelteryBlockEntityHelper.applyHeatDamage(player, basin.getTemperature());
        }
        basin.clearContent();
        if (!player.getInventory().add(output)) {
            spawnOutputItem(output);
        }
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
        Object prefix = getMoldRecipePrefix();
        if (prefix == null || solidMaterial == null) return;
        ItemStack output;
        if (prefix instanceof BlockMaterialPrefix blockPrefix) {
            output = GTBlocks.getStack(blockPrefix, solidMaterial);
        } else if (prefix instanceof MaterialPrefix materialPrefix) {
            output = GTItems.getStack(materialPrefix, solidMaterial);
            if (isNuggetFallback()) {
                output.setCount(Integer.bitCount(moldShape));
            }
        } else {
            return;
        }
        if (!output.isEmpty()) {
            spawnOutputItem(output);
        }
        contentMaterial = null;
        contentAmount = 0;
    }

    // === ITileEntityMold implementation ===

    @Override
    public boolean isMoldInputSide(int side) {
        Direction d = Direction.from3DDataValue(side);
        return d != null && (d == Direction.UP || d.getAxis().isHorizontal());
    }

    @Override
    public long getMoldMaxTemperature() {
        return spec().meltDownTemperatureK();
    }

    @Override
    public long getMoldRequiredMaterialUnits() {
        return MoldShapes.requiredMaterialUnits(moldShape);
    }

    @Override
    public long fillMold(GTMaterial material, long amount, long temperature, int side) {
        if (material == null) return 0;
        if (!isMoldInputSide(side)) return 0;
        // GT6: acid melts reject non-acid-proof molds
        if (!spec().acidProof() && material.has(com.gregtech.gregtech.api.material.MaterialProperty.ACID)) return 0;

        // Mold-basin stacking: redirect fill to basin below if present
        MoldBasinBlockEntity basin = getBasinBelow();
        if (basin != null) {
            return basin.fillMold(material, amount, temperature, Direction.UP.ordinal());
        }

        if (contentMaterial != null) return 0; // Already filled
        if (!solidOutput.isEmpty()) return 0; // Solid output not yet extracted

        long required = getMoldRequiredMaterialUnits();
        long accepted = com.gregtech.gregtech.api.machine.crucible.MoldCastingRules.acceptedAmount(
                material, amount, required, spec().acidProof(), contentMaterial != null || !solidOutput.isEmpty());
        if (accepted > 0) {
            contentMaterial = material;
            contentAmount = accepted;
            this.temperature = temperature;
            syncToClient();
            return accepted;
        }
        return 0;
    }

    @Override
    public long getMoldContentAmount() {
        if (contentAmount > 0) return contentAmount;
        MoldBasinBlockEntity basin = getBasinBelow();
        return basin != null ? basin.getMoldContentAmount() : 0;
    }

    @Override
    public GTMaterial getMoldContentMaterial() {
        if (contentMaterial != null) return contentMaterial;
        MoldBasinBlockEntity basin = getBasinBelow();
        return basin != null ? basin.getMoldContentMaterial() : null;
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
        moldShape = tag.contains(NBT_SHAPE) ? tag.getInt(NBT_SHAPE) : 0;
        autoPullDirections = tag.contains(NBT_AUTO_PULL) ? tag.getByte(NBT_AUTO_PULL) : 0;
        useRedstone = tag.getBoolean(NBT_USE_REDSTONE);
        temperature = tag.contains(NBT_TEMPERATURE) ? tag.getLong(NBT_TEMPERATURE) : GregTechConstants.DEF_ENV_TEMP;
        if (tag.contains("gt.mold.has_content")) {
            if (tag.getBoolean("gt.mold.has_content") && tag.contains(NBT_CONTENT_MATERIAL)) {
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
        contentSolidified = tag.getBoolean(NBT_CONTENT_SOLIDIFIED);
        if (tag.contains("gt.mold.has_solid")) {
            if (tag.getBoolean("gt.mold.has_solid") && tag.contains(NBT_SOLID_OUTPUT)) {
                solidOutput = ItemStack.parseOptional(lookup,tag.getCompound(NBT_SOLID_OUTPUT));
            } else {
                solidOutput = ItemStack.EMPTY;
            }
        } else if (tag.contains(NBT_SOLID_OUTPUT)) {
            solidOutput = ItemStack.parseOptional(lookup,tag.getCompound(NBT_SOLID_OUTPUT));
        } else {
            solidOutput = ItemStack.EMPTY;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.saveAdditional(tag,lookup);
        tag.putInt(NBT_SHAPE, moldShape);
        tag.putByte(NBT_AUTO_PULL, autoPullDirections);
        tag.putBoolean(NBT_USE_REDSTONE, useRedstone);
        tag.putLong(NBT_TEMPERATURE, temperature);
        if (contentMaterial != null) {
            tag.putString(NBT_CONTENT_MATERIAL, contentMaterial.getName());
        }
        tag.putLong(NBT_CONTENT_AMOUNT, contentAmount);
        tag.putBoolean(NBT_CONTENT_SOLIDIFIED, contentSolidified);
        if (!solidOutput.isEmpty()) {
            tag.put(NBT_SOLID_OUTPUT, solidOutput.saveOptional(lookup));
        }
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup) {
        CompoundTag tag = super.getUpdateTag(lookup);
        tag.putInt(NBT_SHAPE, moldShape);
        tag.putBoolean(NBT_USE_REDSTONE, useRedstone);
        tag.putByte(NBT_AUTO_PULL, autoPullDirections);
        tag.putLong(NBT_TEMPERATURE, temperature);
        tag.putBoolean("gt.mold.has_content", contentMaterial != null);
        if (contentMaterial != null) {
            tag.putString(NBT_CONTENT_MATERIAL, contentMaterial.getName());
        }
        tag.putLong(NBT_CONTENT_AMOUNT, contentAmount);
        tag.putBoolean(NBT_CONTENT_SOLIDIFIED, contentSolidified);
        tag.putBoolean("gt.mold.has_solid", !solidOutput.isEmpty());
        if (!solidOutput.isEmpty()) {
            tag.put(NBT_SOLID_OUTPUT, solidOutput.saveOptional(lookup));
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup) {
        super.handleUpdateTag(tag,lookup);
        if (tag.contains(NBT_SHAPE)) moldShape = tag.getInt(NBT_SHAPE);
        if (tag.contains(NBT_AUTO_PULL)) autoPullDirections = tag.getByte(NBT_AUTO_PULL);
        if (tag.contains(NBT_USE_REDSTONE)) useRedstone = tag.getBoolean(NBT_USE_REDSTONE);
        if (tag.contains(NBT_TEMPERATURE)) temperature = tag.getLong(NBT_TEMPERATURE);
        if (tag.contains("gt.mold.has_content")) {
            if (tag.getBoolean("gt.mold.has_content") && tag.contains(NBT_CONTENT_MATERIAL)) {
                contentMaterial = GTMaterialRegistry.get(tag.getString(NBT_CONTENT_MATERIAL));
            } else {
                contentMaterial = null;
            }
        } else if (tag.contains(NBT_CONTENT_MATERIAL)) {
            contentMaterial = GTMaterialRegistry.get(tag.getString(NBT_CONTENT_MATERIAL));
        }
        if (tag.contains(NBT_CONTENT_AMOUNT)) contentAmount = tag.getLong(NBT_CONTENT_AMOUNT);
        if (tag.contains(NBT_CONTENT_SOLIDIFIED)) contentSolidified = tag.getBoolean(NBT_CONTENT_SOLIDIFIED);
        if (tag.contains("gt.mold.has_solid")) {
            if (tag.getBoolean("gt.mold.has_solid") && tag.contains(NBT_SOLID_OUTPUT)) {
                solidOutput = ItemStack.parseOptional(lookup,tag.getCompound(NBT_SOLID_OUTPUT));
            } else {
                solidOutput = ItemStack.EMPTY;
            }
        } else if (tag.contains(NBT_SOLID_OUTPUT)) {
            solidOutput = ItemStack.parseOptional(lookup,tag.getCompound(NBT_SOLID_OUTPUT));
        }
    }

    private void syncToClient() {
        SmelteryBlockEntityHelper.syncToClient(this);
    }

    /** GT6's workshop chooses each prepared mould from the registered casting recipes. */
    public static int randomDungeonShape(net.minecraft.util.RandomSource random) {
        int[] shapes = MoldShapes.recipes().keySet().stream().mapToInt(Integer::intValue).sorted().toArray();
        return shapes.length == 0 ? 0 : shapes[random.nextInt(shapes.length)];
    }

    /** Initialise a newly generated dungeon mould with an actual castable cavity. */
    public boolean setDungeonShape(int shape) {
        if (shape != 0 && !MoldShapes.recipes().containsKey(shape)) return false;
        if (contentAmount != 0 || !solidOutput.isEmpty()) return false;
        moldShape = shape;
        syncToClient();
        return true;
    }

    // Accessors for interaction handler
    public int getMoldShape() { return moldShape; }
    public boolean isContentSolidified() { return contentSolidified; }
    public boolean isUseRedstone() { return useRedstone; }
    public byte getAutoPullDirections() { return autoPullDirections; }
    public long getTemperature() { return temperature; }
    public CrucibleSpec spec() { return spec; }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    @Override public void onDataPacket(net.minecraft.network.Connection connection,net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket packet,net.minecraft.core.HolderLookup.Provider lookup){if(packet.getTag()!=null)handleUpdateTag(packet.getTag(),lookup);}
}
