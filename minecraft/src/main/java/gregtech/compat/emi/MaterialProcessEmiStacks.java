package gregtech.compat.emi;

import dev.emi.emi.api.stack.EmiStack;
import gregtech.engine.fluid.FluidDisplayBinding;
import net.minecraft.world.item.ItemStack;

/** The overview's display item is never the lookup handle for a native fluid. */
public final class MaterialProcessEmiStacks {
    private MaterialProcessEmiStacks() {}

    public static EmiStack of(ItemStack icon) {
        if (!FluidDisplayBinding.hasPayload(icon)) return EmiStack.of(icon);
//? if >=1.21.1 {
        /*var fluid = FluidDisplayBinding.resolveCurrent(icon);
        return fluid.isEmpty() ? EmiStack.EMPTY : EmiStack.of(fluid.getFluid(), fluid.getComponentsPatch(), 1);
*///?} else {
        var fluid = FluidDisplayBinding.resolve(icon);
        return fluid.isEmpty() ? EmiStack.EMPTY : EmiStack.of(fluid.getFluid(), fluid.getTag(), 1);
//?}
    }
}
