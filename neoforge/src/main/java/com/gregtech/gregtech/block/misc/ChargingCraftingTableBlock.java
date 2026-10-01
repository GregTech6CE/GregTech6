package com.gregtech.gregtech.block.misc;

import com.gregtech.gregtech.api.material.GTMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Same crafting/inventory behavior as the advanced table, with direct item charging. */
public final class ChargingCraftingTableBlock extends AdvancedCraftingTableBlock {
    public ChargingCraftingTableBlock(GTMaterial material,Properties properties){super(material,properties);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new ChargingCraftingTableBlockEntity(pos,state);}
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.Item.TooltipContext context,
            java.util.List<net.minecraft.network.chat.Component> tooltip,net.minecraft.world.item.TooltipFlag flag){
        super.appendHoverText(stack,context,tooltip,flag);tooltip.add(net.minecraft.network.chat.Component.translatable("gt.tooltip.charging_crafting.packets"));
    }
}
