package gregtech.client.gui;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
//?}

/**
 * Ghost slot that displays a fluid tank's contents as a FluidItem icon
 * with an amount overlay. Cannot be manipulated by the player.
 */
public class SlotFluid extends SlotItemHandler {
    private final int tankIndex;
    private final boolean isInput;
    private FluidStack fluid = FluidStack.EMPTY;
    private long amount, capacity;

    public SlotFluid(IItemHandler dummy, int slotIndex, int x, int y, int tankIndex, boolean isInput) {
        super(dummy, slotIndex, x, y);
        this.tankIndex = tankIndex;
        this.isInput = isInput;
    }

    public int tankIndex() { return tankIndex; }
    public boolean isInput() { return isInput; }

    public FluidStack fluid() { return fluid; }
    public long amount() { return amount; }
    public long capacity() { return capacity; }
    public void setFluid(FluidStack f, long amount, long capacity) {
        this.fluid = f != null ? f.copy() : FluidStack.EMPTY;
        this.amount = Math.max(0, amount);
        this.capacity = Math.max(0, capacity);
    }

    @Override
    public boolean mayPlace(ItemStack stack) { return false; }

    @Override
    public boolean mayPickup(Player player) { return false; }
}
