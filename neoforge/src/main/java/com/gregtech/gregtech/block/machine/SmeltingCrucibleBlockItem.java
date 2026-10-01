package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.api.machine.CrucibleSpec;
import com.gregtech.gregtech.client.CrucibleTooltips;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

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
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        CrucibleTooltips.appendSmeltingCrucible(spec, tooltip, flag);
    }
}
