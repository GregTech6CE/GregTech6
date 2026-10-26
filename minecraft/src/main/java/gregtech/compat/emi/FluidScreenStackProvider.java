package gregtech.compat.emi;

import gregtech.client.gui.FluidRecipeScreen;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;

/** R/U over a machine tank uses its real fluid identity and current components. */
final class FluidScreenStackProvider {
    static void register(EmiRegistry registry) {
        registry.addGenericStackProvider((screen, x, y) -> {
            if (!(screen instanceof FluidRecipeScreen fluids)) return EmiStackInteraction.EMPTY;
            var fluid = fluids.fluidAt(x, y);
            if (fluid.isEmpty()) return EmiStackInteraction.EMPTY;
//? if >=1.21.1 {
            /*return new EmiStackInteraction(EmiStack.of(fluid.getFluid(), fluid.getComponentsPatch(), fluid.getAmount()));
*///?} else {
            return new EmiStackInteraction(EmiStack.of(fluid.getFluid(), fluid.getTag(), fluid.getAmount()));
//?}
        });
    }
}
