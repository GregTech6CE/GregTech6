package com.gregtech.gregtech.item;
import com.gregtech.gregtech.api.material.MaterialPresentation;

import com.gregtech.gregtech.api.energy.item.IItemEnergy;
import com.gregtech.gregtech.data.GregTechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.Level;
import com.gregtech.gregtech.content.tool.ElectricToolWear;

/** GT6 electric tool base — stores EU and consumes power on use. */
public class ElectricToolItem extends TieredItem implements IItemEnergy {
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

    public String toolName() { return toolName; }
    public int energyTier() { return tier; }

    @Override public float getDestroySpeed(ItemStack stack, net.minecraft.world.level.block.state.BlockState state) {
        if (com.gregtech.gregtech.content.tool.ElectricWrenchHarvest.target(state) && toolName.equals("Wrench"))
            return com.gregtech.gregtech.content.tool.ElectricWrenchHarvest.canHarvest(this,stack,state)
                    ? com.gregtech.gregtech.content.tool.ElectricWrenchHarvest.speed(this,stack) : 0;
        if (toolName.equals("Chainsaw") && com.gregtech.gregtech.content.tool.ElectricChainsawHarvest.target(state))
            return com.gregtech.gregtech.content.tool.ElectricChainsawHarvest.canHarvest(this,stack,state)
                    ? com.gregtech.gregtech.content.tool.ElectricChainsawHarvest.speed(this,stack) : 0;
        return super.getDestroySpeed(stack,state);
    }

    @Override public boolean isCorrectToolForDrops(ItemStack stack, net.minecraft.world.level.block.state.BlockState state) {
        return com.gregtech.gregtech.content.tool.ElectricWrenchHarvest.canHarvest(this,stack,state)
                || com.gregtech.gregtech.content.tool.ElectricChainsawHarvest.canHarvest(this,stack,state);
    }

    @Override public boolean mineBlock(ItemStack stack, Level level, net.minecraft.world.level.block.state.BlockState state,
                                       BlockPos pos, net.minecraft.world.entity.LivingEntity user) {
        if (!level.isClientSide && (toolName.equals("Wrench") || toolName.equals("Chainsaw")) && isPoweredUsable(stack)) {
            float hardness=state.getDestroySpeed(level,pos);
            if (hardness>0) consumeInteractionEnergy(stack,(long)Math.ceil(50F*hardness),user);
        }
        return true;
    }

    /** Only the two currently implemented machine-click tool identities. Drill is not a pickaxe. */
    @javax.annotation.Nullable
    public com.gregtech.gregtech.api.tool.GTToolType interactionType(ItemStack stack) {
        return switch (toolName) {
            case "Wrench" -> monkeyWrenchMode(stack) ? com.gregtech.gregtech.api.tool.GTToolType.MONKEY_WRENCH
                    : com.gregtech.gregtech.api.tool.GTToolType.WRENCH;
            case "Screwdriver" -> com.gregtech.gregtech.api.tool.GTToolType.SCREWDRIVER;
            default -> null;
        };
    }

    public boolean monkeyWrenchMode(ItemStack stack) {
        return toolName.equals("Wrench") && stack.hasTag() && stack.getTag().getBoolean("gt.monkey_wrench");
    }

