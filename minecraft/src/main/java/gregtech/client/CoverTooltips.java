package gregtech.client;

import gregtech.content.cover.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.List;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
//?}

/** Original cover rate/target tooltips, plus the currently saved mode. No Jade dependency. */
public final class CoverTooltips {
    private CoverTooltips() {}

    public static void append(ItemStack stack, Level level, List<Component> tooltip) {
        if (CoverItems.isCover(stack)) hint(tooltip, "tooltip.gt6ce.cover.sneak_place");
        for (var line : CoverDisplayInfo.describe(stack, level)) {
            Object[] args = java.util.Arrays.stream(line.arguments()).map(CoverTooltips::display).toArray();
            tooltip.add(gregtech.engine.fluid.FluidUnits.text(line.key(), args).withStyle(ChatFormatting.AQUA));
        }
        String behavior = CoverItems.behavior(stack);
        boolean component = ComponentCoverRuntime.kind(stack) != null;
        boolean filter = CoverUtilityBehaviors.FILTER_ITEM.equals(behavior)
                || CoverUtilityBehaviors.FILTER_FLUID.equals(behavior)
                || CoverUtilityBehaviors.RETRIEVER_ITEM.equals(behavior);
        if (ComponentCoverRuntime.kind(stack) == ComponentCoverRules.Kind.PUMP)
            tooltip.add(Component.translatable("tooltip.gt6ce.cover.pump_blocks",
                    Component.translatable("item.gt6ce.drain")).withStyle(ChatFormatting.GOLD));
        if (component || filter) {
            // Reuse the imported LH translations rather than duplicating tool instructions.
            hint(tooltip, "gt.lang.use.controlcover.to.toggle");
            hint(tooltip, "gt.lang.use.screwdriver.to.toggle");
            if (filter) hint(tooltip, "gt.lang.use.soft.hammer.to.reset");
            if (ComponentCoverRuntime.kind(stack) == ComponentCoverRules.Kind.ROBOT_ARM)
                hint(tooltip, "gt.lang.use.monkey.wrench.to.toggle");
        }
        var logistics = gregtech.content.logistics.LogisticsCoverType.of(stack);
        if (logistics != null) {
            hint(tooltip, "gt.lang.use.controlcover.to.toggle");
            if (logistics.usesPriority()) hint(tooltip, "gt.lang.use.screwdriver.to.toggle");
            if (logistics.targetStackSize()) hint(tooltip, "gt.lang.use.cutter.to.toggle");
            if (logistics.filtered()) hint(tooltip, "gt.lang.use.soft.hammer.to.reset");
        }
    }

    private static Object display(Object value) {
        if (value instanceof CoverDisplayInfo.Text text) return Component.translatable(text.key());
        if (value instanceof ItemStack item) return item.getHoverName();
        if (value instanceof FluidStack fluid) return fluid.getDisplayName();
        return value;
    }

    private static void hint(List<Component> lines, String key) {
        if (!CommonBlockTooltips.containsKey(lines, key))
            lines.add(gregtech.engine.fluid.FluidUnits.text(key).withStyle(ChatFormatting.DARK_GRAY));
    }
}
