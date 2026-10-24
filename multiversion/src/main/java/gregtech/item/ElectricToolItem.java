package gregtech.item;
import gregtech.internal.material.MaterialPresentation;

import gregtech.internal.energy.item.IItemEnergy;
import gregtech.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.Level;
import gregtech.content.tool.ElectricToolWear;

/** GT6 electric tool base — stores EU and consumes power on use. */

//? if >=1.21.1 {
/*public class ElectricToolItem extends TieredItem implements IItemEnergy, gregtech.internal.tool.PoweredToolItem {
*///?} else {
public class ElectricToolItem extends TieredItem implements IItemEnergy {
//?}

    private final long capacity;
    private final int tier;
    private final long energyPerUse;
    private final String toolName;

    public ElectricToolItem(String toolName, Tier toolTier, long capacity, int tier, long energyPerUse, Properties properties) {
        super(toolTier, properties);
        this.toolName = toolName;
        this.capacity = capacity;
        this.tier = tier;
        this.energyPerUse = energyPerUse;
    }


//? if >=1.21.1 {
/*    public boolean canHarvest(ItemStack stack,net.minecraft.world.level.block.state.BlockState state){return isCorrectToolForDrops(stack,state);}
    public float harvestSpeed(ItemStack stack){return Math.max(Float.MIN_NORMAL,2*headMaterial(stack).getToolSpeed());}
*///?} else {
//?}

    public gregtech.content.tool.ElectricToolCatalog.Definition definition(){return gregtech.content.tool.ElectricToolCatalog.of(toolName,tier);}
    public String toolName() { return toolName; }
    public int energyTier() { return tier; }

    @Override public float getDestroySpeed(ItemStack stack, net.minecraft.world.level.block.state.BlockState state) {
        if (gregtech.content.tool.ElectricWrenchHarvest.target(state) && toolName.equals("Wrench"))
            return gregtech.content.tool.ElectricWrenchHarvest.canHarvest(this,stack,state)
                    ? gregtech.content.tool.ElectricWrenchHarvest.speed(this,stack) : 0;
        if (toolName.equals("Chainsaw") && gregtech.content.tool.ElectricChainsawHarvest.target(state))
            return gregtech.content.tool.ElectricChainsawHarvest.canHarvest(this,stack,state)
                    ? gregtech.content.tool.ElectricChainsawHarvest.speed(this,stack) : 0;
        if(gregtech.content.tool.ElectricUtilityHarvest.target(this,state))return gregtech.content.tool.ElectricUtilityHarvest.canHarvest(this,stack,state)?gregtech.content.tool.ElectricUtilityHarvest.speed(this,stack):0;
        return super.getDestroySpeed(stack,state);
    }

    @Override public boolean isCorrectToolForDrops(ItemStack stack, net.minecraft.world.level.block.state.BlockState state) {
        return gregtech.content.tool.ElectricWrenchHarvest.canHarvest(this,stack,state)
                || gregtech.content.tool.ElectricChainsawHarvest.canHarvest(this,stack,state)
                || gregtech.content.tool.ElectricUtilityHarvest.canHarvest(this,stack,state);
    }

    @Override public boolean mineBlock(ItemStack stack, Level level, net.minecraft.world.level.block.state.BlockState state,
                                       BlockPos pos, net.minecraft.world.entity.LivingEntity user) {
        if (!level.isClientSide && (toolName.equals("Wrench") || toolName.equals("Chainsaw") || toolName.equals("Mining Drill") || toolName.equals("BuzzSaw") || toolName.equals("Trimmer")) && isPoweredUsable(stack)) {
            float hardness=state.getDestroySpeed(level,pos);
            if (hardness>0) consumeInteractionEnergy(stack,(long)Math.ceil(definition().blockCost()*hardness),user);
        }
        return true;
    }

    /** Only the two currently implemented machine-click tool identities. Drill is not a pickaxe. */
    @javax.annotation.Nullable
    public gregtech.internal.tool.GTToolType interactionType(ItemStack stack) {
        return switch (toolName) {
            case "Wrench" -> monkeyWrenchMode(stack) ? gregtech.internal.tool.GTToolType.MONKEY_WRENCH
                    : gregtech.internal.tool.GTToolType.WRENCH;
            case "Drill" -> gregtech.internal.tool.GTToolType.HAND_DRILL;
            case "Chainsaw","BuzzSaw" -> gregtech.internal.tool.GTToolType.SAW;
            case "Trimmer" -> gregtech.internal.tool.GTToolType.BRANCH_CUTTER;
            case "Screwdriver" -> gregtech.internal.tool.GTToolType.SCREWDRIVER;
            default -> null;
        };
    }

