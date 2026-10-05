package com.gregtech.gregtech.item;

import com.gregtech.gregtech.content.logistics.LongDistanceCatalog;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import java.util.List;

/** Original voltage/amperage/loss and pipeline temperature, with a material label for identical covers. */
public final class LongDistanceBlockItem extends BlockItem {
    private final LongDistanceCatalog.Spec spec;
    public LongDistanceBlockItem(Block block, Properties properties, LongDistanceCatalog.Spec spec) {
        super(block, properties); this.spec = spec;
    }
    @Override public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (!spec.material().isEmpty()) tooltip.add(Component.translatable("tooltip.gregtech.long_distance.material",
                com.gregtech.gregtech.api.material.MaterialPresentation.name(
                        com.gregtech.gregtech.api.material.GTMaterialRegistry.get(spec.material()))).withStyle(ChatFormatting.GRAY));
        if (spec.kind().endsWith("ENDPOINT") || spec.kind().equals("TRANSFORMER")) {
            tooltip.add(Component.translatable("tooltip.gregtech.long_distance.wrench").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("tooltip.gregtech.long_distance.reset").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("tooltip.gregtech.long_distance.inspect").withStyle(ChatFormatting.DARK_GRAY));
            if(spec.kind().equals("TRANSFORMER")) tooltip.add(Component.translatable("tooltip.gregtech.long_distance.transformer",spec.voltage()/2,spec.voltage()*2,spec.voltage()).withStyle(ChatFormatting.AQUA));
        }
        if (spec.kind().equals("WIRE")) {
            tooltip.add(Component.translatable("tooltip.gregtech.long_distance.voltage", spec.voltage()).withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("tooltip.gregtech.long_distance.amperage").withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("tooltip.gregtech.long_distance.loss").withStyle(ChatFormatting.AQUA));
        } else if (spec.kind().equals("FLUID_PIPE")) {
            tooltip.add(Component.translatable("tooltip.gregtech.long_distance.temperature", spec.maximumTemperature()).withStyle(ChatFormatting.AQUA));
        }
    }
}
