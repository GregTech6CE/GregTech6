package gregtech.engine.inventory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.PlayerMainInvWrapper;
*///?} else {
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;
//?}

/** Shared item-handler insertion helpers across Forge 1.20.1 and NeoForge 1.21.1. */
public final class ItemHandlerCompat {
    private ItemHandlerCompat() {}

    public static ItemStack insertItem(IItemHandler dest, ItemStack stack, boolean simulate) {
        return ItemHandlerHelper.insertItem(dest, stack, simulate);
    }

    public static ItemStack insertItemStacked(IItemHandler dest, ItemStack stack, boolean simulate) {
        return ItemHandlerHelper.insertItemStacked(dest, stack, simulate);
    }

    public static ItemStack insertPlayerMainInventory(Player player, ItemStack stack, boolean simulate) {
        return ItemHandlerHelper.insertItemStacked(new PlayerMainInvWrapper(player.getInventory()), stack, simulate);
    }
}
