package com.gregtech.gregtech.client.gui;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/** Input slot — items can be freely inserted and extracted by the player. */
public class SlotInput extends SlotItemHandler {
    public SlotInput(IItemHandler handler, int index, int x, int y) {
        super(handler, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return true;
    }
}
