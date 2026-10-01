package com.gregtech.gregtech.item;
import com.gregtech.gregtech.api.material.MaterialPresentation;

import com.gregtech.gregtech.api.energy.WireSpec;
import com.gregtech.gregtech.block.energy.ElectricWireBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** BlockItem for wires that computes the display name from size and material. */
public class WireBlockItem extends BlockItem {
    public WireBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (getBlock() instanceof ElectricWireBlock wire) {
            WireSpec spec = wire.spec();
            String key = "item." + com.gregtech.gregtech.GregTech.MODID + "." + (spec.insulated() ? "cable" : "wire");
            return Component.translatable(key,
                    MaterialPresentation.name(spec.material()),
                    Component.literal(String.valueOf(spec.size())));
        }
        return super.getName(stack);
    }
}
