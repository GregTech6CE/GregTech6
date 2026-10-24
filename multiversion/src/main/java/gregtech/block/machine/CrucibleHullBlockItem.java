package gregtech.block.machine;

import gregtech.api.machine.CrucibleSpec;
import gregtech.client.SmelteryCompanionTooltips;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;

/** Block item for smeltery hull blocks that share a {@link CrucibleSpec}. */
public class CrucibleHullBlockItem extends BlockItem {
    private final CrucibleSpec spec;

    public CrucibleHullBlockItem(Block block, Properties properties, CrucibleSpec spec) {
        super(block, properties);
        this.spec = spec;
    }

    public CrucibleSpec spec() {
        return spec;
    }

    @Override
//? if >=1.21.1 {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack,context,tooltip,flag);
*///?} else {
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
//?}
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
