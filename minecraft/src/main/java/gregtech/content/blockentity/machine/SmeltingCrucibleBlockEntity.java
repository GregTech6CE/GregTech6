package gregtech.content.blockentity.machine;

import gregtech.api.machine.BlockFace;
import gregtech.api.material.GTValues;
import gregtech.content.data.GregTechConstants;
import gregtech.content.data.GregTechTags;
import gregtech.content.data.MaterialPrefix;
import gregtech.content.material.Materials;
import gregtech.content.registry.GTBlockEntities;
import gregtech.content.registry.GTItems;
import gregtech.engine.machine.CrucibleSpec;
import gregtech.engine.machine.InitialSmelteryDefinitions;
import gregtech.engine.machine.crucible.*;
import gregtech.engine.tool.GTToolHelper;
import gregtech.util.GTEntityHelper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
*///?} else {
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
//?}

/**
 * Smelting crucible (GT6 {@code MultiTileEntitySmeltery}).
 * <p>
 * 16 material units of internal storage, one top cache slot, HU input on the bottom face.
 */
public class SmeltingCrucibleBlockEntity extends gregtech.content.blockentity.GTEnergyBlockEntity
        implements gregtech.engine.machine.ITileEntityCrucible,
                gregtech.engine.fluid.MoltenMaterialStorage,
                gregtech.api.machine.HeatHandler,
                gregtech.api.sensor.WeightSensorSource,
                gregtech.api.sensor.CompressionSensorSource {
    @Override
    public double weightKilograms(BlockFace side) {
        return CrucibleMaterialStack.weight(content);
    }

    @Override
    public long gibblValue(BlockFace side) {
        return gregtech.engine.machine.crucible.CrucibleMath.units(
                CrucibleMaterialStack.total(content), GTValues.U9, 1000, true);
    }

    @Override
    public long gibblMaximum(BlockFace side) {
        return gregtech.engine.machine.crucible.CrucibleMath.units(maxMaterialAmount(), GTValues.U9, 1000, true);
    }

    public static final long MAX_AMOUNT = 16L * GTValues.U;
    private final List<CrucibleMaterialStack> content = new java.util.concurrent.CopyOnWriteArrayList<>();
    /** Client-facing snapshot; renderers run on chunk-build threads and must not read {@code content}. */
    private byte displayedHeight;

    private int displayedMaterialId = -1;
    private boolean displayedMolten;
    private final ItemStackHandler cache = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final IItemHandler itemInput = new CrucibleAutomationInput(cache);

    private final gregtech.engine.machine.CrucibleSpec spec;
    private ThermalState thermal = new ThermalState(293, 293, 0, 100);

    public SmeltingCrucibleBlockEntity(BlockPos pos, BlockState state) {
        this(
                GTBlockEntities.SMELTING_CRUCIBLE.get(),
                pos,
                state,
                state.getBlock() instanceof gregtech.content.block.machine.SmeltingCrucibleBlock block
                        ? block.spec()
                        : InitialSmelteryDefinitions.ceramicCrucible());
    }

    public SmeltingCrucibleBlockEntity(BlockPos pos, BlockState state, CrucibleSpec spec) {
        this(GTBlockEntities.SMELTING_CRUCIBLE.get(), pos, state, spec);
    }

    protected SmeltingCrucibleBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            gregtech.engine.machine.CrucibleSpec spec) {
        super(type, pos, state);
        this.spec = spec;
    }

    public gregtech.engine.machine.CrucibleSpec spec() {
        return spec;
    }

    protected long maxMaterialAmount() {
        return MAX_AMOUNT;
    }

    protected CrucibleHazards.Profile hazardProfile() {
        return CrucibleHazards.SMALL;
    }

    protected double thermalMassKg() {
        return spec.smeltingThermalMassKg();
    }

    public long getMeltDownLimitK() {
        return spec.meltDownTemperatureK();
    }

    protected ItemStackHandler cacheHandler() {
        return cache;
    }

    protected AABB itemSuctionArea() {
        return new AABB(
                worldPosition.getX() + .125,
                worldPosition.getY() + .125,
                worldPosition.getZ() + .125,
                worldPosition.getX() + .875,
                worldPosition.getY() + 1,
                worldPosition.getZ() + .875);
    }

    protected long environmentTemperature() {
        return gregtech.content.blockentity.machine.SmelteryBlockEntityHelper.environmentTemperature(
                level, worldPosition);
    }

    protected void sync() {
        updateDisplayState();
        setChanged();
        syncToClient();
    }

    protected void meltdown() {
        level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F);
        gregtech.content.blockentity.machine.SmelteryFireHelper.crucibleMeltdown(
                level, worldPosition, hazardProfile(), getTemperature());
    }

    /** GT6 {@code MultiTileEntitySmeltery.removedByPlayer}: a crucible at lava's melting point melts down. */
    public void meltDownOnBreak() {
        content.clear();
        updateDisplayState();
        level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F);
        gregtech.content.blockentity.machine.SmelteryFireHelper.crucibleMeltdown(
                level, worldPosition, hazardProfile(), getTemperature(), 1.0F);
        setChanged();
    }

    protected void addHeatEnergy(long units) {
        if (units > 0) {
            thermal = new ThermalState(
                    getTemperature(),
                    thermal.previousTemperatureK(),
                    Math.min(Long.MAX_VALUE - units, getEnergyBuffer()) + units,
                    thermal.cooldownTicks());
            setChanged();
        }
    }

    protected void removeHeatEnergy(long units) {
        if (units > 0) {
            thermal = new ThermalState(
                    getTemperature(),
                    thermal.previousTemperatureK(),
                    Math.max(0, getEnergyBuffer() - units),
                    thermal.cooldownTicks());
            setChanged();
        }
    }

    protected void coolWithoutStructure() {
        if (level == null || level.isClientSide) return;
        long environment = environmentTemperature(), before = getTemperature(), temp = before;
        if (level.getGameTime() % 10 == 0) {
            if (temp > environment) temp--;
            else if (temp < environment) temp++;
        }
        temp = Math.max(temp, Math.min(200, environment));
        if (temp != before) {
            thermal =
                    new ThermalState(temp, thermal.previousTemperatureK(), getEnergyBuffer(), thermal.cooldownTicks());
            sync();
        }
    }

    protected boolean takeMaterial(gregtech.api.material.GTMaterial material, long amount) {
        if (amount <= 0
                || content.stream()
                                .filter(s -> s.material.resolve() == material.resolve())
                                .mapToLong(s -> s.amount)
                                .sum()
                        < amount) return false;
        long remaining = amount;
        for (var stack : content) {
            if (stack.material.resolve() != material.resolve()) continue;
            long taken = Math.min(remaining, stack.amount);
            stack.amount -= taken;
            remaining -= taken;
            if (remaining == 0) break;
        }
        content.removeIf(s -> s.amount <= 0);
        sync();
        return true;
    }

    protected void reactWithAir(long amount) {
        if (amount <= 0) return;
        CrucibleMaterialStack.of(Materials.Air, amount).addToList(content);
        CrucibleReactions.react(content, getTemperature());
        content.removeIf(s -> s.material.resolve() == Materials.Air.resolve() || s.amount <= 0);
        sync();
    }

    public byte getDisplayedHeight() {
        return displayedHeight;
    }

    public gregtech.api.material.GTMaterial getDisplayedMaterial() {
        return displayedMaterialId > 0 ? gregtech.engine.material.GTMaterialRegistry.get(displayedMaterialId) : null;
    }

    public int getDisplayedMaterialId() {
        return displayedMaterialId;
    }

    public boolean isDisplayedMolten() {
        return displayedMolten;
    }

    /** Recompute the render snapshot on the thread that owns {@code content}. */
    private void updateDisplayState() {
        CrucibleMaterialStack lightest = null;
        CrucibleMaterialStack lightestSolid = null;
        long totalAmount = 0;
        for (CrucibleMaterialStack stack : content) {
            if (stack == null
                    || stack.material == Materials.Invalid
                    || stack.material == Materials.Air
                    || stack.amount <= 0) continue;
            if (lightest == null || stack.material.getDensity() < lightest.material.getDensity()) lightest = stack;
            if (getTemperature() < stack.material.getMeltingPoint()
                    && (lightestSolid == null || stack.material.getDensity() < lightestSolid.material.getDensity()))
                lightestSolid = stack;
            totalAmount += stack.amount;
        }
        displayedHeight = (byte) CrucibleMath.scale(totalAmount, maxMaterialAmount(), 255, false);
        if (totalAmount <= 0) {
            displayedMaterialId = -1;
            displayedMolten = false;
        } else if (lightestSolid != null) {
            displayedMaterialId = lightestSolid.material.getId();
            displayedMolten = false;
        } else if (lightest != null && getTemperature() >= lightest.material.getMeltingPoint()) {
            displayedMaterialId = lightest.material.getId();
            displayedMolten = true;
        } else {
            displayedMaterialId = -1;
            displayedMolten = false;
        }
    }

    public boolean isMeltDownWarning() {
        return getTemperature() + 100 > getMeltDownLimitK();
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder()
                .with(gregtech.client.CrucibleModelData.MELTDOWN, isMeltDownWarning())
                .build();
    }

    @Override
