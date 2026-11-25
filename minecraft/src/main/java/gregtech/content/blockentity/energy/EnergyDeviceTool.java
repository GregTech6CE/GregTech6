package gregtech.content.blockentity.energy;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@FunctionalInterface
interface EnergyDeviceTool {
    void use(Player player, ItemStack tool);

    static void returnItem(Player player, ItemStack stack) {
        if (!stack.isEmpty() && !player.addItem(stack)) player.drop(stack, false);
    }
}
