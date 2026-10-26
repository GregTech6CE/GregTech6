package gregtech.client.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Common type for the default behavior machine screen and an addon screen hosted over it. */
public abstract class BehaviorMachineHostScreen extends MachineContainerScreen<BehaviorMachineMenu> {
    protected BehaviorMachineHostScreen(BehaviorMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override protected net.minecraft.world.item.ItemStack titleIcon() {
        return new net.minecraft.world.item.ItemStack(menu.machine().getBlockState().getBlock());
    }
}
