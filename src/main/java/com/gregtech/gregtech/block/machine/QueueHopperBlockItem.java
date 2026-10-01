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

/** Block item for GT6 queuehoppers with detailed material/block tooltips. */
public class QueueHopperBlockItem extends BlockItem {
    private final HopperSpec spec;

    public QueueHopperBlockItem(QueueHopperBlock block, Properties properties, HopperSpec spec) {
        super(block, properties);
        this.spec = spec;
    }

    public HopperSpec spec() { return spec; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        HopperTooltips.appendQueueHopper(spec, tooltip, flag);
    }
}
