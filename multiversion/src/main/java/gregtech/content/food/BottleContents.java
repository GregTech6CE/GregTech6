package gregtech.content.food;

import gregtech.item.BottleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.1 {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
*///?} else {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
//?}

/** Actual crate contents. Unknown/custom empty payloads are never invented as water. */
public final class BottleContents {
    private BottleContents() {}
    public static FluidStack resolve(ItemStack stack) {
        if (stack.isEmpty() || stack.is(Items.GLASS_BOTTLE)
                || stack.is(BottleItem.emptyBottle().getItem())) return FluidStack.EMPTY;
        if (stack.getItem() instanceof BottleItem bottle) {
            var fluid = bottle.fluid();
            return fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, BottleItem.BOTTLE_AMOUNT);
        }
        // Preserve third-party fluid stack tags/components for texture and tint extensions.
        var contained = FluidUtil.getFluidContained(stack);
        if (contained.isPresent()) return contained.get();
        String field = stack.is(Items.EXPERIENCE_BOTTLE) ? "Potion_Jump_1"
                : stack.is(Items.HONEY_BOTTLE) ? "Honey"
                : stack.is(Items.DRAGON_BREATH) ? "Dragon_Breath" : null;
        if (field != null) {
            var fluid = GTDrinks.fluidForField(field);
            return fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, BottleItem.BOTTLE_AMOUNT);
        }
        // Vanilla normal/splash/lingering potions intentionally use water plus their potion tint.
        return stack.getItem() instanceof PotionItem
                ? new FluidStack(Fluids.WATER, BottleItem.BOTTLE_AMOUNT) : FluidStack.EMPTY;
    }
}
