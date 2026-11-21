package gregtech.content.block.machine;

import gregtech.client.CrucibleTooltips;
import gregtech.content.item.GTBlockItem;
import gregtech.engine.machine.CrucibleSpec;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class SmeltingCrucibleBlockItem extends GTBlockItem {
    private final CrucibleSpec spec;

    public SmeltingCrucibleBlockItem(Block block, Properties properties, CrucibleSpec spec) {
        super(block, properties);
        this.spec = spec;
    }

    public CrucibleSpec spec() {
        return spec;
    }

    @Override
    protected void appendTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendTooltip(stack, level, tooltip, flag);
        CrucibleTooltips.appendSmeltingCrucible(spec, tooltip, flag);
    }
}
