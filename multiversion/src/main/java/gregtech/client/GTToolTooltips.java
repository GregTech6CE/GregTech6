package gregtech.client;
import gregtech.internal.material.MaterialPresentation;

//? if >=1.21.1 {
//?} else {
import gregtech.GregTech;
//?}
import gregtech.internal.material.GTMaterial;
import gregtech.internal.tool.GTToolHelper;
import gregtech.internal.tool.GTToolType;
import gregtech.util.GTCodeFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

/** GT6-style meta-tool tooltip lines ({@code MultiItemTool#addAdditionalToolTips}). */
public final class GTToolTooltips {
    private GTToolTooltips() {}

    public static void append(ItemStack stack, List<Component> tooltip, TooltipFlag flag) {
        if (!GTToolHelper.isTool(stack)) {
            return;
        }
        GTToolType type = GTToolHelper.getType(stack);
        GTToolType statsType = stack.getItem() instanceof gregtech.content.item.PocketToolItem pocket ? pocket.toolType() : type;
        GTMaterial head = GTToolHelper.getHead(stack);
        if (!head.isValid()) {
            return;
        }

        long max = GTToolHelper.getMaxDurability(stack);
        long damage = stack.getDamageValue();
        long remaining = Math.max(0, max - damage);

//? if >=1.21.1 {
        /*tooltip.add(Component.translatable("tooltip." + "gt6ce" + ".tool_durability",
*///?} else {
        tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".tool_durability",
//?}
                        Component.literal(GTCodeFormat.makeString(remaining)).withStyle(ChatFormatting.GREEN),
                        Component.literal(GTCodeFormat.makeString(max)).withStyle(ChatFormatting.GREEN))
                .withStyle(ChatFormatting.WHITE));

        int level = statsType.baseQuality() + head.getToolQuality();
        tooltip.add(Component.empty()
                .append(MaterialPresentation.name(head))
                .append(Component.translatable("tooltip.gt6ce.ui.level", level).withStyle(ChatFormatting.YELLOW)));

        float combat = statsType.baseDamage() + head.getToolQuality();
        float hearts = (combat + 1.0F) / 2.0F;
//? if >=1.21.1 {
        /*tooltip.add(Component.translatable("tooltip." + "gt6ce" + ".tool_melee_damage",
*///?} else {
        tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".tool_melee_damage",
//?}
                        Component.literal(String.format(Locale.ROOT, "+%.1f", combat)).withStyle(ChatFormatting.BLUE),
                        Component.literal(String.format(Locale.ROOT, "(= %.1f Hearts)", hearts)).withStyle(ChatFormatting.RED))
                .withStyle(ChatFormatting.WHITE));

        float attackSpeed = statsType.attackSpeed();
//? if >=1.21.1 {
        /*tooltip.add(Component.translatable("tooltip." + "gt6ce" + ".tool_attack_speed",
*///?} else {
        tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".tool_attack_speed",
//?}
                        Component.literal(String.format(Locale.ROOT, "%.1f", attackSpeed)).withStyle(ChatFormatting.GREEN))
                .withStyle(ChatFormatting.WHITE));

        float miningSpeed = Math.max(Float.MIN_NORMAL, statsType.speedMultiplier() * head.getToolSpeed());
//? if >=1.21.1 {
        /*tooltip.add(Component.translatable("tooltip." + "gt6ce" + ".tool_mining_speed",
*///?} else {
        tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".tool_mining_speed",
//?}
                        Component.literal(String.format(Locale.ROOT, "%.1f", miningSpeed)).withStyle(ChatFormatting.LIGHT_PURPLE))
                .withStyle(ChatFormatting.WHITE));

        long craftingUses = GTCodeFormat.divUp(remaining, statsType.damagePerCraft());
//? if >=1.21.1 {
        /*tooltip.add(Component.translatable("tooltip." + "gt6ce" + ".tool_crafting_uses",
*///?} else {
        tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".tool_crafting_uses",
//?}
                        Component.literal(GTCodeFormat.makeString(craftingUses)).withStyle(ChatFormatting.GREEN))
                .withStyle(ChatFormatting.WHITE));

        if (type.canPenetrate()) {
//? if >=1.21.1 {
            /*tooltip.add(Component.translatable("tooltip." + "gt6ce" + ".tool_penetrate_armor")
*///?} else {
            tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".tool_penetrate_armor")
//?}
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (type.canCollect()) {
//? if >=1.21.1 {
            /*tooltip.add(Component.translatable("tooltip." + "gt6ce" + ".tool_autocollect")
*///?} else {
            tooltip.add(Component.translatable("tooltip." + GregTech.NAMESPACE + ".tool_autocollect")
//?}
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (type.tooltipKey() != null) {
//? if >=1.21.1 {
            /*var hint = Component.translatable("tooltip." + "gt6ce" + ".tool_hint." + type.tooltipKey());
*///?} else {
            var hint = Component.translatable("tooltip." + GregTech.NAMESPACE + ".tool_hint." + type.tooltipKey());
//?}
            // The original Chinese patch intentionally leaves some tool descriptions empty.
            if (!hint.getString().isEmpty()) tooltip.add(hint.withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
