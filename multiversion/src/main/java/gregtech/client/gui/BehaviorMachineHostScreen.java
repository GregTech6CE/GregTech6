package gregtech.client.gui;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Common type for the default behavior machine screen and an addon screen hosted over it. */
public abstract class BehaviorMachineHostScreen extends AbstractContainerScreen<BehaviorMachineMenu> {
    protected BehaviorMachineHostScreen(BehaviorMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
