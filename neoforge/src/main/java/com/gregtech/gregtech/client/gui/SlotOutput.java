package com.gregtech.gregtech.client.gui;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Output slot — items can be taken but not placed by the player. */
public class SlotOutput extends SlotItemHandler {
    public SlotOutput(IItemHandler handler, int index, int x, int y) {
        super(handler, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

        @Override
    public boolean mayPickup(net.minecraft.world.entity.player.Player player) {
        return true;
    }
}