    public boolean monkeyWrenchMode(ItemStack stack) {

//? if >=1.21.1 {
/*        return toolName.equals("Wrench") && gregtech.platform.neoforge.StackCustomData.read(stack).getBoolean("gt.monkey_wrench");
*///?} else {
        return toolName.equals("Wrench") && stack.hasTag() && stack.getTag().getBoolean("gt.monkey_wrench");
//?}

    }

    /** GT6 switch behavior follows the tool click and ignores foreign block entities. */
    @Override public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        var player = context.getPlayer();
        var stack = context.getItemInHand();
        if(toolName.equals("Mining Drill")) {
            if(!isPoweredUsable(stack))return net.minecraft.world.InteractionResult.PASS;
            var plugged=gregtech.item.behavior.BehaviorPlugLeak.use(context);
            return plugged.consumesAction()?plugged:gregtech.item.behavior.BehaviorPlaceWoodworkingSupplies.placeTorch(context,true);
        }
        if (toolName.equals("Chainsaw")) {
            boolean usable=stack.getCount()==1 && !ElectricToolWear.broken(this,stack)
                    && (isPoweredUsable(stack)||player!=null&&player.getAbilities().instabuild);
            return usable ? gregtech.item.behavior.BehaviorPlaceWoodworkingSupplies.use(context)
                    : net.minecraft.world.InteractionResult.PASS;
        }
        if (toolName.equals("Screwdriver") && player != null && player.isShiftKeyDown() && stack.getCount() == 1) {
            var level = context.getLevel();
            var pos = context.getClickedPos();
            var state = level.getBlockState(pos);
//? if >=1.21.1 {
/*            if (state.getBlock() instanceof gregtech.internal.tool.ScrewdriverUseTarget target) {
*///?} else {
            if (state.getBlock() instanceof gregtech.block.energy.SignalWireBlock) {
//?}
                var hit = new net.minecraft.world.phys.BlockHitResult(context.getClickLocation(),
                        context.getClickedFace(), pos, context.isInside());
//? if >=1.21.1 {
/*                return target.useScrewdriver(state, level, pos, player, context.getHand(), hit);
*///?} else {
                return state.use(level, player, context.getHand(), hit);
//?}
            }
        }
        if (!toolName.equals("Wrench") || player == null || !player.isShiftKeyDown() || stack.getCount() != 1)
            return net.minecraft.world.InteractionResult.PASS;
        var level = context.getLevel();
        var pos = context.getClickedPos();
        if (!player.mayBuild() || !level.mayInteract(player, pos)) return net.minecraft.world.InteractionResult.FAIL;
        var state = level.getBlockState(pos);

//? if >=1.21.1 {
/*        var key = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
*///?} else {
        var key = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock());
//?}

        boolean ownBlock = key != null && key.getNamespace().equals("gt6ce");
        // Sneaking skips Block.use. Give the machine's existing tool handling first chance.
        if (ownBlock) {
            var hit = new net.minecraft.world.phys.BlockHitResult(context.getClickLocation(),
                    context.getClickedFace(), pos, context.isInside());

//? if >=1.21.1 {
/*            var itemResult=state.useItemOn(stack,level,player,context.getHand(),hit);
            var result=itemResult==net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION?state.useWithoutItem(level,player,hit):itemResult.result();
*///?} else {
            var result = state.use(level, player, context.getHand(), hit);
//?}

            if (result != net.minecraft.world.InteractionResult.PASS) return result;
        } else if (level.getBlockEntity(pos) != null) return net.minecraft.world.InteractionResult.PASS;
        if (!level.isClientSide) {

//? if >=1.21.1 {
/*            boolean mode=!monkeyWrenchMode(stack);gregtech.platform.neoforge.StackCustomData.update(stack,tag->tag.putBoolean("gt.monkey_wrench",mode));
*///?} else {
            stack.getOrCreateTag().putBoolean("gt.monkey_wrench", !monkeyWrenchMode(stack));
//?}

            player.getInventory().setChanged();
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(monkeyWrenchMode(stack)
                    ? "item.gt6ce.tool.monkey_wrench" : "item.gt6ce.tool.wrench"), true);
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }


