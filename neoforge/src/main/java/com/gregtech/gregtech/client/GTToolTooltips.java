package com.gregtech.gregtech.client;
import com.gregtech.gregtech.api.material.MaterialPresentation;

import com.gregtech.gregtech.api.material.GTMaterial;
import com.gregtech.gregtech.api.tool.GTToolHelper;
import com.gregtech.gregtech.api.tool.GTToolType;
import com.gregtech.gregtech.util.GTCodeFormat;
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
        GTMaterial head = GTToolHelper.getHead(stack);
        if (!head.isValid()) {
            return;
        }

        long max = GTToolHelper.getMaxDurability(stack);
        long damage = stack.getDamageValue();
        long remaining = Math.max(0, max - damage);

        tooltip.add(Component.translatable("tooltip." + "gregtech" + ".tool_durability",
                        Component.literal(GTCodeFormat.makeString(remaining)).withStyle(ChatFormatting.GREEN),
                        Component.literal(GTCodeFormat.makeString(max)).withStyle(ChatFormatting.GREEN))
                .withStyle(ChatFormatting.WHITE));

        int level = type.baseQuality() + head.getToolQuality();
        tooltip.add(Component.empty()
                .append(MaterialPresentation.name(head))
                .append(Component.literal(" Level: " + level).withStyle(ChatFormatting.YELLOW)));

        float combat = type.baseDamage() + head.getToolQuality();
        float hearts = (combat + 1.0F) / 2.0F;
        tooltip.add(Component.translatable("tooltip." + "gregtech" + ".tool_melee_damage",
                        Component.literal(String.format(Locale.ROOT, "+%.1f", combat)).withStyle(ChatFormatting.BLUE),
                        Component.literal(String.format(Locale.ROOT, "(= %.1f Hearts)", hearts)).withStyle(ChatFormatting.RED))
                .withStyle(ChatFormatting.WHITE));

        float attackSpeed = type.attackSpeed();
        tooltip.add(Component.translatable("tooltip." + "gregtech" + ".tool_attack_speed",
                        Component.literal(String.format(Locale.ROOT, "%.1f", attackSpeed)).withStyle(ChatFormatting.GREEN))
                .withStyle(ChatFormatting.WHITE));

        float miningSpeed = Math.max(Float.MIN_NORMAL, type.speedMultiplier() * head.getToolSpeed());
        tooltip.add(Component.translatable("tooltip." + "gregtech" + ".tool_mining_speed",
                        Component.literal(String.format(Locale.ROOT, "%.1f", miningSpeed)).withStyle(ChatFormatting.LIGHT_PURPLE))
                .withStyle(ChatFormatting.WHITE));

        long craftingUses = GTCodeFormat.divUp(remaining, type.damagePerCraft());
        tooltip.add(Component.translatable("tooltip." + "gregtech" + ".tool_crafting_uses",
                        Component.literal(GTCodeFormat.makeString(craftingUses)).withStyle(ChatFormatting.GREEN))
                .withStyle(ChatFormatting.WHITE));

        if (type.canPenetrate()) {
            tooltip.add(Component.translatable("tooltip." + "gregtech" + ".tool_penetrate_armor")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (type.canCollect()) {
            tooltip.add(Component.translatable("tooltip." + "gregtech" + ".tool_autocollect")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (type.tooltipKey() != null) {
            tooltip.add(Component.translatable("tooltip." + "gregtech" + ".tool_hint." + type.tooltipKey())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
