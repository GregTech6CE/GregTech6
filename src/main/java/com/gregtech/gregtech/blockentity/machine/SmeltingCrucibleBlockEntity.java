package com.gregtech.gregtech.blockentity.machine;
import com.gregtech.gregtech.api.machine.crucible.CrucibleContentsNbt;

import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.api.machine.CrucibleSpec;
import com.gregtech.gregtech.api.machine.ITileEntityCrucible;
import com.gregtech.gregtech.api.machine.ITileEntityMold;
import com.gregtech.gregtech.api.machine.crucible.CrucibleItemInput;
import com.gregtech.gregtech.api.machine.crucible.CrucibleMaterialStack;
import com.gregtech.gregtech.api.machine.crucible.CrucibleMath;
import com.gregtech.gregtech.api.machine.crucible.CrucibleProcess;
import com.gregtech.gregtech.api.machine.crucible.ThermalState;
import com.gregtech.gregtech.api.machine.crucible.ThermalStep;
import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.api.material.MaterialProperty;
import com.gregtech.gregtech.block.machine.SmeltingCrucibleBlock;
import com.gregtech.gregtech.blockentity.GTEnergyBlockEntity;
import com.gregtech.gregtech.client.CrucibleModelData;
import com.gregtech.gregtech.client.MaterialTooltips;
import com.gregtech.gregtech.data.GregTechConstants;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.registry.GTBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.util.GTEntityHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Smelting crucible (GT6 {@code MultiTileEntitySmeltery}).
 * <p>
 * 16 material units of internal storage, one top cache slot, HU input on the bottom face.
 */
public class SmeltingCrucibleBlockEntity extends GTEnergyBlockEntity implements ITileEntityCrucible, com.gregtech.gregtech.api.fluid.MoltenMaterialStorage {
    public static final long MAX_AMOUNT = 16L * GTValues.U;
    public static final int CACHE_SLOT = 0;
    public static final int CACHE_SLOT_LIMIT = 64;

    private static final String NBT_MATERIALS = "gt.materials";
    private static final String NBT_TEMPERATURE = "gt.temperature";
    private static final String NBT_TEMPERATURE_OLD = "gt.temperature.old";
    private static final String NBT_ENERGY_BUFFER = "gt.energy.buffer";
    private static final String NBT_COOLDOWN = "gt.cooldown";
    private static final String NBT_MELTDOWN = "gt.meltdown";
    private static final String NBT_DISPLAY_HEIGHT = "gt.display.height";
    private static final String NBT_DISPLAY_MATERIAL = "gt.display.material";
    private static final String NBT_DISPLAY_MOLTEN = "gt.display.molten";
    /** GT6 {@code 255 / 0.875} — maps byte height to inner cavity fill. */
    private static final float HEIGHT_SCALE = 292.571428F;

