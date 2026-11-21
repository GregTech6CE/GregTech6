package gregtech.content.block.machine;

import gregtech.client.SmelteryCompanionTooltips;
import gregtech.content.item.GTBlockItem;
import gregtech.engine.machine.CrucibleSpec;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Block item for smeltery hull blocks that share a {@link CrucibleSpec}. */
public class CrucibleHullBlockItem extends GTBlockItem {
    private final CrucibleSpec spec;

    public CrucibleHullBlockItem(Block block, Properties properties, CrucibleSpec spec) {
        super(block, properties);
        this.spec = spec;
    }

    public CrucibleSpec spec() {
        return spec;
    }

    @Override
    protected void appendTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendTooltip(stack, level, tooltip, flag);
        Block block = getBlock();
        if (block instanceof MoldBlock) {
            SmelteryCompanionTooltips.appendMold(stack, spec, tooltip);
        } else if (block instanceof MoldBasinBlock) {
            SmelteryCompanionTooltips.appendMoldBasin(spec, tooltip);
        } else if (block instanceof CrucibleFaucetBlock) {
            SmelteryCompanionTooltips.appendCrucibleFaucet(spec, tooltip);
        } else if (block instanceof CrucibleCrossingBlock) {
            SmelteryCompanionTooltips.appendCrucibleCrossing(spec, tooltip);
        }
    }
}
