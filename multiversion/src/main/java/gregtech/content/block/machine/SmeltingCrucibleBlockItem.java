package gregtech.content.block.machine;

import gregtech.internal.machine.CrucibleSpec;
import gregtech.client.CrucibleTooltips;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
//? if >=1.21.1 {
//?} else {
import net.minecraft.world.level.Level;
//?}
import net.minecraft.world.level.block.Block;

//? if >=1.21.1 {
//?} else {
import javax.annotation.Nullable;
//?}
import java.util.List;

public class SmeltingCrucibleBlockItem extends BlockItem {
    private final CrucibleSpec spec;

    public SmeltingCrucibleBlockItem(Block block, Properties properties, CrucibleSpec spec) {
        super(block, properties);
        this.spec = spec;
    }

    public CrucibleSpec spec() {
        return spec;
    }

    @Override
//? if >=1.21.1 {
    /*public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
*///?} else {
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
//?}
        CrucibleTooltips.appendSmeltingCrucible(spec, tooltip, flag);
    }
}
