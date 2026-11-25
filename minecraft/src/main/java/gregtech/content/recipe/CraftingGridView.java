package gregtech.content.recipe;

import java.util.List;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.1 {
/*import net.minecraft.world.item.crafting.CraftingInput;
*///?} else {
import net.minecraft.world.inventory.CraftingContainer;
//?}

/**
 * Unified read-only view over the crafting grid ({@code CraftingInput} on 1.21.1,
 * {@code CraftingContainer} on 1.20.1), including uncropped form-conversion slot metadata.
 */
public final class CraftingGridView {
//? if >=1.21.1 {
    /*private final CraftingInput grid;

    private CraftingGridView(CraftingInput grid) {
        this.grid = grid;
    }

    public static CraftingGridView of(CraftingInput grid) {
        return new CraftingGridView(grid);
    }

    CraftingInput raw() {
        return grid;
    }

    public int size() {
        return grid.size();
    }

    public int width() {
        return grid.width();
    }

    public int height() {
        return grid.height();
    }

    public ItemStack getItem(int slot) {
        return grid.getItem(slot);
    }

    public List<ItemStack> items() {
        return grid.items();
    }

    public int formGridSize() {
        return ((CraftingFormPosition) grid).gregtech$formGridSize();
    }

    public int formFirstSlot() {
        return ((CraftingFormPosition) grid).gregtech$formFirstSlot();
    }

    public int occupiedCount() {
        return grid.ingredientCount();
    }
*///?} else {
    private final CraftingContainer grid;

    private CraftingGridView(CraftingContainer grid) {
        this.grid = grid;
    }

    public static CraftingGridView of(CraftingContainer grid) {
        return new CraftingGridView(grid);
    }

    CraftingContainer raw() {
        return grid;
    }

    public int size() {
        return grid.getContainerSize();
    }

    public int width() {
        return grid.getWidth();
    }

    public int height() {
        return grid.getHeight();
    }

    public ItemStack getItem(int slot) {
        return grid.getItem(slot);
    }

    public List<ItemStack> items() {
        return grid.getItems();
    }

    public int formGridSize() {
        return grid.getContainerSize();
    }

    public int formFirstSlot() {
        for (int i = 0; i < grid.getContainerSize(); i++) {
            if (!grid.getItem(i).isEmpty()) return i;
        }
        return -1;
    }

    public int occupiedCount() {
        int occupied = 0;
        for (int i = 0; i < grid.getContainerSize(); i++) {
            if (!grid.getItem(i).isEmpty()) occupied++;
        }
        return occupied;
    }
//?}
}