//? if >=1.21.1 {
    /*public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookup) {
        super.handleUpdateTag(tag, lookup);
*///?} else {
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
//?}
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
//? if >=1.21.1 {
            /*gregtech.client.CrucibleClientSync.refreshHull(worldPosition);
*///?} else {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT,
                    () -> () -> gregtech.client.CrucibleClientSync.refreshHull(worldPosition));
//?}
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
//? if >=1.21.1 {
    /*public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider lookup) {
        if (packet.getTag() != null) handleUpdateTag(packet.getTag(), lookup);
    }
*///?} else {
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) handleUpdateTag(packet.getTag());
    }
//?}

    public long getTemperature() {
        return thermal.temperatureK();
    }

    @Override
    public long getMoltenCapacityUnits() {
        return maxMaterialAmount();
    }

    @Override
    public int fillMoltenMaterial(
            gregtech.api.material.GTMaterial material, int requested, long incomingTemperature, boolean execute) {
        return fillMoltenMaterial(
                material,
                requested,
                incomingTemperature,
                execute,
                gregtech.engine.fluid.MoltenMaterialUnits.ratio(material));
    }

    @Override
    public int fillMoltenMaterial(
            gregtech.api.material.GTMaterial material,
            int requested,
            long incomingTemperature,
            boolean execute,
            gregtech.engine.fluid.MoltenMaterialUnits.Ratio ratio) {
        if (level == null || level.isClientSide) return 0;
        int accepted = gregtech.engine.fluid.MoltenFluidPlans.fillAmount(
                content, maxMaterialAmount(), material, requested, incomingTemperature, ratio);
        if (accepted <= 0) return 0;
        var destination = execute
                ? content
                : new ArrayList<>(
                        content.stream().map(CrucibleMaterialStack::copy).toList());
        Long mixed = CrucibleProcess.admit(
                destination,
                List.of(CrucibleMaterialStack.of(
                        material, gregtech.engine.fluid.MoltenFluidPlans.toUnits(accepted, ratio))),
                maxMaterialAmount(),
                thermalMassKg(),
                getTemperature(),
                incomingTemperature);
        if (mixed == null) return 0;
        if (execute) {
            thermal = new ThermalState(
                    mixed, thermal.previousTemperatureK(), thermal.energyHU(), thermal.cooldownTicks());
            sync();
        }
        return accepted;
    }

    @Override
    public int drainMoltenMaterial(gregtech.api.material.GTMaterial material, int requested, boolean execute) {
        return drainMoltenMaterial(
                material, requested, execute, gregtech.engine.fluid.MoltenMaterialUnits.ratio(material));
    }

    @Override
    public int drainMoltenMaterial(
            gregtech.api.material.GTMaterial material,
            int requested,
            boolean execute,
            gregtech.engine.fluid.MoltenMaterialUnits.Ratio ratio) {
        if (level == null || level.isClientSide) return 0;
        int amount = gregtech.engine.fluid.MoltenFluidPlans.drainAmount(
                content, material, requested, getTemperature(), ratio);
        if (execute && amount > 0) {
            gregtech.engine.fluid.MoltenFluidPlans.remove(content, material, amount, ratio);
            sync();
        }
        return amount;
    }

    public long getEnergyBuffer() {
        return thermal.energyHU();
    }

    // ── HeatHandler: direct HU access into the signed buffer; the tick still owns conversion ──

    @Override
    public long heatStored() {
        return thermal.energyHU();
    }
    /** The crucible buffer is a long with no rated ceiling. */
    @Override
    public long heatCapacity() {
        return Long.MAX_VALUE;
    }

    @Override
    public long temperatureK() {
        return getTemperature();
    }

    @Override
    public long insertHeat(long signedHU, boolean simulate) {
        if (signedHU == 0) return 0;
        if (signedHU > 0) {
            long accepted = thermal.energyHU() < 0 ? signedHU : Math.min(signedHU, Long.MAX_VALUE - thermal.energyHU());
            if (!simulate && accepted > 0) addHeatEnergy(accepted);
            return accepted;
        }
        if (signedHU == Long.MIN_VALUE) return 0;
        long removed = Math.min(Math.max(0, thermal.energyHU()), -signedHU);
        if (!simulate && removed > 0) removeHeatEnergy(removed);
        return -removed;
    }

    @Override
    public long extractHeat(long signedHU, boolean simulate) {
        if (signedHU == 0) return 0;
        if (signedHU > 0) {
            long removed = Math.min(Math.max(0, thermal.energyHU()), signedHU);
            if (!simulate && removed > 0) removeHeatEnergy(removed);
            return removed;
        }
        if (signedHU == Long.MIN_VALUE) return 0;
        return -insertHeat(-signedHU, simulate);
    }

    public List<CrucibleMaterialStack> getContentView() {
        return content.stream().map(CrucibleMaterialStack::copy).toList();
    }

    public int getDisplayFillPercent() {
        return (getDisplayedHeight() & 255) * 100 / 255;
    }

    /** GT6 {@code setBlockBounds2} pass 5 top Y in block space. */
    public float getContentFillMaxY() {
        int height = displayedHeight & 0xFF;
        return (2 / 16.0F) + height / 292.571428F;
    }

    public String formatContentSummary() {
        if (content.isEmpty()) return "empty";
        return content.stream()
                .map(stack -> displayUnits(stack.amount) + " " + stack.material.getDisplayNameFallback())
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private static String displayUnits(long amount) {
        if (amount < 0) return "?.???";
        long digits = ((amount % GTValues.U) * 1000) / GTValues.U;
        return (amount / GTValues.U) + "." + (digits < 10 ? "00" : digits < 100 ? "0" : "") + digits;
    }

    public ItemStack getCacheStack() {
        return cache.getStackInSlot(0);
    }

    public long receiveHeat(Direction side, long size, long amount, boolean commit) {
        return doEnergyInjection(GregTechTags.Energy.HU, side, size, amount, commit);
    }

    public boolean addMaterialStacks(List<CrucibleMaterialStack> incoming, long incomingTemperature) {
        Long mixed = CrucibleProcess.admit(
                content, incoming, maxMaterialAmount(), thermalMassKg(), getTemperature(), incomingTemperature);
        if (mixed == null) return false;
        thermal = new ThermalState(mixed, thermal.previousTemperatureK(), thermal.energyHU(), thermal.cooldownTicks());
        setChanged();
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SmeltingCrucibleBlockEntity be) {
        if (!level.isClientSide) {
            be.tickServer();
        }
    }

    protected void tickServer() {
        int before = content.hashCode();
        if (getBlockState().getBlock() instanceof gregtech.content.block.machine.SmeltingCrucibleBlock) {
            long rain = gregtech.content.blockentity.machine.SmallCrucibleRain.amount(level, worldPosition);
            if (rain > 0)
                addMaterialStacks(List.of(CrucibleMaterialStack.of(Materials.Water, rain)), environmentTemperature());
        }

        if (getCacheStack().isEmpty()) {
            AABB area = itemSuctionArea();
            for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, area)) {
                if (entity.isRemoved() || entity.getItem().isEmpty()) continue;
                cache.setStackInSlot(0, entity.getItem().copy());
                entity.discard();
                break;
            }
        }
        ItemStack item = getCacheStack();
        if (!item.isEmpty() && addMaterialStacks(CrucibleItemInput.parse(item), environmentTemperature())) {
            item.shrink(1);
            if (item.isEmpty()) cache.setStackInSlot(0, ItemStack.EMPTY);
        }
        var phase = CrucibleProcess.process(
                content,
                getTemperature(),
                thermal.previousTemperatureK(),
                before != content.hashCode(),
                spec.acidProof());
        for (int i = 0; i < phase.fizzCount(); i++)
            level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F);
        for (var vapor : phase.vapors())
            gregtech.content.blockentity.machine.SmelteryFireHelper.vaporize(
                    level, worldPosition, hazardProfile(), vapor);
        if (phase.explosiveAmount() > 0) {
            int strength = CrucibleHazards.explosionPower(hazardProfile(), phase.explosiveAmount());
            level.removeBlock(worldPosition, false);
            level.explode(
                    null,
                    worldPosition.getX(),
                    worldPosition.getY(),
                    worldPosition.getZ(),
                    strength,
                    net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
            return;
        }
        if (phase.acidDestroyedHull()) {
            level.removeBlock(worldPosition, false);
            return;
        }
        thermal = ThermalStep.advance(
                thermal, environmentTemperature(), thermalMassKg() + CrucibleMaterialStack.weight(content));
        updateDisplayState();
        if (getTemperature() > getMeltDownLimitK()) {
            content.clear();
            updateDisplayState();
            meltdown();
            return;
        }
        if (before != content.hashCode() || level.getGameTime() % 20 == 0) sync();
        setChanged();
    }

    @Override
    public long getCrucibleTemperature() {
        return getTemperature();
    }

    @Override
    public long getCrucibleContentAmount() {
        return CrucibleMaterialStack.total(content);
    }

    @Override
    public boolean fillMoldAtSide(gregtech.engine.machine.ITileEntityMold mold, int crucibleSide, int moldSide) {
        if (mold == null) return false;
        boolean transferred = false;
        for (var stack : content) {
            if (stack.amount <= 0 || getTemperature() < stack.material.getMeltingPoint()) continue;
            long consumed = mold.fillMold(stack.material, stack.amount, getTemperature(), moldSide);
            if (consumed > 0) {
                stack.amount -= consumed;
                transferred = true;
            }
        }
        content.removeIf(stack -> stack.amount <= 0);
        if (transferred) {
            CrucibleMaterialStack.consolidate(content);
            sync();
        }
        return transferred;
    }

    public boolean use(Player player, InteractionHand hand, BlockHitResult hit) {
        if (hit.getDirection() != Direction.UP) return false;
        if (level.isClientSide) return canHandleUse(player, hand);
        return tryUse(player, hand, hit).consumesAction();
    }

    public boolean canHandleUse(Player player, InteractionHand hand) {
        return wouldHandleUse(player, hand);
    }

    /** Server-only interaction entry (via {@link gregtech.event.CrucibleInteractionHandler}). */
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
            ItemStack cached = cache.getStackInSlot(0);
            if (!cached.isEmpty()) {
                giveItemPreferMainHand(player, cached.copy());
                cache.setStackInSlot(0, ItemStack.EMPTY);
                GTEntityHelper.applyTemperatureDamage(player, getTemperature(), 1.0F, 5.0F);
                sync();
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
        InteractionHand other =
                hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
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
//? if >=1.21.1 {
                /*&& ItemStack.isSameItemSameComponents(held, expected)
*///?} else {
                && ItemStack.isSameItemSameTags(held, expected)
//?}
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
        if (!cache.getStackInSlot(0).isEmpty()) {
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
            if (getTemperature() >= stack.material.getMeltingPoint()) {
                continue;
            }
            if (lightest == null || stack.material.getDensity() < lightest.material.getDensity()) {
                lightest = stack;
            }
        }
        return lightest;
    }

    /** GT6 {@code onBlockActivated3} — empty hand, one {@code scrapGt} (U9), getTemperature() damage. */
    private InteractionResult handScrapeSolidContent(Player player, InteractionHand hand) {
        if (!extractOneSolidScrap(player, hand, true)) {
            return InteractionResult.PASS;
        }
        sync();
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
        int maxStacks =
                lightest.amount < scrapUnit ? 1 : (int) Math.max(1L, Math.min(64L, lightest.amount / scrapUnit));
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
        sync();
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
                GTEntityHelper.applyTemperatureDamage(player, getTemperature(), 1.0F, 5.0F);
            }
            return true;
        }

        ItemStack output = GTItems.getStack(MaterialPrefix.scrapGt, lightest.material, 1);
        if (output.isEmpty()) {
            lightest.amount = 0;
            content.removeIf(stack -> stack == null || stack.amount <= 0);
            CrucibleMaterialStack.consolidate(content);
            if (temperatureDamage) {
                GTEntityHelper.applyTemperatureDamage(player, getTemperature(), 1.0F, 5.0F);
            }
            return true;
        }

        giveScrapToPlayer(player, hand, output);
        lightest.amount -= scrapUnit;
        content.removeIf(stack -> stack == null || stack.amount <= 0);
        CrucibleMaterialStack.consolidate(content);
        if (temperatureDamage) {
            GTEntityHelper.applyTemperatureDamage(player, getTemperature(), 1.0F, 5.0F);
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
//? if >=1.21.1 {
            /*if (ItemStack.isSameItemSameComponents(clicked, scrap) && clicked.getCount() < clicked.getMaxStackSize()) {
*///?} else {
            if (ItemStack.isSameItemSameTags(clicked, scrap) && clicked.getCount() < clicked.getMaxStackSize()) {
//?}
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
//? if >=1.21.1 {
        /*if (ItemStack.isSameItemSameComponents(main, scrap) && main.getCount() < main.getMaxStackSize()) {
*///?} else {
        if (ItemStack.isSameItemSameTags(main, scrap) && main.getCount() < main.getMaxStackSize()) {
//?}
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
//? if >=1.21.1 {
        /*if (ItemStack.isSameItemSameComponents(main, stack)
                && main.getCount() + stack.getCount() <= main.getMaxStackSize()) {
*///?} else {
        if (ItemStack.isSameItemSameTags(main, stack)
                && main.getCount() + stack.getCount() <= main.getMaxStackSize()) {
//?}
            main.grow(stack.getCount());
            player.setItemInHand(InteractionHand.MAIN_HAND, main);
            return;
        }
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    public void dropContents() {
        if (level == null || level.isClientSide) return;
        gregtech.util.GTItemDrops.dropItemStack(
                level,
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5,
                getCacheStack());
        cache.setStackInSlot(0, ItemStack.EMPTY);
    }

    @Override
//? if >=1.21.1 {
    /*protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
*///?} else {
    public void load(CompoundTag tag) {
        super.load(tag);
//?}
        long temperature =
                tag.contains("gt.temperature") ? tag.getLong("gt.temperature") : GregTechConstants.DEF_ENV_TEMP;
        thermal = new ThermalState(
                temperature,
                tag.contains("gt.temperature.old") ? tag.getLong("gt.temperature.old") : temperature,
                tag.getLong("gt.energy.buffer"),
                tag.contains("gt.cooldown") ? tag.getInt("gt.cooldown") : 100);
        content.clear();
        ListTag list = tag.getList("gt.materials", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            var material = gregtech.engine.material.StoredMaterial.readStackMaterial(entry);
            long amount = entry.getLong("amount");
            if (material.isValid() && amount > 0) content.add(CrucibleMaterialStack.of(material, amount));
        }
//? if >=1.21.1 {
        /*if (tag.contains("gt.cache")) cache.deserializeNBT(lookup, tag.getCompound("gt.cache"));
*///?} else {
        if (tag.contains("gt.cache")) cache.deserializeNBT(tag.getCompound("gt.cache"));
//?}
        updateDisplayState();
    }

    @Override
//? if >=1.21.1 {
    /*protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
*///?} else {
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
//?}
        tag.putInt("Meta", spec.gt6MetaId());
        tag.putLong("gt.temperature", thermal.temperatureK());
        tag.putLong("gt.temperature.old", thermal.previousTemperatureK());
        tag.putLong("gt.energy.buffer", thermal.energyHU());
        tag.putInt("gt.cooldown", thermal.cooldownTicks());
        tag.putBoolean("gt.meltdown", getTemperature() + 100 > getMeltDownLimitK());
        ListTag list = new ListTag();
        for (var stack : content) {
            CompoundTag entry = new CompoundTag();
            gregtech.engine.material.StoredMaterial.writeStack(entry, stack.material, stack.amount);
            list.add(entry);
        }
        tag.put("gt.materials", list);
//? if >=1.21.1 {
        /*tag.put("gt.cache", cache.serializeNBT(lookup));
*///?} else {
        tag.put("gt.cache", cache.serializeNBT());
//?}
    }

    @Override
    public boolean isEnergyAcceptingFrom(GregTechTags.Tag type, @Nullable Direction side, boolean theoretical) {
        return gregtech.engine.machine.crucible.CrucibleEnergyRules.accepts(type);
    }

    @Override
    public boolean isEnergyType(GregTechTags.Tag energyType, @Nullable Direction side, boolean emitting) {
        return !emitting && gregtech.engine.machine.crucible.CrucibleEnergyRules.accepts(energyType);
    }

    @Override
    public Collection<GregTechTags.Tag> getEnergyTypes(@Nullable Direction side) {
        return gregtech.engine.machine.crucible.CrucibleEnergyRules.INPUTS;
    }

    @Override
    public boolean hasEnergySurface(@Nullable Direction side) {
        return true;
    }

    @Override
    public long getEnergySizeInputMin(GregTechTags.Tag energyType, @Nullable Direction side) {
        return gregtech.engine.machine.crucible.CrucibleEnergyRules.accepts(energyType)
                ? CrucibleSpec.MIN_HU_PER_TICK
                : 0;
    }

    @Override
    public long getEnergySizeInputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) {
        return gregtech.engine.machine.crucible.CrucibleEnergyRules.accepts(energyType) ? 2048 : 0;
    }

    @Override
    public long getEnergySizeInputMax(GregTechTags.Tag energyType, @Nullable Direction side) {
        return gregtech.engine.machine.crucible.CrucibleEnergyRules.accepts(energyType) ? Long.MAX_VALUE : 0;
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
        if (!gregtech.engine.machine.crucible.CrucibleEnergyRules.accepts(energyType)) {
            return 0;
        }
        return Long.MAX_VALUE;
    }

    @Override
    public long doInject(
            GregTechTags.Tag energyType, @Nullable Direction side, long size, long amount, boolean doInject) {
        if (!gregtech.engine.machine.crucible.CrucibleEnergyRules.accepts(energyType) || amount <= 0) {
            return 0;
        }
        if (energyType == GregTechTags.Energy.KU) {
            if (doInject) {
                long air = gregtech.engine.machine.crucible.CrucibleEnergyRules.airAmount(
                        size, amount, Math.max(0, maxMaterialAmount() - getCrucibleContentAmount()));
                if (air > 0) addMaterialStacks(List.of(CrucibleMaterialStack.of(Materials.Air, air)), getTemperature());
            }
            return amount;
        }
        ThermalState received;
        try {
            received = ThermalStep.receive(
                    thermal,
                    energyType == GregTechTags.Energy.CU ? ThermalStep.EnergyKind.COOLING : ThermalStep.EnergyKind.HEAT,
                    size,
                    amount);
        } catch (IllegalArgumentException | ArithmeticException invalidPacket) {
            // Reject the entire packet count; simulated and actual acceptance must agree.
            return 0;
        }
        if (doInject) {
            thermal = received;
            setChanged();
        }
        return amount;
    }

//? if >=1.21.1 {
    /*public IItemHandler itemInputHandler(@Nullable Direction side) {
        return side == Direction.UP && !isRemoved() ? itemInput : null;
    }

    public net.neoforged.neoforge.fluids.capability.IFluidHandler moltenFluidHandler() {
*///?} else {
    private net.minecraftforge.common.util.LazyOptional<IItemHandler> itemInputCapability =
            net.minecraftforge.common.util.LazyOptional.of(() -> itemInput);
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.fluids.capability.IFluidHandler>
            moltenFluidCapability =
                    net.minecraftforge.common.util.LazyOptional.of(() -> new gregtech.engine.fluid.MoltenFluidHandler(this));

    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction side) {
        if (capability == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER
                && side == Direction.UP) return itemInputCapability.cast();
        if (capability == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER
                && side != Direction.DOWN) return moltenFluidCapability.cast();
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        moltenFluidCapability.invalidate();
        itemInputCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemInputCapability = net.minecraftforge.common.util.LazyOptional.of(() -> itemInput);
        moltenFluidCapability =
                net.minecraftforge.common.util.LazyOptional.of(() -> new gregtech.engine.fluid.MoltenFluidHandler(this));
    }

    public net.minecraftforge.fluids.capability.IFluidHandler moltenFluidHandler() {
//?}
        return new gregtech.engine.fluid.MoltenFluidHandler(this);
    }
}