//? if >=1.21.1 {
/*    public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, net.minecraft.world.entity.player.Player player) {
*///?} else {
    @Override public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, net.minecraft.world.entity.player.Player player) {
//?}

        return toolName.equals("Chainsaw")
                && gregtech.content.tool.ElectricChainsawHarvest.convertDrops(this,stack,pos,player)
                || gregtech.content.tool.ElectricUtilityHarvest.trim(this,stack,pos,player);
    }

    public boolean canInteract(ItemStack stack) {
        return interactionType(stack) != null && isPoweredUsable(stack);
    }

    public boolean isPoweredUsable(ItemStack stack) {
        return stack.getCount() == 1
                && !ElectricToolWear.broken(this, stack)
                && getEnergyStored(stack, GregTechTags.Energy.EU) > 0;
    }

    /** GT6 Behavior_Tool -> MultiItemTool.doDamage -> EnergyStat.useEnergy.
     * A successful click may consume the last partial charge; the next click is disabled. */
    public void consumeInteractionEnergy(ItemStack stack, long amount,
                                         @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity user) {
        if (amount <= 0 || !isPoweredUsable(stack)
                || user instanceof net.minecraft.world.entity.player.Player player && player.getAbilities().instabuild) return;
        long stored = getEnergyStored(stack, GregTechTags.Energy.EU);

//? if >=1.21.1 {
/*        gregtech.platform.neoforge.StackCustomData.update(stack,tag->tag.putLong("gt.charge",stored-Math.min(stored,amount)));
*///?} else {
        stack.getOrCreateTag().putLong("gt.charge", stored - Math.min(stored, amount));
//?}

        applyWear(stack, amount, user);
        if (user instanceof net.minecraft.world.entity.player.Player player) player.getInventory().setChanged();
    }

    private void applyWear(ItemStack stack, long amount, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity user) {
        var random = user == null ? net.minecraft.util.RandomSource.create() : user.getRandom();
        ElectricToolWear.apply(this, stack, amount, random::nextInt);
        if (user != null && !user.level().isClientSide) finishBrokenTool(stack, user);
    }

    private void finishBrokenTool(ItemStack stack, net.minecraft.world.entity.LivingEntity user) {
        if (stack.getCount() != 1 || !ElectricToolWear.broken(this, stack)
                || user instanceof net.minecraft.world.entity.player.Player player && player.getAbilities().instabuild) return;
        var scraps = gregtech.registry.GTItems.getStack(gregtech.data.MaterialPrefix.scrapGt,
                headMaterial(stack), 1 + user.getRandom().nextInt(ElectricToolWear.scrapRandomBound(this)));
        stack.shrink(1);
        if (!scraps.isEmpty()) {
            if (user instanceof net.minecraft.world.entity.player.Player player) {
                if (!player.addItem(scraps)) player.drop(scraps, false);
                player.getInventory().setChanged();
            } else user.spawnAtLocation(scraps);
        }
    }

    @Override public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity,
                                         int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);

//? if >=1.21.1 {
/*        if(!level.isClientSide) gregtech.internal.tool.GTToolEnchantments.apply(stack,level.registryAccess());
*///?} else {
        if(!level.isClientSide) gregtech.internal.tool.GTToolEnchantments.apply(stack);
