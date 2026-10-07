package com.gregtech.gregtech.platform.neoforge.smeltery;

import com.gregtech.gregtech.api.machine.InitialSmelteryDefinitions;
import com.gregtech.gregtech.api.machine.crucible.*;
import com.gregtech.gregtech.api.material.GTMaterialRegistry;
import com.gregtech.gregtech.api.material.GTValues;
import com.gregtech.gregtech.data.GregTechConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemStackHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.api.machine.CrucibleSpec;
import javax.annotation.Nullable;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.registry.GTItems;
import com.gregtech.gregtech.content.material.Materials;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.util.GTEntityHelper;
import net.minecraft.world.InteractionResult;

/** Neo world/cache/storage adapter for the shared real crucible engine. */
public class SmeltingCrucibleEntity extends com.gregtech.gregtech.blockentity.GTEnergyBlockEntity implements com.gregtech.gregtech.api.energy.HeatEnergyBlock, com.gregtech.gregtech.api.machine.ITileEntityCrucible, com.gregtech.gregtech.api.fluid.MoltenMaterialStorage {
    public static final long MAX_AMOUNT = 16L * GTValues.U;
    private final List<CrucibleMaterialStack> content = new ArrayList<>();
    private final ItemStackHandler cache = new ItemStackHandler(1) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private final com.gregtech.gregtech.api.machine.CrucibleSpec spec;
    private ThermalState thermal = new ThermalState(293, 293, 0, 100);
    public SmeltingCrucibleEntity(BlockPos pos, BlockState state) { this(SmelteryRegistries.SMELTING_CRUCIBLE.get(),pos,state,state.getBlock() instanceof com.gregtech.gregtech.block.machine.SmeltingCrucibleBlock block?block.spec():InitialSmelteryDefinitions.ceramicCrucible()); }
    public SmeltingCrucibleEntity(BlockPos pos,BlockState state,CrucibleSpec spec){this(SmelteryRegistries.SMELTING_CRUCIBLE.get(),pos,state,spec);}
    protected SmeltingCrucibleEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,BlockPos pos,BlockState state,com.gregtech.gregtech.api.machine.CrucibleSpec spec){super(type,pos,state);this.spec=spec;}
    public com.gregtech.gregtech.api.machine.CrucibleSpec spec(){return spec;}
    protected long maxMaterialAmount(){return MAX_AMOUNT;}
    protected double thermalMassKg(){return spec.smeltingThermalMassKg();}
    public long getMeltDownLimitK(){return spec.meltDownTemperatureK();}
    protected ItemStackHandler cacheHandler(){return cache;}
    protected AABB itemSuctionArea(){return new AABB(worldPosition.getX()+.125,worldPosition.getY()+.125,worldPosition.getZ()+.125,worldPosition.getX()+.875,worldPosition.getY()+1,worldPosition.getZ()+.875);}
    protected long environmentTemperature(){return com.gregtech.gregtech.blockentity.machine.SmelteryBlockEntityHelper.environmentTemperature(level,worldPosition);}
    protected void sync(){setChanged();syncToClient();}
    protected void meltdown(){com.gregtech.gregtech.blockentity.machine.SmelteryBlockEntityHelper.meltdown(level,worldPosition);}
    protected void addHeatEnergy(long units){if(units>0){thermal=new ThermalState(getTemperature(),thermal.previousTemperatureK(),Math.min(Long.MAX_VALUE-units,getEnergyBuffer())+units,thermal.cooldownTicks());setChanged();}}
    protected void removeHeatEnergy(long units){if(units>0){thermal=new ThermalState(getTemperature(),thermal.previousTemperatureK(),Math.max(0,getEnergyBuffer()-units),thermal.cooldownTicks());setChanged();}}
    protected void coolWithoutStructure(){if(level==null||level.isClientSide)return;long environment=environmentTemperature(),before=getTemperature(),temp=before;if(level.getGameTime()%10==0){if(temp>environment)temp--;else if(temp<environment)temp++;}temp=Math.max(temp,Math.min(200,environment));if(temp!=before){thermal=new ThermalState(temp,thermal.previousTemperatureK(),getEnergyBuffer(),thermal.cooldownTicks());sync();}}
    protected void tickServer(){serverTick();}
    protected boolean takeMaterial(com.gregtech.gregtech.api.material.GTMaterial material,long amount){if(amount<=0||content.stream().filter(s->s.material.resolve()==material.resolve()).mapToLong(s->s.amount).sum()<amount)return false;long remaining=amount;for(var stack:content){if(stack.material.resolve()!=material.resolve())continue;long taken=Math.min(remaining,stack.amount);stack.amount-=taken;remaining-=taken;if(remaining==0)break;}content.removeIf(s->s.amount<=0);sync();return true;}
    protected void reactWithAir(long amount){if(amount<=0)return;CrucibleMaterialStack.of(Materials.Air,amount).addToList(content);CrucibleReactions.react(content,getTemperature());content.removeIf(s->s.material.resolve()==Materials.Air.resolve()||s.amount<=0);sync();}
    public byte getDisplayedHeight(){return (byte)CrucibleMath.scale(content.stream().filter(s->s.material!=Materials.Invalid&&s.material!=Materials.Air&&s.amount>0).mapToLong(s->s.amount).sum(),maxMaterialAmount(),255,false);}
    public com.gregtech.gregtech.api.material.GTMaterial getDisplayedMaterial(){var solid=content.stream().filter(s->s.material!=Materials.Invalid&&s.material!=Materials.Air&&s.amount>0&&getTemperature()<s.material.getMeltingPoint()).min(java.util.Comparator.comparingDouble(s->s.material.getDensity()));return solid.or(()->content.stream().filter(s->s.material!=Materials.Invalid&&s.material!=Materials.Air&&s.amount>0).min(java.util.Comparator.comparingDouble(s->s.material.getDensity()))).map(s->s.material).orElse(null);}
    public int getDisplayedMaterialId(){var material=getDisplayedMaterial();return material==null?-1:material.getId();}
    public boolean isDisplayedMolten(){var material=getDisplayedMaterial();return material!=null&&getTemperature()>=material.getMeltingPoint();}
    public boolean isMeltDownWarning(){return getTemperature()+100>getMeltDownLimitK();}

    @Override
    public net.neoforged.neoforge.client.model.data.ModelData getModelData() {
        return net.neoforged.neoforge.client.model.data.ModelData.builder()
                .with(com.gregtech.gregtech.client.CrucibleModelData.MELTDOWN, isMeltDownWarning()).build();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookup) {
        super.handleUpdateTag(tag, lookup);
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
            com.gregtech.gregtech.client.CrucibleClientSync.refreshHull(worldPosition);
        }
    }

    public long getTemperature() { return thermal.temperatureK(); }

    @Override public long getMoltenCapacityUnits() { return maxMaterialAmount(); }
    @Override public int fillMoltenMaterial(com.gregtech.gregtech.api.material.GTMaterial material, int requested, long incomingTemperature, boolean execute) {
        if (level == null || level.isClientSide) return 0;
        int accepted = com.gregtech.gregtech.api.fluid.MoltenFluidPlans.fillAmount(content, maxMaterialAmount(), material, requested, incomingTemperature);
        if (accepted <= 0) return 0;
        var destination = execute ? content : new ArrayList<>(content.stream().map(CrucibleMaterialStack::copy).toList());
        Long mixed = CrucibleProcess.admit(destination, List.of(CrucibleMaterialStack.of(material, com.gregtech.gregtech.api.fluid.MoltenFluidPlans.toUnits(accepted))), maxMaterialAmount(), thermalMassKg(), getTemperature(), incomingTemperature);
        if (mixed == null) return 0;
        if (execute) { thermal = new ThermalState(mixed, thermal.previousTemperatureK(), thermal.energyHU(), thermal.cooldownTicks()); sync(); }
        return accepted;
    }
    @Override public int drainMoltenMaterial(com.gregtech.gregtech.api.material.GTMaterial material, int requested, boolean execute) {
        if (level == null || level.isClientSide) return 0;
        int amount = com.gregtech.gregtech.api.fluid.MoltenFluidPlans.drainAmount(content, material, requested, getTemperature());
        if (execute && amount > 0) { com.gregtech.gregtech.api.fluid.MoltenFluidPlans.remove(content, material, amount); sync(); }
        return amount;
    }
    public net.neoforged.neoforge.fluids.capability.IFluidHandler moltenFluidHandler() { return new com.gregtech.gregtech.api.fluid.MoltenFluidHandler(this); }

    public long getEnergyBuffer() { return thermal.energyHU(); }
    public List<CrucibleMaterialStack> getContentView() { return content.stream().map(CrucibleMaterialStack::copy).toList(); }
    public int getDisplayFillPercent() { return (getDisplayedHeight() & 255) * 100 / 255; }
    public String formatContentSummary() {
        if(content.isEmpty()) return "empty";
        return content.stream().map(stack -> displayUnits(stack.amount)+" "+stack.material.getDisplayNameFallback())
                .collect(java.util.stream.Collectors.joining(", "));
    }
    private static String displayUnits(long amount) {
        if(amount<0) return "?.???";
        long digits=((amount % GTValues.U)*1000)/GTValues.U;
        return (amount / GTValues.U)+"."+(digits<10?"00":digits<100?"0":"")+digits;
    }
    public ItemStack getCacheStack() { return cache.getStackInSlot(0); }

    public long receiveHeat(Direction side,long size,long amount,boolean commit){return doEnergyInjection(GregTechTags.Energy.HU,side,size,amount,commit);}
    public boolean addMaterialStacks(List<CrucibleMaterialStack> incoming, long incomingTemperature) {
        Long mixed = CrucibleProcess.admit(content, incoming, maxMaterialAmount(),
                thermalMassKg(), getTemperature(), incomingTemperature);
        if (mixed == null) return false;
        thermal = new ThermalState(mixed, thermal.previousTemperatureK(), thermal.energyHU(), thermal.cooldownTicks());
        setChanged();
        return true;
    }
    public void serverTick() {
        int before = content.hashCode();
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
        var phase = CrucibleProcess.process(content, getTemperature(), thermal.previousTemperatureK(),
                before != content.hashCode(), spec.acidProof());
        for (int i = 0; i < phase.vaporizedStacks(); i++)
            level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F);
        if (phase.acidDestroyedHull()) { level.removeBlock(worldPosition, false); return; }
        thermal = ThermalStep.advance(thermal, environmentTemperature(),
                thermalMassKg() + CrucibleMaterialStack.weight(content));
        if(phase.vaporizedStacks()>0)com.gregtech.gregtech.blockentity.machine.SmelteryFireHelper.tryIgniteNearby(level,worldPosition,getTemperature());
        if (getTemperature() > getMeltDownLimitK()) {
            content.clear(); meltdown(); return;
        }
        if (before != content.hashCode() || level.getGameTime() % 20 == 0) sync();
        setChanged();
    }
    public boolean fillMold(MoldEntity mold, Direction moldSide) {
        return fillMoldAtSide(mold, moldSide.getOpposite().ordinal(), moldSide.ordinal());
    }
    @Override public long getCrucibleTemperature() { return getTemperature(); }
    @Override public long getCrucibleContentAmount() { return CrucibleMaterialStack.total(content); }
    @Override public boolean fillMoldAtSide(com.gregtech.gregtech.api.machine.ITileEntityMold mold, int crucibleSide, int moldSide) {
        if (mold == null) return false;
        boolean transferred = false;
        for (var stack : content) {
            if (stack.amount <= 0 || getTemperature() < stack.material.getMeltingPoint()) continue;
            long consumed = mold.fillMold(stack.material, stack.amount, getTemperature(), moldSide);
            if (consumed > 0) { stack.amount -= consumed; transferred = true; }
        }
        content.removeIf(stack -> stack.amount <= 0);
        if (transferred) { CrucibleMaterialStack.consolidate(content); sync(); }
        return transferred;
    }
    public boolean use(Player player,InteractionHand hand,BlockHitResult hit){if(hit.getDirection()!=Direction.UP)return false;if(level.isClientSide)return canHandleUse(player,hand);return tryUse(player,hand,hit).consumesAction();}
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
                && ItemStack.isSameItemSameComponents(held, expected)
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
            if (ItemStack.isSameItemSameComponents(clicked, scrap) && clicked.getCount() < clicked.getMaxStackSize()) {
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
        if (ItemStack.isSameItemSameComponents(main, scrap) && main.getCount() < main.getMaxStackSize()) {
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
        if (ItemStack.isSameItemSameComponents(main, stack)
                && main.getCount() + stack.getCount() <= main.getMaxStackSize()) {
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
        com.gregtech.gregtech.util.GTItemDrops.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, getCacheStack());
        cache.setStackInSlot(0, ItemStack.EMPTY);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        long temperature = tag.contains("gt.temperature") ? tag.getLong("gt.temperature") : GregTechConstants.DEF_ENV_TEMP;
        thermal = new ThermalState(temperature, tag.contains("gt.temperature.old") ? tag.getLong("gt.temperature.old") : temperature,
                tag.getLong("gt.energy.buffer"), tag.contains("gt.cooldown") ? tag.getInt("gt.cooldown") : 100);
        content.clear();
        ListTag list = tag.getList("gt.materials", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            var material = GTMaterialRegistry.get(entry.getInt("id"));
            long amount = entry.getLong("amount");
            if (material.isValid() && amount > 0) content.add(CrucibleMaterialStack.of(material, amount));
        }
        if (tag.contains("gt.cache")) cache.deserializeNBT(lookup, tag.getCompound("gt.cache"));
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        tag.putInt("Meta", spec.gt6MetaId());
        tag.putLong("gt.temperature", thermal.temperatureK());
        tag.putLong("gt.temperature.old", thermal.previousTemperatureK());
        tag.putLong("gt.energy.buffer", thermal.energyHU());
        tag.putInt("gt.cooldown", thermal.cooldownTicks());
        tag.putBoolean("gt.meltdown", getTemperature() + 100 > getMeltDownLimitK());
        ListTag list = new ListTag();
        for (var stack : content) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("id", stack.material.getId());
            entry.putLong("amount", stack.amount);
            list.add(entry);
        }
        tag.put("gt.materials", list);
        tag.put("gt.cache", cache.serializeNBT(lookup));
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
        return Long.MAX_VALUE - getEnergyBuffer();
    }

    @Override
    public long doInject(GregTechTags.Tag energyType, @Nullable Direction side, long size, long amount, boolean doInject) {
        if (energyType != GregTechTags.Energy.HU || side != Direction.DOWN || amount <= 0) {
            return 0;
        }
        ThermalState received;
        try {
            received = ThermalStep.receive(
                    thermal,
                    ThermalStep.EnergyKind.HEAT, size, amount);
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

}