    private final CrucibleSpec spec;
    private final ItemStackHandler cache = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return CACHE_SLOT_LIMIT;
        }

        @Override
        protected void onContentsChanged(int slot) {
            SmeltingCrucibleBlockEntity.this.setChanged();
        }
    };

    private final List<CrucibleMaterialStack> content = new ArrayList<>();
    private long energyBuffer;
    private long temperature = GregTechConstants.DEF_ENV_TEMP;
    private long previousTemperature = GregTechConstants.DEF_ENV_TEMP;
    private int cooldown = 100;
    private boolean meltDownWarning;
    private byte displayedHeight;
    private int displayedMaterialId = -1;
    private boolean displayedMolten;

    private byte previousDisplayedHeight;
    private int previousDisplayedMaterialId = -1;
    private boolean previousDisplayedMolten;

    public SmeltingCrucibleBlockEntity(BlockPos pos, BlockState state, CrucibleSpec spec) {
        this(GTBlockEntities.SMELTING_CRUCIBLE.get(), pos, state, spec);
    }

    /** Shared GT6 crucible material engine; large variants supply their own BE type and hull. */
    protected SmeltingCrucibleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, CrucibleSpec spec) {
        super(type, pos, state);
        this.spec = spec;
    }

    public SmeltingCrucibleBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, ((SmeltingCrucibleBlock) state.getBlock()).spec());
    }

    public CrucibleSpec spec() {
        return spec;
    }

    public long getTemperature() {
        return temperature;
    }


    @Override public long getMoltenCapacityUnits() { return maxMaterialAmount(); }
    @Override public int fillMoltenMaterial(com.gregtech.gregtech.api.material.GTMaterial material, int requested, long incomingTemperature, boolean execute) {
        if (level == null || level.isClientSide) return 0;
        int accepted = com.gregtech.gregtech.api.fluid.MoltenFluidPlans.fillAmount(content, maxMaterialAmount(), material, requested, incomingTemperature);
        if (accepted <= 0) return 0;
        var destination = execute ? content : new ArrayList<>(content.stream().map(CrucibleMaterialStack::copy).toList());
        Long mixed = CrucibleProcess.admit(destination, List.of(CrucibleMaterialStack.of(material, com.gregtech.gregtech.api.fluid.MoltenFluidPlans.toUnits(accepted))), maxMaterialAmount(), thermalMassKg(), getTemperature(), incomingTemperature);
        if (mixed == null) return 0;
        if (execute) { temperature = mixed; updateDisplayState(); syncCrucibleState(); setChanged(); }
        return accepted;
    }
    @Override public int drainMoltenMaterial(com.gregtech.gregtech.api.material.GTMaterial material, int requested, boolean execute) {
        if (level == null || level.isClientSide) return 0;
        int amount = com.gregtech.gregtech.api.fluid.MoltenFluidPlans.drainAmount(content, material, requested, getTemperature());
        if (execute && amount > 0) { com.gregtech.gregtech.api.fluid.MoltenFluidPlans.remove(content, material, amount); updateDisplayState(); syncCrucibleState(); setChanged(); }
        return amount;
    }
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.fluids.capability.IFluidHandler> moltenFluidCapability = net.minecraftforge.common.util.LazyOptional.of(() -> new com.gregtech.gregtech.api.fluid.MoltenFluidHandler(this));
    @Override public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, Direction side) {
        if (capability == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER && side != Direction.DOWN) return moltenFluidCapability.cast();
        return super.getCapability(capability, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); moltenFluidCapability.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); moltenFluidCapability = net.minecraftforge.common.util.LazyOptional.of(() -> new com.gregtech.gregtech.api.fluid.MoltenFluidHandler(this)); }

    public long getEnergyBuffer() {
        return energyBuffer;
    }

    public List<CrucibleMaterialStack> getContentView() {
        return List.copyOf(content);
    }

    public byte getDisplayedHeight() {
        return displayedHeight;
    }

    public int getDisplayedMaterialId() {
        return displayedMaterialId;
    }

    public boolean isDisplayedMolten() {
        return displayedMolten;
    }

    @Nullable
    public GTMaterial getDisplayedMaterial() {
        return displayedMaterialId > 0 ? GTMaterialRegistry.get(displayedMaterialId) : null;
    }

    public boolean isMeltDownWarning() {
        return meltDownWarning;
    }

    /** GT6 meltdown warning hull tint ({@code mMeltDown} render pass). */
    public int getWarningHullTintRgb() {
        return 0xFFFFAA00;
    }

    public long getMeltDownLimitK() {
        return spec.meltDownTemperatureK();
    }

    protected long maxMaterialAmount() { return MAX_AMOUNT; }

    protected double thermalMassKg() { return spec.thermalMassKg(); }

    protected AABB itemSuctionArea() {
        return new AABB(worldPosition.getX() + 2 / 16.0D, worldPosition.getY() + 2 / 16.0D,
                worldPosition.getZ() + 2 / 16.0D, worldPosition.getX() + 14 / 16.0D,
                worldPosition.getY() + 1.0D, worldPosition.getZ() + 14 / 16.0D);
    }

    /** The large crucible's upper wall exposes insertion but never lets automation take its cache. */
    protected ItemStackHandler cacheHandler() { return cache; }

    protected void addHeatEnergy(long units) {
        if (units <= 0) return;
        energyBuffer = Math.min(Long.MAX_VALUE - units, energyBuffer) + units;
        setChanged();
    }

    protected void removeHeatEnergy(long units) {
        if (units <= 0) return;
        energyBuffer = Math.max(0, energyBuffer - units);
        setChanged();
    }

    public int getDisplayFillPercent() {
        return (displayedHeight & 0xFF) * 100 / 255;
    }

    /** GT6 {@code setBlockBounds2} pass 5 top Y in block space. */
    public float getContentFillMaxY() {
        int height = displayedHeight & 0xFF;
        return (2 / 16.0F) + height / HEIGHT_SCALE;
    }

    public ItemStack getCacheStack() {
        return cache.getStackInSlot(CACHE_SLOT);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SmeltingCrucibleBlockEntity be) {
        if (!level.isClientSide) {
            be.tickServer();
        }
    }

    protected void tickServer() {
        long environmentTemperature = environmentTemperature();
        int contentHash = content.hashCode();
        boolean anyVaporized = false;

        suckItemIntoCache();
        processCacheSlot(environmentTemperature);
        boolean newContent = contentHash != content.hashCode();
        CrucibleProcess.PhaseResult phases = CrucibleProcess.process(content, temperature,
                previousTemperature, newContent, spec.acidProof());
        for (int i = 0; i < phases.vaporizedStacks(); i++) playFizz();
        anyVaporized = phases.vaporizedStacks() > 0;
        if (phases.acidDestroyedHull()) {
            if (level != null) level.removeBlock(worldPosition, false);
            return;
        }

        double thermalMass = thermalMassKg() + CrucibleMaterialStack.weight(content);

        ThermalState stepped = ThermalStep.advance(
                new ThermalState(temperature, previousTemperature, energyBuffer, cooldown),
                environmentTemperature, thermalMass);
        previousTemperature = stepped.previousTemperatureK();
        updateDisplayState();
        energyBuffer = stepped.energyHU();
        temperature = stepped.temperatureK();
        cooldown = stepped.cooldownTicks();

        if (anyVaporized) {
            SmelteryFireHelper.tryIgniteNearby(level, worldPosition, temperature);
        }

        if (temperature > getMeltDownLimitK()) {
            content.clear();
            SmelteryBlockEntityHelper.meltdown(level, worldPosition);
            return;
        }

        boolean warning = temperature + 100 > getMeltDownLimitK();
        if (meltDownWarning != warning) {
            meltDownWarning = warning;
            syncCrucibleState();
        } else if (displayedHeight != previousDisplayedHeight
                || displayedMaterialId != previousDisplayedMaterialId
                || displayedMolten != previousDisplayedMolten) {
            syncCrucibleState();
        }
        previousDisplayedHeight = displayedHeight;
        previousDisplayedMaterialId = displayedMaterialId;
        previousDisplayedMolten = displayedMolten;
        setChanged();
    }

    /**
     * GT6 large crucibles keep cooling when their wall is incomplete. The vessel must not
     * melt inputs or spend buffered heat until the structure is restored.
     */
    protected void coolWithoutStructure() {
        if (level == null || level.isClientSide) return;
        long environment = environmentTemperature();
        long before = temperature;
        if (level.getGameTime() % 10 == 0) {
            if (temperature > environment) temperature--;
            else if (temperature < environment) temperature++;
        }
        temperature = Math.max(temperature, Math.min(200, environment));
        if (temperature != before) {
            updateDisplayState();
            meltDownWarning = temperature + 100 > getMeltDownLimitK();
            setChanged();
            syncCrucibleState();
        }
    }

    /** GT6 {@code WD.suck} — absorb one dropped stack into the cache slot when empty. */
    private void suckItemIntoCache() {
        if (level == null || !cache.getStackInSlot(CACHE_SLOT).isEmpty()) {
            return;
        }
        AABB area = itemSuctionArea();
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, area)) {
            if (entity.isRemoved()) {
                continue;
            }
            ItemStack stack = entity.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            cache.setStackInSlot(CACHE_SLOT, stack.copy());
            entity.discard();
            setChanged();
            return;
        }
    }

    private void processCacheSlot(long environmentTemperature) {
        ItemStack stack = cache.getStackInSlot(CACHE_SLOT);
        if (stack.isEmpty()) {
            return;
        }
        List<CrucibleMaterialStack> incoming = CrucibleItemInput.parse(stack);
        if (incoming.isEmpty()) {
            return;
        }
        if (addMaterialStacks(incoming, environmentTemperature)) {
            stack.shrink(1);
            if (stack.isEmpty()) {
                cache.setStackInSlot(CACHE_SLOT, ItemStack.EMPTY);
            }
        }
    }

    public boolean addMaterialStacks(List<CrucibleMaterialStack> incoming, long incomingTemperature) {
        Long mixedTemperature = CrucibleProcess.admit(content, incoming, maxMaterialAmount(),
                thermalMassKg(), temperature, incomingTemperature);
        if (mixedTemperature == null) return false;
        temperature = mixedTemperature;
        setChanged();
        return true;
    }

    /** Client-side hint for {@link SmeltingCrucibleBlock#use} swing prediction. */
    public boolean canHandleUse(Player player, InteractionHand hand) {
        return wouldHandleUse(player, hand);
    }

    /** Server-only interaction entry (via {@link com.gregtech.gregtech.event.CrucibleInteractionHandler}). */
    public InteractionResult tryUse(Player player, InteractionHand hand, BlockHitResult hit) {
        if (level == null || level.isClientSide || !hit.getDirection().equals(Direction.UP)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        if (GTToolHelper.isCrucibleScrapeShovel(held)) {
            return scrapeSolidContent(player, hand, held);
        }

        if (!canHandScrape(player, hand)) {
            return InteractionResult.PASS;
        }

        if (held.isEmpty()) {
            ItemStack cached = cache.getStackInSlot(CACHE_SLOT);
            if (!cached.isEmpty()) {
                giveItemPreferMainHand(player, cached.copy());
                cache.setStackInSlot(CACHE_SLOT, ItemStack.EMPTY);
                GTEntityHelper.applyTemperatureDamage(player, temperature, 1.0F, 5.0F);
                commitContentChange();
                player.swing(hand, true);
                return InteractionResult.CONSUME;
            }
            return handScrapeSolidContent(player, hand);
        }
        return handScrapeSolidContent(player, hand);
    }

    /**
     * GT6 {@code onBlockActivated3}: both hands must be empty or hold matching {@code scrapGt}.
     * Prevents off-hand empty clicks while the main hand carries unrelated items.
     */
    private boolean canHandScrape(Player player, InteractionHand hand) {
        if (!isHandAllowedForScrape(player.getItemInHand(hand))) {
            return false;
        }
        InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        return isHandAllowedForScrape(player.getItemInHand(other));
    }

    private boolean isHandAllowedForScrape(ItemStack held) {
        return held.isEmpty() || isMatchingSolidScrap(held);
    }

    /** GT6 {@code onBlockActivated3}: empty hand or matching {@code scrapGt} in the clicking hand. */
    private boolean isMatchingSolidScrap(ItemStack held) {
        if (held.isEmpty()) {
            return false;
        }
        CrucibleMaterialStack lightest = findLightestSolidStack();
        if (lightest == null) {
            return false;
        }
        ItemStack expected = GTItems.getStack(MaterialPrefix.scrapGt, lightest.material, 1);
        return !expected.isEmpty()
                && ItemStack.isSameItemSameTags(held, expected)
                && held.getCount() < held.getMaxStackSize();
    }

    private boolean wouldHandleUse(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (GTToolHelper.isCrucibleScrapeShovel(held)) {
            return findLightestSolidStack() != null;
        }
        if (!canHandScrape(player, hand)) {
            return false;
        }
        if (!held.isEmpty()) {
            return isMatchingSolidScrap(held);
        }
        if (!cache.getStackInSlot(CACHE_SLOT).isEmpty()) {
            return true;
        }
        return findLightestSolidStack() != null;
    }

    @Nullable
    private CrucibleMaterialStack findLightestSolidStack() {
        CrucibleMaterialStack.consolidate(content);
        CrucibleMaterialStack lightest = null;
        for (CrucibleMaterialStack stack : content) {
            if (stack == null || stack.material == Materials.Invalid || stack.amount <= 0) {
                continue;
            }
            if (temperature >= stack.material.getMeltingPoint()) {
                continue;
            }
            if (lightest == null || stack.material.getDensity() < lightest.material.getDensity()) {
                lightest = stack;
            }
        }
        return lightest;
    }

    /** GT6 {@code onBlockActivated3} — empty hand, one {@code scrapGt} (U9), temperature damage. */
    private InteractionResult handScrapeSolidContent(Player player, InteractionHand hand) {
        if (!extractOneSolidScrap(player, hand, true)) {
            return InteractionResult.PASS;
        }
        commitContentChange();
        player.swing(hand, true);
        return InteractionResult.CONSUME;
    }

    /** GT6 {@code onToolClick2} — same extraction as hand scrape, bulk stacks, then tool cost. */
    private InteractionResult scrapeSolidContent(Player player, InteractionHand hand, ItemStack tool) {
        CrucibleMaterialStack lightest = findLightestSolidStack();
        if (lightest == null) {
            return InteractionResult.PASS;
        }
        long scrapUnit = MaterialPrefix.scrapGt.getMaterialWeight();
        int maxStacks = lightest.amount < scrapUnit
                ? 1
                : (int) Math.max(1L, Math.min(64L, lightest.amount / scrapUnit));
        int extracted = 0;
        for (int i = 0; i < maxStacks; i++) {
            if (!extractOneSolidScrap(player, hand, false)) {
                break;
            }
            extracted++;
        }
        if (extracted <= 0) {
            return InteractionResult.PASS;
        }
        ItemStack liveTool = player.getItemInHand(hand);
        if (!liveTool.isEmpty() && GTToolHelper.isCrucibleScrapeShovel(liveTool)) {
            GTToolHelper.damageCrucibleScrape(liveTool, extracted, player, hand);
        }
        commitContentChange();
        player.swing(hand, true);
        return InteractionResult.CONSUME;
    }

    /**
     * Remove one {@code scrapGt} unit from solid content and deliver it to the player.
     *
     * @return {@code true} if one scrap was taken (content changed or cleared)
     */
    private boolean extractOneSolidScrap(Player player, InteractionHand hand, boolean temperatureDamage) {
        CrucibleMaterialStack lightest = findLightestSolidStack();
        if (lightest == null) {
            return false;
        }

        long scrapUnit = MaterialPrefix.scrapGt.getMaterialWeight();
        if (lightest.amount < scrapUnit) {
            lightest.amount = 0;
            content.removeIf(stack -> stack == null || stack.amount <= 0);
            CrucibleMaterialStack.consolidate(content);
            if (temperatureDamage) {
                GTEntityHelper.applyTemperatureDamage(player, temperature, 1.0F, 5.0F);
            }
            return true;
        }

        ItemStack output = GTItems.getStack(MaterialPrefix.scrapGt, lightest.material, 1);
        if (output.isEmpty()) {
            lightest.amount = 0;
            content.removeIf(stack -> stack == null || stack.amount <= 0);
            CrucibleMaterialStack.consolidate(content);
            if (temperatureDamage) {
                GTEntityHelper.applyTemperatureDamage(player, temperature, 1.0F, 5.0F);
            }
            return true;
        }

        giveScrapToPlayer(player, hand, output);
        lightest.amount -= scrapUnit;
        content.removeIf(stack -> stack == null || stack.amount <= 0);
        CrucibleMaterialStack.consolidate(content);
        if (temperatureDamage) {
            GTEntityHelper.applyTemperatureDamage(player, temperature, 1.0F, 5.0F);
        }
        return true;
    }

    /**
     * GT6 {@code onBlockActivated3} scrap delivery — stack on the clicking hand when it already
     * holds matching scrap; empty-hand clicks never fill the offhand.
     */
    private static void giveScrapToPlayer(Player player, InteractionHand hand, ItemStack scrap) {
        ItemStack clicked = player.getItemInHand(hand);
        if (!clicked.isEmpty()) {
            if (ItemStack.isSameItemSameTags(clicked, scrap) && clicked.getCount() < clicked.getMaxStackSize()) {
                clicked.grow(1);
                return;
            }
            if (!addToInventoryAvoidOffhand(player, scrap)) {
                player.drop(scrap, false);
            }
            return;
        }
        if (hand == InteractionHand.MAIN_HAND) {
            player.setItemInHand(InteractionHand.MAIN_HAND, scrap);
            return;
        }
        ItemStack main = player.getMainHandItem();
        if (main.isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, scrap);
            return;
        }
        if (ItemStack.isSameItemSameTags(main, scrap) && main.getCount() < main.getMaxStackSize()) {
            main.grow(1);
            return;
        }
        if (!addToInventoryAvoidOffhand(player, scrap)) {
            player.drop(scrap, false);
        }
    }

    private static boolean addToInventoryAvoidOffhand(Player player, ItemStack stack) {
        return player.getInventory().add(stack);
    }

    private static void giveItemPreferMainHand(Player player, ItemStack stack) {
        ItemStack main = player.getMainHandItem();
        if (main.isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            return;
        }
        if (ItemStack.isSameItemSameTags(main, stack)
                && main.getCount() + stack.getCount() <= main.getMaxStackSize()) {
            main.grow(stack.getCount());
            player.setItemInHand(InteractionHand.MAIN_HAND, main);
            return;
        }
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    /** Persist content/display changes and push them to watching clients. */
    protected void commitContentChange() {
        updateDisplayState();
        setChanged();
        syncToClient();
    }

    /** Recompute client-facing fill height / material after content changes. */
    private void updateDisplayState() {
        CrucibleMaterialStack lightest = null;
        CrucibleMaterialStack lightestSolid = null;
        long totalAmount = 0;
        for (CrucibleMaterialStack stack : content) {
            if (stack == null || stack.material == Materials.Invalid || stack.material == Materials.Air || stack.amount <= 0) {
                continue;
            }
            if (lightest == null || stack.material.getDensity() < lightest.material.getDensity()) {
                lightest = stack;
            }
            if (temperature < stack.material.getMeltingPoint()) {
                if (lightestSolid == null || stack.material.getDensity() < lightestSolid.material.getDensity()) {
                    lightestSolid = stack;
                }
            }
            totalAmount += stack.amount;
        }
        displayedHeight = (byte) CrucibleMath.scale(totalAmount, maxMaterialAmount(), 255, false);
        if (totalAmount <= 0) {
            displayedMaterialId = -1;
            displayedMolten = false;
        } else if (lightestSolid != null) {
            displayedMaterialId = lightestSolid.material.getId();
            displayedMolten = false;
        } else if (lightest != null && temperature >= lightest.material.getMeltingPoint()) {
            displayedMaterialId = lightest.material.getId();
            displayedMolten = true;
        } else {
            displayedMaterialId = -1;
            displayedMolten = false;
        }
    }

    /** Drops cache-slot items when the block is broken (GT6 inventory slot 0). */
    public void dropContents() {
        if (level == null || level.isClientSide) {
            return;
        }
        ItemStack cached = cache.getStackInSlot(CACHE_SLOT);
        if (!cached.isEmpty()) {
            com.gregtech.gregtech.util.GTItemDrops.dropItemStack(level,
                    worldPosition.getX() + 0.5D,
                    worldPosition.getY() + 0.5D,
                    worldPosition.getZ() + 0.5D,
                    cached);
            cache.setStackInSlot(CACHE_SLOT, ItemStack.EMPTY);
        }
    }

    protected long materialAmount(GTMaterial material) {
        return content.stream().filter(stack -> stack.material.resolve() == material.resolve())
                .mapToLong(stack -> stack.amount).sum();
    }

    protected boolean takeMaterial(GTMaterial material, long amount) {
        if (amount <= 0 || materialAmount(material) < amount) return false;
        long remaining = amount;
        for (CrucibleMaterialStack stack : content) {
            if (stack.material.resolve() != material.resolve()) continue;
            long taken = Math.min(remaining, stack.amount);
            stack.amount -= taken;
            remaining -= taken;
            if (remaining == 0) break;
        }
        content.removeIf(stack -> stack.amount <= 0);
        commitContentChange();
        return true;
    }

    /** GT6 large crucible KU input provides air for steelmaking while consuming only the reacted air. */
    protected void reactWithAir(long amount) {
        if (amount <= 0) return;
        CrucibleMaterialStack.of(Materials.Air, amount).addToList(content);
        com.gregtech.gregtech.api.machine.crucible.CrucibleReactions.react(content, temperature);
        content.removeIf(stack -> stack.material.resolve() == Materials.Air.resolve() || stack.amount <= 0);
        commitContentChange();
    }

    private long environmentTemperature() {
        return SmelteryBlockEntityHelper.environmentTemperature(level, worldPosition);
    }

    private void playFizz() {
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F);
        }
    }

    private void syncCrucibleState() {
        SmelteryBlockEntityHelper.syncToClient(this);
        previousDisplayedHeight = displayedHeight;
        previousDisplayedMaterialId = displayedMaterialId;
        previousDisplayedMolten = displayedMolten;
        refreshHullRender();
    }

    /** Force hull tint / model-data to refresh without waiting for neighbor block updates. */
    private void refreshHullRender() {
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        if (!level.isClientSide) {
            level.sendBlockUpdated(worldPosition, state, state, 11);
            requestModelDataUpdate();
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.getChunkSource().blockChanged(worldPosition);
            }
        } else {
            requestModelDataUpdate();
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.gregtech.gregtech.client.CrucibleClientSync.refreshHull(worldPosition));
        }
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(CrucibleModelData.MELTDOWN, meltDownWarning).build();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && temperature == 0) {
            temperature = environmentTemperature();
            previousTemperature = temperature;
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energyBuffer = tag.getLong(NBT_ENERGY_BUFFER);
        temperature = tag.contains(NBT_TEMPERATURE) ? tag.getLong(NBT_TEMPERATURE) : GregTechConstants.DEF_ENV_TEMP;
        previousTemperature = tag.contains(NBT_TEMPERATURE_OLD) ? tag.getLong(NBT_TEMPERATURE_OLD) : temperature;
        cooldown = tag.contains(NBT_COOLDOWN) ? tag.getInt(NBT_COOLDOWN) : 100;
        meltDownWarning = tag.getBoolean(NBT_MELTDOWN);
        displayedHeight = tag.getByte(NBT_DISPLAY_HEIGHT);
        displayedMaterialId = tag.contains(NBT_DISPLAY_MATERIAL) ? tag.getInt(NBT_DISPLAY_MATERIAL) : -1;
        displayedMolten = tag.getBoolean(NBT_DISPLAY_MOLTEN);
        content.clear();
        content.addAll(CrucibleContentsNbt.loadList(NBT_MATERIALS, tag));
        if (tag.contains("gt.cache")) {
            cache.deserializeNBT(tag.getCompound("gt.cache"));
        }
        meltDownWarning = temperature + 100 > getMeltDownLimitK();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Meta", spec.gt6MetaId());
        tag.putLong(NBT_ENERGY_BUFFER, energyBuffer);
        tag.putLong(NBT_TEMPERATURE, temperature);
        tag.putLong(NBT_TEMPERATURE_OLD, previousTemperature);
        tag.putInt(NBT_COOLDOWN, cooldown);
        tag.putBoolean(NBT_MELTDOWN, meltDownWarning);
        tag.putByte(NBT_DISPLAY_HEIGHT, displayedHeight);
        tag.putInt(NBT_DISPLAY_MATERIAL, displayedMaterialId);
        tag.putBoolean(NBT_DISPLAY_MOLTEN, displayedMolten);
        CrucibleContentsNbt.saveList(NBT_MATERIALS, tag, content);
        tag.put("gt.cache", cache.serializeNBT());
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putLong(NBT_TEMPERATURE, temperature);
        tag.putByte(NBT_DISPLAY_HEIGHT, displayedHeight);
        tag.putInt(NBT_DISPLAY_MATERIAL, displayedMaterialId);
        tag.putBoolean(NBT_DISPLAY_MOLTEN, displayedMolten);
        tag.putBoolean(NBT_MELTDOWN, meltDownWarning);
        CrucibleContentsNbt.saveList(NBT_MATERIALS, tag, content);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag);
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        if (tag.contains(NBT_TEMPERATURE)) {
            temperature = tag.getLong(NBT_TEMPERATURE);
        }
        if (tag.contains(NBT_DISPLAY_HEIGHT)) {
            displayedHeight = tag.getByte(NBT_DISPLAY_HEIGHT);
        }
        if (tag.contains(NBT_DISPLAY_MATERIAL)) {
            displayedMaterialId = tag.getInt(NBT_DISPLAY_MATERIAL);
        }
        if (tag.contains(NBT_DISPLAY_MOLTEN)) {
            displayedMolten = tag.getBoolean(NBT_DISPLAY_MOLTEN);
        }
        if (tag.contains(NBT_MELTDOWN)) {
            meltDownWarning = tag.getBoolean(NBT_MELTDOWN);
        }
        if (tag.contains(NBT_MATERIALS)) {
            content.clear();
            content.addAll(CrucibleContentsNbt.loadList(NBT_MATERIALS, tag));
            CrucibleMaterialStack.consolidate(content);
        }
        refreshHullRender();
    }

    @Override
    public boolean isEnergyType(GregTechTags.Tag energyType, @Nullable Direction side, boolean emitting) {
        return !emitting && energyType == GregTechTags.Energy.HU;
    }

    @Override
    public Collection<GregTechTags.Tag> getEnergyTypes(@Nullable Direction side) {
        return GregTechTags.Energy.HU.asList();
    }

    @Override
    public boolean hasEnergySurface(@Nullable Direction side) {
        return side == Direction.DOWN;
    }

    @Override
    public long getEnergySizeInputMin(GregTechTags.Tag energyType, @Nullable Direction side) {
        return energyType == GregTechTags.Energy.HU ? CrucibleSpec.MIN_HU_PER_TICK : 0;
    }

    @Override
    public long getEnergySizeInputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) {
        return energyType == GregTechTags.Energy.HU ? 2048 : 0;
    }

    @Override
    public long getEnergySizeInputMax(GregTechTags.Tag energyType, @Nullable Direction side) {
        return energyType == GregTechTags.Energy.HU ? Long.MAX_VALUE : 0;
    }

    @Override
    public long getEnergySizeOutputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) {
        return 0;
    }

    @Override
    public long getEnergyOffered(GregTechTags.Tag energyType, @Nullable Direction side, long size) {
        return 0;
    }

    @Override
    public long getEnergyDemanded(GregTechTags.Tag energyType, @Nullable Direction side, long size) {
        if (energyType != GregTechTags.Energy.HU || side != Direction.DOWN) {
            return 0;
        }
        return Long.MAX_VALUE - energyBuffer;
    }

    @Override
    public long doInject(GregTechTags.Tag energyType, @Nullable Direction side, long size, long amount, boolean doInject) {
        if (energyType != GregTechTags.Energy.HU || side != Direction.DOWN || amount <= 0) {
            return 0;
        }
        ThermalState received;
        try {
            received = ThermalStep.receive(
                    new ThermalState(temperature, previousTemperature, energyBuffer, cooldown),
                    ThermalStep.EnergyKind.HEAT, size, amount);
        } catch (IllegalArgumentException | ArithmeticException invalidPacket) {
            // Reject the entire packet count; simulated and actual acceptance must agree.
            return 0;
        }
        if (doInject) {
            energyBuffer = received.energyHU();
            setChanged();
        }
        return amount;
    }

    // === ITileEntityCrucible ===

    @Override
    public boolean fillMoldAtSide(ITileEntityMold mold, int crucibleSide, int moldSide) {
        if (mold == null) return false;
        boolean transferred = false;
        for (CrucibleMaterialStack stack : content) {
            if (stack == null || stack.material == Materials.Invalid || stack.amount <= 0) continue;
            if (temperature < stack.material.getMeltingPoint()) continue;
            long consumed = mold.fillMold(stack.material, stack.amount, temperature, moldSide);
            if (consumed > 0) {
                stack.amount -= consumed;
                transferred = true;
            }
        }
        content.removeIf(s -> s == null || s.amount <= 0);
        if (transferred) {
            CrucibleMaterialStack.consolidate(content);
            setChanged();
        }
        return transferred;
    }

    @Override
    public long getCrucibleTemperature() {
        return temperature;
    }

    @Override
    public long getCrucibleContentAmount() {
        return CrucibleMaterialStack.total(content);
    }

    public Component formatTemperatureLine() {
        String suffix = temperature >= 1300 ? "K (too hot to pick it up right now!)" : "K";
        return Component.literal("Temperature: " + temperature + suffix);
    }

    public String formatContentSummary() {
        if (content.isEmpty()) {
            return "empty";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < content.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            CrucibleMaterialStack stack = content.get(i);
            builder.append(MaterialTooltips.displayUnits(stack.amount))
                    .append(' ')
                    .append(stack.material.getDisplayNameFallback());
        }
        return builder.toString();
    }
}
