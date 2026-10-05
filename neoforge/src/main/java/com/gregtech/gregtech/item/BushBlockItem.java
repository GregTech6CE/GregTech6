package com.gregtech.gregtech.item;

import com.gregtech.gregtech.block.plant.BushBlock;
import com.gregtech.gregtech.content.plant.GTBerryBushes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/** Material variant names reuse the actual berry's localized material name. */
public final class BushBlockItem extends BlockItem {
    public BushBlockItem(BushBlock block, Properties properties) { super(block, properties); }

    @Override public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, java.util.List<Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        var id=((BushBlock)getBlock()).berryId();
        var key=GTBerryBushes.itemId(id);
        Component output=key==null ? Component.translatable("tooltip.gregtech.bush.set_output")
                : new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(key)).getHoverName();
        lines.add(output.copy().withStyle(net.minecraft.ChatFormatting.AQUA));
    }

    @Override public Component getName(ItemStack stack) { return getBlock().getName(); }
}
