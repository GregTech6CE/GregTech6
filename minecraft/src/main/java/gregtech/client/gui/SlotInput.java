package gregtech.client.gui;

import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
*///?} else {
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
//?}

/** Input slot — items can be freely inserted and extracted by the player. */
public class SlotInput extends SlotItemHandler {
    private final Runnable onChanged;

    public SlotInput(IItemHandler handler, int index, int x, int y) {
        this(handler, index, x, y, () -> {});
    }

    public SlotInput(IItemHandler handler, int index, int x, int y, Runnable onChanged) {
        super(handler, index, x, y);
        this.onChanged = onChanged;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        onChanged.run();
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return true;
    }
}
