package com.gregtech.gregtech.item;
import com.gregtech.gregtech.api.material.MaterialPresentation;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.client.GTToolTooltips;
import com.gregtech.gregtech.platform.neoforge.smeltery.SmeltingCrucibleEntity;
import com.gregtech.gregtech.registry.GTToolItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** One registered item per {@link GTToolType} (GT6 {@code gt.metatool.01} split by tool kind). */
public class GTToolItem extends Item {

    private final GTToolType toolType;

    public GTToolItem(Properties properties, GTToolType toolType) {
        super(properties);
        this.toolType = toolType;
    }

    public GTToolType toolType() {
        return toolType;
    }

    /** GT6 crucible shovel scrape — runs on server via {@code onItemUseFirst} before {@code Block#use}. */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        // Original Behavior_Tool takes priority over the ordinary builder plane copy.
        var multiblock = com.gregtech.gregtech.api.multiblock.MultiblockTools.use(context);
        if (multiblock.consumesAction()) return multiblock;
        // §108: GT6's TOOL_plunger and TOOL_igniter clicks are onItemUseFirst behaviours as well, so
        // they run before Block#use (Behavior_Plunger_Fluid:49-62, Behavior_FlintAndTinder:45-61).
        if (!context.getLevel().isClientSide()) {
            BlockPos clicked = context.getClickedPos();
            float hx = (float) (context.getClickLocation().x - clicked.getX());
            float hy = (float) (context.getClickLocation().y - clicked.getY());
            float hz = (float) (context.getClickLocation().z - clicked.getZ());
            if (toolType == GTToolType.PLUNGER
                    && com.gregtech.gregtech.item.behavior.BehaviorPlungerFluid.plungeFluid(
                            context.getLevel(), clicked, context.getPlayer(), stack,
                            com.gregtech.gregtech.item.behavior.BehaviorPlungerFluid.COSTS)) {
                return InteractionResult.SUCCESS;
            }
            if (toolType == GTToolType.FLINT_AND_TINDER
                    && com.gregtech.gregtech.item.behavior.BehaviorFlintAndTinder.useOn(
                            context.getLevel(), clicked, context.getClickedFace(), context.getPlayer(),
                            stack,
                            com.gregtech.gregtech.item.behavior.BehaviorFlintAndTinder.DEFAULT_FLINT_CHANCE,
                            context.getLevel().getRandom(), hx, hy, hz).acted()) {
                return InteractionResult.SUCCESS;
            }
            // §110: the builder's wand is a tool, not a multi-item, so its click lands here as well
            // (GT6 hangs Behavior_Builderwand on TOOL_builderwand, GT_Tool_Builderwand:62).
            if (toolType == GTToolType.BUILDER_WAND
                    && com.gregtech.gregtech.item.behavior.BehaviorBuilderWand.useOn(
                            context.getLevel(), clicked, context.getClickedFace(), context.getPlayer(),
                            stack, hx, hy, hz).acted()) {
                return InteractionResult.SUCCESS;
            }
        }
        if (!isCrucibleShovelType()) {
            return InteractionResult.PASS;
        }
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }
        BlockPos pos = context.getClickedPos();
        if (!(context.getLevel().getBlockEntity(pos) instanceof SmeltingCrucibleEntity crucible)) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide()) {
            return crucible.canHandleUse(context.getPlayer(), context.getHand())
                    ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        BlockHitResult hit = new BlockHitResult(
                context.getClickLocation(), context.getClickedFace(), pos, context.isInside());
        InteractionResult result = crucible.tryUse(context.getPlayer(), context.getHand(), hit);
        return result != InteractionResult.PASS ? result : InteractionResult.PASS;
    }

    /** Screwdriver shift-right-click: client skips Block.use when sneaking, so delegate back via useOn. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (toolType == GTToolType.AXE || toolType == GTToolType.DOUBLE_AXE || toolType == GTToolType.SAW) {
            return GTToolHelper.isUsable(context.getItemInHand())
                    ? com.gregtech.gregtech.item.behavior.BehaviorPlaceWoodworkingSupplies.use(context)
                    : InteractionResult.PASS;
        }
        if (toolType != GTToolType.SCREWDRIVER) {
            return InteractionResult.PASS;
        }
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (state.getBlock() instanceof com.gregtech.gregtech.api.tool.ScrewdriverUseTarget) {
            Player player = context.getPlayer();
            InteractionHand hand = context.getHand();
            BlockHitResult hit = new BlockHitResult(
                    context.getClickLocation(), context.getClickedFace(),
                    context.getClickedPos(), context.isInside());
            return ((com.gregtech.gregtech.api.tool.ScrewdriverUseTarget)state.getBlock()).useScrewdriver(state,context.getLevel(),context.getClickedPos(),player,hand,hit);
        }
        return InteractionResult.PASS;
    }

    private boolean isCrucibleShovelType() {
        return toolType == GTToolType.SHOVEL
                || toolType == GTToolType.SPADE
                || toolType == GTToolType.UNIVERSAL_SPADE;
    }

    public static ItemStack create(GTToolType type, GTMaterial head, GTMaterial handle) {
        ItemStack stack = GTToolItems.empty(type);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return GTToolHelper.write(stack, head, handle);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (!level.isClientSide) com.gregtech.gregtech.api.tool.GTToolEnchantments.apply(stack, level.registryAccess());
        super.inventoryTick(stack, level, entity, slot, selected);
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return GTToolHelper.getMaxDurability(stack);
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return GTToolHelper.isTool(stack);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (GTToolHelper.isMachineHarvestTool(stack, state)) {
            return GTToolHelper.getMachineHarvestSpeed(stack);
        }
        if (!GTToolHelper.isUsable(stack)) {
            return 1.0F;
        }
        float speed = GTToolHelper.getMiningSpeed(stack, state);
        return speed > 0.0F ? speed : 1.0F;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return GTToolHelper.isCorrectToolForDrops(stack, state);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        if (GTToolHelper.isUsable(stack))
            com.gregtech.gregtech.item.behavior.ToolPlantHarvest.harvest(GTToolHelper.getType(stack), stack, level, state, pos);
        if (!level.isClientSide && !com.gregtech.gregtech.item.behavior.AxeColumnHarvest.harvesting()) {
            if (state.getDestroySpeed(level, pos) != 0.0F) GTToolHelper.damageForBlockBreak(stack, state, entity);
            if (entity instanceof net.minecraft.server.level.ServerPlayer player)
                com.gregtech.gregtech.item.behavior.AxeColumnHarvest.harvest(player, stack, state, pos);
        }
        return true;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // §108: GT6's onLeftClickEntity creeper branch (Behavior_FlintAndTinder:69-78) - a flint and
        // tinder lights a creeper instead of just hitting it.
        if (toolType == GTToolType.FLINT_AND_TINDER && attacker instanceof Player player
                && com.gregtech.gregtech.item.behavior.BehaviorFlintAndTinder.igniteCreeper(
                        target, stack, player)) {
            return true;
        }
        GTToolHelper.damageForEntityAttack(stack, attacker);
        return true;
    }

    @Override public net.minecraft.world.item.component.ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack){
        if(!GTToolHelper.isUsable(stack))return net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY;
        return net.minecraft.world.item.component.ItemAttributeModifiers.builder()
          .add(Attributes.ATTACK_DAMAGE,new AttributeModifier(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","tool_attack_damage"),GTToolHelper.getCombatDamage(stack),AttributeModifier.Operation.ADD_VALUE),net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
          .add(Attributes.ATTACK_SPEED,new AttributeModifier(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gregtech","tool_attack_speed"),toolType.attackSpeed()-4.0F,AttributeModifier.Operation.ADD_VALUE),net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND).build().withTooltip(false);
    }
    @Override
    public String getDescriptionId() {
        return "item." + "gregtech" + ".tool." + toolType.id();
    }

    @Override
    public Component getName(ItemStack stack) {
        if (!GTToolHelper.isTool(stack)) {
            return super.getName(stack);
        }
        GTMaterial head = GTToolHelper.getHead(stack);
        return Component.translatable("item." + "gregtech" + ".tool." + toolType.id() + ".named",
                MaterialPresentation.name(head), Component.translatable(toolType.translationKey()));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (GTToolHelper.isTool(stack)) {
            GTToolTooltips.append(stack, tooltip, flag);
        }
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return false;
    }
}
