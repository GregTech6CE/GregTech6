package gregtech.block.misc;

import gregtech.internal.material.GTMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Same crafting/inventory behavior as the advanced table, with direct item charging. */
public final class ChargingCraftingTableBlock extends AdvancedCraftingTableBlock {

    public ChargingCraftingTableBlock(GTMaterial material, Properties properties) {
        super(material, properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChargingCraftingTableBlockEntity(pos, state);
    }
//? if >=1.21.1 {
    /*@Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
*///?} else {
    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, @javax.annotation.Nullable net.minecraft.world.level.BlockGetter level,
//?}
            java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
//? if >=1.21.1 {
        /*super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(net.minecraft.network.chat.Component.translatable("gt.tooltip.charging_crafting.packets"));
*///?} else {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(net.minecraft.network.chat.Component.translatable("gt.tooltip.charging_crafting.packets"));
//?}
    }
}