//?}

        if (!level.isClientSide && entity instanceof net.minecraft.world.entity.LivingEntity user) finishBrokenTool(stack, user);
    }

    /** GT6 Loader_Tools constructs an uncharged tool, with capacity inherited from its battery. */
    public ItemStack assembled(gregtech.internal.material.GTMaterial material, long batteryCapacity) {
        if (!gregtech.content.tool.ElectricToolAssembly.validMaterial(material) || batteryCapacity<=0)
            throw new IllegalArgumentException("Invalid electric tool material/capacity");

//? if >=1.21.1 {
/*        var result=new ItemStack(this);
        gregtech.platform.neoforge.StackCustomData.update(result,tag->{var stats=new CompoundTag();stats.putString("head",material.getName());stats.putString("handle",definition().handleMaterial());tag.put("GT.ToolStats",stats);tag.putLong("gt.capacity",batteryCapacity);});
        result.remove(net.minecraft.core.component.DataComponents.MAX_DAMAGE);result.remove(net.minecraft.core.component.DataComponents.DAMAGE);
*///?} else {
        var result=gregtech.internal.tool.GTToolHelper.write(new ItemStack(this),material,
                gregtech.internal.material.GTMaterialRegistry.get(definition().handleMaterial()));
        result.getOrCreateTag().putLong("gt.capacity",batteryCapacity);
//?}

        return result;
    }

    public gregtech.internal.material.GTMaterial headMaterial(ItemStack stack) {
        var head=gregtech.internal.tool.GTToolHelper.getHead(stack);
        return head!=null && head.isValid() ? head : gregtech.content.material.Materials.Steel;
    }

    public int tint(ItemStack stack,int layer) {
        if(layer==1) return headMaterial(stack).getColor();
        if(layer==0) {
            var handle=gregtech.internal.tool.GTToolHelper.getHandle(stack);
            return handle!=null && handle.isValid()?handle.getColor():gregtech.internal.material.GTMaterialRegistry.get(definition().handleMaterial()).getColor();
        }
        return 0xFFFFFF;
    }

    @Override public net.minecraft.network.chat.Component getName(ItemStack stack) {

//? if >=1.21.1 {
/*        return gregtech.platform.neoforge.StackCustomData.read(stack).contains("GT.ToolStats")
*///?} else {
        return stack.hasTag() && stack.getTag().contains("GT.ToolStats")
//?}

                ? net.minecraft.network.chat.Component.translatable("item.gt6ce.electric_tool.named",
                    MaterialPresentation.name(headMaterial(stack)),super.getName(stack)) : super.getName(stack);
    }

    public boolean hasEnergyForUse(ItemStack stack) {
        return stack.getCount() == 1 && !ElectricToolWear.broken(this, stack)
                && getEnergyStored(stack, GregTechTags.Energy.EU) >= energyPerUse;
    }

    @Override public net.minecraft.world.InteractionResult onItemUseFirst(ItemStack stack,
            net.minecraft.world.item.context.UseOnContext context) {
        if(toolName.equals("Mixer")&&context.getClickedFace()==net.minecraft.core.Direction.UP&&context.getLevel().getBlockEntity(context.getClickedPos()) instanceof gregtech.blockentity.tool.ProcessingToolBlockEntity bowl){if(!context.getLevel().isClientSide&&bowl.processMixer(context.getPlayer(),stack))return net.minecraft.world.InteractionResult.SUCCESS;return net.minecraft.world.InteractionResult.PASS;}
        return toolName.equals("Drill")
                ? gregtech.item.behavior.BehaviorPlaceDynamite.use(this, stack, context)
                : net.minecraft.world.InteractionResult.PASS;
    }


//? if >=1.21.1 {
/*    @Override public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
*///?} else {
    @Override public void appendHoverText(ItemStack stack, @javax.annotation.Nullable Level level,
//?}

            java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {

//? if >=1.21.1 {
/*        super.appendHoverText(stack, context, tooltip, flag);
*///?} else {
        super.appendHoverText(stack, level, tooltip, flag);
//?}

        tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.gt6ce.electric_tool.energy",
                getEnergyStored(stack,GregTechTags.Energy.EU),getEnergyCapacity(stack,GregTechTags.Energy.EU)));
        tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.gt6ce.electric_tool.tier",definition().tierName().toUpperCase(java.util.Locale.ROOT),definition().voltage()));
        tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.gt6ce.electric_tool.use."+toolName.replace(" ","_").toLowerCase(java.util.Locale.ROOT)));
        long maximum = ElectricToolWear.maximum(this, stack);
        tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.gt6ce.tool_durability",
                Math.max(0, maximum - ElectricToolWear.damage(stack)), maximum));
        if (toolName.equals("Wrench")) {
            tooltip.add(net.minecraft.network.chat.Component.translatable(monkeyWrenchMode(stack)
                    ? "item.gt6ce.tool.monkey_wrench" : "item.gt6ce.tool.wrench"));
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.gt6ce.electric_wrench.switch"));
        }
        if (toolName.equals("Drill")) tooltip.add(net.minecraft.network.chat.Component.translatable("gt.behaviour.placedynamite"));
    }

    public boolean consumeEnergy(ItemStack stack) {
        return consumeEnergy(stack, null);
    }

    public boolean consumeEnergy(ItemStack stack, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity user) {
        if (user instanceof net.minecraft.world.entity.player.Player player && player.getAbilities().instabuild) return true;
        long stored = getEnergyStored(stack, GregTechTags.Energy.EU);
        if (!hasEnergyForUse(stack)) return false;

//? if >=1.21.1 {
/*        gregtech.platform.neoforge.StackCustomData.update(stack,tag->tag.putLong("gt.charge",stored-energyPerUse));
*///?} else {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putLong("gt.charge", stored - energyPerUse);
//?}

        applyWear(stack, energyPerUse, user);
        return true;
    }

    @Override public boolean hurtEnemy(ItemStack stack,net.minecraft.world.entity.LivingEntity target,net.minecraft.world.entity.LivingEntity attacker){if(isPoweredUsable(stack))consumeInteractionEnergy(stack,definition().attackCost(),attacker);return true;}

