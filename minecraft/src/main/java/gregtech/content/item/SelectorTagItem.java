package gregtech.content.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Original ItemIntegratedCircuit family name and configuration tooltip, for the flattened 0..24 tags. */
public final class SelectorTagItem extends TechItem {
    private final int configuration;

    public SelectorTagItem(int configuration, Properties properties) {
        super("Selector Tag", true, properties);
        if (configuration < 0 || configuration > 24) throw new IllegalArgumentException("Selector tag configuration");
        this.configuration = configuration;
    }

    @Override
    protected void appendTooltip(
            ItemStack stack,
            @org.jetbrains.annotations.Nullable net.minecraft.world.level.Level level,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendTooltip(stack, level, tooltip, flag);
        tooltip.add(
                Component.translatable("gt.integrated_circuit.configuration").append("== " + configuration));
    }
}
