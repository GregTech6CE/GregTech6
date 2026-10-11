/* Copyright GregTech-6 Team / Gregorius Techneticies; LGPL-3.0-or-later.
 * MultiTileEntityMold/Basin/Faucet/Crossing.addToolTips, with native saved item shape data. */
package com.gregtech.gregtech.client;

import com.gregtech.gregtech.api.machine.CrucibleSpec;
import com.gregtech.gregtech.api.machine.crucible.MoldShapes;
import com.gregtech.gregtech.block.machine.MoldItemData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public final class SmelteryCompanionTooltips {
    private SmelteryCompanionTooltips() {}
    public static void appendMold(ItemStack stack, CrucibleSpec spec, List<Component> tooltip) {
        int shape = MoldItemData.shape(stack);
        if (shape == 0) tooltip.add(Component.translatable("gt.lang.recipes.mold.select").withStyle(ChatFormatting.AQUA));
        else {
            var recipe = MoldShapes.recipe(shape);
            String prefix = MoldShapes.sourcePrefixName(recipe);
            tooltip.add(Component.translatable("gt.lang.recipes.mold").append(" ")
                    .append(Component.translatable("oredict.prefix." + prefix))
                    .append(Component.literal(MaterialTooltips.displayUnits(MoldShapes.requiredMaterialUnits(shape)) + " Units")
                            .withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.AQUA));
        }
        appendInteract(tooltip, true);
        appendHeat(spec, tooltip, true);
        add(tooltip, "gt.lang.use.pincers.to.take", ChatFormatting.DARK_GRAY);
        add(tooltip, "gt.lang.use.monkey.wrench.to.toggle.auto.inputs", ChatFormatting.DARK_GRAY);
        add(tooltip, "gt.lang.use.soft.hammer.to.reset", ChatFormatting.DARK_GRAY);
    }
    public static void appendMoldBasin(CrucibleSpec spec, List<Component> tooltip) {
        tooltip.add(Component.translatable("gt.lang.recipes.mold").append(" ")
                .append(Component.translatable("oredict.prefix.blockSolid")).withStyle(ChatFormatting.AQUA));
        appendInteract(tooltip, true);
        appendHeat(spec, tooltip, true);
        add(tooltip, "gt.lang.use.pincers.to.take", ChatFormatting.DARK_GRAY);
    }
    public static void appendCrucibleFaucet(CrucibleSpec spec, List<Component> tooltip) {
        appendInteract(tooltip, false);
        appendHeat(spec, tooltip, false);
        add(tooltip, "gt.lang.use.monkey.wrench.to.toggle.auto.inputs", ChatFormatting.DARK_GRAY);
    }
    /** Crossing defines no specialized rows in the original. */
    public static void appendCrucibleCrossing(CrucibleSpec spec, List<Component> tooltip) {}
    private static void appendInteract(List<Component> tooltip, boolean top) {
        var line = Component.translatable("gt.lang.nogui.rightclick.interact");
        if (top) line.append(" (").append(Component.translatable("gt.lang.face.top")).append(")");
        tooltip.add(line.withStyle(ChatFormatting.GOLD));
    }
    private static void appendHeat(CrucibleSpec spec, List<Component> tooltip, boolean contact) {
        if (spec.acidProof()) add(tooltip, "gt.lang.proof.acid", ChatFormatting.GOLD);
        tooltip.add(Component.translatable("gt.lang.hazard.meltdown")
                .append(" (" + spec.meltDownTemperatureK() + " K)").withStyle(ChatFormatting.DARK_RED));
        if (contact) add(tooltip, "gt.lang.hazard.contact", ChatFormatting.DARK_RED);
    }
    private static void add(List<Component> tooltip, String key, ChatFormatting color) {
        tooltip.add(Component.translatable(key).withStyle(color));
    }
}
