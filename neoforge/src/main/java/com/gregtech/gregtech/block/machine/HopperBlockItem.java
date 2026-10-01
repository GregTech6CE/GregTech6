package com.gregtech.gregtech.block.machine;

import com.gregtech.gregtech.api.machine.HopperSpec;
import com.gregtech.gregtech.client.HopperTooltips;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Block item for GT6 hoppers with detailed material/block tooltips. */
public class HopperBlockItem extends BlockItem {
    private final HopperSpec spec;

    public HopperBlockItem(HopperBlock block, Properties properties, HopperSpec spec) {
        super(block, properties);
        this.spec = spec;
    }

    public HopperSpec spec() { return spec; }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack,context,tooltip,flag);
        HopperTooltips.appendHopper(spec, tooltip, flag);
    }
}
