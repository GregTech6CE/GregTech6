package gregtech.content.block.machine;

import gregtech.client.HopperTooltips;
import gregtech.content.item.GTBlockItem;
import gregtech.engine.machine.HopperSpec;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Block item for GT6 hoppers with detailed material/block tooltips. */
public class HopperBlockItem extends GTBlockItem {
    private final HopperSpec spec;

    public HopperBlockItem(HopperBlock block, Properties properties, HopperSpec spec) {
        super(block, properties);
        this.spec = spec;
    }

    public HopperSpec spec() {
        return spec;
    }

    @Override
    protected void appendTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendTooltip(stack, level, tooltip, flag);
        HopperTooltips.appendHopper(spec, stack, tooltip);
    }
}