//? if >=1.21.1 {
/*    @Override public net.minecraft.world.item.component.ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack){if(!isPoweredUsable(stack))return net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY;return net.minecraft.world.item.component.ItemAttributeModifiers.builder().add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,new net.minecraft.world.entity.ai.attributes.AttributeModifier(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6ce","powered_tool_damage"),definition().damage()+headMaterial(stack).getToolQuality(),net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND).build();}
*///?} else {
    @Override public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute,net.minecraft.world.entity.ai.attributes.AttributeModifier> getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot slot,ItemStack stack){if(slot!=net.minecraft.world.entity.EquipmentSlot.MAINHAND||!isPoweredUsable(stack))return com.google.common.collect.ImmutableMultimap.of();return com.google.common.collect.ImmutableMultimap.of(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,new net.minecraft.world.entity.ai.attributes.AttributeModifier(java.util.UUID.fromString("CB3F55D3-645C-4FBC-A695-84CC934974"),"GT powered tool damage",definition().damage()+headMaterial(stack).getToolQuality(),net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));}
//?}


    // ── IItemEnergy ──────────────────────────────────────────────────────────

    @Override
    public boolean isEnergyType(ItemStack stack, GregTechTags.Tag energyType) { return energyType == GregTechTags.Energy.EU; }

    @Override
    public long getEnergyCapacity(ItemStack stack, GregTechTags.Tag energyType) {
        if(energyType!=GregTechTags.Energy.EU) return 0;

//? if >=1.21.1 {
/*        long configured=gregtech.platform.neoforge.StackCustomData.read(stack).getLong("gt.capacity");
*///?} else {
        long configured=stack.hasTag()?stack.getTag().getLong("gt.capacity"):0;
//?}

        return configured>0?configured:capacity;
    }

    @Override
    public long getEnergyStored(ItemStack stack, GregTechTags.Tag energyType) {
        if (energyType != GregTechTags.Energy.EU) return 0;

//? if >=1.21.1 {
/*        CompoundTag tag = gregtech.platform.neoforge.StackCustomData.read(stack);
*///?} else {
        CompoundTag tag = stack.getTag();
//?}

        return tag != null ? Math.max(0,Math.min(tag.getLong("gt.charge"),getEnergyCapacity(stack,energyType))) : 0;
    }

    @Override public boolean canEnergyInjection(ItemStack stack, GregTechTags.Tag type, long size) {
        long voltage = 8L << (2 * tier);
        return stack.getCount() == 1 && isEnergyType(stack,type) && size >= voltage / 2 && size <= voltage * 2;
    }
    @Override public boolean canEnergyExtraction(ItemStack stack, GregTechTags.Tag type, long size) { return false; }

    @Override
    public long doEnergyInjection(GregTechTags.Tag energyType, ItemStack stack, long size, long amount,
                                   Level level, BlockPos pos, boolean doInject) {
        if (amount <= 0 || size == Long.MIN_VALUE || !canEnergyInjection(stack,energyType,Math.abs(size))) return 0;
        size = Math.abs(size);
        long stored=getEnergyStored(stack,energyType);
        long space = getEnergyCapacity(stack,energyType) - stored;
        if (space <= 0) return 0;
        long accepted = Math.min(Math.min(64, amount), Math.max(1, space / size));
        if (doInject && accepted > 0) {

//? if >=1.21.1 {
/*            long charge=stored+Math.min(space,accepted*size);gregtech.platform.neoforge.StackCustomData.update(stack,tag->tag.putLong("gt.charge",charge));
*///?} else {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putLong("gt.charge", stored + Math.min(space, accepted * size));
//?}

        }
        return accepted;
    }

    @Override
    public long doEnergyExtraction(GregTechTags.Tag energyType, ItemStack stack, long size, long amount,
                                    Level level, BlockPos pos, boolean doExtract) {
        // GT6 EnergyStat.makeTool has mCanDecharge=false; use consumes energy internally.
        return 0;
    }

    // ── Tooltip / Durability bar ─────────────────────────────────────────────

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getEnergyStored(stack, GregTechTags.Energy.EU) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        long stored = getEnergyStored(stack, GregTechTags.Energy.EU);
        long actualCapacity=getEnergyCapacity(stack,GregTechTags.Energy.EU);
        return actualCapacity > 0 ? (int) (stored * 13.0 / actualCapacity) : 0;
    }

    @Override
    public int getBarColor(ItemStack stack) { return 0xFFFF00; }

    @Override
    public int getMaxDamage(ItemStack stack) { return 0; }

    /** Material wear lives in GT.ToolStats.k; TieredItem's vanilla durability flag must not leak. */
    @Override public boolean isDamageable(ItemStack stack) { return false; }

    @Override
    public boolean isDamaged(ItemStack stack) { return false; }
}
