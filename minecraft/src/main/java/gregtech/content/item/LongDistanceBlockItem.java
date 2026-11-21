package gregtech.content.item;

import gregtech.content.logistics.LongDistanceCatalog;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;

/** Original voltage/amperage/loss and pipeline temperature, with a material label for identical covers. */
public final class LongDistanceBlockItem extends GTBlockItem {
    private final LongDistanceCatalog.Spec spec;

    public LongDistanceBlockItem(Block block, Properties properties, LongDistanceCatalog.Spec spec) {
        super(block, properties);
        this.spec = spec;
    }

    @Override
    protected void appendTooltip(
            ItemStack stack, net.minecraft.world.level.Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendTooltip(stack, level, tooltip, flag);
        if (!spec.material().isEmpty())
            tooltip.add(Component.translatable(
                            "tooltip.gt6ce.long_distance.material",
                            gregtech.engine.material.MaterialPresentation.name(
                                    gregtech.engine.material.GTMaterialRegistry.get(spec.material())))
                    .withStyle(ChatFormatting.GRAY));
        if (spec.kind().endsWith("ENDPOINT") || spec.kind().equals("TRANSFORMER")) {
            tooltip.add(
                    Component.translatable("tooltip.gt6ce.long_distance.wrench").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(
                    Component.translatable("tooltip.gt6ce.long_distance.reset").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("tooltip.gt6ce.long_distance.inspect")
                    .withStyle(ChatFormatting.DARK_GRAY));
            if (spec.kind().equals("TRANSFORMER"))
                tooltip.add(Component.translatable(
                                "tooltip.gt6ce.long_distance.transformer",
                                spec.voltage() / 2,
                                spec.voltage() * 2,
                                spec.voltage())
                        .withStyle(ChatFormatting.AQUA));
        }
        if (spec.kind().equals("WIRE")) {
            tooltip.add(Component.translatable("tooltip.gt6ce.long_distance.voltage", spec.voltage())
                    .withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("tooltip.gt6ce.long_distance.amperage")
                    .withStyle(ChatFormatting.AQUA));
            tooltip.add(
                    Component.translatable("tooltip.gt6ce.long_distance.loss").withStyle(ChatFormatting.AQUA));
        } else if (spec.kind().equals("FLUID_PIPE")) {
            tooltip.add(Component.translatable("tooltip.gt6ce.long_distance.temperature", spec.maximumTemperature())
                    .withStyle(ChatFormatting.AQUA));
        }
    }
}