    /** GT6 switch behavior follows the tool click and ignores foreign block entities. */
    @Override public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        var player = context.getPlayer();
        var stack = context.getItemInHand();
        if (toolName.equals("Chainsaw")) {
            boolean usable=stack.getCount()==1 && !ElectricToolWear.broken(this,stack)
                    && (isPoweredUsable(stack)||player!=null&&player.getAbilities().instabuild);
            return usable ? com.gregtech.gregtech.item.behavior.BehaviorPlaceWoodworkingSupplies.use(context)
                    : net.minecraft.world.InteractionResult.PASS;
        }
        if (!toolName.equals("Wrench") || player == null || !player.isShiftKeyDown() || stack.getCount() != 1)
            return net.minecraft.world.InteractionResult.PASS;
        var level = context.getLevel();
        var pos = context.getClickedPos();
        if (!player.mayBuild() || !level.mayInteract(player, pos)) return net.minecraft.world.InteractionResult.FAIL;
        var state = level.getBlockState(pos);
        var key = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock());
        boolean ownBlock = key != null && key.getNamespace().equals("gregtech");
        // Sneaking skips Block.use. Give the machine's existing tool handling first chance.
        if (ownBlock) {
            var hit = new net.minecraft.world.phys.BlockHitResult(context.getClickLocation(),
                    context.getClickedFace(), pos, context.isInside());
            var result = state.use(level, player, context.getHand(), hit);
            if (result != net.minecraft.world.InteractionResult.PASS) return result;
        } else if (level.getBlockEntity(pos) != null) return net.minecraft.world.InteractionResult.PASS;
        if (!level.isClientSide) {
            stack.getOrCreateTag().putBoolean("gt.monkey_wrench", !monkeyWrenchMode(stack));
            player.getInventory().setChanged();
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(monkeyWrenchMode(stack)
                    ? "item.gregtech.tool.monkey_wrench" : "item.gregtech.tool.wrench"), true);
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        return toolName.equals("Chainsaw")
                && com.gregtech.gregtech.content.tool.ElectricChainsawHarvest.convertDrops(this,stack,pos,player);
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
        stack.getOrCreateTag().putLong("gt.charge", stored - Math.min(stored, amount));
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
        var scraps = com.gregtech.gregtech.registry.GTItems.getStack(com.gregtech.gregtech.data.MaterialPrefix.scrapGt,
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
        if (!level.isClientSide && entity instanceof net.minecraft.world.entity.LivingEntity user) finishBrokenTool(stack, user);
    }

    /** GT6 Loader_Tools constructs an uncharged tool, with capacity inherited from its battery. */
    public ItemStack assembled(com.gregtech.gregtech.api.material.GTMaterial material, long batteryCapacity) {
        if (!com.gregtech.gregtech.content.tool.ElectricToolAssembly.validMaterial(material) || batteryCapacity<=0)
            throw new IllegalArgumentException("Invalid electric tool material/capacity");
        var result=com.gregtech.gregtech.api.tool.GTToolHelper.write(new ItemStack(this),material,
                com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Orange"));
        result.getOrCreateTag().putLong("gt.capacity",batteryCapacity);
        return result;
    }

    public com.gregtech.gregtech.api.material.GTMaterial headMaterial(ItemStack stack) {
        var head=com.gregtech.gregtech.api.tool.GTToolHelper.getHead(stack);
        return head!=null && head.isValid() ? head : com.gregtech.gregtech.content.material.Materials.Steel;
    }

    public int tint(ItemStack stack,int layer) {
        if(layer==2) return headMaterial(stack).getColor();
        if(layer==0) {
            var handle=com.gregtech.gregtech.api.tool.GTToolHelper.getHandle(stack);
            return handle!=null && handle.isValid()?handle.getColor():com.gregtech.gregtech.api.material.GTMaterialRegistry.get("Orange").getColor();
        }
        return 0xFFFFFF;
    }

    @Override public net.minecraft.network.chat.Component getName(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("GT.ToolStats")
                ? net.minecraft.network.chat.Component.translatable("item.gregtech.electric_tool.named",
                    MaterialPresentation.name(headMaterial(stack)),super.getName(stack)) : super.getName(stack);
    }

    public boolean hasEnergyForUse(ItemStack stack) {
        return stack.getCount() == 1 && !ElectricToolWear.broken(this, stack)
                && getEnergyStored(stack, GregTechTags.Energy.EU) >= energyPerUse;
    }

    @Override public net.minecraft.world.InteractionResult onItemUseFirst(ItemStack stack,
            net.minecraft.world.item.context.UseOnContext context) {
        return toolName.equals("Drill")
                ? com.gregtech.gregtech.item.behavior.BehaviorPlaceDynamite.use(this, stack, context)
                : net.minecraft.world.InteractionResult.PASS;
    }

    @Override public void appendHoverText(ItemStack stack, @javax.annotation.Nullable Level level,
            java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.gregtech.electric_tool.energy",
                getEnergyStored(stack,GregTechTags.Energy.EU),getEnergyCapacity(stack,GregTechTags.Energy.EU)));
        long maximum = ElectricToolWear.maximum(this, stack);
        tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.gregtech.tool_durability",
                Math.max(0, maximum - ElectricToolWear.damage(stack)), maximum));
        if (toolName.equals("Wrench")) {
            tooltip.add(net.minecraft.network.chat.Component.translatable(monkeyWrenchMode(stack)
                    ? "item.gregtech.tool.monkey_wrench" : "item.gregtech.tool.wrench"));
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.gregtech.electric_wrench.switch"));
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
        CompoundTag tag = stack.getOrCreateTag();
        tag.putLong("gt.charge", stored - energyPerUse);
        applyWear(stack, energyPerUse, user);
        return true;
    }

    // ── IItemEnergy ──────────────────────────────────────────────────────────

    @Override
    public boolean isEnergyType(ItemStack stack, GregTechTags.Tag energyType) { return energyType == GregTechTags.Energy.EU; }

    @Override
    public long getEnergyCapacity(ItemStack stack, GregTechTags.Tag energyType) {
        if(energyType!=GregTechTags.Energy.EU) return 0;
        long configured=stack.hasTag()?stack.getTag().getLong("gt.capacity"):0;
        return configured>0?configured:capacity;
    }

    @Override
    public long getEnergyStored(ItemStack stack, GregTechTags.Tag energyType) {
        if (energyType != GregTechTags.Energy.EU) return 0;
        CompoundTag tag = stack.getTag();
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
            CompoundTag tag = stack.getOrCreateTag();
            tag.putLong("gt.charge", stored + Math.min(space, accepted * size));
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
